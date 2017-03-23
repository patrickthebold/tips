# --- !Ups

CREATE TABLE users (
    username varchar(64),
    pw_hash char(60) NOT NULL, -- bcrypt
    PRIMARY KEY(username)
);

CREATE TABLE tips (
    id SERIAL,
    message text NOT NULL,
    username varchar(64) NOT NULL REFERENCES users (username),
    created timestamp NOT NULL,
    modified timestamp NOT NULL,
    PRIMARY KEY(id)
);

CREATE TABLE tips_history (
    hid SERIAL,
    id integer NOT NULL REFERENCES tips (id),
    message text NOT NULL,
    username varchar(64) NOT NULL REFERENCES users (username),
    created timestamp NOT NULL,
    modified timestamp NOT NULL,
    PRIMARY KEY(hid)
);

CREATE TABLE comments (
    id SERIAL,
    tip_id integer NOT NULL REFERENCES tips (id),
    comment text NOT NULL,
    username varchar(64) NOT NULL REFERENCES users (username),
    created timestamp NOT NULL,
    modified timestamp NOT NULL ,
    PRIMARY KEY(id)
);

CREATE TABLE comments_history (
    hid SERIAL,
    id integer NOT NULL REFERENCES comments (id),
    tip_id integer NOT NULL REFERENCES tips (id),
    comment text NOT NULL,
    username varchar(64) NOT NULL REFERENCES users (username),
    created timestamp NOT NULL,
    modified timestamp NOT NULL,
    PRIMARY KEY(hid)
);

CREATE OR REPLACE FUNCTION create_tips_history() RETURNS TRIGGER AS $tips_history$
    BEGIN
        NEW.modified = now();;
        IF (TG_OP = 'UPDATE' AND OLD.username = NEW.username) THEN
            INSERT INTO tips_history VALUES (DEFAULT, NEW.*);;
            RETURN NEW;;
        ELSIF (TG_OP = 'INSERT') THEN
            NEW.created = now();;
            INSERT INTO tips_history VALUES (DEFAULT, NEW.*);;
            RETURN NEW;;
        END IF;;
        RETURN NULL;; -- different user tried an update
    END;;
$tips_history$ LANGUAGE plpgsql;

CREATE TRIGGER tips_history
BEFORE INSERT OR UPDATE OF message ON tips
    FOR EACH ROW EXECUTE PROCEDURE create_tips_history();

CREATE OR REPLACE FUNCTION create_comments_history() RETURNS TRIGGER AS $comments_history$
    BEGIN
        NEW.modified = now();;
        UPDATE tips SET modified = now() WHERE id = NEW.tip_id;;
        IF (TG_OP = 'UPDATE' AND OLD.username = NEW.username) THEN
            INSERT INTO comments_history VALUES (DEFAULT, NEW.*);;
            RETURN NEW;;
        ELSIF (TG_OP = 'INSERT') THEN
            NEW.created = now();;
            INSERT INTO comments_history VALUES (DEFAULT, NEW.*);;
            RETURN NEW;;
        END IF;;
        RETURN NULL;; -- different user tried an update
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

