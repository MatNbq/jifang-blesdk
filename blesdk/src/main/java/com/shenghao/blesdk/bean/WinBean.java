package com.shenghao.blesdk.bean;

import java.io.Serializable;

public class WinBean implements Serializable {
    private int rightFrontWindow;//0=Default，1=关闭，2=开启
    private int leftFrontWindow;//

    public int getRightFrontWindow() {
        return rightFrontWindow;
    }

    public void setRightFrontWindow(int rightFrontWindow) {
        this.rightFrontWindow = rightFrontWindow;
    }

    public int getLeftFrontWindow() {
        return leftFrontWindow;
    }

    public void setLeftFrontWindow(int leftFrontWindow) {
        this.leftFrontWindow = leftFrontWindow;
    }
}
