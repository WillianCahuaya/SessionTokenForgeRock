import org.forgerock.http.protocol.Response
import org.forgerock.http.protocol.Status

logger.info("Security Headers Filter: processing request...")

return next.handle(context, request).thenOnResult{ Response response ->

    logger.info("Security Headers Filter: modifying response to add security headers")

    def securityHeaders = [
        "Content-Security-Policy":
            "default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'",

        "X-Content-Type-Options": "nosniff",

        "Referrer-Policy": "strict-origin-when-cross-origin",

        "Permissions-Policy": "geolocation=(), microphone=(), camera=(), payment=(), accelerometer=(), gyroscope=()",

        "Strict-Transport-Security": "max-age=31536000; includeSubDomains; preload"
    ]

    def addSecurityHeader = { String headerName, String headerValue ->
        response.headers.put(headerName, headerValue)
    }

    securityHeaders.each { headerName, headerValue ->
        addSecurityHeader(headerName, headerValue)
    }

    return response
})
