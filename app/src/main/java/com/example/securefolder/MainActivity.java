package com.example.securefolder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.securefolder.ui.modules.DocumentsActivity;
import com.example.securefolder.ui.modules.NotesActivity;
import com.example.securefolder.ui.modules.PasswordsActivity;
import com.example.securefolder.ui.modules.PhotosActivity;
import com.example.securefolder.ui.modules.SettingsActivity;
import com.example.securefolder.ui.modules.TrashActivity;
import com.example.securefolder.ui.modules.VideosActivity;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure FLAG_SECURE is set globally for the home screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_home); // Ensure this matches your XML file name

        // Check Permissions on Start
        checkPermissions();

        // Setup Grid Clicks
        findViewById(R.id.cardPhotos).setOnClickListener(v -> startActivity(new Intent(this, PhotosActivity.class)));
        findViewById(R.id.cardVideos).setOnClickListener(v -> startActivity(new Intent(this, VideosActivity.class)));
        findViewById(R.id.cardDocuments).setOnClickListener(v -> startActivity(new Intent(this, DocumentsActivity.class)));
        findViewById(R.id.cardNotes).setOnClickListener(v -> startActivity(new Intent(this, NotesActivity.class)));
        findViewById(R.id.cardPasswords).setOnClickListener(v -> startActivity(new Intent(this, PasswordsActivity.class)));
        findViewById(R.id.cardTrash).setOnClickListener(v -> startActivity(new Intent(this, TrashActivity.class)));
        findViewById(R.id.cardSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("This app requires 'All Files Access' to secure your files and create backups. Please grant this permission on the next screen.")
                        .setPositiveButton("Grant", (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                intent.addCategory("android.intent.category.DEFAULT");
                                intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
                            } catch (Exception e) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
                            }
                        })
                        .setNegativeButton("Exit", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        } else {
            // Android 10 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied. App functionality will be limited.", Toast.LENGTH_LONG).show();
            }
        }
    }
}