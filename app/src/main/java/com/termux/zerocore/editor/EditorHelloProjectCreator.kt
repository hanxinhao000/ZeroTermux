package com.termux.zerocore.editor

import com.example.xh_lib.utils.UUtils
import com.termux.shared.termux.TermuxConstants
import java.io.File

enum class EditorHelloProjectType {
    C,
    JAVA,
    PYTHON,
    PHP,
    NPM
}

object EditorHelloProjectCreator {

    const val MAIN_MENU_PROJECT_DIR = TermuxConstants.TERMUX_HOME_DIR_PATH + "/project"

    data class HelloProjectSpec(
        val dirBaseName: String,
        val entryFileName: String,
        val content: String,
        val extraFiles: Map<String, String> = emptyMap()
    )

    private val JAVA_HELLO_TEMPLATE = """
        public class Hello {
            public static void main(String[] args) {
                System.out.println("Hello, World!");
            }
        }
    """.trimIndent()

    private val C_HELLO_TEMPLATE = """
        #include <stdio.h>

        int main(void) {
            printf("Hello, World!\n");
            return 0;
        }
    """.trimIndent()

    private val PYTHON_HELLO_TEMPLATE = """
        #!/usr/bin/env python3

        def main():
            print("Hello, World!")


        if __name__ == "__main__":
            main()
    """.trimIndent()

    private val PHP_HELLO_TEMPLATE = """
        <?php

        function main(): void
        {
            echo "Hello, World!\n";
        }

        main();
    """.trimIndent()

    private val PHP_COMPOSER_JSON_TEMPLATE = """
        {
            "name": "hello/world",
            "description": "Hello World PHP project",
            "type": "project",
            "require": {}
        }
    """.trimIndent()

    private val NODE_HELLO_TEMPLATE = """
        #!/usr/bin/env node

        function main() {
            console.log('Hello, World!');
        }

        if (require.main === module) {
            main();
        }
    """.trimIndent()

    private val NODE_PACKAGE_JSON_TEMPLATE = """
        {
          "name": "hello",
          "version": "1.0.0",
          "description": "Hello World Node.js project",
          "main": "index.js",
          "scripts": {
            "start": "node index.js"
          }
        }
    """.trimIndent()

    fun specFor(type: EditorHelloProjectType): HelloProjectSpec {
        return when (type) {
            EditorHelloProjectType.JAVA -> HelloProjectSpec("project_java", "Hello.java", JAVA_HELLO_TEMPLATE)
            EditorHelloProjectType.C -> HelloProjectSpec("project_c", "hello.c", C_HELLO_TEMPLATE)
            EditorHelloProjectType.PYTHON -> HelloProjectSpec("project_python", "hello.py", PYTHON_HELLO_TEMPLATE)
            EditorHelloProjectType.PHP -> HelloProjectSpec(
                "project_php",
                "index.php",
                PHP_HELLO_TEMPLATE,
                mapOf("composer.json" to PHP_COMPOSER_JSON_TEMPLATE)
            )
            EditorHelloProjectType.NPM -> HelloProjectSpec(
                "project_node",
                "index.js",
                NODE_HELLO_TEMPLATE,
                mapOf("package.json" to NODE_PACKAGE_JSON_TEMPLATE)
            )
        }
    }

    fun ensureProjectRoot(): File? {
        val dir = File(MAIN_MENU_PROJECT_DIR)
        if (dir.exists()) {
            return dir.takeIf { it.isDirectory }
        }
        return dir.takeIf { it.mkdirs() && it.isDirectory }
    }

    fun createInDirectory(parentDir: File, type: EditorHelloProjectType): File? {
        if (!parentDir.isDirectory) return null
        return createInDirectory(parentDir, specFor(type))
    }

    fun createFromMainMenu(type: EditorHelloProjectType): File? {
        val parentDir = ensureProjectRoot() ?: return null
        return createInDirectory(parentDir, type)
    }

    fun createInDirectory(parentDir: File, spec: HelloProjectSpec): File? {
        if (!parentDir.isDirectory) return null
        val projectDir = allocateProjectDirectory(parentDir, spec.dirBaseName)
        if (!projectDir.mkdirs()) return null
        val targetFile = File(projectDir, spec.entryFileName)
        if (!UUtils.setFileString(targetFile, spec.content)) return null
        for ((relativePath, fileContent) in spec.extraFiles) {
            val extraFile = File(projectDir, relativePath)
            extraFile.parentFile?.mkdirs()
            if (!UUtils.setFileString(extraFile, fileContent)) return null
        }
        return targetFile
    }

    private fun allocateProjectDirectory(parent: File, baseName: String): File {
        val first = File(parent, baseName)
        if (!first.exists()) return first
        var index = 1
        while (true) {
            val candidate = File(parent, baseName + index)
            if (!candidate.exists()) return candidate
            index++
        }
    }
}
