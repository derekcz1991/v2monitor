package com.derek.v2monitor.model;

/**
 * Created by derek on 2018/3/19.
 */

public class BookInfo {
    private String id;
    private String date;
    private String time;

    public BookInfo(String info) {
        info = info.replace("(", "").replace(")", "").replace("'", "");
        String[] infos = info.split(",");
        id = infos[0];
        date = infos[1];
        time = infos[2];
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "BookInfo{" +
            "id='" + id + '\'' +
            ", date='" + date + '\'' +
            ", time='" + time + '\'' +
            '}';
    }

    public String print() {
        return "date='" + date + '\'' + ", time='" + time + '\'';
    }

}
