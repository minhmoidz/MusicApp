package com.example.musicplayer.profile;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.R;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton btnBackProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        btnBackProfile = findViewById(R.id.btnBackProfile);

        // TODO: Load user profile data
        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            tvTitle.setText("Hồ sơ cá nhân");
        }

        setupBackButton();
    }

    private void setupBackButton() {
        btnBackProfile.setOnClickListener(v -> finish());
    }
}