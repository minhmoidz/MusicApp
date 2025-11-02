package com.example.musicplayer.login;

import com.google.gson.annotations.SerializedName;

public class UpdateUserRequest {
    @SerializedName("full_name")
    private final String fullName;

    @SerializedName("phone")
    private final String phone;

    @SerializedName("address")
    private final String address;

    public UpdateUserRequest(String fullName, String phone, String address) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
    }
}
