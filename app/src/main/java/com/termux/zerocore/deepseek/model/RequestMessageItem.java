package com.termux.zerocore.deepseek.model;

public class RequestMessageItem {
    public String role;
    public String content;

    public RequestMessageItem() {
    }

    public RequestMessageItem(String role, String content) {
        this.role = role;
        this.content = content;
    }

    @Override
    public String toString() {
        return "{" +
                "\"role\":\"" + role + "\"," +
                "\"content\":\"" + content + "\"" +
                '}';
    }
}
