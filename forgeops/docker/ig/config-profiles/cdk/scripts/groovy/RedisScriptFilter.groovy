import java.net.URLEncoder
import java.util.Base64

import org.forgerock.http.protocol.Request
import org.forgerock.http.protocol.Response
import org.forgerock.http.protocol.Status

// --------------------------------------------------
// 1. Revocar el access token cuando el path termina en /delete
// --------------------------------------------------

if (request.uri.path.endsWith("/delete")) {

    logger.info("Redis Script Filter: revoking access token...")

    def revokeRequest = new Request()
    def baseUrl = idcBaseUrl.replaceAll('/+$', '')
    revokeRequest.setUri(
        "${baseUrl}/oauth2/realms/root/token/revoke"
    )
    revokeRequest.setMethod("POST")
    revokeRequest.headers.add(
        "Content-Type",
        "application/x-www-form-urlencoded"
    )
    def credential = "${revocationClientId}:${revocationClientSecret}"
    def encodedCredential = Base64.getEncoder().encodeToString(
        credential.getBytes("UTF-8")
    )
    revokeRequest.headers.add("Authorization", "Basic ${encodedCredential}")

    def accessToken = URLEncoder.encode(
        contexts.oauth2.accessToken.token,
        "UTF-8"
    )

    revokeRequest.setEntity("token=${accessToken}")

    return http.send(context, revokeRequest).thenAsync {
    revokeResponse ->
    logger.info("Token revocation endpoint returned ${revokeResponse.status}")

    if (!revokeResponse.status.successful) {
        logger.error("Token revocation failed")
        def response = new Response(Status.INTERNAL_SERVER_ERROR)
        response.headers.add("Content-Type", "application/json")
        response.setEntity('{"error":"token_revocation_failed"}')
        return response
    }

    return next.handle(context, request)
}

} else if (
    contexts.oauth2.accessToken.info.sub != contexts.amSession.username
) {

    logger.warn("Redis Script Filter: token subject does not match AM session username")

    def response = new Response(Status.UNAUTHORIZED)
    response.headers.add(
        "Content-Type",
        "application/json"
    )
    response.setEntity(
        '{"error":"invalid_session","message":"Token subject does not match AM session username"}'
    )
    return response
}

return next.handle(context, request)
