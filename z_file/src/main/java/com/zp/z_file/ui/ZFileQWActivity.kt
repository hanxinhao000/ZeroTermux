package com.zp.z_file.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.zp.z_file.R
import com.zp.z_file.common.ZFileActivity
import com.zp.z_file.content.*
import com.zp.z_file.databinding.ActivityZfileQwBinding
import com.zp.z_file.util.ZFileLog
import com.zp.z_file.util.ZFilePermissionUtil
import com.zp.z_file.util.ZFileQWUtil
import com.zp.z_file.util.ZFileUtil

internal class ZFileQWActivity : ZFileActivity(), ViewPager.OnPageChangeListener {

    private val vb by inflate<ActivityZfileQwBinding>()

    private var toManagerPermissionPage = false

    private var hasPermission = false

    private val selectArray by lazy {
        ArrayMap<String, ZFileBean>()
    }

    private var type = ZFileConfiguration.QQ

    private lateinit var vpAdapter: ZFileQWAdapter
    private var isManage = false

    override fun getContentView() = R.layout.activity_zfile_qw

    override fun create() = Unit

    override fun init(savedInstanceState: Bundle?) {
        type = getZFileConfig().filePath!!
        setBarTitle(if (type == ZFileConfiguration.QQ) "QQ文件" else "微信文件")
        vb.zfileQwToolBar.apply {
            if (getZFileConfig().showBackIcon) setNavigationIcon(R.drawable.zfile_back) else navigationIcon = null
            inflateMenu(R.menu.zfile_qw_menu)
            setOnMenuItemClickListener { menu -> menuItemClick(menu) }
            setNavigationOnClickListener { onBackPressed() }
        }
        initViewStub()
        callPermission()
    }

    private fun initAll() {
        setPermissionState(View.GONE)
        hasPermission = true
        vb.zfileQwViewPager.addOnPageChangeListener(this)
        vb.zfileQwTabLayout.setupWithViewPager( vb.zfileQwViewPager)
        vpAdapter = ZFileQWAdapter(type, isManage, this, supportFragmentManager)
        vb.zfileQwViewPager.adapter = vpAdapter
        vb.zfileQwViewPager.offscreenPageLimit = 4
    }

    fun observer(bean: ZFileQWBean) {
        val item = bean.zFileBean!!
        if (bean.isSelected) {
            val size = selectArray.size
            if (size >= getZFileConfig().maxLength) {
                toast(getZFileConfig().maxLengthStr)
                getVPFragment( vb.zfileQwViewPager.currentItem)?.removeLastSelectData(bean.zFileBean)
            } else {
                selectArray[item.filePath] = item
            }
        } else {
            if (selectArray.contains(item.filePath)) {
                selectArray.remove(item.filePath)
            }
        }
        setBarTitle("已选中${selectArray.size}个文件")
        isManage = true
        getMenu().isVisible = true
    }

    private fun getMenu() =  vb.zfileQwToolBar.menu.findItem(R.id.menu_zfile_qw_down)

    private fun menuItemClick(menu: MenuItem?): Boolean {
        if (!hasPermission) {
            ZFileLog.e("no permission")
            callPermission()
            return true
        }
        when (menu?.itemId) {
            R.id.menu_zfile_qw_down -> {
                if (selectArray.isNullOrEmpty()) {
                    vpAdapter.list.indices.forEach {
                        getVPFragment(it)?.apply {
                            resetAll()
                        }
                    }
                    isManage = false
                    getMenu().isVisible = false
                    setBarTitle(if (getZFileConfig().filePath!! == ZFileConfiguration.QQ) "QQ文件" else "微信文件")
                    getZFileHelp().getFileClickListener().emptyDataDownClick()
                } else {
                    setResult(ZFILE_RESULT_CODE, Intent().apply {
                        putParcelableArrayListExtra(ZFILE_SELECT_DATA_KEY, selectArray.toFileList() as java.util.ArrayList<out Parcelable>)
                    })
                    finish()
                }
            }
        }
        return true
    }

    private fun getVPFragment(currentItem: Int): ZFileQWFragment? {
        val fragmentId = vpAdapter.getItemId(currentItem)
        val tag = "android:switcher:${vb.zfileQwViewPager.id}:$fragmentId"
        return supportFragmentManager.findFragmentByTag(tag) as? ZFileQWFragment
    }

    override fun onPageScrollStateChanged(state: Int) = Unit
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit
    override fun onPageSelected(position: Int) {
        getVPFragment(position)?.setManager(isManage)
    }

    override fun onResume() {
        super.onResume()
        if (toManagerPermissionPage) {
            toManagerPermissionPage = false
            callPermission()
        }
    }

    private fun callPermission() {
        if (ZFilePermissionUtil.isRorESM()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkHasPermission() else initAll()
        } else {
            setPermissionState(View.VISIBLE)
            val builder = AlertDialog.Builder(this)
                .setTitle(R.string.zfile_11_title)
                .setMessage(R.string.zfile_11_content)
                .setCancelable(false)
                .setPositiveButton(R.string.zfile_down) { d, _ ->
                    toManagerPermissionPage = true
                    toFileManagerPage()
                    d.dismiss()
                }
                .setNegativeButton(R.string.zfile_cancel) { d, _ ->
                    toast(getStringById(R.string.zfile_11_bad))
                    d.dismiss()
                }
            builder.show()
        }
    }

    private fun checkHasPermission() {
        val hasPermission = ZFilePermissionUtil.hasPermission(
            this, ZFilePermissionUtil.READ_EXTERNAL_STORAGE,
            ZFilePermissionUtil.WRITE_EXTERNAL_STORAGE
        )
        if (hasPermission) {
            ZFilePermissionUtil.requestPermission(
                this,
                ZFilePermissionUtil.WRITE_EXTERNAL_CODE,
                ZFilePermissionUtil.READ_EXTERNAL_STORAGE,
                ZFilePermissionUtil.WRITE_EXTERNAL_STORAGE
            )
        } else {
            initAll()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ZFilePermissionUtil.WRITE_EXTERNAL_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) initAll()
            else {
                toast(getStringById(R.string.zfile_permission_bad))
                setPermissionState(View.VISIBLE)
            }
        }
    }

    private fun setBarTitle(title: String) {
        when (getZFileConfig().titleGravity) {
            ZFileConfiguration.TITLE_LEFT -> {
                vb.zfileQwToolBar.title = title
                vb.zfileQwCenterTitle.visibility = View.GONE
            }
            else -> {
                vb.zfileQwToolBar.title = ""
                vb.zfileQwCenterTitle.visibility = View.VISIBLE
                vb.zfileQwCenterTitle.text = title
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isManage = false
        selectArray.clear()
        ZFileUtil.resetAll()
    }

    private var noPermissionView: View? = null

    private fun initViewStub() {
        vb.zfileQwPermissionStub.layoutResource = getFilePermissionFailedLayoutId()
    }

    private fun setPermissionState(viewState: Int) {
        if (noPermissionView == null) {
            noPermissionView = vb.zfileQwPermissionStub.inflate()
            val btn = noPermissionView?.findViewById<View>(R.id.zfile_permission_againBtn)
            if (btn == null) {
                ZFileLog.e(PERMISSION_FAILED_TITLE)
                throw ZFileException(PERMISSION_FAILED_TITLE2)
            }
            btn.setOnClickListener { callPermission() }
        }
        noPermissionView?.visibility = viewState
    }

    private class ZFileQWAdapter(
        type: String,
        isManger: Boolean,
        context: Context,
        fragmentManager: FragmentManager
    ) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        var list = ArrayList<Fragment>()
        private val titles by lazy {
            ZFileQWUtil.getQWTitle(context)
        }

        init {
            list.add(ZFileQWFragment.newInstance(type, ZFILE_QW_PIC, isManger))
            list.add(ZFileQWFragment.newInstance(type, ZFILE_QW_MEDIA, isManger))
            list.add(ZFileQWFragment.newInstance(type, ZFILE_QW_DOCUMENT, isManger))
            list.add(ZFileQWFragment.newInstance(type, ZFILE_QW_OTHER, isManger))
        }

        override fun getItem(position: Int) = list[position]

        override fun getCount() = list.size

        override fun getItemPosition(any: Any) = PagerAdapter.POSITION_NONE

        override fun getPageTitle(position: Int): String? {
            val list = getZFileHelp().getQWFileLoadListener()?.getTitles() ?: titles
            if (list.size != QW_SIZE) {
                throw ZFileException("ZQWFileLoadListener.getTitles() size must be $QW_SIZE")
            } else {
               return list[position]
            }
        }
    }

}
