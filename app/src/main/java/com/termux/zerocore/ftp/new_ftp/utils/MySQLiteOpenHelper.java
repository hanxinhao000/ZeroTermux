package com.termux.zerocore.ftp.new_ftp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.zerocore.ftp.new_ftp.Constants;
import com.termux.zerocore.ftp.new_ftp.bean.AccountItem;


public class MySQLiteOpenHelper extends SQLiteOpenHelper {


    public MySQLiteOpenHelper(Context context) {
        super(context, Constants.SQLConsts.SQL_USERS_FILENAME, null, Constants.SQLConsts.SQL_VERSION);
    }

    /**
     * 插入或者更新AccountItem到数据库
     *
     * @param id_update 不为空则更新指定行，指定为item.id即可
     * @return 执行结果
     */
    public static long saveOrUpdateAccountItem2DB(@NonNull Context context, AccountItem item, @Nullable Long id_update) {
        SQLiteDatabase db = new MySQLiteOpenHelper(context).getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.SQLConsts.COLUMN_ACCOUNT_NAME, item.account);
        contentValues.put(Constants.SQLConsts.COLUMN_PASSWORD, item.password);
        contentValues.put(Constants.SQLConsts.COLUMN_PATH, item.path);
        contentValues.put(Constants.SQLConsts.COLUMN_WRITABLE, item.writable ? 1 : 0);
        long result;
        if (id_update == null)
            result = db.insert(Constants.SQLConsts.TABLE_NAME, null, contentValues);
        else
            result = db.update(Constants.SQLConsts.TABLE_NAME, contentValues, Constants.SQLConsts.COLUMN_ID + "=" + id_update, null);
        db.close();
        return result;
    }

    /**
     * 根据ID值删除指定行
     */
    public static long deleteRow(Context context, long id) {
        SQLiteDatabase database = new MySQLiteOpenHelper(context).getWritableDatabase();
        long result = database.delete(Constants.SQLConsts.TABLE_NAME, Constants.SQLConsts.COLUMN_ID + "=" + id, null);
        database.close();
        return result;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists " + Constants.SQLConsts.TABLE_NAME + " ("
                + Constants.SQLConsts.COLUMN_ID + " integer primary key autoincrement not null,"
                + Constants.SQLConsts.COLUMN_ACCOUNT_NAME + " text,"
                + Constants.SQLConsts.COLUMN_PASSWORD + " text,"
                + Constants.SQLConsts.COLUMN_PATH + " text,"
                + Constants.SQLConsts.COLUMN_WRITABLE + " integer not null default 0);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

}
