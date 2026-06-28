package com.termux.zerocore.editor

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import com.google.android.material.button.MaterialButton
import com.gaurav.avnc.ui.vnc.EmbeddedVncFragment
import com.termux.shared.termux.extrakeys.ExtraKeyButton
import com.termux.shared.termux.extrakeys.ExtraKeysConstants
import com.termux.shared.termux.extrakeys.ExtraKeysInfo
import com.termux.shared.termux.extrakeys.ExtraKeysView
import com.termux.shared.termux.extrakeys.SpecialButton
import org.json.JSONException

class EditorVncExtraKeys(
    private val activity: Activity,
    private val extraKeysView: ExtraKeysView,
    private val findFragment: () -> EmbeddedVncFragment?
) : ExtraKeysView.IExtraKeysView {

    private var extraKeysInfo: ExtraKeysInfo? = null

    init {
        loadExtraKeys()
    }

    fun reload() {
        loadExtraKeys()
        reloadView()
    }

    private fun loadExtraKeys() {
        extraKeysInfo = try {
            ExtraKeysInfo(
                DEFAULT_VNC_EXTRA_KEYS,
                "default",
                ExtraKeysConstants.CONTROL_CHARS_ALIASES
            )
        } catch (e: JSONException) {
            null
        }
    }

    fun bindView() {
        extraKeysView.setExtraKeysViewClient(this)
        extraKeysView.setRepetitiveKeys(ExtraKeysConstants.PRIMARY_REPETITIVE_KEYS)
        reloadView()
    }

    fun reloadView() {
        val info = extraKeysInfo ?: return
        val rowCount = info.matrix.size.coerceAtLeast(1)
        val rowHeightPx = dp(DEFAULT_ROW_HEIGHT_DP)
        val totalHeight = rowHeightPx * rowCount
        val params = extraKeysView.layoutParams
        if (params != null && params.height != totalHeight) {
            params.height = totalHeight
            extraKeysView.layoutParams = params
        }
        extraKeysView.reload(info, rowHeightPx.toFloat())
    }

    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }

    override fun onExtraKeyButtonClick(view: View, buttonInfo: ExtraKeyButton, button: MaterialButton) {
        if (extraKeysView.isSpecialButton(buttonInfo)) {
            return
        }
        if (buttonInfo.isMacro) {
            val keys = buttonInfo.key.split(" ")
            var ctrlDown = false
            var altDown = false
            var shiftDown = false
            var fnDown = false
            for (key in keys) {
                when (key) {
                    SpecialButton.CTRL.key -> ctrlDown = true
                    SpecialButton.ALT.key -> altDown = true
                    SpecialButton.SHIFT.key -> shiftDown = true
                    SpecialButton.FN.key -> fnDown = true
                    else -> {
                        dispatchKey(key, ctrlDown, altDown, shiftDown, fnDown)
                        ctrlDown = false
                        altDown = false
                        shiftDown = false
                        fnDown = false
                    }
                }
            }
            return
        }
        dispatchKey(
            buttonInfo.key,
            readModifier(SpecialButton.CTRL),
            readModifier(SpecialButton.ALT),
            readModifier(SpecialButton.SHIFT),
            readModifier(SpecialButton.FN)
        )
    }

    override fun performExtraKeyButtonHapticFeedback(
        view: View,
        buttonInfo: ExtraKeyButton,
        button: MaterialButton
    ): Boolean = false

    private fun dispatchKey(
        key: String,
        ctrlDown: Boolean,
        altDown: Boolean,
        shiftDown: Boolean,
        fnDown: Boolean
    ) {
        val fragment = findFragment() ?: return
        when (key) {
            "KEYBOARD" -> EditorVncKeyHelper.showSoftKeyboard(fragment)
            else -> {
                val keyCode = ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS[key]
                if (keyCode != null) {
                    EditorVncKeyHelper.sendVirtualKey(
                        fragment,
                        keyCode,
                        buildMetaState(ctrlDown, altDown, shiftDown, fnDown)
                    )
                } else if (key.isNotEmpty()) {
                    EditorVncKeyHelper.sendVirtualText(fragment, key)
                }
            }
        }
    }

    private fun readModifier(button: SpecialButton): Boolean {
        return extraKeysView.readSpecialButton(button, true) == true
    }

    private fun buildMetaState(ctrl: Boolean, alt: Boolean, shift: Boolean, fn: Boolean): Int {
        var meta = 0
        if (ctrl) {
            meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        }
        if (alt) {
            meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        }
        if (shift) {
            meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        }
        if (fn) {
            meta = meta or KeyEvent.META_FUNCTION_ON
        }
        return meta
    }

    companion object {
        /**
         * Row1: UP at col 4; Row2: LEFT/DOWN/RIGHT at cols 3/4/5 — cross-aligned d-pad.
         */
        private const val DEFAULT_VNC_EXTRA_KEYS =
            "[['KEYBOARD', 'ESC', 'TAB', 'CTRL', 'UP', 'ENTER']," +
                "['ALT', 'SHIFT', '-', 'LEFT', 'DOWN', 'RIGHT', 'PGUP', 'PGDN', 'DEL']]"
        private const val DEFAULT_ROW_HEIGHT_DP = 32
    }
}
