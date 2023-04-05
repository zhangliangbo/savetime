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

            if (pop.getRight() != null) {
                stack.push(pop.getRight());
            }

            if (pop.getLeft() != null) {
                stack.push(pop.getLeft());
            }

        }

    }

    public static void main(String[] args) {
        Triple<Triple, String, Triple> b = Triple.of(null, "B", null);
        Triple<Triple, String, Triple> d = Triple.of(b, "D", null);
        Triple<Triple, String, Triple> a = Triple.of(null, "A", null);
        Triple<Triple, String, Triple> c = Triple.of(a, "C", d);
        Triple<Triple, String, Triple> m = Triple.of(null, "M", null);
        Triple<Triple, String, Triple> g = Triple.of(m, "G", null);
        Triple<Triple, String, Triple> h = Triple.of(null, "H", null);
        Triple<Triple, String, Triple> e = Triple.of(h, "E", g);
        Triple<Triple, String, Triple> f = Triple.of(c, "F", e);
        new StStack().dfs(f, new Consumer<String>() {
            @Override
            public void accept(String s) {
                System.out.println(s);
            }
        });
    }

}
