package io.github.zhangliangbo.savetime.inner;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zhangliangbo
 * @since 2022-09-30
 */
public class StCollection {
    /**
     * 交集
     *
     * @param s1  集合1
     * @param s2  集合2
     * @param <T> 元素
     * @return 交集
     */
    @SafeVarargs
    public final <T> Set<T> intersection(Collection<T> s1, Collection<T> s2, Collection<T>... so) {
        Sets.SetView<T> result = Sets.intersection(new LinkedHashSet<>(s1), new LinkedHashSet<>(s2));
        for (Collection<T> ts : so) {
            result = Sets.intersection(result, new LinkedHashSet<>(ts));
        }
        return result;
    }

    /**
     * 并集
     *
     * @param s1  集合1
     * @param s2  集合2
     * @param <T> 元素
     * @return 交集
     */
    @SafeVarargs
    public final <T> Set<T> union(Collection<T> s1, Collection<T> s2, Collection<T>... so) {
        Sets.SetView<T> result = Sets.union(new LinkedHashSet<>(s1), new LinkedHashSet<>(s2));
        for (Collection<T> ts : so) {
            result = Sets.union(result, new LinkedHashSet<>(ts));
        }
        return result;
    }

    /**
     * 补集
     *
     * @param all 母集
     * @param s1  集合1
     * @param <T> 元素
     * @return 补集
     */
    @SafeVarargs
    public final <T> Set<T> complement(Collection<T> all, Collection<T> s1, Collection<T>... so) {
        Sets.SetView<T> result = Sets.difference(new LinkedHashSet<>(all), new LinkedHashSet<>(s1));
        for (Collection<T> ts : so) {
            result = Sets.difference(result, new LinkedHashSet<>(ts));
        }
        return result;
    }

    /**
     * 子集
     *
     * @param s   母集
     * @param <T> 元素
     * @return 子集
     */
    public <T> Set<Set<T>> subsets(Collection<T> s) {
        return Sets.powerSet(new LinkedHashSet<>(s));
    }

    /**
     * 删除重复元素
     *
     * @param c   集合
     * @param <T> 元素
     * @return 不重复集合
     */
    public <T> Set<T> deleteDuplicates(Collection<T> c) {
        return new LinkedHashSet<>(c);
    }
}
