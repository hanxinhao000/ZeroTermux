package com.termux.zerocore.config.mainmenu.config

import com.termux.R
import com.termux.zerocore.editor.EditorHelloProjectType

class CreatePhpProjectClickConfig : CreateEditorProjectClickConfig(
    EditorHelloProjectType.PHP,
    R.drawable.ic_project_php,
    R.string.menu_create_project_php
)
