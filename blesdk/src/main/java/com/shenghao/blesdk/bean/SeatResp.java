package com.shenghao.blesdk.bean;


import com.shenghao.blesdk.okhttp.OkHttpBaseResp;

import java.util.List;

public class SeatResp extends OkHttpBaseResp {
    private SeatBean data;    //token

    public SeatBean getData() {
        return data;
    }

    public void setData(SeatBean data) {
        this.data = data;
    }
}
