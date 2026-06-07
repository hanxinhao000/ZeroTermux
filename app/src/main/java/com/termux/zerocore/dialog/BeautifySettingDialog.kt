package com.termux.zerocore.dialog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.xh_lib.utils.SaveData
import com.example.xh_lib.utils.UUtils
import com.rtugeek.android.colorseekbar.ColorSeekBar
import com.termux.R
import com.termux.shared.logger.Logger
import com.termux.zerocore.activity.ImageActivity
import com.termux.zerocore.data.UsbFileData
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.FileIOUtils
import java.io.File
import java.io.FileInputStream

class BeautifySettingDialog : BaseDialogDown {
    private var mColorFont: ColorSeekBar? = null
    private var img_rl: RelativeLayout? = null
    private var back_color: ColorSeekBar? = null
    private var mFontColorChange: FontColorChange? = null
    private var mBackColorChange:BackColorChange? = null
    private var mOnChangeTextView:OnChangeTextView? = null
    private var def_tv:TextView? = null
    private var show1:ImageView? = null
    private var show2:ImageView? = null
    private var mOnTextCheckedChangeListener:OnTextCheckedChangeListener? = null
    private var mOnMenuBackListener:OnMenuBackListener? = null
    private var mOnChangeImageFile:OnChangeImageFile? = null
    private var mBackApSeekBar: SeekBar? = null
    private var back_text_show_switch: Switch? = null
    private var mBackMenuVisible: Switch? = null
    private var mBlurSwitch: Switch? = null
    private var mBlurSeekBar: SeekBar? = null
    private var mBlurLabel: TextView? = null
    private var mTextShadowSeekBar: SeekBar? = null
    private var mTextShadowSwitch: Switch? = null

    private var fontColor:Int = Color.parseColor("#ffffff")
    private var fontColorProgress:Int = 0
    private val LOG_TAG = "Termux--Apk:BeautifySettingDialog"

    private var mOnBlurChangeListener: OnBlurChangeListener? = null
    private var mOnTextShadowChangeListener: OnTextShadowChangeListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {
        mColorFont = mView.findViewById(R.id.font_color)
        back_color = mView.findViewById(R.id.back_color)
        mBackMenuVisible = mView.findViewById(R.id.back_menu_visible)
        show1 = mView.findViewById(R.id.show1)
        show2 = mView.findViewById(R.id.show2)
        img_rl = mView.findViewById(R.id.img_rl)
        def_tv = mView.findViewById(R.id.def_tv)
        mBackApSeekBar = mView.findViewById(R.id.back_ap_seekbar)
        back_text_show_switch = mView.findViewById(R.id.back_text_show_switch)
        mBlurSwitch = mView.findViewById(R.id.blur_switch)
        mBlurSeekBar = mView.findViewById(R.id.blur_seekbar)
        mBlurLabel = mView.findViewById(R.id.blur_label)
        mTextShadowSeekBar = mView.findViewById(R.id.text_shadow_seekbar)
        mTextShadowSwitch = mView.findViewById(R.id.text_shadow_switch)
        setColorAll()
        initProgress()
    }

    private fun initProgress(){
        val stringOther = SaveData.getStringOther("font_color_progress")
        val back_color_progress = SaveData.getStringOther("back_color_progress")
        val change_text = SaveData.getStringOther("change_text")
        val change_text_show = SaveData.getStringOther("change_text_show")

        val blurEnabled = SaveData.getStringOther("blur_enabled")
        val blurRadius = SaveData.getStringOther("blur_radius")
        val textShadowStrength = SaveData.getStringOther("text_shadow_strength")
        val textShadowEnabled = SaveData.getStringOther("text_shadow_enabled")

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
        if (!(change_text == null || change_text.isEmpty() || change_text == "def")) {
            try {
                mBackApSeekBar?.progress = change_text.toInt()
            } catch (e: Exception) { }
        }
        back_text_show_switch?.isChecked = (change_text_show == null || change_text_show.isEmpty() || change_text_show == "def")
        mBackMenuVisible?.isChecked = UserSetManage.get().getZTUserBean().isBackMenuVisible

        val blurOn = blurEnabled != null && blurEnabled == "true"
        mBlurSwitch?.isChecked = blurOn
        mBlurSeekBar?.visibility = if (blurOn) View.VISIBLE else View.GONE
        mBlurLabel?.visibility = if (blurOn) View.VISIBLE else View.GONE
        if (!(blurRadius == null || blurRadius.isEmpty() || blurRadius == "def")) {
            try {
                mBlurSeekBar?.progress = blurRadius.toInt()
            } catch (e: Exception) { }
        }
        if (!(textShadowStrength == null || textShadowStrength.isEmpty() || textShadowStrength == "def")) {
            try {
                mTextShadowSeekBar?.progress = textShadowStrength.toInt()
            } catch (e: Exception) { }
        }
        val textShadowOn = textShadowEnabled != "false"
        mTextShadowSwitch?.isChecked = textShadowOn
        mTextShadowSeekBar?.isEnabled = textShadowOn

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

        back_color?.setOnColorChangeListener { progress, color ->

            mBackColorChange?.onColorChange(color)

            SaveData.saveStringOther("back_color","$color")
            SaveData.saveStringOther("back_color_progress","$progress")

        }

        mBackApSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    Logger.logDebug(LOG_TAG, "back_ap_alpha:$progress")
                    SaveData.saveStringOther("change_text", "$progress")
                    mOnChangeTextView?.onChange(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        back_text_show_switch?.setOnCheckedChangeListener { compoundButton, b ->
            mOnTextCheckedChangeListener?.onChange(b)
            if(b){
                SaveData.saveStringOther("change_text_show","def")
            }else{
                SaveData.saveStringOther("change_text_show","$b")
            }

        }

        mBackMenuVisible?.setOnCheckedChangeListener { button, bool ->
            val ztUserBean = UserSetManage.get().getZTUserBean()
            ztUserBean.setIsBackMenuVisible(bool)
            UserSetManage.get().setZTUserBean(ztUserBean)
            mOnMenuBackListener?.onChange()
        }

        img_rl?.setOnClickListener {
            val intent = Intent(mContext as Activity, ImageActivity::class.java)
            intent.action = ImageActivity.ImageActivityFlgh.IMAGE_FLGH
            mContext.startActivity(intent)
        }
        def_tv?.setOnClickListener {


        }

        mBlurSwitch?.setOnCheckedChangeListener { _, isChecked ->
            mBlurSeekBar?.visibility = if (isChecked) View.VISIBLE else View.GONE
            mBlurLabel?.visibility = if (isChecked) View.VISIBLE else View.GONE
            SaveData.saveStringOther("blur_enabled", if (isChecked) "true" else "false")
            mOnBlurChangeListener?.onBlurChanged(
                if (isChecked) mBlurSeekBar?.progress ?: 10 else 0
            )
        }

        mBlurSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mBlurSwitch?.isChecked == true) {
                    SaveData.saveStringOther("blur_radius", "$progress")
                    mOnBlurChangeListener?.onBlurChanged(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mTextShadowSwitch?.setOnCheckedChangeListener { _, isChecked ->
            mTextShadowSeekBar?.isEnabled = isChecked
            SaveData.saveStringOther("text_shadow_enabled", if (isChecked) "true" else "false")
            mOnTextShadowChangeListener?.onTextShadowChanged(
                if (isChecked) mTextShadowSeekBar?.progress ?: 50 else 0
            )
        }

        mTextShadowSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mTextShadowSwitch?.isChecked == true) {
                    SaveData.saveStringOther("text_shadow_strength", "$progress")
                    mOnTextShadowChangeListener?.onTextShadowChanged(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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

    public fun setOnMenuBackListener(onMenuBackListener: OnMenuBackListener) {
        this.mOnMenuBackListener = onMenuBackListener
    }
    public fun setOnChangeImageFile(mOnChangeImageFile:OnChangeImageFile){
        this.mOnChangeImageFile = mOnChangeImageFile
    }

    public fun setOnBlurChangeListener(listener: OnBlurChangeListener) {
        mOnBlurChangeListener = listener
    }

    public fun setOnTextShadowChangeListener(listener: OnTextShadowChangeListener) {
        mOnTextShadowChangeListener = listener
    }

    public interface OnTextShadowChangeListener {
        fun onTextShadowChanged(strength: Int)
    }

    public interface FontColorChange{
        fun onColorChange(color:Int)
    }

    public interface BackColorChange{
        fun onColorChange(color:Int)
    }

    public interface OnChangeTextView{
        fun onChange(alpha: Int)
    }

    public interface OnChangeImageFile{
        fun onChangImage(mFile:File)
    }

    public interface OnTextCheckedChangeListener{
        fun onChange(change:Boolean)
    }

    public interface OnBlurChangeListener {
        fun onBlurChanged(radius: Int)
    }

    override fun getContentView(): Int {
        return R.layout.dialog_beauify
    }

    public interface OnMenuBackListener {
        fun onChange()
    }

}
