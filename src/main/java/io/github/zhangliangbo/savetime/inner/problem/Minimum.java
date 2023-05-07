package io.github.zhangliangbo.savetime.inner.problem;

public interface Minimum {
    default int[] minimumSample() {
        return new int[]{3, 2, 9, 0, 7, 5, 4, 8, 6, 1};
    }

    int minimum(int[] a);
}
