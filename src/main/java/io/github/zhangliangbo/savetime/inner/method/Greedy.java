package io.github.zhangliangbo.savetime.inner.method;

import io.github.zhangliangbo.savetime.inner.problem.MaxSubArray;

import java.util.Arrays;

/**
 * @author zhangliangbo
 * @since 2023/4/17
 */
public class Greedy implements MaxSubArray {
    public static void main(String[] args) {
        Greedy greedy = new Greedy();
        int[] ints = greedy.maxSubArray(greedy.maxSubArraySample());
        System.out.println(Arrays.toString(ints));
    }

    @Override
    public int[] maxSubArray(int[] nums) {
        int dp = nums[0];
        int low = 0;
        int high = 0;

        int pre = dp;

        int max = dp;
        int maxLeft = 0;
        int maxRight = 0;


        for (int i = 1; i < nums.length; i++) {
            int plus = pre + nums[i];
            if (plus > nums[i]) {
                dp = plus;
                high = i;
            } else {
                dp = nums[i];
                low = i;
            }
            if (dp > max) {
                max = dp;
                maxLeft = low;
                maxRight = high;
            }
        }
        return new int[]{maxLeft, maxRight, max};
    }
}
