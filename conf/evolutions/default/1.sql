# --- !Ups

CREATE TABLE users (
    username varchar(64),
    pw_hash char(60), -- bcrypt
    PRIMARY KEY(username)
);

CREATE TABLE tips (
    id SERIAL,
    message text,
    username varchar(64) REFERENCES users (username),
    created timestamp,
    modified timestamp,
    PRIMARY KEY(id)
);

CREATE TABLE tips_history (
    hid SERIAL,
    id integer REFERENCES tips (id),
    message text,
    username varchar(64) REFERENCES users (username),
    created timestamp,
    modified timestamp,
    PRIMARY KEY(hid)
);

CREATE OR REPLACE FUNCTION create_tips_history() RETURNS TRIGGER AS $tips_history$
    BEGIN
        NEW.modified = now();;
        IF (TG_OP = 'UPDATE') THEN
            INSERT INTO tips_history VALUES (DEFAULT, NEW.*);;
            RETURN NEW;;
        ELSIF (TG_OP = 'INSERT') THEN
            NEW.created = now();;
            INSERT INTO tips_history VALUES (DEFAULT, NEW.*);;
            RETURN NEW;;
        END IF;;
        RETURN NULL;; -- Should not happen
    END;;
$tips_history$ LANGUAGE plpgsql;

CREATE TRIGGER tips_history
BEFORE INSERT OR UPDATE OF message ON tips
    FOR EACH ROW EXECUTE PROCEDURE create_tips_history();

CREATE TABLE comments (
    id SERIAL,
    tip_id integer REFERENCES tips (id),
    comment text,
    username varchar(64) REFERENCES users (username),
    created timestamp,
    modified timestamp,
    PRIMARY KEY(id)
);

CREATE TABLE comments_history (
    hid SERIAL,
    id integer REFERENCES comments (id),
    tip_id integer REFERENCES tips (id),
    comment text,
    username varchar(64) REFERENCES users (username),
    created timestamp,
    modified timestamp,
    PRIMARY KEY(hid)
);

CREATE OR REPLACE FUNCTION create_comments_history() RETURNS TRIGGER AS $comments_history$
    BEGIN
        NEW.modified = now();;
        UPDATE tips SET modified = now() WHERE id = NEW.tip_id;;
        IF (TG_OP = 'UPDATE') THEN
            INSERT INTO comments_history VALUES (DEFAULT, NEW.*);;
            RETURN NEW;;
        ELSIF (TG_OP = 'INSERT') THEN
            NEW.created = now();;
            INSERT INTO comments_history VALUES (DEFAULT, NEW.*);;
            RETURN NEW;;
        END IF;;
        RETURN NULL;; -- Should not happen
    END;;
$comments_history$ LANGUAGE plpgsql;

CREATE TRIGGER comments_history
BEFORE INSERT OR UPDATE OF comment ON comments
    FOR EACH ROW EXECUTE PROCEDURE create_comments_history();

# --- !Downs
DROP TRIGGER tips_history on tips;
DROP TRIGGER comments_history on comments;
DROP TABLE comments_history;
DROP TABLE comments;
DROP TABLE tips_history;
DROP TABLE tips;

