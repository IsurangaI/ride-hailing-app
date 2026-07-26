-- Creates the per-service databases on first postgres container start.
-- Mounted into /docker-entrypoint-initdb.d/ by docker-compose; runs only when
-- the postgres_data volume is empty.
CREATE DATABASE authdb;
CREATE DATABASE bookingdb;
CREATE DATABASE faredb;
