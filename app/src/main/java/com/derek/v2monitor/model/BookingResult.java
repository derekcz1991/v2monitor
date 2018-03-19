package com.derek.v2monitor.model;

/**
 * Created by derek on 29/01/2018.
 */

public class BookingResult {
    private String type;
    private String detail;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        return "BookingResult{" +
            "type='" + type + '\'' +
            ", detail='" + detail + '\'' +
            '}';
    }
}
