# CDN to SCDN integration release

This integration is deployed once from a short-lived CDN branch. CDN and SCDN retain separate
repositories, databases, images, credentials, domains, releases, and rollback procedures.

## User experience

The CDN dashboard shows one SCDN menu item when `SCDN_INTEGRATION_ENABLED=true`. Users and CDN
administrators follow the same `/scdn` entry route. CDN creates a 60-second, single-use code and
the SCDN backend exchanges it over the internal mTLS endpoint. SCDN maps the signed `role` claim,
so neither users nor administrators enter credentials in a second console.

## Required CDN configuration

- `SCDN_INTEGRATION_ENABLED=true`
- `SCDN_CONSOLE_URL=https://<scdn-console-host>`
- `SCDN_INTERNAL_TOKEN=<at-least-32-random-characters>`
- `SCDN_SSO_CODE_TTL_SECONDS=60`
- `SCDN_ACCESS_TOKEN_TTL_SECONDS=900` or less
- `JWT_PUBLIC_KEY` and `JWT_PRIVATE_KEY` containing the RSA key pair used for `aud=kuocai-scdn`
- `SCDN_MTLS_REQUIRED=true`
- `SCDN_MTLS_VERIFIED_HEADER=X-Client-Cert-Verified`
- `SCDN_TRUSTED_PROXY_ADDRESSES=127.0.0.1,::1` or the exact private proxy addresses
- `RABBITMQ_PUBLISHER_CONFIRM_TYPE=correlated`
- `SCDN_PUBLISHER_CONFIRMS=true`

Use [scdn-internal-mtls-nginx.conf](runbooks/scdn-internal-mtls-nginx.conf) on a private internal
hostname. The proxy must remove any inbound client-verification header, set it only from
`$ssl_client_verify`, and prevent direct network access to the application internal routes.

## Deployment order

1. Back up the CDN database and deploy the CDN build with integration disabled.
2. Enable the private mTLS listener and verify a request without the SCDN client certificate fails.
3. Enable the integration. Startup validates HTTPS, token length, JWT keys, mTLS settings, and
   RabbitMQ publisher confirms before serving the internal API.
4. Verify SSO exchange, eligibility, order creation/query, debit replay, refund replay, and Outbox
   delivery with a non-production account.
5. Deploy SCDN with its own database and fixed `v1` client only after the CDN API is healthy.

CDN owns only the `scdn_*` integration tables in the CDN database. SCDN receives external user
IDs and non-sensitive status snapshots; passwords, identity document values, payment keys, and
balance records never cross the API.

## Rollback

Disable `SCDN_INTEGRATION_ENABLED`, remove the menu entry by restarting the CDN application, and
roll back only the CDN image if required. Do not roll back or modify the SCDN image or database.
The additive `scdn_*` tables may remain for a later compatible deployment. SCDN rollback follows
its own runbook and must never replace the CDN JAR, database, or configuration.
