package dev.jefersonsiqueira.clinicflow.auth;

public record LoginResponse(String token, long expiresInSeconds, Role role) {}
