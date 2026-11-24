package com.shenghao.blesdk.bean;


import com.shenghao.blesdk.okhttp.OkHttpBaseResp;

public class ControlInfoResp extends OkHttpBaseResp {
    private ControlInfo data;

    public ControlInfo getData() {
        return data;
    }

    public void setData(ControlInfo data) {
        this.data = data;
    }
}
