package io.github.zhangliangbo.savetime.inner;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangliangbo
 * @since 2023/4/5
 */
public class StString {
    /**
     * 最长数字串
     *
     * @param line 输入字符串
     * @return 最长数字串+长度
     */
    public Pair<String, Integer> longestDigitString(String line) {
        char[] chars = line.toCharArray();

        Map<Integer, StringBuilder> map = new HashMap<>();

        int lastDigitPos = -1;
        int max = 0;
        for (int i = 0; i < chars.length; i++) {
            String one = null;
            if (Character.isDigit(chars[i])) {
                if (lastDigitPos == -1) {
                    lastDigitPos = i;
                }
            } else {
                if (lastDigitPos != -1) {
                    one = line.substring(lastDigitPos, i);
                    lastDigitPos = -1;
                }
            }
            if (i == chars.length - 1) {
                if (lastDigitPos != -1) {
                    one = line.substring(lastDigitPos);
                }
            }
            if (one == null) {
                continue;
            }
            int len = one.length();
            StringBuilder value = map.get(len);
            if (value == null) {
                value = new StringBuilder(one);
                map.put(len, value);
            } else {
                value.append(one);
            }
            max = Math.max(max, len);
        }
        StringBuilder value = map.get(max);
        return Pair.of(value.toString(), max);
    }

}
