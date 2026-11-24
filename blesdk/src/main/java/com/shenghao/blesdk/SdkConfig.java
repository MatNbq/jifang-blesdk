package com.shenghao.blesdk;

import android.content.Context;
import android.text.TextUtils;

import com.shenghao.blesdk.utils.AppUtils;

/**
 * SDK 全局配置中心。
 * 宿主在使用蓝牙 / 4G 车控能力前必须先调用 {@link #init(Context)}。
 * 4G 车控还需通过 {@link #setToken(String)} / {@link #setImei(String)} /
 * {@link #setTerminalNo(String)} 注入登录态与设备信息。
 */
public final class SdkConfig {

    private static Context appContext;
    private static String token;
    private static String imei;
    private static String terminalNo;
    private static String serverHost;
    private static boolean debug = true;

    private SdkConfig() {
    }

    /**
     * 初始化 SDK，必须在 Application 或首次使用前调用。
     */
    public static void init(Context context) {
        if (context == null) {
            return;
        }
        appContext = context.getApplicationContext();
    }

    public static Context getContext() {
        return appContext;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean debug) {
        SdkConfig.debug = debug;
    }

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        SdkConfig.token = token;
    }

    /**
     * 获取设备 imei；若未主动设置，则回退到本地生成并持久化的 imei（与原 App 行为一致）。
     */
    public static String getImei() {
        if (!TextUtils.isEmpty(imei)) {
            return imei;
        }
        return AppUtils.getImei();
    }

    public static void setImei(String imei) {
        SdkConfig.imei = imei;
    }

    /**
     * 当前设备号，用于蓝牙设备名过滤（isCurrentDevice）。
     */
    public static String getTerminalNo() {
        return terminalNo;
    }

    public static void setTerminalNo(String terminalNo) {
        SdkConfig.terminalNo = terminalNo;
    }

    /**
     * 4G 车控服务器地址；为空时使用 {@link com.shenghao.blesdk.http.Network} 默认地址。
     */
    public static String getServerHost() {
        return serverHost;
    }

    public static void setServerHost(String serverHost) {
        SdkConfig.serverHost = serverHost;
    }
}
