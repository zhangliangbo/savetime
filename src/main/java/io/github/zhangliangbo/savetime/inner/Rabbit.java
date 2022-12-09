package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.*;
import io.github.zhangliangbo.savetime.ST;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.ContentType;

import java.net.URI;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zhangliangbo
 * @since 2022/9/4
 */
public class Rabbit extends AbstractConfigurable<Channel> {

    @Override
    protected boolean isValid(Channel channel) {
        return channel.isOpen();
    }

    @Override
    protected Channel create(String key) throws Exception {
        JsonNode conf = ST.io.readTree(getConfig()).get(key);

        JsonNode ssh = conf.get("ssh");
        String url = conf.get("uri").asText();
        URI uri = URI.create(url);

        url = ST.ssh.forward(ssh, uri, url);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(url);
        factory.setVirtualHost(conf.get("virtualHost").asText());
        factory.setUsername(conf.get("username").asText());
        factory.setPassword(conf.get("password").asText());
        Connection connection = factory.newConnection();
        return connection.createChannel();
    }

    public long getNextPublishSeqNo(String key) throws Exception {
        return getOrCreate(key).getNextPublishSeqNo();
    }

    public int getChannelNumber(String key) throws Exception {
        return getOrCreate(key).getChannelNumber();
    }

    public AMQP.BasicProperties.Builder newBasicPropertiesBuilder() throws Exception {
        return new AMQP.BasicProperties().builder();
    }

    public long messageCount(String key, String queueName) throws Exception {
        return getOrCreate(key).messageCount(queueName);
    }

    public long consumerCount(String key, String queueName) throws Exception {
        return getOrCreate(key).consumerCount(queueName);
    }

    public JsonNode basicGet(String key, String queueName, boolean autoAck) throws Exception {
        GetResponse getResponse = getOrCreate(key).basicGet(queueName, autoAck);
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);

        objectNode.put("messageCount", getResponse.getMessageCount());

        AMQP.BasicProperties props = getResponse.getProps();
        ObjectNode propsNode = new ObjectNode(JsonNodeFactory.instance);
        propsNode.put("contentType", props.getContentType());
        propsNode.put("contentEncoding", props.getContentEncoding());
        propsNode.set("headers", ST.io.toJsonNode(props.getHeaders()));
        propsNode.put("deliveryMode", props.getDeliveryMode());
        propsNode.put("priority", props.getPriority());
        propsNode.put("correlationId", props.getCorrelationId());
        propsNode.put("replyTo", props.getReplyTo());
        propsNode.put("expiration", props.getExpiration());
        propsNode.put("messageId", props.getMessageId());
        propsNode.put("timestamp", Optional.ofNullable(props.getTimestamp()).map(Date::toString).orElse(null));
        propsNode.put("type", props.getType());
        propsNode.put("userId", props.getUserId());
        propsNode.put("appId", props.getAppId());
        propsNode.put("clusterId", props.getClusterId());
        objectNode.set("props", propsNode);

        Envelope envelope = getResponse.getEnvelope();
        objectNode.set("envelope", ST.io.toJsonNode(envelope));

        byte[] body = getResponse.getBody();
        String mimeType = StringUtils.isBlank(props.getContentType()) ? null : ContentType.create(props.getContentType()).getMimeType();
        if (Objects.equals(ContentType.APPLICATION_JSON.getMimeType(), mimeType)) {
            objectNode.set("body", ST.io.readTree(body));
        } else {
            try {
                JsonNode jsonNode = ST.io.readTree(body);
                objectNode.set("body", jsonNode);
            } catch (Exception e) {
                objectNode.put("body", new String(body));
            }
        }

        return objectNode;
    }

}
