package com.termux.zerocore.deepseek.data;

public class ChatMessage {
    private String messageText;
    private boolean isUser; // True if the message is from the user, false otherwise.
    private long timestamp;
    private int avatarResId;

    public ChatMessage(String messageText, boolean isUser, long timestamp, int avatarResId) {
        this.messageText = messageText;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.avatarResId = avatarResId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void appendMessageText(String additionalText) {
        this.messageText += additionalText;
    }

    public boolean isUser() {
        return isUser;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getAvatarResId() {
        return avatarResId;
    }
}
