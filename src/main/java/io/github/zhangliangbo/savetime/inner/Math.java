package io.github.zhangliangbo.savetime.inner;

/**
 * @author zhangliangbo
 * @since 2023/4/5
 */
public class Math {
    /**
     * 立方根是个单调递增函数
     *
     * @param num 输入数字
     * @return 立方根
     */
    public double cubeRoot(double num) {
        double left = java.lang.Math.min(num, -1);
        double right = java.lang.Math.max(1, num);

        double res = num;
        while (right - left > 10e-6) {
            double mid = (left + right) / 2.0;
            double mid3 = mid * mid * mid;
            if (mid3 > num) {
                right = mid;
            } else if (mid3 < num) {
                left = mid;
            } else {
                return mid;
            }
            res = mid;
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println(new Math().cubeRoot(19.9));
    }
}
