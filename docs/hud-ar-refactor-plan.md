# StarboundMC 通用 HUD / AR 系统重构计划

> 建议文件名：`docs/hud-ar-refactor-plan.md`
>
> 状态：视觉策略已修订 / 分阶段实施中
>
> 目标：将当前以 EPP / EVA 为中心的 HUD 原型重构为 StarboundMC 全游戏通用的信息呈现系统，并为后续任务、探索、Space POI、Scanner 与环境系统提供统一基础。

---

# 1. 重构目标

现有 HUD 已经验证了以下体验：

- Visor HUD 的轻度光学风格成立，但明显曲率不应成为视觉主体；
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
轻微边缘压缩
轻微边缘淡出
低强度 Glow
固定在玩家视野
```

这些元素需要共享克制、一致的 Visor 视觉语言，但不要求玩家明显看出它们位于同一张几何弧面。
文字清晰度和信息读取速度优先于曲面连续性的理论正确性。

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

例如启动阶段的 Calibration Grid 可以使用轻微边缘形变和渐隐，而 Warp Flash 可以保持纯屏幕 Overlay。

---

# 4. 统一 Visor 光学风格

当前：

```text
VisorHudProjection
VisorSurface.project()
VisorSurface.navigation()
```

曾分别为不同小区域建立不同的曲率模型，后续又尝试把它们映射到同一全局曲面。

实机验证表明，强调几何连续性容易让视觉变成：

```text
所有组件都被强行弯曲
文字基线出现不必要的弧线
玩家首先注意到 UI 形变而不是信息
```

修正后的目标：

> Visor HUD 使用统一的轻度光学风格。曲率仅作为辅助视觉效果，优先保证可读性、边缘空间感和轻微视差，不追求明显几何弧面。

不需要建立一张巨大的全屏 RenderTarget。

继续允许：

```text
Compass
O₂
Thermal
System Status
```

分别使用自己的小型 TextureTarget。

组件可以共享同一个轻度边缘形变和透明衰减函数：

```text
HudVisorSurface
```

---

# 5. HudVisorSurface

建议新建：

```text
HudVisorSurface
```

输入可以继续使用全局归一化屏幕坐标：

```text
左上：(-1, -1)

中心：(0, 0)

右下：(+1, +1)
```

推荐组件流程：

```text
Component Local Coordinate
↓
Component Bounds
↓
Normalized Viewport Coordinate
↓
轻微边缘压缩 / 透明衰减
↓
组件级四角投影
```

不需要继续用高密度网格扭曲字形内部。Compass、O₂、Thermal 和 System Status 可以继续使用独立小纹理，
共享边缘压缩、fade、glow 和 drift 参数即可建立一致视觉语言。

`HudVisorSurface` 应保持简单：中心近似恒等映射，边缘只有轻微的轴向压缩；不要加入会让水平文字基线弯成抛物线的跨轴项。

---

# 6. Visor 光学视觉要求

曲率不再是核心效果，也不需要主动向玩家证明 HUD 位于曲面上。

科幻感主要来自组合：

```text
非常轻的边缘形变
+
轻微视差 / Optical Lag
+
透明衰减
+
低强度 Glow
+
启动 / 扫描 / 淡入动画
+
World AR 的空间锁定感
```

几何要求：

```text
中心区域基本平直
屏幕边缘轻微压缩
垂直形变低于水平形变
文字基线保持水平
字形内部不做明显网格扭曲
```

当前实现参考值：水平边缘最大压缩约 `1.5%`，垂直边缘最大压缩约 `0.3%`；水平/垂直边缘
透明衰减上限分别为 `16%` / `4%`；组件纹理使用约 `0.45 px`、`10%` 强度的低强度 glow。
这些数值是视觉调优起点，不是必须维持几何连续性的物理模型。

避免：

```text
CRT
鱼眼镜头
夸张球面
大幅文字变形
明显抛物线弯曲
为了统一曲面而牺牲可读性
```

文字可读性优先。

Optical Lag 保持克制。当前参考参数：

```text
Yaw 注入：0.065 GUI px / degree
Pitch 注入：0.045 GUI px / degree
最大偏移：水平 1.8 px / 垂直 1.2 px
恢复：exp(-9 × seconds)
```

暂停、打开菜单、切换相机、长帧间隔或大幅 camera cut 时立即归零。目标是表现投影系统的轻微光学惯性，
不是让 HUD 在屏幕上持续漂动。

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

World AR 不经过：

```text
HudVisorSurface
```

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
    HudVisorSurface
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
- `HudBootController` 由 H4 接管个人 HUD Boot 状态；H5 的 Core Link 仍未接入。

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
- H4 不监听 `CORE_REBOOTING` / `CORE_ONLINE` 的动态转换，留给 H5。

---

## H5 — Core Link

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

---

## H6 — N.O.V.A. Objective AR

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
旧的过度曲率函数和高密度形变网格
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
Visor edge deformation / fade bounds
Text baseline preservation
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
全屏 Calibration Grid
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
Compass 清晰且基线平直
O₂ / Thermal 自然，不出现明显倾斜
中心区域近似无形变
屏幕边缘有轻微压缩与透明空间感
快速转头时 drift 短促、稳定、很快恢复
整体具有科幻投影感，但玩家不会首先注意到“UI 被弯曲”
```

Calibration Grid 只用于观察边缘渐隐、对齐和轻微 optical lag，不再用于证明所有组件属于连续曲面。

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

**Visor 使用统一的轻度光学风格；曲率只是辅助效果，可读性优先。**

**World AR 是现实空间中的信息投影，不使用曲率。**

**N.O.V.A. 通讯是独立平面通信层，不使用曲率。**

**HUD 在玩家苏醒时进入 Safe Mode，而不是突然完整上线。**

**N.O.V.A. Core 恢复后，HUD 才完成完整系统连接。**

**AR 展示已经获得的信息，不替玩家探索。**

**星图负责宏观导航，AR 负责局部导航。**

**默认 HUD 保持安静。**

**先把苏醒、Moon、EVA 三个场景做扎实，再扩展 Scanner 和更复杂的信息层。**
