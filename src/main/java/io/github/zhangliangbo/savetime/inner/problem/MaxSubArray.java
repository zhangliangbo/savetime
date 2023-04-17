package io.github.zhangliangbo.savetime.inner.problem;

/**
 * @author zhangliangbo
 * @since 2023/4/17
 */
public interface MaxSubArray {
    /**
     * 结果：[7, 10, 43]
     *
     * @return 样本数据
     */
    default int[] maxSubArraySample() {
        return new int[]{13, -3, -25, 20, -3, -16, -23, 18, 20, -7, 12, -5, -22, 15, -4, 7};
    }

    /**
     * @param nums 数组
     * @return 三元组[左索引，右索引，最大值]
     */
    int[] maxSubArray(int[] nums);
}
