# 贡献指南

感谢您对 tester-app 项目的关注！我们欢迎各种形式的贡献，包括但不限于代码提交、文档改进、Bug 报告等。

## 📋 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发环境](#开发环境)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [提交信息规范](#提交信息规范)
- [Pull Request 流程](#pull-request-流程)
- [报告问题](#报告问题)

## 行为准则

参与本项目的所有成员都必须遵守以下行为准则：

- 使用友好和包容的语言
- 尊重不同的观点和经验
- 接受建设性的批评
- 关注社区的最佳利益
- 对其他社区成员表示善意

## 如何贡献

### 报告 Bug

如果您发现了 Bug，请通过 GitHub Issues 报告。请包含以下信息：

- 清晰的 Bug 描述
- 复现步骤
- 预期行为 vs 实际行为
- 您的环境信息（操作系统、Android 版本等）
- 相关的日志或截图

### 提交功能请求

我们欢迎新功能的建议！请通过 GitHub Issues 提交，并使用 `feature request` 标签。

### 代码贡献

我们非常欢迎代码贡献！请遵循以下流程：

1. Fork 本仓库
2. 创建您的功能分支
3. 进行开发
4. 确保所有测试通过
5. 提交 Pull Request

## 开发环境

### 环境要求

- JDK 17 或更高版本
- Android SDK 34
- Gradle 8.5
- Android Studio Iguana+ 或 IntelliJ IDEA 2024.1+

### 设置开发环境

```bash
# 1. 克隆您的 fork
git clone https://github.com/YOUR_USERNAME/tester-app.git
cd tester-app

# 2. 添加上游仓库
git remote add upstream https://github.com/AntoniotheFuture/tester-app.git

# 3. 安装依赖
./gradlew dependencies

# 4. 同步代码
./gradlew sync

# 5. 运行应用
./gradlew :androidApp:installDebug
```

## 开发流程

### 1. 创建分支

```bash
# 确保在最新的 main 分支上
git checkout main
git pull upstream main

# 创建新分支
git checkout -b feature/your-feature-name
# 或者
git checkout -b fix/issue-number
```

### 2. 进行开发

在开发时请注意：

- 遵循项目的代码规范
- 编写有意义的注释
- 确保代码质量
- 添加必要的测试

### 3. 保持同步

在开发过程中，定期与上游仓库同步：

```bash
git fetch upstream
git rebase upstream/main
```

### 4. 提交更改

```bash
# 添加更改
git add .

# 提交（遵循提交信息规范）
git commit -m "feat: add new feature"

# 推送
git push origin feature/your-feature-name
```

## 代码规范

### Kotlin 代码规范

- 遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 使用有意义的变量和函数命名
- 函数应该简短且单一职责
- 避免使用 `!!` 操作符
- 优先使用不可变性

### 代码结构

```kotlin
// 文件组织顺序
@file:Suppress("UNUSED")

package com.antoniofuture.testerapp

import ...

// 类和函数
class ExampleClass {

    private val privateProperty = ...
    
    companion object {
        const val CONSTANT = ...
    }

    fun publicFunction() {
        // ...
    }

    private fun privateFunction() {
        // ...
    }
}
```

### Compose 代码规范

- 使用函数式 Compose（避免类继承）
- 组件应该是纯函数
- 避免在 Compose 中执行业务逻辑
- 使用 `remember` 记住计算结果
- 合理使用 `Modifier`

```kotlin
@Composable
fun MyScreen(
    viewModel: MyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ...
    }
}
```

## 提交信息规范

我们使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 类型 (Type)

- `feat`: 新功能
- `fix`: 修复 Bug
- `docs`: 文档更改
- `style`: 代码格式（不影响代码运行的变动）
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

### 示例

```
feat(webview): add JavaScript injection feature

- implement custom JS injection
- add preset script library
- support injection timing control

Closes #123
```

```
fix(logs): resolve memory leak in log storage

- limit log entries to 1000
- add automatic cleanup
- optimize data structure

Fixes #456
```

## Pull Request 流程

### 1. 创建 PR

- 使用清晰的标题
- 描述您的更改
- 关联相关的问题
- 确保所有 CI 检查通过

### 2. PR 模板

```markdown
## 描述
简要描述您的更改

## 更改类型
- [ ] Bug 修复
- [ ] 新功能
- [ ] 文档更新
- [ ] 重构
- [ ] 其他

## 测试
描述您如何测试这些更改

## 截图（如适用）
添加相关截图

## 检查清单
- [ ] 我的代码遵循项目的代码规范
- [ ] 我已经进行了自我审查
- [ ] 我已经添加了必要的注释
- [ ] 我的更改不会产生新的警告
- [ ] 我已经添加了测试（如果适用）
- [ ] 所有测试都通过了
```

### 3. 代码审查

- 响应审查意见
- 进行必要的修改
- 等待合并

## 报告问题

### Issue 模板

```markdown
## 描述
清晰描述您的问题或建议

## 环境
- 操作系统：
- Android 版本：
- 应用版本：

## 复现步骤
1.
2.
3.

## 期望行为
您期望发生什么？

## 实际行为
实际发生了什么？

## 日志（如适用）
添加相关日志

## 截图（如适用）
添加相关截图
```

## 🆘 获取帮助

如果您在贡献过程中遇到任何问题：

- 查看 [文档](README.md)
- 搜索已解决的问题
- 在 GitHub Issues 中提问
- 联系维护者

## 📜 许可证

通过贡献代码，您同意您的贡献将在 MIT 许可证下发布。

感谢您的贡献！🎉
