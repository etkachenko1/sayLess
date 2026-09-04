FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ARG VITE_API_URL=http://localhost:8080
ARG VITE_WS_URL=http://localhost:8085
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_WS_URL=$VITE_WS_URL
RUN npm run build

FROM nginxinc/nginx-unprivileged:alpine
ARG VITE_API_URL=http://localhost:8080
ARG VITE_WS_URL=http://localhost:8085
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
USER root
RUN WS_ORIGIN=$(printf '%s' "$VITE_WS_URL" | sed -e 's#^http://#ws://#' -e 's#^https://#wss://#') && \
    sed -i "s#__API_ORIGIN__#${VITE_API_URL}#g; s#__WS_ORIGIN__#${WS_ORIGIN}#g" /etc/nginx/conf.d/default.conf
USER nginx
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]