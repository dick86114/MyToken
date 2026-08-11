# 第 3 项完成报告

## 失败验证

先在 `ProjectBootstrapTests` 新增设置页和更新日志视图的测试并运行 `scripts/test.sh`。测试套件执行 211 项，其中新增断言失败：设置页缺少“当前版本”和 `UpdateNotesView(notes: update.notes)`，更新日志资源未同步，符合预期的红灯阶段。

## 通过验证

实现 `UpdateNotesView` 后再次运行 `scripts/test.sh`，211 项测试全部通过，0 个失败。视图会渲染 Markdown；空白日志显示“此版本未提供更新日志”；解析失败时显示原始文本。设置页始终显示 `CFBundleShortVersionString`，发现更新时显示完整日志。

## 提交

提交信息：`feat: 在设置显示版本与更新日志`。

已使用该提交信息提交本项变更。

## 疑问

无。
