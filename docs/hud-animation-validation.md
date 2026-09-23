# HUD / AR 动画交付与验证

日期：2026-09-23。分支：`codex/hud-ar-refactor`。接续 `agentrouter2`、`agentrouter3` 的工作，对照用户提供的《StarboundMC HUD - AR 动画与视效增强 — Astra 完整指导 Plan》完成。范围为 HUD 表现；不扩展 H6 任务内容、Scanner、EPP 规则或飞船玩法。

## 1. 最终动画语言与设计判断

世界始终是主体。Visor 表达个人设备的显示建立与稳定，AR 表达世界对象的发现与识别，N.O.V.A. 表达独立通信。动画只在新信息、能力恢复、身份变化和风险升级时强调；稳定 Compass、普通 POI 和安全遥测没有持续呼吸。

保留已确认的全屏共享双曲线、清晰字形与短促 optical lag。组件进入位移小于 1 GUI px，避免放大、弹跳和明显滑入。没有加入比例动画：已有曲率与相机反馈足够表达光学设备，额外缩放会降低小字稳定性。

AR 不平滑目标屏幕坐标，也不保存旧屏幕位置。隐藏后不会在旧位置做淡出，因为那会产生脱离真实对象的标记。短暂离开保留识别记忆，重新可见时快速恢复。

## 2. 删除与替换的旧表现

- 移除中央启动框、右上六行滚动状态、扫描进度线、启动期间 Compass / EVA controls 下移；删除对应 20 个旧中英文语言键。
- Boot 改为准星下方两行短提示，一次只报告一项有用能力；减少一个独立 prompt compositor。
- 移除 Survival 的持续正弦闪烁，改为升级提醒和长安静间隔。
- N.O.V.A. 远程 HUD 的入退场从独立墙钟动画改为通信时钟；小肖像移除扫描线、抖动、浮动缩放和循环装饰粒子，保留眨眼、视线、活动与语音文字反馈。终端大肖像保留原设计。

## 3. Boot 与 Core Link

真实状态仍为 `DORMANT → STARTING → SAFE_MODE → LINKING → ONLINE`；ONLINE 只能由服务端快照触发。

首次苏醒在现有服务端广播到达后开始。STARTING 共 96 tick，依次为视觉恢复、32 tick 时建立本地方位、58 tick 时报告核心无响应、72 tick 时开放 World AR、96 tick 时进入 Safe Mode。前 32 tick 只出现四条短校准参考，没有屏幕扫描。

Safe Mode 提示保留 30 tick、淡出 16 tick；随后 N.O.V.A. 开始通信。功能受限通过真实能力与本地终端指引体现，安全模式文字不常驻。重复离线快照不会提前消掉正在显示的提示。

Core 重启期间显示连接等待；服务端确认 ONLINE 后显示完成提示，停留 25 tick、淡出 20 tick。整个提示结束才发送个人回执。打开背包时核心仍可在服务端完成重启，但客户端不消耗呈现时钟，关闭背包后才呈现完成并确认。

已上线世界的新玩家走 40 tick 的个人初始化，再显示连接完成，整个过程不会宣称核心离线。已有 `HUD_CORE_LINK_PRESENTED` 的玩家重连直接恢复稳定 ONLINE。服务端回执校验、schema 2 迁移、死亡继承和连接重置均保留。

## 4. Compass 与组件过渡

中央 Heading reference 先出现，邻近刻度随后建立，外围刻度最后补齐，总建立时间约 0.52 秒。Heading 的实际转动仍直接来自相机。中心刻度最清晰，外围连续衰减；稳定后只有真实方位变化。

组件过渡、camera lag 和布局各自独立。进入偏移通过最终绘制参数组合，不进入网格缓存键。原有可见性 fade 与 Survival 安全停留保留：先确保解除危险后的恢复信息有时间读完，再收起，而不是倒放建立动画。

## 5. World AR 识别与生命周期

`ArVisualStateCache` 按 `ArTarget.id()` 缓存。首次出现为短信号点与断开的锁定笔画，约 0.3 秒完成锁定，随后名称建立，约 0.55 秒形成识别记忆。重新进入视野约 0.12 秒恢复，不重播扫描。屏外箭头只有一次轻微笔画收敛；仅看见箭头不算已经识别对象。

`identificationKey`、category、guidance 区分身份变化与实时距离变化。身份变化时旧名先退 0.1 秒，再在 0.14 秒内显示新名，避免两段文字叠在一起。Signal 与完整目标有不同笔画闭合度。当前 Provider 的名称和距离仍作为紧凑的一行建立，没有添加逐字扫描或多行慢速读数。

连续不再存在 30 秒后清理，最多记住 128 个对象。世界/连接变化清空；同世界的相机切换保留已识别身份。暂停、菜单和 F1 不推进动画时钟。周期性 warning 使用独立循环时间，修复了原来 60 秒后可能停在脉冲中段的问题；90 秒回归测试覆盖此情况。

`ArWorldRenderer` 是单独注册的 HUD pass，目标由 Provider 决定，`navigationOpacity`、EVA 状态和舰船维度不再是它的渲染总开关。Ship / Relay Provider 自己仍有合理的舰船/EVA条件；将来的普通星球 Provider 可以直接使用同一个 pass。当前测试 Provider 仅存在于 `src/renderTest`，没有向正式游戏增加假目标。

重要目标由 category、priority 决定，同一帧只有一个目标获得额外 attention cue，warning 优先；普通 POI 没有重复动画。目标按优先级领取标签空间，`ArLabelLayout` 复用最多八个已占用矩形，依次尝试默认、下移、上移。容不下的低优先级文字隐藏，世界标记仍保留；绝不为了文字避让移动世界锚点。

## 6. Survival 与动态 Glow

O₂ 沿用真实规则：大于 30% 为安全，30% 以下 caution，15% 以下 danger，5% 以下 critical，耗尽/无 EPP 在无空气环境下按最高视觉级别处理。冷热使用既有 exposure 读数的 25 / 50 / 75 分级。

数字立即显示快照真值。条带恶化也立即跟随，只有恢复使用指数追赶：O₂ 约 0.28 秒、Cold 约 0.45 秒、Heat 约 0.3 秒。不会把氧气剩余量显示得比真实更安全。

Caution 升级时提醒一次；danger 每 4.5 秒短提醒；critical 每 2.8 秒短提醒，之间保持静态。最终采用单次短强调配不同间隔，没有叠加双脉冲，以降低同时出现冷热风险时的运动密度。O₂ 只轻动警告图标（0.35 px 内），Cold 强调条带下端，Heat 强调上端，数字不跳动。

Glow 基础强度为 0.08，组件建立暂时增加最多 0.035，危险提示增加最多 0.055。进入与告警取较大值，避免叠加。Boot 使用 0.04。仍复用原 TextureTarget、halo 与 VertexBuffer；没有为每个 AR 对象创建纹理或 shader。

## 7. N.O.V.A.

仍为左下独立平面通信层，不经过 Visor 曲率、optical lag 或 World AR lock。保留角色身份、语言本地化和按字符宽度分配的流式预算；Latin 小批量最多两字符，CJK 通常一字符。修正批次跨标点的问题，段落增加短停顿。

入场先建立身份，再出现正文。入退场最多 3 GUI px 的横向收敛，与打字共用暂停时钟，解决菜单关闭时入场动画已经自行播放完的问题。最长现有英文通信（303 字符）纳入最终 GPU 验证，检查全文与完整退场；同时检查新玩家个人初始化。

## 8. 主要类

修改：`StarboundHudLayer`、`HudClientEvents`、`HudBootController`、`HudBootStatusRenderer`、`HudVisorProjection`、`VisorCompassRenderer`、`ArWorldRenderer`、`ArTarget`、`ArTargetCollector`、`RelayPoiProvider`、`Stage2ClientRegistrar`、`ClientNovaBroadcastState`、`NovaBroadcastHudLayer`、`NovaBroadcastHudRoot`、`NovaBroadcastTimeline`、`NovaPortraitElement`。

本轮相对动画计划前的基线新增：`HudAnimationClock`、`HudComponentPresentation`、`SurvivalFeedback`、`HudSurvivalRenderer`、`ArVisualStateCache`、`ArMarkerRenderer`、`ArLabelLayout`。测试专用新增/扩展：`HudVisorRenderSmoke`、`NovaBroadcastRenderFixture`、`HudWorldRenderSmoke`。

## 9. 测试与证据

最终批次全部通过。以下分别记录逻辑测试、实际存档和合成 GPU 场景的证据。

- JUnit：656 项通过；包括所有原有几何、运动、Boot、收集器和投影测试。新增组件完成/安静终态、30/60/144 FPS、暂停/长帧时钟、AR 识别/重入/身份变化/TTL/容量/注意力、生存真值与恢复、八标签碰撞与空间耗尽、通信入退场与标点测试。旧架构测试改为检查独立 AR 注册和真实调用关系，不再写死已经删除的 Boot 美术参数。
- 真实世界：`tmp/hud-final-world-confirmed.log` 成功；`run-hud-world-smoke/hud-world-passed.txt` 记录 13 项断言。独立新存档实际运行苏醒和 Safe Mode、终端 AR、Core 重启、背包中收到 ONLINE、服务端回执写入、跨维度普通世界 AR、金块目标、转头/屏外/身后/重入/分类、F1/暂停/第三人称/相机实体/FOV 110，以及实际退出存档再加入。
- GPU 矩阵：1920×1080 与 2560×1080、GUI 2/3/4、亮/暗背景、中/英文；正式 Boot / N.O.V.A. renderer 检查全文、入退场与个人初始化。正式 Compass / Survival / AR artwork 检查八个近邻目标、捕获、屏外、身份变化、正常/告警/critical/恢复、FBO/GL 状态和缓存资源释放后重建。四次 presentation 启动、两次组件启动全部成功，日志为 `tmp/hud-final-presentation-{1920,2560}-{zh_cn,en_us}.log` 与 `tmp/hud-final-components-{1920,2560}.log`。
- 服务端 GameTest：44 项全部通过，日志 `tmp/hud-final-gametest.log`。包括个人 HUD 回执存档/死亡继承、不同玩家状态隔离，以及未打开终端也能收到权威快照。
- 正式构建：不带测试开关的 `gradlew build` 成功，最终 JUnit XML 为 656 tests / 0 failures / 0 errors / 0 skipped，日志 `tmp/hud-final-build.log`。产物为 `build/libs/starboundmc-0.1-alpha.jar`；检查 ZIP 内容确认正式 Boot / AR 类存在，开发渲染入口、GameTest 类与 `shuttle_test_empty.nbt` 均未打入。SHA-256：`85f1f2387c0c2fa64a1264721b0bc91840677b1ca02eff4e398bf97cefb2092e`。

画面证据位于 `run-hud-visor-smoke/screenshots/` 与 `run-hud-world-smoke/screenshots/`。前者是客户端真实 GPU 上的合成场景，后者是集成服务器实际存档；两者不能混称为完整人工游玩验收。所有辅助源仅通过 `-PhudVisorSmoke` 或 `-PhudWorldSmoke` 纳入开发启动。

逐帧视觉复核确认：Safe Mode 提示没有遮住准星和终端目标；普通世界金块目标在转头、屏外与重入时保持世界锚点；八个近邻目标的文字分行避让；最长英文通信全文可读并完整退场；超宽屏 GUI 4 的危险读数在亮背景下仍可辨认。合成画面故意并排显示 Cold / Heat 等极端状态以检查布局，不能据此推断实际玩法中的状态组合。

## 10. 调参、边界与性能

- 可调：Compass 0.52 秒建立速度；AR 0.55 秒首次识别 / 0.12 秒恢复；Survival 告警间隔；Boot 提示持续时间和准星下方间距；组件 glow 强度。当前值以稳定、可读和低干扰优先。
- `-PhudNoTransitions` 关闭新增组件建立、AR 捕获、风险脉冲；`-PhudAnimationSpeed=0.25` 放慢这些视觉时钟；`-PhudArStates` 显示 AR 阶段。它们不改变服务端规则，也不改 Boot/通信的回执时序。原有 `-PhudCalibrationGrid`、`-PhudVisorNoGlow` 仍可用于对比。
- 尚未进行两台真实客户端联机和几十分钟人工游戏体验。多人个人状态隔离、保存/死亡继承由服务器测试覆盖，客户端晚加入由控制器与 GPU 场景覆盖。自定义字体/资源包和少于常用窗口宽度的排版仍需按实际环境复核。
- AR 仍使用每帧 Provider 收集和排序，当前上限八目标；识别状态是缓存的。未来真正接入大量 POI 时，应先做 profiling，再决定快照收集频率或空间查询优化。
- compositor 保留现有每组件离屏绘制、halo 采样与 GL 状态查询成本；此次没有宣称测得 FPS 提升。动画不修改 mesh key、不增加逐帧 TextureTarget 创建；标签避让不逐帧分配集合。Boot 少用一套 prompt target。没有发现本轮新增的无界缓存或服务器权威倒置。
- 最终自查：每个新增动画分别表达显示建立、目标识别、身份变化、风险升级、能力恢复或通信接入；普通环境保持安静。没有加入全屏 glitch、CRT、RGB 分离、强 Bloom、大幅摇晃或无意义持续扫描。

## 复现命令

```powershell
.\gradlew.bat test
.\gradlew.bat runClient -PhudVisorSmoke -PhudPresentationSmoke -PhudVisorSmokeWidth=1920 -PhudSmokeLanguage=zh_cn
.\gradlew.bat runClient -PhudVisorSmoke -PhudVisorSmokeWidth=2560
.\gradlew.bat runClient -PhudWorldSmoke
.\gradlew.bat -PshipGameTests runGameTestServer
.\gradlew.bat build
```

语言切换为 `en_us`，宽度在 1920/2560 之间替换即可重复全部画面组合。World smoke 每次创建独立命名的新存档；它不会打开或更改 `run/saves` 中的玩家存档。
