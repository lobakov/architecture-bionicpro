CREATE DATABASE IF NOT EXISTS reports;

CREATE TABLE reports.crm_telemetry_queue (
    event_id UInt64,
    user_id UInt64,
    event_time DateTime,
    signal_strength Float64,
    duration Float64
) ENGINE = Kafka
SETTINGS
    kafka_broker_list = 'kafka:9092',
    kafka_topic_list = 'crm.telemetry',
    kafka_group_name = 'clickhouse_telemetry_consumer',
    kafka_format = 'JSONEachRow';

CREATE TABLE reports.crm_clients_queue (
    user_id UInt64,
    full_name String,
    email String,
    prosthesis_model String
) ENGINE = Kafka
SETTINGS
    kafka_broker_list = 'kafka:9092',
    kafka_topic_list = 'crm.clients',
    kafka_group_name = 'clickhouse_clients_consumer',
    kafka_format = 'JSONEachRow';

CREATE TABLE reports.telemetry (
    event_id UInt64,
    user_id UInt64,
    event_time DateTime,
    signal_strength Float64,
    duration Float64
) ENGINE = MergeTree()
ORDER BY (user_id, event_time);

CREATE TABLE reports.clients (
    user_id UInt64,
    full_name String,
    email String,
    prosthesis_model String
) ENGINE = ReplacingMergeTree()
ORDER BY user_id;

CREATE MATERIALIZED VIEW reports.mv_telemetry TO reports.telemetry AS
SELECT
    event_id,
    user_id,
    event_time,
    signal_strength,
    duration
FROM reports.crm_telemetry_queue;

CREATE MATERIALIZED VIEW reports.mv_clients TO reports.clients AS
SELECT
    user_id,
    full_name,
    email,
    prosthesis_model
FROM reports.crm_clients_queue;

CREATE TABLE reports.user_report_mart_cdc (
    user_id UInt64,
    event_date Date,
    total_actions UInt64,
    sum_signal_strength Float64,
    count_signal_strength UInt64,
    max_signal_strength Float64,
    total_duration Float64,
    full_name String,
    email String,
    prosthesis_model String
) ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(event_date)
ORDER BY (user_id, event_date);

CREATE MATERIALIZED VIEW reports.mv_user_report
TO reports.user_report_mart_cdc
AS
SELECT
    t.user_id,
    toDate(t.event_time) AS event_date,
    count() AS total_actions,
    sum(t.signal_strength) AS sum_signal_strength,
    count(t.signal_strength) AS count_signal_strength,
    max(t.signal_strength) AS max_signal_strength,
    sum(t.duration) AS total_duration,
    c.full_name,
    c.email,
    c.prosthesis_model
FROM reports.telemetry AS t
LEFT JOIN reports.clients AS c ON t.user_id = c.user_id
GROUP BY
    t.user_id,
    event_date,
    c.full_name,
    c.email,
    c.prosthesis_model;
