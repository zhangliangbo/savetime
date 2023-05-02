package io.github.zhangliangbo.savetime.inner.problem;

/**
 * @author zhangliangbo
 * @since 2023/5/2
 */
public interface QuickSort {

    default int[] quickSortSample() {
        return new int[]{2, 8, 7, 1, 3, 5, 6, 4};
    }

    void quickSort(int[] a);

}
