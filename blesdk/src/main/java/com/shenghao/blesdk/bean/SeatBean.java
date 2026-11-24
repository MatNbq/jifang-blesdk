package com.shenghao.blesdk.bean;

import java.io.Serializable;
import java.util.List;

public class SeatBean implements Serializable {
    private int rearHeatStatus;//1=开启/2=关闭状态
    private int frontHeatStatus;//前排1=开启/2=关闭状态
    private int rearHeatGear;
    private int frontHeatGear;

    public int getRearHeatStatus() {
        return rearHeatStatus;
    }

    public void setRearHeatStatus(int rearHeatStatus) {
        this.rearHeatStatus = rearHeatStatus;
    }

    public int getFrontHeatStatus() {
        return frontHeatStatus;
    }

    public void setFrontHeatStatus(int frontHeatStatus) {
        this.frontHeatStatus = frontHeatStatus;
    }

    public int getRearHeatGear() {
        return rearHeatGear;
    }

    public void setRearHeatGear(int rearHeatGear) {
        this.rearHeatGear = rearHeatGear;
    }

    public int getFrontHeatGear() {
        return frontHeatGear;
    }

    public void setFrontHeatGear(int frontHeatGear) {
        this.frontHeatGear = frontHeatGear;
    }
}
