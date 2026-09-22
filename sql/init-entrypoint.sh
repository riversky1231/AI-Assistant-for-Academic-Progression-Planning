#!/bin/bash
set -e

# Start MySQL
docker-entrypoint.sh mysqld &
MYSQL_PID=$!

# Wait for MySQL to be ready
echo "Waiting for MySQL to be ready..."
for i in {1..60}; do
  if MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqladmin ping -h 127.0.0.1 -u root --silent 2>/dev/null; then
    echo "MySQL is ready!"
    break
  fi
  echo "Attempt $i: MySQL not ready yet..."
  sleep 1
done

# Initialize database with UTF-8 encoding
echo "Initializing database with UTF-8 encoding..."
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

# Run schema
if [ -f /docker-entrypoint-initdb.d/01-schema.sql ]; then
  echo "Running schema initialization..."
  ( echo "SET NAMES utf8mb4; SET CHARACTER SET utf8mb4; SET COLLATION_CONNECTION = utf8mb4_0900_ai_ci;" && cat /docker-entrypoint-initdb.d/01-schema.sql ) | MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -h 127.0.0.1 -u root "$MYSQL_DATABASE" || true
fi

# Run data
if [ -f /docker-entrypoint-initdb.d/02-data.sql ]; then
  echo "Running data initialization..."
  ( echo "SET NAMES utf8mb4; SET CHARACTER SET utf8mb4; SET COLLATION_CONNECTION = utf8mb4_0900_ai_ci;" && cat /docker-entrypoint-initdb.d/02-data.sql ) | MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -h 127.0.0.1 -u root "$MYSQL_DATABASE" || true
fi

echo "Initialization complete!"

# Keep MySQL running
wait $MYSQL_PID
