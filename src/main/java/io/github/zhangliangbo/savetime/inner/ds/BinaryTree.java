package io.github.zhangliangbo.savetime.inner.ds;

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
public class BinaryTree {
    public static void main(String[] args) {
        BinaryTree binaryTree = new BinaryTree();
        binaryTree.inorderTreeWalk(BinaryData.data_12_1_a());
    }

    /**
     * 【递归】中序遍历
     *
     * @param x   根结点
     * @param <T> 卫星数据
     */
    public <T extends Comparable<T>> void inorderTreeWalk(BinaryNode<T> x) {
        if (x != null) {
            inorderTreeWalk(x.getLeft());
            System.out.println(x.getKey());
            inorderTreeWalk(x.getRight());
        }
    }

    /**
     * 【递归】查找
     *
     * @param x   根结点
     * @param k   关键字
     * @param <T> 卫星数据
     * @return 关键字的结点
     */
    public <T extends Comparable<T>> BinaryNode<T> treeSearch(BinaryNode<T> x, T k) {
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
     * @param x   根结点
     * @param k   关键字
     * @param <T> 卫星数据
     * @return 关键字的结点
     */
    public <T extends Comparable<T>> BinaryNode<T> iterativeTreeSearch(BinaryNode<T> x, T k) {
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
     * @param x   根结点
     * @param <T> 卫星数据
     * @return 最小结点
     */
    public <T extends Comparable<T>> BinaryNode<T> treeMinimum(BinaryNode<T> x) {
        while (x.getLeft() != null) {
            x = x.getLeft();
        }
        return x;
    }

    /**
     * 【迭代】最大结点
     *
     * @param x   根结点
     * @param <T> 卫星数据
     * @return 最大结点
     */
    public <T extends Comparable<T>> BinaryNode<T> treeMaximum(BinaryNode<T> x) {
        while (x.getRight() != null) {
            x = x.getRight();
        }
        return x;
    }

    /**
     * 【迭代】后继结点
     *
     * @param x   根结点
     * @param <T> 卫星数据
     * @return 后继结点
     */
    public <T extends Comparable<T>> BinaryNode<T> treeSuccessor(BinaryNode<T> x) {
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

}
