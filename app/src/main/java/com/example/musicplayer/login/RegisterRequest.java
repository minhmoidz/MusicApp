package com.example.musicplayer.login;

public class RegisterRequest {
    private final String username;
    private final String email;
    private final String password;
    private final String full_name;

    public RegisterRequest(String username, String email, String password, String fullName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.full_name = fullName;
    }
}
