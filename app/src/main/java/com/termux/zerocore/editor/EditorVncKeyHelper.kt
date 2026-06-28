package com.termux.zerocore.editor

import android.os.SystemClock
import android.view.KeyEvent
import com.gaurav.avnc.ui.vnc.EmbeddedVncFragment

/** Sends shortcut keys to [EmbeddedVncFragment], with reflection fallback for older avnc.aar. */
object EditorVncKeyHelper {

    fun showSoftKeyboard(fragment: EmbeddedVncFragment) {
        invokeNoArg(fragment, "showSoftKeyboard") {
            fragment.showKeyboard()
        }
    }

    fun sendVirtualKey(fragment: EmbeddedVncFragment, keyCode: Int, metaState: Int = 0) {
        if (invokeVirtualKey(fragment, keyCode, metaState)) return
        dispatchViaKeyHandler(fragment, keyCode, metaState, asText = false)
    }

    fun sendVirtualText(fragment: EmbeddedVncFragment, text: String) {
        if (text.isEmpty()) return
        if (invokeVirtualText(fragment, text)) return
        dispatchViaKeyHandler(fragment, 0, 0, asText = true, text = text)
    }

    private fun invokeVirtualKey(fragment: EmbeddedVncFragment, keyCode: Int, metaState: Int): Boolean {
        return try {
            fragment.javaClass
                .getMethod("sendVirtualKey", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .invoke(fragment, keyCode, metaState)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun invokeVirtualText(fragment: EmbeddedVncFragment, text: String): Boolean {
        return try {
            fragment.javaClass
                .getMethod("sendVirtualText", String::class.java)
                .invoke(fragment, text)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun invokeNoArg(fragment: EmbeddedVncFragment, name: String, fallback: () -> Unit) {
        try {
            fragment.javaClass.getMethod(name).invoke(fragment)
        } catch (_: Exception) {
            fallback()
        }
    }

    private fun dispatchViaKeyHandler(
        fragment: EmbeddedVncFragment,
        keyCode: Int,
        metaState: Int,
        asText: Boolean,
        text: String = ""
    ) {
        if (!fragment.isConnected()) return
        val handler = readKeyHandler(fragment) ?: return
        val event = if (asText) {
            KeyEvent(SystemClock.uptimeMillis(), text, 0, 0)
        } else {
            KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, metaState, 0, 0)
        }
        try {
            handler.javaClass.getMethod("onKeyEvent", KeyEvent::class.java).invoke(handler, event)
            if (!asText) {
                val up = KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0, metaState, 0, 0)
                handler.javaClass.getMethod("onKeyEvent", KeyEvent::class.java).invoke(handler, up)
            }
        } catch (_: Exception) {
        }
    }

    private fun readKeyHandler(fragment: EmbeddedVncFragment): Any? {
        return try {
            val field = EmbeddedVncFragment::class.java.getDeclaredField("keyHandler")
            field.isAccessible = true
            field.get(fragment)
        } catch (_: Exception) {
            null
        }
    }
}
