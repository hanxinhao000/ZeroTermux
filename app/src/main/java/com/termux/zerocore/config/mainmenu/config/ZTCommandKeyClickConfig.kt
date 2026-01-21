package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY.ACTION_RELOAD_STYLE
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants.KEY_EXTRA_KEYS
import com.termux.zerocore.dialog.MingLShowDialog
import org.json.JSONArray
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class ZTCommandKeyClickConfig : BaseMenuClickConfig() {
    companion object {
        val TAG = ZTCommandKeyClickConfig::class.simpleName
    }

    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.zt_command_key)
    }

    override fun getString(context: Context?): String? {
        return context?.getString(R.string.zt_command_key)
    }

    override fun onClick(view: View?, context: Context?) {
        val termuxActivity: TermuxActivity = context as TermuxActivity
        val mingLShowDialog = MingLShowDialog(context!!)
        mingLShowDialog.mTitleCard.visibility = View.GONE
        mingLShowDialog.mSwitchCard.visibility = View.GONE
        mingLShowDialog.edit_text.hint = UUtils.getString(R.string.zt_command_edit_h)
        val dataMessageFileString = termuxActivity.getProperties()
            .getInternalPropertyValue(TermuxPropertyConstants.KEY_EXTRA_KEYS, true) as String
        if (!(dataMessageFileString.isNullOrEmpty())) {
            mingLShowDialog.edit_text.setText(dataMessageFileString)
        }
        mingLShowDialog.def_commit_ll.visibility = View.VISIBLE
        mingLShowDialog.def_commit_ll.setOnClickListener {
            // 恢复默认值
            val properties = getProperties()
            if (properties != null) {
                LogUtils.i(TAG, "setDefKey: $properties")
                properties.setProperty(KEY_EXTRA_KEYS, DEFAULT_IVALUE_EXTRA_KEYS)
                setProperties(properties)
                reload(context)
                UUtils.showMsg(UUtils.getString(R.string.zt_command_path_ok))
                mingLShowDialog.dismiss()
            } else {
                UUtils.showMsg(UUtils.getString(R.string.zt_command_path_error))
            }

        }
        mingLShowDialog.start.setOnClickListener {
            val text = mingLShowDialog.edit_text.text
            setKey(mingLShowDialog, termuxActivity, text.toString())
        }
        mingLShowDialog.show()
    }

    private fun setKey(
        mingLShowDialog: MingLShowDialog,
        termuxActivity: TermuxActivity,
        propertiesInfo: String
    ) {
        try {
            val arr: JSONArray = JSONArray(propertiesInfo)
            val matrix = arrayOfNulls<Array<Any?>>(arr.length())
            for (i in 0..<arr.length()) {
                val line = arr.getJSONArray(i)
                matrix[i] = arrayOfNulls<Any>(line.length())
                for (j in 0..<line.length()) {
                    matrix[i]!![j] = line.get(j)
                }
            }
            val properties = getProperties()
            if (properties == null) {
                UUtils.showMsg(UUtils.getString(R.string.zt_command_path_error))
                return
            }
            LogUtils.i(TAG, "setKey: $propertiesInfo")
            properties.setProperty(KEY_EXTRA_KEYS, propertiesInfo)
            setProperties(properties)
            mingLShowDialog.dismiss()
            UUtils.showMsg(UUtils.getString(R.string.zt_command_path_ok))
            reload(termuxActivity)
        } catch (e: Exception) {
            e.printStackTrace()
            UUtils.showMsg(UUtils.getString(R.string.zt_command_key_error))
        }
    }

    private fun setProperties(properties: Properties) {
        FileOutputStream(TermuxConstants.TERMUX_PROPERTIES_PRIMARY_FILE_PATH).use { output ->
            properties.store(output, "Application Configuration")
        }
    }

    private fun getProperties(): Properties? {
        try {
            val properties = Properties()

            // 从文件读取
            FileInputStream(TermuxConstants.TERMUX_PROPERTIES_PRIMARY_FILE_PATH).use { input ->
                properties.load(input)
            }
            val key = properties.getProperty(KEY_EXTRA_KEYS)
            LogUtils.i(TAG, "getProperties: $key")
            return properties
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun reload(context: Context) {
        // Make sure that terminal styling is always applied.
        val stylingIntent = Intent(ACTION_RELOAD_STYLE)
        stylingIntent.putExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, false)
        context.sendBroadcast(stylingIntent)
    }
}
