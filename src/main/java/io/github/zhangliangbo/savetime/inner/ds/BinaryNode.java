package io.github.zhangliangbo.savetime.inner.ds;

import lombok.*;

/**
 * @author zhangliangbo
 * @since 2023/5/20
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BinaryNode<T extends Comparable<T>> {
    private T key;
    private BinaryNode<T> left;
    private BinaryNode<T> right;
    private BinaryNode<T> p;

    public BinaryNode(T key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "BinaryNode{" +
                "key=" + key +
                '}';
    }

}
