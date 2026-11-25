# JiFang BleSdk

蓝牙 + 4G 车控能力 SDK。将蓝牙（扫描 / 连接 / 通知 / 写指令 / RSSI 设置 / 一键挪车）与 4G 车控（airControl 车控、空调、座椅、车窗、断油电、防狼模式等）封装进同一个 aar，**蓝牙与 4G 调用入口彼此独立**，接口全部开放。

> 工程环境：Gradle 8.9 / AGP 8.7.2 / compileSdk 35 / minSdk 24 / JDK 17。

---

## 功能特性

### 蓝牙（`BleSdk`）
- 设备扫描 / 停止扫描（带自动停止与等待队列）
- 连接 / 断开 / 自动重连（后台保活）
- 通知订阅 / 写数据（挪车、寻车、设防等指令）
- RSSI 上下限设置（普通版 / PKE 版，AT 指令）
- 任意 AT 文本指令透传
- **一键挪车 / 自动泊车**（CAN 协议新蓝牙）：开启挪车模式 → 方向 + 角度控制 → 停止恢复手动
- 已配对设备获取、当前设备过滤（按设备号尾号匹配）

### 4G 车控（`CarControlSdk`）
- 车控信息查询 / 设置
- airControl / airControl2 车控（尾门、空调、车窗、制冷制热、灯光、寻车等）
- 空调状态查询 / 档位调节 / 温度加减
- 座椅 / 车窗状态查询
- 断油电 / 恢复油电
- 防狼模式
- 通用 GET / POST 透传（便于扩展未封装接口）

---

## 远程依赖

通过 JitPack 发布，托管在 GitHub：

```groovy
// settings.gradle  →  dependencyResolutionManagement { repositories { ... } }
maven { url 'https://jitpack.io' }
```

```groovy
// build.gradle (app)
implementation 'com.github.MatNbq:jifang-blesdk:1.0.1'
```

> 版本号对应 GitHub 仓库的 tag。更新版本时打新 tag 推送，JitPack 会自动构建。

---

## 权限

在 `AndroidManifest.xml` 中声明（宿主合并）：

```xml
<!-- 蓝牙 -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<!-- 网络（4G 车控） -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Android 12+ 需在运行时申请 `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`，扫描还需定位权限。

---

## 快速开始

### 1. 初始化

在 `Application.onCreate` 或首次使用前调用：

```java
import com.shenghao.blesdk.BleSdk;
import com.shenghao.blesdk.SdkConfig;

// 初始化蓝牙 SDK（内部会同时初始化 SdkConfig）
BleSdk.init(this);
```

### 2. 注入 4G 车控登录态（仅使用 4G 车控时需要）

```java
SdkConfig.setToken("你的登录token");
SdkConfig.setImei("设备imei");
SdkConfig.setTerminalNo("终端号");
SdkConfig.setServerHost("https://your-server-host"); // 可选，为空走默认地址
```

或一次性配置：

```java
import com.shenghao.blesdk.CarControlSdk;

CarControlSdk.config(token, imei, terminalNo, serverHost);
```

---

## 蓝牙使用示例

```java
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.shenghao.blesdk.BleSdk;

// 扫描
BleSdk.scan(new BleScanCallback() {
    @Override public void onScanStarted(boolean success) {}
    @Override public void onScanning(BleDevice bleDevice) {
        // bleDevice.getName() / bleDevice.getMac()
    }
    @Override public void onScanFinished(List<BleDevice> scanResultList) {}
});

// 连接
BleSdk.connect(mac, new BleGattCallback() {
    @Override public void onStartConnect() {}
    @Override public void onConnectFail(BleDevice bleDevice, BleException exception) {}
    @Override public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
        // 连接成功后开启通知
        BleSdk.notify(bleDevice, null);
    }
    @Override public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {}
});
```

### 一键挪车

```java
// 开启挪车模式（开通知 → 交换秘钥 → 500ms 后校准）
BleSdk.startMoveCarMode(device, notifyCallback, writeCallback);

// 控制方向与角度（500ms 节流）
// direction: 0=停止 1=前进 2=后退；angle: -25~25
BleSdk.moveCar(device, BleSdk.MOVE_DIR_FORWARD, 10, false, writeCallback);

// 停止挪车 / 恢复手动
BleSdk.stopMoveCarMode(device, writeCallback);
```

### RSSI 设置 / AT 指令

```java
BleSdk.setRssi(device, new BlueRssi(lower, upper), writeCallback);   // 普通版
BleSdk.setRssiPke(device, new BlueRssiPke(lower, upper), writeCallback); // PKE 版
BleSdk.sendAtCommand(device, "AT+RSSI?", writeCallback);            // 任意 AT 指令
```

---

## 4G 车控使用示例

所有异步结果通过 `OkHttpResultCallBack` 回调：

```java
import com.shenghao.blesdk.CarControlSdk;
import com.shenghao.blesdk.okhttp.OkHttpResultCallBack;

// 查询车控信息
CarControlSdk.getCarControlInfo(terminalNo, new OkHttpResultCallBack() {
    @Override public void onSuccess(String result) { /* 解析结果 */ }
    @Override public void onFailure(String error) {}
});

// airControl 车控：type 06-尾门 07-空调 0B-车窗 21-制冷制热 27-灯光 9-寻车
CarControlSdk.airControl(terminalNo, "07", "1", null, callback);

// 空调档位：3~-3（制热3档 ~ 制冷3档，0=关闭）
CarControlSdk.adjust(terminalNo, 1, callback);

// 断油电 / 恢复油电
CarControlSdk.changePowerControl(terminalNo, command, callback);

// 防狼模式：param 0-关 1-开
CarControlSdk.antiWolf(terminalNo, 1, callback);
```

---

## 本地构建

```bash
# 构建 release aar
./gradlew :blesdk:assembleRelease
# 产物：blesdk/build/outputs/aar/blesdk-release.aar

# 发布到本地 maven（验证 publishing 配置）
./gradlew :blesdk:publishReleasePublicationToMavenLocal
```

Demo 应用（`:app`）依赖本地 `:blesdk` 模块，集成了全部功能用于测试：

```bash
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

---

## 项目结构

```
BleSdk/
├── blesdk/          # Android Library，SDK 主体（包名 com.shenghao.blesdk）
│   └── src/main/java/com/shenghao/blesdk/
│       ├── BleSdk.java         # 蓝牙入口
│       ├── CarControlSdk.java  # 4G 车控入口
│       └── SdkConfig.java      # 全局配置中心
├── app/             # Demo 应用（包名 com.shenghao.blesdk.demo）
├── jitpack.yml      # JitPack 构建配置（JDK 17）
└── README.md
```

---

## 依赖传递

SDK 自动传递以下依赖（通过 pom），宿主无需重复引入：

- `com.github.Jasonchenlijian:FastBle:2.4.0`（蓝牙）
- `org.greenrobot:eventbus:3.1.1`（事件总线，连接状态事件）
- `com.squareup.okhttp3:okhttp:4.9.3` / `logging-interceptor:4.9.3`（4G 车控网络）
- `com.alibaba:fastjson:1.2.83` / `com.google.code.gson:gson:2.10.1`（JSON）
- `androidx.appcompat:appcompat:1.6.1`
