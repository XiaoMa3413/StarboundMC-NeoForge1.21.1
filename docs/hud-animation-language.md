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
- **BOOT / NOVA**：保留 Boot 状态机及真实服务端状态；用同一套 Compass / AR 建立过程收尾。NOVA 保留身份先于正文、标点停顿与独立阅读节奏。
- **DO NOT**：改变真实目标投影、曲率、HUD 光学滞后或 EPP 规则；重写 Boot；常驻扫描/旋转/glitch；为动画每帧重建网格。
- **IMPLEMENTATION CONTRACT**：时钟、组件 transition、风险反馈、AR 记忆与 camera lag 各自独立；最后组合姿态。所有开发开关默认关闭。

## 架构审查

1. `ArWorldRenderer.render` 目前由 `drawNavigation` 调用，受舰船维度和导航 fade 限制。需要移为独立 HUD pass，由 Provider 决定可提供的信息。
2. Provider 的 `label` 含距离/高差。识别状态不能以整段标签字符串作为指纹，需要明确的稳定身份/识别内容；移动每帧更新的读数不应重播识别。
3. 当前 O₂ 危险图标为持续正弦闪烁，恢复条也持续呼吸。替换为事件提醒与间歇提醒，数字保持稳定。
4. `HudVisorProjection` 的网格键含布局横向中心及缩放。组件动画通过目标 pose 平移及 shader glow 参数合成，保持网格键不变。
5. NOVA 已有标点停顿、按中英文字符分配的流式预算、暂停与 H5 排队。优先保留，检查其入场是否与暂停一致，避免额外拖慢正文。

## 实施与审计清单

以下覆盖用户原计划的全部范围，不能用某一阶段测试通过代替最终完成。

| 阶段 | 对应原计划 | 验证证据 | 状态 |
|---|---|---|---|
| A：时钟、组件建立、Compass、可控 glow | 1、2、9、12、14–16 | 46 项 HUD 测试；1920×1080、GUI 2/3/4 明暗背景、建立/稳定阶段 GPU 检查通过；动画不改变网格键 | 已完成，最终矩阵仍需复核 |
| B：AR 独立入口、记忆与识别、边缘导航、优先级 | 3–6、13 | 54 项 HUD 测试；宽屏/超宽屏分阶段 GPU 检查；非舰船独立 pass 仍待真实世界检查 | 实现完成，整体验证待做 |
| C：Survival 真实值/显示值与风险反馈 | 7、8 | 恶化立即响应、恢复平滑、危险节奏、暂停/隐藏测试与截图 | 待实施 |
| D：Boot / NOVA polish | 10、11 | 保留 Boot/通信测试，启动顺序、暂停/排队、长文本与中英文检查 | 待实施 |
| E：整体复核、打包与交付 | 17–23 | 完整测试、客户端场景矩阵、无持续无意义运动、正常 JAR 无测试入口 | 待实施 |

最终视觉矩阵：1920×1080、2560×1080，GUI 2/3/4；Boot、Safe Mode、Compass、EVA；O₂ 普通/警告/critical、Cold、Heat；Ship、Relay、多目标、屏外、身后；快转、暂停、F1、菜单、第一/第三人称；NOVA 通信。区分纯状态测试、合成 GPU 检查与真实世界运行证据，不把合成画面称作完整游玩验收。

## 交付说明待核对

最终说明需要覆盖：动画语言、保留/替换建议及原因、核心类、AR 生命周期、危险提醒、独立 AR pass、测试与结果、可调参数、架构发现、性能成本。完成前逐项审查实际实现与证据。

阶段 A 记录：`tmp/hud-animation-components-smoke.log`；截图 `run-hud-visor-smoke/screenshots/hud-visor-1920x1080-*-entry-*.png` 及稳定帧。入场约 67 ms 为中央参考，200 ms 为邻近刻度，700 ms 已稳定。GUI 2/3/4、明暗背景及 GL 状态/资源重建检查通过，使用合成客户端内容。开发开关 `-PhudNoTransitions` 关闭额外建立效果，`-PhudAnimationSpeed=0.25` 放慢新动画时钟，不改变服务端规则或 Boot 时序。

阶段 B：AR 单独注册 HUD pass，Provider 继续负责维度/EVA 条件；Compass 仅根据 AR 目标存在性提供方位参考，不控制 AR 渲染。`ArTarget.identificationKey` 表示稳定识别身份，动态 label 中的距离/高差不会重播识别。Relay ID 包含 encounter origin，避免不同遭遇共享记忆。缓存最多 128 项，目标连续消失 30 秒后删除；世界/连接变化清空，短暂离开或相机切换保留已识别身份。首次识别约 0.55 秒，重新进入约 0.12 秒。

视觉审查移除了重叠交叉淡入的名称替换：旧文字先退 0.1 秒，新文字再显 0.14 秒。紧急 Warning 优先领取唯一 attention cue，持续 0.6 秒后安静至 3.5 秒周期结束；普通 POI 无重复 cue。布局只挪标签，不挪世界标记。`-PhudArStates` 显示开发阶段状态。

B 的合成 GPU 日志：`tmp/hud-animation-ar-smoke.log`、`tmp/hud-animation-ar-wide-smoke.log`。1920/2560×1080、GUI 2/3/4 明暗背景中检查捕获、稳定、身后边缘、重新进入及身份替换。早期 harness 把平面 GUI 的 shader 选择误当成 compositor 状态泄漏，已将两类绘制按各自契约检查：compositor 检查 shader/FBO 恢复，AR 检查 FBO/GL 错误；没有移除任何相应检查。54 项测试包含 8 项 AR 视觉状态测试，原投影/收集器/Boot/几何/运动测试全部保留。
