import groovy.json.JsonOutput

import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

logger.info("INTERNAL_TOKEN: initializing...")

def generateInternalToken() {

    logger.info("INTERNAL_TOKEN: Token Info = {}",
        JsonOutput.prettyPrint(JsonOutput.toJson(contexts.oauth2.accessToken.info))
    )

    String privateKeyPath = "/var/ig/secrets/jwt-private.pem"
    String privateKeyPem = new String(Files.readAllBytes(Paths.get(privateKeyPath)), "UTF-8")
    if (!privateKeyPem) {
        logger.error("INTERNAL_TOKEN: No se pudo leer la clave privada desde ${privateKeyPath}")
        return null;
    }
    logger.info("INTERNAL_TOKEN: genrando secretos ...");
    
    String privateKeyContent = privateKeyPem
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s", "")

    byte[] keyBytes = Base64.decoder.decode(privateKeyContent)

    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes)

    KeyFactory keyFactory = KeyFactory.getInstance("RSA")

    PrivateKey privateKey = keyFactory.generatePrivate(keySpec)

    long now = System.currentTimeMillis() / 1000L

    def header = [ alg: "RS256", typ: "JWT"]

    def loggedUser = contexts.oauth2.accessToken.info?.sub ?: "internal-user"
    Long ttlSeconds = 30L
    logger.info("INTERNAL_TOKEN: Preparando payload")

    def payload = [
        sub: loggedUser,
        iss: "forgerock-ig",
        aud: "backend",
        iat: now,
        exp: now + ttlSeconds,
        jti: UUID.randomUUID().toString()
    ]

    def encoder = Base64.urlEncoder.withoutPadding()

    String encodedHeader = encoder.encodeToString(JsonOutput.toJson(header).getBytes("UTF-8"))

    String encodedPayload = encoder.encodeToString(JsonOutput.toJson(payload).getBytes("UTF-8"))

    logger.info("INTERNAL_TOKEN: Header y payload generados")

    String signingInput = encodedHeader + "." + encodedPayload

    Signature signer = Signature.getInstance("SHA256withRSA")

    signer.initSign(privateKey)
    signer.update(signingInput.getBytes("UTF-8"))

    String signature = encoder.encodeToString(signer.sign())
    logger.info("INTERNAL_TOKEN: Firma generada y token interno creado")

    return signingInput + "." + signature
}

try {
    String internalToken = generateInternalToken()

    if (internalToken?.trim()) {
        request.getHeaders().put("X-Internal-Authorization", "Bearer " + internalToken)
        logger.info("INTERNAL_TOKEN: Token internogenerado y agregado")
    } else {
        logger.error("INTERNAL_TOKEN: No se genero token interno, continua sin header")
    }
} catch (Exception e) {
    logger.error("INTERNAL_TOKEN: Error generando token interno, continua sin header", e)
    throw e
}

return next.handle(context, request)
