package com.jeevan.core.dto.response;

/** Small status + human message envelope for simple action responses. */
public record MessageResponse(String status, String message) {
}
