package com.termux.zerocore.adb

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.termux.BuildConfig
import com.termux.R

import java.io.File
import java.io.PrintStream
import java.util.concurrent.TimeUnit

class ADB(private val context: Context) {
    companion object {
        const val MAX_OUTPUT_BUFFER_SIZE = 1024 * 16
        const val OUTPUT_BUFFER_DELAY_MS = 100L

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: ADB? = null
        fun getInstance(context: Context): ADB = instance ?: synchronized(this) {
            instance ?: ADB(context).also { instance = it }
        }
    }

    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val adbPath = "${context.applicationInfo.nativeLibraryDir}/libadb.so"
    private val scriptPath = "${context.getExternalFilesDir(null)}/script.sh"

    /**
     * Is the shell ready to handle commands?
     */
    private val _started = MutableLiveData(false)
    val started: LiveData<Boolean> = _started

    private var tryingToPair = false

    /**
     * Is the shell closed for any reason?
     */
    private val _closed = MutableLiveData(false)
    val closed: LiveData<Boolean> = _closed

    /**
     * Where shell output is stored
     */
    val outputBufferFile: File = File.createTempFile("buffer", ".txt").also {
        it.deleteOnExit()
    }

    /**
     * Single shell instance where we can pipe commands to
     */
    private var shellProcess: Process? = null

    /**
     * Returns the user buffer size if valid, else the default
     */
    fun getOutputBufferSize(): Int {
        val userValue = sharedPrefs.getString(context.getString(R.string.buffer_size_key), "16384")!!
        return try {
            Integer.parseInt(userValue)
        } catch (_: NumberFormatException) {
            MAX_OUTPUT_BUFFER_SIZE
        }
    }

    /**
     * Start the ADB server
     */
    fun initServer(): Boolean {
        if (_started.value == true || tryingToPair)
            return true

        tryingToPair = true

        val autoShell = sharedPrefs.getBoolean(context.getString(R.string.auto_shell_key), true)

        val secureSettingsGranted =
            context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

        if (autoShell) {
            /* Only do wireless debugging steps on compatible versions */
            if (secureSettingsGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isWirelessDebuggingEnabled()) {
                    Settings.Global.putInt(
                        context.contentResolver,
                        "adb_wifi_enabled",
                        1
                    )

                    Thread.sleep(2_000)
                } else if (!isUSBDebuggingEnabled()) {
                    Settings.Global.putInt(
                        context.contentResolver,
                        Settings.Global.ADB_ENABLED,
                        1
                    )

                    Thread.sleep(2_000)
                }
            }

            /* Check again... */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isWirelessDebuggingEnabled()) {
                debug("Wireless debugging is not enabled!")
                debug("Settings -> Developer options -> Wireless debugging")
                debug("Waiting for wireless debugging...")

                while (!isWirelessDebuggingEnabled()) {
                    Thread.sleep(1_000)
                }
            } else if (!isUSBDebuggingEnabled()) {
                debug("USB debugging is not enabled!")
                debug("Settings -> Developer options -> USB debugging")
                debug("Waiting for USB debugging...")

                while (!isUSBDebuggingEnabled()) {
                    Thread.sleep(1_000)
                }
            }

            adb(false, listOf("start-server")).waitFor()
            debug("Waiting for device to connect...")
            debug("This may take a minute")
            val waitProcess = adb(false, listOf("wait-for-device")).waitFor(1, TimeUnit.MINUTES)
            if (!waitProcess) {
                debug("Your device didn't connect to LADB")
                debug("If a reboot doesn't work, please contact support")

                tryingToPair = false
                return false
            }
        }

        shellProcess = if (autoShell) {
            val argList = if (Build.SUPPORTED_ABIS[0] == "arm64-v8a")
                listOf("-t", "1", "shell")
            else
                listOf("shell")

            adb(true, argList)
        } else {
            shell(true, listOf("sh", "-l"))
        }

        sendToShellProcess("alias adb=\"$adbPath\"")

        if (!secureSettingsGranted) {
            sendToShellProcess("pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS &> /dev/null")
        }

        if (autoShell)
            sendToShellProcess("echo 'Entered adb shell'")
        else
            sendToShellProcess("echo 'Entered non-adb shell'")

        val startupCommand =
            sharedPrefs.getString(context.getString(R.string.startup_command_key), "echo 'Success! ※\\(^o^)/※'")!!
        if (startupCommand.isNotEmpty())
            sendToShellProcess(startupCommand)

        _started.postValue(true)
        tryingToPair = false

        return true
    }

    private fun isWirelessDebuggingEnabled() =
        Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1

    private fun isUSBDebuggingEnabled() =
        Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1

    /**
     * Wait restart the shell once it dies
     */
    fun waitForDeathAndReset() {
        while (true) {
            shellProcess?.waitFor()
            _started.postValue(false)
            debug("Shell is dead, resetting")
            adb(false, listOf("kill-server")).waitFor()
            Thread.sleep(3_000)
            initServer()
        }
    }

    /**
     * Ask the device to pair on Android 11+ devices
     */
    fun pair(port: String, pairingCode: String): Boolean {
        val pairShell = adb(false, listOf("pair", "localhost:$port"))

        /* Sleep to allow shell to catch up */
        Thread.sleep(5000)

        /* Pipe pairing code */
        PrintStream(pairShell.outputStream).apply {
            println(pairingCode)
            flush()
        }

        /* Continue once finished pairing (or 10s elapses) */
        pairShell.waitFor(10, TimeUnit.SECONDS)
        pairShell.destroyForcibly().waitFor()

        val killShell = adb(false, listOf("kill-server"))
        killShell.waitFor(3, TimeUnit.SECONDS)
        killShell.destroyForcibly()

        return pairShell.exitValue() == 0
    }

    /**
     * Send a raw ADB command
     */
    private fun adb(redirect: Boolean, command: List<String>): Process {
        val commandList = command.toMutableList().also {
            it.add(0, adbPath)
        }
        return shell(redirect, commandList)
    }

    /**
     * Send a raw shell command
     */
    private fun shell(redirect: Boolean, command: List<String>): Process {
        val processBuilder = ProcessBuilder(command)
            .directory(context.filesDir)
            .apply {
                if (redirect) {
                    redirectErrorStream(true)
                    redirectOutput(outputBufferFile)
                }

                environment().apply {
                    put("HOME", context.filesDir.path)
                    put("TMPDIR", context.cacheDir.path)
                }
            }

        return processBuilder.start()!!
    }

    /**
     * Execute a script
     */
    fun sendScript(code: String) {
        /* Store script locally */
        val internalScript = File(scriptPath).apply {
            bufferedWriter().use {
                it.write(code)
            }
            deleteOnExit()
        }

        /* Execute the script here */
        sendToShellProcess("sh ${internalScript.absolutePath}")
    }

    /**
     * Send commands directly to the shell process
     */
    fun sendToShellProcess(msg: String) {
        if (shellProcess == null || shellProcess?.outputStream == null)
            return
        PrintStream(shellProcess!!.outputStream!!).apply {
            println(msg)
            flush()
        }
    }

    /**
     * Write a debug message to the user
     */
    fun debug(msg: String) {
        synchronized(outputBufferFile) {
            if (outputBufferFile.exists())
                outputBufferFile.appendText("* $msg" + System.lineSeparator())
        }
    }
}
