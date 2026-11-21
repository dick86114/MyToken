# MyToken Android 设计稿

三份自包含的高保真 HTML，直接用浏览器打开；所有 CSS/JS/供应商 Logo 已内联，无需构建或联网。

| 文件 | 内容 |
| --- | --- |
| `home-hifi.html` | 首页：简洁 / 完整双列对照，浅色 / 深色切换，卡片类型与 token 说明 |
| `cards-spec.html` | 卡片系统：8 种内容原型、6 种状态、颜色 / 字号 / 间距 / 动效规格 |
| `pages.html` | 四页：凭证列表 / 新增凭证 / 设置 / 卡片详情；可切换标签、主题，新增页可打开供应商 sheet |

这些是设计稿，不是产品代码。实现规格与决策见
`docs/superpowers/specs/2026-11-21-mytoken-android-visual-refresh-design.md`。

## 与实现同步（2026-11-21）

三份 HTML 已按 Android 落地结果回写，后续改动以这里和实现规格为准：

- 简洁卡高度随内容自适应，首页简洁网格用瀑布流（`LazyVerticalStaggeredGrid`，HTML 用 CSS columns 模拟）。
- 重置卡 chip：简洁 17px 仅数字，完整 19px「重置卡 N」；主指标 chip 换行到标签下方，窗口 chip 紧跟窗口标签。
- 供应商 logo 保持 24px（简洁）/ 28px（完整），不再放大。
- CommandCode 简洁卡只保留窗口细条，不再重复余额行。
- 余额构成图例在简洁卡纵向排列（行距 8px）；完整卡图例与 facts 间距同步加大。
- 凭证列表入口图标为「两长一短」三横线；健康标尺到卡片区间距 14px。
