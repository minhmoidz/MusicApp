package com.example.myapplication.profile;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView tvVersion = findViewById(R.id.tvVersion);
        TextView tvDescription = findViewById(R.id.tvDescription);

        if (tvVersion != null) {
            tvVersion.setText("Phiên bản 1.0.0");
        }

        if (tvDescription != null) {
            tvDescription.setText("Zing MP3 - Ứng dụng nghe nhạc trực tuyến\n\n" +
                    "Tính năng:\n" +
                    "• Nghe nhạc trực tuyến\n" +
                    "• Tạo playlist\n" +
                    "• Yêu thích bài hát\n" +
                    "• Lịch sử phát\n" +
                    "• Và nhiều tính năng khác...");
        }
    }
}
