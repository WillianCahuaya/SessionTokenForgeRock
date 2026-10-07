Deajr temrinal abiert para conectarse de mac adocker al AM de forgeRock

kubectl port-forward svc/am 8080:80 -n forgerock

Recueprar contraseña admin de AM

./bin/forgeops info | grep amadmin

> > 9HKHQeCZNBL8GEx7qwkmrCpq

Lamar con el password:

curl -sS \
 -X POST \
 http://localhost:8080/am/json/realms/root/authenticate \
 -H "Host: am" \
 -H "Content-Type: application/json" \
 -H "X-OpenAM-Username: amadmin" \
 -H "X-OpenAM-Password: TU_PASSWORD" \
 -H "Accept-API-Version: resource=2.0" \
 -d '{}'

USAR ENTONCES:

curl -sS \
 -X POST \
 http://localhost:8080/am/json/realms/root/authenticate \
 -H "Host: am" \
 -H "Content-Type: application/json" \
 -H "X-OpenAM-Username: amadmin" \
 -H "X-OpenAM-Password: 9HKHQeCZNBL8GEx7qwkmrCpq" \
 -H "Accept-API-Version: resource=2.0" \
 -d '{}'

RESPONSE APRA USAR COMO PASSWORD DE ADMIN

{"tokenId":"v_TyXogjm9PzpphoVG3_dnfPUmY._AAJTSQACMDIAAlNLABxaa3JMOTR5bjV0KzB2aTc4UVcwNy9VRlZXM2c9AAR0eXBlAANDVFMAAlMxAAIwMQ.._","successUrl":"/am/console","realm":"/"}%

QUERY PARA CONSULTAR

curl -sS \
 -X POST \
 'http://localhost:8080/am/json/realms/root/users/?\_action=create' \
 -H "Host: am" \
 -H "Content-Type: application/json" \
 -H "Accept-API-Version: protocol=2.1,resource=3.0" \
 -H "iPlanetDirectoryPro: TU_TOKEN_ID" \
 -d '{
"username": "testuser",
"userpassword": "Password123!",
"mail": "testuser@example.com",
"givenName": "Test",
"sn": "User"
}'

curl -sS \
 -X POST \
 'http://localhost:8080/am/json/realms/root/users/?\_action=create' \
 -H "Host: am" \
 -H "Content-Type: application/json" \
 -H "Accept-API-Version: protocol=2.1,resource=3.0" \
 -H "iPlanetDirectoryPro: v_TyXogjm9PzpphoVG3_dnfPUmY._AAJTSQACMDIAAlNLABxaa3JMOTR5bjV0KzB2aTc4UVcwNy9VRlZXM2c9AAR0eXBlAANDVFMAAlMxAAIwMQ.._" \
 -d '{
"username": "testuser",
"userpassword": "Password123!",
"mail": "testuser@example.com",
"givenName": "Test",
"sn": "User"
}'

UPDATE PASSWORD:

curl -sS \
 -X PUT \
 "http://localhost:8080/am/json/realms/root/users/testuser" \
 -H "Host: am" \
 -H "iPlanetDirectoryPro: EisIYQQW6Po-CXSApkDZk1KliUI._AAJTSQACMDIAAlNLABxVeEp2bldKSFZpeEFQaXJSM21VVXVlTGZzYWs9AAR0eXBlAANDVFMAAlMxAAIwMQ.._" \
 -H "Content-Type: application/json" \
 -H "Accept-API-Version: protocol=2.1,resource=4.0" \
 -d '{
"userpassword": "Password123!"
}'

COSNULTA ADMINISTRATIVA

curl -sS \
 -X POST \
 http://localhost:8080/am/json/realms/root/authenticate \
 -H "Host: am" \
 -H "Content-Type: application/json" \
 -H "X-OpenAM-Username: amadmin" \
 -H "X-OpenAM-Password: 9HKHQeCZNBL8GEx7qwkmrCpq" \
 -H "Accept-API-Version: resource=2.0" \
 -d '{}'

RESPONSE
{"tokenId":"EisIYQQW6Po-CXSApkDZk1KliUI._AAJTSQACMDIAAlNLABxVeEp2bldKSFZpeEFQaXJSM21VVXVlTGZzYWs9AAR0eXBlAANDVFMAAlMxAAIwMQ.._","successUrl":"/am/console","realm":"/"}%

curl -sS \
 -H "Host: am" \
 -H "iPlanetDirectoryPro: EisIYQQW6Po-CXSApkDZk1KliUI._AAJTSQACMDIAAlNLABxVeEp2bldKSFZpeEFQaXJSM21VVXVlTGZzYWs9AAR0eXBlAANDVFMAAlMxAAIwMQ.._" \
 -H "Accept-API-Version: resource=1.0" \
 "http://localhost:8080/am/json/realms/root/realm-config/agents/OAuth2Client/mobile-simulator"

curl -sS \
 -H "Host: am" \
 -H "iPlanetDirectoryPro: EisIYQQW6Po-CXSApkDZk1KliUI._AAJTSQACMDIAAlNLABxVeEp2bldKSFZpeEFQaXJSM21VVXVlTGZzYWs9AAR0eXBlAANDVFMAAlMxAAIwMQ.._" \
 -H "Accept-API-Version: resource=1.0" \
 "http://localhost:8080/am/json/realms/root/realm-config/agents/OAuth2Client?\_queryFilter=true"

curl -sS \
 -H "Host: am" \
 -H "iPlanetDirectoryPro: EisIYQQW6Po-CXSApkDZk1KliUI._AAJTSQACMDIAAlNLABxVeEp2bldKSFZpeEFQaXJSM21VVXVlTGZzYWs9AAR0eXBlAANDVFMAAlMxAAIwMQ.._" \
 "http://localhost:8080/am/json/realms/root/users/testuser?\_fields=username,uid,inetUserStatus,givenName,sn"

curl -sS \
 -H "Host: am" \
 -H "iPlanetDirectoryPro: EisIYQQW6Po-CXSApkDZk1KliUI._AAJTSQACMDIAAlNLABxVeEp2bldKSFZpeEFQaXJSM21VVXVlTGZzYWs9AAR0eXBlAANDVFMAAlMxAAIwMQ.._" \
 -H "Accept-API-Version: resource=2.0" \
 "http://localhost:8080/am/json/realms/root/realm-config/services/oauth-oidc"

curl -sS \
 -X PUT \
 "http://localhost:8080/am/json/realms/root/realm-config/services/oauth-oidc" \
 -H "Host: am" \
 -H "iPlanetDirectoryPro: EisIYQQW6Po-CXSApkDZk1KliUI._AAJTSQACMDIAAlNLABxVeEp2bldKSFZpeEFQaXJSM21VVXVlTGZzYWs9AAR0eXBlAANDVFMAAlMxAAIwMQ.._" \
 -H "Content-Type: application/json" \
 -H "Accept-API-Version: resource=2.0" \
 -d '{
"passwordGrantAuthService": "DataStore"
}'

curl -sS \
 -X PUT \
 "http://localhost:8080/am/json/realms/root/realm-config/services/oauth-oidc" \
 -H "Host: am" \
 -H "iPlanetDirectoryPro: EisIYQQW6Po-CXSApkDZk1KliUI._AAJTSQACMDIAAlNLABxVeEp2bldKSFZpeEFQaXJSM21VVXVlTGZzYWs9AAR0eXBlAANDVFMAAlMxAAIwMQ.._" \
 -H "Content-Type: application/json" \
 -H "Accept-API-Version: resource=2.0" \
 -d '{
"passwordGrantAuthService": "ldapService"
}'

{"tokenId":"\_lcFWChyBmvBVJ3j4zUutyl7bXw._AAJTSQACMDIAAlNLABw2a21ySTNyZHdCcXhzSEdUNTNFSGRKaENRK2M9AAR0eXBlAANDVFMAAlMxAAIwMQ.._","successUrl":"/am/console","realm":"/"}%

curl -sS \
 -H "Host: am" \
 -H "iPlanetDirectoryPro: EisIYQQW6Po-CXSApkDZk1KliUI._AAJTSQACMDIAAlNLABxVeEp2bldKSFZpeEFQaXJSM21VVXVlTGZzYWs9AAR0eXBlAANDVFMAAlMxAAIwMQ.._" \
 -H "Accept-API-Version: resource=0.0" \
 "http://localhost:8080/am/json/realms/root/realm-config/authentication/modules/ldap/ldapService"

curl -sS \
 -H "Host: am" \
 -H "iPlanetDirectoryPro: \_lcFWChyBmvBVJ3j4zUutyl7bXw._AAJTSQACMDIAAlNLABw2a21ySTNyZHdCcXhzSEdUNTNFSGRKaENRK2M9AAR0eXBlAANDVFMAAlMxAAIwMQ.._" \
 -H "Accept-API-Version: resource=0.0" \
 "http://localhost:8080/am/json/realms/root/realm-config/authentication/modules/ldap?\_queryFilter=true"

curl -sS -X POST 'http://localhost:8080/am/json/realms/root/authenticate?authIndexType=service&authIndexValue=ldapService' -H 'Host: am' -H 'Content-Type: application/json' -H 'X-OpenAM-Username: testuser' -H 'X-OpenAM-Password: Password123!' -H 'Accept-API-Version: resource=2.0, protocol=1.0' -d '{}'

curl -k --resolve am:8080:127.0.0.1 -X POST http://am:8080/am/oauth2/access_token -u 'mobile-simulator:mobile-secret' -H 'Content-Type: application/x-www-form-urlencoded' --data-urlencode 'grant_type=password' --data-urlencode 'username=testuser' --data-urlencode 'password=Password123!' --data-urlencode 'scope=openid profile' --data-urlencode 'auth_chain=ldapService'
