package com.termux.zerocore.deepseek.data;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.deepseek.DeepSeekTransitFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.ChatSessionViewHolder> {
    private static final String TAG = ChatSessionAdapter.class.getSimpleName();
    private Context context;
    private List<ChatSession> sessions;
    private ChatDatabaseHelper dbHelper;
    private DeepSeekTransitFragment mDeepSeekTransitFragment;

    public ChatSessionAdapter(Context context, List<ChatSession> sessions, DeepSeekTransitFragment deepSeekTransitFragment) {
        this.context = context;
        this.sessions = sessions;
        this.dbHelper = new ChatDatabaseHelper(context);
        this.mDeepSeekTransitFragment = deepSeekTransitFragment;
    }

    @NonNull
    @Override
    public ChatSessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_session, parent, false);
        return new ChatSessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatSessionViewHolder holder, int position) {
        ChatSession session = sessions.get(position);
        holder.textView.setText(session.getSessionName());

        // 格式化并显示日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedDate = sdf.format(new Date(session.getCreatedAt()));
        holder.dateView.setText(formattedDate);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("sessionId", session.getSessionId());
            mDeepSeekTransitFragment.switchFragment(1, intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            showEditDialog(session);
            return true;
        });
        holder.deleteSession.setOnClickListener(view -> {
            LogUtils.e(TAG, "onBindViewHolder click session Id: " + session.getSessionId());
            dbHelper.deleteSession(session.getSessionId());
            sessions.remove(position);
            notifyDataSetChanged();
        });
    }

    private void showEditDialog(ChatSession session) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(UUtils.getString(R.string.deepseek_settings_ai_session_name));

        final EditText input = new EditText(context);
        input.setText(session.getSessionName());
        builder.setView(input);

        builder.setPositiveButton(UUtils.getString(R.string.确定), (dialog, which) -> {
            String newName = input.getText().toString();
            session.setSessionName(newName);
            dbHelper.updateSession(session.getSessionId(), session.getSessionName());
            notifyDataSetChanged();
        });

        builder.setNegativeButton(R.string.取消, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    public void setSessions(List<ChatSession> sessions) {
        this.sessions = sessions;
        notifyDataSetChanged();
    }

    public static class ChatSessionViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        TextView dateView; // 新增日期视图
        ImageView deleteSession;

        public ChatSessionViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.sessionName);
            dateView = itemView.findViewById(R.id.sessionDate); // 绑定日期视图
            deleteSession = itemView.findViewById(R.id.delete_session); // 绑定日期视图
        }
    }

    public void release() {
        mDeepSeekTransitFragment = null;
    }
}
