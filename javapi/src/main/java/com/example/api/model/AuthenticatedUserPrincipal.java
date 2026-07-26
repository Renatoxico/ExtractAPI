package com.example.api.model;

public record AuthenticatedUserPrincipal(
    Long localUserId,
    String uid,
    String email,
    String name,
    String picture,
    boolean emailVerified
) {
}
