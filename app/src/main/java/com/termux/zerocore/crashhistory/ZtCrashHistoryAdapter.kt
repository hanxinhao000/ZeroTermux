package com.termux.zerocore.crashhistory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.termux.R

class ZtCrashHistoryAdapter(
    private var items: List<ZtCrashSummary>,
    private val listener: Listener
) : RecyclerView.Adapter<ZtCrashHistoryAdapter.Holder>() {

    interface Listener {
        fun onItemClick(item: ZtCrashSummary)
        fun onDeleteClick(item: ZtCrashSummary)
    }

    fun updateData(newItems: List<ZtCrashSummary>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_zt_crash_history, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.crash_item_title)
        private val timeView: TextView = itemView.findViewById(R.id.crash_item_time)
        private val threadView: TextView = itemView.findViewById(R.id.crash_item_thread)
        private val deleteView: TextView = itemView.findViewById(R.id.crash_item_delete)

        fun bind(item: ZtCrashSummary) {
            titleView.text = item.summary
            timeView.text = ZtCrashHistoryStore.formatTime(item.timestampMs)
            threadView.text = itemView.context.getString(
                R.string.zt_crash_history_thread,
                item.threadName,
                item.appVersion
            )
            itemView.setOnClickListener { listener.onItemClick(item) }
            deleteView.setOnClickListener { listener.onDeleteClick(item) }
        }
    }
}
