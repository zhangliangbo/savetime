package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.zhangliangbo.savetime.ST;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;

import java.lang.reflect.InvocationTargetException;

/**
 * @author zhangliangbo
 * @since 2022/9/9
 */
public class Mail extends AbstractConfigurable<JsonNode> {
    @Override
    protected boolean isValid(JsonNode jsonNode) {
        return true;
    }

    @Override
    protected JsonNode create(String key) throws Exception {
        return ST.io.readTree(getConfig()).get(key);
    }

    public <T extends Email> T newEmail(String key, Class<T> cls) throws Exception {
        T email = cls.getConstructor().newInstance();
        JsonNode jsonNode = getOrCreate(key);
        email.setHostName(jsonNode.get("host").asText());
        email.setSmtpPort(jsonNode.get("port").asInt());
        email.setAuthenticator(new DefaultAuthenticator(jsonNode.get("username").asText(), jsonNode.get("password").asText()));
        email.setSSLOnConnect(true);
        email.setFrom(jsonNode.get("username").asText());
        return email;
    }

}
