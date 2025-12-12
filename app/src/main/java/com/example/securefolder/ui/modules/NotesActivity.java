package com.example.securefolder.ui.modules;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securefolder.R;
import com.example.securefolder.utils.CryptoManager;
import com.example.securefolder.utils.DatabaseHelper;
import com.example.securefolder.utils.KeyManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NotesActivity extends AppCompatActivity implements NotesAdapter.OnNoteActionListener {

    private DatabaseHelper dbHelper;
    private List<NoteItem> noteList;
    private RecyclerView rv;
    private NotesAdapter adapter;
    private LinearLayout layoutSelection;
    private TextView tvSelectionCount;
    private FloatingActionButton fab;

    // Public for Adapter access
    public static class NoteItem {
        public int id;
        public String title;
        public String content;
        public long timestamp;
        NoteItem(int id, String title, String content, long timestamp) {
            this.id = id; this.title = title; this.content = content; this.timestamp = timestamp;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos); // Reuse coordinator layout

        if (KeyManager.getMasterKey() == null) { finish(); return; }

        dbHelper = new DatabaseHelper(this);
        noteList = new ArrayList<>();

        rv = findViewById(R.id.recyclerView);
        layoutSelection = findViewById(R.id.layoutSelection);
        tvSelectionCount = findViewById(R.id.tvSelectionCount);
        fab = findViewById(R.id.fabAdd);

        // Staggered Grid for Notes looks better
        rv.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new NotesAdapter(noteList, this);
        rv.setAdapter(adapter);

        // UI Setup
        fab.setImageResource(android.R.drawable.ic_input_add);
        fab.setOnClickListener(v -> startActivity(new Intent(this, NoteEditorActivity.class)));

        findViewById(R.id.btnDeleteSelected).setOnClickListener(v -> deleteSelected());
        findViewById(R.id.btnExportSelected).setOnClickListener(v -> exportSelected());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void loadNotes() {
        noteList.clear();
        Cursor cursor = dbHelper.getAllNotes(false);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String encTitle = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE));
                String encContent = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTENT));
                long time = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIMESTAMP));

                // DECRYPT
                String title = CryptoManager.decryptString(encTitle);
                String content = CryptoManager.decryptString(encContent);
                // Simple truncate for preview
                if (content != null && content.length() > 100) content = content.substring(0, 100) + "...";

                noteList.add(new NoteItem(id, title, content, time));
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onNoteClick(NoteItem note) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra("ID", note.id);
        // We need to fetch full content from DB again ideally, but decrypting here is okay for now
        // Wait, the item has truncated content. We should fetch fresh in Editor or pass full.
        // For security, Editor should fetch. But let's pass params to keep it simple as Editor expects them.
        // We must re-fetch full content because NoteItem might be truncated.
        // Actually, let's just pass what we have, but NoteEditor should ideally handle ID fetch.
        // Let's rely on Editor to fetch or just pass fields.
        // For this tutorial, we will fetch full content here to pass to Editor.
        // Or better: Let Editor fetch.
        // Refactoring Editor to fetch by ID is safer. But let's stick to Intent passing for simplicity.
        // Retrieve FULL content
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_NOTES + " WHERE " + DatabaseHelper.COL_ID + "=" + note.id, null);
        if (c.moveToFirst()) {
            String fullContent = CryptoManager.decryptString(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONTENT)));
            String fullTitle = CryptoManager.decryptString(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE)));
            intent.putExtra("TITLE", fullTitle);
            intent.putExtra("CONTENT", fullContent);
        }
        c.close();

        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(boolean active, int count) {
        layoutSelection.setVisibility(active ? View.VISIBLE : View.GONE);
        fab.setVisibility(active ? View.GONE : View.VISIBLE);
        tvSelectionCount.setText(count + " Selected");
    }

    private void deleteSelected() {
        List<Integer> ids = adapter.getSelectedIds();
        new AlertDialog.Builder(this)
                .setTitle("Delete " + ids.size() + " notes?")
                .setPositiveButton("Delete", (d, w) -> {
                    for (int id : ids) dbHelper.setNoteDeleted(id, true);
                    adapter.clearSelection();
                    loadNotes();
                    Toast.makeText(this, "Moved to Trash", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportSelected() {
        List<Integer> ids = adapter.getSelectedIds();
        File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        int count = 0;

        for (int id : ids) {
            Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_NOTES + " WHERE " + DatabaseHelper.COL_ID + "=" + id, null);
            if (c.moveToFirst()) {
                String title = CryptoManager.decryptString(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE)));
                String content = CryptoManager.decryptString(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONTENT)));

                try {
                    File dest = new File(exportDir, title.replaceAll("[^a-zA-Z0-9.-]", "_") + ".txt");
                    FileOutputStream fos = new FileOutputStream(dest);
                    fos.write(("Title: " + title + "\n\n" + content).getBytes(StandardCharsets.UTF_8));
                    fos.close();
                    count++;
                } catch (Exception e) { e.printStackTrace(); }
            }
            c.close();
        }

        adapter.clearSelection();
        Toast.makeText(this, "Exported " + count + " notes to Downloads", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        if (adapter.getSelectedIds().size() > 0) adapter.clearSelection();
        else super.onBackPressed();
    }
}