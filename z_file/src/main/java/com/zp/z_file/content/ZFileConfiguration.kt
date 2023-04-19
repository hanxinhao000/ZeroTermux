package com.zp.z_file.content

import android.content.Context
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.zp.z_file.R
import com.zp.z_file.ui.ZFileListFragment
import com.zp.z_file.ui.ZFileVideoPlayer
import com.zp.z_file.listener.*
import com.zp.z_file.async.ZFileStipulateAsync
import com.zp.z_file.listener.ZQWFileLoadListener
import java.io.Serializable

/**
 * 配置信息（单列保存，一处设置，全局通用）
 *
 * 1.4.5 主要更新信息：
 * 1) 新增 [showFolderBadgeHint]、[ZFileFolderBadgeHintListener] 文件夹角标展示
 * 2) 新增 [ZFileClickListener] 用于捕获点击事件
 * 3) 修复泄露
 *
 * 1.4.4 主要更新信息：
 * 1) 新增 [canNotSelecteFileTypeArray]、[canNotSelecteFileTypeStr] 属性
 * 2) 优化图片、自定义图片查看
 *
 * 1.4.3 主要更新信息：
 * 1) 修复点击隐藏 / 显示 隐藏文件后，路径错误的bug
 * 2) 自定义 QQ、Wechat 数据获取错误的bug修复
 * 3) QQ、Wechat 自定义功能 更新
 *
 * 1.4.2 主要更新信息：
 * 1) 修复在特定条件下无法获取文件的bug
 * 2) 新增音频、视频 播放中自动暂停，防止打扰来电等操作（避免社死 >_<:: ）
 * 3) 新增 [ZFileOtherListener]，其他多处优化
 *
 * 1.4.1 主要更新信息：
 * 1) 修复 错误的提示语句，新增 [clickAndAutoSelected] 属性
 * 2) 完善zip文件解压，修复展位图不展示问题
 *
 * 1.4.0 主要更新信息：
 * 1) 移除已过时配置
 *
 * 1.3.3 主要更新信息：
 * 1) 嵌套 Fragment 优化，列表优化，移除废弃方法！
 * 2) 新增 [needTwiceClick] 属性
 * 3) 新增 对于 m4a音频文件支持
 *
 * 1.3.2 主要更新信息：
 * 1) 支持 直接在 Activity 、 Fragment、 Fragment + ViewPager 中使用
 * 2) 文件复制优化
 *
 * 1.3.1 主要更新信息：
 * 1) 新增 [qwData] QQ、Wechat配置信息，不需要通过自定义 [ZQWFileLoadListener] 即可实现
 * 2) 修复 QQ、Wechat 部分路径下无法获取数据的bug
 *
 * 1.3.0 主要更新信息：
 * 1) Android 11 12 支持，完善 WPS 文件类型
 * 2) 新增 [titleGravity] 配置
 * 3) 删除文件崩溃、解压缩中文乱码问题修复
 * 4) [ZFileVideoPlayer] internal ---> open, ZFileAsyncImpl 重命名为 [ZFileStipulateAsync]
 *
 */
class ZFileConfiguration : Serializable {

    companion object {
         var mApplicationContext: Context? = null
        public fun init(mContext: Context) {
            mApplicationContext = mContext
        }
        /** QQ目录 */
        const val QQ = "ZFILE_QQ_FILE_PATH"

        /** 微信目录 */
        const val WECHAT = "ZFILE_WECHAT_FILE_PATH"

        /** 默认 */
        const val BY_DEFAULT = 0x1000

        /** 根据名字 */
        const val BY_NAME = 0x1001

        /** 根据最后修改时间 */
        const val BY_DATE = 0x1003

        /** 根据大小 */
        const val BY_SIZE = 0x1004

        /** 升序 */
        const val ASC = 0x2001

        /** 降序 */
        const val DESC = 0x2002

        /** 样式一 */
        const val STYLE1 = 1

        /** 样式二 */
        const val STYLE2 = 2

        const val RENAME = "重命名(rename)"
        const val COPY = "复制(copy)"
        const val MOVE = "移动(move)"
        const val DELETE = "删除(delete)"
        const val INFO = "查看详情(property)"

        /** 标题居左 */
        const val TITLE_LEFT = 0

        /** 标题居中 */
        const val TITLE_CENTER = 1

        /** 标题居右 */
        @Deprecated("肯定没人有这种BT需求，有的话我zhiboduodiao")
        const val TITLE_RIGHT = 2
    }

    /**
     * 起始访问位置，空为SD卡根目录，还可指定 [QQ] 或 [WECHAT] 目录
     */
    var filePath: String? = null

    /**
     * QQ、Wechat 配置信息
     */
    var qwData = ZFileQWData()

    /**
     * 图片资源配置
     */
    var resources = ZFileResources()

    /**
     * 是否显示隐藏文件
     */
    var showHiddenFile = true

    /**
     * 根据什么排序 see [BY_DEFAULT]、[BY_NAME]、[BY_DATE]、[BY_SIZE]
     */
    var sortordBy = BY_NAME

    /**
     * 排序方式 see [ASC]、[DESC]
     */
    var sortord = ASC

    /**
     * 过滤规则，默认显示所有的文件类型
     * 如 arrayOf(PNG, JPG, JPEG, GIF) 只显示图片类型
     */
    var fileFilterArray: Array<String>? = null

    /**
     * 不能选择的文件类型，默认都可以选择
     * 如 arrayOf(JPEG, GIF) 无法选择 jpeg 和 gif 图
     */
    var canNotSelecteFileTypeArray: Array<String>? = null

    /**
     * 不能选择的文件类型 文字提醒
     */
    var canNotSelecteFileTypeStr = "您不能选择当前类型的文件"

    /**
     * 文件选取大小的限制，单位：M
     */
    var maxSize = 10
        set(value) {
            field = value
            maxSizeStr = "您只能选取小于${field}M的文件"
        }

    /**
     * 超过最大选择大小文字提醒
     */
    var maxSizeStr = "您只能选取小于${maxSize}M的文件"

    /**
     * 最大选取数量
     */
    var maxLength = 9
        set(value) {
            field = value
            maxLengthStr = "您最多可以选取${field}个文件"
        }

    /**
     * 超过最大选择数量文字提醒
     */
    var maxLengthStr = "您最多可以选取${maxLength}个文件"

    /**
     * 选中的样式 see [STYLE1]、[STYLE2]
     */
    var boxStyle = STYLE2

    /**
     * 单个文件点击 是否自动选中文件，如只需要文件选择 设为true即可
     * true：直接选中文件，跳过打开、预览文件的步骤， 此时 [needTwiceClick] 属性将会自动设为 false
     * false：打开、预览文件
     */
    var clickAndAutoSelected = false
        set(value) {
            field = value
            needTwiceClick = !field
        }

    /**
     * 需要长按事件  默认为true；
     * 如只需要文件选择不需要文件操作，设为false即可
     */
    var needLongClick = true

    /**
     * 只有文件才有长按事件  默认为true；
     * 长按不支持对于文件夹的操作，如需要对于文件夹的操作，请实现 [ZFileOperateListener] 所有方法
     */
    var isOnlyFileHasLongClick = true

    /**
     * 长按后需要显示的操作类型 see [RENAME]、[COPY]、[MOVE]、[DELETE]、[INFO]
     * 空默认为 arrayOf(RENAME, COPY, MOVE, DELETE, INFO)
     * 目前只可以是这几种类型，个数、顺序可以自定义，文字不支持自定义
     */
    var longClickOperateTitles: Array<String>? = null

    /**
     * 是否只需要显示文件夹
     * 慎用！！！
     */
    var isOnlyFolder = false

    /**
     * 是否只需要显示文件
     * 慎用！！！
     */
    var isOnlyFile = false

    /**
     * 打开文件需要 [FileProvider] 一般都是包名 + xxxFileProvider
     * 如果项目中已经存在或其他原因无法修改，请自己实现 [ZFileOpenListener]
     */
    var authority = mApplicationContext?.packageName + ".fileProvider"

    /**
     * 是否需要显示 已选择的文件个数 提示
     */
    var showSelectedCountHint = false

    /**
     * 标题位置 see [TITLE_LEFT] [TITLE_CENTER]
     * 重写 [R.string.zfile_title] 即可自定义标题，默认展示 “文件管理”
     */
    var titleGravity = TITLE_LEFT
        set(value) {
            if (value in TITLE_LEFT..TITLE_RIGHT) {
                field = value
            } else {
                throwError("titleGravity")
            }
        }

    /**
     * 是否开启懒加载
     * 嵌套在 VP + Fragment 使用
     */
    var needLazy = true

    /**
     * Fragment TAG，可以通过 [FragmentManager.findFragmentByTag] 获取 [ZFileListFragment]
     * 在 ViewPager + Fragment 时，需要根据 [FragmentPagerAdapter.makeFragmentName] 设置 Tag
     */
    var fragmentTag = ZFILE_FRAGMENT_TAG

    /**
     * 是否显示 返回按钮图标
     */
    var showBackIcon = true

    /**
     * 是否需要两次点击后才能选择文件(参考百度网盘)  false：点击后立刻自动选中文件；true：默认
     */
    var needTwiceClick = true

    /**
     * 是否展示 文件夹 标签/角标、说明文字 相关，see [ZFileFolderBadgeHintListener]
     */
    var showFolderBadgeHint = true

    /**
     * 是否显示日志
     */
    var showLog = true

    /**
     * 方便java同学调用
     */
    class Build {

        private var configuration = ZFileConfiguration()

        /**
         * 起始访问位置
         * @param filePath String?  空为SD卡根目录 还可指定 [QQ] 或 [WECHAT] 目录
         */
        fun filePath(filePath: String?): Build {
            configuration.filePath = filePath
            return this
        }

        /**
         * QQ、Wechat 配置信息
         */
        fun qwData(qwData: ZFileQWData): Build {
            configuration.qwData = qwData
            return this
        }

        /**
         * 图片资源配置
         */
        fun resources(resources: ZFileResources): Build {
            configuration.resources = resources
            return this
        }

        /**
         * 是否显示隐藏文件
         */
        fun showHiddenFile(showHiddenFile: Boolean): Build {
            configuration.showHiddenFile = showHiddenFile
            return this
        }

        /**
         * 根据什么排序
         * @param sortordBy Int    see [BY_DEFAULT]、[BY_NAME]、[BY_DATE]、[BY_SIZE]
         */
        fun sortordBy(sortordBy: Int): Build {
            configuration.sortordBy = sortordBy
            return this
        }

        /**
         * 排序方式
         * @param sortord Int   see [ASC] or [DESC]
         */
        fun sortord(sortord: Int): Build {
            configuration.sortord = sortord
            return this
        }

        /**
         * 过滤规则，默认显示所有的文件类型
         * 如 arrayOf(PNG, JPG, JPEG, GIF) 只显示图片类型
         */
        fun fileFilterArray(fileFilterArray: Array<String>?): Build {
            configuration.fileFilterArray = fileFilterArray
            return this
        }

        /**
         * 不能选择的文件类型，默认都可以选择
         * 如 arrayOf(JPEG, GIF) 无法选择 jpeg 和 gif 图
         */
        fun canNotSelecteFileTypeArray(canNotSelecteFileTypeArray: Array<String>?): Build {
            configuration.canNotSelecteFileTypeArray = canNotSelecteFileTypeArray
            return this
        }

        /**
         * 不能选择的文件类型 文字提醒
         */
        fun canNotSelecteFileTypeStr(canNotSelecteFileTypeStr: String): Build {
            configuration.canNotSelecteFileTypeStr = canNotSelecteFileTypeStr
            return this
        }

        /**
         * 文件选取大小的限制，单位：M
         */
        fun maxSize(maxSize: Int): Build {
            configuration.maxSize = maxSize
            return this
        }

        /**
         * 超过最大选择大小文字提醒
         */
        fun maxSizeStr(maxSizeStr: String): Build {
            configuration.maxSizeStr = maxSizeStr
            return this
        }

        /**
         * 最大选取数量
         */
        fun maxLength(maxLength: Int): Build {
            configuration.maxLength = maxLength
            return this
        }

        /**
         * 超过最大选择数量文字提醒
         */
        fun maxLengthStr(maxLengthStr: String): Build {
            configuration.maxLengthStr = maxLengthStr
            return this
        }

        /**
         * 选中的样式
         * @param boxStyle Int  [STYLE1] or [STYLE2]
         */
        fun boxStyle(boxStyle: Int): Build {
            configuration.boxStyle = boxStyle
            return this
        }

        /**
         * 单个文件点击 是否自动选中文件，如只需要文件选择 设为true即可
         * true：直接选中文件，跳过打开、预览文件的步骤， 此时 [ZFileConfiguration.needTwiceClick] 属性将会自动设为 false
         * false：打开、预览文件
         */
        fun clickAndAutoSelected(clickAndAutoSelected: Boolean): Build {
            configuration.clickAndAutoSelected = clickAndAutoSelected
            return this
        }

        /**
         * 需要长按事件  默认为true；
         * 如只需要文件选择不需要文件操作，设为false即可
         */
        fun needLongClick(needLongClick: Boolean): Build {
            configuration.needLongClick = needLongClick
            return this
        }

        /**
         * 只有文件才有长按事件  默认为true；
         * 长按不支持对于文件夹的操作，如需要对于文件夹的操作，请实现 [ZFileOperateListener] 所有方法
         */
        fun isOnlyFileHasLongClick(isOnlyFileHasLongClick: Boolean): Build {
            configuration.isOnlyFileHasLongClick = isOnlyFileHasLongClick
            return this
        }

        /**
         * 长按后需要显示的操作类型 see [RENAME] [COPY] [MOVE] [DELETE] [INFO]
         * 空默认为 arrayOf(RENAME, COPY, MOVE, DELETE, INFO)
         * 目前只可以是这几种类型，个数、顺序可以自定义，文字不支持自定义
         */
        fun longClickOperateTitles(longClickOperateTitles: Array<String>?): Build {
            configuration.longClickOperateTitles = longClickOperateTitles
            return this
        }

        /**
         * 是否只需要显示文件夹
         * 慎用！！！
         */
        fun isOnlyFolder(isOnlyFolder: Boolean): Build {
            configuration.isOnlyFolder = isOnlyFolder
            return this
        }

        /**
         * 是否只需要显示文件
         * 慎用！！！
         */
        fun isOnlyFile(isOnlyFile: Boolean): Build {
            configuration.isOnlyFile = isOnlyFile
            return this
        }

        /**
         * 打开文件需要 [FileProvider] 一般都是包名 + xxxFileProvider
         * 如果项目中已经存在或其他原因无法修改，请自己实现 [ZFileOpenListener]
         */
        fun authority(authority: String): Build {
            configuration.authority = authority
            return this
        }

        /**
         * 是否需要显示 已选择的文件个数 提示
         */
        fun showSelectedCountHint(showSelectedCountHint: Boolean): Build {
            configuration.showSelectedCountHint = showSelectedCountHint
            return this
        }

        /**
         * 标题位置 《设置标题 重写 [R.string.zfile_title] 即可自定义》
         * @param titleGravity Int  [TITLE_LEFT] or [TITLE_CENTER]
         */
        fun titleGravity(titleGravity: Int): Build {
            configuration.titleGravity = titleGravity
            return this
        }

        /**
         * 是否开启懒加载
         * 嵌套在 VP + Fragment 使用
         */
        fun needLazy(needLazy: Boolean): Build {
            configuration.needLazy = needLazy
            return this
        }

        /**
         * Fragment TAG，可以通过 [FragmentManager.findFragmentByTag] 获取 [ZFileListFragment]
         * 嵌套在 VP + Fragment 使用，see [FragmentPagerAdapter.makeFragmentName]
         */
        fun fragmentTag(fragmentTag: String): Build {
            configuration.fragmentTag = fragmentTag
            return this
        }

        /**
         * 是否显示 返回按钮图标
         */
        fun showBackIcon(showBackIcon: Boolean): Build {
            configuration.showBackIcon = showBackIcon
            return this
        }

        /**
         * 是否需要两次点击后才能选择文件
         */
        fun needTwiceClick(needTwiceClick: Boolean): Build {
            configuration.needTwiceClick = needTwiceClick
            return this
        }

        /**
         * 是否展示 文件夹 标签/角标、说明文字 相关，see [ZFileFolderBadgeHintListener]
         */
        fun showFolderBadgeHint(showFolderBadgeHint: Boolean): Build {
            configuration.showFolderBadgeHint = showFolderBadgeHint
            return this
        }

        /**
         * 是否显示日志
         */
        fun showLog(showLog: Boolean): Build {
            configuration.showLog = showLog
            return this
        }

        /**
         * 构建 [ZFileConfiguration]
         */
        fun build() = configuration

    }

    /**
     * 图片相关资源配置 设置 [ZFILE_DEFAULT] 将使用默认资源 各种文件类型的图片 建议 128 * 128
     * @property audioRes Int        音频
     * @property txtRes Int          文本
     * @property pdfRes Int          PDF
     * @property pptRes Int          PPT
     * @property wordRes Int         Word
     * @property excelRes Int        Excel
     * @property zipRes Int          ZIP
     * @property otherRes Int        其他类型
     * @property emptyRes Int        空资源，还可以重写 [ZFileOtherListener.getFileListEmptyLayoutId] 达到完全自定义
     * @property folderRes Int       文件夹
     * @property lineColor Int       列表分割线颜色
     */
    data class ZFileResources @JvmOverloads constructor(
        var audioRes: Int = ZFILE_DEFAULT,
        var txtRes: Int = ZFILE_DEFAULT,
        var pdfRes: Int = ZFILE_DEFAULT,
        var pptRes: Int = ZFILE_DEFAULT,
        var wordRes: Int = ZFILE_DEFAULT,
        var excelRes: Int = ZFILE_DEFAULT,
        var zipRes: Int = ZFILE_DEFAULT,
        var otherRes: Int = ZFILE_DEFAULT,
        var emptyRes: Int = ZFILE_DEFAULT,
        var folderRes: Int = ZFILE_DEFAULT,
        var lineColor: Int = ZFILE_DEFAULT
    ) : Serializable

}


