# 🚀 tester-app 快速开始指南

## 项目概述

**tester-app** 是一个多平台原生测试应用，使用 Kotlin Multiplatform + Compose Multiplatform 构建，支持 Android、iOS 和鸿蒙平台。

当前版本：v1.0.0-alpha（仅 Android 平台）

## 技术栈

- **Kotlin**: 1.9.22
- **Compose Multiplatform**: 1.5.10
- **Android Gradle Plugin**: 8.2.2
- **Gradle**: 8.5
- **目标 SDK**: 34 (Android 14)
- **最低 SDK**: 24 (Android 7.0)

## 环境要求

### 必需工具

1. **JDK 17+**
   - 下载地址：https://adoptium.net/
   - 验证：`java -version`

2. **Android SDK**
   - 推荐使用 Android Studio
   - 或单独安装 Android SDK Command Line Tools
   - 需要 Android SDK 34

3. **Gradle 8.5+**
   - 项目已包含 Gradle Wrapper
   - 无需手动安装

### 推荐工具

1. **Android Studio Iguana+** (推荐)
   - 下载地址：https://developer.android.com/studio
   - 内置 Kotlin 插件和 Gradle 支持

2. **IntelliJ IDEA 2024.1+**
   - 下载地址：https://www.jetbrains.com/idea/
   - 需要安装 Kotlin 插件

## 安装步骤

### 1. 克隆项目

```bash
git clone https://github.com/AntoniotheFuture/tester-app.git
cd tester-app
```

### 2. 设置 Android SDK

如果你使用的是 Android Studio，SDK 会自动配置。

如果使用命令行，需要设置环境变量：

```bash
export ANDROID_HOME=/path/to/android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

### 3. 构建项目

```bash
# 首次构建（自动下载依赖）
./gradlew build

# 或者只构建 Debug 版本
./gradlew assembleDebug

# 清理后重新构建
./gradlew clean build
```

### 4. 运行应用

```bash
# 安装到已连接的设备/模拟器
./gradlew :androidApp:installDebug

# 或者直接运行
./gradlew :androidApp:run
```

### 5. 找到 APK

构建完成后，APK 文件位于：

```bash
androidApp/build/outputs/apk/debug/tester-app-debug.apk
```

## 项目结构

```
tester-app/
├── shared/                      # 共享模块（跨平台代码）
│   └── src/commonMain/
│       └── kotlin/
│           └── com/antoniofuture/testerapp/
│               └── common/
│                   ├── AppConfig.kt       # 应用配置
│                   ├── LogModels.kt        # 日志数据模型
│                   └── WebViewModels.kt   # WebView 数据模型
│
├── androidApp/                 # Android 应用模块
│   └── src/main/
│       ├── kotlin/com/antoniofuture/testerapp/
│       │   ├── MainActivity.kt            # 主活动
│       │   └── ui/
│       │       ├── screens/               # 界面
│       │       │   ├── HomeScreen.kt      # 首页
│       │       │   └── WebViewTestScreen.kt  # WebView 测试
│       │       └── theme/
│       │           └── Theme.kt           # 主题配置
│       └── res/                           # 资源文件
│
├── .github/workflows/          # GitHub Actions
│   └── android.yml             # CI/CD 配置
│
├── build.gradle.kts            # 根构建配置
├── settings.gradle.kts         # 项目设置
├── gradle.properties           # Gradle 属性
└── README.md                   # 项目说明
```

## 功能使用

### WebView 测试工具

1. **基础导航**
   - 在 URL 输入框输入网址
   - 点击箭头按钮导航
   - 使用前进/后退按钮浏览历史
   - 刷新或停止加载

2. **JavaScript 注入**
   - 点击右上角代码图标打开 JS 注入面板
   - 输入自定义 JavaScript 代码
   - 选择注入时机（加载前/后）
   - 点击"注入"执行脚本
   - 使用预设脚本快速测试

3. **日志查看**
   - 点击右上角列表图标打开日志面板
   - 查看 JS 控制台日志
   - 查看 WebView Activity 日志
   - 按级别筛选（Debug, Info, Warning, Error）
   - 清除日志或导出

4. **WebView 信息**
   - 点击右上角信息图标查看
   - 查看 WebView 版本
   - 查看支持的功能特性

5. **文件操作**
   - 文件下载：点击下载链接自动下载
   - 文件选择：点击附件图标选择本地文件

## 开发指南

### 添加新功能

#### 1. 共享模块（跨平台代码）

在 `shared/src/commonMain/kotlin/com/antoniofuture/testerapp/common/` 中添加：

```kotlin
// 1. 定义接口或抽象类
interface MyFeature {
    fun execute()
}

// 2. 定义数据模型
data class MyData(
    val name: String,
    val value: Any
)

// 3. 定义 ViewModel 状态
data class MyViewState(
    val data: MyData? = null,
    val isLoading: Boolean = false
)
```

#### 2. Android 特定实现

在 `androidApp/src/main/kotlin/com/antoniofuture/testerapp/ui/screens/` 中添加：

```kotlin
@Composable
fun MyFeatureScreen(
    viewModel: MyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Compose UI 实现
}
```

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行特定模块的测试
./gradlew :shared:test

# 查看测试报告
open tester-app/build/reports/tests/test/index.html
```

### 代码检查

```bash
# 运行 lint
./gradlew lint

# 查看 lint 报告
open androidApp/build/reports/lint-results.html
```

## 常见问题

### 1. 构建失败

**问题**: `./gradlew: Permission denied`

**解决**:
```bash
chmod +x gradlew
```

**问题**: `SDK location not found`

**解决**:
```bash
# 创建 local.properties
echo "sdk.dir=/path/to/android/sdk" > local.properties
```

### 2. Gradle 版本不匹配

**问题**: `Gradle version mismatch`

**解决**:
```bash
# 使用项目自带的 Gradle Wrapper
./gradlew wrapper
```

### 3. Kotlin 插件版本问题

**问题**: `Kotlin plugin version mismatch`

**解决**:
确保 `build.gradle.kts` 中的 Kotlin 版本与 `gradle.properties` 中的一致。

### 4. Compose 编译错误

**问题**: `Compose compiler version mismatch`

**解决**:
确保 Compose Compiler Extension Version 与 Kotlin 版本匹配：
- Kotlin 1.9.22 → Compose Compiler 1.5.8

## Git 工作流

### 创建分支

```bash
# 功能分支
git checkout -b feature/add-new-feature

# 修复分支
git checkout -b fix/issue-123
```

### 提交代码

```bash
# 添加更改
git add .

# 提交
git commit -m "feat: add new feature"

# 推送到远程
git push origin feature/add-new-feature
```

### 创建 Pull Request

1. 在 GitHub 上创建 Pull Request
2. 描述您的更改
3. 等待代码审查
4. 合并到主分支

## 学习资源

### Kotlin Multiplatform

- 官方文档：https://kotlinlang.org/docs/multiplatform.html
- KMM 官方：https://kotlinlang.org/lp/mobile/

### Compose Multiplatform

- 官方文档：https://www.jetbrains.com/compose/
- 示例项目：https://github.com/JetBrains/compose-multiplatform

### Android 开发

- Android 官方文档：https://developer.android.com/
- Jetpack Compose：https://developer.android.com/jetpack/compose

## 获取帮助

- 查看 [README.md](README.md) 了解项目详情
- 查看 [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) 了解开发规划
- 查看 [CHANGELOG.md](CHANGELOG.md) 了解版本历史
- 查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解贡献指南

## 下一步

1. ✅ 尝试构建项目
2. ✅ 运行应用并测试功能
3. 📖 阅读 [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md)
4. 🔧 尝试修改代码添加新功能
5. 📝 阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 了解如何贡献

祝您开发愉快！🎉
