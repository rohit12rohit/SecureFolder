package com.example.securefolder.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "SecureVault.db";
    private static final int DB_VERSION = 4;

    // Table: Notes
    public static final String TABLE_NOTES = "notes";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_IS_DELETED = "is_deleted";

    // Table: Passwords
    public static final String TABLE_PASSWORDS = "passwords";
    public static final String COL_APP_NAME = "app_name";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    // Table: Files (Photos/Videos)
    public static final String TABLE_FILES = "files";
    public static final String COL_FILE_TYPE = "type"; // "PHOTO" or "VIDEO"
    public static final String COL_ENC_NAME = "enc_name";
    public static final String COL_ORIG_PATH = "orig_path";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NOTES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_CONTENT + " TEXT, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_IS_DELETED + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_PASSWORDS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_APP_NAME + " TEXT, " +
                COL_USERNAME + " TEXT, " +
                COL_PASSWORD + " TEXT, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_IS_DELETED + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_FILES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FILE_TYPE + " TEXT, " +
                COL_ENC_NAME + " TEXT, " +
                COL_ORIG_PATH + " TEXT, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_IS_DELETED + " INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PASSWORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FILES);
        onCreate(db);
    }

    // --- HELPER FOR IDS ---
    public int getFileId(String encName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_ID + " FROM " + TABLE_FILES + " WHERE " + COL_ENC_NAME + "=?", new String[]{encName});
        int id = -1;
        if (cursor != null) {
            if (cursor.moveToFirst()) id = cursor.getInt(0);
            cursor.close();
        }
        return id;
    }

    // --- FILES OPERATIONS ---
    public void addFile(String type, String encName, String origPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FILE_TYPE, type);
        values.put(COL_ENC_NAME, encName);
        values.put(COL_ORIG_PATH, origPath);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        values.put(COL_IS_DELETED, 0);
        db.insert(TABLE_FILES, null, values);
        db.close();
    }

    public Cursor getActiveFiles(String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_FILES + " WHERE " + COL_FILE_TYPE + "=? AND " + COL_IS_DELETED + "=0 ORDER BY " + COL_TIMESTAMP + " DESC", new String[]{type});
    }

    public Cursor getDeletedFiles() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_FILES + " WHERE " + COL_IS_DELETED + "=1 ORDER BY " + COL_TIMESTAMP + " DESC", null);
    }

    public String getOriginalPath(String encName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_ORIG_PATH + " FROM " + TABLE_FILES + " WHERE " + COL_ENC_NAME + "=?", new String[]{encName});
        if (cursor != null && cursor.moveToFirst()) {
            String path = cursor.getString(0);
            cursor.close();
            return path;
        }
        return null;
    }

    public void setFileDeleted(int id, boolean deleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IS_DELETED, deleted ? 1 : 0);
        db.update(TABLE_FILES, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteFileRecordPermanently(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FILES, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // --- NOTES OPERATIONS ---
    public void addNote(String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_CONTENT, content);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        values.put(COL_IS_DELETED, 0);
        db.insert(TABLE_NOTES, null, values);
        db.close();
    }

    public void updateNote(int id, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_CONTENT, content);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        db.update(TABLE_NOTES, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void setNoteDeleted(int id, boolean deleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IS_DELETED, deleted ? 1 : 0);
        db.update(TABLE_NOTES, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteNotePermanently(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public Cursor getAllNotes(boolean showDeleted) {
        SQLiteDatabase db = this.getReadableDatabase();
        String where = COL_IS_DELETED + "=?";
        String[] args = { showDeleted ? "1" : "0" };
        return db.query(TABLE_NOTES, null, where, args, null, null, COL_TIMESTAMP + " DESC");
    }

    // --- PASSWORDS OPERATIONS ---
    public void addPassword(String appName, String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_APP_NAME, appName);
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        values.put(COL_IS_DELETED, 0);
        db.insert(TABLE_PASSWORDS, null, values);
        db.close();
    }

    public void updatePassword(int id, String appName, String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_APP_NAME, appName);
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        db.update(TABLE_PASSWORDS, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void setPasswordDeleted(int id, boolean deleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IS_DELETED, deleted ? 1 : 0);
        db.update(TABLE_PASSWORDS, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deletePasswordPermanently(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PASSWORDS, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public Cursor getAllPasswords(boolean showDeleted) {
        SQLiteDatabase db = this.getReadableDatabase();
        String where = COL_IS_DELETED + "=?";
        String[] args = { showDeleted ? "1" : "0" };
        return db.query(TABLE_PASSWORDS, null, where, args, null, null, COL_TIMESTAMP + " DESC");
    }
}