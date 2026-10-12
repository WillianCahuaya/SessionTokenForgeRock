package com.willca.ciam.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TokenService {

    private static final Logger LOG = Logger.getLogger(TokenService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Map<String, Object> inspectJwt(String authorizationHeader) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            result.put("present", false);
            return result;
        }

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7).trim()
                : authorizationHeader.trim();

        result.put("present", true);

        try {
            String[] parts = token.split("\\.");


            if (parts.length != 3) {
                result.put("format", "not a JWT");
                result.put("decoded", false);
                return result;
            }

            byte[] payloadBytes = Base64.getUrlDecoder()
                    .decode(parts[1]);

            JsonNode payload = OBJECT_MAPPER.readTree(new String(payloadBytes, StandardCharsets.UTF_8));

            result.put("format", "JWT");
            result.put("decoded", true);
            result.put("sub", payload.path("sub").asText(null));
            result.put("iss", payload.path("iss").asText(null));
            result.put("aud", payload.has("aud") ? payload.get("aud").toString() : null);
            result.put("scope", payload.path("scope").asText(null));

            long now = Instant.now().getEpochSecond();

            if (payload.has("iat") && payload.get("iat").canConvertToLong()) {
                long issuedAt = payload.get("iat").asLong();
                result.put("issuedAt",
                        Instant.ofEpochSecond(issuedAt).toString());
            }

            if (payload.has("exp") && payload.get("exp").canConvertToLong()) {
                long expiresAt = payload.get("exp").asLong();

                result.put("expiresAt",
                        Instant.ofEpochSecond(expiresAt).toString());
                result.put("expired", now >= expiresAt);
                result.put("secondsRemaining",
                        Math.max(0L, expiresAt - now));
            }

        } catch (Exception e) {
            result.put("decoded", false);
            result.put("error", "Unable to decode JWT payload");
        }

        return result;
    }

}