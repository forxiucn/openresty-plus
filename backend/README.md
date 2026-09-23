# OpenResty Plus Control Plane

Spring Boot 4.1.1 / Java 21 control plane.

## Local run

Set `SPRING_PROFILES_ACTIVE=local` and the `OPENRESTY_DB_*` / `OPENRESTY_REDIS_*` environment variables, then run `mvn spring-boot:run` from this directory.

The local profile disables authentication only for development. Production requires OIDC and must not use this profile.
