package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import io.github.zhangliangbo.savetime.ST;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.time.DateFormatUtils;
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
                System.out.println("???????????????????????????");

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
     * ????????????SQL
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ???
     * @return ????????????SQL
     * @throws Exception ??????
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
     * ????????????
     *
     * @param key       ??????
     * @param schema    ?????????
     * @param table     ???
     * @param batchSize ????????????
     * @return ???????????????????????????????????????????????????
     * @throws Exception ??????
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
     * ???????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ???
     * @return ???????????????
     * @throws Exception ??????
     */
    public String createBackupTable(String key, String schema, String table) throws Exception {
        String newTable = table + "_" + DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");
        String createSql = createTableSql(key, schema, table).replace(table, newTable);
        int create = update(key, schema, createSql);
        System.out.println("????????????" + newTable + "=" + create);
        return newTable;
    }

    /**
     * ????????????SQL
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ???
     * @return ????????????SQL
     * @throws Exception ??????
     */
    public String insertTableSql(String key, String schema, String table) throws Exception {
        List<String> columns = getColumnNames(key, schema, table);
        return "insert into " + table + " " + columns.stream().collect(Collectors.joining(",", "(", ")")) +
                " values " + columns.stream().map(t -> "?").collect(Collectors.joining(",", "(", ")"));
    }

    /**
     * ??????????????????
     *
     * @param key       ??????
     * @param schema    ?????????
     * @param table     ???
     * @param parallel  ?????????
     * @param batchSize ????????????
     * @param executor  ?????????
     * @return ???????????????????????????????????????????????????
     * @throws Exception ??????
     */
    public Triple<Long, Duration, String> backupParallel(String key, String schema, String table, int parallel, int batchSize, Executor executor) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        String newTable = createBackupTable(key, schema, table);
        String sql = insertTableSql(key, schema, newTable);
        long count = Long.parseLong(String.valueOf(query(key, schema, "select count(*) from " + table).get("count(*)").get(0)));
        System.out.println("??????" + count);
        int portion = count / parallel > 0 ? parallel : 1;
        System.out.println("??????" + portion);
        long part = count / parallel + (count % parallel == 0 ? 0 : 1);
        System.out.println("????????????" + part);
        long[] ids = new long[portion + 1];
        ids[0] = 0;
        List<CompletableFuture<Void>> completableFutureList = new LinkedList<>();
        String primary = getPrimaryColumn(key, schema, table);
        for (int i = 1; i < ids.length; i++) {
            final int fi = i;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    System.out.printf("??????ID %s-%s %s%n", fi, ids.length, Thread.currentThread().getName());
                    try {
                        if (fi == ids.length - 1) {
                            String querySql = String.format("select %s from %s order by %s desc limit 1", primary, table, primary);
                            ids[fi] = Long.parseLong(String.valueOf(query(key, schema, querySql).get(primary).get(0)));
                        } else {
                            String querySql = String.format("select %s from %s order by %s limit ?,1", primary, table, primary);
                            ids[fi] = Long.parseLong(String.valueOf(query(key, schema, querySql, part * fi - 1).get(primary).get(0)));
                        }
                    } catch (Exception e) {
                        System.out.println("backupParallel??????" + e);
                    }
                }
            };
            CompletableFuture<Void> completableFuture = Objects.isNull(executor) ? CompletableFuture.runAsync(runnable) :
                    CompletableFuture.runAsync(runnable, executor);
            completableFutureList.add(completableFuture);
        }

        System.out.println("????????????ID??????");
        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
        System.out.println("????????????ID??????");

        AtomicLong total = new AtomicLong(0);
        completableFutureList.clear();
        for (int i = 0; i < ids.length - 1; i++) {
            long startId = ids[i];
            long endId = ids[i + 1];
            Runnable runnable = () -> {
                System.out.printf("???????????? %s-%s %s%n", startId, endId, Thread.currentThread().getName());
                try {
                    long last = startId;
                    while (true) {
                        String querySql = String.format("select * from %s where %s>? and %s<=? order by %s limit ?", table, primary, primary, primary);
                        List<Map<String, Object>> page = queryList(key, schema, querySql, last, endId, batchSize);
                        if (page.isEmpty()) {
                            break;
                        }
                        Object[][] args = page.stream().map(it -> it.values().toArray(new Object[0])).toArray(Object[][]::new);
                        int[] r = batchNoRetry(key, schema, sql, args);
                        long l = total.addAndGet(r.length);
                        System.out.printf("???????????? %s %s%n", l, Thread.currentThread().getName());
                        last = Long.parseLong(String.valueOf(page.get(page.size() - 1).get(primary)));
                    }
                } catch (Exception e) {
                    System.out.println("backupParallel??????" + e);
                }
            };
            CompletableFuture<Void> completableFuture = Objects.isNull(executor) ?
                    CompletableFuture.runAsync(runnable) : CompletableFuture.runAsync(runnable, executor);
            completableFutureList.add(completableFuture);
        }

        System.out.println("????????????????????????");
        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
        System.out.println("????????????????????????");

        return Triple.of(total.get(), sw.stop().elapsed(), newTable);
    }

    /**
     * ????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ???
     * @return ??????????????????
     * @throws Exception ??????
     */
    public int dropTable(String key, String schema, String table) throws Exception {
        return update(key, schema, "drop table " + table);
    }

    /**
     * ?????????????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ???
     * @return ???????????????
     * @throws Exception ??????
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
     * ??????????????????
     *
     * @param key      ??????
     * @param schema   ?????????
     * @param variable ??????
     * @return ????????????
     */
    public Map<String, List<Object>> showGlobalVariable(String key, String schema, String variable) throws Exception {
        return query(key, schema, String.format("show global variables like '%s'", "%" + variable + "%"));
    }

    /**
     * ????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ??????
     * @return ????????????
     */
    public List<Object> showTableLike(String key, String schema, String table) throws Exception {
        return query(key, schema, String.format("show tables like '%s'", "%" + table + "%")).values().stream().findFirst().orElse(new LinkedList<>());
    }

    /**
     * ??????????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ??????
     * @return ????????????
     */
    public Object count(String key, String schema, String table) throws Exception {
        return query(key, schema, String.format("select count(*) from %s", table)).values().stream().findFirst().orElse(new LinkedList<>())
                .stream().findFirst().orElse(0L);
    }

    /**
     * ??????????????????????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ??????
     * @return ????????????
     */
    public Object countTableLikeParallel(String key, String schema, String table) throws Exception {
        return showTableLike(key, schema, table).stream().parallel()
                .map(t -> {
                            try {
                                return (Long) count(key, schema, (String) t);
                            } catch (Exception e) {
                                System.out.printf("?????????????????? %s %s %s %s%n", key, schema, t, e);
                                return 0L;
                            }
                        }
                )
                .reduce(0L, Long::sum);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param tables ??????
     * @return ????????????
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
                        System.out.printf("?????????????????? %s %s %s %s%n", key, schema, t, e);
                        count = 0L;
                    }
                    map.put("count", count);
                    return map;
                })
                .collect(Collectors.toList());
        return toMap(list);
    }

    /**
     * ???????????????????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ??????
     * @return ????????????
     */
    public Object countTableLike(String key, String schema, String table) throws Exception {
        return showTableLike(key, schema, table).stream()
                .map(t -> {
                            try {
                                Long res = (Long) count(key, schema, (String) t);
                                System.out.printf("???????????? %s %s %s %s%n", key, schema, t, res);
                                return res;
                            } catch (Exception e) {
                                System.out.printf("?????????????????? %s %s %s %s%n", key, schema, t, e);
                                return 0L;
                            }
                        }
                )
                .reduce(0L, Long::sum);
    }

    /**
     * ????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ??????
     * @return ????????????
     */
    public Map<String, List<Object>> showIndex(String key, String schema, String table) throws Exception {
        return query(key, schema, String.format("show index from %s", table));
    }

    /**
     * ?????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ???
     * @return ??????????????????
     * @throws Exception ??????
     */
    public int truncate(String key, String schema, String table) throws Exception {
        return update(key, schema, "truncate " + table);
    }

    /**
     * ??????????????????????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ??????
     * @return ????????????
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
                                System.out.printf("???????????? %s %s %s %s%n", key, schema, t, e);
                                map.put("result", -1);
                            }

                            return map;
                        }
                )
                .collect(Collectors.toList());
        return toMap(result);
    }

    /**
     * ???????????????????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @param table  ??????
     * @return ????????????
     */
    public Map<String, List<Object>> truncateTableLike(String key, String schema, String table) throws Exception {
        List<Map<String, Object>> result = showTableLike(key, schema, table).stream()
                .map(t -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("table", t);
                            try {
                                int res = truncate(key, schema, (String) t);
                                System.out.printf("?????? %s %s %s %s%n", key, schema, t, res);
                                map.put("result", res);
                            } catch (Exception e) {
                                System.out.printf("???????????? %s %s %s %s%n", key, schema, t, e);
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
     * ??????????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @return ????????????
     */
    public Map<String, List<Object>> showProcessList(String key, String schema) throws Exception {
        return query(key, schema, "select * from information_schema.processlist order by time desc");
    }

    /**
     * ??????????????????
     *
     * @param key    ??????
     * @param schema ?????????
     * @return ????????????
     */
    public Map<String, List<Object>> showTransactionList(String key, String schema) throws Exception {
        return query(key, schema, "select * from information_schema.innodb_trx");
    }

    /**
     * ??????sql??????
     *
     * @param key       ??????
     * @param schema    ?????????
     * @param table     ??????
     * @param consumer  ???????????????
     * @param batchSize ????????????
     * @return ????????????
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
            String insertSql = "insert into %s %s values %s;";

            for (Map<String, Object> record : page) {
                StringJoiner keyJoiner = new StringJoiner(",", "(", ")");
                StringJoiner valueJoiner = new StringJoiner(",", "(", ")");
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    keyJoiner.add(entry.getKey());
                    Object value = entry.getValue();
                    String v;
                    if (value instanceof String) {
                        v = "'" + value + "'";
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
                String format = String.format(insertSql, table, keyJoiner, valueJoiner);
                sqlList.add(format);
            }

            if (Objects.nonNull(consumer)) {
                consumer.accept(sqlList);
            }

            total += page.size();
            batch++;
            System.out.printf("?????? %s ?????? %s%n", total, batch);
            last = Long.parseLong(String.valueOf(page.get(page.size() - 1).get(primary)));
        }

        return Triple.of(total, sw.stop().elapsed(), batch);
    }

    /**
     * ??????sql??????
     *
     * @param key      ??????
     * @param schema   ?????????
     * @param table    ??????
     * @param consumer ???????????????
     * @return ????????????
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

}
