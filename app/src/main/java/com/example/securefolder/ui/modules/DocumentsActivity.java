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
import androidx.recyclerview.widget.LinearLayoutManager;
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

public class DocumentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DocumentsAdapter adapter;
    private List<File> docFiles = new ArrayList<>();
    private LinearLayout loadingLayout;
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
        setContentView(R.layout.activity_documents);

        dbHelper = new DatabaseHelper(this);
        loadingLayout = findViewById(R.id.layoutLoading);
        recyclerView = findViewById(R.id.rvDocuments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DocumentsAdapter(docFiles, dbHelper, this::openDocumentViewer);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fabAddDoc).setOnClickListener(v -> {
            // Allow picking generic files
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimetypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            pickDocLauncher.launch(intent);
        });

        vaultDir = new File(getExternalFilesDir(null), "Vault/Documents");
        if (!vaultDir.exists()) vaultDir.mkdirs();

        loadFilesFromDB();
    }

    private void handleDocSelection(Uri uri) {
        if (uri == null) return;
        loadingLayout.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String originalPath = getFileNameFromUri(uri);
                if (originalPath == null) originalPath = "Unknown_Doc";

                InputStream inputStream = getContentResolver().openInputStream(uri);
                String fileName = "DOC_" + System.currentTimeMillis() + ".enc";
                File outputFile = new File(vaultDir, fileName);
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                boolean success = CryptoManager.encrypt(KeyManager.getMasterKey(), inputStream, outputStream);

                if (success) {
                    // Save as DOCUMENT type
                    dbHelper.addFile("DOCUMENT", fileName, originalPath);

                    // Note: ACTION_OPEN_DOCUMENT grants read permission, we can't easily delete original system files
                    // without specific permissions or using ACTION_PICK. For now, we just copy & encrypt.

                    runOnUiThread(() -> {
                        loadingLayout.setVisibility(View.GONE);
                        loadFilesFromDB();
                        Toast.makeText(this, "Document Encrypted & Imported", Toast.LENGTH_SHORT).show();
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

    // Helper to get simple name for DB
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
                String fileName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ENC_NAME));
                File file = new File(vaultDir, fileName);
                if (file.exists()) {
                    docFiles.add(file);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private void openDocumentViewer(File file) {
        Intent intent = new Intent(this, DocumentViewerActivity.class);
        intent.putExtra("FILE_PATH", file.getAbsolutePath());
        intent.putExtra("FILE_NAME", file.getName());
        startActivity(intent);
    }
}