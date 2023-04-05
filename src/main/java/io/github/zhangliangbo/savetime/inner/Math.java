package io.github.zhangliangbo.savetime.inner;

/**
 * @author zhangliangbo
 * @since 2023/4/5
 */
public class Math {
    /**
     * 立方根是个单调递增函数
     *
     * @param d 输入数字
     * @return 立方根
     * @see ./png/inner/Math/cubeRoot.png
     */
    public double cubeRoot(double d) {
        double left = java.lang.Math.min(d, -1);
        double right = java.lang.Math.max(1, d);

        double res = d;
        while (java.lang.Math.abs(right - left) > 10e-6) {
            double mid = (left + right) / 2.0;
            if (mid * mid * mid > d) {
                right = mid;
            } else {
                left = mid;
            }
            res = mid;
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println(new Math().cubeRoot(2.7));
    }
}
