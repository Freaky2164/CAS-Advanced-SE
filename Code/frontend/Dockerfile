# @author Nils
# Frontend-Image: Angular-Build + nginx als einziger nach außen offener Einstiegspunkt.
# nginx servt die SPA und reverse-proxyt /api intern auf backend:8080 –
# das Backend selbst veröffentlicht keinen Port mehr (siehe backend/docker-compose.yml).

# Build
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY angular.json tsconfig.json tsconfig.app.json ./
COPY public ./public
COPY src ./src
RUN npx ng build --configuration production

# Run
FROM nginx:1.29-alpine
# openssl für das selbstsignierte Fallback-Zertifikat (tls-selbstsigniert.sh)
RUN apk add --no-cache openssl
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --chmod=755 tls-selbstsigniert.sh /docker-entrypoint.d/10-tls-selbstsigniert.sh
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80 443
