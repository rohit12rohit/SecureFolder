package com.example.securefolder.utils;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupHelper {

    public static String createSecureBackup(Context context) {
        File vaultRoot = context.getExternalFilesDir("Vault");
        File dbFile = context.getDatabasePath("SecureVault.db");
        File cacheDir = context.getCacheDir();

        // 1. Prepare Staging
        File stagingDir = new File(cacheDir, "Staging");
        if (stagingDir.exists()) deleteRecursive(stagingDir);
        stagingDir.mkdirs();

        try {
            copyFile(dbFile, new File(stagingDir, "SecureVault.db"));
            copyDirectory(vaultRoot, new File(stagingDir, "Vault"));

            // 2. Zip
            File zipFile = new File(cacheDir, "temp_backup.zip");
            zipDirectory(stagingDir, zipFile);

            // 3. Encrypt & Save using MediaStore (Visible to User)
            String backupName = "SecureVault_Backup_" + System.currentTimeMillis() + ".enc";

            Uri uri = saveToDownloads(context, backupName);
            if (uri == null) return null;

            OutputStream out = context.getContentResolver().openOutputStream(uri);
            FileInputStream fis = new FileInputStream(zipFile);

            // Encrypt directly to the output stream (Downloads folder)
            boolean success = CryptoManager.encrypt(KeyManager.getMasterKey(), fis, out);

            fis.close();
            if (out != null) out.close();

            // Cleanup
            deleteRecursive(stagingDir);
            zipFile.delete();

            return success ? "Downloads/" + backupName : null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Uri saveToDownloads(Context context, String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        return context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        }
    }

    private static void copyDirectory(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            if (!dst.exists()) dst.mkdirs();
            String[] children = src.list();
            if (children != null) {
                for (String child : children) {
                    copyDirectory(new File(src, child), new File(dst, child));
                }
            }
        } else {
            copyFile(src, dst);
        }
    }

    private static void zipDirectory(File sourceFolder, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipRecursive(sourceFolder, sourceFolder, zos);
        }
    }

    private static void zipRecursive(File rootDir, File sourceFile, ZipOutputStream zos) throws IOException {
        if (sourceFile.isDirectory()) {
            File[] files = sourceFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    zipRecursive(rootDir, file, zos);
                }
            }
        } else {
            String relativePath = sourceFile.getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1);
            ZipEntry ze = new ZipEntry(relativePath);
            zos.putNextEntry(ze);
            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
        }
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }
}