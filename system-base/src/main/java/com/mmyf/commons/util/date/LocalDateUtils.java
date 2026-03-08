package com.mmyf.commons.util.date;

import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * package com.mmyf.commons.util.date
 * description: 日期时间类工具
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-03-06 23:08:40
 */
@UtilityClass
public class LocalDateUtils {
    /**
     * 指定年份的第一天
     *
     * @param year 年份
     */
    public static LocalDate firstDateOfYear(int year) {
        return LocalDate.ofYearDay(year, 1);
    }

    /**
     * 指定年份的最后一天(用指定年份的第一天加1年,再减去1天)
     *
     * @param year
     * @return
     */
    public static LocalDate lastDateOfYear(int year) {
        return firstDateOfYear(year).plusYears(1).minusDays(1);
    }

    /**
     * 指定年月的第一天
     *
     * @param year  年份
     * @param month 月份
     */
    public static LocalDate firstDateOfMonth(int year, int month) {
        return LocalDate.of(year, month, 1);
    }

    /**
     * 指定年月的最后一天(用指定年月的第一天加1月,再减去1天)
     *
     * @param year  年份
     * @param month 月份
     */
    public static LocalDate lastDateOfMonth(int year, int month) {
        return firstDateOfMonth(year, month).plusMonths(1).minusDays(1);
    }

    /**
     * 指定年份的第一秒
     *
     * @param year
     * @return
     */
    public static LocalDateTime firstTimeOfYear(int year) {
        return LocalDateTime.of(LocalDate.ofYearDay(year, 1), LocalTime.MIN);
    }

    /**
     * 指定年份的最后一秒(用指定年份的第一秒加1年,再减去1秒)
     *
     * @param year
     * @return
     */
    public static LocalDateTime lastTimeOfYear(int year) {
        return firstTimeOfYear(year).plusYears(1).minusSeconds(1);
    }

    /**
     * 指定年月的第一秒
     *
     * @param year  年份
     * @param month 月份
     */
    public static LocalDateTime firstTimeOfMonth(int year, int month) {
        return LocalDateTime.of(LocalDate.of(year, month, 1), LocalTime.MIN);
    }

    /**
     * 指定日期的第一秒
     *
     * @param date  日期
     */
    public static LocalDateTime firstTimeOfDate(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MIN);
    }

    /**
     * 指定日期的最后秒
     *
     * @param date  日期
     */
    public static LocalDateTime lastTimeOfDate(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MAX);
    }

    /**
     * 指定年月的最后一秒(用指定年月的第一秒加1月,再减去1秒)
     *
     * @param year  年份
     * @param month 月份
     */
    public static LocalDateTime lastTimeOfMonth(int year, int month) {
        return firstTimeOfMonth(year, month).plusMonths(1).minusSeconds(1);
    }

    /**
     * 判断时间是否在某个区间内    startInclude <=  target <  endExclude
     *
     * @param target 目标时间
     * @param startInclude 区间开始
     * @param endExclude 区间结束
     * @return boolean
     */
    public static boolean isBetweenTime(LocalTime target, String startInclude, String endExclude) {
        if (StringUtils.isBlank(startInclude) || StringUtils.isBlank(endExclude)) {
            return false;
        }
        return isBetweenTime(target, LocalTime.parse(startInclude), LocalTime.parse(endExclude));
    }

    /**
     * 判断时间是否在某个区间内    start <=  target <  end
     *
     * @param target 目标时间
     * @param startInclude 区间开始
     * @param endExclude 区间结束
     * @return boolean
     * @date 2024/3/15 下午1:59
     **/
    public static boolean isBetweenTime(LocalTime target, LocalTime startInclude, LocalTime endExclude) {
        if (target.isBefore(startInclude)) {
            return false;
        }
        if (target.isAfter(endExclude) || target.equals(endExclude)) {
            return false;
        }
        return true;
    }

    /**
     * 判断多个时间段是否有重叠
     * @param timeList 时间段
     * @return 是否有重叠
     */
    public static boolean isOverlapping(List<Pair<LocalTime, LocalTime>> timeList) {
        if (CollectionUtils.isEmpty(timeList) || timeList.size() == 1) {
            return false;
        }
        for(int i = 0; i < timeList.size() - 1; i++) {
            Pair<LocalTime, LocalTime> interval = timeList.get(i);
            LocalTime start = interval.getLeft();
            LocalTime end = interval.getRight();
            for (int j = i + 1; j < timeList.size(); j++) {
                Pair<LocalTime, LocalTime> target = timeList.get(j);
                // 判断 interval 的开始时间是否在 target 的开始时间和结束时间之间
                if (isBetweenTime(start, target.getLeft(), target.getRight())) {
                    return true;
                }
                // 判断 interval 的结束时间是否在 target 的开始时间和结束时间之间
                if (isBetweenTime(end , target.getLeft(), target.getRight()) && !end.equals(target.getLeft())) {
                    return true;
                }
                // 判断 target 的开始时间是否在 interval 的开始时间和结束时间之间
                if (isBetweenTime(target.getLeft(), start, end)) {
                    return true;
                }
                // 判断 target 的结束时间是否在 interval 的开始时间和结束时间之间
                if (isBetweenTime(target.getRight() , start, end)  && !target.getRight().equals(start)) {
                    return true;
                }
            }
        }
        return false;
    }
}
