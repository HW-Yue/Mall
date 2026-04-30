-- 新库首次初始化时由 docker-entrypoint-initdb.d 执行；已有 data 卷的实例请用手工执行：
-- mysql -h127.0.0.1 -P13306 -uroot -p < 本文件
-- 慢日志表初始化统一维护在根目录 dev-ops/mysql/sql 下。

SET @OLD_SLOW := @@GLOBAL.slow_query_log;
SET GLOBAL slow_query_log = OFF;

CREATE TABLE IF NOT EXISTS mysql.slow_log (
    start_time     TIMESTAMP(6) NOT NULL,
    user_host      MEDIUMTEXT NOT NULL,
    query_time     TIME(6) NOT NULL,
    lock_time      TIME(6) NOT NULL,
    rows_sent      INT NOT NULL,
    rows_examined  INT NOT NULL,
    db             VARCHAR(512) NOT NULL,
    last_insert_id INT NOT NULL,
    insert_id      INT NOT NULL,
    server_id      INT UNSIGNED NOT NULL,
    sql_text       MEDIUMBLOB NOT NULL,
    thread_id      BIGINT UNSIGNED NOT NULL
) ENGINE=CSV DEFAULT CHARSET=utf8mb3 COMMENT='Slow log';

SET GLOBAL log_output = 'FILE,TABLE';
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 2;
