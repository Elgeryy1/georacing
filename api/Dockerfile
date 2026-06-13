# syntax=docker/dockerfile:1
# ----------------------------------------------------------------------------
# GeoRacing backend API — production image.
# Small (alpine), reproducible (npm ci against the committed lockfile) and run
# as the unprivileged `node` user.
# ----------------------------------------------------------------------------
FROM node:20-alpine

# dumb-init reaps zombies and forwards SIGTERM so the Express/HTTPS server and
# its mysql2 pool shut down cleanly instead of being SIGKILLed on `stop`.
# openssl is used by docker-entrypoint.sh to mint a dev self-signed cert when
# none is mounted, and wget (busybox) powers the container HEALTHCHECK.
RUN apk add --no-cache dumb-init openssl

ENV NODE_ENV=production
WORKDIR /app

# Install dependencies first so this layer is cached unless the lockfile changes.
COPY package.json package-lock.json* ./
RUN npm ci --omit=dev

# Copy the application source.
COPY . .

# Writable directory for the dev self-signed certificate the entrypoint mints
# when no real cert is mounted (the `node` user cannot write to /app).
RUN mkdir -p /app/certs && chown node:node /app/certs

# Drop privileges: the base image ships an unprivileged `node` user.
USER node

EXPOSE 4010

# The server speaks HTTPS only; the self-signed dev cert is not trusted, so the
# health probe disables certificate verification.
HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
    CMD wget --no-check-certificate -qO- https://127.0.0.1:${PORT:-4010}/health || exit 1

ENTRYPOINT ["dumb-init", "--", "./docker-entrypoint.sh"]
CMD ["node", "server.js"]
