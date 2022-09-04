package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import io.github.zhangliangbo.savetime.ST;
import io.github.zhangliangbo.savetime.TokenGenerator;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Content;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.util.Timeout;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zhangliangbo
 * @since 2022/8/22
 */
public class Http extends AbstractConfigurable<Triple<JsonNode, String, Long>> {

    private TokenGenerator tokenGenerator;

    public void setTokenGenerator(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    protected boolean isValid(Triple<JsonNode, String, Long> triple) {
        return triple != null && !(triple.getRight() + 1000 < System.currentTimeMillis());
    }

    private JsonNode getNode(String key) throws Exception {
        return ST.io.readTree(getConfig()).get(key);
    }

    @Override
    protected Triple<JsonNode, String, Long> create(String key) throws Exception {
        System.out.println("生成新的token " + key);
        JsonNode jsonNode = getNode(key);
        if (Objects.isNull(tokenGenerator)) {
            return null;
        }
        Pair<String, Long> pair = tokenGenerator.getToken(jsonNode);
        if (Objects.isNull(pair)) {
            return null;
        }
        String m = pair.getLeft();
        Long r = System.currentTimeMillis() + pair.getRight() * 1000;
        return Triple.of(jsonNode, m, r);
    }

    public String queryString(Map<String, Object> map) {
        if (Objects.isNull(map) || map.isEmpty()) {
            return "";
        }
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            copy.put(entry.getKey(), java.net.URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return "?" + Joiner.on("&").withKeyValueSeparator("=").join(copy);
    }

    public JsonNode fromJson(Content s) {
        try {
            return ST.io.readTree(s.asBytes());
        } catch (Exception e) {
            ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
            objectNode.put("response", s.asString());
            return objectNode;
        }
    }

    public JsonNode send(Request request) throws Exception {
        Response response = request.execute();
        Content content = response.returnContent();
        return fromJson(content);
    }

    public String getToken(String key) throws Exception {
        return makeToken(key);
    }

    private String makeToken(String key) throws Exception {
        Triple<JsonNode, String, Long> token = getOrCreate(key);
        return token.getMiddle();
    }

    private URI createUri(String key, String url, Map<String, Object> query) throws Exception {
        JsonNode env = Optional.ofNullable(getOrCreate(key)).map(Triple::getLeft).orElse(getNode(key));
        URI uri = URI.create(env.get("gateway").asText() + url + queryString(query));
        System.out.println(uri);
        return uri;
    }

    public JsonNode get(String key, String url, Map<String, Object> query, Map<String, String> header) throws Exception {
        URI uri = createUri(key, url, query);
        Request request = Request.get(uri).connectTimeout(Timeout.ofMinutes(1));
        processHeader(key, request, header);
        return send(request);
    }

    public JsonNode get(String key, String url, Map<String, Object> query) throws Exception {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return get(key, url, query, header);
    }

    public JsonNode get(String key, String url) throws Exception {
        return get(key, url, null);
    }

    private void processHeader(String key, Request request, Map<String, String> header) throws Exception {
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
    public JsonNode post(String key, String url, Map<String, Object> query, Object body, Map<String, String> header) throws Exception {
        URI uri = createUri(key, url, query);
        Request request = Request.post(uri);
        if (Objects.nonNull(body)) {
            request.bodyString(ST.io.toJson(body), null);
        }
        processHeader(key, request, header);
        return send(request);
    }

    public JsonNode post(String key, String url, Map<String, Object> query, Object body) throws Exception {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return post(key, url, query, body, header);
    }

    public JsonNode post(String key, String url, Map<String, Object> query) throws Exception {
        return post(key, url, query, null);
    }

    public JsonNode post(String key, String url) throws Exception {
        return post(key, url, null);
    }

    public JsonNode postToken(JsonNode env, String url, Map<String, Object> query, Object body, Map<String, String> header) throws Exception {
        URI uri = URI.create(env.get("gateway").asText() + url + queryString(query));
        System.out.println(uri);
        Request request = Request.post(uri);
        if (Objects.nonNull(body)) {
            request.bodyString(ST.io.toJson(body), null);
        }
        if (Objects.nonNull(header)) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return send(request);
    }

    public JsonNode postToken(JsonNode env, String url, Map<String, Object> query, Object body) throws Exception {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return postToken(env, url, query, body, header);
    }

    public JsonNode postMultipart(String key, String url, Map<String, Object> query, Map<String, Object> body) throws Exception {
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
    public JsonNode put(String key, String url, Map<String, Object> query, Object body, Map<String, String> header) throws Exception {
        URI uri = createUri(key, url, query);
        Request request = Request.put(uri);
        if (Objects.nonNull(body)) {
            request.bodyString(ST.io.toJson(body), null);
        }
        processHeader(key, request, header);
        return send(request);
    }

    public JsonNode put(String key, String url, Map<String, Object> query, Object body) throws Exception {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return put(key, url, query, body, header);
    }

    public JsonNode put(String key, String url, Map<String, Object> query) throws Exception {
        return put(key, url, query, null);
    }

    public JsonNode put(String key, String url) throws Exception {
        return put(key, url, null);
    }

    //delete请求
    public JsonNode delete(String key, String url, Map<String, Object> query, Map<String, String> header) throws Exception {
        URI uri = createUri(key, url, query);
        Request request = Request.delete(uri);
        processHeader(key, request, header);
        return send(request);
    }

    public JsonNode delete(String key, String url, Map<String, Object> query) throws Exception {
        LinkedHashMap<String, String> header = io.vavr.collection.LinkedHashMap.of(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).toJavaMap();
        return delete(key, url, query, header);
    }

    public JsonNode delete(String key, String url) throws Exception {
        return delete(key, url, null);
    }

    public String basicAuthorization(String username, String password) {
        return "Basic " + Base64.encodeBase64String(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8)
        );
    }

}
