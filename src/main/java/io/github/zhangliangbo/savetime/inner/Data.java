package io.github.zhangliangbo.savetime.inner;

import org.apache.commons.lang3.tuple.Triple;

/**
 * @author zhangliangbo
 * @since 2023/4/10
 */
public class Data {

    /**
     * 一棵普通的树
     */
    public Triple<Triple, String, Triple> tree() {
        Triple<Triple, String, Triple> b = Triple.of(null, "B", null);
        Triple<Triple, String, Triple> d = Triple.of(b, "D", null);
        Triple<Triple, String, Triple> a = Triple.of(null, "A", null);
        Triple<Triple, String, Triple> c = Triple.of(a, "C", d);
        Triple<Triple, String, Triple> m = Triple.of(null, "M", null);
        Triple<Triple, String, Triple> g = Triple.of(m, "G", null);
        Triple<Triple, String, Triple> h = Triple.of(null, "H", null);
        Triple<Triple, String, Triple> e = Triple.of(h, "E", g);
        return Triple.of(c, "F", e);
    }

}
