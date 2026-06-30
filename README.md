# KuocaiCDN Open Source Edition

KuocaiCDN is a Spring Boot 2.7 based CDN management application.

This open source edition keeps all CDN vendor integrations and removes the commercial license check. It also removes online payment, proxy/agent, withdrawal, traffic package purchase, traffic gift/donation, and monthly gift features. Traffic billing is balance-based metered billing only, and the application enforces a single administrator account.


## Configuration

Runtime configuration is supplied through environment variables or JVM system properties. Required values include:

```properties
JWT_PUBLIC_KEY=
JWT_PRIVATE_KEY=
PASSWORD_LEGACY_AES_KEY=
CONFIG_RSA_PRIVATE_KEY=
CONFIG_RSA_PUBLIC_KEY=
BILLING_API_TOKEN=
ALIPAY_FACE_APP_ID=
ALIPAY_FACE_PRIVATE_KEY=
ALIPAY_FACE_PUBLIC_KEY=
ALIPAY_FACE_SERVER_URL=
ALIPAY_AUTH_APP_ID=
ALIPAY_AUTH_PRIVATE_KEY=
ALIPAY_AUTH_PUBLIC_KEY=
ALIPAY_AUTH_SERVER_URL=
ALIPAY_AUTH_REDIRECT_URI=
YIFAN_CDN_AK=
YIFAN_CDN_SK=
TENCENT_DNS_SECRET_ID=
TENCENT_DNS_SECRET_KEY=
TENCENT_DNS_LOCAL_DOMAIN=
QINIU_CDN_AK=
QINIU_CDN_SK=
VOLCENGINE_CDN_AK=
VOLCENGINE_CDN_SK=
VOLCENGINE_CDN_PROJECT=
WEIXIN_WEBHOOK_URL=
DB_URL=
DB_USERNAME=
DB_PASSWORD=
MONGO_HOST=
MONGO_PORT=
MONGO_USERNAME=
MONGO_PASSWORD=
MONGO_DATABASE=
RABBITMQ_HOST=
RABBITMQ_PORT=
RABBITMQ_USERNAME=
RABBITMQ_PASSWORD=
RABBITMQ_VHOST=
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=
MINIO_ENDPOINT=
MINIO_ACCESS_KEY=
MINIO_SECRET_KEY=
MINIO_BUCKET=
MINIO_PUBLIC_URL=
WX_APP_ID=
WX_APP_SECRET=
WX_TOKEN=
SMS_APP_ID=
SMS_APP_KEY=
SMS_SIGN=
```

Generate a new RSA key pair for JWT signing and set the Base64 DER public/private key values in `JWT_PUBLIC_KEY` and `JWT_PRIVATE_KEY`.

`PASSWORD_LEGACY_AES_KEY` is only needed while migrating old reversible AES password records. New and changed passwords are stored with BCrypt.

`CONFIG_RSA_PRIVATE_KEY` and `CONFIG_RSA_PUBLIC_KEY` protect encrypted system configuration records. Rotate them if the historical repository keys were ever deployed.

Online payment merchant private key files and Sentry runtime configuration are not used by this open source edition.
