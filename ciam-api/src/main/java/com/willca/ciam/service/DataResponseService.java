package com.willca.ciam.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class DataResponseService {

    private static final Logger LOG = Logger.getLogger(DataResponseService.class);

    @Inject
    TokenService tokenService;

    @Context
    UriInfo uriInfo;

    @Context
    HttpHeaders headers;

    public Response respond(String operation, Map<String, Object> body, HttpHeaders headers) {
        // Request information
        String method = "POST";
        String url = uriInfo.getRequestUri().toString();
        String path = uriInfo.getPath();
        String query = uriInfo.getRequestUri().getRawQuery();

        String ciamAuthKey = "X-CIAM-Authorization";
        String consumerAuthKey = "X-Consumer-Authorization";
        String internalAuthKey = "X-Internal-Authorization";
        String subscriptionKey = "Ocp-Apim-Subscription-Key";
        String authKey = "Authorization";

        String hostKey = "Host";
        String contentTypeKey = "Content-Type";
        String acceptKey = "Accept";
        String userAgentKey = "User-Agent";
        String requestIdKey = "X-Request-ID";
        String forwardedForKey = "X-Forwarded-For";

        String ciamAuthValue = headers.getHeaderString(ciamAuthKey);
        String consumerAuthValue = headers.getHeaderString(consumerAuthKey);
        String internalAuthValue = headers.getHeaderString(internalAuthKey);
        String subscriptionValue = headers.getHeaderString(subscriptionKey);
        String authorizationValue = headers.getHeaderString(authKey);

        String hostValue = headers.getHeaderString(hostKey);
        String contentTypeValue = headers.getHeaderString(contentTypeKey);
        String acceptValue = headers.getHeaderString(acceptKey);
        String userAgentValue = headers.getHeaderString(userAgentKey);
        String requestIdValue = headers.getHeaderString(requestIdKey);
        String forwardedForValue = headers.getHeaderString(forwardedForKey);

        LOG.infof("""
                ========== CIAM API REQUEST ==========
                Operation:                    %s
                Method:                       %s
                URL:                          %s
                Path:                         %s
                Query:                        %s
                Host:                         %s
                Content-Type:                 %s
                Accept:                       %s
                User-Agent:                   %s
                X-Request-ID:                 %s
                X-Forwarded-For:              %s
                X-CIAM-Authorization:         %s
                X-Consumer-Authorization:     %s
                X-Internal-Authorization      %s
                Ocp-Apim-Subscription-Key:    %s
                Authorization:                %s
                body:                         %s
                ======================================
            """,
                operation,
                method,
                url,
                path,
                query,
                hostValue,
                contentTypeValue,
                acceptValue,
                userAgentValue,
                requestIdValue,
                forwardedForValue,
                ciamAuthValue,
                consumerAuthValue,
                internalAuthValue,
                subscriptionValue,
                authorizationValue,
                body
        );

        Map<String, Object> receivedRequest = new LinkedHashMap<>();
        receivedRequest.put("operation", operation);
        receivedRequest.put("method", method);
        receivedRequest.put("url", url);
        receivedRequest.put("path", path);
        receivedRequest.put("query", query);
        receivedRequest.put("service", "ciam-api");
        receivedRequest.put("status", "success");
        receivedRequest.put("timestamp", Instant.now().toString());
        receivedRequest.put("headersReceived", Map.ofEntries(
                Map.entry(ciamAuthKey, valueOrFalse(ciamAuthValue)),
                Map.entry(consumerAuthKey, valueOrFalse(consumerAuthValue)),
                Map.entry(internalAuthKey, valueOrFalse(internalAuthValue)),
                Map.entry(subscriptionKey, valueOrFalse(subscriptionValue)),
                Map.entry(authKey, valueOrFalse(authorizationValue)),
                Map.entry(hostKey, valueOrFalse(hostValue)),
                Map.entry(contentTypeKey, valueOrFalse(contentTypeValue)),
                Map.entry(acceptKey, valueOrFalse(acceptValue)),
                Map.entry(userAgentKey, valueOrFalse(userAgentValue)),
                Map.entry(requestIdKey, valueOrFalse(requestIdValue)),
                Map.entry(forwardedForKey, valueOrFalse(forwardedForValue))
        ));
        receivedRequest.put("bodyReceived", body == null ? Map.of() : body);

        Map<String, Object> tokenInfo = tokenService.inspectJwt(internalAuthValue);
        LOG.infof("Internal Token Info: %s", tokenInfo);
        receivedRequest.put("internalTokenInfo", tokenInfo);

        LOG.infof("Ending Operation...");
        return Response.ok(receivedRequest).build();
    }

    private Object valueOrFalse(String value) {
        return value == null || value.isBlank() ? false : value;
    }

}
