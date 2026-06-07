package com.termux.zerocore.config.mainmenu.config

import com.termux.R
import com.termux.zerocore.editor.EditorHelloProjectType

class CreateNpmProjectClickConfig : CreateEditorProjectClickConfig(
    EditorHelloProjectType.NPM,
    R.drawable.ic_project_npm,
    R.string.menu_create_project_npm
)
