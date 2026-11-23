# MyToken Android 客户端

MyToken Android 是 macOS 菜单栏用量监控工具的移动端伴侣应用：从 Mac 扫码迁移
凭证后，在手机上查看各供应商的用量、余额、剩余额度与重置时间，接收用量提醒，
并支持 Routin 每日签到。

面向使用者的图文说明见 [`使用手册.md`](使用手册.md)。

## 系统要求

- **Android 10（API 29）或更高版本**。
- 迁移扫码、通知提醒和签到需要真机；模拟器可运行但相机与通知体验受限。

## 构建

前置条件：JDK 17、Android SDK（`ANDROID_HOME` 指向 commandlinetools），
本机可在 `android/local.properties` 写入 `sdk.dir`（该文件不入库）。

```bash
cd android
./gradlew test                          # 全模块 JVM 单元测试（Robolectric 覆盖 Compose UI）
./gradlew :app:assembleDebug            # 构建 Debug APK
./gradlew :app:assembleDebugAndroidTest # 编译 instrumented 测试 APK
./gradlew :app:connectedDebugAndroidTest # 需要已连接的真机/模拟器
```

## 模块结构

| 模块 | 职责 |
| --- | --- |
| `:app` | MainActivity、首页主轴与左右工具抽屉（左凭证/右设置）、子路由（详情/编辑器/迁移）、单凭证桌面小组件、手写依赖图 `AppGraph`、后台刷新代理与用量提醒分发 |
| `:domain` | 纯 Kotlin 模型与用例：`Credential`、`CredentialSecret`、`CredentialRepository`、`RefreshCredentialsUseCase`（含快照缓存读写与启动恢复） |
| `:data` | Room（凭证元数据 + usage snapshot 缓存）、Keystore 加密 secret 存储、DataStore 偏好/提醒状态 |
| `:core` | 传输加密原语（X25519+HKDF+AES-GCM）、AndroidKeystoreSecretStore、WorkManager 调度、提醒评估器、通知渠道 |
| `:provider` | 各供应商用量适配器（Routin/DeepSeek/GLM/火山方舟/NewAPI）与 `ProviderRegistry` |
| `:feature-home` | 首页用量仪表盘、详情页（含签到入口） |
| `:feature-credentials` | 凭证列表（启停/拖动排序）、凭证编辑器、独立于 Mac 的排序 DataStore |
| `:feature-settings` | 刷新/主题/通知/数据迁移/关于设置 |
| `:feature-transfer` | 扫码（CameraX + ML Kit）、局域网握手、解密、导入预览与事务导入 |

跨端迁移协议常量见 `shared/transfer-schema/wire-contract.md` 与
`shared/transfer-schema/transfer-schema-v1.json`。

## CI

- `.github/workflows/android-ci.yml`：`./gradlew test` 全模块 JVM 单元测试 +
  `:app:assembleDebugAndroidTest` 编译验证。`connectedAndroidTest` 需要带
  KVM/真机的 self-hosted runner（工作流内有注释说明如何启用）。
- macOS 侧继续由 `.github/workflows/ci.yml` 执行 `scripts/test.sh`。

## 发布配置审计

- 权限最小化：声明 `POST_NOTIFICATIONS`、`INTERNET`（正常权限，安装时授予）
  与 `CAMERA`；相机权限在 manifest 声明，扫码时经系统对话框授权。
- 云备份：`android:allowBackup="false"`，并以 `backup_rules.xml`
  （Android 11-）与 `data_extraction_rules.xml`（Android 12+）排除数据库、
  DataStore 与 `noBackupFilesDir` 作为纵深防御。Keystore 包裹的密文
  （`cred-*.bin`）本身因密钥不可导出也无法跨设备恢复。
- 发布构建（release）：无开发密钥、无测试 URL、无完整日志开关；已开启
  R8（`isMinifyEnabled` + `isShrinkResources`），发布前需在真机回归供应商
  凭证、刷新、分享、扫码导入、Widget 与通知，并留存
  `app/build/outputs/mapping/release/mapping.txt` 用于崩溃栈还原。
- 安装包体积：只打包 `arm64-v8a` 与 `armeabi-v7a`；原生库压缩进 APK
  （`useLegacyPackaging`），安装时解压到应用目录，以缩小下载体积。

## 发布通道

- `release-android.yml` 的 `release_channel` 选择 `preview` 时，构建产物以
  `android-preview-X-N` 独立标签、GitHub prerelease 形式发布，资产名带
  `-android-preview.N`；应用内更新与历史版本只识别正式 `vX` / `android-vX`，
  因此预览版不会推送给正式用户。
- 预览版不提交版本号到 master；测试通过后用相同 versionName / versionCode
  选择 `stable` 发布正式版，可直接覆盖安装预览包（需使用同一签名）。
- 日志：诊断输出只包含失败消息，不含凭证材料、密文或网页会话内容。

## 手工验收（待真机验证）

以下项目需要在 Android 10+ 真机上与当前 macOS 客户端联调确认：

- [ ] 全供应商迁移（含 accessKeyPair、已停用凭证）与重复导入（跳过/覆盖）。
- [ ] 首次刷新、下拉/手动刷新、断网重试、会话过期提示。
- [ ] 后台周期刷新与用量/失效提醒（含通知权限拒绝后的静默降级）。
- [ ] Routin 签到 Custom Tab 打开与回跳。
- [ ] Keystore 密文在应用更新/重启后的可读性。
- [ ] 手机竖屏可从左上角图标打开凭证抽屉，并在首页任意位置横滑切换左右抽屉；物理最边缘仍留给系统返回手势。
- [ ] 全应用使用统一语义字号层级：页面标题、卡片主数值、数据标签和辅助说明各只保留一个常用级别，大字体下说明文字不低于 10sp。
- [ ] 首页顶部使用单行工具条：左侧凭证入口、中央简洁/完整切换、右侧设置；不再显示“账户用量”标题和数量气泡，凭证抽屉标题下显示“已启用 X / Y”。
- [ ] 首页顶部工具条固定在内容顶部，不随卡片列表滚动。
- [ ] 更新提醒使用发光星星图标，星星做轻微呼吸闪烁；系统关闭动画时保持静态。
- [ ] 手机横屏和平板切换时仍使用双侧抽屉，首页按窗口宽度显示双列或三列卡片。
- [ ] 抽屉支持整页横滑打开和收起、遮罩、返回键；收起动画未结束立即反向滑动仍可响应，不会残留不可见遮罩或锁死首页。
- [ ] 凭证列表显示供应商 Logo，点击整行进入编辑，开关与更多菜单仍可独立操作。
- [ ] 首页健康短条顺序与卡片一致，带供应商 Logo；点击后目标卡片居中并弹跳浮动提示。
- [ ] 详情页顶部只保留凭证身份卡，不重复显示用量环或本周用量/请求次数摘要，完整指标仍在下方分区展示。
- [ ] 账户健康压缩为单行摘要与紧凑状态条，保留健康数、更新时间、说明入口和全部供应商状态。
- [ ] 首页顶部中央分段控件可切换简洁/完整模式，并正确播报选中状态。
- [ ] 简洁/完整切换不再使用中间硬边色块，改为底部滑动指示线，切换无竖向割裂线。
- [ ] 首页滚动到底后内容在最后一张卡片下方自然结束，不保留半屏空白。
- [ ] 简洁模式百分比卡使用 270° 细弧且无内层玻璃底板，1/2/3 个指标始终单行排列；周期与重置时间位于环下方，重置卡紧随周期；余额卡使用金额 + 构成条，值卡使用 2×2 发丝表格。
- [ ] 简洁模式过期状态显示为“待刷新”，长别名最多两行并与供应商 Logo 垂直对齐。
- [ ] 点击健康度短条时列表平滑滚动到目标卡片，随后卡片以较缓的节奏弹跳提示。
- [ ] 从凭证/设置抽屉进入新增、编辑或迁移页后返回首页，不残留旧抽屉方向；设置和左滑都能正常打开对应抽屉。
- [ ] 新增凭证、详情、编辑和迁移等二级页面使用横向推入过渡，返回时沿相反路径退出。
- [ ] 从首页进入详情等二级页面再返回时，保留进入前的首页滚动位置。
- [ ] 卡片别名字号按长度自适应，套餐到期时间位于别名下方；完整模式的配额型凭证使用非聚焦环双栏结构，窄卡宽时自动改为上下堆叠。
- [ ] Routin / GLM / 火山方舟 / OpenCode / Command Code / New API / 套餐卡均保留重置卡、状态与全部统计字段。
- [ ] DeepSeek / MiMo API 完整模式使用全宽余额主信息，模型数量收进金额行；MiMo 31 天用量位于下方，不再出现空栏和底部裁切。
- [ ] Command Code 的额度构成在独立全宽摘要行内单行展示，本周期消费与累计请求并列。
- [ ] 应用冷启动自动检查一次更新；发现新版本时首页设置入口显示提示点，且同进程内不会重复检查。
- [ ] 深色模式下首页、凭证抽屉和详情页的供应商 Logo 均保持清晰可辨。
- [ ] 说明、删除确认、刷新失败、日志导出和历史版本弹窗共用统一结构，明暗模式下分区清晰且操作可辨。
- [ ] 阔折叠展开后首页显示三列或按最小宽度回退，凭证页可拖拽排序。
- [ ] 分屏窗口在 600dp 和 840dp 附近缩放时不出现重复导航或横向溢出。
- [ ] 滚动时卡片不常驻 graphicsLayer；刷新中不再为每张卡叠加无限描边动画，高亮定位只为目标卡临时启用合成层。
- [ ] 大字体和深色主题下，抽屉标题、设置分组和迁移页面无文字重叠。
