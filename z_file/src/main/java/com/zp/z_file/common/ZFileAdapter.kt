package com.zp.z_file.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import com.zp.z_file.content.ZFileException
import com.zp.z_file.util.ZFileLog
import java.util.LinkedHashSet

internal abstract class ZFileAdapter<T>(protected var context: Context) : RecyclerView.Adapter<ZFileViewHolder>() {

    constructor(context: Context, layoutID: Int) : this(context) {
        this.layoutID = layoutID
    }

    var itemClick: ((View, Int, T) -> Unit)? = null
    var itemLongClick: ((View, Int, T) -> Boolean)? = null
    var itemChildClick: ((View, Int, T) -> Unit)? = null

    private var layoutID = -1
    private var datas: MutableList<T> = arrayListOf()

    open fun getDatas() = datas

    open fun setDatas(list: MutableList<T>?) {
        clear()
        if (!list.isNullOrEmpty()) {
            if (datas.addAll(list)) {
                notifyDataSetChanged()
            }
        }
    }

    open fun addAll(list: MutableList<T>) {
        val oldSize = itemCount
        if (datas.addAll(list)) {
            notifyItemRangeChanged(oldSize, list.size)
        }
    }

    open fun addItem(position: Int, t: T) {
        datas.add(position, t)
        notifyItemInserted(position)
    }

    open fun setItem(position: Int, t: T) {
        if (itemCount > 0) {
            datas[position] = t
            notifyItemChanged(position)
        }
    }

    open fun remove(position: Int, changeDataNow: Boolean = true) {
        remove(position, changeDataNow, null)
    }

    open fun remove(
        position: Int,
        changeDataNow: Boolean = true,
        nullBlock: ((Boolean) -> Unit)? = null
    ) {
        if (itemCount > 0) {
            datas.removeAt(position)
            if (changeDataNow) {
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, itemCount)
            }
        }
        nullBlock?.invoke(datas.isNullOrEmpty())
    }

    open fun clear(changeDataNow: Boolean = true) {
        datas.clear()
        if (changeDataNow) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZFileViewHolder {
        val layoutRes = getLayoutID(viewType)
        if (layoutRes > 0) {
            val view = LayoutInflater.from(context).inflate(layoutRes, parent, false)
            val holder = ZFileViewHolder(view)
            bindViewClickListener(holder, viewType)
            return holder
        } else {
            throw ZFileException("adapter layoutId is not null")
        }
    }

    override fun onBindViewHolder(holder: ZFileViewHolder, position: Int) {
        bindView(holder, getItem(position), position)
    }

    override fun getItemCount() = datas.size

    fun getItem(position: Int) = datas[position]

    open fun getLayoutID(viewType: Int) = layoutID

    protected abstract fun bindView(holder: ZFileViewHolder, item: T, position: Int)

    // 参考 BRVAH
    private fun bindViewClickListener(holder: ZFileViewHolder, viewType: Int) {

        holder.setOnItemClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) {
                showLog()
                return@setOnItemClickListener
            }
            itemClick?.invoke(this, position, getItem(position))
        }

        holder.setOnItemLongClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) {
                showLog()
                return@setOnItemLongClickListener false
            }
            val item = getItem(position)
            itemLongClick?.invoke(this, position, item) ?: true
        }

        for (id in childClickViewIds) {
            holder.setOnViewClickListener(id) {
                val position = holder.adapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    showLog()
                    return@setOnViewClickListener
                }
                itemChildClick?.invoke(this, position, getItem(position))
            }
        }
    }

    private fun showLog() = ZFileLog.e("position == RecyclerView.NO_POSITION")

    private val childClickViewIds = LinkedHashSet<Int>()

    private fun getChildClickViewIds() = childClickViewIds

    protected fun addChildClickViewIds(@IdRes vararg viewIds: Int) {
        for (viewId in viewIds) {
            childClickViewIds.add(viewId)
        }
    }

}