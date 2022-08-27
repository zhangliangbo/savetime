package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zhangliangbo.savetime.ST;
import io.github.zhangliangbo.savetime.inner.AbstractConfigurable;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

import java.net.URI;

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

}
