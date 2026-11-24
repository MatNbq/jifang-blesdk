package com.shenghao.blesdk.bean;


import com.shenghao.blesdk.okhttp.OkHttpBaseResp;

public class ACResp extends OkHttpBaseResp {
    private ACBean data;    //token

    public ACBean getData() {
        return data;
    }

    public void setData(ACBean data) {
        this.data = data;
    }
}
