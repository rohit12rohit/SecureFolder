package com.example.securefolder.ui.modules;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Settings (Coming in Phase 7)");
        tv.setTextSize(24);
        tv.setPadding(50,50,50,50);
        setContentView(tv);
    }
}