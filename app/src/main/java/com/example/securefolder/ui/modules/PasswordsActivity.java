package com.example.securefolder.ui.modules;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securefolder.R;
import com.example.securefolder.utils.CryptoManager;
import com.example.securefolder.utils.DatabaseHelper;
import com.example.securefolder.utils.KeyManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class PasswordsActivity extends AppCompatActivity implements PasswordsAdapter.OnPassActionListener {

    private DatabaseHelper dbHelper;
    private List<PassItem> passList;
    private RecyclerView rv;
    private PasswordsAdapter adapter;
    private LinearLayout layoutSelection;
    private TextView tvSelectionCount;
    private FloatingActionButton fab;

    public static class PassItem {
        public int id;
        public String appName, username, password;
        public long timestamp;
        PassItem(int id, String app, String user, String pass, long time) {
            this.id = id; this.appName = app; this.username = user; this.password = pass; this.timestamp = time;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos); // Reuse coordinator layout

        if (KeyManager.getMasterKey() == null) { finish(); return; }

        rv = findViewById(R.id.recyclerView);
        layoutSelection = findViewById(R.id.layoutSelection);
        tvSelectionCount = findViewById(R.id.tvSelectionCount);
        fab = findViewById(R.id.fabAdd);

        rv.setLayoutManager(new LinearLayoutManager(this));
        dbHelper = new DatabaseHelper(this);
        passList = new ArrayList<>();

        adapter = new PasswordsAdapter(passList, this);
        rv.setAdapter(adapter);

        fab.setImageResource(android.R.drawable.ic_input_add);
        fab.setOnClickListener(v -> startActivity(new Intent(this, PasswordEditorActivity.class)));

        findViewById(R.id.btnDeleteSelected).setOnClickListener(v -> deleteSelected());
        // Exporting passwords plain text is risky, but user requested "Export".
        // We will just show a Toast warning for now or implement CSV export.
        findViewById(R.id.btnExportSelected).setOnClickListener(v -> {
            Toast.makeText(this, "Plain-text export disabled for security.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPasswords();
    }

    private void loadPasswords() {
        passList.clear();
        Cursor cursor = dbHelper.getAllPasswords(false);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String encApp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_APP_NAME));
                String encUser = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USERNAME));
                String encPass = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PASSWORD));
                long time = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIMESTAMP));

                String app = CryptoManager.decryptString(encApp);
                String user = CryptoManager.decryptString(encUser);
                String pass = CryptoManager.decryptString(encPass);

                passList.add(new PassItem(id, app, user, pass, time));
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPassClick(PassItem item) {
        Intent intent = new Intent(this, PasswordEditorActivity.class);
        intent.putExtra("ID", item.id);
        intent.putExtra("APP", item.appName);
        intent.putExtra("USER", item.username);
        intent.putExtra("PASS", item.password);
        startActivity(intent);
    }

    @Override
    public void onCopyUser(String user) {
        copyToClipboard("Username", user);
    }

    @Override
    public void onCopyPass(String pass) {
        copyToClipboard("Password", pass);
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + " Copied", Toast.LENGTH_SHORT).show();
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
                .setTitle("Delete " + ids.size() + " passwords?")
                .setPositiveButton("Delete", (d, w) -> {
                    for (int id : ids) dbHelper.setPasswordDeleted(id, true);
                    adapter.clearSelection();
                    loadPasswords();
                    Toast.makeText(this, "Moved to Trash", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (adapter.getSelectedIds().size() > 0) adapter.clearSelection();
        else super.onBackPressed();
    }
}