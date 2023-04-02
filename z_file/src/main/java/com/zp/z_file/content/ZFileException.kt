package com.zp.z_file.content

import java.lang.RuntimeException

internal class ZFileException(msg: String) : RuntimeException(msg) {

    companion object {

        fun throwConfigurationError(title: String) {
            throw ZFileException("ZFileConfiguration $title error")
        }

    }

}