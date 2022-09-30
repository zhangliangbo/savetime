package io.github.zhangliangbo.savetime.inner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * @author zhangliangbo
 * @since 2022-09-30
 */
public class StCollection {
    public <T> Set<T> intersection(Set<T> s1, Set<T> s2) {
        return Sets.intersection(s1, s2);
    }
}
