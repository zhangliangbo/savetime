package io.github.zhangliangbo.savetime.inner.method;

import io.github.zhangliangbo.savetime.inner.problem.HeapSort;

import java.util.Arrays;

/**
 * @author zhangliangbo
 * @since 2023/5/1
 */
public class Recursion implements HeapSort {

    public static void main(String[] args) {
        Recursion recursion = new Recursion();
        int[] a = recursion.heapSortSample();
        recursion.heapSort(a);
        System.out.println(Arrays.toString(a));
    }

    @Override
    public void heapSort(int[] a) {
        buildMaxHeap(a);

        int size = a.length;
        for (int i = a.length; i > 1; i--) {
            int t = a[0];
            a[0] = a[i - 1];
            a[i - 1] = t;

            size = size - 1;
            maxHeapify(a, 1, size);
        }
    }

    private int parent(int i) {
        return i >> 1;
    }

    private int left(int i) {
        return i << 1;
    }

    private int right(int i) {
        return (i << 1) + 1;
    }

    private void maxHeapify(int[] a, int ordinal, int size) {
        int l = left(ordinal);
        int r = right(ordinal);
        int largest = ordinal;

        if (l <= size && a[l - 1] > a[largest - 1]) {
            largest = l;
        }

        if (r <= size && a[r - 1] > a[largest - 1]) {
            largest = r;
        }

        if (largest != ordinal) {
            int t = a[ordinal - 1];
            a[ordinal - 1] = a[largest - 1];
            a[largest - 1] = t;

            maxHeapify(a, largest, size);
        }
    }

    private void buildMaxHeap(int[] a) {
        int nonLeafEnd = a.length / 2;
        for (int i = nonLeafEnd; i > 0; i--) {
            maxHeapify(a, i, a.length);
        }
    }

}
