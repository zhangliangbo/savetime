package io.github.zhangliangbo.savetime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.lang3.RandomUtils;

import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zhangliangbo
 * @since 2022/8/21
 */
public class Ssh {
    private static URL config;

    public static void setConfig(URL config) {
        Ssh.config = config;
    }

    private static final Map<String, Session> sshMap = new HashMap<>();

    public static Session getOrCreateSession(String key) throws Exception {
        Preconditions.checkNotNull(config, "配置文件不能为空");
        Session result = sshMap.get(key);
        if (result != null) {
            return result;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode treeNode = objectMapper.readTree(config);
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
        sshMap.put(key, session);

        return session;
    }

    /**
     * @param ssh
     * @param uri
     * @return 本地转发端口
     * @throws Exception
     */
    public static String forward(JsonNode ssh, URI uri, String url) throws Exception {
        if (Objects.isNull(ssh)) {
            return url;
        }
        Session session = Ssh.getOrCreateSession(ssh.asText());

        List<String> hadDirect = Arrays.stream(session.getPortForwardingL())
                .filter(it -> it.contains(uri.getHost() + ":" + uri.getPort()))
                .collect(Collectors.toList());
        int port;
        if (hadDirect.isEmpty()) {
            Set<Integer> usedPort = Stream.of(session.getPortForwardingL())
                    .map(it -> Integer.parseInt(it.split(":")[0]))
                    .collect(Collectors.toSet());
            int localPort = RandomUtils.nextInt(49152, 65535);
            while (true) {
                if (usedPort.contains(localPort)) {
                    localPort = RandomUtils.nextInt(49152, 65535);
                } else {
                    break;
                }
            }
            port = session.setPortForwardingL(localPort, uri.getHost(), uri.getPort());
        } else {
            port = Integer.parseInt(hadDirect.get(0).split(":")[0]);
        }
        url = url.replace(uri.getHost(), "127.0.0.1");
        url = url.replace(String.valueOf(uri.getPort()), String.valueOf(port));
        System.out.println("ssh转发成功 " + url);
        return url;
    }

    public static void clear() {
        for (Map.Entry<String, Session> entry : sshMap.entrySet()) {
            System.out.println(entry.getKey() + "断开连接");
            entry.getValue().disconnect();
        }
        sshMap.clear();
    }

    public static String[] showForwardL(String key) throws JSchException {
        Session session = sshMap.get(key);
        if (Objects.isNull(session)) {
            return new String[0];
        }
        return session.getPortForwardingL();
    }

}
