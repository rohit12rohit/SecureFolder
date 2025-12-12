package com.example.securefolder.utils;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupHelper {

    /**
     * Creates a Secure Backup of the entire Vault.
     * 1. Zips the Vault directory (files are already encrypted).
     * 2. Encrypts the ZIP file itself to protect the database and structure.
     * 3. Saves to Downloads folder.
     */
    public static String createSecureBackup(Context context) {
        File vaultRoot = context.getExternalFilesDir("Vault");
        File dbFile = context.getDatabasePath("SecureVault.db");
        File cacheDir = context.getCacheDir();

        // 1. Prepare Staging Area
        File stagingDir = new File(cacheDir, "Staging");
        if (stagingDir.exists()) deleteRecursive(stagingDir);
        stagingDir.mkdirs();

        try {
            // Copy Database (Critical for mapping filenames)
            copyFile(dbFile, new File(stagingDir, "SecureVault.db"));

            // Copy Vault Files (Photos, Videos, etc)
            // They are already encrypted, but we copy them to zip them together.
            copyDirectory(vaultRoot, new File(stagingDir, "Vault"));

            // 2. Zip the Staging Directory
            File zipFile = new File(cacheDir, "temp_backup.zip");
            zipDirectory(stagingDir, zipFile);

            // 3. Encrypt the Zip File
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String backupName = "SecureVault_Backup_" + System.currentTimeMillis() + ".enc";
            File finalBackup = new File(downloadDir, backupName);

            FileInputStream fis = new FileInputStream(zipFile);
            FileOutputStream fos = new FileOutputStream(finalBackup);

            // This encrypts the entire zip with the Master Key
            boolean success = CryptoManager.encrypt(KeyManager.getMasterKey(), fis, fos);

            // Cleanup
            deleteRecursive(stagingDir);
            zipFile.delete();

            if (success) return finalBackup.getAbsolutePath();
            else return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
            // Calculate relative path
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