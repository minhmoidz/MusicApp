package com.example.musicplayer.profile;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.R;
import com.example.musicplayer.api.SpotifyApi;
import com.example.musicplayer.login.LoginActivity;
import com.example.musicplayer.login.UpdateUserRequest;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileActivity extends AppCompatActivity {

    private EditText etProfileFullName, etProfileEmail, etProfilePhone, etProfileAddress;
    private Button btnSaveChanges, btnChangePassword;
    private SpotifyApi spotifyApi;
    private LoginActivity.SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new LoginActivity.SessionManager(this);

        setupRetrofit();
        initViews();
        loadUserData();
        setupListeners();
    }

    private void setupRetrofit() {
        // CORRECT IMPLEMENTATION: Use an Interceptor to add the auth token
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = sessionManager.getAccessToken();
                    okhttp3.Request.Builder builder = chain.request().newBuilder();
                    if (token != null) {
                        builder.header("Authorization", "Bearer " + token);
                    }
                    return chain.proceed(builder.build());
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.30.28:5030/")
                .client(okHttpClient) // Use the client with the interceptor
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        spotifyApi = retrofit.create(SpotifyApi.class);
    }

    private void initViews() {
        etProfileFullName = findViewById(R.id.etProfileFullName);
        etProfileEmail = findViewById(R.id.etProfileEmail);
        etProfilePhone = findViewById(R.id.etProfilePhone);
        etProfileAddress = findViewById(R.id.etProfileAddress);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        etProfileEmail.setEnabled(false);
    }

    private void loadUserData() {
        String fullName = getIntent().getStringExtra("USER_NAME");
        String email = getIntent().getStringExtra("USER_EMAIL");

        etProfileFullName.setText(fullName);
        etProfileEmail.setText(email);
    }

    private void setupListeners() {
        btnSaveChanges.setOnClickListener(v -> handleSaveChanges());

        btnChangePassword.setOnClickListener(v -> {
            Toast.makeText(this, "Chuyển đến màn hình đổi mật khẩu", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleSaveChanges() {
        String fullName = etProfileFullName.getText().toString().trim();
        String phone = etProfilePhone.getText().toString().trim();
        String address = etProfileAddress.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Họ và tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        UpdateUserRequest request = new UpdateUserRequest(fullName, phone, address);

        spotifyApi.updateProfile(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ProfileActivity.this, "Lỗi cập nhật hồ sơ. Mã lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
