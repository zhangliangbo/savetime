package io.github.zhangliangbo.savetime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangliangbo
 * @since 2022/8/21
 */
public class Lettuce {
    private static URL config;

    public static void setConfig(URL config) {
        Lettuce.config = config;
    }

    private static final Map<String, StatefulRedisConnection<String, String>> connectionMap = new HashMap<>();

    public static StatefulRedisConnection<String, String> getOrCreateConnection(String env) throws Exception {
        Preconditions.checkNotNull(config, "配置文件不能为空");
        StatefulRedisConnection<String, String> result = connectionMap.get(env);
        if (result != null) {
            return result;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode treeNode = objectMapper.readTree(config);
        JsonNode redis = treeNode.get(env);
        String url = redis.get("uri").asText();
        String password = redis.get("password").asText();
        URI uri = URI.create(url);
        JsonNode ssh = redis.get("ssh");

        url = Ssh.forward(ssh, uri, url);

        RedisURI redisUri = RedisURI.create(url);
        redisUri.setPassword(password.toCharArray());
        RedisClient redisClient = RedisClient.create(redisUri);
        StatefulRedisConnection<String, String> connection = redisClient.connect();

        connectionMap.put(env, connection);
        return connection;
    }
}
