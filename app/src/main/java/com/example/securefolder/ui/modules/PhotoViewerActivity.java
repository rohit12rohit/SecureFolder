package com.example.securefolder.ui.modules;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.CryptoManager;
import com.example.securefolder.utils.DatabaseHelper;
import com.example.securefolder.utils.KeyManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class PhotoViewerActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private String currentFilePath;
    private String currentFileName; // This is now the UUID (System Name)
    private int fileId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Prevent screenshots
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_photo_viewer);

        dbHelper = new DatabaseHelper(this);
        ImageView imageView = findViewById(R.id.ivFullPhoto);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        Button btnUnlock = findViewById(R.id.btnUnlock);
        Button btnTrash = findViewById(R.id.btnTrash);

        currentFilePath = getIntent().getStringExtra("FILE_PATH");
        currentFileName = getIntent().getStringExtra("FILE_NAME");

        if (currentFilePath == null) { finish(); return; }

        // FIX: Use new method for UUID lookup
        fileId = dbHelper.getFileIdBySystemName(currentFileName);

        // Decrypt and Show
        new Thread(() -> {
            try {
                File file = new File(currentFilePath);
                FileInputStream fis = new FileInputStream(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                boolean success = CryptoManager.decrypt(KeyManager.getMasterKey(), fis, baos);

                if (success) {
                    byte[] imageData = baos.toByteArray();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        imageView.setImageBitmap(bitmap);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Decryption Failed", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(this::finish);
            }
        }).start();

        btnUnlock.setOnClickListener(v -> restorePhoto());
        btnTrash.setOnClickListener(v -> moveToTrash());
    }

    private void moveToTrash() {
        new AlertDialog.Builder(this)
                .setTitle("Move to Trash?")
                .setMessage("This file will be moved to Trash. You can restore it later.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (fileId != -1) {
                        dbHelper.setFileDeleted(fileId, true);
                        Toast.makeText(this, "Moved to Trash", Toast.LENGTH_SHORT).show();
                        finish(); // Close viewer
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void restorePhoto() {
        new Thread(() -> {
            try {
                // 1. Get Real Name from DB (Since file on disk is just a UUID)
                String realName = dbHelper.getDisplayName(currentFileName);
                if (realName == null || realName.equals("Unknown")) {
                    realName = "Restored_Img_" + System.currentTimeMillis() + ".jpg";
                } else {
                    realName = "Restored_" + realName;
                }

                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File destFile = new File(picturesDir, realName);

                // 2. Decrypt to Destination
                FileInputStream fis = new FileInputStream(currentFilePath);
                FileOutputStream fos = new FileOutputStream(destFile);
                boolean success = CryptoManager.decrypt(KeyManager.getMasterKey(), fis, fos);

                if (success) {
                    // 3. Delete from Vault (Disk)
                    new File(currentFilePath).delete();

                    // 4. Delete from DB (Permanently removed)
                    if (fileId != -1) {
                        dbHelper.deleteFileRecordPermanently(fileId);
                    }

                    // 5. Refresh Gallery
                    MediaScannerConnection.scanFile(this, new String[]{destFile.getAbsolutePath()}, null, null);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Restored to Pictures: " + destFile.getName(), Toast.LENGTH_LONG).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Restore Failed", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}