package com.derek.v2monitor.utils;


import com.derek.v2monitor.model.BookInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by derek on 2018/1/28.
 */

public class DealStrSubUtils {
    private static String TAG = "DealStrSubUtils";

    public static List<BookInfo> getTimeInfoList(String soap, String rgex) {
        List<String> list = getSubUtil(soap, rgex);
        List<BookInfo> timeInfoList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            BookInfo timeInfo = new BookInfo(list.get(i));
            timeInfoList.add(timeInfo);
            //Log.d(TAG, timeInfo.toString());
        }
        return timeInfoList;
    }

    /**
     * 正则表达式匹配两个指定字符串中间的内容
     *
     * @param soap
     * @return
     */
    public static List<String> getSubUtil(String soap, String rgex) {
        List<String> list = new ArrayList<>();
        Pattern pattern = Pattern.compile(rgex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while (m.find()) {
            int i = 1;
            list.add(m.group(i));
            i++;
        }
        return list;
    }

    /**
     * 返回单个字符串，若匹配到多个的话就返回第一个，方法与getSubUtil一样
     *
     * @param soap
     * @param rgex
     * @return
     */
    public static String getSubUtilSimple(String soap, String rgex) {
        Pattern pattern = Pattern.compile(rgex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while (m.find()) {
            return m.group(1);
        }
        return "";
    }
}
