package com.mmyf.commons.util.lang;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期工具类
 *
 * @author Teddy
 * @version 1.0.0
 * @date 2019-08-07 11:09
 */
@UtilityClass
@Slf4j
public class DateUtils {

    private static final String DATE_STRING = "yyyy-MM-dd";
    private static final String TIME_STRING = "HH:mm:ss";
    private static final String DATE_TIME_STRING = "yyyy-MM-dd HH:mm:ss";


    /**
     * 格式化日期时间
     *
     * @param date date
     * @return date time str
     */
    public static String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return DateFormatUtils.format(date, DATE_TIME_STRING);
    }


    /**
     * 由开始日期转换为开始时间，一般在以时间为条件的查询中使用
     *
     * @param date date
     * @return date time 2019-05-06 00:00:00
     */
    public static Date date2StartTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    /**
     * 由结束日期转换为结束时间，一般在以时间为条件的查询中使用
     *
     * @param date date
     * @return date time  2019-05-06 23:59:59
     */
    public static Date date2EndTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTime();
    }

    public static String formatTime(Long ms) {
        Integer ss = 1000;
        Integer mi = ss * 60;
        Integer hh = mi * 60;
        Integer dd = hh * 24;

        Long day = ms / dd;
        Long hour = (ms - day * dd) / hh;
        Long minute = (ms - day * dd - hour * hh) / mi;
        Long second = (ms - day * dd - hour * hh - minute * mi) / ss;
        Long milliSecond = ms - day * dd - hour * hh - minute * mi - second * ss;

        StringBuilder sb = new StringBuilder();
        if (day > 0) {
            sb.append(day).append("天");
        }
        if (hour > 0) {
            sb.append(hour).append("小时");
        }
        if (minute > 0) {
            sb.append(minute).append("分");
        }
        if (second > 0) {
            sb.append(second).append("秒");
        }
        if (milliSecond > 0) {
            sb.append(milliSecond).append("毫秒");
        }
        return sb.toString();
    }

    public Date parseDate(String dateTime) {
        try {
            return org.apache.commons.lang3.time.DateUtils.parseDate(dateTime, DATE_TIME_STRING);
        } catch (ParseException e) {
            log.error("解析日期错误：{}", dateTime);
            return null;
        }
    }
}
