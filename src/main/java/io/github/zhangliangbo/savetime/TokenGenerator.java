package io.github.zhangliangbo.savetime;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.core5.http.HttpHeaders;

/**
 * @author zhangliangbo
 * @since 2022/8/23
 */
public interface TokenGenerator {
    /**
     * 获取token
     *
     * @param env 环境信息（用户名，密码等）
     * @return 【token,有效时长s】
     */
    Pair<String, Long> getToken(JsonNode env) throws Exception;

    /**
     * @return token的请求头
     */
    default String getTokenHeader() {
        return HttpHeaders.AUTHORIZATION;
    }
}
