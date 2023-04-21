package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhangliangbo.savetime.ST;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.collections.CollectionUtils;

import java.net.URI;
import java.util.List;
import java.util.Objects;
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
    public void scan(String key, String pattern, int limit, Consumer<List<String>> consumer) throws Exception {
        ScanArgs scanArgs = ScanArgs.Builder.matches(pattern).limit(limit);
        RedisCommands<String, String> sync = getOrCreate(key).sync();

        KeyScanCursor<String> keyScanCursor = sync.scan(scanArgs);
        while (!keyScanCursor.isFinished()) {
            List<String> keys = keyScanCursor.getKeys();
            if (CollectionUtils.isNotEmpty(keys)) {
                consumer.accept(keys);
            }
            keyScanCursor = sync.scan(keyScanCursor);
        }
    }

}
