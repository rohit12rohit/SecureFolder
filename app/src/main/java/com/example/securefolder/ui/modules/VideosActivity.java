package com.example.securefolder.ui.modules;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securefolder.R;
import com.example.securefolder.utils.CryptoManager;
import com.example.securefolder.utils.DatabaseHelper;
import com.example.securefolder.utils.KeyManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class VideosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VideosAdapter adapter;
    private List<File> videoFiles = new ArrayList<>();
    private LinearLayout loadingLayout;
    private File vaultDir;
    private DatabaseHelper dbHelper;

    private final ActivityResultLauncher<Intent> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleVideoSelection(result.getData().getData());
                }
            }
    );

    private final ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(this, "Original File Deleted.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos); // We reuse the photos layout

        dbHelper = new DatabaseHelper(this);
        loadingLayout = findViewById(R.id.layoutLoading);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        adapter = new VideosAdapter(videoFiles, this::openVideoViewer);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            pickVideoLauncher.launch(intent);
        });

        vaultDir = new File(getExternalFilesDir(null), "Vault/Videos");
        if (!vaultDir.exists()) vaultDir.mkdirs();

        loadFilesFromDB();
    }

    private void handleVideoSelection(Uri uri) {
        if (uri == null) return;
        loadingLayout.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String originalPath = getRealPathFromURI(uri);
                if (originalPath == null) originalPath = "Unknown_Location";

                InputStream inputStream = getContentResolver().openInputStream(uri);
                String fileName = "VID_" + System.currentTimeMillis() + ".enc";
                File outputFile = new File(vaultDir, fileName);
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                boolean success = CryptoManager.encrypt(KeyManager.getMasterKey(), inputStream, outputStream);

                if (success) {
                    dbHelper.addFile("VIDEO", fileName, originalPath);

                    boolean isDeleted = false;
                    if (!originalPath.equals("Unknown_Location")) {
                        File original = new File(originalPath);
                        if (original.exists()) isDeleted = original.delete();
                    }

                    if (!isDeleted) {
                        try {
                            int rows = getContentResolver().delete(uri, null, null);
                            if (rows == 0) requestDeletePermission(uri);
                            else isDeleted = true;
                        } catch (SecurityException e) {
                            requestDeletePermission(uri);
                        }
                    }

                    final boolean finalDeleted = isDeleted;
                    runOnUiThread(() -> {
                        loadingLayout.setVisibility(View.GONE);
                        loadFilesFromDB();
                        if (finalDeleted) Toast.makeText(this, "Encrypted & Deleted", Toast.LENGTH_SHORT).show();
                    });

                } else {
                    runOnUiThread(() -> {
                        loadingLayout.setVisibility(View.GONE);
                        Toast.makeText(this, "Encryption Failed", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> loadingLayout.setVisibility(View.GONE));
            }
        }).start();
    }

    private void requestDeletePermission(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                List<Uri> uris = new ArrayList<>();
                uris.add(uri);
                PendingIntent pi = MediaStore.createDeleteRequest(getContentResolver(), uris);
                deleteRequestLauncher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        try {
            String[] proj = { MediaStore.Video.Media.DATA };
            Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(column_index);
                cursor.close();
                return path;
            }
        } catch (Exception e) {}
        return null;
    }

    private void loadFilesFromDB() {
        videoFiles.clear();
        Cursor cursor = dbHelper.getActiveFiles("VIDEO");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String fileName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ENC_NAME));
                File file = new File(vaultDir, fileName);
                if (file.exists()) {
                    videoFiles.add(file);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private void openVideoViewer(File file) {
        Intent intent = new Intent(this, VideoViewerActivity.class);
        intent.putExtra("FILE_PATH", file.getAbsolutePath());
        intent.putExtra("FILE_NAME", file.getName());
        startActivity(intent);
    }
}