package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.commons.lang3.RandomUtils;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zhangliangbo
 * @since 2022/8/21
 */
public class Ssh extends AbstractConfigurable<Session> {

    @Override
    protected boolean isValid(Session session) {
        return session.isConnected();
    }

    @Override
    protected Session create(String key) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode treeNode = objectMapper.readTree(getConfig());
        JsonNode ssh = treeNode.get(key);
        JSch jsch = new JSch();
        Session session = jsch.getSession(
                ssh.get("username").asText(),
                ssh.get("host").asText(),
                ssh.get("port") == null ? 22 : ssh.get("port").asInt());
        session.setPassword(ssh.get("password").asText());
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();

        System.out.println("ssh开启成功 " + key);

        return session;
    }

    protected void clearOne(Session session) {
        session.disconnect();
    }

    /**
     * @param ssh
     * @param uri
     * @return 本地转发端口
     * @throws Exception
     */
    public String forward(JsonNode ssh, URI uri, String url) throws Exception {
        if (Objects.isNull(ssh)) {
            return url;
        }
        Session session = getOrCreate(ssh.asText());

        List<String> hadDirect = Arrays.stream(session.getPortForwardingL())
                .filter(it -> it.contains(uri.getHost() + ":" + uri.getPort()))
                .collect(Collectors.toList());
        int port;
        if (hadDirect.isEmpty()) {
            Set<Integer> usedPort = Stream.of(session.getPortForwardingL())
                    .map(it -> Integer.parseInt(it.split(":")[0]))
                    .collect(Collectors.toSet());
            do {
                int localPort = RandomUtils.nextInt(49152, 65535);
                if (usedPort.contains(localPort)) {
                    port = -1;
                    continue;
                }
                try {
                    port = session.setPortForwardingL(localPort, uri.getHost(), uri.getPort());
                } catch (Exception e) {
                    System.out.println("ssh端口已占用 " + localPort);
                    port = -1;
                }
            }
            while (port == -1);
        } else {
            port = Integer.parseInt(hadDirect.get(0).split(":")[0]);
        }
        url = url.replace(uri.getHost(), "127.0.0.1");
        url = url.replace(String.valueOf(uri.getPort()), String.valueOf(port));
        System.out.println("ssh转发成功 " + url);
        return url;
    }

    public String[] showForwardL(String key) throws Exception {
        Session session = getOrCreate(key);
        if (Objects.isNull(session)) {
            return new String[0];
        }
        return session.getPortForwardingL();
    }

}
