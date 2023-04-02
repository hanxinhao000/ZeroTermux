package com.zp.z_file.async

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.zp.z_file.content.ZFileBean
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

/**
 * 更方便的去获取符合要求的数据
 */
open class ZFileAsync(
    private var context: Context,
    private var block: MutableList<ZFileBean>?.() -> Unit
) {

    private val LIST: Int
        get() = 21

    private val OTHER: Int
        get() = 20

    private var handler: ZFileAsyncHandler? = null

    private val softReference by lazy {
        SoftReference<Context>(context)
    }

    /**
     * 获取数据
     * @param filterArray 过滤规则
     */
    fun start(filterArray: Array<String>) {
        doStart()
        thread { sendMessage(OTHER, doingWork(filterArray)) }
    }

    protected fun getContext(): Context? = softReference.get()

    /**
     * 执行前调用 mainThread
     */
    protected open fun onPreExecute() = Unit

    /**
     * 获取数据
     * @param filterArray  过滤规则
     */
    protected open fun doingWork(filterArray: Array<String>): MutableList<ZFileBean>? {
        return null
    }

    private fun onPostExecute(list: MutableList<ZFileBean>?) {
        handler?.removeMessages(LIST)
        handler?.removeMessages(OTHER)
        handler?.removeCallbacksAndMessages(null)
        handler = null
        block.invoke(list)
        onPostExecute()
    }

    /**
     * 完成后调用 mainThread
     */
    protected open fun onPostExecute() = Unit

    private class ZFileAsyncHandler(zFileAsync: ZFileAsync) : Handler(Looper.myLooper()!!) {

        private val weakReference by lazy {
            WeakReference<ZFileAsync>(zFileAsync)
        }

        @Suppress("UNCHECKED_CAST")
        override fun handleMessage(msg: Message) {
            val list = msg.obj as? MutableList<ZFileBean>
            /*when (msg.what) {
                weakReference.get()?.OTHER -> {}
                weakReference.get()?.LIST -> {}
            }*/
            weakReference.get()?.onPostExecute(list)
        }
    }

    private fun doStart() {
        if (handler == null) {
            handler = ZFileAsyncHandler(this)
        }
        onPreExecute()
    }

    private fun sendMessage(messageWhat: Int, messageObj: Any?) {
        handler?.sendMessage(Message.obtain().apply {
            what = messageWhat
            obj = messageObj
        })
    }

    // =============================================================================================

    internal fun start(filePath: String?) {
        doStart()
        thread { sendMessage(LIST, doingWork(filePath)) }
    }

    internal open fun doingWork(filePath: String?): MutableList<ZFileBean>? {
        return null
    }

}