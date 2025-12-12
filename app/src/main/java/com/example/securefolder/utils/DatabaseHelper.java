package com.example.securefolder.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "SecureVault.db";
    // INCREMENT VERSION TO FORCE UPDATE
    private static final int DB_VERSION = 5;

    // Table: Files
    public static final String TABLE_FILES = "files";
    public static final String COL_ID = "id";
    public static final String COL_FILE_TYPE = "type"; // "PHOTO", "VIDEO", "DOC"
    public static final String COL_SYSTEM_NAME = "system_name"; // UUID.bin (On Disk)
    public static final String COL_DISPLAY_NAME = "display_name"; // "MyPic.jpg" (In App)
    public static final String COL_ORIG_PATH = "orig_path";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_IS_DELETED = "is_deleted";

    // Table: Notes & Passwords (Unchanged for now)
    public static final String TABLE_NOTES = "notes";
    public static final String TABLE_PASSWORDS = "passwords";
    // ... define columns for notes/passwords as before ...
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_APP_NAME = "app_name";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_FILES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FILE_TYPE + " TEXT, " +
                COL_SYSTEM_NAME + " TEXT, " +
                COL_DISPLAY_NAME + " TEXT, " +
                COL_ORIG_PATH + " TEXT, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_IS_DELETED + " INTEGER DEFAULT 0)");

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For development, just drop and recreate
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FILES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PASSWORDS);
        onCreate(db);
    }

    // --- FILES OPERATIONS ---
    public void addFile(String type, String systemName, String displayName, String origPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FILE_TYPE, type);
        values.put(COL_SYSTEM_NAME, systemName);
        values.put(COL_DISPLAY_NAME, displayName);
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

    public int getFileIdBySystemName(String sysName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_ID + " FROM " + TABLE_FILES + " WHERE " + COL_SYSTEM_NAME + "=?", new String[]{sysName});
        int id = -1;
        if (cursor != null) {
            if (cursor.moveToFirst()) id = cursor.getInt(0);
            cursor.close();
        }
        return id;
    }

    // Get Original Path for Restore
    public String getOriginalPath(String sysName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_ORIG_PATH + " FROM " + TABLE_FILES + " WHERE " + COL_SYSTEM_NAME + "=?", new String[]{sysName});
        if (cursor != null && cursor.moveToFirst()) {
            String path = cursor.getString(0);
            cursor.close();
            return path;
        }
        return null;
    }

    // Get Display Name (Real filename)
    public String getDisplayName(String sysName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_DISPLAY_NAME + " FROM " + TABLE_FILES + " WHERE " + COL_SYSTEM_NAME + "=?", new String[]{sysName});
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(0);
            cursor.close();
            return name;
        }
        return "Unknown";
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

    // --- NOTES & PASSWORDS (CRUD) ---
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
        return db.query(TABLE_NOTES, null, COL_IS_DELETED + "=?", new String[]{showDeleted?"1":"0"}, null, null, COL_TIMESTAMP + " DESC");
    }

    public void addPassword(String app, String user, String pass) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_APP_NAME, app);
        values.put(COL_USERNAME, user);
        values.put(COL_PASSWORD, pass);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        values.put(COL_IS_DELETED, 0);
        db.insert(TABLE_PASSWORDS, null, values);
        db.close();
    }

    public void updatePassword(int id, String app, String user, String pass) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_APP_NAME, app);
        values.put(COL_USERNAME, user);
        values.put(COL_PASSWORD, pass);
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
        return db.query(TABLE_PASSWORDS, null, COL_IS_DELETED + "=?", new String[]{showDeleted?"1":"0"}, null, null, COL_TIMESTAMP + " DESC");
    }
}