package com.example.securefolder.ui.modules;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

public class VideosActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Videos Vault (Coming in Phase 5)");
        tv.setTextSize(24);
        tv.setPadding(50,50,50,50);
        setContentView(tv);
    }
}