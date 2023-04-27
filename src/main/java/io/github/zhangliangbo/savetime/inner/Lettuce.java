package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhangliangbo.savetime.ST;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.collections.CollectionUtils;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author zhangliangbo
 * @since 2022/8/21
 */
public class Lettuce extends AbstractConfigurable<StatefulRedisConnection<String, String>> {

    @Override
    protected boolean isValid(StatefulRedisConnection<String, String> connection) {
        return connection.isOpen();
    }

    @Override
    protected StatefulRedisConnection<String, String> create(String key) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode treeNode = objectMapper.readTree(getConfig());
        JsonNode redis = treeNode.get(key);
        String url = redis.get("uri").asText();
        String password = redis.get("password").asText();
        URI uri = URI.create(url);
        JsonNode ssh = redis.get("ssh");

        url = ST.ssh.forward(ssh, uri, url);

        RedisURI redisUri = RedisURI.create(url);
        redisUri.setPassword(password.toCharArray());
        RedisClient redisClient = RedisClient.create(redisUri);
        return redisClient.connect();
    }

    /**
     * 扫描键
     *
     * @param key      环境
     * @param pattern  正则表达式
     * @param limit    每批次数量
     * @param consumer 消费者
     * @throws Exception 异常
     */
    public boolean scan(String key, String pattern, int limit, Consumer<List<String>> consumer) throws Exception {
        RedisCommands<String, String> sync = getOrCreate(key).sync();

        ScanCursor scanCursor = ScanCursor.INITIAL;
        boolean finished;
        do {
            KeyScanCursor<String> keyScanCursor = sync.scan(ScanArgs.Builder.matches(pattern).limit(limit));
            List<String> keys = keyScanCursor.getKeys();

            if (CollectionUtils.isEmpty(keys)) {
                break;
            }
            consumer.accept(keys);

            scanCursor = ScanCursor.of(keyScanCursor.getCursor());
            finished = keyScanCursor.isFinished();
        } while (!finished);

        return true;
    }

    /**
     * 获取键
     *
     * @param env 环境
     * @param key 键
     * @throws Exception 异常
     */
    public String get(String env, String key) throws Exception {
        return getOrCreate(env).sync().get(key);
    }

    /**
     * 删除键
     *
     * @param env 环境
     * @param key 键
     * @throws Exception 异常
     */
    public long del(String env, String... key) throws Exception {
        return getOrCreate(env).sync().del(key);
    }

    /**
     * 扫描键
     *
     * @param key      环境
     * @param pattern  正则表达式
     * @param consumer 消费者
     * @throws Exception 异常
     */
    public long keys(String key, String pattern, Consumer<String> consumer) throws Exception {
        return getOrCreate(key).sync().keys(consumer::accept, pattern);
    }

}
