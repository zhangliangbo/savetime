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
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

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

    //备份数据表
    public Pair<Long, Long> backup(String key, String schema, String table) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();

        List<String> columns = getColumnNames(key, schema, table);

        String newTable = table + "_" + DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");
        String createSql = createTableSql(key, schema, table).replace(table, newTable);
        int create = update(key, schema, createSql);
        System.out.println("创建表格" + newTable + "=" + create);
        String sql = "insert into " + newTable + " " + columns.stream().collect(Collectors.joining(",", "(", ")")) +
                " values " + columns.stream().map(t -> "?").collect(Collectors.joining(",", "(", ")"));
        Long total = 0L;
        long last = 0L;
        while (true) {
            List<Map<String, Object>> page = queryList(key, schema, "select * from " + table + " where id>? order by id limit 10000", last);
            if (page.isEmpty()) {
                break;
            }
            Object[][] args = page.stream().map(it -> it.values().toArray(new Object[0])).toArray(Object[][]::new);
            int[] r = batchNoRetry(key, schema, sql, args);
            total += r.length;
            System.out.println(total);
            last = Long.parseLong(String.valueOf(page.get(page.size() - 1).get("id")));
        }

        sw.stop();
        return Pair.of(total, sw.elapsed(java.util.concurrent.TimeUnit.MILLISECONDS));
    }

}
