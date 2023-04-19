package com.zp.z_file.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.system.Os
import android.view.*
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.zp.z_file.R
import com.zp.z_file.common.ZFileAdapter
import com.zp.z_file.common.ZFileViewHolder
import com.zp.z_file.content.*
import com.zp.z_file.databinding.FragmentZfileListBinding
import com.zp.z_file.dsl.ZFileDsl
import com.zp.z_file.listener.ZFragmentListener
import com.zp.z_file.ui.adapter.ZFileListAdapter
import com.zp.z_file.ui.dialog.YesNoDialog
import com.zp.z_file.ui.dialog.ZFileSelectFolderDialog
import com.zp.z_file.ui.dialog.ZFileSortDialog
import com.zp.z_file.util.ZFileLog
import com.zp.z_file.util.ZFilePermissionUtil
import com.zp.z_file.util.ZFileUUtils
import com.zp.z_file.util.ZFileUtil
import java.io.File

/**
 * 文件管理 核心实现，可以 在 Activity、Fragment or ViewPager中使用
 *
 * 注意：使用 [FragmentManager] 或者 [ViewPager] 动态添加或直接嵌套使用时
 * 1. 无法通过 [ZFileDsl] 获取返回的数据，其他配置将不受影响
 * 2. 需在 Activity 中配置 [ZFragmentListener] 来接收选中的文件
 * 3. Activity onBackPressed 方法 需要 调用 [onBackPressed]
 * 4. Activity onResume 方法 需要调用 [showPermissionDialog]
 */
class ZFileListFragment : Fragment() {

    private var vb: FragmentZfileListBinding? = null

    private lateinit var mActivity: FragmentActivity

    private var isFirstLoad = true

    private var toManagerPermissionPage = false

    private var barShow = false
    private lateinit var filePathAdapter: ZFileAdapter<ZFilePathBean>
    private var fileListAdapter: ZFileListAdapter? = null

    private var pageLoadIndex = 0
    private var rootPath = "/data/data/com.termux/files/" // 根目录
    private var specifyPath: String? = "" // 指定目录
    private var nowPath: String? = "/data/data/com.termux/files/" // 当前目录

    private var hasPermission = false

    var zFragmentListener: ZFragmentListener? = null

    private val titleArray by lazy {
        if (getZFileConfig().longClickOperateTitles.isNullOrEmpty()) {
            arrayOf(
                ZFileConfiguration.RENAME,
                ZFileConfiguration.COPY,
                ZFileConfiguration.MOVE,
                ZFileConfiguration.DELETE,
                ZFileConfiguration.INFO
            )
        } else getZFileConfig().longClickOperateTitles
    }

    private val backList by lazy {
        ArrayList<String>()
    }

    private var sortSelectId = R.id.zfile_sort_by_default // 排序方式选中的ID
    private var sequenceSelectId = R.id.zfile_sequence_asc // 顺序选中的ID

    /** 返回当前的路径 */
    private fun getThisFilePath() = if (backList.isEmpty()) null else backList[backList.size - 1]

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentActivity) {
            mActivity = context
        } else {
            throw ZFileException("activity must be FragmentActivity！！！")
        }
    }

    companion object {

        /**
         * 获取 [ZFileListFragment] 实例
         */
        @JvmStatic
        fun newInstance(): ZFileListFragment {
            val startPath = getZFileConfig().filePath
            if (startPath == ZFileConfiguration.QQ || startPath == ZFileConfiguration.WECHAT) {
                throw ZFileException(
                    "startPath must be real path or empty, if you want use \" qq \" or \" wechat \", " +
                            "please use \" getZFileHelp().start() \""
                )
            }
            var newPath = if (startPath.isNullOrEmpty()) SD_ROOT else startPath
            if (!newPath.toFile().exists()) {
                newPath = "/data/data/com.termux/"
            }
            if (!newPath.toFile().exists()) {
                newPath = Environment.getExternalStorageDirectory().absolutePath
            }
            return ZFileListFragment().apply {
                arguments = Bundle().run {
                    putString(FILE_START_PATH_KEY, newPath)
                    this
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        vb = FragmentZfileListBinding.inflate(inflater, container, false)
        return vb?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!getZFileConfig().needLazy) {
            initAll()
        }
    }

    override fun onResume() {
        super.onResume()
        if (getZFileConfig().needLazy) {
            if (isFirstLoad) {
                initAll()
                isFirstLoad = false
            }
        }
    }

    /**
     * 可以有效避免 VP + Fragment 出现的问题
     * 请在 Activity 中的 onResume 中调用该方法
     */
    fun showPermissionDialog() {
        if (toManagerPermissionPage) {
            toManagerPermissionPage = false
            callPermission()
        }
    }

    private fun setMenuState() {
        vb?.zfileListToolBar?.menu?.apply {
            findItem(R.id.menu_zfile_down).isVisible = barShow
           /* findItem(R.id.menu_zfile_px).isVisible = !barShow
            findItem(R.id.menu_zfile_show).isVisible = !barShow
            findItem(R.id.menu_zfile_hidden).isVisible = !barShow*/
        }
    }

    private fun menuItemClick(menu: MenuItem?): Boolean {
        if (!hasPermission) {
            ZFileLog.e("no permission")
            callPermission()
            return true
        }
        when (menu?.itemId) {
            R.id.menu_zfile_down -> {
                val list = fileListAdapter?.selectData
                if (list.isNullOrEmpty()) {
                    setBarTitle(requireContext() getStringById R.string.zfile_title)
                    fileListAdapter?.isManage = false
                    barShow = false
                    setMenuState()
                    getZFileHelp().getFileClickListener().emptyDataDownClick()
                } else {
                    if (zFragmentListener == null) {
                        /*mActivity.setResult(ZFILE_RESULT_CODE, Intent().apply {
                            putParcelableArrayListExtra(
                                ZFILE_SELECT_DATA_KEY,
                                list as java.util.ArrayList<out Parcelable>
                            )
                        })
                        mActivity.finish()*/
                        getZFileHelp().setResult(mActivity, list)
                    } else {
                        zFragmentListener?.selectResult(list)
                    }
                }
            }
            R.id.new_file -> {
                if (nowPath.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), requireContext() getStringById R.string.zfile_create_file_error, Toast.LENGTH_SHORT).show()
                    return false
                }
                val yesNoDialog = YesNoDialog(context!!)
                val createEditDialog = yesNoDialog
                    .createEditDialog(requireContext() getStringById R.string.zfile_new_file)
                createEditDialog.yesTv.setOnClickListener {
                    val text = createEditDialog.inputSystemName.text
                    if (text.isNullOrEmpty()) {
                        Toast
                            .makeText(
                                requireContext(),
                                requireContext() getStringById R.string.zfile_create_file_error,
                                Toast.LENGTH_SHORT)
                            .show()
                        return@setOnClickListener
                    }
                    createEditDialog.dismiss()
                    val file = File(nowPath, "/$text")
                    file.createNewFile().apply{
                        if (!this) {
                            Toast.makeText(requireContext(), requireContext() getStringById R.string.zfile_create_file_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                    Toast
                        .makeText(
                            requireContext(),
                            requireContext() getStringById R.string.zfile_create_ok,
                            Toast.LENGTH_SHORT)
                        .show()
                    getData(nowPath)
                }
                createEditDialog.show()

            }
            R.id.new_folder -> {

                if (nowPath.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), requireContext() getStringById R.string.zfile_create_file_error, Toast.LENGTH_SHORT).show()
                    return false
                }
                val yesNoDialog = YesNoDialog(context!!)
                val createEditDialog = yesNoDialog
                    .createEditDialog(requireContext() getStringById R.string.zfile_new_folder)
                createEditDialog.yesTv.setOnClickListener {
                    val text = createEditDialog.inputSystemName.text
                    if (text.isNullOrEmpty()) {
                        Toast
                            .makeText(
                                requireContext(),
                                requireContext() getStringById R.string.zfile_create_file_error,
                                Toast.LENGTH_SHORT)
                            .show()
                        return@setOnClickListener
                    }
                    createEditDialog.dismiss()
                    val file = File(nowPath, "/$text")
                    file.mkdirs().apply{
                        if (!this) {
                            Toast.makeText(requireContext(), requireContext() getStringById R.string.zfile_create_file_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                    Toast
                        .makeText(
                            requireContext(),
                            requireContext() getStringById R.string.zfile_create_ok,
                            Toast.LENGTH_SHORT)
                        .show()
                    getData(nowPath)
                }
                createEditDialog.show()
                getData(nowPath)
            }
            R.id.create_sdcard -> {
                checkHasPermission()
                val sdcardPath =  Environment.getExternalStorageDirectory().absolutePath
                setupFileSymlinks(sdcardPath, "$nowPath/sdcard").apply {
                    if (!this) {
                        Toast
                            .makeText(
                                requireContext(),
                                requireContext() getStringById R.string.zfile_create_ok,
                                Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                getData(nowPath)

            }
            R.id.open_folder -> {
                LocalBroadcastManager.getInstance(view!!.context).apply {
                    val intent = Intent()
                    intent.action = "localbroadcast"
                    val sendText = "cd $nowPath"
                    intent.putExtra("broadcastString", sendText)
                    sendBroadcast(intent)
                }
            }
            R.id.git_clone -> {
                val yesNoDialog = YesNoDialog(context!!)
                val createEditDialog = yesNoDialog
                    .createEditDialog(requireContext() getStringById R.string.zfile_git_clone)
                createEditDialog.inputSystemName.hint = "请输入git网址  input git url"
                createEditDialog.yesTv.setOnClickListener {
                    val text = createEditDialog.inputSystemName.text
                    if (text.isNullOrEmpty()) {
                        Toast
                            .makeText(
                                requireContext(),
                                requireContext() getStringById R.string.zfile_git_clone_empty,
                                Toast.LENGTH_SHORT)
                            .show()
                        return@setOnClickListener
                    }
                    createEditDialog.dismiss()

                    LocalBroadcastManager.getInstance(view!!.context).apply {
                        val intent = Intent()
                        intent.action = "localbroadcast"
                        val sendText = "pkg install git && git clone $text"
                        intent.putExtra("broadcastString", sendText)
                        sendBroadcast(intent)
                    }

                }
                createEditDialog.show()

            }

            R.id.wget -> {
                val yesNoDialog = YesNoDialog(context!!)
                val createEditDialog = yesNoDialog
                    .createEditDialog(requireContext() getStringById R.string.zfile_git_clone)
                createEditDialog.inputSystemName.hint = "请输入下载地址  input download url"
                createEditDialog.yesTv.setOnClickListener {
                    val text = createEditDialog.inputSystemName.text
                    if (text.isNullOrEmpty()) {
                        Toast
                            .makeText(
                                requireContext(),
                                requireContext() getStringById R.string.zfile_git_clone_empty,
                                Toast.LENGTH_SHORT)
                            .show()
                        return@setOnClickListener
                    }
                    createEditDialog.dismiss()

                    LocalBroadcastManager.getInstance(view!!.context).apply {
                        val intent = Intent()
                        intent.action = "localbroadcast"
                        val sendText = "pkg install wget && wget $text"
                        intent.putExtra("broadcastString", sendText)
                        sendBroadcast(intent)
                    }

                }
                createEditDialog.show()
            }
            R.id.open_http -> {
                LocalBroadcastManager.getInstance(view!!.context).apply {
                    val intent = Intent()
                    intent.action = "localbroadcast"
                    val sendText = "open"
                    intent.putExtra("broadcastHttp", sendText)
                    sendBroadcast(intent)
                }
            }
            R.id.close_http -> {
                LocalBroadcastManager.getInstance(view!!.context).apply {
                    val intent = Intent()
                    intent.action = "localbroadcast"
                    val sendText = "close"
                    intent.putExtra("broadcastHttp", sendText)
                    sendBroadcast(intent)
                }
            }
            R.id.zt_developer -> {
                LocalBroadcastManager.getInstance(view!!.context).apply {
                    val intent = Intent()
                    intent.action = "localbroadcast"
                    val sendText = "DeveloperActivity"
                    intent.putExtra("broadcastStartActivity", sendText)
                    sendBroadcast(intent)
                }
            }

         //   R.id.menu_zfile_px -> showSortDialog()
         //   R.id.menu_zfile_show -> {
         //       menu.isChecked = true
         //       getZFileConfig().showHiddenFile = true
         //       getData(nowPath)
         //   }
         //   R.id.menu_zfile_hidden -> {
         //       menu.isChecked = true
          //      getZFileConfig().showHiddenFile = false
         //       getData(nowPath)
        //    }
        }
        return true
    }

    private fun initAll() {
        setSortSelectId()
        specifyPath = arguments?.getString(FILE_START_PATH_KEY)
        getZFileConfig().filePath = specifyPath
        rootPath = specifyPath ?: ""
        backList.add(rootPath)
        nowPath = rootPath
        vb?.zfileListToolBar?.apply {
           // if (getZFileConfig().showBackIcon) setNavigationIcon(R.drawable.zfile_back) else navigationIcon = null
            inflateMenu(R.menu.zfile_list_menu)
            setOnMenuItemClickListener { menu -> menuItemClick(menu) }
            setNavigationOnClickListener { back() }
        }
        setHiddenState()
        setBarTitle(requireContext() getStringById R.string.zfile_title)
        initViewStub()
        callPermission()
    }

    private fun initRV() {
        hasPermission = true
        setPermissionState(View.GONE)

        vb?.zfileListRefreshLayout?.property() {
            getData(nowPath)
        }
        initPathRecyclerView()
        initListRecyclerView()
    }

    private fun initPathRecyclerView() {
        filePathAdapter = object : ZFileAdapter<ZFilePathBean>(requireContext(), R.layout.item_zfile_path) {
            override fun bindView(holder: ZFileViewHolder, item: ZFilePathBean, position: Int) {
                holder.setText(R.id.item_zfile_path_title, item.fileName)
                holder.setOnItemClickListener {
                    if (backList.size > 1) {
                        backList.removeAt(backList.size - 1)
                        val lastPath = getThisFilePath()
                        getData(lastPath)
                        nowPath = lastPath
                        filePathAdapter.remove(filePathAdapter.itemCount - 1)
                        vb?.zfileListPathRecyclerView?.scrollToPosition(filePathAdapter.itemCount - 1)
                    }
                }
            }

            override fun addItem(position: Int, t: ZFilePathBean) {
                var hasData = false
                getDatas().forEach forEach@{
                    if (it.filePath == t.filePath) {
                        hasData = true
                        return@forEach
                    }
                }
                if (!(hasData || t.filePath == SD_ROOT)) {
                    super.addItem(position, t)
                }
            }

        }

        val llM = object : LinearLayoutManager(activity, HORIZONTAL, false) {
            override fun canScrollHorizontally(): Boolean {
                return false
            }
        }
        vb?.zfileListPathRecyclerView?.apply {
            layoutManager = llM
            adapter = filePathAdapter
        }
        getPathData()
    }

    private fun getPathData() {
        val filePath = getZFileConfig().filePath
        val pathList = ArrayList<ZFilePathBean>()
        if (filePath.isNullOrEmpty() || filePath == SD_ROOT) {
            pathList.add(ZFilePathBean(requireContext() getStringById R.string.zfile_root_path, "root"))
        } else {
            pathList.add(ZFilePathBean("${requireActivity() getStringById R.string.zfile_path}${filePath.getFileName()}", filePath))
        }
        filePathAdapter.addAll(pathList)
    }

    private fun initListRecyclerView() {
        fileListAdapter = ZFileListAdapter(requireContext()).run {
            itemClick = { v, position, item ->
                val fileClickListener = getZFileHelp().getFileClickListener()
                if (item.isFile) {
                    if (getZFileConfig().clickAndAutoSelected) {
                        boxLayoutClick(position, item)
                    } else {
                        ZFileUtil.openFile(item.filePath, v)
                    }
                    fileClickListener.itemFileClick(item, v)
                } else {
                    ZFileLog.i("进入 ${item.filePath}")
                    backList.add(item.filePath)
                    filePathAdapter.addItem(filePathAdapter.itemCount, item.toPathBean())
                    vb?.zfileListPathRecyclerView?.scrollToPosition(filePathAdapter.itemCount - 1)
                    getData(item.filePath)
                    nowPath = item.filePath
                    fileClickListener.itemFoldClick(item, v)
                }
            }
            itemLongClick = { _, index, item ->
                if (fileListAdapter?.isManage == true) {
                    false
                } else {
                    if (getZFileConfig().needLongClick) {
                        if (getZFileConfig().isOnlyFileHasLongClick) {
                           /* if (item.isFile)*/ showSelectDialog(index, item)
                           // else false
                        } else {
                            showSelectDialog(index, item)
                        }
                    } else {
                        false
                    }
                }
            }
            changeListener = { isManage, size ->
                if (isManage) {
                    if (barShow) {
                        setBarTitle("已选中${size}个文件")
                    } else {
                        barShow = true
                        setBarTitle("已选中0个文件")
                        setMenuState()
                    }
                }
            }
            this
        }
        vb?.zfileListListRecyclerView?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = fileListAdapter
        }
        getData(getZFileConfig().filePath)
        pageLoadIndex ++
    }

    private fun getData(filePath: String?) {
        if (!hasPermission) {
            ZFileLog.e("no permission")
            return
        }
        vb?.zfileListRefreshLayout?.isRefreshing = true
        val key = if (filePath.isNull()) SD_ROOT else filePath!!
        if (rootPath.isEmpty()) {
            rootPath = key
        }
        getZFileConfig().filePath = filePath
        if (pageLoadIndex != 0 && SD_ROOT != key) {
            val pathBean = File(key).toPathBean()
            filePathAdapter.addItem(filePathAdapter.itemCount, pathBean)
            vb?.zfileListPathRecyclerView?.scrollToPosition(filePathAdapter.itemCount - 1)
        }
        ZFileUtil.getList(requireContext()) {
            if (isNullOrEmpty()) {
                fileListAdapter?.clear()
                setEmptyState(View.VISIBLE)
            } else {
                fileListAdapter?.setDatas(this)
                setEmptyState(View.GONE)
            }
            vb?.zfileListRefreshLayout?.isRefreshing = false
        }
    }

    private fun showSelectDialog(index: Int, item: ZFileBean): Boolean {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("请选择(select)")
            setItems(titleArray) { dialog, which ->
                jumpByWhich(item, which, index)
                dialog.dismiss()
            }
            setPositiveButton("取消(cancel)") { dialog, _ -> dialog.dismiss() }
            show()
        }
        return true
    }

    private val TAG by lazy {
        ZFileSelectFolderDialog::class.java.simpleName
    }

    private fun jumpByWhich(item: ZFileBean, which: Int, index: Int) {
        when (titleArray!![which]) {
            ZFileConfiguration.RENAME -> {
                getZFileHelp().getFileOperateListener()
                    .renameFile(item.filePath, requireContext()) { isSuccess, newName ->
                        if (isSuccess) {
                            val oldFile = item.filePath.toFile()
                            val oldPath =
                                oldFile.path.substring(0, oldFile.path.lastIndexOf("/") + 1)
                            val newFilePath = "$oldPath$newName"
                            fileListAdapter?.getItem(index)?.apply {
                                filePath = newFilePath
                                fileName = "$newName"
                            }
                            fileListAdapter?.notifyItemChanged(index)
                        }
                    }
            }
            ZFileConfiguration.COPY, ZFileConfiguration.MOVE -> {
                mActivity.checkFragmentByTag(TAG)
                ZFileSelectFolderDialog.newInstance(titleArray!![which]).apply {
                    selectFolder = {
                        doSth(item, this, titleArray!![which], index)
                    }
                }.show(mActivity.supportFragmentManager, TAG)
            }
            ZFileConfiguration.DELETE -> getZFileHelp().getFileOperateListener().deleteFile(
                item.filePath,
                requireContext()
            ) {
                if (this) {
                    fileListAdapter?.remove(index, nullBlock = {
                        setEmptyState(if (it) View.VISIBLE else View.GONE)
                    })
                    ZFileLog.i("文件删除成功")
                } else {
                    ZFileLog.i("文件删除失败")
                }
            }
            ZFileConfiguration.INFO -> ZFileUtil.infoFile(item, requireContext())
            else -> throwError("longClickOperateTitles")
        }
    }

    private fun doSth(item: ZFileBean, targetPath: String, type: String, position: Int) {
        if (type == ZFileConfiguration.COPY) { // 复制文件
            getZFileHelp().getFileOperateListener().copyFile(item.filePath, targetPath, requireContext()) {
                if (this) {
                    ZFileLog.i("文件复制成功")
                    observer(true)
                } else {
                    ZFileLog.e("文件复制失败")
                }
            }
        } else { // 移动文件
            getZFileHelp().getFileOperateListener().moveFile(item.filePath, targetPath, requireContext()) {
                if (this) {
                    fileListAdapter?.remove(position, nullBlock = {
                        setEmptyState(if (it) View.VISIBLE else View.GONE)
                    })
                    ZFileLog.i("文件移动成功")
                } else {
                    ZFileLog.e("文件移动失败")
                }
            }
        }
    }

    private fun showSortDialog() {
        val tag = ZFileSortDialog::class.java.simpleName
        mActivity.checkFragmentByTag(tag)
        ZFileSortDialog.newInstance(sortSelectId, sequenceSelectId).apply {
            checkedChangedListener = { sortId, sequenceId ->
                sortSelectId = sortId
                sequenceSelectId = sequenceId
                val sortordByWhat = when (sortId) {
                    R.id.zfile_sort_by_default -> ZFileConfiguration.BY_DEFAULT
                    R.id.zfile_sort_by_name -> ZFileConfiguration.BY_NAME
                    R.id.zfile_sort_by_date -> ZFileConfiguration.BY_DATE
                    R.id.zfile_sort_by_size -> ZFileConfiguration.BY_SIZE
                    else -> ZFileConfiguration.BY_DEFAULT
                }
                val sortord = when (sequenceId) {
                    R.id.zfile_sequence_asc -> ZFileConfiguration.ASC
                    R.id.zfile_sequence_desc -> ZFileConfiguration.DESC
                    else -> ZFileConfiguration.ASC
                }
                getZFileConfig().apply {
                    sortordBy = sortordByWhat
                    this.sortord = sortord
                }
                getData(nowPath)
            }
        }.show(mActivity.supportFragmentManager, tag)
    }

    /**
     * 监听返回按钮
     * 请在 Activity 中的 onBackPressed 中调用该方法
     */
    fun onBackPressed() {
        back()
    }

    private fun back() {
        val path = getThisFilePath()
        if (path == rootPath || path.isNullOrEmpty()) { // 根目录
            if (barShow) {  // 存在编辑状态
                setBarTitle(requireContext() getStringById R.string.zfile_title)
                fileListAdapter?.isManage = false
                barShow = false
                setMenuState()
            } else {
                if (zFragmentListener == null) {
                    mActivity.onBackPressed()
                } else {
                    zFragmentListener?.onActivityBackPressed(mActivity)
                }
            }
        } else { // 返回上一级
            // 先清除当前一级的数据
            backList.removeAt(backList.size - 1)
            val lastPath = getThisFilePath()
            getData(lastPath)
            nowPath = lastPath
            filePathAdapter.remove(filePathAdapter.itemCount - 1)
            vb?.zfileListPathRecyclerView?.scrollToPosition(filePathAdapter.itemCount - 1)
        }
    }

    private fun callPermission() {
        if (ZFilePermissionUtil.isRorESM()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkHasPermission() else initRV()
        } else {
            setPermissionState(View.VISIBLE)
            val builder = AlertDialog.Builder(requireContext())
                .setTitle(R.string.zfile_11_title)
                .setMessage(R.string.zfile_11_content)
                .setCancelable(false)
                .setPositiveButton(R.string.zfile_down) { d, _ ->
                    toManagerPermissionPage = true
                    requireContext().toFileManagerPage()
                    d.dismiss()
                }
                .setNegativeButton(R.string.zfile_cancel) { d, _ ->
                    d.dismiss()
                    if (zFragmentListener == null) {
                        requireContext().toast(requireContext() getStringById R.string.zfile_11_bad)
                        mActivity.finish()
                    } else {
                        zFragmentListener?.onExternalStorageManagerFiled(mActivity)
                    }
                }
            builder.show()
        }
    }

    private fun checkHasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasSDPermission = ZFilePermissionUtil.hasPermission(
                requireContext(),
                ZFilePermissionUtil.READ_EXTERNAL_STORAGE,
                ZFilePermissionUtil.WRITE_EXTERNAL_STORAGE
            )
            if (hasSDPermission) {
                ZFilePermissionUtil.requestPermission(
                    this,
                    ZFilePermissionUtil.WRITE_EXTERNAL_CODE,
                    ZFilePermissionUtil.READ_EXTERNAL_STORAGE,
                    ZFilePermissionUtil.WRITE_EXTERNAL_STORAGE
                )
            } else {
                initRV()
            }
        } else {
            initRV()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ZFilePermissionUtil.WRITE_EXTERNAL_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) initRV()
            else {
                setPermissionState(View.VISIBLE)
                if (zFragmentListener == null) {
                    requireContext().toast(requireContext() getStringById R.string.zfile_permission_bad)
                    mActivity.finish()
                } else {
                    zFragmentListener?.onSDPermissionsFiled(mActivity)
                }
            }
        }
    }

    private fun setSortSelectId() {
        sortSelectId = when (getZFileConfig().sortordBy) {
            ZFileConfiguration.BY_NAME -> R.id.zfile_sort_by_name
            ZFileConfiguration.BY_SIZE -> R.id.zfile_sort_by_size
            ZFileConfiguration.BY_DATE -> R.id.zfile_sort_by_date
            else -> R.id.zfile_sort_by_default
        }
        sequenceSelectId = when (getZFileConfig().sortord) {
            ZFileConfiguration.DESC -> R.id.zfile_sequence_desc
            else -> R.id.zfile_sequence_asc
        }
    }

    private fun setHiddenState() {
        vb?.zfileListToolBar?.post {
            val menu = vb?.zfileListToolBar?.menu
            val showMenuItem = menu?.findItem(R.id.new_file)
            val hiddenMenuItem = menu?.findItem(R.id.new_folder)
            if (getZFileConfig().showHiddenFile) {
                showMenuItem?.isChecked = true
            } else {
                hiddenMenuItem?.isChecked = true
            }
        }
    }

    fun observer(isSuccess: Boolean) {
        if (isSuccess) getData(nowPath)
    }

    private fun setBarTitle(title: String) {
        when (getZFileConfig().titleGravity) {
            ZFileConfiguration.TITLE_LEFT -> {
                vb?.zfileListToolBar?.title = title
                vb?.zfileListToolBar?.setOnClickListener {
                    if ( ZFileUUtils.getHostIP() != null &&  ZFileUUtils.getHostIP().isNullOrEmpty()) {
                        try {
                            val split = ZFileUUtils.getHostIP().split("\n")
                            if (split.isNotEmpty()) {
                                ZFileUUtils.startUrl("http://" + split[0] + ":19956")
                            } else {
                                ZFileUUtils.showMsg(ZFileUUtils.getString(R.string.no_http_msg))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ZFileUUtils.showMsg(e.toString())
                        }
                    }
                }

                vb?.zfileListCenterTitle?.visibility = View.GONE
            }
            else -> {
                vb?.zfileListToolBar?.title = ""
                vb?.zfileListCenterTitle?.visibility = View.VISIBLE
                vb?.zfileListCenterTitle?.text = title
            }
        }
    }

    override fun onDestroy() {
        ZFileUtil.resetAll()
        super.onDestroy()
        fileListAdapter?.reset()
        backList.clear()
        zFragmentListener = null
    }

    private var emptyView: View? = null
    private var noPermissionView: View? = null

    private fun initViewStub() {
        vb?.zfileListEmptyStub?.layoutResource = getFileEmptyLayoutId()
        vb?.zfileListNoPermissionStub?.layoutResource = getFilePermissionFailedLayoutId()
    }

    private fun setEmptyState(viewState: Int) {
        if (emptyView == null) {
            emptyView = vb?.zfileListEmptyStub?.inflate()
            emptyView?.findViewById<ImageView>(R.id.zfile_list_emptyPic)?.setImageResource(emptyRes)
        }
        emptyView?.visibility = viewState
    }

    private fun setPermissionState(viewState: Int) {
        if (noPermissionView == null) {
            noPermissionView = vb?.zfileListNoPermissionStub?.inflate()
            val btn = noPermissionView?.findViewById<View>(R.id.zfile_permission_againBtn)
            if (btn == null) {
                ZFileLog.e(PERMISSION_FAILED_TITLE)
                throw ZFileException(PERMISSION_FAILED_TITLE2)
            }
            btn.setOnClickListener {
                callPermission()
                getZFileHelp().getFileClickListener().permissionBtnApplyClick(it)
            }
        }
        noPermissionView?.visibility = viewState
    }

    public fun setupFileSymlinks(originalPath: String, linkPath: String): Boolean {
    //    LogUtils.d(TAG, "setupFileSymlinks originalPath:${originalPath} linkPath:$linkPath")
        try {
            Os.symlink(originalPath, linkPath)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context!!, e.toString(), Toast.LENGTH_SHORT) .show()
            return false
           // LogUtils.d(TAG, "setupFileSymlinks error${e.toString()}")
        }
    }

}
