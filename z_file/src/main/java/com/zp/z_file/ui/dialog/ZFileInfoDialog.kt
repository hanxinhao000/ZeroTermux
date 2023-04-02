package com.zp.z_file.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zp.z_file.R
import com.zp.z_file.common.ZFileManageDialog
import com.zp.z_file.common.ZFileType
import com.zp.z_file.common.ZFileTypeManage
import com.zp.z_file.content.ZFileBean
import com.zp.z_file.content.ZFileInfoBean
import com.zp.z_file.content.setNeedWH
import com.zp.z_file.databinding.DialogZfileInfoBinding
import com.zp.z_file.type.ZFileAudioType
import com.zp.z_file.type.ZFileImageType
import com.zp.z_file.type.ZFileVideoType
import com.zp.z_file.util.ZFileOtherUtil
import java.lang.ref.WeakReference

internal class ZFileInfoDialog : ZFileManageDialog(), Runnable {

    companion object {
        fun newInstance(bean: ZFileBean) = ZFileInfoDialog().apply {
            arguments = Bundle().apply { putParcelable("fileBean", bean) }
        }
    }

    private var vb: DialogZfileInfoBinding? = null

    private var handler: InfoHandler? = null
    private lateinit var thread: Thread
    private var filePath = ""
    private lateinit var fileType: ZFileType

    override fun create(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        vb = DialogZfileInfoBinding.inflate(inflater, container, false)
        return vb?.root
    }

    override fun getContentView() = R.layout.dialog_zfile_info

    override fun createDialog(savedInstanceState: Bundle?) = Dialog(context!!, R.style.ZFile_Common_Dialog).apply {
        window?.setGravity(Gravity.CENTER)
    }

    override fun init(savedInstanceState: Bundle?) {
        val bean = arguments?.getParcelable("fileBean") ?: ZFileBean()
        filePath = bean.filePath
        fileType = ZFileTypeManage.getTypeManager().getFileType(bean.filePath)
        handler = InfoHandler(this)
        thread = Thread(this)
        thread.start()

        vb?.zfileDialogInfoFileName?.text = bean.fileName
        vb?.zfileDialogInfoFileType?.text = bean.filePath.run {
            substring(lastIndexOf(".") + 1, length)
        }
        vb?.zfileDialogInfoFileDate?.text = bean.date
        vb?.zfileDialogInfoFileSize?.text = bean.size
        vb?.zfileDialogInfoFilePath?.text = bean.filePath

        vb?.zfileDialogInfoMoreBox?.setOnClickListener {
            vb?.zfileDialogInfoMoreLayout?.visibility = if (vb?.zfileDialogInfoMoreBox?.isChecked == true) View.VISIBLE
            else View.GONE
        }
        when (fileType) {
            is ZFileImageType -> {
                vb?.zfileDialogInfoMoreBox?.visibility = View.VISIBLE
                vb?.zfileDialogInfoFileDurationLayout?.visibility = View.GONE
                vb?.zfileDialogInfoFileOther?.text = "无"
                val wh = ZFileOtherUtil.getImageWH(filePath)
                vb?.zfileDialogInfoFileFBL?.text = String.format("%d * %d", wh[0], wh[1])
            }
            is ZFileAudioType -> {
                vb?.zfileDialogInfoMoreBox?.visibility = View.VISIBLE
                vb?.zfileDialogInfoFileFBLLayout?.visibility = View.GONE
                vb?.zfileDialogInfoFileOther?.text = "无"
            }
            is ZFileVideoType -> {
                vb?.zfileDialogInfoMoreBox?.visibility = View.VISIBLE
                vb?.zfileDialogInfoFileOther?.text = "无"
            }
            else -> {
                vb?.zfileDialogInfoMoreBox?.visibility = View.GONE
                vb?.zfileDialogInfoMoreLayout?.visibility = View.GONE
            }
        }
        vb?.zfileDialogInfoDown?.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        setNeedWH()
    }

    override fun onDestroyView() {
        vb = null
        super.onDestroyView()
        handler?.removeMessages(0)
        handler?.removeCallbacks(this)
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    override fun run() {
        if (fileType !is ZFileAudioType && fileType !is ZFileVideoType) return
        handler?.sendMessage(Message().apply {
            what = 0
            obj = ZFileOtherUtil.getMultimediaInfo(filePath, fileType is ZFileVideoType)
        })
    }

    class InfoHandler(dialog: ZFileInfoDialog) : Handler() {
        private val week: WeakReference<ZFileInfoDialog> by lazy {
            WeakReference<ZFileInfoDialog>(dialog)
        }

        override fun handleMessage(msg: Message) {
            if (msg.what == 0) {
                val bean = msg.obj as ZFileInfoBean
                week.get()?.apply {
                    when (fileType) {
                        is ZFileAudioType -> {
                            vb?.zfileDialogInfoFileDuration?.text = bean.duration
                        }
                        is ZFileVideoType -> {
                            vb?.zfileDialogInfoFileDuration?.text = bean.duration
                            vb?.zfileDialogInfoFileFBL?.text =
                                String.format("%s * %s", bean.width, bean.height)
                        }
                    }
                }
            }
        }
    }
}