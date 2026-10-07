# ============================================
# STAGE 1: BUILD
# ============================================
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
# Build com env var apontando pra API relativa (nginx vai fazer proxy)
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# ============================================
# STAGE 2: NGINX
# ============================================
FROM nginx:1.27-alpine AS runtime

# Remove config default
RUN rm /etc/nginx/conf.d/default.conf

# Copia nossa config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copia o build estático
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]