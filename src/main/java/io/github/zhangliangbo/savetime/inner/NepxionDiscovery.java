package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author zhangliangbo
 * @since 2023/3/3
 */
public class NepxionDiscovery extends Http {
    /**
     * 所有服务
     *
     * @param key 环境
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode services(String key) throws Exception {
        return post(key, "/common/do-list-service-names");
    }

    /**
     * 服务元数据
     *
     * @param key     环境
     * @param service 服务
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode serviceMetaData(String key, String service) throws Exception {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("serviceName", service);
        return post(key, "/common/do-list-service-metadata", query);
    }

}
