package com.example.securefolder.ui.modules;

import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.CryptoManager;
import com.example.securefolder.utils.DatabaseHelper;
import com.example.securefolder.utils.KeyManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class VideoViewerActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private String currentFilePath;
    private String currentFileName;
    private File tempFile;
    private int fileId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_video_viewer);

        dbHelper = new DatabaseHelper(this);
        VideoView videoView = findViewById(R.id.videoView);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        Button btnUnlock = findViewById(R.id.btnUnlock);
        Button btnTrash = findViewById(R.id.btnTrash);

        currentFilePath = getIntent().getStringExtra("FILE_PATH");
        currentFileName = getIntent().getStringExtra("FILE_NAME");

        if (currentFilePath == null) { finish(); return; }

        fileId = dbHelper.getFileId(currentFileName);

        // Decrypt to Temp File
        new Thread(() -> {
            try {
                File encryptedFile = new File(currentFilePath);
                tempFile = File.createTempFile("PLAY", ".mp4", getCacheDir());

                FileInputStream fis = new FileInputStream(encryptedFile);
                FileOutputStream fos = new FileOutputStream(tempFile);

                boolean success = CryptoManager.decrypt(KeyManager.getMasterKey(), fis, fos);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (success) {
                        MediaController mediaController = new MediaController(this);
                        videoView.setMediaController(mediaController);
                        videoView.setVideoPath(tempFile.getAbsolutePath());
                        videoView.start();
                    } else {
                        Toast.makeText(this, "Decryption Failed", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(this::finish);
            }
        }).start();

        btnUnlock.setOnClickListener(v -> restoreVideo());
        btnTrash.setOnClickListener(v -> moveToTrash());
    }

    private void moveToTrash() {
        new AlertDialog.Builder(this)
                .setTitle("Move to Trash?")
                .setMessage("This video will be moved to Trash. You can restore it later.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (fileId != -1) {
                        dbHelper.setFileDeleted(fileId, true);
                        Toast.makeText(this, "Moved to Trash", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void restoreVideo() {
        new Thread(() -> {
            try {
                String originalPath = dbHelper.getOriginalPath(currentFileName);
                if (originalPath == null || originalPath.equals("Unknown_Location")) {
                    File moviesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES);
                    originalPath = new File(moviesDir, "Restored_" + currentFileName.replace(".enc", ".mp4")).getAbsolutePath();
                }

                File destFile = new File(originalPath);
                File parent = destFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();

                FileInputStream fis = new FileInputStream(currentFilePath);
                FileOutputStream fos = new FileOutputStream(destFile);
                boolean success = CryptoManager.decrypt(KeyManager.getMasterKey(), fis, fos);

                if (success) {
                    new File(currentFilePath).delete();
                    if (fileId != -1) dbHelper.deleteFileRecordPermanently(fileId);
                    MediaScannerConnection.scanFile(this, new String[]{destFile.getAbsolutePath()}, null, null);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Restored to: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Restore Failed", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete(); // Secure delete temp file
        }
    }
}