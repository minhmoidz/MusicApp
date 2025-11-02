package com.example.musicplayer.profile;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Handle Back button click
        TextView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        // Example: Handle Streaming Quality click
        findViewById(R.id.btnStreamingQuality).setOnClickListener(v -> {
            Toast.makeText(this, "Mở cài đặt chất lượng stream", Toast.LENGTH_SHORT).show();
        });

        // Example: Handle Logout click
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            // You should delegate the logout logic to MainActivity
            // or a dedicated auth manager, but for now, we just show a toast.
            Toast.makeText(this, "Đăng xuất...", Toast.LENGTH_SHORT).show();
            // In a real app, you would call a method like:
            // AuthManager.getInstance().logout(this);
        });
    }
}
