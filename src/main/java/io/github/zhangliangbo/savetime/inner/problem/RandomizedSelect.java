package io.github.zhangliangbo.savetime.inner.problem;

public interface RandomizedSelect {
    default int[] randomizedSelectSample() {
        return new int[]{3, 2, 9, 0, 7, 5, 4, 8, 6, 1};
    }

    int randomizedSelect(int[] a, int i);
}
