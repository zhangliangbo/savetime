package io.github.zhangliangbo.savetime.inner;

import io.github.zhangliangbo.savetime.ST;
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

    public <V, T extends Triple<T, V, T>> void dfsPost(Triple<T, V, T> root, Consumer<V> consumer) {
        Stack<Triple<T, V, T>> stack = new Stack<>();
        Triple<T, V, T> current = root;
        Triple<T, V, T> prevAccess = null;
        while (current != null || !stack.isEmpty()) {
            while (current != null) {
                stack.push(current);
                current = current.getLeft();
            }
            current = stack.peek();
            if (current.getRight() == null || current.getRight() == prevAccess) {
                current = stack.pop();
                consumer.accept(current.getMiddle());
                prevAccess = current;
                current = null;
            } else {
                current = current.getRight();
            }
        }
    }

    public static void main(String[] args) {
        Triple<Triple, String, Triple> tree = ST.data.tree();
        StStack stStack = new StStack();
        System.out.println("pre");
        stStack.dfsPre(tree, System.out::println);
        System.out.println("in");
        stStack.dfsIn(tree, System.out::println);
        System.out.println("post");
        stStack.dfsPost(tree, System.out::println);
    }

}
