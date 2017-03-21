# --- !Ups

CREATE TABLE tips (
    id SERIAL,
    message text,
    username varchar(64),
    created timestamp,
    PRIMARY KEY(id)
);

# --- !Downs
DROP TABLE tips;