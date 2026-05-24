# App Tester 调试指南

## 🚀 快速开始

### 一、准备工作

#### 1. 确认您有 Android Studio
如果没有，请从 [Android Studio 官网](https://developer.android.com/studio) 下载安装

#### 2. 项目已准备好
我们已经完成了项目代码的编写，现在需要：
- 连接 Android 设备
- 构建应用
- 安装到设备
- 运行和调试

---

## 📱 方式一：USB 有线连接（最稳定）

### 步骤：

1. **开启开发者选项和 USB 调试**
   - 打开手机设置
   - 找到 "关于手机"
   - 连续点击 "版本号" 7 次（通常连续点击）
   - 输入您的锁屏密码（如果有）
   - 会显示 "您已处于开发者模式"

2. **返回设置**
   - 现在应该能看到 "开发者选项" 或 "开发人员选项"
   - 进入开发者选项
   - 找到并开启 "USB 调试"

3. **连接手机到电脑**
   - 使用数据线连接您的 Android 手机和 Mac
   - 手机上会弹出 "允许 USB 调试"
   - 点击 "允许" 或 "始终允许来自此计算机"

4. **验证连接**
   在终端运行：
   ```bash
   adb devices
   ```
   应该能看到您的设备序列号

---

## 📱 方式二：无线局域网连接（更方便）

### 注意：首次配置需要 USB 连接一次

#### Android 11 及以上（推荐）：

1. **开启无线调试**
   - 开发者选项 → 无线调试 → 开启
   - 点击 "使用配对码配对设备"
   - 记录下 IP 地址、端口和配对码

2. **配对设备**
   ```bash
   adb pair 手机IP:端口 配对码
   ```
   例如：
   ```bash
   adb pair 192.168.1.100:37123 123456
   ```

3. **连接设备**
   在无线调试页面查看连接端口：
   ```bash
   adb connect 手机IP:连接端口
   ```
   例如：
   ```bash
   adb connect 192.168.1.100:5555
   ```

#### 旧版本 Android（Android 10 及以下）：

1. **先用 USB 连接**
2. **设置 TCP 端口**
   ```bash
   adb tcpip 5555
   ```
3. **拔掉 USB 线**
4. **查看手机 IP 地址**
   - 设置 → WLAN → 查看当前连接 WiFi 的 IP
5. **无线连接**
   ```bash
   adb connect 手机IP:5555
   ```

---

## 🔧 方式三：使用 Android Studio（最简单）

如果您安装了 Android Studio：

### 步骤：

1. **打开 Android Studio**
2. **打开项目**
   - File → Open
   - 选择 `/Users/antonioliang/Documents/projects/app-tester` 目录
3. **等待 Gradle 同步完成**（右下角进度条）
4. **连接设备**（USB 或无线）
5. **在顶部工具栏选择设备**
6. **点击 Run 按钮（绿色三角形 ▶️）**

Android Studio 会自动：
- 构建项目
- 安装到设备
- 运行应用
- 打开 Logcat 日志窗口

---

## 📋 连接设备检查清单

连接设备后，在终端运行：

```bash
adb devices
```

应该看到类似：
```
List of devices attached
emulator-5554   device    # 模拟器
abc123def456    device    # 真实设备
```

如果显示 `unauthorized`：
- 查看手机屏幕
- 点击 "允许 USB 调试"

---

## 🛠️ 构建和安装应用

### 使用命令行：

```bash
# 进入项目目录
cd /Users/antonioliang/Documents/projects/app-tester

# 构建 Debug 版本
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 或者直接运行
./gradlew installDebug
```

### 构建输出：
Debug APK 位置：`androidApp/build/outputs/apk/debug/`

---

## 🐛 调试技巧

### 查看日志：
```bash
# 查看实时日志
adb logcat

# 过滤应用日志
adb logcat | grep "App Tester"

# 清除日志
adb logcat -c
```

### 截屏：
```bash
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

### 录屏：
```bash
adb shell screenrecord /sdcard/demo.mp4
# 按 Ctrl+C 停止
adb pull /sdcard/demo.mp4
```

---

## 📱 常见问题

### 问题 1：设备未识别
- 尝试更换 USB 线
- 更换 USB 接口
- 重启手机和电脑
- 确保 USB 线是传输线（不是充电线）

### 问题 2：adb 命令找不到
```bash
# 确保 adb 在 PATH 中
export PATH=$PATH:~/Library/Android/sdk/platform-tools
```

### 问题 3：无线连接断开
重新执行连接命令：
```bash
adb connect 手机IP:5555
```

---

## 🎯 推荐流程

1. **首先用 USB 连接**（最稳定）
2. **用 Android Studio 运行**（最简单）
3. **熟悉后尝试无线连接**（更方便）
4. **使用 Logcat 查看日志**

---

## 📱 推荐的 Android Studio 操作

### 快捷操作：
- **Run**：`⌘R`（Mac）
- **Debug**：`⌘D`
- **停止**：`⌘F2`
- **安装应用**：`⇧⌘A` → "Install"

### 调试功能：
- 断点调试
- 变量查看
- 表达式求值
- 布局检查器
- Profiler 性能分析

---

## 📝 下一步

连接设备后告诉我，我会帮您：
1. 构建应用
2. 安装到设备
3. 运行和调试
4. 查看日志

有问题随时问！
