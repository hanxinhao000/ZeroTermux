package com.zp.z_file.async

import android.content.Context
import com.zp.z_file.content.ZFileBean
import com.zp.z_file.content.getZFileHelp

internal class ZFileListAsync(
    context: Context,
    block: MutableList<ZFileBean>?.() -> Unit
) : ZFileAsync(context, block) {

    override fun doingWork(filePath: String?) =
        getZFileHelp().getFileLoadListener().getFileList(getContext(), filePath)

}