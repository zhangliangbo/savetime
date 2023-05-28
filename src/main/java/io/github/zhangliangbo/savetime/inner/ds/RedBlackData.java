package io.github.zhangliangbo.savetime.inner.ds;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangliangbo
 * @since 2023-05-27
 */
public class RedBlackData {

    private static RedBlackNode<Integer> fromMap(Map<Integer, Integer> map) {
        Map<Integer, RedBlackNode<Integer>> nodeMap = new HashMap<>();

        int length = Integer.MIN_VALUE;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            Integer index = entry.getKey();
            Integer value = entry.getValue();

            if (index > length) {
                length = index;
            }

            nodeMap.put(index, new RedBlackNode<>(value));
        }

        RedBlackNode<Integer> root = nodeMap.get(1);
        for (int i = 0; (i << 1) + 1 < length + 1; i++) {

            RedBlackNode<Integer> node = nodeMap.get(i);
            int leftIndex = i << 1;
            int rightIndex = (i << 1) + 1;
            RedBlackNode<Integer> left = nodeMap.get(leftIndex);
            RedBlackNode<Integer> right = nodeMap.get(rightIndex);

            if (node != null) {
                node.setLeft(left);
                node.setRight(right);
            }
            if (left != null) {
                left.setP(node);
            }
            if (right != null) {
                right.setP(node);
            }

        }
        return root;
    }

    public static RedBlackNode<Integer> data_13_3() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 7);
        map.put(2, 4);
        map.put(3, 11);
        map.put(4, 3);
        map.put(5, 6);
        map.put(6, 9);
        map.put(7, 18);
        map.put(8, 2);
        map.put(14, 14);
        map.put(15, 19);
        map.put(15, 19);
        return fromMap(map);
    }

}
