package io.github.zhangliangbo.savetime.inner.method;

import io.github.zhangliangbo.savetime.inner.problem.MaxSubArray;

/**
 * @author zhangliangbo
 * @since 2023/4/17
 */
public class DivideConquer implements MaxSubArray {

    @Override
    public int[] maxSubArray(int a) {
        return new int[0];
    }
    public int maxSubArray(int[] nums) {
        return getMaxSubSum(nums, 0, nums.length - 1);
    }
    private int getMaxSubSum(int[] nums, int left, int right){
        if(left == right){
            return nums[left];
        }
        int mid = left + (right - left) / 2;
        int leftMaxSum = getMaxSubSum(nums, left, mid);
        int rightMaxSum = getMaxSubSum(nums, mid + 1, right);
        int crossingMaxSum = getCrossingMaxSum(nums, mid, left, right);
        return Math.max(Math.max(leftMaxSum, rightMaxSum), crossingMaxSum);
    }

    private int getCrossingMaxSum(int[] nums, int mid, int left, int right){
        int leftMaxSum = nums[mid];
        int tempSum = nums[mid];
        for(int i = mid - 1; i >= left; i--){
            tempSum += nums[i];
            leftMaxSum = Math.max(leftMaxSum, tempSum);
        }

        int rightMaxSum = nums[mid + 1];
        tempSum = nums[mid + 1];
        for(int i = mid + 2; i <= right; i++){
            tempSum += nums[i];
            rightMaxSum = Math.max(rightMaxSum, tempSum);
        }

        return leftMaxSum + rightMaxSum;
    }
}
