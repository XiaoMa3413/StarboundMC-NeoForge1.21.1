# HUD 动画语言与实施记录

基线：`codex/hud-ar-refactor`，`7cfc2ce`。2026-09-22 按用户完整动画计划实施；具体节奏授权实现方确定。此任务不是 H6 任务内容开发。

## Design Contract

- **SCREEN / PURPOSE**：Starbound HUD；观察世界时快速识别新信息、目标身份和生存风险。
- **INTERACTION FANTASY**：正在运行的个人视觉系统；建立方位、捕获真实目标、报告风险，平时安静。
- **MODE / PRIMARY VISUAL**：Experience。世界优先，危险或当前目标短暂获得注意力。
- **INFORMATION PRIORITY**：紧急生存风险 → 当前目标与方位 → 普通遥测 → 通信与临时系统状态。
- **FLOW**：信息出现 → 短暂建立/识别 → 稳定呈现 → 状态变化时有意义地提示 → 收起或记忆。
- **SPATIAL ORGANIZATION**：保留已确认双曲线及现有布局；World AR 为独立平面世界投影；NOVA 为左下独立通信层。
- **ART / GENERIC UI**：复用现有细线、文字、警告图标、Portrait；无新增面板、装饰贴图或设置页面。
- **MOTION**：Visor 入场小于 1 GUI px 的收敛位移；Compass 中央参考先建立，刻度由中央向外建立。无缩放弹跳、无持续呼吸。Glow 只作短暂强调，随后回到 8%。
- **AR**：短暂信号 → 锁定几何 → 名称与读数 → 稳定。中心位置始终由当前世界矩阵直接计算。已识别目标快速恢复；身份变化过渡不受距离读数变化触发。
- **SURVIVAL**：数字立即真实；恶化方向立即跟随，恢复条缓慢追赶。危险是短提醒与安静间隔；O₂ 使用警告图标，冷热使用条带端点，避免所有读数一起闪烁。
- **BOOT / NOVA**：保留真实状态与个人回执，替换旧中央框与右上滚动日志。短暂的中央光学参考 → 视觉恢复 / Compass 建立 → Core 无响应 / 本地 AR 捕获 → Safe Mode 自动收起。每次只显示两行能力信息，放在准星下方，Compass 保持正常位置。Core 恢复使用同一区域提示接入与同步；已经上线的世界只做个人初始化。NOVA 在系统提示退出后开始独立通信。
- **DO NOT**：改变真实目标投影、曲率、HUD 光学滞后或 EPP 规则；常驻扫描/旋转/glitch；为动画每帧重建网格。
- **IMPLEMENTATION CONTRACT**：时钟、组件 transition、风险反馈、AR 记忆与 camera lag 各自独立；最后组合姿态。所有开发开关默认关闭。

## 架构审查（实施前记录）

1. `ArWorldRenderer.render` 目前由 `drawNavigation` 调用，受舰船维度和导航 fade 限制。需要移为独立 HUD pass，由 Provider 决定可提供的信息。
2. Provider 的 `label` 含距离/高差。识别状态不能以整段标签字符串作为指纹，需要明确的稳定身份/识别内容；移动每帧更新的读数不应重播识别。
3. 当前 O₂ 危险图标为持续正弦闪烁，恢复条也持续呼吸。替换为事件提醒与间歇提醒，数字保持稳定。
4. `HudVisorProjection` 的网格键含布局横向中心及缩放。组件动画通过目标 pose 平移及 shader glow 参数合成，保持网格键不变。
5. NOVA 已有标点停顿、按中英文字符分配的流式预算、暂停与 H5 排队。优先保留，检查其入场是否与暂停一致，避免额外拖慢正文。

## 实施与审计清单

以下覆盖用户原计划的全部范围，不能用某一阶段测试通过代替最终完成。

| 阶段 | 对应原计划 | 验证证据 | 状态 |
|---|---|---|---|
| A：时钟、组件建立、Compass、可控 glow | 1–11、45–49 | 时钟/组件测试；两种宽度、GUI 2/3/4 明暗背景建立/稳定阶段 GPU 检查；动画不改变网格键 | 已完成 |
| B：AR 独立入口、记忆与识别、边缘导航、优先级 | 12–22、50 | 识别/重入/身份/缓存/长期告警与标签碰撞测试；GPU 矩阵及真实普通世界独立 AR pass | 已完成 |
| C：Survival 真实值/显示值与风险反馈 | 23–30 | 真值/恢复/帧率与间歇提醒测试；正式 renderer 在两种宽度验证普通/警告/危险/critical/恢复 | 已完成 |
| D：Boot / NOVA polish | 31–44 | Boot/通信测试；中英文全流程 GPU 场景；真实苏醒、菜单内 ONLINE、回执及重连 | 已完成 |
| E：整体复核、打包与交付 | 46–62 | 656 JUnit、44 GameTest、六次客户端矩阵启动、真实存档 13 项断言、正常 JAR 无测试入口；完整交付报告 | 已完成 |

最终视觉矩阵：1920×1080、2560×1080，GUI 2/3/4；Boot、Safe Mode、Compass、EVA；O₂ 普通/警告/critical、Cold、Heat；Ship、Relay、多目标、屏外、身后；快转、暂停、F1、菜单、第一/第三人称；NOVA 通信。区分纯状态测试、合成 GPU 检查与真实世界运行证据，不把合成画面称作完整游玩验收。

## 交付说明

最终动画语言、关键取舍、主要类、生命周期、告警、测试证据、调参项与性能边界统一见 [HUD 动画交付与验证](hud-animation-validation.md)。下方保留各阶段的实施历史。

阶段 A 记录：`tmp/hud-animation-components-smoke.log`；截图 `run-hud-visor-smoke/screenshots/hud-visor-1920x1080-*-entry-*.png` 及稳定帧。入场约 67 ms 为中央参考，200 ms 为邻近刻度，700 ms 已稳定。GUI 2/3/4、明暗背景及 GL 状态/资源重建检查通过，使用合成客户端内容。开发开关 `-PhudNoTransitions` 关闭额外建立效果，`-PhudAnimationSpeed=0.25` 放慢新动画时钟，不改变服务端规则或 Boot 时序。

阶段 B：AR 单独注册 HUD pass，Provider 继续负责维度/EVA 条件；Compass 仅根据 AR 目标存在性提供方位参考，不控制 AR 渲染。`ArTarget.identificationKey` 表示稳定识别身份，动态 label 中的距离/高差不会重播识别。Relay ID 包含 encounter origin，避免不同遭遇共享记忆。缓存最多 128 项，目标连续消失 30 秒后删除；世界/连接变化清空，短暂离开或相机切换保留已识别身份。首次识别约 0.55 秒，重新进入约 0.12 秒。

视觉审查移除了重叠交叉淡入的名称替换：旧文字先退 0.1 秒，新文字再显 0.14 秒。紧急 Warning 优先领取唯一 attention cue，持续 0.6 秒后安静至 3.5 秒周期结束；普通 POI 无重复 cue。布局只挪标签，不挪世界标记。`-PhudArStates` 显示开发阶段状态。

B 的合成 GPU 日志：`tmp/hud-animation-ar-smoke.log`、`tmp/hud-animation-ar-wide-smoke.log`。1920/2560×1080、GUI 2/3/4 明暗背景中检查捕获、稳定、身后边缘、重新进入及身份替换。早期 harness 把平面 GUI 的 shader 选择误当成 compositor 状态泄漏，已将两类绘制按各自契约检查：compositor 检查 shader/FBO 恢复，AR 检查 FBO/GL 错误；没有移除任何相应检查。54 项测试包含 8 项 AR 视觉状态测试，原投影/收集器/Boot/几何/运动测试全部保留。

阶段 C：`HudSurvivalRenderer` 从 HUD Root 提取，正式 HUD 与渲染检查共用；数字直接显示 snapshot 真值。`SurvivalFeedback` 对恶化方向立即同步条带，仅对恢复作指数追赶（O₂ 0.28 秒、Cold 0.45 秒、Heat 0.3 秒响应时间）。警告图标保持可见，短暂强调后回到静态；危险提醒间隔 4.5 秒，critical 2.8 秒，普通 caution 只在升级时提示一次。O₂ 使用 0.35 px 内的图标位移；冷热只强调条带下/上端点，不移动字形。入场光晕与危险光晕取较大值，不叠加放大。

58 项 HUD 测试通过，新增 4 项生存动画测试覆盖真实恶化、恢复边界、30/60/144 FPS、暂停及长安静间隔。`tmp/hud-animation-survival-smoke.log` 记录 1920×1080、GUI 2/3/4 明暗背景中使用正式 Survival renderer 的状态切换、阶段截图、GL/FBO 检查；输出 `entry-70` 为 critical、`entry-102` 为恢复。服务器氧气和暴露规则未改动。

阶段 D：Boot 改为准星下两行能力恢复提示和短暂校准参考，移除中央框、滚动日志及 Compass 下移。Safe Mode 提示退出后再开始 N.O.V.A.；ONLINE 完成必须来自服务器，并在可见呈现结束后确认个人回执。N.O.V.A. 的入退场与打字共用时钟，远程小肖像减少持续装饰运动。最长英文、中文、个人初始化和完整退场均经过客户端 GPU 检查。

阶段 E：2026-09-23 最终构建、656 项 JUnit、44 项服务器 GameTest 全部通过。1920/2560×1080、GUI 2/3/4 明暗背景与中英文呈现矩阵完成；独立实际存档完成 13 项状态断言及世界 AR、菜单、F1、第三人称、重连检查。正式 JAR 不含开发检查入口和测试结构。具体日志、视觉复核、调参建议与未覆盖的人工联机/长时间体验边界见 [交付与验证](hud-animation-validation.md)。
