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
import com.termux.zerocore.editor.EditorHelloProjectType

abstract class CreateEditorProjectClickConfig(
    private val projectType: EditorHelloProjectType,
    private val iconResId: Int,
    private val labelResId: Int
) : BaseMenuClickConfig() {

    override fun getType(): Int = MainMenuConfig.CODE_CREATE_PROJECT

    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(iconResId)
    }

    override fun getString(context: Context?): String? {
        return context?.getString(labelResId)
    }

    override fun onClick(view: View?, context: Context?) {
        val ctx = context ?: return
        val entryFile = EditorHelloProjectCreator.createFromMainMenu(projectType)
        if (entryFile == null) {
            UUtils.showMsg(UUtils.getString(R.string.editor_sidebar_create_failed))
            return
        }
        UUtils.showMsg(ctx.getString(R.string.editor_sidebar_project_created, entryFile.parentFile?.name.orEmpty()))
        val intent = Intent(ctx, EditTextActivity::class.java)
        intent.putExtra("edit_path", entryFile.absolutePath)
        ctx.startActivity(intent)
    }
}
