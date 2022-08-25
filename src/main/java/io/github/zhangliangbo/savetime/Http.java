package io.github.zhangliangbo.savetime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Content;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.util.Timeout;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhangliangbo
 * @since 2022/8/22
 */
public class Http {

    private static URL config;

    public static void setConfig(URL config) {
        Http.config = config;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    public static JsonNode fromJson(Content s) {
        try {
            return objectMapper.readTree(s.asBytes());
        } catch (Exception e) {
            ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
            objectNode.put("response", s.asString());
            return objectNode;
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

    public static JsonNode send(Request request) throws IOException {
        Response response = request.execute();
        Content content = response.returnContent();
        return fromJson(content);
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

    private static URI createUri(String key, String url, Map<String, Object> query) throws IOException {
        JsonNode env = readEnv(key);
        URI uri = URI.create(env.get("gateway").asText() + url + queryString(query));
        System.out.println(uri);
        return uri;
    }

    public static JsonNode get(String key, String url, Map<String, Object> query, Map<String, String> header) throws Exception {
        URI uri = createUri(key, url, query);
        Request request = Request.get(uri).connectTimeout(Timeout.ofMinutes(1));
        processHeader(key, request, header);
        return send(request);
    }

    public static JsonNode get(String key, String url, Map<String, Object> query) throws Exception {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return get(key, url, query, header);
    }

    public static JsonNode get(String key, String url) throws Exception {
        return get(key, url, null);
    }

    private static void processHeader(String key, Request request, Map<String, String> header) throws IOException {
        if (Objects.nonNull(header)) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
        if (Objects.nonNull(tokenGenerator)) {
            String token = makeToken(key);
            request.addHeader(HttpHeaders.AUTHORIZATION, token);
        }
    }

    //post请求
    public static JsonNode post(String key, String url, Map<String, Object> query, Map<String, Object> body, Map<String, String> header) throws IOException {
        URI uri = createUri(key, url, query);
        Request request = Request.post(uri);
        if (Objects.nonNull(body)) {
            request.bodyString(toJson(body), null);
        }
        processHeader(key, request, header);
        return send(request);
    }

    public static JsonNode post(String key, String url, Map<String, Object> query, Map<String, Object> body) throws IOException {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return post(key, url, query, body, header);
    }

    public static JsonNode post(String key, String url, Map<String, Object> query) throws IOException {
        return post(key, url, query, null);
    }

    public static JsonNode post(String key, String url) throws IOException {
        return post(key, url, null);
    }

    public static JsonNode postToken(JsonNode env, String url, Map<String, Object> query, Map<String, Object> body, Map<String, String> header) throws IOException {
        URI uri = URI.create(env.get("gateway").asText() + url + queryString(query));
        System.out.println(uri);
        Request request = Request.post(uri);
        if (Objects.nonNull(body)) {
            request.bodyString(toJson(body), null);
        }
        if (Objects.nonNull(header)) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return send(request);
    }

    public static JsonNode postToken(JsonNode env, String url, Map<String, Object> query, Map<String, Object> body) throws IOException {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return postToken(env, url, query, body, header);
    }

    public static JsonNode postMultipart(String key, String url, Map<String, Object> query, Map<String, Object> body) throws IOException {
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            if (Objects.isNull(entry.getValue())) {
                continue;
            }
            if (entry.getValue() instanceof File) {
                File f = (File) entry.getValue();
                multipartEntityBuilder.addBinaryBody(entry.getKey(), f, ContentType.APPLICATION_OCTET_STREAM, f.getName());
            } else {
                multipartEntityBuilder.addTextBody(entry.getKey(), entry.getValue().toString());
            }
        }
        HttpEntity httpEntity = multipartEntityBuilder.build();

        URI uri = createUri(key, url, query);
        Request request = Request.post(uri).body(httpEntity).connectTimeout(Timeout.ofMinutes(1));
        processHeader(key, request, null);
        return send(request);
    }

    //put请求
    public static JsonNode put(String key, String url, Map<String, Object> query, Map<String, Object> body, Map<String, String> header) throws IOException {
        URI uri = createUri(key, url, query);
        Request request = Request.put(uri);
        if (Objects.nonNull(body)) {
            request.bodyString(toJson(body), null);
        }
        processHeader(key, request, header);
        return send(request);
    }

    public static JsonNode put(String key, String url, Map<String, Object> query, Map<String, Object> body) throws IOException {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return put(key, url, query, body, header);
    }

    public static JsonNode put(String key, String url, Map<String, Object> query) throws IOException {
        return put(key, url, query, null);
    }

    public static JsonNode put(String key, String url) throws IOException {
        return put(key, url, null);
    }

    //delete请求
    public static JsonNode delete(String key, String url, Map<String, Object> query, Map<String, String> header) throws IOException {
        URI uri = createUri(key, url, query);
        Request request = Request.delete(uri);
        processHeader(key, request, header);
        return send(request);
    }

    public static JsonNode delete(String key, String url, Map<String, Object> query) throws IOException {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return delete(key, url, query, header);
    }

    public static JsonNode delete(String key, String url) throws IOException {
        return delete(key, url, null);
    }

}
