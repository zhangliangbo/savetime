package io.github.zhangliangbo.savetime.inner.method;

import io.github.zhangliangbo.savetime.inner.problem.MaxSubArray;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author zhangliangbo
 * @since 2023/4/5
 */
public class DynamicProgramming implements MaxSubArray {

    /**
     * 只求长度，不用打印序列
     *
     * @param nums 输入序列
     * @return 最大递增长度
     */
    public int longestIncreasingSubsequenceLength(int[] nums) {
        int n = nums.length;

        //不赋初值1，节省时间，最后结果加上1
        int[] dp = new int[n];

        int max = 0;
        for (int i = 1; i < nums.length; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[i] > nums[j]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                    //循环的同时找出最大值
                    if (dp[i] > max) {
                        max = dp[i];
                    }
                }
            }
        }

        return max + 1;
    }

    /**
     * 递增序列
     *
     * @param nums 输入序列
     * @return 最大递增序列
     */
    public List<Integer> longestIncreasingSubsequence(int[] nums) {
        List<Integer> res = new LinkedList<>();

        int n = nums.length;

        //不赋初值1，节省时间，最后结果加上1
        int[] dp = new int[n];

        int max = 0;
        for (int i = 1; i < nums.length; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[i] > nums[j]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                    //循环的同时找出最大值
                    if (dp[i] > max) {
                        res.add(nums[i]);
                        max = dp[i];
                    }
                }
            }
        }

        return res;
    }

    public static void main(String[] args) {
        String[] s = "73 58 213 78 255 231 165 52 51 288 93 177 61 270 116".split(" ");
        int[] nums = new int[s.length];
        for (int i = 0; i < s.length; i++) {
            nums[i] = Integer.parseInt(s[i]);
        }
        DynamicProgramming dynamicProgramming = new DynamicProgramming();
        System.out.println(dynamicProgramming.longestIncreasingSubsequenceLength(nums));
        System.out.println(dynamicProgramming.longestIncreasingSubsequence(nums));
        int[] ints = dynamicProgramming.maxSubArray(new int[]{9, -1, -1, -1});
        System.out.println(Arrays.toString(ints));
    }

    @Override
    public int[] maxSubArray(int[] nums) {
        int[] dp = new int[nums.length];
        dp[0] = nums[0];

        int max = dp[0];
        int maxLeft = 0;
        int maxRight = 0;

        int low = 0;
        int high = 0;
        for (int i = 1; i < nums.length; i++) {
            int plus = dp[i - 1] + nums[i];
            if (plus > nums[i]) {
                dp[i] = plus;
                high = i;
            } else {
                dp[i] = nums[i];
                low = i;
            }
            if (dp[i] > max) {
                max = dp[i];
                maxLeft = low;
                maxRight = high;
            }
        }

        return new int[]{maxLeft, maxRight, max};
    }

}
