package io.github.zhangliangbo.savetime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * @author zhangliangbo
 * @since 2022/8/22
 */
public class Http {

    private static URL config;

    public static void setConfig(URL config) {
        Http.config = config;
    }

    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public HttpClient getClient() {
        return client;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static String queryString(Map<String, Object> map) {
        if (Objects.isNull(map) || map.isEmpty()) {
            return "";
        }
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            copy.put(entry.getKey(), java.net.URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return "?" + Joiner.on("&").withKeyValueSeparator("=").join(copy);
    }

    public static String toJson(Object obj) throws JsonProcessingException {
        if (obj instanceof String) {
            return (String) obj;
        }
        return objectMapper.writeValueAsString(obj);
    }

    public static Map<String, Object> toMap(JsonNode jsonNode) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(jsonNode);
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    public static Map<String, Object> toMap(File file) throws IOException {
        return toMap(file, true);
    }

    public static Map<String, Object> toMap(File file, boolean filterNull) throws IOException {
        Map<String, Object> map = objectMapper.readValue(file, new TypeReference<>() {
        });
        if (!filterNull) {
            return map;
        }
        map.entrySet().removeIf(next -> Objects.isNull(next.getValue()));
        return map;
    }

    public static JsonNode fromJson(HttpResponse<String> s) {
        System.out.println(s.request().uri());
        try {
            return objectMapper.readTree(s.body());
        } catch (Exception e) {
            return new TextNode(s.body());
        }
    }

    public static Map<String, JsonNode> envMap = new HashMap<>();

    public static JsonNode readEnv(String key) throws IOException {
        Preconditions.checkNotNull(config, "配置文件不能为空");

        JsonNode env = envMap.get(key);
        if (env != null) {
            return env;
        }
        env = objectMapper.readTree(config).get(key);

        envMap.put(key, env);
        return env;
    }

    public static JsonNode send(HttpRequest.Builder builder) {
        return client.sendAsync(builder.build(), BodyHandlers.ofString())
                .thenApply(Http::fromJson)
                .join();
    }

    private static TokenGenerator tokenGenerator;

    public static void setTokenGenerator(TokenGenerator tokenGenerator) {
        Http.tokenGenerator = tokenGenerator;
    }

    private static final Map<String, Pair<String, Long>> tokenMap = new HashMap<>();

    public static String getToken(String key) throws IOException {
        return makeToken(key);
    }

    private static String makeToken(String key) throws IOException {
        Pair<String, Long> token = tokenMap.get(key);
        boolean expire = false;
        if (token != null && !(expire = (token.getRight() + 1000 < System.currentTimeMillis()))) {
            return token.getLeft();
        }
        System.out.println("生成新的token " + expire);
        JsonNode jsonNode = readEnv(key);
        if (Objects.isNull(tokenGenerator)) {
            return null;
        }
        Pair<String, Long> pair = tokenGenerator.getToken(jsonNode);
        if (Objects.isNull(pair)) {
            return null;
        }
        pair = Pair.of(pair.getLeft(), System.currentTimeMillis() + pair.getRight() * 1000);
        tokenMap.put(key, pair);
        return pair.getLeft();
    }

    public static JsonNode get(String key, String url, Map<String, Object> query, Map<String, String> header) throws Exception {
        JsonNode env = readEnv(key);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(env.get("gateway").asText() + url + queryString(query)))
                .timeout(Duration.ofMinutes(1))
                .GET();
        processHeader(key, builder, header);
        return send(builder);
    }

    public static JsonNode get(String key, String url, Map<String, Object> query) throws Exception {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return get(key, url, query, header);
    }

    public static JsonNode get(String key, String url) throws Exception {
        return get(key, url, null);
    }

    private static void processHeader(String key, HttpRequest.Builder builder, Map<String, String> header) throws IOException {
        if (Objects.nonNull(header)) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        if (Objects.nonNull(tokenGenerator)) {
            String token = makeToken(key);
            builder.header(HttpHeaders.AUTHORIZATION, token);
        }
    }

    //post请求
    public static JsonNode post(String key, String url, Map<String, Object> query, Map<String, Object> body, Map<String, String> header) throws IOException {
        JsonNode env = readEnv(key);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(env.get("gateway").asText() + url + queryString(query)))
                .timeout(Duration.ofMinutes(2))
                .POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(toJson(body)));
        processHeader(key, builder, header);
        return client.sendAsync(builder.build(), BodyHandlers.ofString())
                .thenApply(Http::fromJson)
                .join();
    }

    public static JsonNode post(String key, String url, Map<String, Object> query, Map<String, Object> body) throws IOException {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return post(key, url, query, body, header);
    }

    public static JsonNode post(String key, String url, Map<String, Object> query) throws IOException {
        return post(key, url, query, null);
    }

    public static JsonNode post(String key, String url) throws IOException {
        return post(key, url, null, null);
    }

    public static JsonNode postToken(JsonNode env, String url, Map<String, Object> query, Map<String, Object> body, Map<String, String> header) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(env.get("gateway").asText() + url + queryString(query)))
                .timeout(Duration.ofMinutes(2))
                .POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(toJson(body)));
        if (Objects.nonNull(header)) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        return client.sendAsync(builder.build(), BodyHandlers.ofString())
                .thenApply(Http::fromJson)
                .join();
    }

    public static JsonNode postToken(JsonNode env, String url, Map<String, Object> query, Map<String, Object> body) throws IOException {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return postToken(env, url, query, body, header);
    }

}
