package com.termux.zerocore.activity.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.zerocore.bean.SSHDeviceBean;
import com.zp.z_file.util.LogUtils;

import java.util.List;

public class SSHAdapter extends RecyclerView.Adapter<SSHAdapter.SSHViewHolder> {
    private static final String TAG = SSHAdapter.class.getSimpleName();
    private List<SSHDeviceBean> mDataList;
    private OnSSHItemClickListener mListener;
    private OnStartDragListener mDragListener;

    public SSHAdapter(List<SSHDeviceBean> dataList) {
        this.mDataList = dataList;
    }

    public void setOnSSHItemClickListener(OnSSHItemClickListener listener) {
        this.mListener = listener;
    }

    public void setOnStartDragListener(OnStartDragListener listener) {
        this.mDragListener = listener;
    }

    @NonNull
    @Override
    public SSHViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ssh_device, parent, false);
        return new SSHViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SSHViewHolder holder, int position) {
        SSHDeviceBean bean = mDataList.get(position);
        int currentPos = position;

        holder.tvAlias.setText(bean.getAlias());
        String detail = bean.getUsername() + "@" + bean.getHost() + ":" + bean.getPort();
        LogUtils.d(TAG, "content ssh:" + detail);
        holder.tvDetail.setText(detail);

        //连接
        holder.itemRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onConnect(bean);
                }
            }
        });

        //删除
        holder.itemRoot.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mListener != null) {
                    mListener.onDelete(holder.getAdapterPosition(), bean);
                }
                return true;
            }
        });

        //拖拽
        if (holder.dragHandle != null) {
            holder.dragHandle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onEdit(holder.getAdapterPosition(), bean);
                    }
                }
            });

            holder.dragHandle.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mDragListener != null) {
                        mDragListener.onStartDrag(holder);
                    }
                    return true;
                }
            });
        }

        //编辑
        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onEdit(holder.getAdapterPosition(), bean);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDataList == null ? 0 : mDataList.size();
    }

    static class SSHViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlias;
        TextView tvDetail;
        LinearLayout itemRoot;
        ImageView btnEdit;
        View dragHandle;
        public SSHViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAlias = itemView.findViewById(R.id.tv_alias);
            tvDetail = itemView.findViewById(R.id.tv_detail);
            itemRoot = itemView.findViewById(R.id.item_root);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            dragHandle = itemView.findViewById(R.id.drag_handle);
        }
    }

    public interface OnSSHItemClickListener {
        void onConnect(SSHDeviceBean bean);
        void onDelete(int position, SSHDeviceBean bean);
        void onEdit(int position, SSHDeviceBean bean);
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }
}
