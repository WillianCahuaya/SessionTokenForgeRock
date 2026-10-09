# ForgeRock AM — Recuperar contraseña de `amadmin`

Guía rápida para recuperar la contraseña de `amadmin`, autenticarse contra ForgeRock Access Management (AM) y ejecutar operaciones administrativas mediante REST API.

> **Importante:** no guardar contraseñas ni `tokenId` reales directamente en este documento. Utilizar variables de entorno o reemplazar los valores por placeholders.

---

## 1. Recuperar la contraseña de `amadmin`

En un entorno ForgeOps, primero consultar la información disponible:

```bash
./bin/forgeops info | grep amadmin
```

Ejemplo:

```text
amadmin-password > TU_PASSWORD
```

### ¿Qué obtenemos?

El valor asociado a `amadmin` corresponde a la **contraseña del usuario administrativo `amadmin`**.

Es decir:

```text
amadmin
   │
   └── password
          │
          └── contraseña utilizada para autenticarse en AM
```

Podemos guardarla temporalmente como variable:

```bash
export AMADMIN_PASSWORD='TU_PASSWORD'
source ~/.zshrc
```

Así evitamos repetir la contraseña directamente en todos los comandos.

---

# 2. Autenticar `amadmin` contra ForgeRock AM

Endpoint:

```text
POST /am/json/realms/root/authenticate
```

Comando:

```bash
curl -sS \
  -X POST \
  "http://localhost:8080/am/json/realms/root/authenticate" \
  -H "Host: am" \
  -H "Content-Type: application/json" \
  -H "X-OpenAM-Username: amadmin" \
  -H "X-OpenAM-Password: ${AMADMIN_PASSWORD}" \
  -H "Accept-API-Version: resource=2.0" \
  -d '{}'
```

### ¿Qué hace?

Le estamos diciendo a AM:

```text
Usuario:
    amadmin

Password:
    AMADMIN_PASSWORD

        ↓

POST /authenticate

        ↓

ForgeRock AM valida las credenciales

        ↓

Devuelve un token de sesión
```

Una respuesta exitosa tiene una estructura similar a:

```json
{
    "tokenId": "TU_ADMIN_TOKEN",
    "successUrl": "/am/console",
    "realm": "/"
}
```

El valor importante es:

```text
tokenId
```

Este **NO es la contraseña de `amadmin`**.

Es un **token de sesión autenticado** que utilizaremos para realizar llamadas administrativas.

Podemos guardarlo:

```bash
export ADMIN_TOKEN='TU_ADMIN_TOKEN'
source ~/.zshrc
```

---

# 3. Diferencia entre contraseña y `tokenId`

Es importante no confundir estos dos valores.

```text
amadmin password
       │
       │ autentica
       ▼
POST /authenticate
       │
       ▼
tokenId
       │
       │ autoriza llamadas posteriores
       ▼
REST API de AM
```

### Contraseña

```text
AMADMIN_PASSWORD
```

Sirve para demostrar:

> "Soy el usuario `amadmin`."

### Token

```text
ADMIN_TOKEN
```

Sirve para demostrar:

> "Ya estoy autenticado y tengo una sesión válida."

Por eso las llamadas administrativas normalmente utilizan:

```http
iPlanetDirectoryPro: <TOKEN>
```

---

# 4. Crear un usuario

Endpoint:

```text
POST /am/json/realms/root/users/?_action=create
```

Comando:

```bash
curl -sS \
  -X POST \
  "http://localhost:8080/am/json/realms/root/users/?_action=create" \
  -H "Host: am" \
  -H "Content-Type: application/json" \
  -H "Accept-API-Version: protocol=2.1,resource=3.0" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
  -d '{
    "username": "testuser",
    "userpassword": "Password123!",
    "mail": "testuser@example.com",
    "givenName": "Test",
    "sn": "User"
  }'
```

### ¿Qué hace?

Utiliza el `ADMIN_TOKEN` para ejecutar una operación administrativa:

```text
ADMIN_TOKEN
     │
     ▼
POST /users/?_action=create
     │
     ▼
ForgeRock AM
     │
     └── crea testuser
```

El usuario creado tendrá:

```text
username    = testuser
password    = Password123!
email       = testuser@example.com
givenName   = Test
sn          = User
```

> Para producción, utilizar una contraseña segura y nunca dejarla escrita directamente en scripts o documentación.

---

# 5. Consultar un usuario

Endpoint:

```text
GET /am/json/realms/root/users/{username}
```

Comando:

```bash
curl -sS \
  -H "Host: am" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
  "http://localhost:8080/am/json/realms/root/users/testuser?_fields=username,uid,inetUserStatus,givenName,sn"
```

### ¿Qué estamos consultando?

Solicitamos únicamente determinados atributos:

```text
username
uid
inetUserStatus
givenName
sn
```

La parte:

```text
?_fields=username,uid,inetUserStatus,givenName,sn
```

permite limitar los campos devueltos por AM.

---

# 6. Actualizar la contraseña de un usuario

Endpoint:

```text
PUT /am/json/realms/root/users/{username}
```

Comando:

```bash
curl -sS \
  -X PUT \
  "http://localhost:8080/am/json/realms/root/users/testuser" \
  -H "Host: am" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "Accept-API-Version: protocol=2.1,resource=4.0" \
  -d '{
    "userpassword": "Password123!"
  }'
```

### Flujo

```text
ADMIN_TOKEN
     │
     ▼
PUT /users/testuser
     │
     ▼
AM localiza testuser
     │
     ▼
actualiza userpassword
```

---

# 7. Autenticarse como `testuser`

Una vez creado el usuario, podemos probar sus credenciales.

```bash
curl -sS \
  -X POST \
  "http://localhost:8080/am/json/realms/root/authenticate" \
  -H "Host: am" \
  -H "Content-Type: application/json" \
  -H "X-OpenAM-Username: testuser" \
  -H "X-OpenAM-Password: Password123!" \
  -H "Accept-API-Version: resource=2.0" \
  -d '{}'
```

### ¿Qué estamos comprobando?

```text
testuser
   +
Password123!
   │
   ▼
/authenticate
   │
   ▼
AM
   │
   ├── credenciales correctas → tokenId
   │
   └── credenciales incorrectas → error
```

---

# 8. Consultar un OAuth 2.0 Client

Para consultar la configuración de un cliente OAuth/OIDC específico:

```bash
curl -sS \
  -H "Host: am" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
  -H "Accept-API-Version: resource=1.0" \
  "http://localhost:8080/am/json/realms/root/realm-config/agents/OAuth2Client/mobile-simulator"
```

### ¿Qué representa?

Estamos consultando el agente OAuth 2.0:

```text
OAuth2Client
     │
     └── mobile-simulator
```

Este recurso representa la configuración de un cliente OAuth registrado en AM.

---

# 9. Consultar todos los clientes OAuth 2.0

```bash
curl -sS \
  -H "Host: am" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
  -H "Accept-API-Version: resource=1.0" \
  "http://localhost:8080/am/json/realms/root/realm-config/agents/OAuth2Client?_queryFilter=true"
```

### Diferencia

Consultar un cliente:

```text
.../OAuth2Client/mobile-simulator
```

Consultar los clientes:

```text
.../OAuth2Client?_queryFilter=true
```

---

# 10. Consultar la configuración OAuth/OIDC del Realm

Endpoint:

```text
GET /realm-config/services/oauth-oidc
```

Comando:

```bash
curl -sS \
  -H "Host: am" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
  -H "Accept-API-Version: resource=2.0" \
  "http://localhost:8080/am/json/realms/root/realm-config/services/oauth-oidc"
```

### ¿Qué consultamos?

La configuración del servicio:

```text
Realm
 │
 └── OAuth/OIDC
       │
       ├── configuración OAuth
       ├── configuración OIDC
       └── comportamiento de autenticación/autorización
```

---

# 11. Modificar `passwordGrantAuthService`

## Configurar `DataStore`

```bash
curl -sS \
  -X PUT \
  "http://localhost:8080/am/json/realms/root/realm-config/services/oauth-oidc" \
  -H "Host: am" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "Accept-API-Version: resource=2.0" \
  -d '{
    "passwordGrantAuthService": "DataStore"
  }'
```

## Configurar `ldapService`

```bash
curl -sS \
  -X PUT \
  "http://localhost:8080/am/json/realms/root/realm-config/services/oauth-oidc" \
  -H "Host: am" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "Accept-API-Version: resource=2.0" \
  -d '{
    "passwordGrantAuthService": "ldapService"
  }'
```

### ¿Qué significa?

Se está indicando qué servicio de autenticación utilizará AM para el Password Grant.

Conceptualmente:

```text
Password Grant
      │
      ▼
passwordGrantAuthService
      │
      ├── DataStore
      │
      └── ldapService
```

Por ejemplo:

```text
passwordGrantAuthService = ldapService
```

significa que el flujo Password Grant utilizará el servicio de autenticación configurado como `ldapService`.

---

# 12. Consultar la configuración de `ldapService`

```bash
curl -sS \
  -H "Host: am" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
  -H "Accept-API-Version: resource=0.0" \
  "http://localhost:8080/am/json/realms/root/realm-config/authentication/modules/ldap/ldapService"
```

### ¿Qué obtenemos?

La configuración específica del módulo:

```text
Authentication Module
        │
        └── ldap
              │
              └── ldapService
```

---

# 13. Consultar los módulos LDAP

```bash
curl -sS \
  -H "Host: am" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
  -H "Accept-API-Version: resource=0.0" \
  "http://localhost:8080/am/json/realms/root/realm-config/authentication/modules/ldap?_queryFilter=true"
```

Esto permite consultar los módulos LDAP configurados en el Realm.

---

# 14. Autenticar `testuser` utilizando `ldapService`

```bash
curl -sS \
  -X POST \
  "http://localhost:8080/am/json/realms/root/authenticate?authIndexType=service&authIndexValue=ldapService" \
  -H "Host: am" \
  -H "Content-Type: application/json" \
  -H "X-OpenAM-Username: testuser" \
  -H "X-OpenAM-Password: Password123!" \
  -H "Accept-API-Version: resource=2.0, protocol=1.0" \
  -d '{}'
```

### Parte importante

Normalmente:

```text
/authenticate
```

utiliza el flujo de autenticación configurado por defecto.

Aquí estamos indicando explícitamente:

```text
authIndexType=service
authIndexValue=ldapService
```

Por lo tanto:

```text
testuser
   │
   ├── username
   └── password
          │
          ▼
    ldapService
          │
          ▼
    autenticación
          │
          ▼
      tokenId
```

---

# 15. Obtener un OAuth 2.0 Access Token mediante Password Grant

Una vez configurado el cliente OAuth y el servicio de autenticación, podemos solicitar un Access Token.

```bash
curl -k \
  --resolve am:8080:127.0.0.1 \
  -X POST \
  "http://am:8080/am/oauth2/access_token" \
  -u 'mobile-simulator:TU_CLIENT_SECRET' \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'username=testuser' \
  --data-urlencode 'password=Password123!' \
  --data-urlencode 'scope=openid profile' \
  --data-urlencode 'auth_chain=ldapService'
```

### Flujo completo

```text
                 ForgeRock AM
                      │
                      │
             OAuth 2.0 Client
                      │
              mobile-simulator
                      │
                      ▼
              /oauth2/access_token
                      │
                      │
              grant_type=password
                      │
                      ▼
                 testuser
                      │
                      ▼
                ldapService
                      │
                      ▼
              autenticación OK
                      │
                      ▼
                Access Token
```

---

# 16. Resumen de los diferentes tipos de credenciales

Durante estas pruebas aparecen varios valores diferentes.

| Valor               | Ejemplo conceptual      | Uso                                  |
| ------------------- | ----------------------- | ------------------------------------ |
| `amadmin` password  | `AMADMIN_PASSWORD`      | Autenticar al administrador          |
| `ADMIN_TOKEN`       | `tokenId` de `amadmin`  | Ejecutar operaciones administrativas |
| `testuser` password | `Password123!`          | Autenticar al usuario                |
| User `tokenId`      | `tokenId` de `testuser` | Sesión autenticada del usuario       |
| OAuth Client ID     | `mobile-simulator`      | Identificar el cliente OAuth         |
| OAuth Client Secret | `TU_CLIENT_SECRET`      | Autenticar el cliente OAuth          |
| OAuth Access Token  | `access_token`          | Acceder a APIs protegidas            |

---

# 17. Flujo completo

```text
┌──────────────────────────────┐
│ ./bin/forgeops info          │
│ grep amadmin                 │
└──────────────┬───────────────┘
               │
               ▼
       AMADMIN_PASSWORD
               │
               ▼
┌──────────────────────────────┐
│ POST /authenticate           │
│ username = amadmin           │
│ password = AMADMIN_PASSWORD  │
└──────────────┬───────────────┘
               │
               ▼
          ADMIN_TOKEN
               │
               ▼
     ┌─────────┴──────────┐
     │                    │
     ▼                    ▼
Crear usuario       Consultar AM
     │                    │
     ▼                    ▼
testuser             OAuth/OIDC
     │                configuración
     │
     ▼
Actualizar password
     │
     ▼
Autenticar testuser
     │
     ▼
 ldapService
     │
     ▼
 OAuth Password Grant
     │
     ▼
 Access Token
```

---

# 18. Comandos esenciales — versión resumida

Si únicamente necesitamos repetir el procedimiento básico:

### 1. Obtener password de `amadmin`

```bash
./bin/forgeops info | grep amadmin
```

### 2. Guardar password

```bash
export AMADMIN_PASSWORD='TU_PASSWORD'
```

### 3. Autenticar administrador

```bash
export ADMIN_TOKEN=$(
  curl -sS \
    -X POST \
    "http://localhost:8080/am/json/realms/root/authenticate" \
    -H "Host: am" \
    -H "Content-Type: application/json" \
    -H "X-OpenAM-Username: amadmin" \
    -H "X-OpenAM-Password: ${AMADMIN_PASSWORD}" \
    -H "Accept-API-Version: resource=2.0" \
    -d '{}' \
  | jq -r '.tokenId'
)
```

Ahora:

```bash
echo "${ADMIN_TOKEN}"
```

debería mostrar el token administrativo.

### 4. Utilizar el token

```bash
-H "iPlanetDirectoryPro: ${ADMIN_TOKEN}"
```

en las operaciones administrativas posteriores.

---

# 19. Regla mental para recordar todo el proceso

```text
PASSWORD
   │
   │ autentica
   ▼
TOKEN
   │
   │ autoriza llamadas administrativas
   ▼
AM REST API
   │
   ├── Users
   ├── Realm Config
   ├── OAuth Clients
   ├── Authentication Modules
   └── OAuth/OIDC
```

La idea fundamental es:

> **La contraseña permite obtener una sesión; el `tokenId` representa esa sesión y se utiliza para las llamadas posteriores.**

Y para OAuth:

```text
Client ID + Client Secret
          +
Usuario + Password
          +
grant_type=password
          │
          ▼
    OAuth Token Endpoint
          │
          ▼
     Access Token
```

---

## Seguridad

Los siguientes valores son secretos y no deberían quedar almacenados en Git:

```text
AMADMIN_PASSWORD
ADMIN_TOKEN
testuser password
OAuth Client Secret
OAuth Access Token
```

Para documentación, utilizar siempre:

```text
TU_PASSWORD
TU_ADMIN_TOKEN
TU_CLIENT_SECRET
TU_ACCESS_TOKEN
```

y, para pruebas locales, preferir variables de entorno:

```bash
export AMADMIN_PASSWORD='...'
export ADMIN_TOKEN='...'
export CLIENT_SECRET='...'
```

## Para obtener todos los usaurios registrados

```bash
curl -sS \
  "http://localhost:8080/am/json/realms/root/users?_queryId=*" \
  -H "Host: am" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}"
```

```bash
curl -sS \
  -H "Host: am" \
  -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
  -H "Accept-API-Version: resource=0.0" \
  "http://localhost:8080/am/json/realms/root/realm-config/authentication/modules/ldap/ldapService"
```
