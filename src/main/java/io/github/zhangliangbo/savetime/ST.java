package io.github.zhangliangbo.savetime;

import io.github.zhangliangbo.savetime.inner.*;
import io.vavr.collection.List;

/**
 * @author zhangliangbo
 * @since 2022/8/27
 */
public class ST {
    public static final Ssh ssh = new Ssh();
    public static final Http http = new Http();
    public static final Jdbc jdbc = new Jdbc();
    public static final Lettuce lettuce = new Lettuce();
    public static final IO io = new IO();

    public static <T> List<T> listOf(Iterable<T> iterable) {
        return List.ofAll(iterable);
    }

    @SafeVarargs
    public static <T> List<T> listOf(T... t) {
        return List.of(t);
    }

}
