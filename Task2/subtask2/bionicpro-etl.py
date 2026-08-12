from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.providers.postgres.hooks.postgres import PostgresHook
from airflow.providers.clickhouse.hooks.clickhouse import ClickHouseHook
from airflow.utils.dates import days_ago
import requests
import logging
from datetime import datetime, timedelta

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'start_date': days_ago(1),
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

dag = DAG(
    dag_id='bionicpro_etl_reports',
    default_args=default_args,
    description='ETL процесс для подготовки витрины отчётов BionicPRO',
    schedule_interval='0 2 * * *',
    catchup=False,
    tags=['bionicpro', 'etl', 'reports'],
)

def extract_from_crm(**context):
    url = "http://crm-service/api/clients"
    try:
        response = requests.get(url, timeout=30)
        response.raise_for_status()
        data = response.json()
    except requests.RequestException as e:
        logging.error(f"Ошибка при обращении к CRM: {e}")
        raise

    context['ti'].xcom_push(key='crm_data', value=data)
    logging.info(f"Извлечено клиентов из CRM: {len(data)}")
    return len(data)

extract_crm_task = PythonOperator(
    task_id='extract_from_crm',
    python_callable=extract_from_crm,
    provide_context=True,
    dag=dag,
)

def extract_telemetry_aggregates(**context):
    pg_hook = PostgresHook(postgres_conn_id='postgres_default')
    sql = """
    SELECT
        user_id,
        DATE(event_time) AS event_date,
        COUNT(*) AS total_actions,
        AVG(signal_strength) AS avg_signal_strength,
        MAX(signal_strength) AS max_signal_strength,
        SUM(duration) AS total_duration
    FROM telemetry
    GROUP BY user_id, DATE(event_time)
    ORDER BY event_date, user_id
    """
    connection = pg_hook.get_conn()
    cursor = connection.cursor()
    cursor.execute(sql)
    rows = cursor.fetchall()
    columns = [desc[0] for desc in cursor.description]
    telemetry_agg = [dict(zip(columns, row)) for row in rows]
    cursor.close()
    connection.close()

    context['ti'].xcom_push(key='telemetry_agg', value=telemetry_agg)
    logging.info(f"Извлечено агрегированных записей телеметрии: {len(telemetry_agg)}")
    return len(telemetry_agg)

extract_telemetry_task = PythonOperator(
    task_id='extract_telemetry_aggregates',
    python_callable=extract_telemetry_aggregates,
    provide_context=True,
    dag=dag,
)

def transform_and_load_to_clickhouse(**context):
    ti = context['ti']
    crm_data = ti.xcom_pull(key='crm_data', task_ids='extract_from_crm')
    telemetry_agg = ti.xcom_pull(key='telemetry_agg', task_ids='extract_telemetry_aggregates')

    if not crm_data or not telemetry_agg:
        logging.warning("Нет данных для загрузки")
        return

    crm_by_user = {item['user_id']: item for item in crm_data}

    ch_hook = ClickHouseHook(clickhouse_conn_id='clickhouse_default')

    create_table_sql = """
    CREATE TABLE IF NOT EXISTS reports.user_report_mart
    (
        user_id UInt64,
        event_date Date,
        total_actions UInt64,
        avg_signal_strength Float64,
        max_signal_strength Float64,
        total_duration Float64,
        full_name String,
        email String,
        prosthesis_model String
    )
    ENGINE = MergeTree()
    PARTITION BY toYYYYMM(event_date)
    ORDER BY (user_id, event_date)
    """
    ch_hook.run(create_table_sql)

    rows = []
    for row in telemetry_agg:
        user_id = row['user_id']
        crm_info = crm_by_user.get(user_id, {})
        rows.append((
            user_id,
            row['event_date'].strftime('%Y-%m-%d'),
            row['total_actions'],
            row['avg_signal_strength'],
            row['max_signal_strength'],
            row['total_duration'],
            crm_info.get('full_name', ''),
            crm_info.get('email', ''),
            crm_info.get('prosthesis_model', '')
        ))

    if rows:
        ch_hook.run(
            "INSERT INTO reports.user_report_mart "
            "(user_id, event_date, total_actions, avg_signal_strength, max_signal_strength, total_duration, full_name, email, prosthesis_model) "
            "VALUES",
            rows
        )
        logging.info(f"Загружено строк в витрину: {len(rows)}")
    else:
        logging.info("Нет строк для загрузки")

transform_load_task = PythonOperator(
    task_id='transform_and_load_to_clickhouse',
    python_callable=transform_and_load_to_clickhouse,
    provide_context=True,
    dag=dag,
)

[extract_crm_task, extract_telemetry_task] >> transform_load_task
