#!/usr/bin/env bash

psql -h "$TIPS_DB_HOSTNAME" -p "$TIPS_DB_PORT" -U "$TIPS_DB_USER" "$TIPS_DB"

