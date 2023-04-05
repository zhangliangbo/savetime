package io.github.zhangliangbo.savetime.inner;

import org.apache.commons.lang3.ArrayUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author zhangliangbo
 * @since 2022-11-17
 */
public class Cf {
    /**
     * 并行执行一定数量的任务
     * 任务队列无限大
     *
     * @param core     核心数
     * @param runnable 任务集
     * @return cf
     */
    public CompletableFuture<Void> runAll(int core, Runnable... runnable) {
        if (ArrayUtils.isEmpty(runnable)) {
            return null;
        }
        ExecutorService executorService = new ThreadPoolExecutor(core, core, 8, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());

        List<CompletableFuture<Void>> list = new LinkedList<>();
        for (Runnable r : runnable) {
            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(r, executorService);
            list.add(completableFuture);
        }
        CompletableFuture<Void> completableFuture = CompletableFuture.allOf(list.toArray(new CompletableFuture[0]));

        executorService.shutdown();

        return completableFuture;
    }

    /**
     * 默认线程数为可用核心数
     * 任务队列无限大
     *
     * @param runnable 任务集
     * @return cf
     */
    public CompletableFuture<Void> runAll(Runnable... runnable) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        return runAll(java.lang.Math.min(runnable.length, availableProcessors), runnable);
    }

}
