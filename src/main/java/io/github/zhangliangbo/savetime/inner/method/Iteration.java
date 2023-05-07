package io.github.zhangliangbo.savetime.inner.method;

import io.github.zhangliangbo.savetime.inner.problem.MiniMax;
import io.github.zhangliangbo.savetime.inner.problem.Minimum;

import java.util.Arrays;

/**
 * @author zhangliangbo
 * @since 2023-05-07
 */
public class Iteration implements Minimum, MiniMax {

    public static void main(String[] args) {
        Iteration iteration = new Iteration();
        int minimum = iteration.minimum(iteration.minimumSample());
        System.out.println(minimum);
        int[] minimax = iteration.miniMax(iteration.minimumSample());
        System.out.println(Arrays.toString(minimax));
    }

    @Override
    public int minimum(int[] a) {
        int min = a[0];
        for (int i = 1; i < a.length; i++) {
            if (a[i] < min) {
                min = a[i];
            }
        }
        return min;
    }

    @Override
    public int[] miniMax(int[] a) {
        boolean even = a.length % 2 == 0;
        int min;
        int max;
        int begin;
        if (even) {
            if (a[0] < a[1]) {
                min = a[0];
                max = a[1];
            } else {
                min = a[1];
                max = a[0];
            }
            begin = 2;
        } else {
            min = a[0];
            max = a[0];
            begin = 1;
        }
        for (int i = begin; i < a.length; i += 2) {
            if (a[i] < a[i + 1]) {
                if (a[i] < min) {
                    min = a[i];
                }
                if (a[i + 1] > max) {
                    max = a[i + 1];
                }
            } else {
                if (a[i + 1] < min) {
                    min = a[i + 1];
                }
                if (a[i] > max) {
                    max = a[i];
                }
            }
        }
        return new int[]{min, max};
    }

}
