# StarboundMC 局部太空遭遇、EVA 与动态遭遇锚点系统设计

> 建议文件名：`docs/space-encounter-eva-design.md`
>
> 状态：概念设计 / 尚未进入正式实现
>
> 本文档定义 StarboundMC 的局部太空遭遇、EVA、空间站实体化、动态飞船边界与 Encounter Anchor 规则。
>
> 目标是在不移动真实飞船方块结构、不破坏现有 `UniversePosition` 与 `CurrentEntry` 架构的前提下，让玩家能够从自己的飞船真正出舱，在太空中 EVA，并探索空间站、废船、小行星等真实 Minecraft 结构。

---

# 1. 设计目标

StarboundMC 的飞船维度不应只是：

> 飞船内部 + 窗外宇宙背景 + 星图传送大厅。

长期目标是让：

> **太空本身成为一个可以实际进入和探索的游戏空间。**

典型流程：

```text
星图 / N.O.V.A. 发现废弃空间站
→ 飞船导航接近目标
→ 窗外空间站逐渐变大
→ 飞船在安全距离完成接近
→ 玩家穿戴 EPP
→ 进入气闸
→ 出舱进入真空
→ 使用 EVA 推进飞向空间站
→ 从气闸或破损区域进入
→ 探索空间站
→ 获取资源 / 科技 /任务目标
→ EVA 返回自己的飞船
→ 全体船员回舰
→ 飞船恢复航行能力
```

整个过程中不应依赖：

```text
“传送到空间站”
```

作为主要交互方式。

玩家应该真正看到：

- 自己的飞船；
- 远处的目标；
- 二者之间的太空；
- 自己实际完成 EVA 的过程。

---

# 2. 核心设计原则

StarboundMC 使用两个不同尺度描述宇宙。

```text
宏观宇宙
UniversePosition
SectorCoordinate
StarSystem
CelestialBody
Space POI

        ↓

局部太空
Ship Dimension
LocalSpaceFrame
真实方块
真实碰撞
玩家 EVA
空间站 / 废船 / 小行星
```

宏观宇宙负责：

- 恒星系位置；
- 天体位置；
- 星图；
- 航线；
- 星际距离；
- 飞船当前宇宙位置；
- Space POI 的逻辑位置。

局部太空负责：

- 玩家周围几百格的可游戏空间；
- 飞船实体结构；
- 空间站实体结构；
- 真空；
- EVA；
- 气闸；
- 人工重力；
- 太空探索和战斗。

两者不使用真实比例一一映射。

---

# 3. 为什么需要 Local Space

真实宇宙尺度无法直接映射到 Minecraft 方块。

例如两个对象在宏观层可能相距：

```text
5 km
20 km
100 km
```

但如果玩家需要真实 EVA 数万格：

> 不具有游戏意义。

因此进入近距离 Encounter 后，应建立：

```text
LocalSpaceFrame
```

把宏观上的“已经接近目标”映射成一个适合游玩的 Minecraft 场景。

例如：

```text
宏观：

Ship UniversePosition
≈ Abandoned Station UniversePosition


局部：

SHIP
≈ Local Origin

Station
≈ 飞船外部的一处动态安全锚点

Debris A
≈ Encounter Anchor 周围

Debris B
≈ Encounter Anchor 周围
```

局部坐标只服务当前 Encounter。

---

# 4. 飞船真实方块保持固定

StarboundMC 当前的飞船结构固定存在于 Ship Dimension。

这一设计应继续保留。

飞船航行时：

```text
真实 Ship Blocks
不移动
```

变化的是：

```text
UniversePosition
Yaw
Pitch
Roll
Space Renderer
Flight State
```

因此 EVA 系统必须建立在：

> **固定飞船 + 虚拟宇宙移动**

这一架构之上。

不要为了 EVA 改成真正移动整个 Minecraft 方块飞船。

---

# 5. Space Encounter

新增逻辑概念：

```text
SpaceEncounter
```

表示：

> 当前飞船正在近距离接触的一个可互动太空目标。

目标可以是：

- 废弃空间站；
- 废弃飞船；
- 小行星；
- 采矿设施；
- 科研平台；
- 海盗前哨；
- 通讯卫星；
- 探测器；
- 货运残骸；
- 空间异常；
- 巨型生物；
- 其他太空目标。

进入 Encounter 后：

```text
宏观航行暂停 / 锁定
↓
局部目标实体化
↓
玩家可以 EVA
```

---

# 6. Space POI 与 Celestial Body 必须分离

空间站、废船等目标不能直接塞入：

```text
CurrentEntry
```

`CurrentEntry` 应继续只描述：

> 飞船当前关联的 Celestial Body。

例如：

```text
CurrentEntry = sys1:gasgiant
```

玩家正在探索气态巨行星轨道中的空间站时：

```text
CurrentEntry = sys1:gasgiant
CurrentEncounter = starboundmc:abandoned_relay_station
```

离开空间站后：

```text
CurrentEncounter = null
```

但：

```text
CurrentEntry
```

无需变化。

这样不会破坏现有：

- 天体身份；
- 星图；
- 燃料；
- UniversePosition；
- 星系判断；
- 行星环境逻辑。

---

# 7. Space POI

未来可以考虑数据类型：

```text
SpacePoiDefinition
```

概念字段：

```text
id
type
system
orbitContext
universePosition

remoteVisual
encounterStructure

preferredEvaGap
minimumClearance

discoveryRules
completionRules

persistence
```

例如：

```json
{
  "id": "starboundmc:abandoned_relay_station",
  "type": "station",
  "orbit_context": "sys1:gasgiant",
  "preferred_eva_gap": 90,
  "persistent": true
}
```

第一版不要求立即 Registry / Codec 化。

可以先通过一个硬编码测试 Encounter 验证玩法。

---

# 8. Space POI 有两种表现形态

空间目标必须区分：

```text
Remote Representation
```

和：

```text
Local Materialized Structure
```

原因是：

> 飞船真实方块不移动，但宇宙背景会随着虚拟飞船位置和姿态变化。

如果真实空间站从航行开始就固定在 Ship Dimension 某坐标：

```text
背景移动
真实空间站不移动
```

会产生严重穿帮。

---

# 9. Remote Proxy

目标距离较远时：

```text
Space POI
→ Remote Visual Proxy
```

它不是 Minecraft 方块。

由 Space Renderer 根据：

```text
POI UniversePosition
-
Ship UniversePosition
```

计算：

- 方向；
- 相对距离；
- 视觉大小；
- 可见度。

表现可以逐步升级：

```text
远距离
→ 小亮点

较近
→ 简化轮廓

继续接近
→ 低模 / billboard

Encounter Approach
→ 足以辨认空间站形状

Local Encounter
→ 真实方块结构接管
```

---

# 10. 不允许固定 Encounter 坐标

早期设计中的：

```text
Station = (0, 105, 140)
```

只能作为：

> 默认小型飞船下的示意例子。

它绝不能成为正式规则。

原因是玩家可以自由扩建飞船。

例如玩家可能把飞船扩建到：

```text
X: -60 ~ +90
Y: 70 ~ 140
Z: -80 ~ +180
```

此时固定在前方 140 格的空间站可能：

- 生成到船里；
- 与机翼重叠；
- 覆盖玩家建筑；
- 出现在机库内部；
- 被巨型船体包围。

因此 Encounter Anchor 必须动态计算。

---

# 11. ShipEnvelope

建议建立：

```text
ShipEnvelope
```

表示：

> 当前玩家飞船在局部太空中的实际空间占用。

例如初始飞船：

```text
X: -11 ~ +11
Y: 100 ~ 110
Z: -15 ~ +17
```

玩家扩建以后：

```text
X: -45 ~ +62
Y: 78 ~ 130
Z: -70 ~ +105
```

ShipEnvelope 必须跟着扩展。

---

# 12. ShipEnvelope 不等于 Pressurized Area

这两个概念必须严格分开。

```text
ShipEnvelope
=
整艘船的结构空间

Pressure Zone
=
其中真正密闭、有空气的内部区域
```

例如：

```text
外部机翼
太阳能板
天线
引擎喷口
```

可以属于：

```text
ShipEnvelope
```

但不属于：

```text
Pressurized Zone
```

---

# 13. ShipEnvelope 的长期用途

ShipEnvelope 未来可以同时服务：

- Encounter Placement；
- EVA Beacon；
- Ship Size；
- Ship Mass；
- Ship Class；
- Camera Distance；
- Local Space Safety；
- Docking；
- Hangar；
- 环境系统优化；
- 重力系统范围；
- 生命维持容量。

因此不应把它设计成仅供空间站生成使用的临时工具。

---

# 14. ShipEnvelope 不应永远使用初始模板边界

初始燕隼号可以拥有：

```text
MIN_X
MAX_X
MIN_Y
MAX_Y
MIN_Z
MAX_Z
```

用于：

- 结构生成；
- 初始校验；
- 模板导入。

但运行时不能继续通过：

```text
初始模板 AABB
```

回答：

> “飞船现在有多大？”

玩家扩建后，这些常量已经失效。

---

# 15. 哪些方块属于 ShipEnvelope

长期建议引入：

```text
Ship Core
        ↓
ShipStructureGraph
        ↓
ShipEnvelope
```

只有属于当前主飞船结构网络的方块才进入 ShipEnvelope。

例如玩家在几千格外造了一个独立平台：

```text
Platform
```

默认不应该扩大：

```text
ShipEnvelope
```

除非以后通过：

```text
Dock
Connect
Register
```

主动加入当前 Ship ID。

---

# 16. ShipStructureGraph

长期：

```text
ShipStructureGraph
```

负责描述：

- 哪些方块属于当前飞船；
- 哪些设备属于当前飞船；
- 哪些舱室属于当前飞船；
- 哪些扩建结构仍与 Ship Core 连通。

ShipEnvelope 是其空间摘要。

但第一版 Encounter MVP 不需要完整实现这一体系。

---

# 17. MVP ShipEnvelope

第一阶段可以使用较简单方式：

```text
Dynamic Ship Bounds
+
Directional Extent
+
Collision Scan
```

先获得足够准确的飞船边界。

以后再升级到：

```text
ShipStructureGraph
```

---

# 18. EncounterEnvelope

每个 Encounter Structure 也需要：

```text
EncounterEnvelope
```

用于描述目标本身的尺寸。

例如：

```text
Abandoned Relay Station

width: 56
height: 30
length: 68
```

或者对应：

```text
X radius = 28
Y radius = 15
Z radius = 34
```

这样系统才能真正知道：

> 这个空间站放在哪里不会撞到飞船。

---

# 19. EVA Gap

空间站和飞船之间除了不重叠，还要保留：

```text
EVAGap
```

表示：

> 玩家真正用于 EVA 的开放空间。

基础模型：

```text
EncounterDistance
=
Ship Directional Extent
+
Encounter Directional Extent
+
Preferred EVA Gap
```

例如：

```text
飞船前部延伸：25
空间站半径：35
EVA Gap：80

总中心距离 ≈ 140
```

因此：

```text
140 blocks
```

可能恰好是默认小船的优秀视觉距离。

但它只是计算结果，不是系统常量。

---

# 20. 大型飞船自动增加 Encounter 距离

例如：

```text
Ship Directional Extent = 100
Station Extent = 35
EVA Gap = 80
```

那么：

```text
Encounter Distance ≈ 215
```

大型玩家飞船会自然停得更远。

不需要：

```text
if largeShip
    specialCase(...)
```

---

# 21. 不要只使用整个 Ship AABB 半径

单纯使用：

```text
max(width, height, length)
```

会产生不合理情况。

例如玩家向左侧建了一根 200 格长天线。

空间站在正前方。

如果使用整个 AABB 半径：

> 空间站也会因为侧面天线被推远数百格。

因此应引入：

```text
DirectionalShipExtent
```

---

# 22. Directional Extent

对于候选 Encounter Direction：

```text
D
```

只计算：

> 飞船结构在这个方向上延伸到了哪里。

概念：

```text
project occupied ship structure onto D
```

得到：

```text
DirectionalShipBoundary
```

然后：

```text
Anchor Distance
=
DirectionalShipExtent
+
DirectionalEncounterExtent
+
EVAGap
```

侧面的天线不会过度影响正前方 Encounter。

---

# 23. LocalEncounterAnchor

建议正式引入：

```text
LocalEncounterAnchor
```

表示：

> 当前 Encounter 在 Ship Dimension 中最终选择的实体化锚点。

它至少包含：

```text
position
approachDirection
distance
rotation
```

例如：

```text
position:
(72, 118, 186)

approachDirection:
ship forward + 35°

distance:
231
```

---

# 24. Encounter Anchor 必须提前确定

不能等飞船已经抵达以后才决定空间站到底放哪。

否则可能出现：

```text
远景空间站
一直在正前方

↓

抵达

↓

发现前方被飞船扩建挡住

↓

真实空间站突然出现在右边
```

这是不能接受的。

正确流程：

```text
准备 Encounter
↓
计算 ShipEnvelope
↓
读取 EncounterEnvelope
↓
搜索 LocalEncounterAnchor
↓
确定 Approach Direction
↓
Remote Proxy 与目标方向对齐
↓
飞船完成最后接近
↓
真实 Structure 在相同方向实体化
```

---

# 25. Candidate Anchor Search

SpaceEncounterManager 应生成多个候选方向。

例如：

```text
1. forward

2. forward-right 25°

3. forward-left 25°

4. right 60°

5. left 60°

6. forward-up

7. forward-down
```

优先选择：

> 视觉上自然、接近飞船前方的方向。

但目标不需要永远严格位于船头正前方。

现实中的航天器接近设施本来也可能：

> 从侧面或斜前方保持距离。

---

# 26. Anchor Candidate 计算

对于每个候选方向：

```text
direction
    ↓
计算 DirectionalShipExtent
    ↓
读取 DirectionalEncounterExtent
    ↓
加入 PreferredEVAGap
    ↓
得到 Candidate Position
```

然后进入实际空间验证。

---

# 27. 必须检查真实方块碰撞

数学 Envelope 只是快速估算。

真正生成 Structure 之前必须验证：

```text
EncounterBounds
.inflate(SAFETY_MARGIN)
```

是否碰到：

- 飞船；
- 玩家扩建；
- 其他局部结构；
- 当前 Encounter 中其他实体化目标；
- Reserved Local Space。

若碰撞：

```text
Candidate rejected
```

继续尝试下一个方向。

---

# 28. Safety Margin

两个结构即使没有直接碰撞，也不能贴得太近。

建议：

```text
SAFETY_MARGIN
```

例如：

```text
8 ~ 24 blocks
```

实际值由目标类型决定。

目的：

- 避免视觉穿插；
- 给 EVA 留转向空间；
- 避免大型结构看起来互相贴脸；
- 给未来的 docking / debris 留余量。

---

# 29. 永远不能覆盖玩家建筑

Encounter Structure 实体化时：

```text
绝不能强制 replace 玩家已有方块
```

如果目标位置有东西：

```text
Candidate invalid
```

重新找 Anchor。

这是硬约束。

---

# 30. Encounter Placement Pipeline

正式流程：

```text
SpaceEncounterManager
        ↓
读取 ShipEnvelope
        ↓
读取 EncounterEnvelope
        ↓
生成 Candidate Directions
        ↓
计算 DirectionalShipExtent
        ↓
计算 Desired EVA Gap
        ↓
生成 Candidate Anchor
        ↓
StructureBounds + SafetyMargin
        ↓
Collision Validation
        ↓

    冲突？
    ├ YES → 下一个 Candidate
    └ NO
        ↓

锁定 LocalEncounterAnchor
        ↓
Remote Proxy 对齐该 Anchor
        ↓
执行 Approach
        ↓
进入 MATERIALIZING
        ↓
激活真实 Structure
```

---

# 31. Encounter 距离上限

玩家可能建造极端巨大的飞船。

例如：

```text
2000 × 2000 × 500
```

系统不应该无限向外寻找空间站生成位置。

应定义：

```text
MAX_LOCAL_ENCOUNTER_DISTANCE
```

例如未来测试：

```text
512
768
1024
```

具体值由：

- 性能；
- 加载距离；
- EVA 可玩性；
- Beacon；
- Chunk 管理

共同决定。

---

# 32. 找不到安全位置

如果在所有候选方向和最大局部距离内都无法找到安全位置：

```text
Encounter Activation
→ FAILED
```

例如错误：

```text
LOCAL_SPACE_OBSTRUCTED
```

N.O.V.A. 可以提示：

> 无法建立安全近距离接近轨道。当前舰体周围缺乏足够的机动空间。

不要因此禁止玩家扩建。

---

# 33. 不禁止大型飞船

不建议简单规定：

```text
玩家只能在 ±64 blocks 内建造飞船
```

Minecraft 的核心价值之一就是自由建造。

系统应该：

> 尽量适应玩家的大型和异形飞船。

只有当飞船真的塞满整个 Local Space：

> 才限制某些近距离 Encounter。

---

# 34. Ship Size 可以成为后期玩法

ShipEnvelope 将来可以用于计算：

```text
Ship Size
Ship Class
Approximate Ship Mass
```

例如：

```text
SHUTTLE
FRIGATE
CRUISER
CAPITAL
```

这不是简单 RPG 等级。

而是根据：

- 船体尺寸；
- 连接方块数量；
- 设备数量；
- 舱室体积

描述飞船实际规模。

---

# 35. 大型飞船可以产生系统需求

以后玩家不断扩建飞船，可以影响：

```text
Engine Capacity
Life Support Capacity
Gravity Coverage
Fuel Efficiency
Acceleration
Docking Options
```

例如：

```text
Basic Life Support
只能支持有限 Pressurized Volume
```

或者：

```text
Basic Gravity Generator
无法覆盖整艘巨舰
```

这样：

> 大型飞船本身就形成进度需求。

而不是通过建筑边界硬限制玩家。

---

# 36. Encounter Materialization

当：

```text
APPROACHING
```

结束后：

```text
FlightPhase → ARRIVE
```

并进入：

```text
ENCOUNTER_LOCK
```

此时：

```text
Remote Proxy
→ 淡出

Real Structure
→ 激活 / 加载
```

目标位置就是已经提前锁定的：

```text
LocalEncounterAnchor
```

---

# 37. Remote → Local 切换

理想视觉：

```text
空间站远景逐渐变大
↓
飞船减速
↓
空间站已经明显可辨识
↓
最后接近
↓
远景 Proxy 淡出
↓
真实 Minecraft Structure 接管
```

玩家不应该看到：

> 模型突然消失，一个方块建筑突然啪地出现。

可以利用：

- LOD；
- 淡入淡出；
- 船体姿态变化；
- 窗框遮挡；
- 短暂减速；
- 目标局部光照

隐藏切换。

---

# 38. Ship Dimension 环境规则

长期正确模型：

```text
整个 Ship Dimension 默认：

VACUUM
MICROGRAVITY / ZERO GRAVITY
```

飞船内部则通过环境系统提供：

```text
PRESSURIZED
ARTIFICIAL GRAVITY
```

这样玩家真正离开飞船结构后：

> 自然进入太空。

不需要传送或特殊假状态。

---

# 39. 氧气与重力分离

环境至少分成两个独立维度：

```text
Atmosphere
+
Gravity
```

例如：

| 状态 | Atmosphere | Gravity |
|---|---|---|
| 正常飞船舱室 | Pressurized | Artificial |
| 破舱飞船 | Vacuum | Artificial |
| 重力故障舱室 | Pressurized | Zero / Low |
| 船外太空 | Vacuum | Zero |
| 废弃空间站 | Zone-dependent | Zone-dependent |

这允许以后自然支持：

- 破舱；
- 泄压；
- 重力发生器故障；
- 部分空间站仍有空气；
- 部分空间站仍有重力。

---

# 40. 环境系统应复用

不要为：

```text
Ship
Station
Derelict
```

分别写：

```text
ShipOxygenSystem
StationOxygenSystem
WreckOxygenSystem
```

应该共享统一环境服务。

概念上：

```text
SpaceEnvironmentService

├ Atmosphere Zones
├ Gravity Zones
├ Vacuum
└ Player Environment Query
```

当前已有：

```text
ShipEnvironmentService
```

的话，可以逐步扩展职责。

---

# 41. Atmosphere Zone

以后可以使用：

```text
PressureZone
```

表示一个气密空气空间。

例如空间站：

```text
Docking Bay
→ VACUUM

Broken Corridor
→ VACUUM

Laboratory
→ PRESSURIZED

Command Module
→ PRESSURIZED
```

飞船也使用相同机制。

---

# 42. 第一版环境可以简化

EVA MVP 不要求立刻完成完整气压模拟。

第一版可以：

```text
Ship Interior Safe Zone
→ breathable

Outside Ship
→ vacuum

Relay Station
→ entirely vacuum
```

先验证：

> EVA 是否好玩。

后续再替换成：

```text
Dynamic Pressure Zones
```

---

# 43. EVA 装备

第一版玩家进行 EVA 至少需要：

```text
Life Support EPP
```

用于处理：

- 真空；
- 无氧；
- 基本宇航生存。

未来可以增加：

```text
EVA Thruster Module
```

作为：

- EPP Upgrade；
- Augment；
- 或独立设备。

---

# 44. EVA Movement

第一版不建议做完全真实的牛顿运动。

否则：

```text
按一次 W
→ 一直漂
→ 飞过空间站
→ 很难回来
```

游戏体验会很差。

建议：

```text
轻惯性
+
自动阻尼
```

概念：

```text
有输入
→ velocity += thrust

无输入
→ velocity *= damping
```

目标：

> 有明显失重感，但仍然好控制。

---

# 45. EVA Controls

可考虑：

```text
WASD
→ 前后左右推进

Space
→ 上升推进

Shift
→ 下降推进
```

移动方向基于摄像机。

并限制：

```text
MAX_EVA_SPEED
```

避免玩家变成创造模式飞行。

---

# 46. Ship Beacon

EVA 后玩家必须始终能找到自己的飞船。

建议：

```text
Ship Beacon
```

例如 HUD：

```text
FALCON
132 m
↑
```

同时可以显示：

```text
ABANDONED RELAY
48 m
↗
```

这对于黑色太空背景非常重要。

---

# 47. 气闸

正式版本中，飞船应拥有真正的：

```text
Airlock
```

结构：

```text
Interior
   │
Inner Door
   │
Airlock Chamber
   │
Outer Door
   │
Space
```

流程：

```text
进入气闸
↓
关闭 Inner Door
↓
减压
↓
打开 Outer Door
↓
EVA
```

返回流程相反。

---

# 48. MVP 气闸

第一版不要求模拟真实压力变化。

可以只实现：

- 两层门；
- 外部真空；
- 状态提示；
- EPP 检查。

减压动画和压力系统以后再做。

---

# 49. 空间站入口

不同 Encounter 不需要统一入口。

正常空间站：

```text
Outer Airlock
→ Airlock Chamber
→ Inner Door
```

废弃空间站：

```text
Broken Hull
→ Vacuum Interior
```

第一次废弃空间站尤其适合使用：

> 破损船体入口。

这比正常大门更有探索感。

---

# 50. 空间站必须利用太空环境

空间站不应只是：

> “放在太空里的普通 Dungeon。”

它应该使用自己的环境特性。

例如：

```text
外部太阳能板
→ 必须 EVA

Broken Corridor
→ Vacuum

Laboratory
→ 尚有压力

Reactor Section
→ 无重力

Power Failure
→ 黑暗

Life Support Repair
→ 改变整个区域环境
```

这样它才和地表遗迹真正不同。

---

# 51. Space Encounter 生命周期

建议：

```text
INACTIVE
↓
PREPARING
↓
APPROACHING
↓
MATERIALIZING
↓
ACTIVE
↓
LEAVING
↓
UNLOADED
```

---

## INACTIVE

POI 仅存在宏观 Universe 中。

---

## PREPARING

计算：

- ShipEnvelope；
- EncounterEnvelope；
- Candidate Anchor；
- LocalEncounterAnchor；
- Approach Direction。

---

## APPROACHING

显示：

```text
Remote Proxy
```

飞船完成接近。

---

## MATERIALIZING

真实 Encounter Structure：

```text
加载 / 生成
```

远景 Proxy：

```text
淡出
```

---

## ACTIVE

允许：

- EVA；
- 探索；
- 战斗；
- Loot；
- Interaction。

同时宏观航行锁定。

---

## LEAVING

确认所有船员已经安全回舰。

Encounter 准备卸载。

---

## UNLOADED

移除或保存当前局部 Encounter。

飞船重新回到普通宇宙导航状态。

---

# 52. SpaceEncounterManager

建议未来建立：

```text
SpaceEncounterManager
```

职责：

```text
Encounter lifecycle
CurrentEncounter

ShipEnvelope query
EncounterEnvelope query

Candidate Anchor generation
Collision validation

LocalSpaceFrame
Structure activation
Structure unload

EVA presence
Warp lock

Persistence
```

不应该负责：

- 星图 UI；
- 行星渲染；
- 玩家氧气具体计算；
- UniverseCatalog；
- N.O.V.A. 剧情逻辑。

---

# 53. 多人：不能抛下 EVA 玩家

这是硬规则。

例如：

```text
Player A
在飞船内

Player B
正在空间站
```

A 尝试启动跃迁：

```text
Warp Request
→ DENIED
```

原因：

```text
CREW OUTSIDE VESSEL
```

N.O.V.A.：

> 检测到船员仍位于舰外。推进系统已锁定。

---

# 54. 如何判断玩家已经回船

不要简单使用：

```text
distanceToShipCenter < X
```

而应该最终依赖：

```text
isAboardShip(player)
```

例如通过：

- ShipStructure；
- Interior Zone；
- Pressure Zone；
- Ship-owned region

判断。

所有参与玩家安全返回后：

```text
Warp Enabled
```

---

# 55. Emergency Recall

未来可以考虑：

```text
Emergency Recall
```

用于：

- 玩家卡死；
- 掉进无法返回区域；
- 管理员恢复；
- 特殊剧情。

但它不应成为常规 EVA 回程方式。

正常玩法：

> 玩家自己飞回船。

---

# 56. Persistent Encounter

空间 POI 可以分为：

```text
Temporary
Persistent
```

Temporary：

- 随机残骸；
- 临时资源点；
- 小事件。

可以在离开后清理。

Persistent：

- 主线空间站；
- 重要科研站；
- 独特废船；
- 可重复访问设施。

需要保存。

---

# 57. 玩家修改后的空间站

如果允许玩家：

- 拆方块；
- 建方块；
- 放机器；
- 改造设施；

那么 Persistent Encounter 最可靠的办法通常是：

> 保留真实 world chunk。

不要试图把所有玩家改动编码成几十个状态字段。

---

# 58. Encounter Storage

因为所有 Encounter 逻辑上共享 Ship Dimension，不能把所有真实结构都永久放在飞船旁边。

后期可以选择：

```text
Encounter Storage Regions
```

例如：

```text
Station A
→ remote storage region

Station B
→ another region
```

进入 Encounter 时：

> 将对应 Instance 激活到当前 Local Space。

具体 Chunk / Instance 方案需要以后单独设计。

MVP 可以只支持一个测试 Encounter。

---

# 59. Gas Giant 与 Orbital Content

该系统尤其适合 Gas Giant。

Gas Giant 不需要：

> 强行制造一个可登陆的气态地面。

它可以成为：

> 轨道玩法中心。

例如：

```text
Gas Giant
├ Research Station
├ Fuel Refinery
├ Broken Freighter
├ Atmospheric Probe
├ Pirate Relay
└ Mining Platform
```

这样没有 Surface：

> 反而成为它独特的玩法身份。

---

# 60. Asteroid Encounter

同一系统未来可以支持：

```text
Asteroid Encounter
```

流程：

```text
飞船抵达
→ 小行星真实实体化
→ EVA
→ 附着 / 低重力移动
→ 采矿
→ 返回飞船
```

不需要新维度。

---

# 61. Derelict Ship

也可以支持：

```text
Derelict Ship
```

例如：

- 无空气；
- 无重力；
- 漂浮物品；
- 破损电力；
- 生物巢穴；
- 可回收科技；
- 飞船模块。

玩家可以真正：

> 从自己的船飞进另一艘船。

---

# 62. 大型飞船与 Docking

以后大型飞船可以发展：

```text
Docking Port
Hangar Bay
```

例如：

### 小型目标

```text
Probe
Cargo Pod
Small Shuttle
```

可以进入：

```text
Hangar Encounter Anchor
```

### 大型空间站

可以使用：

```text
Docking Port
↔
Ship Docking Port
```

但这些都属于后期。

EVA MVP 不依赖 Docking 系统。

---

# 63. 第一版测试 Encounter

建议第一版只做：

# Abandoned Relay Station

目的：

> 验证完整玩法闭环，而不是做很多内容。

---

# 64. Relay Station 内容

空间站保持小型：

```text
Exterior
├ Broken Antenna
├ Solar Panel
└ Broken Hull Entrance

Interior
├ Entry Corridor
├ Control Room
├ Storage Room
└ Maintenance Room
```

大约：

```text
4 个小房间
```

已经足够。

---

# 65. MVP 完整流程

```text
1.
N.O.V.A. 发现异常信号

2.
星图出现 Abandoned Relay Station

3.
玩家选择导航

4.
SpaceEncounterManager 进入 PREPARING

5.
读取当前 ShipEnvelope

6.
读取 Relay Station EncounterEnvelope

7.
搜索安全 LocalEncounterAnchor

8.
锁定 Approach Direction

9.
飞船开始接近

10.
远距离 Remote Proxy 出现

11.
Proxy 随飞船接近逐渐变大

12.
飞船减速

13.
抵达 Encounter

14.
Relay Station 在动态 Anchor 处实体化

15.
N.O.V.A.：
目标无响应，建议执行 EVA

16.
玩家装备 Life Support EPP

17.
玩家进入气闸

18.
进入真空

19.
通过 EVA 飞向空间站

20.
从 Broken Hull 进入

21.
空间站内部全部保持真空

22.
玩家探索

23.
控制室获得 Guaranteed Tech Component

24.
任务状态更新

25.
玩家 EVA 返回自己的飞船

26.
服务器确认所有船员已回舰

27.
Encounter 进入 LEAVING

28.
飞船恢复导航能力
```

---

# 66. MVP 暂时不做

第一版明确不实现：

- 完整空气流体模拟；
- 高精度气压；
- 复杂泄压动画；
- 完全牛顿物理；
- 多艘独立玩家飞船；
- 动态旋转空间站；
- 程序化空间站；
- 空间舰战；
- 敌舰 AI；
- 飞船炮塔；
- Docking Clamp；
- Hangar Capture；
- 多 Ship Core；
- 真实移动整艘方块船；
- 完整 ShipStructureGraph；
- 精确物理质量；
- 大型供电网络；
- 复杂氧气管道。

先验证：

> **“走出自己的飞船探索太空目标”本身是否足够好玩。**

---

# 67. 推荐实施阶段

## E0 — Encounter 基础数据

新增概念：

```text
CurrentEncounter
SpaceEncounter
SpaceEncounterManager
LocalSpaceFrame
```

先硬编码 Relay Station。

---

## E1 — ShipEnvelope MVP

实现：

```text
Dynamic Ship Bounds
```

第一版允许粗略 AABB。

---

## E2 — Directional Extent

根据候选方向计算：

```text
DirectionalShipExtent
```

避免侧面长天线错误推远 Encounter。

---

## E3 — EncounterEnvelope

Relay Station 提供：

```text
Structure Bounds
```

---

## E4 — Anchor Candidate Search

实现基本候选：

```text
Forward
Forward Left
Forward Right
Left
Right
Forward Up
Forward Down
```

---

## E5 — Collision Validation

检查：

```text
EncounterBounds
+
SafetyMargin
```

绝不覆盖玩家方块。

---

## E6 — Remote Proxy

实现空间站远景表现：

```text
Point
→ Silhouette
→ Simplified Proxy
```

并让它与最终 LocalEncounterAnchor 对齐。

---

## E7 — Structure Materialization

使用：

```text
abandoned_relay_station.nbt
```

在 Anchor 位置实体化。

---

## E8 — Vacuum MVP

Ship Dimension 默认：

```text
VACUUM
```

飞船内部继续提供安全区。

Relay Station：

```text
全部真空
```

---

## E9 — EVA Movement

加入：

```text
EPP Thruster
Damping
Speed Limit
Vertical Movement
```

---

## E10 — Beacon

HUD 显示：

```text
Ship Direction
Ship Distance

Encounter Direction
Encounter Distance
```

---

## E11 — Multiplayer Warp Lock

如果任意玩家不在飞船安全区域：

```text
StartWarp
→ DENIED
```

---

## E12 — Gameplay Loop

加入：

```text
Guaranteed Tech Component
```

以及任务完成条件。

最终验收：

```text
发现
→ 飞过去
→ EVA
→ 探索
→ 获取奖励
→ EVA 回船
→ 离开
```

---

# 68. 长期架构

```text
UniverseCatalog
│
├ StarSystemDefinition
├ CelestialBodyDefinition
└ SpacePoiDefinition
        │
        ├ Station
        ├ Wreck
        ├ Asteroid
        └ Anomaly


ShipStateData
│
├ CurrentEntry
├ UniversePosition
├ Fuel
├ FlightState
└ CurrentEncounter


Ship Core
│
└ ShipStructureGraph
       │
       └ ShipEnvelope


ShipFlightController
│
└ Macro Travel


SpaceEncounterManager
│
├ Encounter Lifecycle
├ EncounterEnvelope
├ Candidate Search
├ LocalEncounterAnchor
├ LocalSpaceFrame
├ Structure Activation
├ EVA Presence
├ Warp Lock
└ Persistence


Space Renderer
│
├ Celestial Bodies
├ Stars
└ Remote Space POI Proxy


SpaceEnvironmentService
│
├ Pressure Zones
├ Vacuum
├ Gravity Zones
└ Player Environment


EVA Controller
│
├ Thrusters
├ Damping
├ Speed Limit
└ Beacon
```

---

# 69. 关键约束

实现过程中必须遵守：

1. **不要移动真实玩家飞船方块。**
2. **不要破坏现有 `UniversePosition`。**
3. **Space POI 不进入 `CurrentEntry`。**
4. **使用独立 `CurrentEncounter`。**
5. **EVA 必须发生在真实 Minecraft 世界空间。**
6. **空间站最终必须是真实可碰撞结构。**
7. **远景 Proxy 与近距离 Structure 分离。**
8. **Encounter Anchor 必须动态计算。**
9. **固定 140 格只能作为示意，不是规则。**
10. **玩家扩建必须受到系统支持。**
11. **绝不让 Encounter Structure 覆盖玩家建筑。**
12. **多人情况下不能抛下 EVA 玩家。**
13. **飞船、空间站和废船尽量共用环境系统。**
14. **ShipEnvelope 与 Pressure Zone 是不同概念。**
15. **第一版优先游戏体验，而不是物理真实性。**
16. **不要为了未来多飞船体系提前过度设计。**

---

# 70. 核心设计公式

动态 Encounter 放置可以概括为：

```text
ShipEnvelope
+
DirectionalShipExtent
+
EncounterEnvelope
+
PreferredEVAGap
+
SafetyMargin
+
CandidateDirectionSearch
+
WorldCollisionValidation
=
LocalEncounterAnchor
```

这意味着：

> Encounter 根据玩家的飞船寻找位置。

而不是：

> 玩家必须把飞船限制在系统预设大小内。

---

# 71. 最终体验目标

Local Space / EVA 系统最终不是为了做真实宇航模拟。

它的目标是：

> 让玩家真正相信自己的飞船停在一个巨大宇宙中。

最关键的一刻应该是：

> 玩家打开外舱门。

外面没有传送按钮。

只有：

- 黑暗的太空；
- 远处悬浮的空间站；
- 身后亮着灯的自己的飞船。

玩家离开安全舱室：

> 真正飞向那个目标。

探索结束之后回头：

> 仍然能看到自己的船停在那里等他。

如果这一体验成立：

> StarboundMC 的飞船就不再只是星图菜单的载体。

它真正成为：

> **玩家在宇宙中的家。**