package com.example.securefolder.ui.modules;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securefolder.R;
import com.example.securefolder.utils.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private List<NoteItem> noteList;
    private RecyclerView rv;

    static class NoteItem {
        int id;
        String title;
        String content;
        long timestamp;
        NoteItem(int id, String title, String content, long timestamp) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.timestamp = timestamp;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        rv = findViewById(R.id.rvNotes);
        rv.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);
        noteList = new ArrayList<>();

        FloatingActionButton fab = findViewById(R.id.fabAddNote);
        fab.setOnClickListener(v -> startActivity(new Intent(this, NoteEditorActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void loadNotes() {
        noteList.clear();
        Cursor cursor = dbHelper.getAllNotes();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTENT));
                long time = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIMESTAMP));
                noteList.add(new NoteItem(id, title, content, time));
            } while (cursor.moveToNext());
            cursor.close();
        }
        rv.setAdapter(new NotesAdapter(noteList));
    }

    // --- ADAPTER ---
    class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {
        List<NoteItem> data;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        NotesAdapter(List<NoteItem> data) { this.data = data; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Create a layout programmatically to show Title AND Date
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(32, 24, 32, 24);
            layout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView tvTitle = new TextView(parent.getContext());
            tvTitle.setTextSize(18);
            tvTitle.setTextColor(Color.BLACK); // Force Black
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvDate = new TextView(parent.getContext());
            tvDate.setTextSize(12);
            tvDate.setTextColor(Color.GRAY); // Date in Gray

            layout.addView(tvTitle);
            layout.addView(tvDate);

            return new ViewHolder(layout, tvTitle, tvDate);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            NoteItem item = data.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvDate.setText("Last edited: " + sdf.format(new Date(item.timestamp)));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(NotesActivity.this, NoteEditorActivity.class);
                intent.putExtra("ID", item.id);
                intent.putExtra("TITLE", item.title);
                intent.putExtra("CONTENT", item.content);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate;
            ViewHolder(View v, TextView t, TextView d) {
                super(v);
                tvTitle = t;
                tvDate = d;
            }
        }
    }
}