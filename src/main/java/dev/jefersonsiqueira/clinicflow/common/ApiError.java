package dev.jefersonsiqueira.clinicflow.common;

/** The one error shape every endpoint returns, whatever went wrong. */
public record ApiError(String field, String message) {}
