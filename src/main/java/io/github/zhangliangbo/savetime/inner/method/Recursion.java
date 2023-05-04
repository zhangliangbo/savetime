package io.github.zhangliangbo.savetime.inner.method;

import io.github.zhangliangbo.savetime.inner.problem.HeapSort;
import io.github.zhangliangbo.savetime.inner.problem.QuickSort;

import java.util.Arrays;

/**
 * @author zhangliangbo
 * @since 2023/5/1
 */
public class Recursion implements HeapSort, QuickSort {

    public static void main(String[] args) {
        Recursion recursion = new Recursion();
        int[] a = recursion.quickSortSample();
        recursion.quickSort(a);
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

    @Override
    public void quickSort(int[] a) {
        quickSort(a, 0, a.length - 1);
    }

    private int[] partition(int[] a, int p, int r) {
        int x = a[r];

        int lt = p - 1;
        int eq = p - 1;
        for (int j = p; j <= r - 1; j++) {
            if (a[j] == x) {
                eq = eq + 1;
                swap(a, eq, j);
            } else if (a[j] < x) {
                lt = lt + 1;
                eq = eq + 1;

                swap(a, eq, j);
                swap(a, lt, eq);
            }
        }

        swap(a, eq + 1, r);

        return new int[]{lt + 1, eq + 1};
    }

    private void quickSort(int[] a, int p, int r) {
        if (p < r) {
            int[] q = partition(a, p, r);
            quickSort(a, p, q[0] - 1);
            quickSort(a, q[1] + 1, r);
        }
    }
}
