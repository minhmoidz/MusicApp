package com.example.musicplayer.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;
import com.example.musicplayer.api.SpotifyApi; // Assuming this is your main API interface

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final String API_BASE_URL = "http://192.168.30.28:5030/";
    private static final int TOKEN_EXPIRATION_SECONDS = 604800; // 7 days

    private EditText etFullName, etUsername, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private SpotifyApi authApi;
    private LoginActivity.SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = new LoginActivity.SessionManager(this);
        setupRetrofit();
        initViews();
        setupListeners();
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        authApi = retrofit.create(SpotifyApi.class);
    }

    private void initViews() {
        etFullName = findViewById(R.id.etRegisterFullName);
        etUsername = findViewById(R.id.etRegisterUsername);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> handleRegister());
        tvGoToLogin.setOnClickListener(v -> {
            // Go back to LoginActivity
            finish();
        });
    }

    private void handleRegister() {
        String fullName = etFullName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        RegisterRequest registerRequest = new RegisterRequest(username, email, password, fullName);

        authApi.register(registerRequest).enqueue(new Callback<LoginActivity.AuthTokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginActivity.AuthTokenResponse> call, @NonNull Response<LoginActivity.AuthTokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Registration successful, auto-login the user
                    LoginActivity.AuthTokenResponse tokenResponse = response.body();
                    sessionManager.saveToken(tokenResponse.accessToken, TOKEN_EXPIRATION_SECONDS);

                    Log.d(TAG, "Registration successful. Token saved.");
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                    // Navigate to MainActivity
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // Handle registration error (e.g., username/email already exists)
                    Toast.makeText(RegisterActivity.this, "Tên đăng nhập hoặc email đã tồn tại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginActivity.AuthTokenResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Registration API call failed: ", t);
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
