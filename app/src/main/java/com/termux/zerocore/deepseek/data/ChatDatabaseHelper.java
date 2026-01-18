package com.termux.zerocore.deepseek.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chat.db";
    private static final int DATABASE_VERSION = 2;

    // 表和列名
    private static final String TABLE_CHAT_SESSIONS = "chat_sessions";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_SESSION_ID = "session_id";
    private static final String COLUMN_SESSION_NAME = "session_name";
    private static final String COLUMN_CREATED_AT = "created_at";

    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_MESSAGE_ID = "_id";
    private static final String COLUMN_SESSION_REF_ID = "session_id";
    private static final String COLUMN_MESSAGE_TEXT = "message_text";
    private static final String COLUMN_IS_USER = "is_user"; // 是否是用户发送的消息
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_AVATAR_RES_ID = "avatar_res_id";

    public ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建会话表
        db.execSQL("CREATE TABLE " + TABLE_CHAT_SESSIONS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_SESSION_ID + " TEXT,"
            + COLUMN_SESSION_NAME + " TEXT,"
            + COLUMN_CREATED_AT + " INTEGER" + ");");

        // 创建消息表
        db.execSQL("CREATE TABLE " + TABLE_MESSAGES + "("
            + COLUMN_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_SESSION_REF_ID + " TEXT,"
            + COLUMN_MESSAGE_TEXT + " TEXT,"
            + COLUMN_IS_USER + " INTEGER," // 1 for user, 0 for bot
            + COLUMN_TIMESTAMP + " INTEGER,"
            + COLUMN_AVATAR_RES_ID + " INTEGER" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // 升级到版本2：添加消息表
            db.execSQL("CREATE TABLE " + TABLE_MESSAGES + "("
                + COLUMN_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_SESSION_REF_ID + " TEXT,"
                + COLUMN_MESSAGE_TEXT + " TEXT,"
                + COLUMN_IS_USER + " INTEGER,"
                + COLUMN_TIMESTAMP + " INTEGER,"
                + COLUMN_AVATAR_RES_ID + " INTEGER" + ");");
        }
    }

    // 插入一个新的会话
    public void insertSession(String sessionId, String sessionName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SESSION_ID, sessionId);
        values.put(COLUMN_SESSION_NAME, sessionName);
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        db.insert(TABLE_CHAT_SESSIONS, null, values);
    }

    // 更新会话名称
    public boolean updateSession(String sessionId, String newSessionName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SESSION_NAME, newSessionName);

        int rowsAffected = db.update(TABLE_CHAT_SESSIONS, values,
            COLUMN_SESSION_ID + " = ?",
            new String[]{sessionId});
        return rowsAffected > 0;
    }

    // 删除一个会话及其所有消息
    public boolean deleteSession(String sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 开始事务
        db.beginTransaction();
        try {
            // 删除会话的所有消息
            int messagesDeleted = db.delete(TABLE_MESSAGES,
                COLUMN_SESSION_REF_ID + " = ?",
                new String[]{sessionId});

            // 删除会话本身
            int sessionsDeleted = db.delete(TABLE_CHAT_SESSIONS,
                COLUMN_SESSION_ID + " = ?",
                new String[]{sessionId});

            // 标记事务成功
            db.setTransactionSuccessful();

            return sessionsDeleted > 0;
        } finally {
            // 结束事务
            db.endTransaction();
        }
    }

    // 批量删除多个会话
    public boolean deleteSessions(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        // 开始事务
        db.beginTransaction();
        try {
            // 为每个会话ID构建占位符
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < sessionIds.size(); i++) {
                placeholders.append("?");
                if (i < sessionIds.size() - 1) {
                    placeholders.append(",");
                }
            }

            // 删除这些会话的所有消息
            int messagesDeleted = db.delete(TABLE_MESSAGES,
                COLUMN_SESSION_REF_ID + " IN (" + placeholders.toString() + ")",
                sessionIds.toArray(new String[0]));

            // 删除这些会话本身
            int sessionsDeleted = db.delete(TABLE_CHAT_SESSIONS,
                COLUMN_SESSION_ID + " IN (" + placeholders.toString() + ")",
                sessionIds.toArray(new String[0]));

            // 标记事务成功
            db.setTransactionSuccessful();

            return sessionsDeleted > 0;
        } finally {
            // 结束事务
            db.endTransaction();
        }
    }

    // 根据sessionId获取会话信息
    public ChatSession getSessionById(String sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ChatSession session = null;

        Cursor cursor = db.query(TABLE_CHAT_SESSIONS, null,
            COLUMN_SESSION_ID + " = ?",
            new String[]{sessionId},
            null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String sessionName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_NAME));
            long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
            session = new ChatSession(sessionId, sessionName, createdAt);
        }

        if (cursor != null) {
            cursor.close();
        }

        return session;
    }

    // 检查会话是否存在
    public boolean sessionExists(String sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHAT_SESSIONS,
            new String[]{COLUMN_SESSION_ID},
            COLUMN_SESSION_ID + " = ?",
            new String[]{sessionId},
            null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();

        return exists;
    }

    // 获取所有会话列表
    public List<ChatSession> getAllSessions() {
        List<ChatSession> sessions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHAT_SESSIONS, null, null, null, null, null, COLUMN_CREATED_AT + " DESC");
        while (cursor.moveToNext()) {
            String sessionId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID));
            String sessionName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_NAME));
            long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
            sessions.add(new ChatSession(sessionId, sessionName, createdAt));
        }
        cursor.close();
        return sessions;
    }

    // 插入一条消息
    public void insertMessage(String sessionId, String messageText, boolean isUser, long timestamp, int avatarResId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SESSION_REF_ID, sessionId);
        values.put(COLUMN_MESSAGE_TEXT, messageText);
        values.put(COLUMN_IS_USER, isUser ? 1 : 0); // 1 for user, 0 for bot
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_AVATAR_RES_ID, avatarResId);
        db.insert(TABLE_MESSAGES, null, values);
    }

    // 获取特定会话的所有消息
    public List<ChatMessage> getMessagesForSession(String sessionId) {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        System.out.println("查询：" + sessionId);
        Cursor cursor = db.query(TABLE_MESSAGES, null, COLUMN_SESSION_REF_ID + "=?", new String[]{sessionId}, null, null, COLUMN_TIMESTAMP + " ASC");
        System.out.println("cursor大小：" + cursor.getCount());
        while (cursor.moveToNext()) {
            String messageText = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_TEXT));
            boolean isUser = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_USER)) == 1;
            long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
            int avatarResId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AVATAR_RES_ID));
            messages.add(new ChatMessage(messageText, isUser, timestamp, avatarResId));
        }
        cursor.close();
        return messages;
    }

    // 删除特定会话的所有消息
    public int deleteMessagesForSession(String sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_MESSAGES,
            COLUMN_SESSION_REF_ID + " = ?",
            new String[]{sessionId});
    }
}
