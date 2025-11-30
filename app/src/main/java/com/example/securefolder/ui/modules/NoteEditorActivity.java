package com.example.securefolder.ui.modules;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securefolder.R;
import com.example.securefolder.utils.DatabaseHelper;

public class NoteEditorActivity extends AppCompatActivity {

    private int existingId = -1; // -1 means new note

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

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

            DatabaseHelper db = new DatabaseHelper(this);
            if (existingId == -1) {
                db.addNote(title, content);
                Toast.makeText(this, "Note Created", Toast.LENGTH_SHORT).show();
            } else {
                db.updateNote(existingId, title, content);
                Toast.makeText(this, "Note Updated", Toast.LENGTH_SHORT).show();
            }

            finish();
        });
    }
}