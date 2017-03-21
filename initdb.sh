#!/usr/bin/env bash

echo "Setting up database $TIPS_DB for user $TIPS_DB_USER"
createdb -h "$TIPS_DB_HOSTNAME" -p "$TIPS_DB_PORT" "$TIPS_DB"
psql -h "$TIPS_DB_HOSTNAME" -p "$TIPS_DB_PORT" "$TIPS_DB" <<EOF
create user "$TIPS_DB_USER" password '$TIPS_DB_PASS';
create schema "$TIPS_DB_SCHEMA" authorization "$TIPS_DB_USER";
grant all privileges on schema "$TIPS_DB_SCHEMA" to "$TIPS_DB_USER";
alter user "$TIPS_DB_USER" set search_path to "$TIPS_DB_SCHEMA";
EOF
