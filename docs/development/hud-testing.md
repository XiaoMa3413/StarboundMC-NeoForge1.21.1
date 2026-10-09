# HUD / AR 开发验证

当前合同见 [HUD reference](../reference/hud.md)。以下是运行方法，不代表这些检查在每次提交都已重新执行。要求 Java 21；Gradle 缓存与 Java 路径按本机环境配置。

```powershell
.\gradlew.bat test build
.\gradlew.bat runClient -PhudVisorSmoke -PhudPresentationSmoke -PhudVisorSmokeWidth=1920 -PhudSmokeLanguage=zh_cn
.\gradlew.bat runClient -PhudVisorSmoke -PhudVisorSmokeWidth=2560
.\gradlew.bat runClient -PhudWorldSmoke
.\gradlew.bat -PshipGameTests runGameTestServer
```

## 检查矩阵与证据

- 合成 GPU 场景：1920/2560 宽、GUI Scale 2/3/4、中英文、明暗背景；检查最长通信全文/退场、Boot、指南针、生存风险/恢复、近邻标签避让、屏外/重入/身份变化、GL 状态与资源释放后重建。语言参数可改为 `en_us`。
- 实际世界夹具：新玩家苏醒与 Safe Mode、终端/Core 恢复、菜单中接收 ONLINE、服务端回执、跨维度 AR、F1/暂停/第三人称/相机实体/FOV、退出再加入。World smoke 每次创建独立命名的新存档，不打开 `run/saves` 玩家存档。
- 图像位于 `run-hud-visor-smoke/screenshots/` 和 `run-hud-world-smoke/screenshots/`；后者的断言摘要为 `run-hud-world-smoke/hud-world-passed.txt`。同时记录本次命令、退出状态与实际日志，旧报告数量不代表当前通过情况。
- JUnit 检查时钟、几何、收集、状态缓存与协议；服务器 GameTests 检查个人状态隔离、保存/死亡继承及权威快照。它们不能代替两台真实客户端、长期人工游玩或自定义字体/窗口验收。
- 合成场景可并列极端风险读数以检查布局，不代表实际玩法允许这些组合；GPU 合成与实际存档也不能混称完整人工验收。

## 调试与边界

`-PhudNoTransitions` 关闭新增组件建立/AR 捕获/风险脉冲；`-PhudAnimationSpeed=0.25` 放慢相关视觉时钟；`-PhudArStates` 显示识别阶段。`-PhudCalibrationGrid` 和 `-PhudVisorNoGlow` 可对照曲率/辉光。这些参数不改变服务器规则或 Boot/通信回执时序。

开发夹具仅通过相应 smoke 开关加入构建；正式 JAR 应排除夹具与测试结构。两客户端、自定义字体、窄窗口、长时间体验和大量 POI profiling 的未完成项统一见 [current-work](../current-work.md)。
