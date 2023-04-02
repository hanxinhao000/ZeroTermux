package com.zp.z_file.content

import androidx.collection.ArrayMap
import com.zp.z_file.listener.ZQWFileLoadListener
import java.io.Serializable

/**
 * QQ、Wechat基本配置信息
 * 通过简单配置，即可 修改标题，文件类型，文件路径，获取相应的数据
 *
 * 若需要完全自定义获取 QQ、Wechat 文件，请实现 [ZQWFileLoadListener] 所有方法
 */
class ZFileQWData : Serializable {

    /**
     * 显示的标题 ，空使用默认  size必须为 [QW_SIZE]
     */
    var titles: Array<String>? = null

    /**
     * QQ、Wechat 文件过滤规则的 Map ，空使用默认
     * Int             表示 文件类型 see[ZFILE_QW_PIC]、[ZFILE_QW_MEDIA]、[ZFILE_QW_DOCUMENT]、[ZFILE_QW_OTHER]
     * Array<String>   表示 过滤规则
     */
    var filterArrayMap: ArrayMap<Int, Array<String>>? = null

    /**
     * QQ 保存至本地SD卡上路径的 Map ，空使用默认
     * Int                  表示 文件类型 see[ZFILE_QW_PIC]、[ZFILE_QW_MEDIA]、[ZFILE_QW_DOCUMENT]、[ZFILE_QW_OTHER]
     * MutableList<String>  表示 SD卡上的路径
     */
    var qqFilePathArrayMap: ArrayMap<Int, MutableList<String>>? = null

    /**
     * Wechat 保存至本地SD卡上路径的 Map ，空使用默认
     * Int                  表示 文件类型 see[ZFILE_QW_PIC]、[ZFILE_QW_MEDIA]、[ZFILE_QW_DOCUMENT]、[ZFILE_QW_OTHER]
     * MutableList<String>  表示 SD卡上的路径
     */
    var wechatFilePathArrayMap: ArrayMap<Int, MutableList<String>>? = null


    /**
     * 方便java同学调用
     */
    class Build {

        private var qwData = ZFileQWData()

        /**
         * 显示的标题 ，空使用默认  size必须为 [QW_SIZE]
         * @param titles Array<String>?     标题
         */
        fun titles(titles: Array<String>?): Build {
            qwData.titles = titles
            return this
        }

        /**
         * QQ、Wechat 文件过滤规则的 Map ，空使用默认
         * @param filterArray ArrayMap<Int, Array<String>>?
         * Int             表示 文件类型 [ZFILE_QW_PIC]、[ZFILE_QW_MEDIA]、[ZFILE_QW_DOCUMENT]、[ZFILE_QW_OTHER]
         * Array<String>   表示 过滤规则
         */
        fun filterArrayMap(filterArray: ArrayMap<Int, Array<String>>?): Build {
            qwData.filterArrayMap = filterArray
            return this
        }

        /**
         * QQ 保存至本地SD卡上路径的 Map ，空使用默认
         *  @param qqFilePathArray ArrayMap<Int, MutableList<String>>?
         * Int                  表示 文件类型 see[ZFILE_QW_PIC]、[ZFILE_QW_MEDIA]、[ZFILE_QW_DOCUMENT]、[ZFILE_QW_OTHER]
         * MutableList<String>  表示 SD卡上的路径
         */
        fun qqFilePathArrayMap(qqFilePathArray: ArrayMap<Int, MutableList<String>>?): Build {
            qwData.qqFilePathArrayMap = qqFilePathArray
            return this
        }

        /**
         * Wechat 保存至本地SD卡上路径的 Map ，空使用默认
         *  @param wechatFilePathArray ArrayMap<Int, MutableList<String>>?
         * Int                  表示 文件类型 see[ZFILE_QW_PIC]、[ZFILE_QW_MEDIA]、[ZFILE_QW_DOCUMENT]、[ZFILE_QW_OTHER]
         * MutableList<String>  表示 SD卡上的路径
         */
        fun wechatFilePathArrayMap(wechatFilePathArray: ArrayMap<Int, MutableList<String>>?): Build {
            qwData.wechatFilePathArrayMap = wechatFilePathArray
            return this
        }

        /**
         * 构建 [ZFileQWData]
         */
        fun build() = qwData
    }

}