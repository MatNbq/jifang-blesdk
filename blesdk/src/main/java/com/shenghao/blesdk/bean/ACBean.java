package com.shenghao.blesdk.bean;

import java.io.Serializable;

public class ACBean implements Serializable {
    private int fanLevel;//风机档位(0=关闭，1=1档，2=2档，3=3档)
    private int acMode;//空调模式(0=关闭，1=加热，2=制冷)
    private float interiorTemperature;

    public float getInteriorTemperature() {
        return interiorTemperature;
    }

    public void setInteriorTemperature(float interiorTemperature) {
        this.interiorTemperature = interiorTemperature;
    }

    public int getFanLevel() {
        return fanLevel;
    }

    public void setFanLevel(int fanLevel) {
        this.fanLevel = fanLevel;
    }

    public int getAcMode() {
        return acMode;
    }

    public void setAcMode(int acMode) {
        this.acMode = acMode;
    }
}
