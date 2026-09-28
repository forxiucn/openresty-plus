FROM node:22.18.0-bookworm-slim AS build
WORKDIR /workspace

# Vben's prepare hook skips local Git hook installation in CI builds.
ENV CI=true
# The production container must use the same-origin proxy in nginx.conf.
ENV VITE_GLOB_API_URL=/api

RUN corepack enable && corepack prepare pnpm@10.33.4 --activate
COPY . .
RUN --mount=type=cache,id=openresty-plus-pnpm-store,target=/pnpm/store \
    pnpm install --frozen-lockfile --store-dir=/pnpm/store
RUN pnpm run build --filter=@vben/web-antd

FROM nginx:1.29-alpine
COPY --from=build /workspace/apps/web-antd/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
