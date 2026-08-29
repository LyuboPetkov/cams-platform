# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html/*

COPY --from=build /app/dist /usr/share/nginx/html

ARG NGINX_CONF=nginx.conf
COPY ${NGINX_CONF} /etc/nginx/conf.d/default.conf

EXPOSE 80
EXPOSE 443