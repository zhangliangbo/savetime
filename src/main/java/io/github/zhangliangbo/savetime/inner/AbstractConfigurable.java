package io.github.zhangliangbo.savetime.inner;

import com.google.common.base.Preconditions;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhangliangbo
 * @since 2022/8/27
 */
public abstract class AbstractConfigurable<T> implements Configurable<T> {

    private URL config;
    private final Map<String, T> cache = new HashMap<>();

    @Override
    public void setConfig(URL config) {
        this.config = config;
    }

    @Override
    public URL getConfig() {
        return this.config;
    }

    @Override
    public T getOrCreate(String key) throws Exception {
        Preconditions.checkNotNull(getConfig(), "配置文件不能为空");
        T t = cache.get(key);
        if (Objects.nonNull(t) && isValid(t)) {
            return t;
        }
        synchronized (this) {
            t = cache.get(key);
            if (Objects.nonNull(t) && isValid(t)) {
                return t;
            }
            t = create(key);
            cache.put(key, t);
        }
        return t;
    }

    @Override
    public void clearAll() {
        for (Map.Entry<String, T> entry : cache.entrySet()) {
            System.out.println(entry.getKey() + "断开连接");
            clearOne(entry.getValue());
        }
        cache.clear();
    }

    protected abstract boolean isValid(T t);

    protected abstract T create(String key) throws Exception;

    protected void clearOne(T t) {

    }

}
