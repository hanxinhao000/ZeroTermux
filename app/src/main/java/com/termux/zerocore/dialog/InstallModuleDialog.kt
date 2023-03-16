package com.termux.zerocore.dialog

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.back.bean.DataBean
import com.termux.zerocore.back.listener.RestoreFileDataListener
import com.termux.zerocore.dialog.adapter.ModuleAdapter
import com.termux.zerocore.utils.FileIOUtils
import com.termux.zerocore.utils.ModuleInstallUtils
import com.termux.zerocore.utils.Z7ExtracatUtils
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class InstallModuleDialog: BaseDialogCentre {
    private var TAG = "InstallModuleDialog"
    private var mInstallEmpty: TextView? = null
    private var mRecyclerView: RecyclerView? = null
    private var mInstallLl: LinearLayout? = null
    private var mConsoleRl: RelativeLayout? = null
    private var mInstallModule: TextView? = null
    private var mConsoleText: TextView? = null
    private var mScrollView: ScrollView? = null
    private var mClose: ImageView? = null
    private var mOk: TextView? = null
    private var mThread: Thread? = null
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View?) {
        mView?.let {
            mInstallEmpty = it.findViewById(R.id.install_empty)
            mRecyclerView = it.findViewById(R.id.recycler_view)
            mInstallLl = it.findViewById(R.id.install_ll)
            mConsoleRl = it.findViewById(R.id.console_rl)
            mInstallModule = it.findViewById(R.id.install_module)
            mConsoleText = it.findViewById(R.id.console_text)
            mScrollView = it.findViewById(R.id.scrollView)
            mClose = it.findViewById(R.id.close)
            mOk = it.findViewById(R.id.ok)
            mOk?.setOnClickListener {
                dismiss()
            }
            mClose?.setOnClickListener {
                mThread?.interrupt()
                dismiss()
            }
        }
        initDataAndAdapter()

    }

    override fun getContentView(): Int {
       return R.layout.dialog_install_commands
    }

    public fun initDataAndAdapter() {
        val moduleFiles = FileIOUtils.getModuleFiles()
        if (moduleFiles == null || moduleFiles.isEmpty()) {
            mInstallEmpty?.visibility = View.VISIBLE
            mRecyclerView?.visibility = View.GONE
            return
        }
        mInstallEmpty?.visibility = View.GONE
        mRecyclerView?.visibility = View.VISIBLE

        val arrayList = ArrayList<DataBean>()
        moduleFiles.forEach {
            val dataBean = DataBean()
            dataBean.mFile = it
            arrayList.add(dataBean)
        }
        val moduleAdapter = ModuleAdapter(arrayList, mContext)
        mRecyclerView?.layoutManager = LinearLayoutManager(UUtils.getContext())
        mRecyclerView?.adapter = moduleAdapter
        moduleAdapter.setRestoreFileDataListener(object: RestoreFileDataListener {
            override fun file(mDataBean: DataBean) {
                mInstallLl?.visibility = View.INVISIBLE
                mConsoleRl?.visibility = View.VISIBLE

                    Z7ExtracatUtils.setUnZipCallBack(object : Z7ExtracatUtils.UnZipCallBack {
                        override fun onStart() {
                            MainScope().launch(Dispatchers.Main) {
                               mConsoleText?.text = "${mConsoleText?.text}\n${UUtils.getString(R.string.install_module_msg1)}\n${UUtils.getString(R.string.install_module_msg2)}"
                               mScrollView!!.post{mScrollView!!.fullScroll(View.FOCUS_DOWN)}
                            }
                        }

                        override fun onGetFileNum(fileNum: Int) {

                        }

                        override fun onProgress(name: String?, size: Long) {
                            MainScope().launch(Dispatchers.Main) {
                                mConsoleText?.text = "${mConsoleText?.text}\n${name}"
                                mScrollView!!.post{mScrollView!!.fullScroll(View.FOCUS_DOWN)}
                            }
                        }

                        override fun onError(errorCode: Int, message: String?) {
                            MainScope().launch(Dispatchers.Main) {
                                mConsoleText?.text = "${mConsoleText?.text}\n${UUtils.getString(R.string.install_module_msg3)}:${message},$errorCode"
                                mScrollView!!.post{mScrollView!!.fullScroll(View.FOCUS_DOWN)}
                            }
                        }

                        override fun onSucceed() {
                            mClose?.visibility = View.VISIBLE
                            mConsoleText?.text = "${mConsoleText?.text}\n${UUtils.getString(R.string.install_module_msg4)}"
                            mConsoleText?.text = "${mConsoleText?.text}\n\n\n\n"
                            mScrollView?.fullScroll(ScrollView.FOCUS_DOWN)
                            mThread =  Thread {
                                ModuleInstallUtils.installModule(object : ModuleInstallUtils.InstallModuleMsg {
                                    override fun msg(msg: String, isInstallEnd: Boolean,  mThrowable: Throwable?) {
                                        UUtils.getHandler().post {
                                            if (isInstallEnd) {
                                                mOk?.visibility = View.VISIBLE
                                            } else {
                                                mOk?.visibility = View.GONE
                                            }
                                            mConsoleText?.text = "${mConsoleText?.text}\n$msg"
                                            mScrollView!!.post{mScrollView!!.fullScroll(View.FOCUS_DOWN)}
                                        }
                                    }
                                })
                            }
                            mThread?.start()
                        }

                    })
                    ModuleInstallUtils.unZipModule(mDataBean.mFile!!)
                }
        })
    }


    data class CallBackBean(val msg: String, val isInstallEnd: Boolean)

    suspend fun setInstallModuleMsg() = suspendCoroutine {
       continuation ->  ModuleInstallUtils.installModule(object : ModuleInstallUtils.InstallModuleMsg {
        override fun msg(msg: String, isInstallEnd: Boolean,  mThrowable: Throwable?) {
            continuation.resume(CallBackBean(msg, isInstallEnd))
            if (mThrowable != null)
                continuation.resumeWithException(mThrowable)
        }
    })
    }


}
