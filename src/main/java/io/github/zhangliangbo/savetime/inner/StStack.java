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
    public <V, T extends Triple<T, V, T>> void dfsPre(Triple<T, V, T> root, Consumer<V> consumer) {
        Stack<Triple<T, V, T>> stack = new Stack<>();
        Triple<T, V, T> current = root;
        while (current != null || !stack.isEmpty()) {
            while (current != null) {
                consumer.accept(current.getMiddle());
                stack.push(current);
                current = current.getLeft();
            }
            current = stack.pop();
            current = current.getRight();
        }

    }

    public <V, T extends Triple<T, V, T>> void dfsIn(Triple<T, V, T> root, Consumer<V> consumer) {
        Stack<Triple<T, V, T>> stack = new Stack<>();
        Triple<T, V, T> current = root;
        while (current != null || !stack.isEmpty()) {
            while (current != null) {
                stack.push(current);
                current = current.getLeft();
            }
            current = stack.pop();
            consumer.accept(current.getMiddle());
            current = current.getRight();
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
        StStack stStack = new StStack();
        System.out.println("pre");
        stStack.dfsPre(f, System.out::println);
        System.out.println("in");
        stStack.dfsIn(f, System.out::println);
    }

}
