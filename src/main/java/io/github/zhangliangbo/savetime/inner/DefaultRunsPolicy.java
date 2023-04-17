package io.github.zhangliangbo.savetime.inner;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 默认拒绝策略
 *
 * @author zhangliangbo
 * @since 2023/4/17
 */
public class DefaultRunsPolicy extends ThreadPoolExecutor.CallerRunsPolicy {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        super.rejectedExecution(r, e);
        System.out.printf("队列拒绝 本地运行 %s%n", e.toString());
    }
}
