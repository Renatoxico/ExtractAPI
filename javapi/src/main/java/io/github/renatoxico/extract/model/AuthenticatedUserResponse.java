package io.github.renatoxico.extract.model;

public record AuthenticatedUserResponse(
    Long localUserId,
    String uid,
    String email,
    String name,
    String picture,
    boolean emailVerified
) {}
