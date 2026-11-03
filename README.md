# Movie Select backend

Java 21 / Spring Boot REST microservices for guest authentication, movie catalog
management, and movie orders. PostgreSQL stores service-owned tables in separate
schemas. Services validate the same signed JWTs; guests can only access their own
orders, while admin-only endpoints manage movies and set order prices.

## Services

| Service | Port | Main routes |
| --- | ---: | --- |
| Auth | 8081 | `POST /api/auth/guest`, `POST /api/auth/admin/login` |
| Movies | 8082 | `GET /api/movies`, `/api/admin/movies` (admin CRUD) |
| Orders | 8083 | `/api/orders`, `PATCH /api/admin/orders/{id}/price` |

All routes other than auth, `/api/webhooks/stripe`, and `/actuator/health` require
a bearer token. The webhook validates Stripe's signature. The order service
checks selected movie IDs against the movie service before saving an order.
Guest requests cannot provide or change a price; an admin sets the price and
transitions the order to `CHECKOUT_READY`. Guests can then create a Stripe
Checkout session; the webhook marks the order `PAID`.

Check service health in a browser or Postman (no bearer token required):

- Auth: `GET http://localhost:8081/actuator/health`
- Movies: `GET http://localhost:8082/actuator/health`
- Orders: `GET http://localhost:8083/actuator/health`

Each returns a JSON status such as `{ "status": "UP" }` when healthy.

## Environments and Supabase PostgreSQL

All services provide Spring profiles named `dev`, `test`, and `prod`. Select a
profile with `SPRING_PROFILES_ACTIVE` or
`-Dspring-boot.run.profiles=<profile>`. The checked-in IntelliJ run
configurations use `dev`. Use the Supabase **Session pooler** connection
details from each environment's **Connect** page.

The backend connects to Supabase PostgreSQL; it does not include an H2/local
database mode or automatically insert demo movies. Add movies through the
admin API, and the movie service stores them in Supabase. Set these environment
variables in the terminal before launching the services, or configure them in
each IntelliJ run configuration under **Environment variables**:

```sh
export SPRING_PROFILES_ACTIVE=dev
export SUPABASE_DB_HOST="aws-1-us-west-2.pooler.supabase.com"
export SUPABASE_DB_PORT=5432
export SUPABASE_DB_NAME=postgres
export SUPABASE_DB_USERNAME="postgres.fjbsuipvhmefaksahhdv"
export SUPABASE_DB_PASSWORD="your-database-password"
export JWT_SECRET_BASE64="$(openssl rand -base64 32)"
```

Set `SUPABASE_DB_PASSWORD` to the database password from Supabase. Generate the
JWT signing key once and use that exact same value for all three services. Do
not commit the database password or signing key.

Use the same database credentials and `JWT_SECRET_BASE64` for all three
services within one environment. Dev uses the existing schemas (`auth_service`,
`movie_service`, `order_service`) to preserve current data. Test uses separate
`*_test` schemas. Use separate Supabase projects for test and production, with
environment-specific credentials, to prevent data from overlapping.

The `test` profile requires `TEST_SUPABASE_DB_HOST`,
`TEST_SUPABASE_DB_USERNAME`, `TEST_SUPABASE_DB_PASSWORD`, and
`TEST_JWT_SECRET_BASE64`. It optionally accepts `TEST_SUPABASE_DB_PORT`,
`TEST_SUPABASE_DB_NAME`, `TEST_DB_POOL_MAX_SIZE`,
`TEST_CORS_ALLOWED_ORIGINS`, and test-specific admin, movie-service, and Stripe
settings. Point these only at a dedicated test Supabase project.

The `prod` profile requires `PROD_SUPABASE_DB_HOST`,
`PROD_SUPABASE_DB_USERNAME`, `PROD_SUPABASE_DB_PASSWORD`,
`PROD_JWT_SECRET_BASE64`, and `PROD_CORS_ALLOWED_ORIGINS`. It optionally accepts
`PROD_SUPABASE_DB_PORT`, `PROD_SUPABASE_DB_NAME`, `PROD_DB_POOL_MAX_SIZE`,
`PROD_ADMIN_EMAIL`, `PROD_ADMIN_PASSWORD`, `PROD_MOVIE_SERVICE_HOST`, and
production Stripe settings. Store production secrets in your deployment
platform's secret manager, never in source control.

Each service limits its JDBC pool to two connections by default so Flyway can
use migration and lock connections while keeping total usage low. Override the
pool per profile with `DB_POOL_MAX_SIZE`, `TEST_DB_POOL_MAX_SIZE`, or
`PROD_DB_POOL_MAX_SIZE`. Stop duplicate service runs and disconnect unused
database clients if Supabase reports that the pool is full.

Run services with the `dev` profile:

```sh
mvn -pl auth-service spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl movie-service spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl order-service spring-boot:run -Dspring-boot.run.profiles=dev
```

This connects the Spring services directly to Supabase PostgreSQL;
authentication remains the app's Spring JWT authentication and does not use
Supabase Auth.

For `test` and `prod`, set the environment-specific variables above and select
the corresponding profile. For example:

```sh
mvn -pl auth-service spring-boot:run -Dspring-boot.run.profiles=test
mvn -pl movie-service spring-boot:run -Dspring-boot.run.profiles=prod
```

Generate a separate private JWT signing key per environment with
`openssl rand -base64 32`; share it only among the three services in that
environment. Configure admin credentials separately per environment.

## Deploy to Render

The repository includes a Render Blueprint (`render.yaml`) and a root
multi-stage `Dockerfile`. In Render, choose **New → Blueprint**, connect this
GitHub repository, and deploy the blueprint. It creates the Auth, Movies, and
Orders web services, builds the Java 21 application, and uses
`/actuator/health` for health checks. Each service listens on Render's `PORT`.

During setup, Render asks for the shared production environment-group values:
`PROD_SUPABASE_DB_HOST`, `PROD_SUPABASE_DB_USERNAME`,
`PROD_SUPABASE_DB_PASSWORD`, `PROD_JWT_SECRET_BASE64`, and
`PROD_CORS_ALLOWED_ORIGINS`. Use your production Supabase session-pooler
details, a strong new JWT secret, and the public origin(s) of your frontend.
The same JWT secret is shared by the three services. The Orders service gets
the Movies service hostname from Render automatically. Auth also accepts
`PROD_ADMIN_EMAIL` and `PROD_ADMIN_PASSWORD`; Orders accepts
`PROD_STRIPE_SECRET_KEY` and `PROD_STRIPE_WEBHOOK_SECRET` if payments are
enabled. Enter secret values in Render's environment settings, not in this
file. The Blueprint defaults to Render's free plan; choose a paid plan in
Render if you need always-on services.

Before deploying, ensure the production Supabase project is separate from dev
and test. The first deployment applies Flyway migrations to the production
schemas, so verify the selected database and keep a backup. After deployment,
copy each service's Render URL and test its `/actuator/health` endpoint.

For Stripe payments, also set `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET`.
Set `STRIPE_SUCCESS_URL` and `STRIPE_CANCEL_URL` to your frontend routes as
needed; Stripe keys must remain server-side. In local development, forward
`checkout.session.completed` events to
`http://localhost:8083/api/webhooks/stripe` using the Stripe CLI webhook secret.

Set `CORS_ALLOWED_ORIGINS` to a comma-separated list of trusted frontend origins
when the app is not served from `http://localhost:5173`.

Start the services in separate terminals (the `dev` profile is selected by the
checked-in IntelliJ run configurations):

```sh
mvn -pl auth-service spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl movie-service spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl order-service spring-boot:run -Dspring-boot.run.profiles=dev
```

The admin account is created when both admin environment variables are
configured. On startup, the configured password is synchronized for that
admin email, so changing `ADMIN_PASSWORD` takes effect after restarting Auth.
Use a password of at least 12 characters. For production, use managed secrets,
TLS, a rate-limited ingress, and separate database credentials with
schema-scoped permissions.

## Example flow

Create a guest session:

```sh
curl -X POST http://localhost:8081/api/auth/guest \
  -H 'Content-Type: application/json' \
  -d '{"name":"Guest User","email":"guest@example.com"}'
```

Use the returned `accessToken` as `Authorization: Bearer <token>` to list
movies. Create catalog movies with the admin API first, and use a movie UUID
returned by the catalog in this order request:

```sh
curl -X POST http://localhost:8083/api/orders \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"movieIds":["<movie-id-from-the-movie-catalog>"]}'
```

Admin API:

- `POST /api/auth/admin/login` with `{ "email": "...", "password": "..." }`
- `GET /api/admin/movies`; `POST /api/admin/movies`
- `PATCH /api/admin/movies/{id}`; `DELETE /api/admin/movies/{id}` (soft delete)
- `GET /api/orders` to view all orders
- `PATCH /api/admin/orders/{id}/price` with `{ "price": 25.00 }`
- `POST /api/orders/{id}/checkout` to create a hosted Stripe Checkout session
- `POST /api/webhooks/stripe` is the signature-verified Stripe callback

The admin token is used for all admin routes. Movie endpoints accept a JSON
object with `title`, optional `year`, `genre`, `poster`, and `description`.
Create one movie with `POST /api/admin/movies` and a single JSON object, or
create up to 100 movies in one request with `POST /api/admin/movies/bulk` and a
JSON array of those objects.
