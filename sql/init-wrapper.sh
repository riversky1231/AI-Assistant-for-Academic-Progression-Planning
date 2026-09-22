#!/bin/bash
set -e

# Wait for MySQL to be ready
echo "Waiting for MySQL to be ready..."
until mysqladmin ping -h 127.0.0.1 -u root -p"$MYSQL_ROOT_PASSWORD" --silent; do
  echo "MySQL is unavailable - sleeping"
  sleep 1
done

echo "MySQL is ready!"

# Set character set for the connection before running init scripts
export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"

# Run init scripts with proper character set
for f in /docker-entrypoint-initdb.d/*; do
  case "$f" in
    *.sql)
      echo "Initializing database with $f"
      mysql -h 127.0.0.1 -u root -e "SET NAMES utf8mb4; SET CHARACTER SET utf8mb4; SET COLLATION_CONNECTION = utf8mb4_0900_ai_ci;" "$MYSQL_DATABASE" < "$f"
      ;;
  esac
done

echo "Database initialization complete!"
