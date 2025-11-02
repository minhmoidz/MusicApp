package com.example.musicplayer.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.musicplayer.api.SpotifyApi;
import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class LoginActivity extends AppCompatActivity {

    // --- Nested classes and interfaces to keep logic in one file ---

    public static class SessionManager {
        private static final String TAG = "SessionManager";
        private static final String PREF_NAME = "AppSession";
        private static final String KEY_ACCESS_TOKEN = "access_token";
        private static final String KEY_EXPIRES_AT = "expires_at";
        private final SharedPreferences prefs;
        private final Context context;

        public SessionManager(Context context) {
            this.context = context.getApplicationContext();
            prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        public void saveToken(String accessToken, int expiresInSeconds) {
            if (accessToken == null || accessToken.trim().isEmpty()) {
                Log.e(TAG, "❌ Attempted to save null or empty token!");
                return;
            }

            long expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
            editor.putLong(KEY_EXPIRES_AT, expiresAt);
            boolean saved = editor.commit(); // Use commit() instead of apply() to ensure immediate save

            if (saved) {
                Log.d(TAG, "✅ Token saved successfully. Expires in " + expiresInSeconds + " seconds");
                Log.d(TAG, "🔑 Token preview: " + accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
            } else {
                Log.e(TAG, "❌ Failed to save token to SharedPreferences!");
            }
        }

        /**
         * Get access token without validation (may return expired token)
         */
        public String getAccessToken() {
            String token = prefs.getString(KEY_ACCESS_TOKEN, null);
            if (token == null || token.trim().isEmpty()) {
                Log.w(TAG, "⚠️ getAccessToken() returned null or empty token");
                return null;
            }
            return token;
        }

        /**
         * Get access token only if it's valid (not null, not empty, not expired)
         * Returns null if token is invalid
         */
        public String getValidAccessToken() {
            String token = prefs.getString(KEY_ACCESS_TOKEN, null);

            // Check if token exists
            if (token == null || token.trim().isEmpty()) {
                Log.e(TAG, "❌ Token is null or empty!");
                return null;
            }

            // Check if token is expired
            long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0);
            long currentTime = System.currentTimeMillis();

            if (currentTime >= expiresAt) {
                long expiredSince = (currentTime - expiresAt) / 1000; // seconds
                Log.e(TAG, "❌ Token expired " + expiredSince + " seconds ago!");
                Log.e(TAG, "🔴 Expired at: " + new java.util.Date(expiresAt));
                return null;
            }

            // Token is valid
            long timeRemaining = (expiresAt - currentTime) / 1000; // seconds
            Log.d(TAG, "✅ Token is valid. Time remaining: " + timeRemaining + " seconds (" + (timeRemaining / 3600)
                    + " hours)");

            return token;
        }

        /**
         * Check if token is valid (exists and not expired)
         */
        public boolean isTokenValid() {
            String token = prefs.getString(KEY_ACCESS_TOKEN, null);
            long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0);
            long currentTime = System.currentTimeMillis();

            // Log detailed status
            if (token == null || token.trim().isEmpty()) {
                Log.w(TAG, "⚠️ isTokenValid(): Token is null or empty");
                return false;
            }

            if (currentTime >= expiresAt) {
                long expiredSince = (currentTime - expiresAt) / 1000;
                Log.w(TAG, "⚠️ isTokenValid(): Token expired " + expiredSince + " seconds ago");
                return false;
            }

            long timeRemaining = (expiresAt - currentTime) / 1000;
            Log.d(TAG, "✅ isTokenValid(): Token valid for " + timeRemaining + " more seconds");
            return true;
        }

        /**
         * Get remaining time in seconds before token expires
         * Returns 0 if token is expired or doesn't exist
         */
        public long getRemainingTimeSeconds() {
            long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0);
            long currentTime = System.currentTimeMillis();
            long remaining = (expiresAt - currentTime) / 1000;
            return Math.max(0, remaining);
        }

        public void clear() {
            Log.d(TAG, "🗑️ Clearing session data");
            prefs.edit().clear().apply();
        }
    }

    public static class LoginRequest {
        private final String username;
        private final String password;

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static class AuthTokenResponse {
        @SerializedName("access_token")
        public String accessToken;

        @SerializedName("user")
        public User user;

        public static class User {
            @SerializedName("email")
            public String email;

            @SerializedName("full_name")
            public String fullName;
        }
    }

    // --- Activity Implementation ---

    private static final String TAG = "LoginActivity";
    private static final String API_BASE_URL = "http://192.168.30.28:5030/";
    private static final int TOKEN_EXPIRATION_SECONDS = 604800; // 7 days

    private SessionManager sessionManager;
    private SpotifyApi authApi;

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (sessionManager.isTokenValid()) {
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

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
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());
        tvGoToRegister.setOnClickListener(v -> {
            // Navigate to RegisterActivity
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        LoginRequest loginRequest = new LoginRequest(username, password);

        authApi.login(loginRequest).enqueue(new Callback<AuthTokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthTokenResponse> call,
                    @NonNull Response<AuthTokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthTokenResponse tokenResponse = response.body();
                    sessionManager.saveToken(tokenResponse.accessToken, TOKEN_EXPIRATION_SECONDS);
                    startMainActivity();
                } else {
                    Toast.makeText(LoginActivity.this, "Tên đăng nhập hoặc mật khẩu không đúng", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthTokenResponse> call, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
