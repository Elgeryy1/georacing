# syntax=docker/dockerfile:1
# ----------------------------------------------------------------------------
# GeoOps Discord bot — production image.
# Small (alpine), reproducible (npm ci against the committed lockfile), and
# run as a non-root user.
# ----------------------------------------------------------------------------
FROM node:20-alpine

# dumb-init reaps zombies and forwards SIGTERM so the bot shuts down cleanly
# (discord.js keeps a websocket open; without proper signal handling the
# container would be SIGKILLed on stop).
RUN apk add --no-cache dumb-init

ENV NODE_ENV=production
WORKDIR /app

# Install dependencies first so this layer is cached unless the lockfile changes.
COPY package.json package-lock.json* ./
RUN npm ci --omit=dev

# Copy the application source.
COPY . .

# Drop privileges: the base image ships an unprivileged `node` user.
USER node

CMD ["dumb-init", "node", "index.js"]
