package com.shenghao.blesdk;

import android.text.TextUtils;

import com.shenghao.blesdk.okhttp.OkHttpHelper;
import com.shenghao.blesdk.okhttp.OkHttpMethod;
import com.shenghao.blesdk.okhttp.OkHttpRequestParams;
import com.shenghao.blesdk.okhttp.OkHttpResultCallBack;
import com.shenghao.blesdk.okhttp.OkHttpURLs;

/**
 * 4G 车控能力入口（与 {@link BleSdk} 彼此独立，同属一个 SDK）。
 * <p>
 * 使用前请先通过 {@link SdkConfig#init(android.content.Context)} 初始化，
 * 并注入登录态：{@link SdkConfig#setToken(String)} / {@link SdkConfig#setImei(String)}。
 * 所有异步结果通过 {@link OkHttpResultCallBack} 回调。
 */
public final class CarControlSdk {

    private CarControlSdk() {
    }

    /**
     * 配置 4G 车控运行参数（等价于分别调用 SdkConfig 的 setter）。
     */
    public static void config(String token, String imei, String terminalNo, String serverHost) {
        SdkConfig.setToken(token);
        SdkConfig.setImei(imei);
        SdkConfig.setTerminalNo(terminalNo);
        SdkConfig.setServerHost(serverHost);
    }

    // ====================== 车控信息 ======================

    /**
     * 查询车控信息（车门/车窗/灯光/空调等状态）。
     */
    public static void getCarControlInfo(String terminalNo, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        requestParams.addBody("terminalNo", terminalNo);
        String url = OkHttpURLs.getCarControlInfo();
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_GET, url, null, requestParams, callBack);
    }

    /**
     * 设置车控信息（旧版 setCarControl 接口）。
     */
    public static void setCarControl(String terminalNo, String type, String status, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        requestParams.addBody("terminalNo", terminalNo);
        requestParams.addBody("type", type);
        requestParams.addBody("status", status);
        String url = OkHttpURLs.setCarControl();
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_GET, url, null, requestParams, callBack);
    }

    // ====================== airControl / airControl2 车控 ======================

    /**
     * airControl 车控（字符串参数版本，与原项目保持一致）。
     * type: 06-尾门开关 07-空调开关 0B-车窗开关 21-制冷制热开关 27-灯光控制 9-寻车
     */
    public static void airControl(String terminalNo, String type, String param, String angle, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        String url = OkHttpURLs.oneKey();
        requestParams.addBody("terminalNo", terminalNo);
        requestParams.addBody("type", type + "");
        requestParams.addBody("param", param + "");
        if (!TextUtils.isEmpty(angle)) {
            requestParams.addBody("angle", angle);
        }
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_POST, url, null, requestParams, callBack);
    }

    /**
     * airControl 车控（int 参数版本）。
     */
    public static void airControl(String terminalNo, int type, int param, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        String url = OkHttpURLs.oneKey();
        requestParams.addBody("terminalNo", terminalNo);
        requestParams.addBody("type", type + "");
        requestParams.addBody("param", param + "");
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_POST, url, null, requestParams, callBack);
    }

    /**
     * airControl2 车控（带角度参数）。
     */
    public static void airControl2(String terminalNo, int type, int param, int angle, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        String url = OkHttpURLs.oneKey();
        requestParams.addBody("terminalNo", terminalNo);
        requestParams.addBody("type", type + "");
        requestParams.addBody("param", param + "");
        requestParams.addBody("angle", angle + "");
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_POST, url, null, requestParams, callBack);
    }

    // ====================== 空调 ======================

    /**
     * 查询空调状态。
     */
    public static void getAC(String terminalNo, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        String url = OkHttpURLs.getAC();
        requestParams.addBody("terminalNo", terminalNo);
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_GET, url, null, requestParams, callBack);
    }

    /**
     * 空调档位调节。
     * status: 3-制热3档 2-制热2档 1-制热1档 0-关闭 -1-制冷1档 -2-制冷2档 -3-制冷3档
     */
    public static void adjust(String terminalNo, int status, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        String url = OkHttpURLs.adjust();
        requestParams.addBody("terminalNo", terminalNo);
        requestParams.addBody("status", status + "");
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_POST, url, null, requestParams, callBack);
    }

    /**
     * 空调温度加减。
     * status: 温度加=1 / 温度减=0
     */
    public static void adjustTemperature(String terminalNo, int status, String temperature, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        String url = OkHttpURLs.adjustTemperature();
        requestParams.addBody("terminalNo", terminalNo);
        requestParams.addBody("status", status + "");
        requestParams.addBody("temperature", temperature);
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_POST, url, null, requestParams, callBack);
    }

    // ====================== 座椅 / 车窗 ======================

    /**
     * 查询座椅状态。
     */
    public static void getSeat(String terminalNo, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        String url = OkHttpURLs.getSeat();
        requestParams.addBody("terminalNo", terminalNo);
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_GET, url, null, requestParams, callBack);
    }

    /**
     * 查询车窗状态。
     */
    public static void getCarWindow(String terminalNo, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        String url = OkHttpURLs.getCarWindow();
        requestParams.addBody("terminalNo", terminalNo);
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_GET, url, null, requestParams, callBack);
    }

    // ====================== 其他车控 ======================

    /**
     * 断油电 / 恢复油电。
     */
    public static void changePowerControl(String terminalNo, int command, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        requestParams.addBody("terminalNo", terminalNo);
        requestParams.addBody("command", String.valueOf(command));
        String url = OkHttpURLs.changePowerControl();
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_POST, url, null, requestParams, callBack);
    }

    /**
     * 防狼模式。
     * param: 0-关 1-开
     */
    public static void antiWolf(String terminalNo, int param, OkHttpResultCallBack callBack) {
        OkHttpRequestParams requestParams = new OkHttpRequestParams();
        String url = OkHttpURLs.antiWolf();
        requestParams.addBody("terminalNo", terminalNo);
        requestParams.addBody("param", param + "");
        OkHttpHelper.getInstance()
                .enqueueAll(OkHttpMethod.M_POST, url, null, requestParams, callBack);
    }

    // ====================== 通用透传（备用） ======================

    /**
     * 通用 GET 请求透传，便于扩展未封装的车控接口。
     */
    public static void get(String url, OkHttpRequestParams params, OkHttpResultCallBack callBack) {
        OkHttpHelper.getInstance().enqueueAll(OkHttpMethod.M_GET, url, null, params, callBack);
    }

    /**
     * 通用 POST 请求透传，便于扩展未封装的车控接口。
     */
    public static void post(String url, OkHttpRequestParams params, OkHttpResultCallBack callBack) {
        OkHttpHelper.getInstance().enqueueAll(OkHttpMethod.M_POST, url, null, params, callBack);
    }
}
