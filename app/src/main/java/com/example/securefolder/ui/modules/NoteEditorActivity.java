package com.example.securefolder.ui.modules;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.CryptoManager;
import com.example.securefolder.utils.DatabaseHelper;
import com.example.securefolder.utils.KeyManager;

public class NoteEditorActivity extends AppCompatActivity {

    private int existingId = -1; // -1 means new note

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        // Safety check: ensure we have the master key
        if (KeyManager.getMasterKey() == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EditText etTitle = findViewById(R.id.etNoteTitle);
        EditText etContent = findViewById(R.id.etNoteContent);
        Button btnSave = findViewById(R.id.btnSaveNote);

        // Check if editing
        if (getIntent().hasExtra("ID")) {
            existingId = getIntent().getIntExtra("ID", -1);
            etTitle.setText(getIntent().getStringExtra("TITLE"));
            etContent.setText(getIntent().getStringExtra("CONTENT"));
            btnSave.setText("Update Note");
        }

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString();
            String content = etContent.getText().toString();

            if (title.isEmpty()) {
                etTitle.setError("Title required");
                return;
            }

            // ENCRYPT DATA
            String encTitle = CryptoManager.encryptString(title);
            String encContent = CryptoManager.encryptString(content);

            if (encTitle == null || encContent == null) {
                Toast.makeText(this, "Encryption Failed!", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseHelper db = new DatabaseHelper(this);
            if (existingId == -1) {
                db.addNote(encTitle, encContent);
                Toast.makeText(this, "Note Encrypted & Saved", Toast.LENGTH_SHORT).show();
            } else {
                db.updateNote(existingId, encTitle, encContent);
                Toast.makeText(this, "Note Encrypted & Updated", Toast.LENGTH_SHORT).show();
            }

            finish();
        });
    }
}