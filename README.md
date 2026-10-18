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

## Use Supabase PostgreSQL

Use the Supabase **Session pooler** connection details from **Connect**. The
IntelliJ run configurations contain the host `aws-1-us-west-2.pooler.supabase.com`,
port `5432`, database `postgres`, and username
`postgres.fjbsuipvhmefaksahhdv`.

The backend connects to Supabase PostgreSQL; it does not include an H2/local
database mode or automatically insert demo movies. Add movies through the
admin API, and the movie service stores them in Supabase. Set these environment
variables in the terminal before launching the services, or configure them in
each IntelliJ run configuration under **Environment variables**:

```sh
export SPRING_PROFILES_ACTIVE=supabase
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

Use the same database credentials and `JWT_SECRET_BASE64` for all services.
Each service applies its Flyway migrations to its own schema (`auth_service`,
`movie_service`, or `order_service`) in the shared Supabase PostgreSQL database.
Each service limits its JDBC pool to two connections by default so Flyway can
use its migration and lock connections while the three services stay within
Supabase's session pooler limit. Stop
duplicate service runs and disconnect unused database clients if Supabase still
reports that the pool is full. You can override the per-service pool limit with
`DB_POOL_MAX_SIZE` if your Supabase plan and other clients leave sufficient
connections available.
Start each service with the Supabase profile:

```sh
mvn -pl auth-service spring-boot:run -Dspring-boot.run.profiles=supabase
mvn -pl movie-service spring-boot:run -Dspring-boot.run.profiles=supabase
mvn -pl order-service spring-boot:run -Dspring-boot.run.profiles=supabase
```

This connects the Spring services directly to Supabase PostgreSQL;
authentication remains the app's Spring JWT authentication and does not use
Supabase Auth.

Set a private signing key and, optionally, the first admin credentials:

```sh
export JWT_SECRET_BASE64="$(openssl rand -base64 32)"
export ADMIN_EMAIL="admin@example.com"
export ADMIN_PASSWORD="use-a-unique-password-of-at-least-12-chars"
```

For Stripe payments, also set `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET`.
Set `STRIPE_SUCCESS_URL` and `STRIPE_CANCEL_URL` to your frontend routes as
needed; Stripe keys must remain server-side. In local development, forward
`checkout.session.completed` events to
`http://localhost:8083/api/webhooks/stripe` using the Stripe CLI webhook secret.

Set `CORS_ALLOWED_ORIGINS` to a comma-separated list of trusted frontend origins
when the app is not served from `http://localhost:5173`.

Start the services in separate terminals (the `supabase` profile is also
selected by the checked-in IntelliJ run configurations):

```sh
mvn -pl auth-service spring-boot:run -Dspring-boot.run.profiles=supabase
mvn -pl movie-service spring-boot:run -Dspring-boot.run.profiles=supabase
mvn -pl order-service spring-boot:run -Dspring-boot.run.profiles=supabase
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
