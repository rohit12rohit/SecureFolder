package com.example.securefolder.ui.modules;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.DatabaseHelper;

public class PasswordEditorActivity extends AppCompatActivity {

    private int existingId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_editor);

        EditText etApp = findViewById(R.id.etAppName);
        EditText etUser = findViewById(R.id.etUsername);
        EditText etPass = findViewById(R.id.etPasswordValue);
        Button btnSave = findViewById(R.id.btnSavePassword);

        // Check if editing
        if (getIntent().hasExtra("ID")) {
            existingId = getIntent().getIntExtra("ID", -1);
            etApp.setText(getIntent().getStringExtra("APP"));
            etUser.setText(getIntent().getStringExtra("USER"));
            etPass.setText(getIntent().getStringExtra("PASS"));
            btnSave.setText("Update Credential");
        }

        btnSave.setOnClickListener(v -> {
            String app = etApp.getText().toString();
            String user = etUser.getText().toString();
            String pass = etPass.getText().toString();

            if (app.isEmpty() || pass.isEmpty()) {
                etApp.setError("Required");
                return;
            }

            DatabaseHelper db = new DatabaseHelper(this);
            if (existingId == -1) {
                db.addPassword(app, user, pass);
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            } else {
                db.updatePassword(existingId, app, user, pass);
                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
            }

            finish();
        });
    }
}