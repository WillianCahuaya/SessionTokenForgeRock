(base) kathy@MacBook-Air-de-Katherine secrets % ls
jwt-private.base64 jwt-public.pem token-b-verifying-key.pem
jwt-private.pem privatekey.base64 tokenBSigningKey.pem
(base) kathy@MacBook-Air-de-Katherine secrets % base64 -D -i jwt-private.base64 -o /tmp/jwt-private-check.pem
(base) kathy@MacBook-Air-de-Katherine secrets % openssl pkey -in /tmp/jwt-private-check.pem -noout
(base) kathy@MacBook-Air-de-Katherine secrets % cmp jwt-private.pem /tmp/jwt-private-check.pem
(base) kathy@MacBook-Air-de-Katherine secrets %
