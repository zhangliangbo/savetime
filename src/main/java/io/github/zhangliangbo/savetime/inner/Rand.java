package io.github.zhangliangbo.savetime.inner;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * @author zhangliangbo
 * @since 2022/12/13
 */
public class Rand {
    /**
     * 某月的随机一天
     *
     * @param localDate 某月
     * @return 随机一天
     */
    public LocalDate randDayOfMonth(LocalDate localDate) {
        LocalDate startDate = localDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endDate = localDate.with(TemporalAdjusters.lastDayOfMonth());
        int startDay = startDate.getDayOfMonth();
        int endDay = endDate.getDayOfMonth();
        int randDay = RandomUtils.nextInt(startDay, endDay + 1);
        return localDate.withDayOfMonth(randDay);
    }

    /**
     * 某月的随机一秒
     *
     * @param localDate 某月
     * @return 随机一秒
     */
    public LocalDateTime randSecondOfMonth(LocalDate localDate) {
        ZoneId zoneId = ZoneOffset.systemDefault();
        ZonedDateTime start = localDate.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(zoneId);
        ZonedDateTime end = start.plusMonths(1);
        long startSecond = start.toEpochSecond();
        long endSecond = end.toEpochSecond();
        long randSecond = RandomUtils.nextLong(startSecond, endSecond);
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(randSecond), zoneId);
    }

    /**
     * 随机数组的一个值
     *
     * @param a   数组
     * @param <T> 数组数据泛型
     * @return 一个数据
     */
    public <T> T randArray(T[] a) {
        if (ArrayUtils.isEmpty(a)) {
            return null;
        }
        int i = RandomUtils.nextInt(0, a.length);
        return a[i];
    }

    /**
     * 随机列表的一个值
     *
     * @param list 列表
     * @param <T>  列表数据泛型
     * @return 一个数据
     */
    public <T> T randList(List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        int i = RandomUtils.nextInt(0, list.size());
        return list.get(i);
    }

}
