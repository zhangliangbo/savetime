package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.github.zhangliangbo.savetime.ST;

import java.net.URI;

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

}
