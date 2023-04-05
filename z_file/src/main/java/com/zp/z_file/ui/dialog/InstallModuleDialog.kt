package com.zp.z_file.ui.dialog

import android.content.Context
import android.os.Environment
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zp.z_file.listener.RestoreFileDataListener
import com.zp.z_file.listener.RestoreRefreshFileListener
import com.zp.z_file.R
import com.zp.z_file.bean.DataBean
import com.zp.z_file.ui.adapter.ModuleAdapter
import com.zp.z_file.util.LogUtils
import com.zp.z_file.util.ModuleInstallUtils
import com.zp.z_file.util.Z7ExtracatUtils
import com.zp.z_file.util.ZFileUUtils
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class InstallModuleDialog: BaseDialogCentre {
    private var TAG = "InstallModuleDialog"
    private var mInstallEmpty: TextView? = null
    private var download_module: TextView? = null
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
    //模块包目录
    public var zeroTermuxModule: File? = null


    override fun initViewDialog(mView: View?) {

        mView?.let {
            mInstallEmpty = it.findViewById(R.id.install_empty)
            mRecyclerView = it.findViewById(R.id.recycler_view)
            download_module = it.findViewById(R.id.download_module)
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
            download_module?.setOnClickListener {
                ZFileUUtils.startUrl("https://od.ixcmstudio.cn/repository/main/module/")
            }

            mClose?.setOnClickListener {
                mThread?.interrupt()
                dismiss()
            }
        }
        zeroTermuxModule = File(Environment.getExternalStorageDirectory(), "/xinhao/module")
        initDataAndAdapter()

    }

    override fun getContentView(): Int {
       return R.layout.dialog_install_commands
    }

    public fun initDataAndAdapter() {
        val moduleFiles = getModuleFiles()
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
        moduleAdapter.setRestoreRefreshFileListener(object : RestoreRefreshFileListener {
            override fun refresh() {
                val moduleFiles1 = getModuleFiles()
                val arrayList1 = ArrayList<DataBean>()
                if (moduleFiles1 == null || moduleFiles1.isEmpty()) {
                    mInstallEmpty?.visibility = View.VISIBLE
                    mRecyclerView?.visibility = View.GONE
                    return
                }
                moduleFiles1.let {
                    it.forEach {
                        val dataBean = DataBean()
                        dataBean.mFile = it
                        arrayList1.add(dataBean)
                    }
                    moduleAdapter.setList(arrayList1)
                    moduleAdapter.notifyDataSetChanged()
                }
            }
        })
        mRecyclerView?.layoutManager = LinearLayoutManager(ZFileUUtils.getContext())
        mRecyclerView?.adapter = moduleAdapter
        moduleAdapter.setRestoreFileDataListener(object: RestoreFileDataListener {
            override fun file(mDataBean: DataBean) {
                val switchDialog = SwitchDialog(mContext)
                switchDialog.createSwitchDialog(ZFileUUtils.getString(R.string.install_module_switch))
                switchDialog.show()
                switchDialog.cancel?.setOnClickListener {
                    switchDialog.dismiss()

                }
                switchDialog.ok?.setOnClickListener {
                    switchDialog.dismiss()
                    installModule(mDataBean)
                }

            }
        })
    }

    public fun installModule(mDataBean: DataBean) {
        mClose?.visibility = View.GONE
        mInstallLl?.visibility = View.INVISIBLE
        mConsoleRl?.visibility = View.VISIBLE

        var fileNumTemp: Int = 0
        var fileNumConnut: Int = 0
        val stringBuilder = StringBuilder()
        LogUtils.d(TAG, "file start install module...")
        Z7ExtracatUtils.setUnZipCallBack(object : Z7ExtracatUtils.UnZipCallBack {
            override fun onStart() {
                ZFileUUtils.getHandler().post {
                    stringBuilder.
                    append(mConsoleText?.text).
                    append("\n").
                    append(ZFileUUtils.getString(R.string.install_module_msg1)).
                    append(ZFileUUtils.getString(R.string.install_module_msg2))

                    mConsoleText?.text = stringBuilder.toString()
                    mScrollView!!.post{mScrollView!!.fullScroll(View.FOCUS_DOWN)}
                }
            }

            override fun onGetFileNum(fileNum: Int) {
                fileNumTemp = fileNum
            }

            override fun onProgress(name: String?, size: Long) {
                fileNumConnut++
                ZFileUUtils.getHandler().post {
                    mConsoleText?.text = "$stringBuilder\n${ZFileUUtils.getString(R.string.module_un7z_)}$fileNumConnut/$fileNumTemp"
                    // mScrollView!!.post{mScrollView!!.fullScroll(View.FOCUS_DOWN)}
                }
            }

            override fun onError(errorCode: Int, message: String?) {
                ZFileUUtils.getHandler().post {
                    mConsoleText?.text = "${mConsoleText?.text}\n${ZFileUUtils.getString(R.string.install_module_msg3)}:${message},$errorCode"
                    mScrollView!!.post{mScrollView!!.fullScroll(View.FOCUS_DOWN)}
                }
            }

            override fun onSucceed() {
                ZFileUUtils.getHandler().post {
                    // mClose?.visibility = View.VISIBLE
                    mConsoleText?.text = "${mConsoleText?.text}\n${ZFileUUtils.getString(R.string.install_module_msg4)}"
                    mConsoleText?.text = "${mConsoleText?.text}\n\n\n\n"
                    mScrollView?.fullScroll(ScrollView.FOCUS_DOWN)
                    mThread = Thread {
                        ModuleInstallUtils.installModule(object : ModuleInstallUtils.InstallModuleMsg {
                            override fun msg(msg: String, isInstallEnd: Boolean,  mThrowable: Throwable?) {
                                ZFileUUtils.getHandler().post {
                                    if (isInstallEnd) {
                                        mOk?.visibility = View.VISIBLE
                                    } else {
                                        mOk?.visibility = View.GONE
                                    }
                                    mConsoleText?.let {
                                        it.text = msg
                                    }
                                    mScrollView!!.post{mScrollView!!.fullScroll(View.FOCUS_DOWN)}
                                }
                            }
                        })
                    }
                    mThread!!.start()
                }
            }

        })
        LogUtils.d(TAG, "file start unZipModule")
        ModuleInstallUtils.unZipModule(mDataBean.mFile!!)
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


    //获取模块包路径
    private fun getModuleFiles(): ArrayList<File>? {
        if (!zeroTermuxModule!!.exists()) {
            if (!zeroTermuxModule!!.mkdirs()) {
                LogUtils.d(TAG, "getModuleFiles create folder is fail, path: " + zeroTermuxModule!!.absolutePath)
                return null
            }
        }
        val listFiles = zeroTermuxModule!!.listFiles()
        if (listFiles == null || listFiles.isEmpty()) {
            LogUtils.d(TAG, "getModuleFiles module listFiles is empty ")
            return null
        }
        val arrayList = ArrayList<File>()
        arrayList.addAll(listFiles)
        return arrayList
    }

}
