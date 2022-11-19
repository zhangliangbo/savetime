package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author zhangliangbo
 * @since 2022-11-19
 */
public class Gitlab extends Http {
    /**
     * 版本
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode version(String key) throws Exception {
        return get(key, "/version");
    }

    /**
     * 组
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode groups(String key) throws Exception {
        return get(key, "/groups");
    }

    /**
     * 项目
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode projects(String key) throws Exception {
        return get(key, "/projects");
    }

}
