package com.shenghao.blesdk.bean;


import com.shenghao.blesdk.okhttp.OkHttpBaseResp;

public class WinResp extends OkHttpBaseResp {
    private WinBean data;    //token

    public WinBean getData() {
        return data;
    }

    public void setData(WinBean data) {
        this.data = data;
    }
}
