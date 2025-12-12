package com.example.securefolder.ui.modules;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
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

public class DocumentsActivity extends AppCompatActivity implements DocumentsAdapter.OnDocumentActionListener {

    private RecyclerView recyclerView;
    private DocumentsAdapter adapter;
    private List<File> docFiles = new ArrayList<>();
    private LinearLayout loadingLayout, layoutSelection;
    private TextView tvSelectionCount;
    private FloatingActionButton fab;
    private File vaultDir;
    private DatabaseHelper dbHelper;

    private final ActivityResultLauncher<Intent> pickDocLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleDocSelection(result.getData().getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos); // Reuse layout

        dbHelper = new DatabaseHelper(this);
        loadingLayout = findViewById(R.id.layoutLoading);
        layoutSelection = findViewById(R.id.layoutSelection);
        tvSelectionCount = findViewById(R.id.tvSelectionCount);
        fab = findViewById(R.id.fabAdd);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DocumentsAdapter(docFiles, dbHelper, this);
        recyclerView.setAdapter(adapter);

        fab.setImageResource(android.R.drawable.ic_input_add);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimetypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            pickDocLauncher.launch(intent);
        });

        findViewById(R.id.btnDeleteSelected).setOnClickListener(v -> deleteSelected());
        findViewById(R.id.btnExportSelected).setOnClickListener(v -> exportSelected());

        vaultDir = new File(getExternalFilesDir(null), "Vault/Documents");
        if (!vaultDir.exists()) vaultDir.mkdirs();

        loadFilesFromDB();
    }

    private void handleDocSelection(Uri uri) {
        if (uri == null) return;
        loadingLayout.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String originalName = getFileNameFromUri(uri);
                if (originalName == null) originalName = "Doc_" + System.currentTimeMillis();

                // 1. GENERATE UUID (Invisible Vault)
                String systemName = UUID.randomUUID().toString();

                File outputFile = new File(vaultDir, systemName);
                InputStream inputStream = getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                boolean success = CryptoManager.encrypt(KeyManager.getMasterKey(), inputStream, outputStream);

                if (success) {
                    // 2. SAVE TO DB (4 Arguments now)
                    // We don't have original path for Documents usually (Scoped Storage), so pass "Unknown"
                    dbHelper.addFile("DOCUMENT", systemName, originalName, "Unknown_Path");

                    runOnUiThread(() -> {
                        loadingLayout.setVisibility(View.GONE);
                        loadFilesFromDB();
                        Toast.makeText(this, "Document Encrypted", Toast.LENGTH_SHORT).show();
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
        return uri.getLastPathSegment();
    }

    private void loadFilesFromDB() {
        docFiles.clear();
        Cursor cursor = dbHelper.getActiveFiles("DOCUMENT");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Use System Name (UUID) to find file on disk
                String sysName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYSTEM_NAME));
                File file = new File(vaultDir, sysName);
                if (file.exists()) {
                    docFiles.add(file);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDocumentClick(File file) {
        Intent intent = new Intent(this, DocumentViewerActivity.class);
        intent.putExtra("FILE_PATH", file.getAbsolutePath());
        intent.putExtra("FILE_NAME", file.getName()); // This is the UUID
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(boolean active, int count) {
        layoutSelection.setVisibility(active ? View.VISIBLE : View.GONE);
        fab.setVisibility(active ? View.GONE : View.VISIBLE);
        tvSelectionCount.setText(count + " Selected");
    }

    private void deleteSelected() {
        List<File> selected = adapter.getSelectedFiles();
        new AlertDialog.Builder(this)
                .setTitle("Delete " + selected.size() + " documents?")
                .setPositiveButton("Delete", (d, w) -> {
                    for (File f : selected) {
                        int id = dbHelper.getFileIdBySystemName(f.getName());
                        if (id != -1) dbHelper.setFileDeleted(id, true);
                    }
                    adapter.clearSelection();
                    loadFilesFromDB();
                    Toast.makeText(this, "Moved to Trash", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportSelected() {
        List<File> selected = adapter.getSelectedFiles();
        loadingLayout.setVisibility(View.VISIBLE);
        new Thread(() -> {
            int count = 0;
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            for (File src : selected) {
                try {
                    String realName = dbHelper.getDisplayName(src.getName());
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
            int finalCount = count;
            runOnUiThread(() -> {
                loadingLayout.setVisibility(View.GONE);
                adapter.clearSelection();
                Toast.makeText(this, "Exported " + finalCount + " docs", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (adapter.getSelectedFiles().size() > 0) adapter.clearSelection();
        else super.onBackPressed();
    }
}