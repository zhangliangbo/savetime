package io.github.zhangliangbo.savetime.inner;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;

/**
 * @author zhangliangbo
 * @since 2023/3/1
 */
public class Time {
    /**
     * 格式化时间
     *
     * @param date    日期
     * @param pattern 格式
     * @return 时间字符串
     */
    public String format(Date date, String pattern) {
        return DateFormatUtils.format(date, pattern);
    }

    /**
     * 格式化时间 yyyy-MM-dd HH:mm:ss
     *
     * @param date 日期
     * @return 时间字符串
     */
    public String format(Date date) {
        return format(date, "yyyy-MM-dd HH:mm:ss");
    }

}
