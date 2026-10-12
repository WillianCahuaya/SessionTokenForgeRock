import groovy.json.JsonOutput

import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

logger.info("INTERNAL_TOKEN_FILTER: initializing...")

String privateKeyPath = "/var/ig/secrets/jwt-private.pem"
logger.info("INTERNAL_TOKEN_FILTER: key cargado correctamente desde: {}", privateKeyPath)

def generateInternalToken = { String userId ->

    logger.info("INTERNAL_TOKEN_FILTER: Leyendo clave privada PEM (PKCS#8)");
    String pem = new String(Files.readAllBytes(Paths.get(privateKeyPath)), "UTF-8")

    String privateKeyContent = pem
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s", "")

    byte[] keyBytes = Base64.decoder.decode(privateKeyContent)
    logger.info("INTERNAL_TOKEN_FILTER: Bytes leidos correctamente..");

    def keySpec = new PKCS8EncodedKeySpec(keyBytes)
    def privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)

    long now = System.currentTimeMillis() / 1000L

    // Header JWT
    def header = [ alg: "RS256", typ: "JWT"]

    // Claims de ejemplo
    def payload = [
        iss: "forgerock-ig",
        sub: userId,
        iat: now,
        exp: now + 60L,
        jti: UUID.randomUUID().toString(),
        transaction: "TX-12345"
    ]
    logger.info("INTERNAL_TOKEN_FILTER: Payload generado correctamente");

    def encoder = Base64.urlEncoder.withoutPadding()

    String encodedHeader = encoder.encodeToString(JsonOutput.toJson(header).getBytes("UTF-8"))

    String encodedPayload = encoder.encodeToString(JsonOutput.toJson(payload).getBytes("UTF-8"))

    String signingInput = encodedHeader + "." + encodedPayload

    // Firmar con RSA + SHA-256
    Signature signer = Signature.getInstance("SHA256withRSA")
    signer.initSign(privateKey)
    signer.update(signingInput.getBytes("UTF-8"))

    String signature = encoder.encodeToString(signer.sign())
    logger.info("INTERNAL_TOKEN_FILTER: Signature generada correctamente");

    return signingInput + "." + signature
}

try {
    String internalToken = generateInternalToken("user123")
    request.getHeaders().put("X-Internal-Authorization", "Bearer " + internalToken)
    logger.info("INTERNAL_TOKEN_FILTER: Cabecera X-Internal-Authorization agregada token: {}", internalToken)
} catch (Exception e) {
    logger.error("INTERNAL_TOKEN_FILTER: Error al agregar la cabecera", e)
    throw e
}


return next.handle(context, request).then { response ->
    logger.info("INTERNAL_TOKEN_FILTER: Response status = {}", response.status)
    return response
}
