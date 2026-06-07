package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.activity.EditTextActivity
import com.termux.zerocore.config.mainmenu.MainMenuConfig
import com.termux.zerocore.editor.EditorHelloProjectCreator

class OpenProjectClickConfig : BaseMenuClickConfig() {

    override fun getType(): Int = MainMenuConfig.CODE_CREATE_PROJECT

    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.drawable.ic_project_open)
    }

    override fun getString(context: Context?): String? {
        return context?.getString(R.string.menu_open_project)
    }

    override fun onClick(view: View?, context: Context?) {
        val ctx = context ?: return
        val projectDir = EditorHelloProjectCreator.ensureProjectRoot()
        if (projectDir == null) {
            UUtils.showMsg(UUtils.getString(R.string.editor_sidebar_create_failed))
            return
        }
        val intent = Intent(ctx, EditTextActivity::class.java)
        intent.putExtra("edit_path", projectDir.absolutePath)
        ctx.startActivity(intent)
    }
}
