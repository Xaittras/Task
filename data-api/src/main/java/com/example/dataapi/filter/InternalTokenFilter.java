package com.example.dataapi.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.dataapi.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Guards every request under /api/** by requiring a valid X-Internal-Token
 * header. This is how Service B trusts that a call actually came from
 * Service A rather than a random public caller.
 *
 * Note: the token value itself is never logged.
 */
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "X-Internal-Token";

    private final String expectedToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InternalTokenFilter(@Value("${app.internal-token}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Only guard the internal API; allow health checks through freely.
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String providedToken = request.getHeader(TOKEN_HEADER);

        if (providedToken == null || !constantTimeEquals(providedToken, expectedToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    objectMapper.writeValueAsString(new ErrorResponse("Missing or invalid X-Internal-Token"))
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
