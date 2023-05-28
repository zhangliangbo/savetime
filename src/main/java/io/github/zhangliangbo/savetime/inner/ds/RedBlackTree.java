package io.github.zhangliangbo.savetime.inner.ds;

import lombok.Getter;
import lombok.Setter;

/**
 * 红黑树
 * <p>
 * 性质：
 * 1. 每个结点或是红色的，或是黑色的
 * 2. 根结点是黑色的
 * 3. 每个叶节点是黑色的
 * 4. 如果一个结点是红色的，则它的两个子节点都是黑色的
 * 5. 对每个结点，从该结点到其所有后代叶结点的简单路径上，均包含相同数据的黑色结点。
 *
 * @author zhangliangbo
 * @since 2023/5/18
 */
@Setter
@Getter
public class RedBlackTree<T extends Comparable<T>> {
    public static void main(String[] args) {
        RedBlackNode<Integer> node = RedBlackData.data_13_3();
        System.out.println(node);
    }

    private RedBlackNode<T> root;

    private RedBlackNode<T> nil = null;

    public RedBlackTree(RedBlackNode<T> root) {
        this.root = root;
    }

    public void leftRotate(RedBlackTree<T> t, RedBlackNode<T> x) {
        RedBlackNode<T> y = x.getRight();

        x.setRight(y.getLeft());
        if (y.getLeft() != t.nil) {
            y.getLeft().setP(x);
        }

        y.setP(x.getP());
        if (x.getP() == t.nil) {
            t.setRoot(y);
        } else if (x == x.getP().getLeft()) {
            x.getP().setLeft(y);
        } else {
            x.getP().setRight(y);
        }

        y.setLeft(x);
        x.setP(y);
    }

    public void rightRotate(RedBlackTree<T> t, RedBlackNode<T> y) {
        RedBlackNode<T> x = y.getLeft();

        y.setLeft(x.getRight());
        if (x.getRight() != t.nil) {
            x.getRight().setP(y);
        }

        x.setP(y.getP());
        if (y.getP() == t.nil) {
            t.setRoot(x);
        } else if (y == y.getP().getLeft()) {
            y.getP().setLeft(x);
        } else {
            y.getP().setRight(x);
        }

        x.setRight(y);
        y.setP(x);
    }

}
