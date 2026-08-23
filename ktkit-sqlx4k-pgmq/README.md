# ktkit-sqlx4k-pgmq

Message-queue support for ktkit services, built on [PGMQ](https://github.com/pgmq/pgmq) — a lightweight message queue on
Postgres, like AWS SQS and RSMQ — via [sqlx4k](https://github.com/smyrgeorge/sqlx4k)'s PGMQ client (PostgreSQL only).
This module glues PGMQ into the ktkit programming model: the `ExecContext` (with the authenticated principal)
and log4k tracing flow through message headers, so an event handler runs with the same context a REST handler does.

## How the integration works

### 1. Enable it via the Gradle plugin

The `Pgmq` extension of the `sqlx4k { }` block adds `ktkit-sqlx4k-pgmq` to the project (it requires the `PostgreSQL`
driver):

```kotlin
ktkit {
    sqlx4k {
        driver = PostgreSQL
        generatedCodePackage = "com.example.generated"
        extensions(Pgmq)
    }
}
```

### 2. Wire the `Pgmq` wrapper

`Pgmq` wraps a sqlx4k `IPostgresSQL` driver and exposes the `PgmqClient` — register it in the application's DI:

```kotlin
val pgmq = Pgmq(db) // db: IPostgresSQL

di {
    single { pgmq }.bind<Pgmq>()
}
```

### 3. Implement an event handler

Consumers extend `AbstractPgmqEventHandler` and implement a single method:

```kotlin
class TestEventHandler(pgmq: Pgmq) : AbstractPgmqEventHandler(
    pgmq = pgmq,
    queue = PgmqClient.Queue("test"),
) {
    context(_: ExecContext)
    override suspend fun EventContext.handler(message: Message) {
        // Process the message. A thrown error nacks the message (it is retried later).
    }
}
```

The base class manages the whole consumer lifecycle:

- `start()` creates the queue if it doesn't exist and starts the consumer (`stop()` is registered automatically as an
  application shutdown hook).
- Failed messages are retried with a backoff (the handler logs a warning when a message's read count exceeds 10), and
  the `onFailToRead`/`onFailToProcess`/`onFailToAck`/`onFailToNack` hooks can be overridden for custom handling.
- Consumer tuning (prefetch, visibility timeout, pull/retry delays) is configured via `PgmqConsumer.Options`.

### Trace and user propagation

The headline feature: the execution context crosses the queue in both directions through message headers.

- **Sending** — the `send()` helpers (available in an `ExecContext` + `QueryExecutor` scope, so a message can be sent
  inside a database transaction) automatically attach the current tracing span (`traceparent` header) and the
  authenticated principal (`x-real-name` header), and wrap the operation in a span.
- **Consuming** — for each message the handler rebuilds the tracing context from the `traceparent` header (so the
  processing span is linked to the producer's trace), extracts the principal from the `x-real-name` header (falling back
  to the constructor's `defaultUser`, otherwise the message is rejected as unauthorized), and runs your `handler`
  inside a fresh `ExecContext` — logging, tracing, and audit fields work exactly as they do in a REST handler.
- `archive()` helpers (also span-wrapped) move processed messages to the queue's archive table.

For the underlying client (queue management, batch operations, and the full PGMQ API) see sqlx4k's
[PGMQ documentation](https://github.com/smyrgeorge/sqlx4k).
