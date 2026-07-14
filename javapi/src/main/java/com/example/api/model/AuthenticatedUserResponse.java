package com.example.api.model;

public record AuthenticatedUserResponse(
    String uid,
    String email,
    String name,
    String picture,
    boolean emailVerified
) {}
