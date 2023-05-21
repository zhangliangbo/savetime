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

    public static BinaryNode<Integer> data_12_1_b() {
        BinaryNode<Integer> n5_1 = new BinaryNode<>(5);
        BinaryNode<Integer> n6 = new BinaryNode<>(6);
        BinaryNode<Integer> n8 = new BinaryNode<>(8);
        BinaryNode<Integer> n7 = new BinaryNode<>(7);
        BinaryNode<Integer> n5_2 = new BinaryNode<>(5);
        BinaryNode<Integer> n2 = new BinaryNode<>(2);

        n2.setRight(n5_2);

        n5_2.setP(n2);
        n5_2.setRight(n7);

        n7.setP(n5_2);
        n7.setLeft(n6);
        n7.setRight(n8);

        n6.setP(n7);
        n6.setLeft(n5_1);

        return n2;
    }

    public static BinaryNode<Integer> data_12_2() {
        BinaryNode<Integer> n15 = new BinaryNode<>(15);
        BinaryNode<Integer> n6 = new BinaryNode<>(6);
        BinaryNode<Integer> n18 = new BinaryNode<>(18);
        BinaryNode<Integer> n3 = new BinaryNode<>(3);
        BinaryNode<Integer> n7 = new BinaryNode<>(7);
        BinaryNode<Integer> n17 = new BinaryNode<>(17);
        BinaryNode<Integer> n20 = new BinaryNode<>(20);
        BinaryNode<Integer> n2 = new BinaryNode<>(2);
        BinaryNode<Integer> n4 = new BinaryNode<>(4);
        BinaryNode<Integer> n13 = new BinaryNode<>(13);
        BinaryNode<Integer> n9 = new BinaryNode<>(9);

        n15.setLeft(n6);
        n15.setRight(n18);

        n6.setP(n15);
        n6.setLeft(n3);
        n6.setRight(n7);

        n18.setP(n15);
        n18.setLeft(n17);
        n18.setRight(n20);

        n3.setP(n6);
        n3.setLeft(n2);
        n3.setRight(n4);

        n7.setP(n6);
        n7.setRight(n13);

        n17.setP(n18);
        n20.setP(n18);

        n2.setP(n3);

        n4.setP(n3);

        n13.setP(n7);
        n13.setLeft(n9);

        n9.setP(n13);

        return n15;
    }
}
