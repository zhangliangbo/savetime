package io.github.zhangliangbo.savetime.inner.ds;

import lombok.Getter;
import lombok.Setter;

/**
 * 二叉搜索树
 * <p>
 * 性质：
 * 设x是二叉搜索树中的一个结点。
 * 如果y是x左子树中的一个结点，那么y.key<=x.key。
 * 如果y是x右子树中的一个结点，那么y.key>=x.key。
 *
 * @author zhangliangbo
 * @since 2023/5/18
 */
@Setter
@Getter
public class BinaryTree<T extends Comparable<T>> {
    public static void main(String[] args) {
        BinaryNode<Integer> root = BinaryData.data_12_3();
        BinaryTree<Integer> binaryTree = new BinaryTree<>(root);
        binaryTree.treeInsert(binaryTree, new BinaryNode<>(13));
        binaryTree.inorderTreeWalk(binaryTree.getRoot());
    }

    private BinaryNode<T> root;

    public BinaryTree(BinaryNode<T> root) {
        this.root = root;
    }

    /**
     * 【递归】中序遍历
     *
     * @param x 根结点
     */
    public void inorderTreeWalk(BinaryNode<T> x) {
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
    public BinaryNode<T> treeSearch(BinaryNode<T> x, T k) {
        if (x == null || k.compareTo(x.getKey()) == 0) {
            return x;
        }
        if (k.compareTo(x.getKey()) < 0) {
            return treeSearch(x.getLeft(), k);
        } else {
            return treeSearch(x.getRight(), k);
        }
    }

    /**
     * 【迭代】查找
     *
     * @param x 根结点
     * @param k 关键字
     * @return 关键字的结点
     */
    public BinaryNode<T> iterativeTreeSearch(BinaryNode<T> x, T k) {
        while (x != null && k.compareTo(x.getKey()) != 0) {
            if (k.compareTo(x.getKey()) < 0) {
                x = x.getLeft();
            } else {
                x = x.getRight();
            }
        }
        return x;
    }

    /**
     * 【迭代】最小结点
     *
     * @param x 根结点
     * @return 最小结点
     */
    public BinaryNode<T> treeMinimum(BinaryNode<T> x) {
        while (x.getLeft() != null) {
            x = x.getLeft();
        }
        return x;
    }

    /**
     * 【迭代】最大结点
     *
     * @param x 根结点
     * @return 最大结点
     */
    public BinaryNode<T> treeMaximum(BinaryNode<T> x) {
        while (x.getRight() != null) {
            x = x.getRight();
        }
        return x;
    }

    /**
     * 【迭代】后继结点
     *
     * @param x 任意一个结点
     * @return 后继结点
     */
    public BinaryNode<T> treeSuccessor(BinaryNode<T> x) {
        if (x.getRight() != null) {
            return treeMinimum(x.getRight());
        }
        BinaryNode<T> y = x.getP();
        while (y != null && x == y.getRight()) {
            x = y;
            y = y.getP();
        }
        return y;
    }

    /**
     * 【迭代】前驱结点
     *
     * @param x 任意一个结点
     * @return 前驱结点
     */
    public BinaryNode<T> treePredecessor(BinaryNode<T> x) {
        if (x.getLeft() != null) {
            return treeMaximum(x.getLeft());
        }
        BinaryNode<T> y = x.getP();
        while (y != null && x == y.getLeft()) {
            x = y;
            y = y.getP();
        }
        return y;
    }

    /**
     * 插入
     *
     * @param t 树
     * @param z 待插入结点
     */
    public void treeInsert(BinaryTree<T> t, BinaryNode<T> z) {
        BinaryNode<T> y = null;

        BinaryNode<T> x = t.getRoot();

        while (x != null) {
            y = x;
            if (z.getKey().compareTo(x.getKey()) < 0) {
                x = x.getLeft();
            } else {
                x = x.getRight();
            }
        }
        z.setP(y);
        if (y == null) {
            t.setRoot(z);
        } else if (z.getKey().compareTo(y.getKey()) < 0) {
            y.setLeft(z);
        } else {
            y.setRight(z);
        }
    }

}
