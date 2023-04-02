package com.zp.z_file.content

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

/**
 * 文件 实体类
 * @property fileName String        文件名
 * @property isFile Boolean         true---文件；false---文件夹
 * @property filePath String        文件路径
 * @property date String            格式化后的时间
 * @property originalDate String    原始时间（时间戳）
 * @property size String            格式化后的大小
 * @property originaSize Long       原始大小（byte）
 * @property parent String?         父级所包含的选择文件个数（自定义文件获取不需要给该字段赋值）
 */

data class ZFileBean(
    var fileName: String = "",
    var isFile: Boolean = true,
    var filePath: String = "",
    var date: String = "",
    var originalDate: String = "",
    var size: String = "",
    var originaSize: Long = 0L,
    var parent: String? = ""
) : Serializable, Parcelable {
    constructor(parcel: Parcel) : this(
        fileName = parcel.readString() ?: "",
        isFile = parcel.readInt() == 1,
        filePath = parcel.readString() ?: "",
        date = parcel.readString() ?: "",
        originalDate = parcel.readString() ?: "",
        size = parcel.readString() ?: "",
        originaSize = parcel.readLong(),
        parent = parcel.readString()
    )

    override fun describeContents(): Int = 0


    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(fileName)
        dest.writeInt(if (isFile) 1 else 0)
        dest.writeString(filePath)
        dest.writeString(date)
        dest.writeString(originalDate)
        dest.writeString(size)
        dest.writeLong(originaSize)
        dest.writeString(parent)

    }

    companion object CREATOR : Parcelable.Creator<ZFileBean> {

        override fun createFromParcel(parcel: Parcel): ZFileBean {
            return ZFileBean(parcel)
        }

        override fun newArray(size: Int): Array<ZFileBean?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * 文件夹 标签/角标、说明文字 实体类
 * @property folderPath String          文件夹路径
 * @property folderHint String          说明，空不展示
 * @property folderBadgeIcon Int        标签/角标 资源文件路径
 * @property folderBadgeType Int        类型 0：不展示；1：展示
 * @constructor
 */
data class ZFileFolderBadgeHintBean(
    var folderPath: String = "",
    var folderHint: String = "",
    var folderBadgeIcon: Int = -1,
    var folderBadgeType: Int = 0
): Serializable, Parcelable {

    constructor(parcel: Parcel) : this(
        folderPath = parcel.readString() ?: "",
        folderHint = parcel.readString() ?: "",
        folderBadgeIcon = parcel.readInt(),
        folderBadgeType = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(folderPath)
        parcel.writeString(folderHint)
        parcel.writeInt(folderBadgeIcon)
        parcel.writeInt(folderBadgeType)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ZFileFolderBadgeHintBean> {
        override fun createFromParcel(parcel: Parcel): ZFileFolderBadgeHintBean {
            return ZFileFolderBadgeHintBean(parcel)
        }

        override fun newArray(size: Int): Array<ZFileFolderBadgeHintBean?> {
            return arrayOfNulls(size)
        }
    }

}

// inner ===========================================================================================

/**
 * 媒体、图片文件详情
 * @property duration String    媒体时长
 * @property width String       宽
 * @property height String      高
 */
internal data class ZFileInfoBean(
    var duration: String = "",
    var width: String = "",
    var height: String = ""
)

/**
 * 文件路径 实体
 * @property fileName String    路径名称
 * @property filePath String    文件路径
 * @constructor
 */
internal data class ZFilePathBean(
    var fileName: String = "",
    var filePath: String = ""
)

/**
 * QQ、Wechat 文件选择实体类
 */
internal data class ZFileQWBean(
    var zFileBean: ZFileBean? = null,
    var isSelected: Boolean = true
)



