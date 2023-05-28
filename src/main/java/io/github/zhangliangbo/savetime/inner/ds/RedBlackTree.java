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
        RedBlackNode<Integer> root = RedBlackData.data_13_3();
        RedBlackTree<Integer> tree = new RedBlackTree<>(root);
        tree.inorderTreeWalk(tree.getRoot());
        RedBlackNode<Integer> n11 = tree.treeSearch(root, 11);
        tree.leftRotate(tree, n11);
        tree.inorderTreeWalk(tree.getRoot());
        RedBlackNode<Integer> n18 = tree.treeSearch(root, 18);
        tree.rightRotate(tree, n18);
        tree.inorderTreeWalk(tree.getRoot());
    }

    private RedBlackNode<T> root;

    private RedBlackNode<T> nil = null;

    public RedBlackTree(RedBlackNode<T> root) {
        this.root = root;
    }

    /**
     * 【递归】中序遍历
     *
     * @param x 根结点
     */
    public void inorderTreeWalk(RedBlackNode<T> x) {
        if (x != null) {
            inorderTreeWalk(x.getLeft());
            System.out.println(x.getKey());
            inorderTreeWalk(x.getRight());
        }
    }

    /**
     * 【递归】查找
     *
     * @param x 根结点
     * @param k 关键字
     * @return 关键字的结点
     */
    public RedBlackNode<T> treeSearch(RedBlackNode<T> x, T k) {
        if (x == null || k.compareTo(x.getKey()) == 0) {
            return x;
        }
        if (k.compareTo(x.getKey()) < 0) {
            return treeSearch(x.getLeft(), k);
        } else {
            return treeSearch(x.getRight(), k);
        }
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
