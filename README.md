# app-tester

一个多平台原生测试应用，支持 Android、iOS、原生鸿蒙等平台的 WebView 测试工具。

## 📱 功能特性

### MVP 版本 (v1.0.0-alpha)

**WebView 测试工具** - 已完成
- ✅ 基础导航功能（URL 导航、前进/后退、刷新/停止）
- ✅ JavaScript 注入（自定义脚本、预设脚本库）
- ✅ 日志系统（JS 日志、Activity 日志、日志查看）
- ✅ WebView 信息查看（版本信息、功能特性）
- ✅ 文件操作（下载事件、文件选择）

### 即将推出

**v1.1.0** (计划)
- 截图功能
- Cookie 管理
- 缓存清理
- User-Agent 切换

**v2.0.0** (计划 2025-Q4)
- iOS 平台支持
- 性能测试模块
- 网络请求监控

**v3.0.0** (计划 2026)
- 鸿蒙平台支持
- 自动化测试脚本
- 测试报告生成

## 🛠️ 技术栈

- **框架**: Kotlin Multiplatform (KMM) + Compose Multiplatform
- **语言**: Kotlin 1.9.22
- **UI**: Jetpack Compose + Material Design 3
- **架构**: MVVM + Clean Architecture
- **构建**: Gradle 8.5 + Android Gradle Plugin 8.2.2

## 📦 项目结构

```
app-tester/
├── shared/                    # 共享模块（跨平台代码）
│   └── src/commonMain/        # 通用业务逻辑
├── androidApp/               # Android 应用
│   └── src/main/             # Android 特定实现
├── iosApp/                   # iOS 应用（待开发）
├── harmonyApp/              # 鸿蒙应用（待开发）
└── .github/
    └── workflows/            # CI/CD 配置
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Android SDK 34
- Gradle 8.5+
- Android Studio Iguana+ / IntelliJ IDEA 2024.1+

### 构建项目

```bash
# 克隆项目
git clone https://github.com/yourusername/app-tester.git
cd app-tester

# 构建 Debug APK
./gradlew assembleDebug

# 运行测试
./gradlew test

# 运行 lint
./gradlew lint

# 构建 Release APK
./gradlew assembleRelease
```

### 运行应用

构建完成后，APK 文件位于:
```
androidApp/build/outputs/apk/debug/app-tester-debug.apk
```

## 📊 版本信息

- **当前版本**: v1.0.0-alpha
- **包名前缀**: com.apptester
- **最低 Android 版本**: API 24 (Android 7.0)
- **目标 Android 版本**: API 34 (Android 14)

## 🔧 开发指南

### 添加新功能

1. 在 `shared/src/commonMain` 添加通用业务逻辑
2. 在对应平台的 `*Main` 目录添加平台特定实现
3. 使用 Compose Multiplatform 实现跨平台 UI

### 代码规范

- 遵循 Kotlin 官方编码规范
- 使用 MVVM + Clean Architecture
- 所有公开 API 需要文档注释
- 新功能需要添加单元测试

### Git 工作流

```bash
# 创建功能分支
git checkout -b feature/your-feature

# 提交更改
git commit -m "feat: add new feature"

# 推送分支
git push origin feature/your-feature

# 创建 Pull Request
```

## 📈 CI/CD

项目使用 GitHub Actions 进行持续集成和部署：

- **Build**: 自动化构建 Debug APK
- **Test**: 运行单元测试和 lint 检查
- **Release**: 自动化发布 Release APK

详细配置请查看 [.github/workflows/android.yml](.github/workflows/android.yml)

## 📝 文档

- [开发规划](DEVELOPMENT_PLAN.md) - 详细的项目规划和技术选型
- [开发日志](CHANGELOG.md) - 版本变更和开发记录

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目基于 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📧 联系方式

- **开发者**: TODO
- **邮箱**: TODO
- **GitHub**: https://github.com/yourusername/app-tester

## 🙏 致谢

- JetBrains - Kotlin 和 IntelliJ IDEA
- Google - Android 和 Jetpack Compose
- 所有开源社区的贡献者

---

**Made with ❤️ using Kotlin Multiplatform + Compose Multiplatform**
