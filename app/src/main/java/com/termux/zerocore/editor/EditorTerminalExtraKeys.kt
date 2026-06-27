package com.termux.zerocore.editor

import android.app.Activity
import android.view.View
import com.google.android.material.button.MaterialButton
import com.termux.shared.interact.ShareUtils
import com.termux.shared.logger.Logger
import com.termux.shared.termux.extrakeys.ExtraKeyButton
import com.termux.shared.termux.extrakeys.ExtraKeysConstants
import com.termux.shared.termux.extrakeys.ExtraKeysInfo
import com.termux.shared.termux.extrakeys.ExtraKeysView
import com.termux.shared.termux.extrakeys.SpecialButton
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants
import com.termux.shared.termux.settings.properties.TermuxSharedProperties
import com.termux.shared.termux.terminal.io.TerminalExtraKeys
import com.termux.view.TerminalView
import org.json.JSONException

class EditorTerminalExtraKeys(
    private val activity: Activity,
    private val terminalView: TerminalView,
    private val extraKeysView: ExtraKeysView,
    private val viewClient: EditorTerminalViewClient
) : TerminalExtraKeys(terminalView) {

    private var extraKeysInfo: ExtraKeysInfo? = null

    init {
        loadExtraKeys()
    }

    fun getExtraKeysInfo(): ExtraKeysInfo? = extraKeysInfo

    private fun loadExtraKeys() {
        extraKeysInfo = null
        val properties = TermuxAppSharedProperties.getProperties() ?: return
        try {
            val extraKeys = properties.getInternalPropertyValue(
                TermuxPropertyConstants.KEY_EXTRA_KEYS,
                true
            ) as String
            var extraKeysStyle = properties.getInternalPropertyValue(
                TermuxPropertyConstants.KEY_EXTRA_KEYS_STYLE,
                true
            ) as String
            val extraKeyDisplayMap = ExtraKeysInfo.getCharDisplayMapForStyle(extraKeysStyle)
            if (ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY == extraKeyDisplayMap &&
                TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE != extraKeysStyle
            ) {
                Logger.logError(
                    TermuxSharedProperties.LOG_TAG,
                    "Invalid extra keys style \"$extraKeysStyle\", using default."
                )
                extraKeysStyle = TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE
            }
            extraKeysInfo = ExtraKeysInfo(
                extraKeys,
                extraKeysStyle,
                ExtraKeysConstants.CONTROL_CHARS_ALIASES
            )
        } catch (e: JSONException) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load extra keys", e)
            try {
                extraKeysInfo = ExtraKeysInfo(
                    TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS,
                    TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE,
                    ExtraKeysConstants.CONTROL_CHARS_ALIASES
                )
            } catch (fallback: JSONException) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load default extra keys", fallback)
            }
        }
    }

    override fun onExtraKeyButtonClick(view: View, buttonInfo: ExtraKeyButton, button: MaterialButton) {
        if (buttonInfo.isMacro) {
            super.onExtraKeyButtonClick(view, buttonInfo, button)
            return
        }
        onTerminalExtraKeyButtonClick(
            view,
            buttonInfo.key,
            readModifier(SpecialButton.CTRL),
            readModifier(SpecialButton.ALT),
            readModifier(SpecialButton.SHIFT),
            readModifier(SpecialButton.FN)
        )
    }

    override fun onTerminalExtraKeyButtonClick(
        view: View,
        key: String,
        ctrlDown: Boolean,
        altDown: Boolean,
        shiftDown: Boolean,
        fnDown: Boolean
    ) {
        when (key) {
            "KEYBOARD" -> viewClient.showKeyboardForInput()
            "PASTE" -> pasteFromClipboard()
            "SCROLL" -> terminalView.mEmulator?.toggleAutoScrollDisabled()
            else -> super.onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown)
        }
    }

    private fun readModifier(button: SpecialButton): Boolean {
        return extraKeysView.readSpecialButton(button, true) == true
    }

    private fun pasteFromClipboard() {
        val text = ShareUtils.getTextStringFromClipboardIfSet(activity, true) ?: return
        val emulator = terminalView.mEmulator ?: return
        emulator.paste(text)
    }

    companion object {
        private const val LOG_TAG = "EditorTerminalExtraKeys"
    }
}
