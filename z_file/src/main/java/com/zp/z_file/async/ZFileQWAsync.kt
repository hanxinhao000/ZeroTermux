package com.zp.z_file.async

import android.content.Context
import com.zp.z_file.content.*
import com.zp.z_file.util.ZFileQWUtil

/**
 * 获取 QQ 或 WeChat 文件 （QQ WeChat ---> QW）
 * @property fileType           QQ 或 WeChat
 * @property type               文件类型
 * @property filePathArray      过滤规则
 */
internal class ZFileQWAsync(
    private var fileType: String,
    private var type: Int,
    context: Context,
    block: MutableList<ZFileBean>?.() -> Unit
) : ZFileAsync(context, block) {

    private var filePathArray = arrayListOf<String>()

    /**
     * 执行前调用 mainThread
     */
    override fun onPreExecute() {
        val loadListener = getZFileHelp().getQWFileLoadListener()
        val array = loadListener?.getQWFilePathArray(fileType, type)
        val list = if (array.isNullOrEmpty()) {
            getQWFilePathArray()
        } else {
            array
        }
        filePathArray.addAll(list)
    }

    /**
     * 获取数据
     * @param filterArray  过滤规则
     */
    override fun doingWork(filterArray: Array<String>): MutableList<ZFileBean> {
        val loadListener = getZFileHelp().getQWFileLoadListener()
        return loadListener?.getQWFileDatas(type, filePathArray, filterArray)
                ?: ZFileQWUtil.getQWFileData(type, filePathArray, filterArray)
    }

    /**
     * 完成后调用 mainThread
     */
    override fun onPostExecute() {
        filePathArray.clear()
    }

    /**
     * 获取 QQ 或 WeChat 文件路径
     */
    private fun getQWFilePathArray() = if (fileType == ZFileConfiguration.QQ) {
        ZFileQWUtil.getQQFilePathMap()[type]!!
    } else {
        ZFileQWUtil.getWechatFilePathMap()[type]!!
    }

}