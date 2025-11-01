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

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;
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

    /**
     * Manages user session, storing the auth token securely.
     */
    public static class SessionManager {
        private static final String PREF_NAME = "AppSession";
        private static final String KEY_ACCESS_TOKEN = "access_token";
        private static final String KEY_EXPIRES_AT = "expires_at";

        private final SharedPreferences prefs;

        public SessionManager(Context context) {
            prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        public void saveToken(String accessToken, int expiresInSeconds) {
            long expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
            editor.putLong(KEY_EXPIRES_AT, expiresAt);
            editor.apply();
        }

        public String getAccessToken() {
            return prefs.getString(KEY_ACCESS_TOKEN, null);
        }

        public boolean isTokenValid() {
            long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0);
            return getAccessToken() != null && System.currentTimeMillis() < expiresAt;
        }
    }

    /**
     * Data model for the login request body.
     */
    public static class LoginRequest {
        private final String username;
        private final String password;

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    /**
     * Data model for the successful authentication response.
     */
    public static class AuthTokenResponse {
        @SerializedName("access_token")
        public String accessToken;

        @SerializedName("user")
        public User user;

        public static class User {
            @SerializedName("full_name")
            public String fullName;
        }
    }

    /**
     * Retrofit interface for authentication APIs.
     */
    public interface AuthApi {
        @POST("/api/auth/login")
        Call<AuthTokenResponse> login(@Body LoginRequest loginRequest);
    }

    // --- Activity Implementation ---

    private static final String TAG = "LoginActivity";
    private static final String API_BASE_URL = "http://192.168.30.28:5030/";
    private static final int TOKEN_EXPIRATION_SECONDS = 604800; // 7 days

    private SessionManager sessionManager;
    private AuthApi authApi;

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (sessionManager.isTokenValid()) {
            Log.d(TAG, "Token is valid, starting MainActivity");
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
        authApi = retrofit.create(AuthApi.class);
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvGoToRegister.setOnClickListener(v -> 
            Toast.makeText(this, "Chức năng đăng ký chưa được cài đặt", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Kiểm tra tài khoản mặc định trước
        if (username.equals("admin") && password.equals("123456")) {
            sessionManager.saveToken("default_token_123", TOKEN_EXPIRATION_SECONDS);
            Toast.makeText(this, "Đăng nhập mặc định thành công!", Toast.LENGTH_SHORT).show();
            startMainActivity();
            return;
        }

        // Nếu không trùng thì gọi API thật
        LoginRequest loginRequest = new LoginRequest(username, password);

        authApi.login(loginRequest).enqueue(new Callback<AuthTokenResponse>() {
            @Override
            public void onResponse(Call<AuthTokenResponse> call, Response<AuthTokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthTokenResponse tokenResponse = response.body();
                    sessionManager.saveToken(tokenResponse.accessToken, TOKEN_EXPIRATION_SECONDS);
                    Log.d(TAG, "Login successful. Token saved.");
                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    startMainActivity();
                } else {
                    Toast.makeText(LoginActivity.this, "Tên đăng nhập hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthTokenResponse> call, Throwable t) {
                Log.e(TAG, "Login API call failed: ", t);
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
