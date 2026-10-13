# 桌面小组件重做发布验收记录

## 范围

分支：`codex/desktop-widgets`

完成提交：

- `da58d81` 建立桌面小组件展示契约
- `c7f6a5f` 桥接 macOS 小组件安全快照
- `50fc769` 新增 macOS 单凭证小组件
- `73d5f70` 支持 macOS 小组件配置与总览
- `f9da107` 完善 macOS 小组件状态与时间线
- `3abab1e` 建立 Android 小组件展示契约
- `fcd8c5d` 合并 Android 小组件刷新请求
- `9db4639` 重做 Android 单凭证小组件
- `e3128f3` 支持 Android 总览小组件配置
- `3e9a51e` 清理 Android 小组件旧渲染路径

## macOS 验证

### 自动化测试

命令：`scripts/test.sh`

结果：PASS

证据：`Executed 670 tests, with 0 failures (0 unexpected)`

### Release DMG

命令：`scripts/build-dmg.sh`

结果：PASS

产物：

- `build/dist/MyToken.dmg`，约 9.4 MB
- Release App：`build/DerivedData/Build/Products/Release/MyToken.app`
- Widget extension：`MyToken.app/Contents/PlugIns/RoutinUsageWidget.appex`

校验：

- App `CFBundleShortVersionString` 为 `5.6.2`
- Widget extension point 为 `com.apple.widgetkit-extension`
- 主 App 与 widget extension 均声明 App Group `group.cc.idickies.mytoken.widgets`

### 未覆盖的 macOS GUI 验收

未执行真实桌面拖放和截图验收。

原因：`scripts/build-dmg.sh` 固定使用 `CODE_SIGNING_ALLOWED=NO`，当前产物未签名，不能可靠注册进 macOS 小组件库。需要在有开发者签名和本机 GUI 会话时复验：

- Small / Medium / Large 单凭证组件
- Medium / Large 总览组件
- 浅色和深色桌面
- 长凭证名、长套餐名和异常状态
- 点击组件打开对应凭证

## Android 验证

### 自动化测试

命令：`./android/gradlew -p android test`

结果：PASS

证据：`BUILD SUCCESSFUL`，482 个 Gradle 任务通过。

### Release 构建

命令：`./android/gradlew -p android :app:assembleRelease`

结果：PASS

产物：

- `android/app/build/outputs/apk/release/app-release-unsigned.apk`，约 74 MB

### 未覆盖的 Android 真机验收

未执行真机或模拟器桌面拖放和截图验收。

需要在安装 Release 包后复验：

- 2x2 单凭证组件
- 4x2 单凭证组件
- 4x2 总览组件
- 4x3 或更大尺寸组件
- 浅色、深色和系统字体放大
- 旧版本升级后旧组件 ID 与凭证选择保留
- 连续手动刷新时上游请求去重

## 已覆盖逻辑

自动化测试覆盖：

- Routin、DeepSeek、GLM、Volcengine、NewAPI、Command Code、Xiaomi MiMo、OpenCode Go 的小组件指标顺序
- 固定主指标存在和缺失
- Xiaomi 完整分组 Token 文本
- macOS 快照原子写入、损坏回退和敏感材料排除
- macOS 单凭证与总览配置枚举
- macOS Timeline 重置、到期和缓存时间点
- Android v2 配置迁移和手动总览最多 6 个凭证
- Android 同凭证并发刷新只请求上游一次
- Android 单凭证、总览和刷新动作构建
- Android 状态文案、单行文本策略和旧 RemoteViews 路径移除

## 结论

自动化测试、macOS DMG 构建、Android Release 构建和扩展嵌入校验均通过。真实桌面拖放截图属于未覆盖项，原因是本轮缺少签名后的 macOS 产物和真实 Android 设备会话；不能以未签名构建替代该项结论。
