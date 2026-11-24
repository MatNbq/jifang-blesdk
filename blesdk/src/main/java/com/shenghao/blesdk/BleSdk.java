package com.shenghao.blesdk;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.shenghao.blesdk.ble.BleConstant;
import com.shenghao.blesdk.ble.BlueRssi;
import com.shenghao.blesdk.ble.BlueRssiPke;
import com.shenghao.blesdk.ble.BluetoothUtils;
import com.shenghao.blesdk.ble.KeyGenerator;
import com.shenghao.blesdk.ble.ScanManager;
import com.shenghao.blesdk.ble.command.CommandUtils;
import com.shenghao.blesdk.utils.ByteUtils;
import com.shenghao.blesdk.utils.SPUtils;
import com.shenghao.blesdk.utils.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 蓝牙能力入口（与 {@link CarControlSdk} 彼此独立，同属一个 SDK）。
 * <p>
 * 使用前请先调用 {@link #init(Context)}。
 * 扫描结果 / 连接状态通过 FastBle 回调（{@link BleScanCallback} / {@link BleGattCallback}）返回，
 * 也可通过 EventBus 订阅 {@link com.shenghao.blesdk.event.BleStatusEvent} 接收连接状态变化。
 */
public final class BleSdk {

    private static BluetoothUtils bluetoothUtils;

    private BleSdk() {
    }

    /**
     * 初始化蓝牙 SDK。必须在使用蓝牙能力前调用（建议在 Application.onCreate 中）。
     */
    public static synchronized void init(Context context) {
        if (context == null) {
            return;
        }
        SdkConfig.init(context);
        BleManager.getInstance().init((Application) SdkConfig.getContext());
    }

    /**
     * 释放资源。
     */
    public static void destroy() {
        try {
            stopScan();
        } catch (Exception ignored) {
        }
        BleManager.getInstance().destroy();
    }

    // ====================== 扫描 ======================

    /**
     * 开始扫描（内部走 ScanManager，带等待队列与自动停止）。
     */
    public static void scan(BleScanCallback callback) {
        ScanManager.getInstance().startScan(callback);
    }

    /**
     * 停止扫描。
     */
    public static void stopScan() {
        ScanManager.getInstance().stopScan();
    }

    public static boolean isScanning() {
        return ScanManager.getInstance().isScanning();
    }

    // ====================== 连接 / 断开 ======================

    /**
     * 连接指定 mac 的设备。
     */
    public static void connect(String mac, BleGattCallback callback) {
        BleManager.getInstance().connect(mac, callback);
    }

    public static void connect(BleDevice device, BleGattCallback callback) {
        BleManager.getInstance().connect(device, callback);
    }

    public static void disconnect(BleDevice device) {
        BleManager.getInstance().disconnect(device);
    }

    public static void disconnectAll() {
        BleManager.getInstance().disconnectAllDevice();
    }

    public static boolean isConnected(String mac) {
        return BleManager.getInstance().isConnected(mac);
    }

    public static boolean isConnected(BleDevice device) {
        return BleManager.getInstance().isConnected(device);
    }

    public static List<BleDevice> getAllConnectedDevice() {
        return BleManager.getInstance().getAllConnectedDevice();
    }

    // ====================== 通知 / 写数据（挪车、寻车等指令） ======================

    public static void notify(BleDevice device, BleNotifyCallback callback) {
        BleManager.getInstance().notify(device,
                BleConstant.SERVICE_UUID_SH, BleConstant.NOTIFY_UUID_SH, callback);
    }

    public static void stopNotify(BleDevice device) {
        BleManager.getInstance().stopNotify(device,
                BleConstant.SERVICE_UUID_SH, BleConstant.NOTIFY_UUID_SH);
    }

    /**
     * 写指令（带响应）。挪车 / 寻车 / 设防等指令均可通过此方法下发。
     */
    public static void write(BleDevice device, byte[] data, BleWriteCallback callback) {
        BleManager.getInstance().write(device,
                BleConstant.SERVICE_UUID_SH, BleConstant.WRITE_UUID_SH, data, callback);
    }

    // ====================== RSSI 设置（AT 指令） ======================

    /**
     * 设置普通版 RSSI 上下限（AT+LRSSI / AT+ULRSSI）。
     */
    public static void setRssi(BleDevice device, BlueRssi rssi, BleWriteCallback callback) {
        if (rssi == null) {
            return;
        }
        write(device, (BleConstant.LIMIT_STR_DISCONNECT + rssi.getValue()).getBytes(), callback);
    }

    /**
     * 设置 PKE 版 RSSI 上下限。
     */
    public static void setRssiPke(BleDevice device, BlueRssiPke rssi, BleWriteCallback callback) {
        if (rssi == null) {
            return;
        }
        write(device, (BleConstant.LIMIT_STR_DISCONNECT + rssi.getValue()).getBytes(), callback);
    }

    /**
     * 下发任意 AT 文本指令（如 "AT+RSSI?"）。
     */
    public static void sendAtCommand(BleDevice device, String cmd, BleWriteCallback callback) {
        if (cmd == null) {
            return;
        }
        write(device, cmd.getBytes(), callback);
    }

    // ====================== 蓝牙挪车（一键挪车 / 自动泊车，CAN 协议新蓝牙） ======================

    /** 挪车方向：停止 */
    public static final int MOVE_DIR_STOP = 0;
    /** 挪车方向：前进 */
    public static final int MOVE_DIR_FORWARD = 1;
    /** 挪车方向：后退 */
    public static final int MOVE_DIR_BACKWARD = 2;

    private static volatile long sLastMoveTime = 0L;
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    /**
     * 生成挪车指令（flag=0x00 不加密，code=0x20）。
     * 与源项目 {@code OneKeyActivity2.generateAutoParkingCommand} 保持一致。
     *
     * @param direction {@link #MOVE_DIR_STOP} / {@link #MOVE_DIR_FORWARD} / {@link #MOVE_DIR_BACKWARD}
     * @param angle     方向盘角度，范围 -25~25（超出会被截断）
     * @param isStop    true=恢复手动模式（param1=0x00）；false=开始调整（param1=0x01）
     */
    public static byte[] buildMoveCarCommand(int direction, int angle, boolean isStop) {
        if (direction < 0 || direction > 2) {
            throw new IllegalArgumentException("direction 必须为 0/1/2");
        }
        // 1. 控制位1：BIT0 前进 / BIT1 后退 / BIT2 角度调整
        byte control1 = 0;
        if (direction == MOVE_DIR_FORWARD) {
            control1 |= 0x01;
        } else if (direction == MOVE_DIR_BACKWARD) {
            control1 |= 0x02;
        }
        control1 |= 0x04; // 总是启用角度调整

        if (angle < -25) angle = -25;
        else if (angle > 25) angle = 25;

        // 2. 参数1：0x01 开始调整 / 0x00 停止（恢复手动）
        byte param1 = isStop ? (byte) 0x00 : (byte) 0x01;
        // 3. 角度（2字节大端，偏移 1024）
        byte angleHigh = (byte) ((angle + 1024) >> 8);
        byte angleLow = (byte) ((angle + 1024) & 0xFF);
        // 4. 角速度
        byte angularVelocity = 25;

        byte[] body = new byte[]{control1, 0, 0, param1, angleHigh, angleLow, angularVelocity};
        return CommandUtils.generateCommand((byte) 0x00, (byte) 0x20, body);
    }

    /**
     * 下发挪车指令（带 500ms 节流，与源项目摇杆行为一致）。
     *
     * @return true=已下发；false=设备为空或被节流丢弃
     */
    public static boolean moveCar(BleDevice device, int direction, int angle, boolean isStop, BleWriteCallback callback) {
        if (device == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - sLastMoveTime < 500) {
            return false;
        }
        sLastMoveTime = now;
        write(device, buildMoveCarCommand(direction, angle, isStop), callback);
        return true;
    }

    /**
     * 开启挪车模式：开启通知 → onNotifySuccess 时下发 0x31 设置交换秘钥 → 500ms 后校准 (0,0)。
     * 还原源项目 OneKeyActivity2.notifyBle 的初始化流程。
     *
     * @param notifyCallback 通知数据回调（onCharacteristicChanged 可拿到设备回传），可为 null
     * @param writeCallback  指令写入回调，可为 null
     */
    public static void startMoveCarMode(BleDevice device, BleNotifyCallback notifyCallback, BleWriteCallback writeCallback) {
        if (device == null) {
            return;
        }
        notify(device, new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() {
                // 1. 设置交换秘钥 (code 0x31)
                byte[] key = KeyGenerator.generate14ByteKey();
                byte[] cmd = CommandUtils.generateCommand((byte) 0x00, (byte) 0x31, key);
                write(device, cmd, writeCallback);
                // 记录密码（补到 16 字节），后续 0x12 加密指令会用到
                SPUtils.getInstance().putString(SPUtils.SP_ONE_KEY_PWD, ByteUtils.byteToString(key) + "ffff");
                // 2. 500ms 后校准 0
                sMainHandler.postDelayed(() -> write(device, buildMoveCarCommand(0, 0, false), writeCallback), 500);
                if (notifyCallback != null) {
                    notifyCallback.onNotifySuccess();
                }
            }

            @Override
            public void onNotifyFailure(BleException exception) {
                if (notifyCallback != null) {
                    notifyCallback.onNotifyFailure(exception);
                }
            }

            @Override
            public void onCharacteristicChanged(byte[] data) {
                if (notifyCallback != null) {
                    notifyCallback.onCharacteristicChanged(data);
                }
            }
        });
    }

    /**
     * 停止挪车 / 恢复手动模式（强制下发，绕过节流，确保停止指令一定到达）。
     */
    public static void stopMoveCarMode(BleDevice device, BleWriteCallback callback) {
        if (device == null) {
            return;
        }
        sLastMoveTime = 0L; // 重置节流，保证下一次 moveCar 立即可发
        write(device, buildMoveCarCommand(0, 0, true), callback);
    }

    // ====================== 蓝牙状态 / 设备 ======================

    public static boolean isSupportBle() {
        return BleManager.getInstance().isSupportBle();
    }

    public static boolean isBlueEnable() {
        return BleManager.getInstance().isBlueEnable();
    }

    /**
     * 获取系统已配对（绑定）的 BLE 设备集合。
     */
    public static Set<BluetoothDevice> getPairedDevices(Context context) {
        return BluetoothUtils.getPairedDevices(context);
    }

    /**
     * 判断扫描到的蓝牙名是否属于当前设备（按设备号尾号匹配）。
     * 非 debug 模式且未设置 terminalNo 时返回 false。
     */
    public static boolean isCurrentDevice(String bleName) {
        return BluetoothUtils.isCurrentDevice(bleName);
    }

    /**
     * 工具：字节数组转十六进制字符串（解析广播 / 指令时常用）。
     */
    public static String bytesToHex(byte[] bytes) {
        return StringUtils.bytesToHex(bytes);
    }

    // ====================== 自动连接（后台保活重连） ======================

    private static synchronized BluetoothUtils getBluetoothUtils() {
        if (bluetoothUtils == null && SdkConfig.getContext() != null) {
            bluetoothUtils = new BluetoothUtils(SdkConfig.getContext());
        }
        return bluetoothUtils;
    }

    /**
     * 开启自动重连（后台循环检测已保存 mac 并自动连接）。
     */
    public static void startAutoConnect() {
        BluetoothUtils utils = getBluetoothUtils();
        if (utils != null) {
            utils.start();
        }
    }

    /**
     * 停止自动重连。
     */
    public static void stopAutoConnect() {
        if (bluetoothUtils != null) {
            bluetoothUtils.stop();
        }
    }

    public static void setAutoConnectEnabled(boolean enabled) {
        BluetoothUtils utils = getBluetoothUtils();
        if (utils != null) {
            utils.setAutoConnectEnabled(enabled);
        }
    }

    public static boolean isAutoConnectEnabled() {
        BluetoothUtils utils = getBluetoothUtils();
        return utils != null && utils.isAutoConnectEnabled();
    }
}
