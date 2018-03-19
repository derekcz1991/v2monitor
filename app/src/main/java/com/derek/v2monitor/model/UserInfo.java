package com.derek.v2monitor.model;

import android.text.TextUtils;

/**
 * Created by derek on 2018/1/27.
 */

public class UserInfo {
    private String type;
    private String sfzmhm;
    private String gsmc;
    private String jbrndsjhm;
    private String sydjhm;
    private String beIdentityType;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSfzmhm() {
        return sfzmhm;
    }

    public void setSfzmhm(String sfzmhm) {
        this.sfzmhm = sfzmhm;
    }

    public String getGsmc() {
        return gsmc;
    }

    public void setGsmc(String gsmc) {
        this.gsmc = gsmc;
    }

    public String getJbrndsjhm() {
        return jbrndsjhm;
    }

    public void setJbrndsjhm(String jbrndsjhm) {
        this.jbrndsjhm = jbrndsjhm;
    }

    public String getSydjhm() {
        return sydjhm;
    }

    public void setSydjhm(String sydjhm) {
        this.sydjhm = sydjhm;
    }

    public String getBeIdentityType() {
        return beIdentityType;
    }

    public void setBeIdentityType(String beIdentityType) {
        this.beIdentityType = beIdentityType;
    }

    public boolean isValidate() {
        return !TextUtils.isEmpty(sfzmhm) && !TextUtils.isEmpty(gsmc) && !TextUtils.isEmpty(sydjhm);
    }

    @Override
    public String toString() {
        return "UserInfo{" +
            "证件号码='" + sfzmhm + '\'' +
            ", 姓名='" + gsmc + '\'' +
            ", 商号='" + sydjhm + '\'' +
            '}';
    }
}
