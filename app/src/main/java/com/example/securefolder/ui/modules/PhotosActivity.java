package com.example.securefolder.ui.modules;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PhotosActivity extends AppCompatActivity implements PhotosAdapter.OnPhotoActionListener {

    private RecyclerView recyclerView;
    private PhotosAdapter adapter;
    private List<File> photoFiles = new ArrayList<>();
    private LinearLayout loadingLayout;
    private LinearLayout layoutSelection;
    private TextView tvSelectionCount;
    private FloatingActionButton fab;

    private File vaultDir;
    private DatabaseHelper dbHelper;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleImageSelection(result.getData().getData());
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
        setContentView(R.layout.activity_photos);

        dbHelper = new DatabaseHelper(this);
        loadingLayout = findViewById(R.id.layoutLoading);
        layoutSelection = findViewById(R.id.layoutSelection);
        tvSelectionCount = findViewById(R.id.tvSelectionCount);
        fab = findViewById(R.id.fabAdd);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        adapter = new PhotosAdapter(photoFiles, dbHelper, this);
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        findViewById(R.id.btnDeleteSelected).setOnClickListener(v -> deleteSelectedItems());
        findViewById(R.id.btnExportSelected).setOnClickListener(v -> exportSelectedItems());

        vaultDir = new File(getExternalFilesDir(null), "Vault/Photos");
        if (!vaultDir.exists()) vaultDir.mkdirs();

        loadFilesFromDB();
    }

    // --- ADAPTER CALLBACKS ---
    @Override
    public void onPhotoClick(File file) {
        openPhotoViewer(file);
    }

    @Override
    public void onSelectionModeChanged(boolean active, int count) {
        if (active) {
            layoutSelection.setVisibility(View.VISIBLE);
            fab.setVisibility(View.GONE);
            tvSelectionCount.setText(count + " Selected");
        } else {
            layoutSelection.setVisibility(View.GONE);
            fab.setVisibility(View.VISIBLE);
        }
    }

    // --- BATCH OPERATIONS ---
    private void deleteSelectedItems() {
        List<File> selected = adapter.getSelectedFiles();
        if (selected.isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete " + selected.size() + " items?")
                .setMessage("These items will be moved to Trash.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    for (File f : selected) {
                        // Use System Name (UUID) to find ID
                        int id = dbHelper.getFileIdBySystemName(f.getName());
                        if (id != -1) {
                            dbHelper.setFileDeleted(id, true);
                        }
                    }
                    adapter.clearSelection();
                    loadFilesFromDB();
                    Toast.makeText(this, "Moved to Trash", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportSelectedItems() {
        List<File> selected = adapter.getSelectedFiles();
        if (selected.isEmpty()) return;

        loadingLayout.setVisibility(View.VISIBLE);
        new Thread(() -> {
            int count = 0;
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            for (File src : selected) {
                try {
                    // Restore original name from DB
                    String realName = dbHelper.getDisplayName(src.getName());
                    if (realName == null || realName.equals("Unknown")) realName = "Export_" + System.currentTimeMillis() + ".jpg";

                    File dest = new File(exportDir, "Restored_" + realName);

                    FileInputStream fis = new FileInputStream(src);
                    FileOutputStream fos = new FileOutputStream(dest);
                    boolean success = CryptoManager.decrypt(KeyManager.getMasterKey(), fis, fos);

                    if (success) {
                        count++;
                        MediaScannerConnection.scanFile(this, new String[]{dest.getAbsolutePath()}, null, null);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }

            final int finalCount = count;
            runOnUiThread(() -> {
                loadingLayout.setVisibility(View.GONE);
                adapter.clearSelection();
                Toast.makeText(this, "Exported " + finalCount + " files to Downloads", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    // --- IMPORT LOGIC (UUID FIX) ---
    private void handleImageSelection(Uri uri) {
        if (uri == null) return;
        loadingLayout.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String originalPath = getRealPathFromURI(uri);
                if (originalPath == null) originalPath = "Unknown_Location";

                String originalName = getFileNameFromUri(uri);

                // GENERATE RANDOM UUID (Invisible Vault)
                String systemName = UUID.randomUUID().toString();

                File outputFile = new File(vaultDir, systemName);
                InputStream inputStream = getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                boolean success = CryptoManager.encrypt(KeyManager.getMasterKey(), inputStream, outputStream);

                if (success) {
                    // Save UUID (System Name) + Real Name (Display Name)
                    dbHelper.addFile("PHOTO", systemName, originalName, originalPath);

                    // Delete Original
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
            String[] proj = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(column_index);
                cursor.close();
                return path;
            }
        } catch (Exception e) {}
        return null;
    }

    private String getFileNameFromUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) {
                String name = cursor.getString(nameIndex);
                cursor.close();
                return name;
            }
            cursor.close();
        }
        return "Photo_" + System.currentTimeMillis();
    }

    private void loadFilesFromDB() {
        photoFiles.clear();
        Cursor cursor = dbHelper.getActiveFiles("PHOTO");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Get System Name (UUID) from DB
                String sysName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYSTEM_NAME));
                File file = new File(vaultDir, sysName);
                if (file.exists()) {
                    photoFiles.add(file);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private void openPhotoViewer(File file) {
        Intent intent = new Intent(this, PhotoViewerActivity.class);
        intent.putExtra("FILE_PATH", file.getAbsolutePath());
        intent.putExtra("FILE_NAME", file.getName()); // This is the UUID
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (adapter.getSelectedFiles().size() > 0) {
            adapter.clearSelection();
        } else {
            super.onBackPressed();
        }
    }
}