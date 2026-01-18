package com.termux.zerocore.deepseek.data;

import java.util.List;

public class ChatSession {
    private String sessionId;
    private String sessionName;
    private long createdAt;

    public ChatSession(String sessionId, String sessionName, long createdAt) {
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.createdAt = createdAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
