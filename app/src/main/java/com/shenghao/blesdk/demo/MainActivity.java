package com.shenghao.blesdk.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.shenghao.blesdk.BleSdk;
import com.shenghao.blesdk.CarControlSdk;
import com.shenghao.blesdk.SdkConfig;
import com.shenghao.blesdk.ble.BlueRssi;
import com.shenghao.blesdk.ble.command.CommandUtils;
import com.shenghao.blesdk.okhttp.OkHttpRequestParams;
import com.shenghao.blesdk.okhttp.OkHttpResultCallBack;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERM = 1001;

    private EditText etToken, etImei, etTerminal, etHost;
    private TextView tvConnStatus, tvLog, tvMoveAngle;

    private final List<BleDevice> deviceList = new ArrayList<>();
    private DeviceAdapter deviceAdapter;
    private BleDevice selectedDevice;
    private int moveAngle = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 初始化 SDK
        BleSdk.init(this);
        SdkConfig.setDebug(true);

        bindViews();
        setupDeviceList();
        setupListeners();

        log("SDK 已初始化，BleSdk.isSupportBle=" + BleSdk.isSupportBle()
                + " isBlueEnable=" + BleSdk.isBlueEnable());
        requestPermissionsIfNeeded();
    }

    // ====================== 视图绑定 ======================

    private void bindViews() {
        etToken = findViewById(R.id.et_token);
        etImei = findViewById(R.id.et_imei);
        etTerminal = findViewById(R.id.et_terminal);
        etHost = findViewById(R.id.et_host);
        tvConnStatus = findViewById(R.id.tv_conn_status);
        tvLog = findViewById(R.id.tv_log);
        tvMoveAngle = findViewById(R.id.tv_move_angle);
        tvLog.setMovementMethod(new ScrollingMovementMethod());
        // 解决日志 TextView 嵌在 ScrollView 中无法滑动的问题：
        // 按下/移动时禁止父容器拦截触摸事件，交给 TextView 自身滚动
        tvLog.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });
    }

    private void setupDeviceList() {
        ListView lv = findViewById(R.id.lv_devices);
        deviceAdapter = new DeviceAdapter();
        lv.setAdapter(deviceAdapter);
        // 解决 ListView 嵌在 ScrollView 中无法滑动的问题：
        // 按下/移动时禁止父容器拦截触摸事件，交给 ListView 自身滚动
        lv.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });
        lv.setOnItemClickListener((parent, view, position, id) -> {
            selectedDevice = deviceList.get(position);
            deviceAdapter.notifyDataSetChanged(); // 刷新高亮
            log("选中设备：" + describe(selectedDevice));
        });
    }

    // ====================== 监听器 ======================

    private void setupListeners() {
        // 配置
        findViewById(R.id.btn_apply_config).setOnClickListener(v -> applyConfig());
        findViewById(R.id.btn_clear_log).setOnClickListener(v -> runOnUiThread(() -> tvLog.setText("")));

        // 权限 / 扫描
        findViewById(R.id.btn_perm).setOnClickListener(v -> requestPermissionsIfNeeded());
        findViewById(R.id.btn_scan).setOnClickListener(v -> startScan());
        findViewById(R.id.btn_stop_scan).setOnClickListener(v -> {
            BleSdk.stopScan();
            log("停止扫描");
        });

        // 连接 / 通知 / 自动连接
        findViewById(R.id.btn_connect).setOnClickListener(v -> connectSelected());
        findViewById(R.id.btn_disconnect).setOnClickListener(v -> {
            BleDevice d = targetDevice();
            if (d != null) {
                BleSdk.disconnect(d);
                log("断开：" + describe(d));
            } else log("无已连接设备");
        });
        findViewById(R.id.btn_disconnect_all).setOnClickListener(v -> {
            BleSdk.disconnectAll();
            log("断开全部");
        });
        findViewById(R.id.btn_notify).setOnClickListener(v -> {
            BleDevice d = targetDevice();
            if (d != null) BleSdk.notify(d, notifyCallback);
            else log("无已连接设备");
        });
        findViewById(R.id.btn_stop_notify).setOnClickListener(v -> {
            BleDevice d = targetDevice();
            if (d != null) {
                BleSdk.stopNotify(d);
                log("停止通知");
            }
        });
        findViewById(R.id.btn_auto_on).setOnClickListener(v -> {
            BleSdk.setAutoConnectEnabled(true);
            BleSdk.startAutoConnect();
            log("自动连接已开启");
        });
        findViewById(R.id.btn_auto_off).setOnClickListener(v -> {
            BleSdk.stopAutoConnect();
            BleSdk.setAutoConnectEnabled(false);
            log("自动连接已关闭");
        });

        // 写指令
        findViewById(R.id.btn_send_at).setOnClickListener(v -> {
            BleDevice d = targetDevice();
            EditText at = findViewById(R.id.et_at);
            if (d != null) {
                BleSdk.sendAtCommand(d, at.getText().toString(), writeCallback);
                log("发送AT：" + at.getText().toString());
            } else log("无已连接设备");
        });
        findViewById(R.id.btn_send_cmd).setOnClickListener(v -> {
            BleDevice d = targetDevice();
            if (d == null) {
                log("无已连接设备");
                return;
            }
            EditText ef = findViewById(R.id.et_flag);
            EditText ec = findViewById(R.id.et_code);
            try {
                byte flag = (byte) Integer.parseInt(ef.getText().toString().trim(), 16);
                byte code = (byte) Integer.parseInt(ec.getText().toString().trim(), 16);
                byte[] cmd = CommandUtils.generateCommand(flag, code, new byte[0]);
                BleSdk.write(d, cmd, writeCallback);
                log("生成指令 flag=0x" + hex(flag) + " code=0x" + hex(code) + " -> " + BleSdk.bytesToHex(cmd));
            } catch (Exception e) {
                log("指令生成失败：" + e.getMessage());
            }
        });

        // RSSI
        findViewById(R.id.btn_rssi_weak).setOnClickListener(v -> setRssi(BlueRssi.WEAK));
        findViewById(R.id.btn_rssi_middle).setOnClickListener(v -> setRssi(BlueRssi.MIDDLE));
        findViewById(R.id.btn_rssi_strong).setOnClickListener(v -> setRssi(BlueRssi.STRONG));
        findViewById(R.id.btn_connected_list).setOnClickListener(v -> {
            List<BleDevice> list = BleSdk.getAllConnectedDevice();
            StringBuilder sb = new StringBuilder("已连接 " + list.size() + " 台：");
            for (BleDevice d : list) sb.append("\n  ").append(describe(d));
            log(sb.toString());
        });

        // 蓝牙挪车
        findViewById(R.id.btn_move_start).setOnClickListener(v -> {
            BleDevice d = targetDevice();
            if (d == null) { log("无已连接设备"); return; }
            moveAngle = 0;
            updateMoveAngleView();
            BleSdk.startMoveCarMode(d, notifyCallback, writeCallback);
            log("开启挪车模式：" + describe(d) + "（已下发 0x31 设秘钥 + 校准0）");
        });
        findViewById(R.id.btn_move_stop_mode).setOnClickListener(v -> {
            BleDevice d = targetDevice();
            if (d == null) { log("无已连接设备"); return; }
            BleSdk.stopMoveCarMode(d, writeCallback);
            log("停止挪车 / 恢复手动模式");
        });
        findViewById(R.id.btn_move_forward).setOnClickListener(v -> moveCar(BleSdk.MOVE_DIR_FORWARD));
        findViewById(R.id.btn_move_backward).setOnClickListener(v -> moveCar(BleSdk.MOVE_DIR_BACKWARD));
        findViewById(R.id.btn_move_stop).setOnClickListener(v -> moveCar(BleSdk.MOVE_DIR_STOP));
        findViewById(R.id.btn_move_left).setOnClickListener(v -> adjustMoveAngle(-3));
        findViewById(R.id.btn_move_right).setOnClickListener(v -> adjustMoveAngle(3));
        findViewById(R.id.btn_move_center).setOnClickListener(v -> {
            moveAngle = 0;
            updateMoveAngleView();
            moveCar(BleSdk.MOVE_DIR_STOP);
        });

        // 4G 车控
        findViewById(R.id.btn_get_info).setOnClickListener(v ->
                CarControlSdk.getCarControlInfo(tn(), cb("查询车控状态")));
        findViewById(R.id.btn_set_control).setOnClickListener(v ->
                CarControlSdk.setCarControl(tn(), txt(R.id.et_cc_type), txt(R.id.et_cc_status), cb("设置车控")));
        findViewById(R.id.btn_air_control).setOnClickListener(v ->
                CarControlSdk.airControl(tn(), txt(R.id.et_ac_type), txt(R.id.et_ac_param), txt(R.id.et_ac_angle), cb("车控")));
        findViewById(R.id.btn_air_control2).setOnClickListener(v ->
                CarControlSdk.airControl2(tn(), i(R.id.et_ac2_type), i(R.id.et_ac2_param), i(R.id.et_ac2_angle), cb("车控2")));
        findViewById(R.id.btn_get_ac).setOnClickListener(v -> CarControlSdk.getAC(tn(), cb("查空调")));
        findViewById(R.id.btn_adjust).setOnClickListener(v -> CarControlSdk.adjust(tn(), i(R.id.et_adjust_status), cb("空调调节")));
        findViewById(R.id.btn_adjust_temp).setOnClickListener(v ->
                CarControlSdk.adjustTemperature(tn(), i(R.id.et_temp_status), txt(R.id.et_temp_val), cb("温度调节")));
        findViewById(R.id.btn_get_seat).setOnClickListener(v -> CarControlSdk.getSeat(tn(), cb("查座椅")));
        findViewById(R.id.btn_get_window).setOnClickListener(v -> CarControlSdk.getCarWindow(tn(), cb("查车窗")));
        findViewById(R.id.btn_power).setOnClickListener(v -> CarControlSdk.changePowerControl(tn(), i(R.id.et_power_cmd), cb("断油电")));
        findViewById(R.id.btn_wolf).setOnClickListener(v -> CarControlSdk.antiWolf(tn(), i(R.id.et_wolf_param), cb("防狼")));
        findViewById(R.id.btn_get_raw).setOnClickListener(v -> CarControlSdk.get(txt(R.id.et_url), new OkHttpRequestParams(), cb("GET请求")));
        findViewById(R.id.btn_post_raw).setOnClickListener(v -> CarControlSdk.post(txt(R.id.et_url), new OkHttpRequestParams(), cb("POST请求")));
    }

    // ====================== 配置 ======================

    private void applyConfig() {
        String token = txt(R.id.et_token);
        String imei = txt(R.id.et_imei);
        String terminal = txt(R.id.et_terminal);
        String host = txt(R.id.et_host);
        CarControlSdk.config(token, imei, terminal, host);
        log("配置已应用：token=" + mask(token) + " imei=" + mask(imei)
                + " terminalNo=" + terminal + " host=" + host);
    }

    // ====================== 权限 ======================

    private void requestPermissionsIfNeeded() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN);
            perms.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        List<String> need = new ArrayList<>();
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                need.add(p);
            }
        }
        if (!need.isEmpty()) {
            ActivityCompat.requestPermissions(this, need.toArray(new String[0]), REQ_PERM);
        } else {
            log("权限已全部授予");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            boolean all = true;
            for (int g : grantResults) if (g != PackageManager.PERMISSION_GRANTED) all = false;
            log("权限请求结果：" + (all ? "全部授予" : "存在拒绝（扫描可能不可用）"));
        }
    }

    // ====================== 扫描 ======================

    private void startScan() {
        deviceList.clear();
        deviceAdapter.notifyDataSetChanged();
        BleSdk.scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                log("扫描开始 success=" + success);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                runOnUiThread(() -> {
                    boolean exists = false;
                    for (BleDevice d : deviceList) {
                        if (d.getMac() != null && d.getMac().equals(bleDevice.getMac())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        deviceList.add(bleDevice);
                        deviceAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                log("扫描结束，共 " + scanResultList.size() + " 台");
            }
        });
    }

    // ====================== 连接 ======================

    private void connectSelected() {
        BleDevice d = selectedDevice;
        if (d == null && !deviceList.isEmpty()) d = deviceList.get(0);
        if (d == null) {
            log("请先扫描并选择设备");
            return;
        }
        log("发起连接：" + describe(d));
        BleSdk.connect(d, gattCallback);
    }

    private final BleGattCallback gattCallback = new BleGattCallback() {
        @Override
        public void onStartConnect() {
            log("GATT onStartConnect");
        }

        @Override
        public void onConnectFail(BleDevice bleDevice, BleException exception) {
            log("连接失败：" + describe(bleDevice) + " err=" + exception);
            updateStatus();
        }

        @Override
        public void onConnectSuccess(BleDevice bleDevice, android.bluetooth.BluetoothGatt gatt, int status) {
            log("连接成功：" + describe(bleDevice) + " status=" + status);
            selectedDevice = bleDevice;
            updateStatus();
            // 连接成功后自动开启通知
            BleSdk.notify(bleDevice, notifyCallback);
        }

        @Override
        public void onDisConnected(boolean isActiveDisConnected, BleDevice device,
                                   android.bluetooth.BluetoothGatt gatt, int status) {
            log("断开连接 active=" + isActiveDisConnected + " " + describe(device) + " status=" + status);
            updateStatus();
        }
    };

    private final BleNotifyCallback notifyCallback = new BleNotifyCallback() {
        @Override
        public void onNotifySuccess() {
            log("通知开启成功");
        }

        @Override
        public void onNotifyFailure(BleException exception) {
            log("通知开启失败：" + exception);
        }

        @Override
        public void onCharacteristicChanged(byte[] data) {
            log("收到通知数据：" + BleSdk.bytesToHex(data));
        }
    };

    private final BleWriteCallback writeCallback = new BleWriteCallback() {
        @Override
        public void onWriteSuccess(int current, int total, byte[] justWrite) {
            log("写成功 current=" + current + "/" + total + " data=" + BleSdk.bytesToHex(justWrite));
        }

        @Override
        public void onWriteFailure(BleException exception) {
            log("写失败：" + exception);
        }
    };

    private void setRssi(BlueRssi rssi) {
        BleDevice d = targetDevice();
        if (d == null) {
            log("无已连接设备");
            return;
        }
        BleSdk.setRssi(d, rssi, writeCallback);
        log("设置RSSI=" + rssi.name() + "(" + rssi.getValue() + ") low=" + rssi.getLowRssi() + " high=" + rssi.getHighRssi());
    }

    // ====================== 蓝牙挪车（demo 辅助） ======================

    private void moveCar(int direction) {
        BleDevice d = targetDevice();
        if (d == null) {
            log("无已连接设备");
            return;
        }
        boolean sent = BleSdk.moveCar(d, direction, moveAngle, false, writeCallback);
        String dir = direction == BleSdk.MOVE_DIR_FORWARD ? "前进"
                : direction == BleSdk.MOVE_DIR_BACKWARD ? "后退" : "停止/调角度";
        if (sent) {
            log("挪车指令：" + dir + " angle=" + moveAngle
                    + " cmd=" + BleSdk.bytesToHex(BleSdk.buildMoveCarCommand(direction, moveAngle, false)));
        } else {
            log("挪车指令被节流丢弃（500ms 内重复）：" + dir + " angle=" + moveAngle);
        }
    }

    private void adjustMoveAngle(int delta) {
        moveAngle = Math.max(-25, Math.min(25, moveAngle + delta));
        updateMoveAngleView();
        moveCar(BleSdk.MOVE_DIR_STOP); // direction=0 仅调节方向盘角度
    }

    private void updateMoveAngleView() {
        if (tvMoveAngle != null) {
            tvMoveAngle.setText("角度:" + moveAngle);
        }
    }

    // ====================== 4G 回调 ======================

    private OkHttpResultCallBack cb(String tag) {
        return new OkHttpResultCallBack() {
            @Override
            public void onResponse(Response response, String body) {
                log("[" + tag + "] onResponse code=" + (response == null ? "null" : response.code())
                        + " body=" + body);
            }

            @Override
            public void onFailed(Request request, Exception e) {
                log("[" + tag + "] onFailed err=" + (e == null ? "null" : e.getMessage()));
            }
        };
    }

    // ====================== 工具 ======================

    private BleDevice targetDevice() {
        if (selectedDevice != null && BleSdk.isConnected(selectedDevice)) {
            return selectedDevice;
        }
        List<BleDevice> list = BleSdk.getAllConnectedDevice();
        if (!list.isEmpty()) {
            selectedDevice = list.get(0);
            return selectedDevice;
        }
        return null;
    }

    private void updateStatus() {
        runOnUiThread(() -> {
            List<BleDevice> list = BleSdk.getAllConnectedDevice();
            if (list.isEmpty()) {
                tvConnStatus.setText("蓝牙：未连接");
            } else {
                StringBuilder sb = new StringBuilder("蓝牙：已连接 " + list.size() + " 台");
                for (BleDevice d : list) sb.append("\n  ").append(describe(d));
                tvConnStatus.setText(sb.toString());
            }
            if (deviceAdapter != null) deviceAdapter.notifyDataSetChanged(); // 刷新连接标记
        });
    }

    private String tn() {
        return txt(R.id.et_terminal);
    }

    private String txt(int id) {
        EditText et = findViewById(id);
        return et == null ? "" : et.getText().toString().trim();
    }

    private int i(int id) {
        try {
            return Integer.parseInt(txt(id));
        } catch (Exception e) {
            return 0;
        }
    }

    private String describe(BleDevice d) {
        if (d == null) return "null";
        return (d.getName() == null ? "null" : d.getName()) + " [" + d.getMac() + "] rssi=" + d.getRssi();
    }

    private String mask(String s) {
        if (s == null || s.isEmpty()) return "(空)";
        if (s.length() <= 4) return s;
        return s.substring(0, 2) + "***" + s.substring(s.length() - 2);
    }

    private String hex(byte b) {
        return String.format(Locale.US, "%02X", b & 0xFF);
    }

    private void log(String msg) {
        runOnUiThread(() -> {
            if (tvLog == null) return;
            String time = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
            tvLog.append(time + "  " + msg + "\n");
            // 自动滚动到底部（布局未完成时 getLayout 为 null，需判空并延迟）
            tvLog.post(() -> {
                if (tvLog == null || tvLog.getLayout() == null) return;
                int scrollAmount = tvLog.getLayout().getLineTop(tvLog.getLineCount()) - tvLog.getHeight();
                if (scrollAmount > 0) tvLog.scrollTo(0, scrollAmount);
            });
        });
    }

    // ====================== 设备列表适配器 ======================

    private class DeviceAdapter extends ArrayAdapter<BleDevice> {
        DeviceAdapter() {
            super(MainActivity.this, android.R.layout.simple_list_item_1, deviceList);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            BleDevice d = deviceList.get(position);
            TextView tv = v.findViewById(android.R.id.text1);
            tv.setText(describe(d));
            boolean selected = selectedDevice != null && d.getMac() != null
                    && d.getMac().equals(selectedDevice.getMac());
            boolean connected = BleSdk.isConnected(d);
            if (selected) {
                // 选中：蓝色背景 + 白色文字 + 勾选标记
                v.setBackgroundColor(0xFF1976D2);
                tv.setTextColor(0xFFFFFFFF);
                tv.setText("✓ " + describe(d));
            } else if (connected) {
                // 已连接：透明背景 + 绿色文字 + 圆点标记
                v.setBackgroundColor(0x00000000);
                tv.setTextColor(0xFF2E7D32);
                tv.setText("● " + describe(d));
            } else {
                v.setBackgroundColor(0x00000000);
                tv.setTextColor(0xFF222222);
            }
            return v;
        }
    }
}
