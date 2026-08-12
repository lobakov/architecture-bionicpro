DO $$
BEGIN
   IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'debezium') THEN
      ALTER USER debezium WITH PASSWORD 'dbz';
   ELSE
      CREATE USER debezium WITH PASSWORD 'dbz' REPLICATION LOGIN;
   END IF;
END
$$;

GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO debezium;

CREATE TABLE IF NOT EXISTS clients (
    user_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    prosthesis_model VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS telemetry (
    event_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES clients(user_id),
    event_time TIMESTAMP,
    signal_strength DOUBLE PRECISION,
    duration DOUBLE PRECISION
);

INSERT INTO clients (user_id, full_name, email, prosthesis_model) VALUES
    (1, 'John Doe', 'john.doe@example.com', 'BionicPro X1'),
    (2, 'Jane Smith', 'jane.smith@example.com', 'BionicPro X2'),
    (3, 'Alex Johnson', 'alex.johnson@example.com', 'BionicPro X3');

INSERT INTO telemetry (user_id, event_time, signal_strength, duration) VALUES
    (1, NOW() - INTERVAL '1 day', 0.75, 12.5),
    (1, NOW() - INTERVAL '1 day', 0.80, 15.0),
    (2, NOW() - INTERVAL '1 day', 0.65, 10.0),
    (3, NOW() - INTERVAL '1 day', 0.90, 20.0);

CREATE PUBLICATION crm_publication FOR TABLE clients, telemetry;
