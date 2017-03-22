#!/usr/bin/env bash
if [ -d "$TIPS_DB_DIR" ]; then
	if pg_ctl status  -D "$TIPS_DB_DIR"; then
		. stopdb.sh
	fi

	rm -rf "$TIPS_DB_DIR"
else
	echo "$TIPS_DB_DIR does not exist. Nothing to do"
fi
