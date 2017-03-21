#!/usr/bin/env bash

. dev_environment

new_db=false

if [ "$TIPS_DB_HOSTNAME" = "localhost" -a -n $TIPS_DB_DIR ]; then
	if [ -d "$TIPS_DB_DIR" ]; then
		echo "Database directory $TIPS_DB_DIR found."
	else
		mkdir -p "$TIPS_DB_DIR"
		pg_ctl init -D "$TIPS_DB_DIR"
		echo "Database directory $TIPS_DB_DIR created. Delete it to start fresh."
		new_db=true
	fi
	
	if pg_ctl status  -D "$TIPS_DB_DIR"; then
		echo "Postgres is running! Won't attempt to start it."
	else
		pg_ctl start -D "$TIPS_DB_DIR" -o "-p $TIPS_DB_PORT -k ''" -w # Note this only allows
		                                                              # connecting over TCP
		echo "Postgres started on port $TIPS_DB_PORT"
	fi
	
	if $new_db; then
	. initdb.sh
	fi
else
	echo "Assuming Postgres is setup correctly on Host: $TIPS_DB_HOSTNAME Port: $TIPS_DB_PORT"
fi
sbt run
