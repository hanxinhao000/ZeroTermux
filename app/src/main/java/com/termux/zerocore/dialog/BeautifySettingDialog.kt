package com.termux.zerocore.dialog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.xh_lib.utils.SaveData
import com.example.xh_lib.utils.UUtils
import com.rtugeek.android.colorseekbar.AlphaSeekBar
import com.rtugeek.android.colorseekbar.ColorSeekBar
import com.termux.R
import com.termux.shared.logger.Logger
import com.termux.zerocore.activity.ImageActivity
import com.termux.zerocore.data.UsbFileData
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.FileIOUtils
import java.io.File
import java.io.FileInputStream

class BeautifySettingDialog : BaseDialogDown {
    private var mColorFont: ColorSeekBar? = null
    private var mFontColorAp: AlphaSeekBar? = null
    private var img_rl: RelativeLayout? = null
    private var back_color: ColorSeekBar? = null
    private var back_color_ap: AlphaSeekBar? = null
    private var mFontColorChange: FontColorChange? = null
    private var mBackColorChange:BackColorChange? = null
    private var mOnChangeTextView:OnChangeTextView? = null
    private var def_tv:TextView? = null
    private var show1:ImageView? = null
    private var show2:ImageView? = null
    private var mOnTextCheckedChangeListener:OnTextCheckedChangeListener? = null
    private var mOnChangeImageFile:OnChangeImageFile? = null
    private var back_ap: Switch? = null
    private var back_text_show_switch: Switch? = null

    private val MAX_ALPHA:Int = 255
    private val MIN_ALPHA:Int = 40

    private var fontColor:Int = Color.parseColor("#ffffff")
    private var fontColorProgress:Int = 0

    private var fontApColor:Int = 0
    private var fontApColorProgress:Int = 0
    private val LOG_TAG = "Termux--Apk:BeautifySettingDialog"
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {
        mColorFont = mView.findViewById(R.id.font_color)
        back_color = mView.findViewById(R.id.back_color)
        mFontColorAp = mView.findViewById(R.id.font_color_ap)
        back_color_ap = mView.findViewById(R.id.back_color_ap)
        show1 = mView.findViewById(R.id.show1)
        show2 = mView.findViewById(R.id.show2)
        img_rl = mView.findViewById(R.id.img_rl)
        def_tv = mView.findViewById(R.id.def_tv)
        back_ap = mView.findViewById(R.id.back_ap)
        back_text_show_switch = mView.findViewById(R.id.back_text_show_switch)
        setColorAll()
        initProgress()
    }

    private fun initProgress(){
        val stringOther = SaveData.getStringOther("font_color_progress")
        val back_color_progress = SaveData.getStringOther("back_color_progress")
        val change_text = SaveData.getStringOther("change_text")
        val change_text_show = SaveData.getStringOther("change_text_show")
        if(!(stringOther == null || stringOther.isEmpty() || stringOther == "def")){
            try {
                val toInt = stringOther.toInt()
                mColorFont?.progress = toInt
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        if(!(back_color_progress == null || back_color_progress.isEmpty() || back_color_progress == "def")){
            try {
                val toInt = back_color_progress.toInt()
                back_color?.progress = toInt
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        back_ap?.isChecked = !(change_text == null || change_text.isEmpty() || change_text == "def")
        back_text_show_switch?.isChecked = (change_text_show == null || change_text_show.isEmpty() || change_text_show == "def")

        val fileImg = File("${FileUrl.mainConfigImg}/back.jpg")
        Log.e(LOG_TAG, "initProgress: check jpg exists: " + fileImg.exists())
        if(fileImg.exists()){
            Glide.with(mContext).load(fileImg).skipMemoryCache(true).diskCacheStrategy(
                DiskCacheStrategy.NONE).into(show2!!)
            show1?.visibility = View.INVISIBLE
            show2?.visibility = View.VISIBLE
        }

    }

    override fun dismiss() {
        super.dismiss()
    }
    private fun setColorAll(){


        mColorFont?.setOnColorChangeListener { progress, color ->

            fontColor = color
            fontColorProgress = progress
            mFontColorChange?.onColorChange(color)

            SaveData.saveStringOther("font_color","$color")
            SaveData.saveStringOther("font_color_progress","$progress")

        }

        mFontColorAp?.setOnAlphaChangeListener { progress, alpha ->

            fontApColor = alpha
            fontApColorProgress = progress
            mFontColorChange?.onColorApChange(UUtils.calculateAlphaValue(fontColor,alpha))

        }

        back_color?.setOnColorChangeListener { progress, color ->

            mBackColorChange?.onColorChange(color)

            SaveData.saveStringOther("back_color","$color")
            SaveData.saveStringOther("back_color_progress","$progress")

        }
        back_color_ap?.setOnAlphaChangeListener { progress, alpha ->
        }

        back_ap?.setOnCheckedChangeListener { compoundButton, b ->
            Logger.logDebug(LOG_TAG, "change:$b")
            mOnChangeTextView?.onChange(b)
            if(b){
                SaveData.saveStringOther("change_text","$b")
            }else{
                SaveData.saveStringOther("change_text","def")
            }
        }

        back_text_show_switch?.setOnCheckedChangeListener { compoundButton, b ->
            mOnTextCheckedChangeListener?.onChange(b)
            if(b){
                SaveData.saveStringOther("change_text_show","def")
            }else{
                SaveData.saveStringOther("change_text_show","$b")
            }

        }

        img_rl?.setOnClickListener {
            val intent = Intent(mContext as Activity, ImageActivity::class.java)
            intent.action = ImageActivity.ImageActivityFlgh.IMAGE_FLGH
            mContext.startActivity(intent)
        }
        def_tv?.setOnClickListener {


        }

        UsbFileData.get().setImageFileCheckListener(object :UsbFileData.ImageFileCheckListener{
            override fun imageFile(file: File) {
                Logger.logDebug(LOG_TAG, "file:${file.absolutePath}")

                val file1 = File(FileUrl.mainConfigImg)
                val fileImg = File("${FileUrl.mainConfigImg}/back.jpg")
                if(!file1.exists()){
                    file1.mkdirs()
                }
                if(fileImg.exists()){
                    fileImg.delete()
                }
                Logger.logDebug(LOG_TAG, "file1:${fileImg.absolutePath}")
                UUtils.runOnThread {
                    Log.e(LOG_TAG, "setImageFileCheckListener: start Writer")
                    UUtils.writerFileRawInput(fileImg,FileInputStream(file))
                    FileIOUtils.clearPathVideo()
                    Log.e(LOG_TAG, "setImageFileCheckListener: end Writer")
                    UUtils.runOnUIThread {
                        Log.e(LOG_TAG, "setImageFileCheckListener: setting Glide")
                        Glide.with(mContext).load(fileImg).skipMemoryCache(true).diskCacheStrategy(
                            DiskCacheStrategy.NONE).into(show2!!)
                        Log.e(LOG_TAG, "setImageFileCheckListener: setting Glide done")
                        show1?.visibility = View.INVISIBLE
                        show2?.visibility = View.VISIBLE
                        mOnChangeImageFile?.onChangImage(fileImg)
                    }
                }


            }

        })
    }

    public fun setFontColorChange(mFontColorChange:FontColorChange){
        this.mFontColorChange = mFontColorChange
    }

    public fun setBackColorChange(mBackColorChange:BackColorChange){
        this.mBackColorChange = mBackColorChange
    }

    public fun setOnChangeTextView(mOnChangeTextView:OnChangeTextView){
        this.mOnChangeTextView = mOnChangeTextView
    }

    public fun setOnTextCheckedChangeListener(mOnTextCheckedChangeListener:OnTextCheckedChangeListener){
        this.mOnTextCheckedChangeListener = mOnTextCheckedChangeListener
    }

    public fun setOnChangeImageFile(mOnChangeImageFile:OnChangeImageFile){
        this.mOnChangeImageFile = mOnChangeImageFile
    }

    public interface FontColorChange{

        fun onColorChange(color:Int)

        fun onColorApChange(ap:Int)

    }

    public interface BackColorChange{

        fun onColorChange(color:Int)

        fun onColorApChange(ap:Int)

    }

    public interface OnChangeTextView{

        fun onChange(change:Boolean)


    }

    public interface OnChangeImageFile{
        fun onChangImage(mFile:File)
    }

    public interface OnTextCheckedChangeListener{

        fun onChange(change:Boolean)


    }

    override fun getContentView(): Int {

        return R.layout.dialog_beauify
    }

}
