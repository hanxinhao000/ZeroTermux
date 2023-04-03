package com.zp.z_file.zerotermux

object ZTConfig {
    private var mCallBackListenerAll: CallBackListener? = null
    private var mStartTarGzListenerAll: StartTarGzListener? = null
    private var mCloseListenerAll: CloseListener? = null
    private var mZ7ListenerALL: Z7Listener? = null

    fun getZ7Listener(): Z7Listener? {
        return mZ7ListenerALL
    }

    fun setZ7Listener(mZ7Listener: Z7Listener) {
        mZ7ListenerALL = mZ7Listener
    }

    fun getCloseListener(): CloseListener? {
        return mCloseListenerAll
    }
    fun setCloseListener(mCloseListener: CloseListener?) {
        mCloseListenerAll = mCloseListener
    }

    fun getCallBackListener(): CallBackListener?{
        return mCallBackListenerAll
    }

    fun setCallBackListener(mCallBackListener: CallBackListener?) {
        mCallBackListenerAll = mCallBackListener
    }

    fun getStartTarGzListenerAll(mStartTarGzListener: StartTarGzListener?): StartTarGzListener? {
        return mStartTarGzListenerAll
    }

    fun setStartTarGzListenerAll(mStartTarGzListener: StartTarGzListener?) {
        mStartTarGzListenerAll = mStartTarGzListener
    }
}
