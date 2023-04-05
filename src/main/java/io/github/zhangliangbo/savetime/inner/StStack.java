package io.github.zhangliangbo.savetime.inner;

import org.apache.commons.lang3.tuple.Triple;

import java.util.Stack;
import java.util.function.Consumer;

/**
 * @author zhangliangbo
 * @since 2023/4/5
 */
public class StStack {
    /**
     * 深度优先遍历-前序遍历
     *
     * @param root     根节点
     * @param consumer 节点值处理器
     * @param <V>      节点值
     * @param <T>      节点
     */
    public <V, T extends Triple<T, V, T>> void dfs(Triple<T, V, T> root, Consumer<V> consumer) {
        Stack<Triple<T, V, T>> stack = new Stack<>();

        stack.push(root);

        while (!stack.isEmpty()) {

            Triple<T, V, T> pop = stack.pop();

            consumer.accept(pop.getMiddle());

            if (pop.getLeft() != null) {
                stack.push(pop.getLeft());
            }

            if (pop.getRight() != null) {
                stack.push(pop.getRight());
            }

        }

    }
}
