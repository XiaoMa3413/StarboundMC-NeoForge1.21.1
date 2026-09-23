# StarboundMC 通用 HUD / AR 系统重构计划

> 2026-09-23 动画扩展：共享双曲线保持，World AR 已独立注册并具备识别记忆；Boot 已替换旧日志/中央框，N.O.V.A. 与风险反馈同步完善。最新表现和验证证据见 [HUD 动画交付与验证](hud-animation-validation.md)。以下 H4/H5 的旧时长、日志和布局描述保留为阶段历史；H6 任务内容仍未开始。

> 建议文件名：`docs/hud-ar-refactor-plan.md`
>
> 状态：H5 Core Link 已完成并推送；Visor 已改为采样全屏上下共享曲线，42 项 HUD 测试及宽屏/超宽屏独立客户端渲染检查通过，待实际游玩确认；暂不进入 H6
>
> 2026-09-21 最新修订：用户反馈右下局部曲线过紧，要求先定义横贯屏幕的上下双曲线，再由渲染采样对应片段；弧度细节已授权实施方确定。顶部中央低、两侧高，底部中央高、两侧低。右下读数采用宽缓下曲线右侧片段，Compass 与 EVA 共用上曲线，文字和图形一起弯折。保留独立组件纹理、清晰度、阅读高度、运动及 H5 布局。详见 [HUD 全屏双曲线设计](hud-local-curves-design.md)。全量《无人深空》方向和旧 [NMS 讨论稿](hud-nms-design.md) 不作为实施依据；NOVA、系统日志与 World AR 继续独立呈现。
>
> 目标：将当前以 EPP / EVA 为中心的 HUD 原型重构为 StarboundMC 全游戏通用的信息呈现系统，并为后续任务、探索、Space POI、Scanner 与环境系统提供统一基础。

---

# 1. 重构目标

现有 HUD 已经验证了以下体验：

- Visor 保留 main 对比中更舒适的文字整体变形与阅读高度；最新方案通过全屏共享双曲线统一弧度，右下仅取其中一段；
- 方位盘可用；
- 世界坐标 → 屏幕 AR 标记可用；
- 屏幕外 / 身后目标导航可用；
- 氧气、冷热等状态适合情境式淡入淡出；
- EVA 中 Ship / Relay 导航有效。

下一阶段不继续把功能堆入 `client.epp`，而是建立正式的通用 HUD / AR 架构。

最终结构：

```text
Starbound HUD
│
├─ Visor Layer
│  ├ Compass
│  ├ Oxygen
│  ├ Cold / Heat
│  ├ EVA Status
│  └ System Status
│
├─ World AR Layer
│  ├ Objective
│  ├ Ship Beacon
│  ├ Space POI
│  ├ Landing Beacon
│  ├ Interaction Target
│  └ Scanner Result
│
├─ Flat Communication Layer
│  └ N.O.V.A. Communication
│
└─ System Overlay
   ├ HUD Boot
   ├ Warp Flash
   └ Critical Effects
```

核心原则：

> HUD / AR 是 StarboundMC 的通用世界内信息呈现层。  
> N.O.V.A.、任务、EPP、Space POI、Scanner 等系统只负责提供信息，不各自实现一套独立导航逻辑。

---

# 2. 三种主要视觉空间

## 2.1 Visor Layer

属于玩家自身设备状态和基础导航的信息：

```text
Compass
O₂
Cold / Heat
EVA Status
System Status
```

视觉特征：

```text
统一的轻度光学风格
轻微光学滞后
导航与生存组件采样全屏上下共享曲线，系统文字保持平面
组件按情境淡入淡出
低强度 Glow
固定在玩家视野
```

这些元素共享克制、一致的 Visor 视觉语言。Compass、EVA 与生存读数的文字、刻度和图标一起沿对应的共享曲线映射；多行系统日志保留平面。文字清晰度与信息读取速度优先。

---

## 2.2 World AR Layer

绑定真实世界对象的信息：

```text
任务目标
Ship Beacon
Space POI
Landing Beacon
Interaction Target
Scanner Signal
```

视觉特征：

```text
无曲率
无 Visor drift
严格锁定世界位置
支持屏幕外方向提示
支持距离 / 高差
```

World AR 不参与 Visor 几何变形。

---

## 2.3 Flat Communication Layer

N.O.V.A. 通讯独立于 Visor。

例如：

```text
[N.O.V.A.]
核心系统已恢复。我正在评估飞船状态。
```

特点：

```text
平面
高可读性
不参与 Visor 曲率
不绑定世界位置
可以保留独立淡入 / 淡出和肖像动画
```

当前 `NovaBroadcastHudLayer` 可以继续作为独立平面 HUD，并逐步纳入统一 HUD 生命周期管理。

---

# 3. System Overlay

用于：

```text
HUD Boot
Warp Flash
Critical Warning Effects
剧情视觉效果
```

这些效果根据自身需求表现，不强制进入 Visor 曲面。

Calibration Grid 包含横贯屏幕的上下参考线与组件内采样网格，与文字共用实际几何，检查连续性和跟随运动。Warp Flash 保持独立屏幕 Overlay。

---

# 4. 现有 HUD 视觉优化与全屏双曲线

本轮保留现有信息组织：顶部 Compass、右下 O₂ / Thermal、右上 H5 系统状态与临时中央启动提示；NOVA 保持左下独立平面通讯。

目标是提高字形清晰度、读数对比度和转头时的稳定感。继续采用现有青色导航、琥珀预警、红色危险语义与情境淡入淡出，不迁移原版生命/护甲，不新增工具/任务区域。

共享曲线只约束相应 HUD 组件的几何，不对世界画面进行全屏后处理，不引入屏幕边缘压缩或全局边缘衰减。生存规则、H5 启动与连接时序保持不变。

采用 [全屏双曲线设计](hud-local-curves-design.md)：上曲线中央低、两端高，下曲线中央高、两端低。由屏幕 GUI 宽高统一计算曲线；Compass 和 EVA 采样上曲线，O₂ / Thermal 采样下曲线右侧段。每个组件不再拥有独立曲率，文字与图形一起变形；H5 日志、中央提示继续平面。

共享曲线已接入。16:9 下从中央到屏幕边缘起伏约为屏幕高度的 4.72%，边缘倾角约 5.75°。480 × 270 GUI 视口中，右下条带左右高度差约 8.2 px，较前版约 16.4 px 明显放缓。生存布局只补偿一次曲线中点高度，以保留 main 阅读高度；通用几何中不得按组件重新归零。实现与验证入口见设计文档。

---

# 5. 保留的清晰度优化与当前合成

`HudVisorProjection` 继续使用独立小型 TextureTarget，按组件实际屏幕横坐标从 `HudVisorGeometry` 共享曲线采样网格；系统文字平面合成，旧 `HudVisorSurface` 不再使用。GUI 宽高、横向布局或显示缩放改变时更新网格，垂直排列和转头跟随只修改绘制变换。

光晕在独立低分辨率组件纹理中做对称九点加权叠加，半径约 `0.7 GUI px`、总强度 `8%`；光晕与清晰原图使用同一曲面网格合成。它替代放大副本的效果，减少字边重影，不新增全屏后处理或 Shader，也不在 CPU 每帧复制九份曲面网格。

生存主读数提高不透明度，并加入字体阴影，改善明亮背景上的辨识度。原有说明文字、警告颜色与行间位置保留。

Compass 保留刻度和方向信息，使用连续浮点位置绘制，边缘刻度平滑淡出；中心角度读数略微提亮。低透明度文字跳过绘制，避免 Minecraft 字体把近零 alpha 当作不透明而闪现。

新版保留清晰读数与连续刻度，并已接入共享曲线；具体网格、边缘亮度、资源生命周期和光晕成本见双曲线设计文档。

---

# 6. 克制的跟随运动

`HudVisorMotion` 使用临界阻尼跟随相机角速度，保持原有小幅度上限：

```text
水平角速度增益：0.0075 GUI px / (degree / second)
垂直角速度增益：0.005 GUI px / (degree / second)
最大偏移：水平 1.8 px / 垂直 1.2 px
响应系数：24 / second
```

使用解析时间步进，避免随帧率改变力度。静止时无漂移，转头时小幅滞后，停头后平稳回正；不设置反复弹跳。

暂停、菜单、F1、第三人称、相机实体/世界切换、长帧间隔和大幅 camera cut 清空偏移与速度。F1 可能跳过整个 GUI 绘制，因此客户端 tick 也执行隐藏状态复位。

World AR 不使用该运动；NOVA 通讯保持独立平面呈现。

本轮验证（2026-09-20）：`compileJava` 与 33 项 HUD 定向 JUnit 测试通过。新增 10 项运动测试覆盖不同帧率、方向/限幅、停头回正、反向转头、角度跨界、隐藏恢复和相机突变。

用户已确认本轮平面优化观感良好；以下保留为共享曲线新版的回归检查，不能视作新版已通过验收。检查中英文、GUI Scale 2/3/4，重点观察：

1. 顶部 Compass 缓慢转动是否顺滑，边缘数字是否平稳消失。
2. 右下读数在明亮地形和暗处的对比度，光晕是否足够克制。
3. 快速转头、急停、F1、菜单与第三人称切换后是否保持稳定。
4. H5 右上连接文字的可读性，左下 NOVA 和 World AR 是否保持独立。

---

# 7. World AR

World AR 继续采用当前成功的方案：

```text
真实 Camera Position
+
View Matrix
+
Projection Matrix
↓
Screen Position
```

World AR 不经过 Visor 组件合成。

也不继承：

```text
Visor drift
```

目标中心必须持续精确锁定真实世界对象。

---

# 8. ArTarget

第一版建议：

```java
record ArTarget(
    ResourceLocation id,
    ArTargetCategory category,
    Vec3 worldPosition,
    Component label,
    ArGuidanceMode guidance,
    int priority,
    double maxDistance,
    int rgb
) {}
```

后续再增加：

```text
icon
sourceId
occlusionMode
distanceFade
pulseStyle
visibilityRule
```

避免第一版过度设计。

---

# 9. AR 分类

第一版：

```text
NAVIGATION
OBJECTIVE
POI
INTERACTION
WARNING
SCANNER
```

示例：

```text
NAVIGATION
◇ SHIP

OBJECTIVE
◇ REPAIR TERMINAL

POI
◇ ABANDONED RELAY

INTERACTION
◇ ACCESS TERMINAL

WARNING
△ HULL BREACH

SCANNER
? UNKNOWN SIGNAL
```

分类可以决定默认视觉样式和优先级，但业务逻辑不要依赖颜色。

---

# 10. Guidance Mode

任务 / 探索不能全部变成精确 waypoint。

第一版支持：

```text
AREA
SIGNAL
TARGET
INTERACTION
```

## AREA

只知道大致区域。

```text
SEARCH AREA
约 200m
```

## SIGNAL

知道方向或大致距离，但尚未确认目标。

```text
? UNKNOWN SIGNAL
东北
~340m
```

## TARGET

已经确认目标。

```text
◇ ABANDONED HABITAT
86m
```

## INTERACTION

已经抵达目标附近，显示具体交互对象。

```text
◇ ACCESS TERMINAL
18m
```

原则：

> AR 展示玩家已经获得的信息，不替玩家完成探索。

---

# 11. ArTargetProvider

HUD 不应直接读取所有业务系统。

统一接口：

```java
interface ArTargetProvider {
    void collect(ArContext context, Consumer<ArTarget> output);
}
```

第一版：

```text
ShipBeaconProvider
NovaObjectiveProvider
RelayPoiProvider
TutorialTargetProvider
```

未来：

```text
SpacePoiProvider
SurveyScannerProvider
SalvageScannerProvider
EnvironmentScannerProvider
```

---

# 12. ArTargetCollector

每帧：

```text
获取当前 Client Context
↓
遍历 Provider
↓
收集 ArTarget
↓
过滤
↓
排序
↓
去重
↓
ArWorldRenderer
```

建议初期限制：

```text
高优先目标 ≤ 3
普通目标 ≤ 5
```

避免满屏 Marker。

---

# 13. AR 遮挡

建议尽早预留：

```text
ALWAYS_VISIBLE
VISIBLE_WHEN_CLEAR
EDGE_ONLY_WHEN_OCCLUDED
```

示例：

```text
Ship Beacon
→ ALWAYS_VISIBLE

任务区域
→ ALWAYS_VISIBLE

房间内部终端
→ VISIBLE_WHEN_CLEAR

Scanner Target
→ 根据扫描器能力决定
```

避免任务 AR 演变成透视外挂。

---

# 14. HUD 默认应保持安静

普通安全环境：

```text
Lush
+
无导航目标
+
无环境危险
```

屏幕应接近 Vanilla。

只有有原因的信息才出现：

```text
导航目标
→ Compass

任务
→ World AR

真空
→ O₂

冷热危险
→ Thermal

N.O.V.A. 信息
→ Flat Communication
```

不要常驻完整科幻仪表盘。

---

# 15. 开局 HUD 采用两阶段启动

开局流程：

```text
玩家苏醒
↓
Emergency Bootstrap
↓
SAFE MODE
↓
寻找 N.O.V.A. Terminal
↓
Core Reboot
↓
LINKING
↓
ONLINE
```

HUD 不应等到 N.O.V.A. 完全在线后才突然出现。

---

# 16. 第一阶段：苏醒 / Emergency Bootstrap

当前序章已有：

```text
玩家进入世界
↓
约 60 tick（3 秒）
↓
Emergency Broadcast
↓
稍后提醒寻找 AI Terminal
```

HUD 可在苏醒阶段同步启动。

建议视觉时长：

```text
中央重启提示约 3.5 秒，完整启动流程约 6.5 秒
```

表现：

```text
屏幕中央先显示：

抬头显示重启中

随后右上角滚动：

SYSTEM STARTING

VISUAL LINK ...... ONLINE
LOCAL NAV ........ DEGRADED
SHIP NETWORK ..... ERROR
N.O.V.A. CORE .... OFFLINE
```

视觉顺序：

- 中央方框先明确提示“抬头显示重启中”；
- 右上角随后逐行滚动恢复状态；
- 完成后展开 Compass，并显示 AI Terminal World AR 指引；
- 使用轻微扫描与淡入，不使用夸张 glitch。

随后进入：

```text
SAFE MODE
```

---

# 17. Safe Mode

Safe Mode 中可使用：

```text
基础 Compass
本地舰内定位
基础 World AR
紧急系统状态
```

但高级信息受限。

世界观解释：

```text
Personal Visual Interface
├ Local Orientation
├ Local Ship Map
├ Emergency Guidance
└ Basic Telemetry

N.O.V.A.
├ Advanced Analysis
├ Objectives
├ Communication
├ Exploration Knowledge
└ Ship Intelligence
```

所以：

```text
HUD = SAFE MODE
N.O.V.A. = OFFLINE
```

并不冲突。

---

# 18. 第一次 AR 教学

当前已有：

> 请前往舰载人工智能终端，重启核心系统。

这一阶段直接使用 World AR。

例如：

```text
? SYSTEM NODE

AI TERMINAL
18m
```

或者：

```text
◇ AI TERMINAL
18m
```

特点：

```text
无曲率
严格锁定真实终端
```

玩家通过实际行动自然学会：

> AR Marker 是真实世界目标指引。

---

# 19. 第二阶段：Core Link

当前已有：

```text
CORE_REBOOTING
50 ticks
≈ 2.5 秒
```

直接利用这个时间窗口。

HUD 显示：

```text
SHIP NETWORK ...... CONNECTING
N.O.V.A. LINK ..... SYNCING
NAVIGATION ........ INITIALIZING
```

当 Core 真正 ONLINE：

```text
N.O.V.A. LINK ..... CONNECTED
NAVIGATION ........ ONLINE
ENVIRONMENT ....... ONLINE

VISUAL INTERFACE
FULL LINK ESTABLISHED
```

随后启动文字淡出。

N.O.V.A. 平面通信：

```text
核心系统已恢复。我正在评估飞船的整体状态。
```

---

# 20. HUD Runtime State

建议：

```text
DORMANT
STARTING
SAFE_MODE
LINKING
ONLINE
```

需要持久化的个人状态建议：

```text
HUD_BOOT_PRESENTED
HUD_CORE_LINK_PRESENTED
```

不要只使用：

```text
HUD_INITIALIZED
```

一个 Boolean。

客户端负责动画播放。

服务端只同步真实故事 / 核心状态。

---

# 21. 多人行为

玩家 A 已完成序章：

```text
Core = ONLINE
```

玩家 B 后加入。

玩家 B 不应该经历：

```text
N.O.V.A. CORE OFFLINE
```

而应该播放简短个人初始化：

```text
VISUAL INTERFACE STARTING

VISUAL LINK ...... ONLINE
SHIP NETWORK ..... CONNECTED
N.O.V.A. LINK .... CONNECTED
NAVIGATION ....... ONLINE
```

然后进入 ONLINE。

HUD Boot 是玩家个人体验。

Core 状态仍是共享飞船状态。

---

# 22. Base HUD 与 EPP

Base HUD：

```text
任务 AR
Ship Beacon
Space POI Navigation
必要 Compass
System Status
```

不依赖 EPP。

EPP 只提供：

```text
Oxygen
Cold
Heat
未来 Radiation
```

卸下 EPP 后：

```text
任务 AR
Ship Navigation
N.O.V.A.
```

仍然存在。

---

# 23. Compass

Compass 不应永久常驻。

建议显示条件：

```text
存在 Navigation Target
或者
存在 Current Objective
或者
玩家正在 EVA
或者
系统启动阶段
```

普通 Lush：

```text
无目标
→ Compass 淡出
```

---

# 24. N.O.V.A. 通讯

N.O.V.A. Broadcast 保持平面。

当前：

```text
NovaBroadcastHudLayer
NovaBroadcastHudRoot
```

可以继续使用现有布局与肖像动画。

重构时只需要：

- 接入统一 HUD 生命周期；
- 与其他 HUD 做层级协调；
- 避免遮挡 World AR；
- 保持平面；
- 不应用 Visor 曲率和 Visor drift。

---

# 25. N.O.V.A. Objective AR

N.O.V.A. 任务本身不绘制 HUD。

建立：

```text
NovaObjectiveProvider
```

它把当前已知任务信息转换成：

```text
ArTarget
```

例如：

```text
N.O.V.A.
检测到一个不稳定信号。

↓

NovaObjectiveProvider

↓

? UNKNOWN SIGNAL
```

HUD 自己不要扫描世界寻找任务目标。

---

# 26. Rocky Moon 第一轮接入

月球只做：

```text
Landing Beacon
+
当前 Objective
```

不要直接显示所有 POI。

例如：

```text
刚登陆
◇ LANDING BEACON

任务开始
? SIGNAL

发现 Habitat
◇ ABANDONED HABITAT
```

---

# 27. Space POI

当前先使用：

```text
RelayPoiProvider
```

把：

```text
RelayClientState
RelayGeometry
```

转换为：

```text
ArTarget
```

以后真正有：

```text
SpacePoiInstance
```

时替换 Provider。

Renderer 不需要变化。

---

# 28. Ship Beacon

当前：

```text
ShipStructure.SHIP_TELEPORTER_POS
```

从 Renderer 中移出。

由：

```text
ShipBeaconProvider
```

提供。

未来多飞船 / 自定义船体时只修改 Provider。

---

# 29. 生存 HUD

现有：

```text
Oxygen
Cold
Heat
```

规则保持不变。

只重构显示层。

不要在 HUD 重构中顺手修改：

```text
OxygenRules
ExposureRules
EppEvents
```

这轮重点是客户端表现与架构。

---

# 30. Fade

可以统一：

```text
HudFadeController
```

参数：

```text
holdSeconds
fadeInSeconds
fadeOutSeconds
```

示例：

```text
Oxygen
hold 1.5
in 0.15
out 0.8

Compass
hold 1.0
in 0.15
out 0.5

Objective
hold 2.0
in 0.2
out 0.5
```

N.O.V.A. Broadcast 可保留自己的动画逻辑。

---

# 31. 推荐包结构

```text
client/hud/
    StarboundHudLayer
    HudRuntime
    HudBootController
    HudStyle

client/hud/visor/
    HudVisorMotion
    HudVisorProjection

client/hud/ar/
    ArTarget
    ArTargetCategory
    ArGuidanceMode
    ArOcclusionMode
    ArTargetProvider
    ArTargetCollector
    ArWorldRenderer
    ArTargetProjection

client/hud/provider/
    ShipBeaconProvider
    NovaObjectiveProvider
    RelayPoiProvider
    TutorialTargetProvider

client/epp/
    EppClientState
    EppTelemetryProvider

client/shipai/
    NovaBroadcastHudLayer
    NovaBroadcastHudRoot
```

N.O.V.A. 通讯不迁入 Visor 包。

---

# 32. 实施阶段

## H0 — Prototype Freeze

记录当前 main 的视觉基线：

```text
Compass
AR Marker
O₂
Cold / Heat
EVA HUD
N.O.V.A. Broadcast
```

保存截图用于回归比较。

不改行为。

---

## H1 — Shared Visor Optics

以下为 H1 历史基线；H5 后的视觉优化以第 4–6 节与全屏双曲线设计为准。旧 `HudVisorSurface` 已移除，独立组件纹理现在采样共享的屏幕曲线；本节的四角投影偏好不约束新版字形曲面。

新增：

```text
HudVisorSurface
HudVisorProjection
```

先接：

```text
Compass
O₂
Cold / Heat
测试 Calibration Grid
```

目标：

> 顶部和右下信息共享克制的边缘压缩、渐隐、glow 与 optical lag；玩家不会首先注意到 UI 被弯曲。

实现优先采用组件级四角投影，不用高密度网格扭曲文字内部。

不要修改 World AR。

N.O.V.A. Broadcast 继续平面。

---

## H2 — Generic World AR

新增：

```text
ArTarget
ArTargetProvider
ArTargetCollector
ArWorldRenderer
```

把当前硬编码：

```text
SHIP
RELAY
```

迁为：

```text
ShipBeaconProvider
RelayPoiProvider
```

World AR 保持无曲率。

---

## H3 — StarboundHudLayer

状态：已完成。

现有 EPP-owned HUD 已迁为通用：

```text
StarboundHudLayer
```

职责：

```text
Visor
World AR
Survival
System Status
```

N.O.V.A. Broadcast 仍作为独立平面 Communication Layer。

当前边界：

- `StarboundHudLayer` 统一编排 Visor、World AR、EVA 导航和 Survival telemetry；
- `EppClientState` 只保存 EPP telemetry，不再反向拥有或重置整套 HUD；
- 连接生命周期由 `ClientConnectionEvents` 显式重置通用 HUD；
- `HudBootController` 由 H4 接管个人 HUD Boot 状态，并由 H5 接入核心连接和个人初始化。

---

## H4 — Wake Bootstrap

状态：已完成。

实现：

```text
HudBootController
```

接入当前 Initial Wake。

实现：

```text
DORMANT
→ STARTING
→ SAFE_MODE
```

同时加入 AI Terminal World AR 指引。

当前实现边界：

- 复用服务端持久化的 `INITIAL_WAKE_BROADCAST` 作为个人 Boot 已呈现信号；
- 登录时通过只读 `HudBootstrapStatePacket` 恢复 DORMANT / SAFE_MODE / ONLINE 基线，避免断线后重播；
- 玩家进入世界约 3 秒后收到 Emergency Broadcast；
- 客户端先在中央显示“抬头显示重启中”，再在右上角逐步滚动约 6.5 秒的 STARTING 流程；
- pause、菜单或隐藏 HUD 期间不推进 Boot 动画；
- STARTING 期间收到的 N.O.V.A. 通讯会排队，SAFE_MODE 建立后再以独立平面层播放，避免与中央提示争夺注意力；
- STARTING 完成后进入 SAFE_MODE，系统状态短暂停留并淡出；
- `TutorialTargetProvider` 将 Marker 锁定 `SHIP_AI_TERMINAL_POS`，终端接触后由服务端状态关闭；
- World AR 继续使用真实 View / Projection Matrix，不经过 Visor drift；
- H4 原本只负责苏醒；`CORE_REBOOTING` / `CORE_ONLINE` 的动态转换已在 H5 接入。

---

## H5 — Core Link

状态：已完成，用户已确认视觉（2026-09-20）。

监听：

```text
CORE_REBOOTING
CORE_ONLINE
```

实现：

```text
SAFE_MODE
→ LINKING
→ ONLINE
```

N.O.V.A. Core Online 后恢复完整 HUD 功能。

当前实现边界：

- `HudStateService` 在核心开始重启、上线和调试状态变更时，向全部在线玩家发送独立于菜单的 `HudBootstrapStatePacket`；登录和终端接触也同步个人状态。
- `HudBootController` 在真实 `REBOOTING` 状态下进入 LINKING，使用右上角现有状态区域逐行显示连接信息；客户端计时不会自行宣布核心上线。
- 收到真实 ONLINE 后立即进入 ONLINE，显示链路、导航、环境监测恢复与“完整系统连接已建立”，停留 25 tick、淡出 20 tick。
- 核心已经在线、但尚未完成个人初始化的玩家，先播放 40 tick 的简短 LINKING 序列；内容显示已连接状态，不播放“核心离线”的紧急启动。
- 暂停、打开菜单、隐藏 HUD 或死亡期间不推进呈现时钟；服务端核心状态仍正常更新。若核心在菜单打开期间恢复，关闭菜单后显示真实的连接完成信息。
- `HUD_CORE_LINK_PRESENTED` 是独立个人记录。客户端在完整呈现结束后发送 `HudCoreLinkPresentedPacket`，服务端只在核心真实 ONLINE 时写入该玩家的记录，不改变任务、身份或共享进度。
- 玩家故事数据升级至 schema 2：schema 1 中已收到核心上线广播或已确认身份的玩家迁移为已连接，避免升级后重复介绍；其他旧玩家仍可完成首次连接。死亡沿用附件的 `copyOnDeath()`，中断且尚未确认的呈现在重登后恢复为简短个人初始化。
- 首次苏醒广播前接触终端也会进入 SAFE_MODE，不再卡在 DORMANT；核心重启后关闭终端教学 AR。
- 连接呈现期间 N.O.V.A. 通讯排队并暂停当前播放；结束后继续。核心恢复只移除已过时的紧急离线/寻找终端提示，保留其他通讯。
- 网络协议升至 16，客户端和服务端需要使用相同版本。
- 本阶段沿用原 Visor 光学、布局和 World AR；不接入任务 Provider，不推进 H6。

验证：

- `gradlew.bat compileJava` 通过。
- HUD、通讯、故事与网络相关的 177 项定向 JUnit 测试通过，覆盖真实核心状态驱动、重复快照、暂停、重登、晚加入、提前接触终端、个人回执与旧数据迁移。
- `gradlew.bat -PshipGameTests runGameTestServer` 的 44 项 GameTest 全部通过；新增 `HudStateGameTests` 覆盖玩家实际保存/加载、死亡继承、多人个人状态隔离，以及无终端菜单时的 HUD 快照发送。
- 用户已确认本轮视觉效果；下列场景保留为后续 Visor 调整的回归检查清单。

实机检查清单：

1. 新存档按正常流程苏醒、找到终端、开始重启；关闭终端观察 LINKING → ONLINE 与随后 N.O.V.A. 通讯。
2. 保持终端打开直到核心恢复，再关闭；应看到连接完成，不能出现陈旧的核心离线状态。
3. 首次广播前直接打开终端；关闭后生存 HUD 可用，且不再提示寻找已接触终端。
4. 连接中打开菜单或 F1，恢复后继续；完成后重登或死亡，不重复初始化。
5. 第二位玩家加入已上线飞船；只播放个人初始化，不宣称 N.O.V.A. 核心离线。
6. 中英文、GUI Scale 1–4 检查右上角文字与 Compass 的间距和清晰度。后续 Visor 视效调整以此为起点。

---

## H6 — N.O.V.A. Objective AR

状态：暂不开始。当前仅进行 H5 之后的 Visor 视觉设计与调整。

新增：

```text
NovaObjectiveProvider
```

第一版只接一个真实任务链。

至少支持：

```text
SIGNAL
TARGET
```

验证任务 AR 不破坏探索。

---

## H7 — Moon / EVA Integration

Rocky Moon：

```text
Landing Beacon
Current Objective
```

EVA：

```text
Ship
Current Space POI
O₂
Compass
```

全部使用新的 Provider / Renderer。

---

## H8 — Cleanup

删除：

```text
ArNavigationHud 中的业务硬编码
废弃的旧投影实现（保留新版共享双曲线几何与组件采样网格）
重复 Renderer
重复 Fade
```

整理：

```text
lang keys
package names
tests
docs
```

---

# 33. 自动测试

单元测试：

```text
ArTarget priority
Target filtering
Guidance modes
Occlusion rules
Clip projection
Behind target
Offscreen clamp
Distance filtering
Fade timing
Provider aggregation
Target deduplication
Component-local curve geometry / fade bounds
Local strip thickness / anchor semantics / no mesh folding
HUD boot state transition
```

服务端 / GameTest：

```text
HUD boot personal persistence
新玩家默认状态
死亡不重置
重登不重复
多人状态独立
Core Online 共享状态
旧存档迁移
```

视觉效果不能只靠自动测试判断。

---

# 34. 必须实机验收

至少：

| 场景 | 检查 |
|---|---|
| 16:9 | 默认布局 |
| 超宽屏 | Visor / AR 边缘位置 |
| GUI Scale 1–4 | 字体清晰度与边缘形变 |
| FOV 改变 | AR 锁定 |
| 快速转头 | Marker 稳定 |
| 抬头 / 低头 | 世界投影 |
| 中文 | 标签长度 |
| 英文 | 标签宽度 |
| 两目标接近 | AR 避让 |
| 多目标 | 优先级 |
| Pause | Boot / Fade |
| Reconnect | 状态恢复 |
| Death | HUD 不重启 |
| EVA | Ship / POI 导航 |
| Moon | Landing / Objective |
| Lush | HUD 是否足够安静 |
| N.O.V.A. Broadcast | 平面可读性 |

---

# 35. Visor 验收标准

可以临时显示：

```text
全屏双曲线参考线 + 组件 Calibration Grid
```

同时显示：

```text
顶部 Compass
右下 O₂
右下 Thermal
System Status
```

验收重点：

```text
Compass 清晰，与 EVA 一起沿屏幕上曲线分布
O₂ / Thermal 沿底部拱线右侧向右下弯，主读数中点保留 main 的视觉高度
文字、图标与条带一起变形，局部厚度稳定，无翻折与裁剪
右下主读数在明亮背景上仍可辨认
刻度连续移动并在边缘平滑消失
快速转头时 drift 短促、稳定、很快恢复
相邻组件沿同一曲线连续拼接，右下弧度舒缓，保留当前清晰度和稳定感
```

Calibration Grid 的上下参考线横贯屏幕，组件网格应遵循对应曲段；同时检查阅读高度、采样和轻微 optical lag。参考线只在开发开关启用时显示。

Calibration Grid 正式游戏默认关闭。

---

# 36. World AR 验收标准

转头、改变 FOV、移动时：

```text
Ship Beacon
Relay
Objective
```

必须始终锁定真实目标。

World AR：

```text
不弯曲
不漂移
不参与 Visor Surface
```

但仍可共享 StarboundMC 的：

```text
颜色语言
细线
透明度
扫描建立动画
```

---

# 37. HUD Boot 验收标准

玩家应该自然理解：

> 我的视觉系统正在启动。

而不是：

> 游戏突然弹出了一个教程 UI。

要求：

```text
短
清晰
不冻结世界
不锁移动
不强制黑屏
可调试重播
不重复骚扰玩家
```

---

# 38. AR 任务验收标准

成功：

```text
玩家知道接下来应该往哪里探索
但仍需要观察世界
```

失败：

```text
远距离就能透视所有任务设备、箱子和秘密地点
```

原则：

> AR 负责方向感，不负责替玩家发现内容。

---

# 39. 星图与 AR

长期固定：

```text
Star Map
= Macro Navigation

World AR
= Local Navigation
```

例如：

```text
星图选择 Relay
↓
进入 Local Space
↓
World AR
◇ RELAY
```

---

# 40. 第一轮停止点

做到以下内容即停止扩展：

```text
统一 Visor 轻度光学风格
✓

N.O.V.A. 通讯保持平面
✓

World AR 独立且无曲率
✓

Ship / Relay Provider 化
✓

苏醒 HUD Safe Mode
✓

AI Terminal 第一次 AR 指引
✓

Core Reboot → Full Online
✓

一个 N.O.V.A. 任务进入 World AR
✓

Moon / EVA 使用通用 Provider
✓

安全环境 HUD 保持干净
✓
```

随后进行完整实机体验。

暂时不要继续：

```text
Scanner
随机多 Space POI
完整 Pressure System
Mk.IV
大型 HUD 设置页
复杂新图标
```

---

# 41. 推荐 Git 提交拆分

```text
1. refactor(hud): introduce shared low-intensity visor optics

2. refactor(ar): introduce generic world AR target providers

3. refactor(hud): establish Starbound HUD runtime layer

4. feat(hud): add wake safe-mode bootstrap

5. feat(hud): link visor state to NOVA core reboot

6. feat(ar): integrate NOVA objective guidance

7. feat(ar): migrate ship, moon and relay navigation

8. docs(hud): document HUD and AR architecture
```

每个提交应：

```text
可编译
尽量可运行
不依赖未来提交才能理解
```

---

# 42. 本轮禁止事项

HUD 重构过程中不要顺手实现：

```text
随机多空间站
完整 Scanner
Pressure System
重写 NovaTask
Mk.IV
复杂 HUD 自定义页面
全套新任务系统
重写整个 LDLib2 UI
```

需要时预留接口并记录 TODO。

---

# 43. 最终设计原则

**Visor 是玩家设备界面。**

**Visor 保留现有布局与独立组件纹理，共享横贯屏幕的上下双曲线；顶部中间低、两端高，右下读数采样下曲线右侧并缓缓下落。结合已确认的清晰度、克制光晕与跟随运动，多行系统文字保留平面。**

**World AR 是现实空间中的信息投影，不使用曲率。**

**N.O.V.A. 通讯是独立平面通信层，不使用曲率。**

**HUD 在玩家苏醒时进入 Safe Mode，而不是突然完整上线。**

**N.O.V.A. Core 恢复后，HUD 才完成完整系统连接。**

**AR 展示已经获得的信息，不替玩家探索。**

**星图负责宏观导航，AR 负责局部导航。**

**默认 HUD 保持安静。**

**先把苏醒、Moon、EVA 三个场景做扎实，再扩展 Scanner 和更复杂的信息层。**
