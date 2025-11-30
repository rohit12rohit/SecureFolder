package com.example.securefolder.ui.modules;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
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
    private String currentFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_photo_viewer);

        dbHelper = new DatabaseHelper(this);
        ImageView imageView = findViewById(R.id.ivFullPhoto);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        Button btnUnlock = findViewById(R.id.btnUnlock);

        currentFilePath = getIntent().getStringExtra("FILE_PATH");
        currentFileName = getIntent().getStringExtra("FILE_NAME");

        if (currentFilePath == null) { finish(); return; }

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
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(this::finish);
            }
        }).start();

        // Handle Unlock/Restore
        btnUnlock.setOnClickListener(v -> restorePhoto());
    }

    private void restorePhoto() {
        new Thread(() -> {
            try {
                // 1. Get Original Path from DB
                String originalPath = dbHelper.getOriginalPath(currentFileName);
                if (originalPath == null || originalPath.equals("Unknown_Location")) {
                    // Fallback to Pictures folder if unknown
                    File picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
                    originalPath = new File(picturesDir, "Restored_" + currentFileName + ".jpg").getAbsolutePath();
                }

                // 2. Prepare Destination
                File destFile = new File(originalPath);
                File parent = destFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();

                // 3. Decrypt to Destination
                FileInputStream fis = new FileInputStream(currentFilePath);
                FileOutputStream fos = new FileOutputStream(destFile);
                boolean success = CryptoManager.decrypt(KeyManager.getMasterKey(), fis, fos);

                if (success) {
                    // 4. Delete from Vault
                    new File(currentFilePath).delete();

                    // 5. Cleanup DB
                    // We need the ID, but for now we can just mark deleted or rely on filename logic.
                    // Ideally, we execute SQL to delete by enc_name
                    dbHelper.getWritableDatabase().delete(DatabaseHelper.TABLE_FILES, DatabaseHelper.COL_ENC_NAME + "=?", new String[]{currentFileName});

                    // 6. Refresh Gallery (Important!)
                    MediaScannerConnection.scanFile(this, new String[]{destFile.getAbsolutePath()}, null, null);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Restored to: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        // Go back to Photos list
                        Intent intent = new Intent(this, PhotosActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
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