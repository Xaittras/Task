package com.example.authapi.security;

import java.util.UUID;

/**
 * Principal object placed into the SecurityContext by JwtAuthFilter.
 * Controllers can access it via @AuthenticationPrincipal or
 * SecurityContextHolder to know which user made the request.
 */
public record AuthenticatedUser(UUID id, String email) {
}
