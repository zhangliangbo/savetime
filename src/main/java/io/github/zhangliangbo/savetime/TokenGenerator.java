package io.github.zhangliangbo.savetime;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author zhangliangbo
 * @since 2022/8/23
 */
public interface TokenGenerator {
    Pair<String, Long> getToken(JsonNode env);
}
