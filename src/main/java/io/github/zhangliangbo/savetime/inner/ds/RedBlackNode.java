package io.github.zhangliangbo.savetime.inner.ds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 0黑1红
 *
 * @author zhangliangbo
 * @since 2023-05-25
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RedBlackNode<T extends Comparable<T>> {
    private int color;
    private T key;
    private RedBlackNode<T> left;
    private RedBlackNode<T> right;
    private RedBlackNode<T> p;

    public RedBlackNode(T key) {
        this.key = key;
        this.color = 1;
    }

}
