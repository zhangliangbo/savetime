package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import io.github.zhangliangbo.savetime.ST;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zhangliangbo
 * @since 2022/8/21
 */
public class Jdbc extends AbstractConfigurable<QueryRunner> {

    @Override
    protected boolean isValid(QueryRunner queryRunner) {
        return true;
    }

    @Override
    protected QueryRunner create(String key) throws Exception {
        String[] split = key.split("-");
        JsonNode mysql = get(split[0], split[1]);
        String url = mysql.get("url").asText();
        URI uri = URI.create(url.replace("jdbc:", ""));
        String username = mysql.get("username").asText();
        String password = mysql.get("password").asText();
        JsonNode ssh = mysql.get("ssh");

        url = ST.ssh.forward(ssh, uri, url);

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return new QueryRunner(dataSource);
    }

    public JsonNode get(String env, String schema) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(getConfig()).get(env).get(schema);
    }

    public QueryRunner getOrCreateRunner(String env, String schema) throws Exception {
        String key = env + "-" + schema;
        return getOrCreate(key);
    }

    public Map<String, List<Object>> queryNoRetry(String key, String schema, String sql, Object... args) throws Exception {
        List<Map<String, Object>> result = queryList(key, schema, sql, args);
        return toMap(result);
    }

    public List<Map<String, Object>> queryList(String key, String schema, String sql, Object... args) throws Exception {
        return getOrCreateRunner(key, schema)
                .query(sql, new MapListHandler(), args);
    }

    interface RetryOperation<T> {
        T operate(String key, String schema, String sql, Object... args) throws Exception;
    }

    public <T> T retry(RetryOperation<T> retryOperation,
                       String key, String schema, String sql, Object... args) throws Exception {
        try {
            return retryOperation.operate(key, schema, sql, args);
        } catch (Exception e) {
            boolean retry = Objects.nonNull(e.getMessage()) && e.getMessage().startsWith("Communications link failure");
            if (retry) {
                System.out.println("连接报错，开始重试");

                clearAll();
                ST.ssh.clearAll();

                return retryOperation.operate(key, schema, sql, args);
            }
            throw e;
        }
    }

    public Map<String, List<Object>> query(String key, String schema, String sql, Object... args) throws Exception {
        return retry(this::queryNoRetry, key, schema, sql, args);
    }

    public int updateNoRetry(String key, String schema, String sql, Object... args) throws Exception {
        return getOrCreateRunner(key, schema).update(sql, args);
    }

    public int update(String key, String schema, String sql, Object... args) throws Exception {
        return retry(this::updateNoRetry, key, schema, sql, args);
    }

    public int[] batchNoRetry(String key, String schema, String sql, Object[][] args) throws Exception {
        return getOrCreateRunner(key, schema).batch(sql, args);
    }

    /**
     * 创建表格SQL
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表
     * @return 创建表格SQL
     * @throws Exception 异常
     */
    public String createTableSql(String key, String schema, String table) throws Exception {
        return query(key, schema, "show create table " + table)
                .get("Create Table").get(0).toString();
    }

    public List<String> getColumnNames(String key, String schema, String table) throws Exception {
        return query(key, schema,
                "show columns from " + table).get("Field")
                .stream().map(String::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * 备份表格
     *
     * @param key       环境
     * @param schema    数据库
     * @param table     表
     * @param batchSize 批量大小
     * @return 【记录条数，耗费时间，备份表名称】
     * @throws Exception 异常
     */
    public Triple<Long, Duration, String> backup(String key, String schema, String table, int batchSize) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();

        String newTable = createBackupTable(key, schema, table);
        String sql = insertTableSql(key, schema, newTable);

        Long total = 0L;
        long last = 0L;
        String primary = getPrimaryColumn(key, schema, table);
        String querySql = String.format("select * from %s where %s>? order by %s limit ?", table, primary, primary);
        while (true) {
            List<Map<String, Object>> page = queryList(key, schema, querySql, last, batchSize);
            if (page.isEmpty()) {
                break;
            }
            Object[][] args = page.stream().map(it -> it.values().toArray(new Object[0])).toArray(Object[][]::new);
            int[] r = batchNoRetry(key, schema, sql, args);
            total += r.length;
            System.out.println(total);
            last = Long.parseLong(String.valueOf(page.get(page.size() - 1).get(primary)));
        }

        return Triple.of(total, sw.stop().elapsed(), newTable);
    }

    public int rename(String key, String schema, String oldTableName, String newTableName) throws Exception {
        String sql = String.format("rename table %s to %s", oldTableName, newTableName);
        return update(key, schema, sql);
    }

    public Map<String, List<Object>> getQueryProcessMoreThan(String key, String schema, int spendSecond) throws Exception {
        String sql = "select * from information_schema.PROCESSLIST where command = 'Query' and time > ? order by time desc";
        return query(key, schema, sql, spendSecond);
    }

    /**
     * 创建备份表
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表
     * @return 备份表名称
     * @throws Exception 异常
     */
    public String createBackupTable(String key, String schema, String table) throws Exception {
        String newTable = table + "_" + DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");
        String createSql = createTableSql(key, schema, table).replace(table, newTable);
        int create = update(key, schema, createSql);
        System.out.println("创建表格" + newTable + "=" + create);
        return newTable;
    }

    /**
     * 插入记录SQL
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表
     * @return 插入记录SQL
     * @throws Exception 异常
     */
    public String insertTableSql(String key, String schema, String table) throws Exception {
        List<String> columns = getColumnNames(key, schema, table);
        return "insert into " + table + " " + columns.stream().collect(Collectors.joining(",", "(", ")")) +
                " values " + columns.stream().map(t -> "?").collect(Collectors.joining(",", "(", ")"));
    }

    /**
     * 并行备份表格
     *
     * @param key       环境
     * @param schema    数据库
     * @param table     表
     * @param parallel  并行度
     * @param batchSize 批量大小
     * @param executor  执行器
     * @return 【记录条数，耗费时间，备份表名称】
     * @throws Exception 异常
     */
    public Triple<Long, Duration, String> backupParallel(String key, String schema, String table, int parallel, int batchSize, Executor executor) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        String newTable = createBackupTable(key, schema, table);
        AtomicLong total = transfer(key, schema, table, key, schema, newTable, parallel, batchSize, executor);
        return Triple.of(total.get(), sw.stop().elapsed(), newTable);
    }

    /**
     * 移除表格
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表
     * @return 移除表格结果
     * @throws Exception 异常
     */
    public int dropTable(String key, String schema, String table) throws Exception {
        return update(key, schema, "drop table " + table);
    }

    /**
     * 获取主键字段名
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表
     * @return 主键字段名
     * @throws Exception 异常
     */
    public String getPrimaryColumn(String key, String schema, String table) throws Exception {
        Map<String, List<Object>> query = query(key, schema, "show columns from " + table);
        List<Object> keyColumn = query.get("Key");
        for (int i = 0; i < keyColumn.size(); i++) {
            if (String.valueOf(keyColumn.get(i)).toUpperCase().startsWith("PRI")) {
                return String.valueOf(query.get("Field").get(i));
            }
        }
        return "id";
    }

    /**
     * 查询全局变量
     *
     * @param key      环境
     * @param schema   数据库
     * @param variable 变量
     * @return 变量信息
     */
    public Map<String, List<Object>> showGlobalVariable(String key, String schema, String variable) throws Exception {
        return query(key, schema, String.format("show global variables like '%s'", "%" + variable + "%"));
    }

    /**
     * 查询表格
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表格
     * @return 变量信息
     */
    public List<Object> showTableLike(String key, String schema, String table) throws Exception {
        return query(key, schema, String.format("show tables like '%s'", "%" + table + "%")).values().stream().findFirst().orElse(new LinkedList<>());
    }

    /**
     * 查询记录总数
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表格
     * @return 变量信息
     */
    public Object count(String key, String schema, String table) throws Exception {
        return query(key, schema, String.format("select count(*) from %s", table)).values().stream().findFirst().orElse(new LinkedList<>())
                .stream().findFirst().orElse(0L);
    }

    /**
     * 并行查询记录总数，快
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表格
     * @return 变量信息
     */
    public Object countTableLikeParallel(String key, String schema, String table) throws Exception {
        return showTableLike(key, schema, table).stream().parallel()
                .map(t -> {
                            try {
                                return (Long) count(key, schema, (String) t);
                            } catch (Exception e) {
                                System.out.printf("查询数量报错 %s %s %s %s%n", key, schema, t, e);
                                return 0L;
                            }
                        }
                )
                .reduce(0L, Long::sum);
    }

    /**
     * 并行查询多个表记录总数，快
     *
     * @param key    环境
     * @param schema 数据库
     * @param tables 表格
     * @return 变量信息
     */
    public Map<String, List<Object>> countTableParallel(String key, String schema, String... tables) throws Exception {
        List<Map<String, Object>> list = Stream.of(tables).parallel()
                .map(t -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("table", t);
                    Long count;
                    try {
                        count = (Long) count(key, schema, t);
                    } catch (Exception e) {
                        System.out.printf("查询数量报错 %s %s %s %s%n", key, schema, t, e);
                        count = 0L;
                    }
                    map.put("count", count);
                    return map;
                })
                .collect(Collectors.toList());
        return toMap(list);
    }

    /**
     * 查询记录总数，有序
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表格
     * @return 变量信息
     */
    public Object countTableLike(String key, String schema, String table) throws Exception {
        return showTableLike(key, schema, table).stream()
                .map(t -> {
                            try {
                                Long res = (Long) count(key, schema, (String) t);
                                System.out.printf("查询数量 %s %s %s %s%n", key, schema, t, res);
                                return res;
                            } catch (Exception e) {
                                System.out.printf("查询数量报错 %s %s %s %s%n", key, schema, t, e);
                                return 0L;
                            }
                        }
                )
                .reduce(0L, Long::sum);
    }

    /**
     * 查询索引
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表格
     * @return 变量信息
     */
    public Map<String, List<Object>> showIndex(String key, String schema, String table) throws Exception {
        return query(key, schema, String.format("show index from %s", table));
    }

    /**
     * 清空表
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表
     * @return 移除表格结果
     * @throws Exception 异常
     */
    public int truncate(String key, String schema, String table) throws Exception {
        return update(key, schema, "truncate " + table);
    }

    /**
     * 并行查询记录总数，快
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表格
     * @return 变量信息
     */
    public Map<String, List<Object>> truncateTableLikeParallel(String key, String schema, String table) throws Exception {
        List<Map<String, Object>> result = showTableLike(key, schema, table).stream().parallel()
                .map(t -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("table", t);
                            try {
                                int res = truncate(key, schema, (String) t);
                                map.put("result", res);
                            } catch (Exception e) {
                                System.out.printf("清空报错 %s %s %s %s%n", key, schema, t, e);
                                map.put("result", -1);
                            }

                            return map;
                        }
                )
                .collect(Collectors.toList());
        return toMap(result);
    }

    /**
     * 查询记录总数，有序
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表格
     * @return 变量信息
     */
    public Map<String, List<Object>> truncateTableLike(String key, String schema, String table) throws Exception {
        List<Map<String, Object>> result = showTableLike(key, schema, table).stream()
                .map(t -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("table", t);
                            try {
                                int res = truncate(key, schema, (String) t);
                                System.out.printf("清空 %s %s %s %s%n", key, schema, t, res);
                                map.put("result", res);
                            } catch (Exception e) {
                                System.out.printf("清空报错 %s %s %s %s%n", key, schema, t, e);
                                map.put("result", -1);
                            }

                            return map;
                        }
                )
                .collect(Collectors.toList());
        return toMap(result);
    }

    private Map<String, List<Object>> toMap(List<Map<String, Object>> result) {
        if (CollectionUtils.isEmpty(result)) {
            return new LinkedHashMap<>();
        }
        Map<String, List<Object>> map = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : result.get(0).entrySet()) {
            List<Object> list = result.stream().map(m -> m.get(entry.getKey())).collect(Collectors.toCollection(LinkedList::new));
            map.put(entry.getKey(), list);
        }
        return map;
    }

    /**
     * 查询线程列表
     *
     * @param key    环境
     * @param schema 数据库
     * @return 线程列表
     */
    public Map<String, List<Object>> showProcessList(String key, String schema) throws Exception {
        return query(key, schema, "select * from information_schema.processlist where command='Query' order by time desc");
    }

    /**
     * 查询事务列表
     *
     * @param key    环境
     * @param schema 数据库
     * @return 事务列表
     */
    public Map<String, List<Object>> showTransactionList(String key, String schema) throws Exception {
        return query(key, schema, "select * from information_schema.innodb_trx order by trx_started");
    }

    /**
     * 根据线程id查询事务
     *
     * @param key    环境
     * @param schema 数据库
     * @return 事务列表
     */
    public Map<String, List<Object>> showTransactionByThreadId(String key, String schema, String threadId) throws Exception {
        return query(key, schema, "select * from information_schema.innodb_trx where trx_mysql_thread_id=? order by trx_started", threadId);
    }

    /**
     * 导出sql文件
     *
     * @param key       环境
     * @param schema    数据库
     * @param table     表格
     * @param consumer  批量消费者
     * @param batchSize 批量大小
     * @return 导出结果
     */
    public Triple<Long, Duration, Long> exportInsertSql(String key, String schema, String table, Consumer<List<String>> consumer, int batchSize) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();

        String primary = getPrimaryColumn(key, schema, table);
        String querySql = String.format("select * from %s where %s>? order by %s limit ?", table, primary, primary);
        long total = 0L;
        long batch = 0L;
        long last = 0L;
        while (true) {
            List<Map<String, Object>> page = queryList(key, schema, querySql, last, batchSize);
            if (page.isEmpty()) {
                break;
            }

            List<String> sqlList = new LinkedList<>();

            for (Map<String, Object> record : page) {
                String format = toInsertSql(table, record);
                sqlList.add(format);
            }

            if (Objects.nonNull(consumer)) {
                consumer.accept(sqlList);
            }

            total += page.size();
            batch++;
            System.out.printf("总数 %s 批次 %s%n", total, batch);
            last = Long.parseLong(String.valueOf(page.get(page.size() - 1).get(primary)));
        }

        return Triple.of(total, sw.stop().elapsed(), batch);
    }

    /**
     * 导出sql文件
     *
     * @param key      环境
     * @param schema   数据库
     * @param table    表格
     * @param consumer 单个消费者
     * @return 导出结果
     */
    public Triple<Long, Duration, Long> exportInsertSql(String key, String schema, String table, Consumer<String> consumer) throws Exception {
        return exportInsertSql(key, schema, table, batch -> {
            if (Objects.nonNull(consumer)) {
                for (String t : batch) {
                    consumer.accept(t);
                }
            }
        }, 1000);
    }

    /**
     * 转成insert语句
     *
     * @param table  表格
     * @param record 记录
     * @return sql
     * @throws Exception 异常
     */
    public String toInsertSql(String table, Map<String, Object> record) throws Exception {
        String insertSql = "insert into %s %s values %s;";
        StringJoiner keyJoiner = new StringJoiner(",", "(", ")");
        StringJoiner valueJoiner = new StringJoiner(",", "(", ")");
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            keyJoiner.add(entry.getKey());
            Object value = entry.getValue();
            String v;
            if (value instanceof String) {
                v = "'" + ((String) value).replace("'", "''") + "'";
            } else if (value instanceof java.sql.Timestamp) {
                v = "'" + DateFormatUtils.format((java.sql.Timestamp) value, "yyyy-MM-dd HH:mm:ss") + "'";
            } else if (value instanceof java.sql.Date) {
                v = "'" + DateFormatUtils.format((java.sql.Date) value, "yyyy-MM-dd") + "'";
            } else if (value instanceof java.sql.Time) {
                v = "'" + DateFormatUtils.format((java.sql.Time) value, "HH:mm:ss") + "'";
            } else {
                v = Objects.isNull(value) ? "null" : value.toString();
            }
            valueJoiner.add(v);
        }
        return String.format(insertSql, table, keyJoiner, valueJoiner);
    }

    /**
     * 并行转移表格
     *
     * @param key          环境
     * @param schema       数据库
     * @param table        表
     * @param keyTarget    目标环境
     * @param schemaTarget 目标数据库
     * @param tableTarget  目标表
     * @param parallel     并行度
     * @param batchSize    批量大小
     * @param executor     执行器
     * @return 【记录条数，耗费时间】
     * @throws Exception 异常
     */
    public Pair<Long, Duration> transferParallel(String key, String schema, String table, String keyTarget, String schemaTarget, String tableTarget, int parallel, int batchSize, Executor executor) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        AtomicLong total = transfer(key, schema, table, keyTarget, schemaTarget, tableTarget, parallel, batchSize, executor);
        return Pair.of(total.get(), sw.stop().elapsed());
    }

    private AtomicLong transfer(String key, String schema, String table, String keyTarget, String schemaTarget, String tableTarget, int parallel, int batchSize, Executor executor) throws Exception {
        String sql = insertTableSql(keyTarget, schemaTarget, tableTarget);
        long count = Long.parseLong(String.valueOf(query(key, schema, "select count(*) from " + table).get("count(*)").get(0)));
        System.out.println("总数" + count);
        int portion = count / parallel > 0 ? parallel : 1;
        System.out.println("份数" + portion);
        long part = count / parallel + (count % parallel == 0 ? 0 : 1);
        System.out.println("每份个数" + part);
        long[] ids = new long[portion + 1];
        ids[0] = 0;
        List<CompletableFuture<Void>> completableFutureList = new LinkedList<>();
        String primary = getPrimaryColumn(key, schema, table);
        for (int i = 1; i < ids.length; i++) {
            final int fi = i;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    System.out.printf("查询ID %s-%s %s%n", fi, ids.length, Thread.currentThread().getName());
                    try {
                        if (fi == ids.length - 1) {
                            String querySql = String.format("select %s from %s order by %s desc limit 1", primary, table, primary);
                            ids[fi] = Long.parseLong(String.valueOf(query(key, schema, querySql).get(primary).get(0)));
                        } else {
                            String querySql = String.format("select %s from %s order by %s limit ?,1", primary, table, primary);
                            ids[fi] = Long.parseLong(String.valueOf(query(key, schema, querySql, part * fi - 1).get(primary).get(0)));
                        }
                    } catch (Exception e) {
                        System.out.println("backupParallel报错" + e);
                    }
                }
            };
            CompletableFuture<Void> completableFuture = Objects.isNull(executor) ? CompletableFuture.runAsync(runnable) :
                    CompletableFuture.runAsync(runnable, executor);
            completableFutureList.add(completableFuture);
        }

        System.out.println("等待所有ID开始");
        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
        System.out.println("等待所有ID结束");

        AtomicLong total = new AtomicLong(0);
        completableFutureList.clear();
        for (int i = 0; i < ids.length - 1; i++) {
            long startId = ids[i];
            long endId = ids[i + 1];
            Runnable runnable = () -> {
                System.out.printf("开始任务 %s-%s %s%n", startId, endId, Thread.currentThread().getName());
                try {
                    long last = startId;
                    while (true) {
                        String querySql = String.format("select * from %s where %s>? and %s<=? order by %s limit ?", table, primary, primary, primary);
                        List<Map<String, Object>> page = queryList(key, schema, querySql, last, endId, batchSize);
                        if (page.isEmpty()) {
                            break;
                        }
                        Object[][] args = page.stream().map(it -> it.values().toArray(new Object[0])).toArray(Object[][]::new);
                        int[] r = batchNoRetry(keyTarget, schemaTarget, sql, args);
                        long l = total.addAndGet(r.length);
                        System.out.printf("当前进度 %s %s%n", l, Thread.currentThread().getName());
                        last = Long.parseLong(String.valueOf(page.get(page.size() - 1).get(primary)));
                    }
                } catch (Exception e) {
                    System.out.println("backupParallel报错" + e);
                }
            };
            CompletableFuture<Void> completableFuture = Objects.isNull(executor) ?
                    CompletableFuture.runAsync(runnable) : CompletableFuture.runAsync(runnable, executor);
            completableFutureList.add(completableFuture);
        }

        System.out.println("等待所有任务开始");
        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
        System.out.println("等待所有任务结束");

        return total;
    }

    /**
     * 更新记录SQL
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表
     * @return 插入记录SQL
     * @throws Exception 异常
     */
    public String updateTableSql(String key, String schema, String table) throws Exception {
        List<String> columns = getColumnNames(key, schema, table);
        String update = columns.stream().map(t -> String.join("=", t, "?")).collect(Collectors.joining(","));
        return "update " + table + " set " + update + " where id=?";
    }

    /**
     * 在多个表中查询sql
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表（用于like）
     * @param sql    sql语句，%s表示表名
     * @param args   参数
     * @return 数据集
     * @throws Exception 异常
     */
    public JsonNode queryLike(String key, String schema, String table, String sql, Object... args) throws Exception {
        List<Object> tables = showTableLike(key, schema, table);
        Map<String, List<Map<String, Object>>> res = new LinkedHashMap<>();

        for (Object o : tables) {
            String t = String.valueOf(o);
            String newSql = String.format(sql, t);
            List<Map<String, Object>> list = retry(this::queryList, key, schema, newSql, args);
            res.put(t, list);
        }

        return ST.io.toJsonNode(res);
    }

    /**
     * 在多个表中更新sql
     *
     * @param key    环境
     * @param schema 数据库
     * @param table  表（用于like）
     * @param sql    sql语句，%s表示表名
     * @param args   参数
     * @return 数据集
     * @throws Exception 异常
     */
    public Map<String, List<Object>> updateLike(String key, String schema, String table, String sql, Object... args) throws Exception {
        List<Object> tables = showTableLike(key, schema, table);

        Map<String, List<Object>> res = new LinkedHashMap<>();
        for (Object o : tables) {
            String t = String.valueOf(o);
            String newSql = String.format(sql, t);
            int update = update(key, schema, newSql, args);
            res.put(t, Lists.newArrayList(update));
        }

        return res;
    }

}
