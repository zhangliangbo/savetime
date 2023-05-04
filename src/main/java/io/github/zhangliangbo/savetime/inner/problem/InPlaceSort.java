package io.github.zhangliangbo.savetime.inner.problem;

/**
 * @author zhangliangbo
 * @since 2023/5/4
 */
public interface InPlaceSort {
    default void swap(int[] a, int i, int j) {
        if (i == j || a[i] == a[j]) {
            return;
        }
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
