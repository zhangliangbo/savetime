package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.zhangliangbo.savetime.ST;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhangliangbo
 * @since 2022/9/4
 */
public class RabbitWeb extends Http {
    public JsonNode queueInfo(String key, String virtualHost, String queue) throws Exception {
        return get(key, "/api/queues/" + URLEncoder.encode(virtualHost, StandardCharsets.UTF_8) + "/" + queue);
    }

    public JsonNode queueInfo(String key, String queue) throws Exception {
        return queueInfo(key, "/", queue);
    }

    public JsonNode exchangePublish(String key, String virtualHost, String exchange, Object body, Map<String, Object> headers) throws Exception {
        Object payload = body instanceof String ? body : ST.io.toJson(body);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("delivery_mode", 1);
        if (Objects.nonNull(headers) && !headers.isEmpty()) {
            properties.put("headers", headers);
        }
        Map<String, Object> fullBody = new LinkedHashMap<>();
        fullBody.put("properties", properties);
        fullBody.put("payload", payload);
        fullBody.put("routing_key", "#");
        fullBody.put("payload_encoding", "string");
        return post(key,
                "/api/exchanges/" + URLEncoder.encode(virtualHost, StandardCharsets.UTF_8) + "/" + exchange + "/publish",
                null,
                fullBody);
    }

    public JsonNode exchangePublish(String key, String virtualHost, String exchange, Object body) throws Exception {
        return exchangePublish(key, virtualHost, exchange, body, null);
    }

    public JsonNode exchangePublish(String key, String exchange, Object body, Map<String, Object> headers) throws Exception {
        return exchangePublish(key, "/", exchange, body, headers);
    }

    public JsonNode exchangePublish(String key, String exchange, Object body) throws Exception {
        return exchangePublish(key, "/", exchange, body, null);
    }
}
