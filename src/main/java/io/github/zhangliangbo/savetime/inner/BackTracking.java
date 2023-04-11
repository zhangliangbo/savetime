package io.github.zhangliangbo.savetime.inner;

import java.util.LinkedList;
import java.util.List;

/**
 * @author zhangliangbo
 * @since 2023/4/11
 */
public class BackTracking {

    /**
     * @return 全排列
     * @see <a href="https://reference.wolfram.com/language/ref/Permutations.html">Permutations</a>
     */
    public <T> List<List<T>> permutation(T[] ts) {
        List<List<T>> res = new LinkedList<>();
        List<T> container = new LinkedList<>();
        recurse(ts, container, res);
        return res;
    }

    private <T> void recurse(T[] ts, List<T> container, List<List<T>> res) {
        if (container.size() == ts.length) {
            List<T> one = new LinkedList<>(container);
            res.add(one);
            return;
        }

        for (T t : ts) {
            if (container.contains(t)) {
                continue;
            }
            container.add(t);
            recurse(ts, container, res);
            int last = container.size() - 1;
            container.remove(last);
        }
    }

    public static void main(String[] args) {
        BackTracking recall = new BackTracking();
        List<List<Integer>> permutation = recall.permutation(new Integer[]{1, 2, 3});
        System.out.println(permutation);
    }

}
