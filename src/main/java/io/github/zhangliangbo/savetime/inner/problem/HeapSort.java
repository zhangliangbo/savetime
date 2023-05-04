package io.github.zhangliangbo.savetime.inner.problem;

/**
 * @author zhangliangbo
 * @since 2023/5/1
 */
public interface HeapSort extends InPlaceSort {

    default int[] heapSortSample() {
        return new int[]{4, 1, 3, 2, 16, 9, 10, 14, 8, 7};
    }

    void heapSort(int[] a);
}
