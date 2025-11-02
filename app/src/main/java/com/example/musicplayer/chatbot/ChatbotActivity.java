package com.example.musicplayer.chatbot;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatbotActivity extends AppCompatActivity {

    private static final String TAG = "ChatbotActivity";
    private static final String BASE_URL = "http://192.168.30.28:5030";
    private static final String API_GET_HISTORY_URL = BASE_URL + "/api/chatbot/stream";
    private static final String API_POST_MESSAGE_URL = BASE_URL + "/api/chatbot/chat";

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient();
    private EditText editTextMessage;
    private ImageButton btnBackChatbot, buttonSend, btnClearChat;
    private LinearLayout emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        initViews();
        setupRecyclerView();
        setupListeners();
        updateEmptyState();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewChat);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        btnBackChatbot = findViewById(R.id.btnBackChatbot);
        btnClearChat = findViewById(R.id.btnClearChat);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Messages start from bottom
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Send button
        buttonSend.setOnClickListener(v -> sendMessageFromInput());

        // Enter key to send
        editTextMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendMessageFromInput();
                return true;
            }
            return false;
        });

        // Back button
        btnBackChatbot.setOnClickListener(v -> finish());

        // Clear chat button
        btnClearChat.setOnClickListener(v -> showClearChatDialog());
    }

    private void sendMessageFromInput() {
        String messageText = editTextMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            sendMessage(messageText);
            editTextMessage.setText("");
        }
    }

    private void sendMessage(String messageText) {
        // Add user message to UI
        ChatMessage userMessage = new ChatMessage("user", messageText);
        runOnUiThread(() -> {
            messageList.add(userMessage);
            adapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
            updateEmptyState();
        });

        // Show typing indicator (optional)
        ChatMessage typingMessage = new ChatMessage("assistant", "Typing...");
        int typingPosition = messageList.size();
        runOnUiThread(() -> {
            messageList.add(typingMessage);
            adapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
        });

        // Send to API
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("message", messageText);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON body", e);
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_POST_MESSAGE_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed: " + e.getMessage());
                runOnUiThread(() -> {
                    // Remove typing indicator
                    if (typingPosition < messageList.size()) {
                        messageList.remove(typingPosition);
                        adapter.notifyItemRemoved(typingPosition);
                    }

                    // Add error message
                    ChatMessage errorMessage = new ChatMessage(
                            "system",
                            "⚠️ Could not connect to server. Please check your connection."
                    );
                    messageList.add(errorMessage);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Server Response: " + responseBody);

                runOnUiThread(() -> {
                    // Remove typing indicator
                    if (typingPosition < messageList.size()) {
                        messageList.remove(typingPosition);
                        adapter.notifyItemRemoved(typingPosition);
                    }
                });

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response);
                    runOnUiThread(() -> {
                        ChatMessage errorMessage = new ChatMessage(
                                "system",
                                "Server error: " + response.code()
                        );
                        messageList.add(errorMessage);
                        adapter.notifyItemInserted(messageList.size() - 1);
                    });
                    return;
                }

                try {
                    JSONObject responseObject = new JSONObject(responseBody);
                    String replyText = responseObject.optString("response", "Sorry, I don't understand.");

                    ChatMessage botMessage = new ChatMessage("assistant", replyText);
                    runOnUiThread(() -> {
                        messageList.add(botMessage);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse JSON response: " + e.getMessage());
                }
            }
        });
    }

    private void showClearChatDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Làm mới đoạn chat")
                .setMessage("Bạn có chắc muốn làm mới đoạn chat")
                .setPositiveButton("Làm mới", (dialog, which) -> {
                    messageList.clear();
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .setNegativeButton("Hủy bỏ", null)
                .show();
    }

    private void updateEmptyState() {
        if (messageList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void fetchMessagesFromApi() {
        Request request = new Request.Builder()
                .url(API_GET_HISTORY_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response);
                    return;
                }

                String body = response.body() != null ? response.body().string() : null;
                if (body == null) {
                    Log.e(TAG, "Empty response body");
                    return;
                }

                try {
                    JSONArray arr = new JSONArray(body);
                    final List<ChatMessage> parsed = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String role = obj.optString("role", "unknown");
                        String content = obj.optString("content", "");
                        parsed.add(new ChatMessage(role, content));
                    }

                    runOnUiThread(() -> {
                        messageList.clear();
                        messageList.addAll(parsed);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                        if (!messageList.isEmpty()) {
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse JSON: " + e.getMessage());
                }
            }
        });
    }
}