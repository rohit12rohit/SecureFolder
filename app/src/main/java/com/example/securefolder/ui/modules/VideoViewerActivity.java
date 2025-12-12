package com.example.securefolder.ui.modules;

import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.OptIn; // Import OptIn
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.UnstableApi; // Import UnstableApi
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;
import com.example.securefolder.R;
import com.example.securefolder.utils.CryptoManager;
import com.example.securefolder.utils.DatabaseHelper;
import com.example.securefolder.utils.EncryptedDataSourceFactory;
import com.example.securefolder.utils.KeyManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

// Mark class to allow Unstable API usage
@OptIn(markerClass = UnstableApi.class)
public class VideoViewerActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private String currentFilePath;
    private String currentFileName; // UUID
    private int fileId = -1;

    private ExoPlayer player;
    private PlayerView playerView;
    private float currentSpeed = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_video_viewer);

        dbHelper = new DatabaseHelper(this);
        playerView = findViewById(R.id.playerView);
        TextView tvTitle = findViewById(R.id.tvVideoTitle);
        ImageButton btnUnlock = findViewById(R.id.btnUnlock);
        ImageButton btnTrash = findViewById(R.id.btnTrash);

        currentFilePath = getIntent().getStringExtra("FILE_PATH");
        currentFileName = getIntent().getStringExtra("FILE_NAME");

        if (currentFilePath == null) { finish(); return; }

        fileId = dbHelper.getFileIdBySystemName(currentFileName);
        String realName = dbHelper.getDisplayName(currentFileName);
        tvTitle.setText(realName != null ? realName : "Secure Video");

        btnUnlock.setOnClickListener(v -> restoreVideo());
        btnTrash.setOnClickListener(v -> moveToTrash());

        initializePlayer();
    }

    private void initializePlayer() {
        File file = new File(currentFilePath);

        // CUSTOM SOURCE: Reads encrypted file directly
        EncryptedDataSourceFactory factory = new EncryptedDataSourceFactory(file, KeyManager.getMasterKey());

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(android.net.Uri.fromFile(file)));

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaSource(mediaSource);
        player.prepare();
        player.play();
    }

    public void toggleSpeed() {
        if (player == null) return;
        if (currentSpeed == 1.0f) currentSpeed = 1.5f;
        else if (currentSpeed == 1.5f) currentSpeed = 2.0f;
        else if (currentSpeed == 2.0f) currentSpeed = 0.5f;
        else currentSpeed = 1.0f;

        player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
        Toast.makeText(this, "Speed: " + currentSpeed + "x", Toast.LENGTH_SHORT).show();
    }

    private void moveToTrash() {
        new AlertDialog.Builder(this)
                .setTitle("Move to Trash?")
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
        if (player != null) player.pause();
        new Thread(() -> {
            try {
                String realName = dbHelper.getDisplayName(currentFileName);
                if (realName == null) realName = "Restored_Vid_" + System.currentTimeMillis() + ".mp4";

                File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                File destFile = new File(moviesDir, realName);

                FileInputStream fis = new FileInputStream(currentFilePath);
                FileOutputStream fos = new FileOutputStream(destFile);
                boolean success = CryptoManager.decrypt(KeyManager.getMasterKey(), fis, fos);

                if (success) {
                    new File(currentFilePath).delete();
                    if (fileId != -1) dbHelper.deleteFileRecordPermanently(fileId);
                    MediaScannerConnection.scanFile(this, new String[]{destFile.getAbsolutePath()}, null, null);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Restored to Movies", Toast.LENGTH_LONG).show();
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
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}