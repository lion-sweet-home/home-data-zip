package org.example.homedatazip.auth.dto;

public record LoginRequest (
    String email,
    String password
){}
