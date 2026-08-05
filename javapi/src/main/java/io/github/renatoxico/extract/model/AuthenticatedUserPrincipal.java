package io.github.renatoxico.extract.model;

public record AuthenticatedUserPrincipal(
    Long localUserId,
    String uid,
    String email,
    String name,
    String picture,
    boolean emailVerified
) {
}
