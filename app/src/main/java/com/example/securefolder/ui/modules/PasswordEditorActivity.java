package com.example.securefolder.ui.modules;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.CryptoManager;
import com.example.securefolder.utils.DatabaseHelper;
import com.example.securefolder.utils.KeyManager;
import com.example.securefolder.utils.PasswordGenerator;

public class PasswordEditorActivity extends AppCompatActivity {

    private int existingId = -1;
    private EditText etPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_editor);

        if (KeyManager.getMasterKey() == null) { finish(); return; }

        EditText etApp = findViewById(R.id.etAppName);
        EditText etUser = findViewById(R.id.etUsername);
        etPass = findViewById(R.id.etPasswordValue);
        Button btnSave = findViewById(R.id.btnSavePassword);

        // Add a Generate Button Programmatically or assume it is in layout (we need to update layout)
        // For simplicity, we will assume layout is updated or just use a logic here.
        // Let's add a "Generate" button to layout in next step or strictly here.
        // Wait, I can't easily add view without XML. Let's update XML first.
        // See Step 9.

        if (getIntent().hasExtra("ID")) {
            existingId = getIntent().getIntExtra("ID", -1);
            etApp.setText(getIntent().getStringExtra("APP"));
            etUser.setText(getIntent().getStringExtra("USER"));
            etPass.setText(getIntent().getStringExtra("PASS"));
            btnSave.setText("Update Credential");
        }

        // Find Generate Button (added in layout below)
        Button btnGenerate = findViewById(R.id.btnGenerate);
        btnGenerate.setOnClickListener(v -> showGeneratorDialog());

        btnSave.setOnClickListener(v -> {
            String app = etApp.getText().toString();
            String user = etUser.getText().toString();
            String pass = etPass.getText().toString();

            if (app.isEmpty() || pass.isEmpty()) {
                etApp.setError("Required");
                return;
            }

            String encApp = CryptoManager.encryptString(app);
            String encUser = CryptoManager.encryptString(user);
            String encPass = CryptoManager.encryptString(pass);

            DatabaseHelper db = new DatabaseHelper(this);
            if (existingId == -1) {
                db.addPassword(encApp, encUser, encPass);
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            } else {
                db.updatePassword(existingId, encApp, encUser, encPass);
                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
    }

    private void showGeneratorDialog() {
        String pass = PasswordGenerator.generate(true, true, true, 16);
        new AlertDialog.Builder(this)
                .setTitle("Generated Password")
                .setMessage(pass)
                .setPositiveButton("Use", (d, w) -> etPass.setText(pass))
                .setNegativeButton("Cancel", null)
                .show();
    }
}