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

public class PasswordsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private List<PassItem> passList;
    private RecyclerView rv;

    static class PassItem {
        int id;
        String appName, username, password;
        long timestamp;
        PassItem(int id, String app, String user, String pass, long time) {
            this.id = id; this.appName = app; this.username = user; this.password = pass; this.timestamp = time;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passwords);

        rv = findViewById(R.id.rvPasswords);
        rv.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);
        passList = new ArrayList<>();

        FloatingActionButton fab = findViewById(R.id.fabAddPassword);
        fab.setOnClickListener(v -> startActivity(new Intent(this, PasswordEditorActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPasswords();
    }

    private void loadPasswords() {
        passList.clear();
        Cursor cursor = dbHelper.getAllPasswords(true);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String app = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_APP_NAME));
                String user = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USERNAME));
                String pass = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PASSWORD));
                long time = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIMESTAMP));
                passList.add(new PassItem(id, app, user, pass, time));
            } while (cursor.moveToNext());
            cursor.close();
        }
        rv.setAdapter(new PassAdapter(passList));
    }

    class PassAdapter extends RecyclerView.Adapter<PassAdapter.ViewHolder> {
        List<PassItem> data;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        PassAdapter(List<PassItem> data) { this.data = data; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(32, 24, 32, 24);
            layout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView tvTitle = new TextView(parent.getContext());
            tvTitle.setTextSize(18);
            tvTitle.setTextColor(Color.BLACK);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvSub = new TextView(parent.getContext());
            tvSub.setTextSize(14);
            tvSub.setTextColor(Color.DKGRAY);

            TextView tvDate = new TextView(parent.getContext());
            tvDate.setTextSize(12);
            tvDate.setTextColor(Color.GRAY);

            layout.addView(tvTitle);
            layout.addView(tvSub);
            layout.addView(tvDate);

            return new ViewHolder(layout, tvTitle, tvSub, tvDate);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PassItem item = data.get(position);
            holder.tvTitle.setText(item.appName);
            holder.tvSub.setText(item.username);
            holder.tvDate.setText("Updated: " + sdf.format(new Date(item.timestamp)));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(PasswordsActivity.this, PasswordEditorActivity.class);
                intent.putExtra("ID", item.id);
                intent.putExtra("APP", item.appName);
                intent.putExtra("USER", item.username);
                intent.putExtra("PASS", item.password);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSub, tvDate;
            ViewHolder(View v, TextView t, TextView s, TextView d) {
                super(v); tvTitle = t; tvSub = s; tvDate = d;
            }
        }
    }
}