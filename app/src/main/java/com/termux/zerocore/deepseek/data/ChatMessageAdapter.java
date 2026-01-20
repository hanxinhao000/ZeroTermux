package com.termux.zerocore.deepseek.data;

import android.content.Context;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.zerocore.deepseek.markdown.MarkDownAPI;
import com.termux.zerocore.deepseek.utils.SpannableTextUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.noties.markwon.Markwon;


public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ChatMessageViewHolder> {
    private List<ChatMessage> messages;
    private Context context;

    public ChatMessageAdapter(Context context, List<ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? R.layout.item_chat_message_user : R.layout.item_chat_message_bot;
    }

    @NonNull
    @Override
    public ChatMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new ChatMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        Markwon.Builder builder = Markwon.builder(context);
        MarkDownAPI markDownAPI = MarkDownAPI.create(context);

        Markwon markwon = builder.usePlugin(markDownAPI).build();
        Spanned markdown = markwon.toMarkdown(message.getMessageText());
        Spanned finalSpanned = SpannableTextUtil.createClickableSpannableString(markdown, context);
        markwon.setParsedMarkdown(holder.messageTextView, finalSpanned);
        holder.messageTextView.setMovementMethod(LinkMovementMethod.getInstance());


        holder.timeTextView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(message.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void release() {
        MarkDownAPI.create(context).release();
    }

    public static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView timeTextView;

        public ChatMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }
    }

    // 更新特定消息的内容并通知适配器刷新
    public void updateMessageText(int position, String additionalText) {
        ChatMessage message = messages.get(position);
        message.appendMessageText(additionalText);
        notifyItemChanged(position);
    }
}
