package io.github.zhangliangbo.savetime.inner;

import java.net.URL;

/**
 * @author zhangliangbo
 * @since 2022/8/27
 */
public interface Configurable<T> {
    void setConfig(URL config);

    URL getConfig();

    T getOrCreate(String key) throws Exception;

    void clearAll();
}
