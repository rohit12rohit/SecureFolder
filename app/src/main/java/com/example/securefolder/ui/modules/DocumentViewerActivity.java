package com.example.securefolder.ui.modules;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.CryptoManager;
import com.example.securefolder.utils.DatabaseHelper;
import com.example.securefolder.utils.KeyManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class DocumentViewerActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private String currentFilePath;
    private String currentFileName;
    private File tempFile;
    private int fileId = -1;

    // PDF Variables
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ImageView ivPdfPage;
    private int pageIndex = 0;
    private Button btnNext, btnPrev;
    private TextView tvPageCount, tvStatus;
    private LinearLayout layoutNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_document_viewer);

        dbHelper = new DatabaseHelper(this);
        currentFilePath = getIntent().getStringExtra("FILE_PATH");
        currentFileName = getIntent().getStringExtra("FILE_NAME");

        ivPdfPage = findViewById(R.id.ivPdfPage);
        tvStatus = findViewById(R.id.tvStatus);
        layoutNav = findViewById(R.id.layoutPdfNav);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        tvPageCount = findViewById(R.id.tvPageCount);

        Button btnRestore = findViewById(R.id.btnRestore);
        Button btnTrash = findViewById(R.id.btnTrash);

        if (currentFilePath == null) { finish(); return; }
        fileId = dbHelper.getFileId(currentFileName);

        // Check if PDF
        String origPath = dbHelper.getOriginalPath(currentFileName);
        boolean isPdf = origPath != null && origPath.toLowerCase().endsWith(".pdf");

        if (isPdf) {
            decryptAndRenderPdf();
        } else {
            tvStatus.setText("Format not supported for internal viewing.\nPlease Restore to view.");
        }

        btnRestore.setOnClickListener(v -> restoreDocument(origPath));
        btnTrash.setOnClickListener(v -> moveToTrash());

        btnNext.setOnClickListener(v -> showPage(pageIndex + 1));
        btnPrev.setOnClickListener(v -> showPage(pageIndex - 1));
    }

    private void decryptAndRenderPdf() {
        new Thread(() -> {
            try {
                // Decrypt to Temp
                File encryptedFile = new File(currentFilePath);
                tempFile = File.createTempFile("VIEW", ".pdf", getCacheDir());
                tempFile.deleteOnExit(); // Security measure

                FileInputStream fis = new FileInputStream(encryptedFile);
                FileOutputStream fos = new FileOutputStream(tempFile);
                boolean success = CryptoManager.decrypt(KeyManager.getMasterKey(), fis, fos);

                runOnUiThread(() -> {
                    if (success) {
                        try {
                            ParcelFileDescriptor fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
                            pdfRenderer = new PdfRenderer(fd);
                            layoutNav.setVisibility(View.VISIBLE);
                            tvStatus.setVisibility(View.GONE);
                            showPage(0);
                        } catch (Exception e) {
                            tvStatus.setText("Error opening PDF");
                        }
                    } else {
                        tvStatus.setText("Decryption Failed");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showPage(int index) {
        if (pdfRenderer == null) return;
        if (index < 0 || index >= pdfRenderer.getPageCount()) return;

        if (currentPage != null) currentPage.close();

        currentPage = pdfRenderer.openPage(index);
        pageIndex = index;

        // Render to Bitmap
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
        // White background for PDF
        bitmap.eraseColor(0xFFFFFFFF);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        ivPdfPage.setImageBitmap(bitmap);
        tvPageCount.setText((index + 1) + " / " + pdfRenderer.getPageCount());

        btnPrev.setEnabled(index > 0);
        btnNext.setEnabled(index < pdfRenderer.getPageCount() - 1);
    }

    private void moveToTrash() {
        new AlertDialog.Builder(this)
                .setTitle("Move to Trash?")
                .setMessage("Move document to Trash?")
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

    private void restoreDocument(String origName) {
        new Thread(() -> {
            try {
                String fileName = (origName != null) ? origName : "Restored_Doc_" + System.currentTimeMillis();
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File destFile = new File(downloadDir, new File(fileName).getName());

                FileInputStream fis = new FileInputStream(currentFilePath);
                FileOutputStream fos = new FileOutputStream(destFile);
                boolean success = CryptoManager.decrypt(KeyManager.getMasterKey(), fis, fos);

                if (success) {
                    // Delete from Vault
                    new File(currentFilePath).delete();
                    if (fileId != -1) dbHelper.deleteFileRecordPermanently(fileId);

                    MediaScannerConnection.scanFile(this, new String[]{destFile.getAbsolutePath()}, null, null);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Exported to Downloads: " + destFile.getName(), Toast.LENGTH_LONG).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Export Failed", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        if (currentPage != null) currentPage.close();
        if (pdfRenderer != null) pdfRenderer.close();
        if (tempFile != null && tempFile.exists()) tempFile.delete();
        super.onDestroy();
    }
}