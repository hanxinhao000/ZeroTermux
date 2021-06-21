package com.example.xh_lib.activity


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blockchain.ub.utils.httputils.BaseHttpUtils
import com.example.xh_lib.R
import com.example.xh_lib.statusBar.StatusBarCompat
import com.example.xh_lib.theme.DayNight
import com.example.xh_lib.theme.DayNightHelper
import com.example.xh_lib.utils.SaveData.getStringOther
import com.example.xh_lib.utils.UUtils


abstract class BaseThemeActivity() : BaseActivity() {


    private lateinit var mRightTv: TextView
    private lateinit var mThisView: View
    private lateinit var mLeftImg: ImageView
    private lateinit var mLeftImg_: ImageView
    private lateinit var mBaseTitle: TextView
    private lateinit var mRightRv1: TextView


    private lateinit var mImageViewRightLl: LinearLayout
    private lateinit var mImageRight1: ImageView
    private lateinit var mImageRight2: ImageView
    private lateinit var mTitleBaseRl: RelativeLayout


    private lateinit var mDayNightHelper: DayNightHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initData()
        initTheme()


        //这块判断
        val login_tk = getStringOther("login_tk")
        if (login_tk == null || login_tk.isEmpty() || login_tk == "def") {

        } else {

            BaseHttpUtils.token = login_tk
        }
    }


    open fun initData() {
        mDayNightHelper = DayNightHelper(this)
    }

    //---------------------------------------------------------------------

    private fun initTheme() {
        if (mDayNightHelper.isDay) {
            setTheme(R.style.DayTheme)
            StatusBarCompat.changeToLightStatusBar(this)
        } else {
            setTheme(R.style.NightTheme)
            //设置状态栏颜色
            //  StatusBarCompat.setStatusBarColor(this, ContextCompat.getColor(this, R.color.color_ffffff));
            //字体颜色
            //黑色
            StatusBarCompat.cancelLightStatusBar(this)

        }
    }


    private fun toggleThemeSetting() {
        if (mDayNightHelper.isDay) {
            mDayNightHelper.setMode(DayNight.NIGHT)
            setTheme(R.style.NightTheme)
            StatusBarCompat.cancelLightStatusBar(this)
        } else {
            mDayNightHelper.setMode(DayNight.DAY)
            setTheme(R.style.DayTheme)


            StatusBarCompat.changeToLightStatusBar(this)
        }
    }


    private fun showAnimation() {
        val decorView = window.decorView
        val cacheBitmap: Bitmap = getCacheBitmapFromView(decorView)!!
        if (decorView is ViewGroup && cacheBitmap != null) {
            val view = View(this)
            view.setBackgroundDrawable(BitmapDrawable(resources, cacheBitmap))
            val layoutParam = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            decorView.addView(view, layoutParam)
            val objectAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
            objectAnimator.duration = 300
            objectAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    decorView.removeView(view)
                }
            })
            objectAnimator.start()
        }
    }


    private fun getCacheBitmapFromView(view: View): Bitmap? {
        val drawingCacheEnabled = true
        view.isDrawingCacheEnabled = drawingCacheEnabled
        view.buildDrawingCache(drawingCacheEnabled)
        val drawingCache = view.drawingCache
        val bitmap: Bitmap?
        if (drawingCache != null) {
            bitmap = Bitmap.createBitmap(drawingCache)
            view.isDrawingCacheEnabled = false
        } else {
            bitmap = null
        }
        return bitmap
    }

    override fun setContentView(layoutResID: Int) {

        //子类View
        val childView = UUtils.getViewLay(layoutResID)

        //本类View
        mThisView = UUtils.getViewLay(R.layout.activity_base_view)


        mTitleBaseRl = mThisView.findViewById(R.id.title_base_rl)

        val findViewById = mThisView.findViewById<FrameLayout>(R.id.content)

        mImageViewRightLl = mThisView.findViewById<LinearLayout>(R.id.right_ll)
        mImageRight1 = mThisView.findViewById(R.id.right_img_1)
        mImageRight2 = mThisView.findViewById(R.id.right_img_2)

        mBaseTitle = mThisView.findViewById<TextView>(R.id.title_base)

        mRightRv1 = mThisView.findViewById<TextView>(R.id.right_tv1)

        mLeftImg = mThisView.findViewById<ImageView>(R.id.left_img)
        mLeftImg_ = mThisView.findViewById<ImageView>(R.id.left_img_)

        mLeftImg.setOnClickListener {

            finish()
        }
        mLeftImg_.setOnClickListener {

            finish()
        }


        mRightTv = mThisView.findViewById<TextView>(R.id.right_tv)




        findViewById.addView(childView)



        super.setContentView(mThisView)
    }

    //获取右边img

    public fun getRightImg1(): ImageView {

        mImageViewRightLl.visibility = View.VISIBLE

        return mImageRight1
    }

    public fun getRightImg2(): ImageView {

        mImageViewRightLl.visibility = View.VISIBLE

        return mImageRight2


    }

    public fun setGoneTitle() {

        mTitleBaseRl.visibility = View.GONE

    }

    public fun setVisibleTitle() {

        mTitleBaseRl.visibility = View.VISIBLE

    }

    public fun getBaseTitle():RelativeLayout {

      return  mTitleBaseRl

    }

    public fun goneRightImg() {

        mImageViewRightLl.visibility = View.GONE

    }
    //获取右边字体View

    public fun getRightTv(): TextView {
        return mRightTv

    }

    public fun getRightTv1(): TextView {
        return mRightRv1

    }

    //获取中间字体
    public fun getBaseTitleTv(): TextView {

        return mBaseTitle

    }

    //设置中间标题
    public fun setBaseTitleString(title: String) {

        mBaseTitle.visibility = View.VISIBLE
        mBaseTitle.text = title

    }

    public fun setBaseTitleStringId(id: Int) {

        mBaseTitle.visibility = View.VISIBLE
        mBaseTitle.text = UUtils.getString(id)

    }


    public fun setImgLiftAs() {

        mLeftImg_.visibility = View.VISIBLE
        mLeftImg.visibility = View.GONE
        mLeftImg_.setImageResource(R.mipmap.cancel)

    }

    public fun setImgLiftGone() {
        mLeftImg_.visibility = View.GONE
        mLeftImg.visibility = View.GONE

    }

    //public fun setImgLi
    public fun setThemeApp() {
        showAnimation()
        toggleThemeSetting()
        refreshUI()
    }

    private fun refreshUI() {

        val background = TypedValue() //背景色

        val textColor = TypedValue() //字体颜色

        val theme = theme
        theme.resolveAttribute(R.attr.clockBackground, background, true)
        theme.resolveAttribute(R.attr.clockTextColor, textColor, true)

        mThisView.setBackgroundResource(background.resourceId)
        refreshUI(background, textColor)

        refreshStatusBar()
    }


    /**
     * 刷新 StatusBar
     */
    private fun refreshStatusBar() {
        if (Build.VERSION.SDK_INT >= 21) {
            val typedValue = TypedValue()
            val theme = theme
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
            window.statusBarColor = resources.getColor(typedValue.resourceId)
        }
    }


    abstract fun refreshUI(mBackground: TypedValue, mTextColor: TypedValue)


}
