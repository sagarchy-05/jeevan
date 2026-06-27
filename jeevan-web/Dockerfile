# ── build stage ──────────────────────────────────────────────────────────
FROM node:24-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# Vite bakes VITE_* at build time. The browser runs on the host, so the API base
# points at the published core port (defaults come from the root .env via compose).
ARG VITE_API_BASE_URL
ARG VITE_DEMO_PACING
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_DEMO_PACING=$VITE_DEMO_PACING
RUN npm run build

# ── serve stage ──────────────────────────────────────────────────────────
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
