package io.github.zhangliangbo.savetime.inner.method;

import io.github.zhangliangbo.savetime.inner.problem.MaxSubArray;

import java.util.Arrays;

/**
 * @author zhangliangbo
 * @since 2023/4/17
 */
public class DivideConquer implements MaxSubArray {

    @Override
    public int[] maxSubArray(int[] nums) {
        return findMaximumSubArray(nums, 0, nums.length - 1);
    }

    private int[] findMaxCrossingSubArray(int[] nums, int low, int mid, int high) {
        int leftSum = Integer.MIN_VALUE;
        int maxLeft = mid;

        int sum = 0;
        for (int i = mid; i >= low; i--) {
            sum = sum + nums[i];
            if (sum > leftSum) {
                leftSum = sum;
                maxLeft = i;
            }
        }

        int rightSum = Integer.MIN_VALUE;
        int maxRight = mid;
        sum = 0;
        for (int i = mid + 1; i <= high; i++) {
            sum = sum + nums[i];
            if (sum > rightSum) {
                rightSum = sum;
                maxRight = i;
            }
        }

        return new int[]{maxLeft, maxRight, leftSum + rightSum};
    }

    private int[] findMaximumSubArray(int[] nums, int low, int high) {
        if (high == low) {
            return new int[]{low, high, nums[low]};
        }
        int mid = (low + high) / 2;
        int[] left = findMaximumSubArray(nums, low, mid);
        int[] right = findMaximumSubArray(nums, mid + 1, high);
        int[] crossing = findMaxCrossingSubArray(nums, low, mid, high);
        if (left[2] >= right[2] && left[2] >= crossing[2]) {
            return left;
        } else if (right[2] >= left[2] && right[2] >= crossing[2]) {
            return right;
        } else {
            return crossing;
        }
    }

    public static void main(String[] args) {
        DivideConquer divideConquer = new DivideConquer();
        int[] maximumSubArray = divideConquer.maxSubArray(divideConquer.maxSubArraySample());
        System.out.println(Arrays.toString(maximumSubArray));
    }

}