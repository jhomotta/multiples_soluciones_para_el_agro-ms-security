package com.msagro.security.reactiveweb.support;

import com.msagro.security.model.auth.ClientContext;
import com.msagro.security.utility.AuthConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.UUID;

/**
 * Builds the {@link ClientContext} the use cases record against.
 *
 * <p>Everything here is read from the transport, never from the request body: a client that
 * could name its own IP address could poison the very evidence that {@code login_attempt} and
 * {@code security_audit} exist to preserve.</p>
 */
@Component
public class ClientContextResolver {

    /** Truncated to the column widths so an oversized header can never fail an insert. */
    private static final int MAX_USER_AGENT = 500;
    private static final int MAX_DEVICE_ID = 100;
    private static final int MAX_CORRELATION_ID = 64;

    public ClientContext resolve(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        String correlationId = trim(headers.getFirst(AuthConstants.CORRELATION_ID_HEADER), MAX_CORRELATION_ID);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        return ClientContext.builder()
                .ipAddress(resolveIp(exchange))
                .userAgent(trim(headers.getFirst(HttpHeaders.USER_AGENT), MAX_USER_AGENT))
                .deviceId(trim(headers.getFirst(AuthConstants.DEVICE_ID_HEADER), MAX_DEVICE_ID))
                .deviceInfo(trim(headers.getFirst(HttpHeaders.USER_AGENT), MAX_USER_AGENT))
                .correlationId(correlationId)
                .build();
    }

    /**
     * Prefers the first hop of {@code X-Forwarded-For} — behind a proxy the socket address is the
     * proxy, not the caller — and falls back to the socket address otherwise. Only trust the
     * header when the service really does sit behind a proxy that rewrites it.
     */
    private String resolveIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst(AuthConstants.FORWARDED_FOR_HEADER);
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress == null || remoteAddress.getAddress() == null
                ? null
                : remoteAddress.getAddress().getHostAddress();
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
