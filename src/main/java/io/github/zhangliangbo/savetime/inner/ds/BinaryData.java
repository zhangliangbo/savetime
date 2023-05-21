package io.github.zhangliangbo.savetime.inner.ds;

/**
 * @author zhangliangbo
 * @since 2023/5/20
 */
public class BinaryData {
    public static BinaryNode<Integer> data_12_1_a() {
        BinaryNode<Integer> n2 = new BinaryNode<>(2);
        BinaryNode<Integer> n5_1 = new BinaryNode<>(5);
        BinaryNode<Integer> n5_2 = new BinaryNode<>(5);
        BinaryNode<Integer> n7 = new BinaryNode<>(7);
        BinaryNode<Integer> n8 = new BinaryNode<>(8);
        BinaryNode<Integer> n6 = new BinaryNode<>(6);

        n6.setLeft(n5_2);
        n6.setRight(n7);

        n5_2.setP(n6);
        n5_2.setLeft(n2);
        n5_2.setRight(n5_1);

        n2.setP(n5_2);
        n5_1.setP(n5_2);

        n7.setP(n6);
        n7.setRight(n8);

        n8.setP(n7);

        return n6;
    }
}
