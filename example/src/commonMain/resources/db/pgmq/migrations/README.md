# PGMQ Migrations

This directory contains the migration files for PGMQ (PostgreSQL Message Queue).

## Initial Setup

1. **Download the PGMQ initialization file**
   Download `pgmq.sql` from the
   [original PGMQ repository](https://github.com/pgmq/pgmq/blob/main/pgmq-extension/sql/pgmq.sql)
   and name it `1_pgmq-1.8.1.sql`. You can change the version number to match the version you want to install. Checkout
   to the correct tag for the version you want to install.

2. **Add the topics file (optional)**
   You can find `topics.sql` in the
   [sqlx4k repository](https://github.com/smyrgeorge/sqlx4k/blob/main/sqlx4k-postgres-pgmq/src/sql/topics.sql)
   and name it `2_topics.sql`

3. **Disable verification for first-time installation**
   If the PGMQ extension is not yet installed in your database, you need to disable the installation verification:
   ```kotlin
   val pgmq = Pgmq(db, options = PgmqClient.Options(verifyInstallation = false))
   ```
   After running the migrations and installing PGMQ, you can enable the verification for subsequent uses.

## Updating PGMQ

After the initial migration files, if you want to update PGMQ to a newer version, add the incremental migration files
provided by the PGMQ repository (not the entire `pgmq.sql` file).

These migration files should be numbered sequentially (e.g., `3_pgmq--1.8.1--1.9.0.sql`, etc.).
