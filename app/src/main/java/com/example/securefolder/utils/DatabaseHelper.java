package com.example.securefolder.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "SecureVault.db";
    private static final int DB_VERSION = 1;

    // TABLE FILES
    public static final String TABLE_FILES = "files";
    public static final String COL_ID = "id";
    public static final String COL_TYPE = "type"; // PHOTO, VIDEO, DOC
    public static final String COL_SYSTEM_NAME = "system_name"; // Encrypted filename (UUID)
    public static final String COL_DISPLAY_NAME = "display_name"; // Original filename
    public static final String COL_ORIGINAL_PATH = "original_path"; // Where it came from
    public static final String COL_MIME_TYPE = "mime_type";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_IS_DELETED = "is_deleted"; // For Trash bin

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_FILES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TYPE + " TEXT, " +
                COL_SYSTEM_NAME + " TEXT UNIQUE, " +
                COL_DISPLAY_NAME + " TEXT, " +
                COL_ORIGINAL_PATH + " TEXT, " +
                COL_MIME_TYPE + " TEXT, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_IS_DELETED + " INTEGER DEFAULT 0)");

        // Add Notes/Passwords tables here later if needed, but start clean
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FILES);
        onCreate(db);
    }

    // --- CRUD OPERATIONS ---

    public long addFile(String type, String sysName, String dispName, String origPath, String mime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TYPE, type);
        values.put(COL_SYSTEM_NAME, sysName);
        values.put(COL_DISPLAY_NAME, dispName);
        values.put(COL_ORIGINAL_PATH, origPath);
        values.put(COL_MIME_TYPE, mime);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        return db.insert(TABLE_FILES, null, values);
    }

    public Cursor getFiles(String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_FILES, null,
                COL_TYPE + "=? AND " + COL_IS_DELETED + "=0",
                new String[]{type}, null, null, COL_TIMESTAMP + " DESC");
    }

    public String getOriginalName(String systemName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FILES, new String[]{COL_DISPLAY_NAME},
                COL_SYSTEM_NAME + "=?", new String[]{systemName}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(0);
            cursor.close();
            return name;
        }
        return "Unknown File";
    }

    // Used for deleting permanently or restoring
    public void deleteFileRecord(String systemName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FILES, COL_SYSTEM_NAME + "=?", new String[]{systemName});
    }
}