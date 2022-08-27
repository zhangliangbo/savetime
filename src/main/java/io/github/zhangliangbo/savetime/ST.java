package io.github.zhangliangbo.savetime;

import io.github.zhangliangbo.savetime.inner.Http;
import io.github.zhangliangbo.savetime.inner.Jdbc;
import io.github.zhangliangbo.savetime.inner.Lettuce;
import io.github.zhangliangbo.savetime.inner.Ssh;

/**
 * @author zhangliangbo
 * @since 2022/8/27
 */
public class ST {
    public static final Ssh ssh = new Ssh();
    public static final Http http = new Http();
    public static final Jdbc jdbc = new Jdbc();
    public static final Lettuce lettuce = new Lettuce();
}
