package com.termux.zerocore.ai.llm.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.SaveData;
import com.google.gson.Gson;
import com.termux.zerocore.ai.model.ProviderProfile;
import com.termux.zerocore.bean.ZTUserBean;

import java.util.ArrayList;
import java.util.List;

public class ChatDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = ChatDatabaseHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "custom_chat.db";
    private static final int DATABASE_VERSION = 3;

    // 表和列名
    private static final String TABLE_CHAT_SESSIONS = "custom_chat_sessions";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_SESSION_ID = "session_id";
    private static final String COLUMN_SESSION_NAME = "session_name";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_PROVIDER_ID = "provider_id";

    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_MESSAGE_ID = "_id";
    private static final String COLUMN_SESSION_REF_ID = "session_id";
    private static final String COLUMN_MESSAGE_TEXT = "message_text";
    private static final String COLUMN_IS_USER = "is_user"; // 是否是用户发送的消息
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_AVATAR_RES_ID = "avatar_res_id";

    private static final String TABLE_AI_PROVIDERS = "ai_providers";
    private static final String COLUMN_PROVIDER_NAME = "name";
    private static final String COLUMN_FORMAT_TYPE = "format_type";
    private static final String COLUMN_API_URL = "api_url";
    private static final String COLUMN_API_KEY = "api_key";
    private static final String COLUMN_MODEL_NAME = "model_name";
    private static final String COLUMN_IS_DEFAULT = "is_default";

    public ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建会话表 (v3 schema)
        db.execSQL("CREATE TABLE " + TABLE_CHAT_SESSIONS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_SESSION_ID + " TEXT,"
            + COLUMN_SESSION_NAME + " TEXT,"
            + COLUMN_CREATED_AT + " INTEGER,"
            + COLUMN_PROVIDER_ID + " INTEGER" + ");");

        // 创建消息表
        db.execSQL("CREATE TABLE " + TABLE_MESSAGES + "("
            + COLUMN_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_SESSION_REF_ID + " TEXT,"
            + COLUMN_MESSAGE_TEXT + " TEXT,"
            + COLUMN_IS_USER + " INTEGER," // 1 for user, 0 for bot
            + COLUMN_TIMESTAMP + " INTEGER,"
            + COLUMN_AVATAR_RES_ID + " INTEGER" + ");");

        // Index for message loading performance
        db.execSQL("CREATE INDEX idx_messages_session ON " + TABLE_MESSAGES
            + "(" + COLUMN_SESSION_REF_ID + ", " + COLUMN_TIMESTAMP + ")");

        // 创建AI提供者表
        db.execSQL("CREATE TABLE " + TABLE_AI_PROVIDERS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_PROVIDER_NAME + " TEXT NOT NULL,"
            + COLUMN_FORMAT_TYPE + " TEXT NOT NULL DEFAULT 'openai',"
            + COLUMN_API_URL + " TEXT NOT NULL,"
            + COLUMN_API_KEY + " TEXT DEFAULT '',"
            + COLUMN_MODEL_NAME + " TEXT NOT NULL,"
            + COLUMN_IS_DEFAULT + " INTEGER DEFAULT 0" + ");");

        // Insert default provider
        insertDefaultProvider(db, readApiKeyFromSharedPrefs());
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
        if (oldVersion < 3) {
            // 升级到版本3：添加AI提供者表和相关列
            upgradeToV3(db);
        }
    }

    private void upgradeToV3(SQLiteDatabase db) {
        // Create ai_providers table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AI_PROVIDERS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_PROVIDER_NAME + " TEXT NOT NULL,"
            + COLUMN_FORMAT_TYPE + " TEXT NOT NULL DEFAULT 'openai',"
            + COLUMN_API_URL + " TEXT NOT NULL,"
            + COLUMN_API_KEY + " TEXT DEFAULT '',"
            + COLUMN_MODEL_NAME + " TEXT NOT NULL,"
            + COLUMN_IS_DEFAULT + " INTEGER DEFAULT 0" + ");");

        // Add provider_id column to chat_sessions
        db.execSQL("ALTER TABLE " + TABLE_CHAT_SESSIONS
            + " ADD COLUMN " + COLUMN_PROVIDER_ID + " INTEGER");

        // Create index for message loading performance
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_session ON " + TABLE_MESSAGES
            + "(" + COLUMN_SESSION_REF_ID + ", " + COLUMN_TIMESTAMP + ")");

        // Migrate existing API key from SharedPreferences (bypass UserSetManage singleton)
        String existingApiKey = readApiKeyFromSharedPrefs();

        // Insert default provider with migrated API key
        long defaultProviderId = insertDefaultProvider(db, existingApiKey);

        // Update all existing sessions to use the default provider
        if (defaultProviderId > 0) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_PROVIDER_ID, defaultProviderId);
            db.update(TABLE_CHAT_SESSIONS, values,
                COLUMN_PROVIDER_ID + " IS NULL", null);
        }
    }

    /**
     * Read API key directly from SharedPreferences via SaveData,
     * bypassing UserSetManage singleton to avoid NPE during database upgrade.
     */
    private String readApiKeyFromSharedPrefs() {
        try {
            String json = SaveData.INSTANCE.getStringOther("zero_termux_user_bean");
            if (json != null && !json.isEmpty() && !"def".equals(json)) {
                ZTUserBean bean = new Gson().fromJson(json, ZTUserBean.class);
                if (bean != null && bean.getCustomApiKey() != null) {
                    return bean.getCustomApiKey();
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to read API key from SharedPreferences: " + e);
        }
        return "";
    }

    /**
     * Insert the default provider and return the inserted row ID.
     */
    private long insertDefaultProvider(SQLiteDatabase db, String apiKey) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVIDER_NAME, "DeepSeek");
        values.put(COLUMN_FORMAT_TYPE, "openai");
        values.put(COLUMN_API_URL, "https://api.deepseek.com/chat/completions");
        values.put(COLUMN_API_KEY, apiKey != null ? apiKey : "");
        values.put(COLUMN_MODEL_NAME, "deepseek-chat");
        values.put(COLUMN_IS_DEFAULT, 1);
        return db.insert(TABLE_AI_PROVIDERS, null, values);
    }

    // ========================= Provider CRUD =========================

    public long insertProvider(ProviderProfile profile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVIDER_NAME, profile.getName());
        values.put(COLUMN_FORMAT_TYPE, profile.getFormatType());
        values.put(COLUMN_API_URL, profile.getApiUrl());
        values.put(COLUMN_API_KEY, profile.getApiKey());
        values.put(COLUMN_MODEL_NAME, profile.getModelName());
        values.put(COLUMN_IS_DEFAULT, profile.isDefault() ? 1 : 0);
        return db.insert(TABLE_AI_PROVIDERS, null, values);
    }

    public boolean updateProvider(ProviderProfile profile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVIDER_NAME, profile.getName());
        values.put(COLUMN_FORMAT_TYPE, profile.getFormatType());
        values.put(COLUMN_API_URL, profile.getApiUrl());
        values.put(COLUMN_API_KEY, profile.getApiKey());
        values.put(COLUMN_MODEL_NAME, profile.getModelName());
        values.put(COLUMN_IS_DEFAULT, profile.isDefault() ? 1 : 0);
        int rows = db.update(TABLE_AI_PROVIDERS, values,
            COLUMN_ID + " = ?", new String[]{String.valueOf(profile.getId())});
        return rows > 0;
    }

    public boolean updateDefaultProviderApiKey(String apiKey) {
        ProviderProfile defaultProvider = getDefaultProvider();
        if (defaultProvider == null) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_API_KEY, apiKey != null ? apiKey : "");
        int rows = db.update(TABLE_AI_PROVIDERS, values,
            COLUMN_ID + " = ?", new String[]{String.valueOf(defaultProvider.getId())});
        return rows > 0;
    }

    public boolean deleteProvider(long providerId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_AI_PROVIDERS,
            COLUMN_ID + " = ?", new String[]{String.valueOf(providerId)});
        return rows > 0;
    }

    public List<ProviderProfile> getAllProviders() {
        List<ProviderProfile> providers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_AI_PROVIDERS, null, null, null,
            null, null, COLUMN_IS_DEFAULT + " DESC, " + COLUMN_ID + " ASC");
        while (cursor.moveToNext()) {
            providers.add(cursorToProvider(cursor));
        }
        cursor.close();
        return providers;
    }

    public ProviderProfile getProviderById(long providerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_AI_PROVIDERS, null,
            COLUMN_ID + " = ?", new String[]{String.valueOf(providerId)},
            null, null, null);
        ProviderProfile provider = null;
        if (cursor != null && cursor.moveToFirst()) {
            provider = cursorToProvider(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
        return provider;
    }

    public ProviderProfile getDefaultProvider() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_AI_PROVIDERS, null,
            COLUMN_IS_DEFAULT + " = 1", null,
            null, null, null, "1");
        ProviderProfile provider = null;
        if (cursor != null && cursor.moveToFirst()) {
            provider = cursorToProvider(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
        // Fallback: return first provider if no default set
        if (provider == null) {
            cursor = db.query(TABLE_AI_PROVIDERS, null, null, null,
                null, null, COLUMN_ID + " ASC", "1");
            if (cursor != null && cursor.moveToFirst()) {
                provider = cursorToProvider(cursor);
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        return provider;
    }

    public int getProviderCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_AI_PROVIDERS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public void setDefaultProvider(long providerId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Clear all defaults
            ContentValues clearValues = new ContentValues();
            clearValues.put(COLUMN_IS_DEFAULT, 0);
            db.update(TABLE_AI_PROVIDERS, clearValues, null, null);
            // Set the new default
            ContentValues setValues = new ContentValues();
            setValues.put(COLUMN_IS_DEFAULT, 1);
            db.update(TABLE_AI_PROVIDERS, setValues, COLUMN_ID + " = ?",
                new String[]{String.valueOf(providerId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ProviderProfile cursorToProvider(Cursor cursor) {
        return new ProviderProfile(
            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROVIDER_NAME)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FORMAT_TYPE)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_API_URL)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_API_KEY)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MODEL_NAME)),
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DEFAULT)) == 1
        );
    }

    // ========================= Session Methods =========================

    // 插入一个新的会话
    public void insertSession(String sessionId, String sessionName) {
        insertSession(sessionId, sessionName, 0);
    }

    public void insertSession(String sessionId, String sessionName, long providerId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SESSION_ID, sessionId);
        values.put(COLUMN_SESSION_NAME, sessionName);
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        if (providerId > 0) {
            values.put(COLUMN_PROVIDER_ID, providerId);
        }
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

    // 更新会话的提供者
    public boolean updateSessionProvider(String sessionId, long providerId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVIDER_ID, providerId);
        int rows = db.update(TABLE_CHAT_SESSIONS, values,
            COLUMN_SESSION_ID + " = ?", new String[]{sessionId});
        return rows > 0;
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
            session = cursorToSession(cursor);
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
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.query(TABLE_CHAT_SESSIONS, null, null, null, null, null, COLUMN_CREATED_AT + " DESC");
            while (cursor.moveToNext()) {
                sessions.add(cursorToSession(cursor));
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sessions;
    }

    private ChatSession cursorToSession(Cursor cursor) {
        String sessionId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID));
        String sessionName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_NAME));
        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
        long providerId = 0;
        int providerIdx = cursor.getColumnIndex(COLUMN_PROVIDER_ID);
        if (providerIdx >= 0 && !cursor.isNull(providerIdx)) {
            providerId = cursor.getLong(providerIdx);
        }
        return new ChatSession(sessionId, sessionName, createdAt, providerId);
    }

    // ========================= Message Methods =========================

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
        Cursor cursor = db.query(TABLE_MESSAGES, null, COLUMN_SESSION_REF_ID + "=?", new String[]{sessionId}, null, null, COLUMN_TIMESTAMP + " ASC");
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
