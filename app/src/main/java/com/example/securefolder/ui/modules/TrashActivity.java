package com.example.securefolder.ui.modules;

import android.app.AlertDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securefolder.R;
import com.example.securefolder.utils.DatabaseHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrashActivity extends AppCompatActivity {

    private RecyclerView rv;
    private DatabaseHelper dbHelper;
    private List<TrashItem> trashItems;

    static class TrashItem {
        int id;
        String name;
        String type; // "NOTE", "PASS", "FILE"
        String extraInfo; // Path or EncName

        TrashItem(int id, String name, String type, String extra) {
            this.id = id; this.name = name; this.type = type; this.extraInfo = extra;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        rv = findViewById(R.id.rvTrash);
        rv.setLayoutManager(new LinearLayoutManager(this));
        dbHelper = new DatabaseHelper(this);
        trashItems = new ArrayList<>();

        loadTrash();
    }

    private void loadTrash() {
        trashItems.clear();

        // 1. Load Notes
        Cursor cNote = dbHelper.getAllNotes(true);
        if (cNote != null && cNote.moveToFirst()) {
            do {
                int id = cNote.getInt(cNote.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String title = cNote.getString(cNote.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE));
                trashItems.add(new TrashItem(id, "Note: " + title, "NOTE", null));
            } while (cNote.moveToNext());
            cNote.close();
        }

        // 2. Load Passwords
        Cursor cPass = dbHelper.getAllPasswords(true);
        if (cPass != null && cPass.moveToFirst()) {
            do {
                int id = cPass.getInt(cPass.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String app = cPass.getString(cPass.getColumnIndexOrThrow(DatabaseHelper.COL_APP_NAME));
                trashItems.add(new TrashItem(id, "Pass: " + app, "PASS", null));
            } while (cPass.moveToNext());
            cPass.close();
        }

        // 3. Load Files (Photos)
        Cursor cFiles = dbHelper.getDeletedFiles();
        if (cFiles != null && cFiles.moveToFirst()) {
            do {
                int id = cFiles.getInt(cFiles.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String name = cFiles.getString(cFiles.getColumnIndexOrThrow(DatabaseHelper.COL_ENC_NAME));
                trashItems.add(new TrashItem(id, "File: " + name, "FILE", name));
            } while (cFiles.moveToNext());
            cFiles.close();
        }

        rv.setAdapter(new TrashAdapter(trashItems));
    }

    class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.ViewHolder> {
        List<TrashItem> data;
        TrashAdapter(List<TrashItem> data) { this.data = data; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(32, 32, 32, 32);
            tv.setTextSize(16);
            tv.setTextColor(Color.BLACK);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TrashItem item = data.get(position);
            holder.tv.setText(item.name);
            holder.itemView.setOnClickListener(v -> showActionDialog(item));
        }

        @Override
        public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv;
            ViewHolder(View v) { super(v); tv = (TextView)v; }
        }
    }

    private void showActionDialog(TrashItem item) {
        String[] options = {"Restore", "Delete Permanently"};
        new AlertDialog.Builder(this)
                .setTitle(item.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) restoreItem(item);
                    else deleteForever(item);
                })
                .show();
    }

    private void restoreItem(TrashItem item) {
        if (item.type.equals("NOTE")) dbHelper.setNoteDeleted(item.id, false);
        if (item.type.equals("PASS")) dbHelper.setPasswordDeleted(item.id, false);
        if (item.type.equals("FILE")) dbHelper.setFileDeleted(item.id, false);

        Toast.makeText(this, "Restored", Toast.LENGTH_SHORT).show();
        loadTrash();
    }

    private void deleteForever(TrashItem item) {
        if (item.type.equals("NOTE")) dbHelper.deleteNotePermanently(item.id);
        if (item.type.equals("PASS")) dbHelper.deletePasswordPermanently(item.id);
        if (item.type.equals("FILE")) {
            // Delete actual file too
            File vaultDir = new File(getExternalFilesDir(null), "Vault/Photos");
            File file = new File(vaultDir, item.extraInfo);
            if(file.exists()) file.delete();
            dbHelper.deleteFileRecordPermanently(item.id);
        }
        Toast.makeText(this, "Deleted Forever", Toast.LENGTH_SHORT).show();
        loadTrash();
    }
}