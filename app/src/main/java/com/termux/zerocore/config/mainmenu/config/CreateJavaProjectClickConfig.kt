package com.termux.zerocore.config.mainmenu.config

import com.termux.R
import com.termux.zerocore.editor.EditorHelloProjectType

class CreateJavaProjectClickConfig : CreateEditorProjectClickConfig(
    EditorHelloProjectType.JAVA,
    R.drawable.ic_project_java,
    R.string.menu_create_project_java
)
