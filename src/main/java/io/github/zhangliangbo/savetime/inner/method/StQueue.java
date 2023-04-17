package io.github.zhangliangbo.savetime.inner.method;

import io.github.zhangliangbo.savetime.ST;
import org.apache.commons.lang3.tuple.Triple;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author zhangliangbo
 * @since 2023/4/10
 */
public class StQueue {

    /**
     * 树的广度优先遍历
     *
     * @param root 树的根
     * @param <V>  节点的值
     * @param <T>  节点
     * @return 右视图
     */
    public <V, T extends Triple<T, V, T>> List<V> bfs(Triple<T, V, T> root) {

        List<V> res = new LinkedList<>();

        Queue<Triple<T, V, T>> level = new LinkedList<>();

        level.offer(root);

        while (!level.isEmpty()) {
            Triple<T, V, T> poll = level.poll();
            res.add(poll.getMiddle());

            if (poll.getLeft() != null) {
                level.offer(poll.getLeft());
            }

            if (poll.getRight() != null) {
                level.offer(poll.getRight());
            }
        }

        return res;
    }

    /**
     * 树的右视图
     *
     * @param root 树的根
     * @param <V>  节点的值
     * @param <T>  节点
     * @return 右视图
     */
    public <V, T extends Triple<T, V, T>> List<V> rightSideView(Triple<T, V, T> root) {

        List<V> res = new LinkedList<>();

        //两层队列，第一层队列清空时，换上下一层队列，每层队列的最后一个节点，即为右视图
        Queue<Triple<T, V, T>> currentLevel = new LinkedList<>();
        Queue<Triple<T, V, T>> nextLevel = new LinkedList<>();

        currentLevel.offer(root);

        while (!currentLevel.isEmpty()) {
            Triple<T, V, T> poll = currentLevel.poll();

            if (poll.getLeft() != null) {
                nextLevel.offer(poll.getLeft());
            }

            if (poll.getRight() != null) {
                nextLevel.offer(poll.getRight());
            }

            if (currentLevel.isEmpty()) {
                res.add(poll.getMiddle());

                currentLevel = nextLevel;
                nextLevel = new LinkedList<>();
            }

        }

        return res;
    }


    /**
     * 树的右视图
     *
     * @param root 树的根
     * @param <V>  节点的值
     * @param <T>  节点
     * @return 右视图
     */
    public <V, T extends Triple<T, V, T>> List<V> leftSideView(Triple<T, V, T> root) {

        List<V> res = new LinkedList<>();

        //两层队列，第一层队列清空时，换上下一层队列，每层队列的第一个节点，即为左视图
        Queue<Triple<T, V, T>> currentLevel = new LinkedList<>();
        Queue<Triple<T, V, T>> nextLevel = new LinkedList<>();

        currentLevel.offer(root);
        res.add(root.getMiddle());

        while (!currentLevel.isEmpty()) {
            Triple<T, V, T> poll = currentLevel.poll();

            if (poll.getLeft() != null) {
                nextLevel.offer(poll.getLeft());
            }

            if (poll.getRight() != null) {
                nextLevel.offer(poll.getRight());
            }

            if (currentLevel.isEmpty()) {
                //取出下一层的第一个
                if (!nextLevel.isEmpty()) {
                    Triple<T, V, T> peek = nextLevel.peek();
                    res.add(peek.getMiddle());
                }
                currentLevel = nextLevel;
                nextLevel = new LinkedList<>();
            }

        }

        return res;
    }

    public static void main(String[] args) {
        Triple<Triple, String, Triple> tree = ST.data.tree();
        System.out.println(ST.queue.bfs(tree));
        System.out.println(ST.queue.leftSideView(tree));
        System.out.println(ST.queue.rightSideView(tree));
    }

}
