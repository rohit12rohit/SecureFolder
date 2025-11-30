package com.example.securefolder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.securefolder.R;
import com.example.securefolder.ui.modules.NotesActivity;
import com.example.securefolder.ui.modules.PasswordsActivity;
import com.example.securefolder.ui.modules.PhotosActivity;
import com.example.securefolder.ui.modules.SettingsActivity;
import com.example.securefolder.ui.modules.VideosActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupGrid();
    }

    private void setupGrid() {
        CardView cardPhotos = findViewById(R.id.cardPhotos);
        CardView cardVideos = findViewById(R.id.cardVideos);
        CardView cardNotes = findViewById(R.id.cardNotes);
        CardView cardPasswords = findViewById(R.id.cardPasswords);
        CardView cardSettings = findViewById(R.id.cardSettings);

        cardPhotos.setOnClickListener(v -> startActivity(new Intent(this, PhotosActivity.class)));
        cardVideos.setOnClickListener(v -> startActivity(new Intent(this, VideosActivity.class)));
        cardNotes.setOnClickListener(v -> startActivity(new Intent(this, NotesActivity.class)));
        cardPasswords.setOnClickListener(v -> startActivity(new Intent(this, PasswordsActivity.class)));
        cardSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }
}