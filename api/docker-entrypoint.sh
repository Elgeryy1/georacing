#!/bin/sh
# ----------------------------------------------------------------------------
# Container entrypoint.
#
# The GeoRacing API serves HTTPS only and reads the TLS material from the paths
# in SSL_KEY_PATH / SSL_CERT_PATH / SSL_CA_PATH. For a frictionless
# `docker compose up`, if no certificate is mounted we mint a throwaway
# self-signed one so the server can boot. In production, mount real certificates
# over these paths (see docker-compose.yml) and this step is skipped.
# ----------------------------------------------------------------------------
set -e

KEY_PATH="${SSL_KEY_PATH:-SSLprivatekey.key}"
CERT_PATH="${SSL_CERT_PATH:-SSLcertificate.crt}"
CA_PATH="${SSL_CA_PATH:-SSLIntermediateCertificate.crt}"

if [ ! -f "$KEY_PATH" ] || [ ! -f "$CERT_PATH" ]; then
    echo "TLS certificate not found; generating a self-signed development certificate."
    openssl req -x509 -newkey rsa:2048 -nodes \
        -keyout "$KEY_PATH" \
        -out "$CERT_PATH" \
        -days 365 \
        -subj "/CN=georacing.example.com" >/dev/null 2>&1
    # The server also loads an intermediate CA file; reuse the self-signed cert.
    if [ ! -f "$CA_PATH" ]; then
        cp "$CERT_PATH" "$CA_PATH"
    fi
fi

exec "$@"
