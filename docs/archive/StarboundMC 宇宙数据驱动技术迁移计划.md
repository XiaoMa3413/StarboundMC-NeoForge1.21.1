# StarboundMC 宇宙数据驱动技术迁移计划

> 状态：已完成并归档（基线提交 `b12b7ec`）。宇宙 Definition、Codec、数据包注册、UniverseCatalog、
> 星图/航行迁移和旧存档兼容已经落地；客户端画面与手感的实机复核另见
> [已发现问题](../known-issues.md)。本文中的批次指令、未来时态和“不 commit、不 push”只记录当时
> 的施工流程，不再是当前开发指令。
>
> 本文用途：Codex 实际进行基础架构重构时使用。  
> 本文只处理技术迁移。  
> 不加入新的行星玩法、不重新平衡游戏。

---

# 1. 总原则

整个迁移必须：

```text
小步完成
每一步可编译
每一步可测试
每一步尽量保持现有游戏行为
```

禁止一次性重写整个宇宙系统。

必须保留：

```text
当前星球位置
当前恒星位置
当前航线
当前燃料消耗
当前跃迁时间
当前视觉
当前剧情门禁
当前老存档
```

---

# 2. 最终技术模块

新增包建议：

```text
com.starboundmc.world.universe
```

包括：

```text
StarSystemDefinition
CelestialBodyDefinition
BodyOrbitDefinition
BodyNavigationProfile
BodySpaceVisualProfile
BodySurfaceDefinition
PlanetEnvironmentProfile

UniverseCatalog
ModUniverseRegistries
LegacyUniverseCompatibility
```

---

# 3. 数据模型

## StarSystemDefinition

至少包含：

```text
systemId
nameKey
descriptionKey
starTypeKey
stellarVisual
navigationCenter
influenceRadius
bodies
```

---

## CelestialBodyDefinition

至少包含：

```text
entryId
nameKey
typeKey
descriptionKey

threatLevel

orbit
starmapVisual

optional navigation
optional spaceVisual
optional surface
```

---

# 4. Navigation Profile

负责替代目前 `ShipSpace` 中的硬编码。

字段：

```text
dockPosition
bodyPosition
bodyRadius
dockYaw
```

必须精确迁移当前四颗可航行星球的数据。

不能重新调整位置。

---

# 5. Space Visual Profile

负责逐步替代 `PlanetRenderer` 中的：

```text
texture
atmosphereColor
atmospherePeak
orientation
pointColor
```

第一阶段：

只复制旧值。

不要调整美术效果。

---

# 6. Surface Definition

至少包含：

```text
dimension
landingPolicy
environment
```

第一版 LandingPolicy：

```text
OVERWORLD_RESPAWN
SURFACE_SCAN
```

即可。

---

# 7. Datapack Registry

第一版只建立：

```text
StarSystemDefinition Registry
```

Body 直接作为 system 内部列表。

不要创建三个相互引用的独立 Registry。

使用 NeoForge 自定义 datapack registry。

必须：

```text
有 Codec
同步给客户端
```

因为客户端需要知道：

```text
星系坐标
天体坐标
天体半径
停靠位置
视觉信息
```

用于与服务器一致地重放飞行曲线。

---

# 8. UniverseCatalog

Registry 加载后建立 immutable runtime index。

至少提供：

```text
system(systemId)

body(entryId)

systemOfBody(entryId)

bodyByDimension(dimension)

allSystems()

allBodies()

navigableBodies()

surfaceBodies()

spaceRenderedBodies()

spatialIndex()
```

内部索引：

```text
systemsById
bodiesById
systemByBodyId
bodyByDimension
GalaxySpatialIndex
```

---

# 9. 第一阶段 A0：锁定旧行为

先运行现有测试。

重点：

```text
Stage7WarpTest
ShipFlightControllerUniverseTest
ShipStateDataTest
StarSystemLayoutTest
StarmapGalaxyGraphTest
GalaxySpatialIndex tests
Payload tests
```

新增：

```text
LegacyUniverseSnapshotTest
```

锁定当前：

```text
system IDs
entry IDs
threat
orbit
parent
star positions
system centres
influence radius
dock positions
body positions
body radius
dock yaw
textures
关键视觉参数
```

A0 不修改生产逻辑。

---

# 10. A1：Definition + Codec

新增所有 data classes。

增加 Codec。

推荐覆盖：

```text
SectorCoordinate
UniversePosition
StellarDistanceResponse
StellarVisualProfile
StarmapBodyVisual
BodyOrbitDefinition
BodyNavigationProfile
BodySpaceVisualProfile
BodySurfaceDefinition
PlanetEnvironmentProfile
CelestialBodyDefinition
StarSystemDefinition
```

UniversePosition 继续采用：

```text
sector
+
local coordinate
```

禁止退回巨大绝对坐标。

---

# 11. A2：Registry 与内置数据

注册同步：

```text
STAR_SYSTEM
```

datapack registry。

通过 datagen 生成现有：

```text
sys1
sys2
```

其中包括：

```text
sys1:barren
sys1:lush
sys1:molten
sys1:gasgiant
sys1:rockymoon
sys2:frozen
```

所有当前数值必须来自现有代码。

不要重新设计。

---

# 12. A3：UniverseCatalog

从 Registry 创建 Catalog。

客户端和服务器分别拥有自己的 Catalog。

服务器停止时清空。

客户端：

```text
logout
换服务器
退出单机
```

时清空。

建立新旧一致性测试。

此阶段结束时：

> 游戏依然继续使用 Planet / StarSystems / ShipSpace。

新系统暂时只作为第二份数据源存在。

这是第一个安全验收点。

---

# 13. A4：星图只读查询迁移

逐步将：

```text
StarSystems.byId
StarSystems.entryById
StarSystems.all
StarSystems.spatialIndex
```

替换为：

```text
ClientUniverseCatalog
```

重点检查：

```text
client/starmap/*
ShipConsoleScreen
StarmapOverlayRenderer
StarmapGeometry
StarSystemResolver
FrozenSkyRenderer
WarpFlashOverlay
StarmapGalaxyGraph
StarmapGalaxyGraphJson
```

不要改变星图交互。

---

# 14. galaxy_graph.json

继续放在：

```text
assets/starboundmc/starmap/
```

不要迁入服务器 datapack。

它继续负责：

```text
银河图二维节点位置
UI 路线
```

服务器不能相信它来决定跃迁权限。

---

# 15. A5：ShipSpace 迁移

把：

```text
Map<Planet, ...>
```

迁成通过 body ID 查询。

例如：

```text
universeDock(entryId)
bodyPosition(entryId)
radius(entryId)
dockYaw(entryId)
```

`ShipSpace` 最终只留下：

```text
数学工具
坐标计算工具
```

而不保存宇宙数据库。

---

# 16. A5：ShipFlightController

当前：

```text
Planet from
Planet target
```

迁成：

```text
fromEntryId
targetEntryId
```

或者 immutable route context。

推荐增加：

```text
FlightRouteContext
```

保存：

```text
起点导航几何
终点导航几何
需要避障的天体几何
```

这样服务器和客户端都调用同一套确定性算法。

---

# 17. Flight Route Cache

不能继续使用：

```text
Planet.ordinal()
```

作为 route cache key。

改成：

```text
fromEntryId + targetEntryId
```

Catalog 重建后必须清缓存。

---

# 18. 飞行避障兼容

第一轮只让：

```text
navigation != null
```

的四颗现有星球参与避障。

不要突然让：

```text
gas giant
rocky moon placeholder
```

参与现有路线计算。

否则会改变航线。

---

# 19. A6：ClientPlanetState

从：

```text
Planet current
Planet warpTarget
```

迁成：

```text
currentEntryId
targetEntryId
```

需要 body 信息时：

```text
ClientUniverseCatalog.body(id)
```

---

# 20. ShipPoseProvider

删除：

```text
Planet currentBody()
Planet targetBody()
```

改成：

```text
String currentBodyId()
String targetBodyId()
```

这样 renderer 不再依赖 Planet enum。

---

# 21. 网络迁移

现有协议已经有：

```text
StartWarpPacket.entryId
SyncStarStatePacket.currentEntryId
SyncFlightPacket.targetEntryId
WarpStartPacket.entryId
```

因此不要重新设计整个 warp protocol。

迁移时：

先让客户端忽略：

```text
WarpStartPacket.planetId
```

改为使用：

```text
entryId
```

解析目标。

---

# 22. SyncPlanetPacket

不要第一时间删除。

顺序：

```text
ClientPlanetState 不再需要 planetId
↓
WarpStartPacket 不再需要 planetId
↓
arrival sound 脱离 SyncPlanetPacket
↓
测试
↓
删除 SyncPlanetPacket
↓
更新 PROTOCOL_VERSION
```

特别注意：

当前抵达音效依赖：

```text
SyncPlanetPacket handler
```

消费 arrival cue。

删除之前必须迁移：

```text
WarpSounds.onWarpFinished()
```

---

# 23. A7：ShipStateData

到这里才开始迁存档。

未来：

```text
CurrentEntry
```

是唯一权威位置。

增加：

```text
SchemaVersion
```

---

# 24. 老存档迁移

旧存档如果有：

```text
CurrentEntry
```

并且有效：

优先使用。

否则读取：

```text
Planet
```

映射：

```text
lush   → sys1:lush
barren → sys1:barren
molten → sys1:molten
frozen → sys2:frozen
```

禁止未知 ID 自动变成：

```text
LUSH
```

---

# 25. 未知 body

如果旧存档保存：

```text
othermod:planet_x
```

但是对应 datapack 消失：

不要偷偷改成 Lush。

应该：

```text
保留原始 ID
记录 WARN
禁止向未知地点继续航行
允许玩家留在飞船维度
```

后续可以增加管理员修复命令。

---

# 26. A7：ShipWarpManager

将：

```text
getCurrentPlanet()
```

迁成：

```text
getCurrentEntryId()
```

航行判断通过 Catalog。

燃料：

```text
同星系 = 20
跨星系 = 100
```

保持不变。

禁止继续通过：

```text
sys1:lush
```

字符串前缀判断星系。

必须：

```text
catalog.systemOfBody(entryId)
```

---

# 27. A8：Surface Travel

删除：

```text
switch (Planet)
```

目标 dimension 从：

```text
body.surface.dimension
```

获取。

把：

```text
BarrenPlanet
FrozenPlanet
MoltenPlanet
```

重复的 surface spawn scan 抽成：

```text
SurfaceLandingService
```

不要改变当前安全登陆算法。

---

# 28. A9：PlanetRenderer

这是客户端最后的大模块。

不要每帧：

```text
遍历 registry
创建 List
创建 Map
```

Catalog 建立时提前生成：

```text
SpaceRenderBody[]
```

Renderer 使用稳定数组。

迁移现有：

```text
纹理
大气颜色
大气强度
固定朝向
点颜色
LOD
```

确保当前四颗星球视觉不变。

---

# 29. MoltenMoonRenderer

只替换：

```text
Planet.MOLTEN.texture()
```

的数据来源。

不要顺手把月相系统泛化。

那是后续任务。

---

# 30. A10：Legacy Cleanup

只有以下全部完成后：

```text
Starmap 无 StarSystems
Flight 无 Planet
ShipSpace 无 Planet
Network 无 Planet
Client state 无 Planet
Renderer 无 Planet
Surface travel 无 Planet switch
SavedData 以 CurrentEntry 为权威
```

才删除生产环境中的：

```text
Planet
StarSystems
```

---

# 31. Codex 批次

严格按：

```text
Batch 1 = A0–A3
Batch 2 = A4
Batch 3 = A5
Batch 4 = A6
Batch 5 = A7–A8
Batch 6 = A9–A10
```

执行。

一次只做一个 Batch。

---

# 32. Batch 1 Codex 指令

第一轮直接给 Codex：

> 只执行 A0–A3。
>
> 建立宇宙 Definition、Codec、同步 StarSystem datapack registry 和 UniverseCatalog。
>
> 将当前 sys1/sys2、6 个 body、6 个可航行 body（其中 5 个有可登陆表面）的现有数据精确复制进新数据层。
>
> 不修改现有 entry ID。
>
> 不迁移 WarpManager、ShipStateData、ShipFlightController、ClientPlanetState、PlanetRenderer 或网络。
>
> 建立 legacy-vs-new equivalence tests。
>
> 当前游戏生产逻辑继续使用旧 Planet / StarSystems / ShipSpace。
>
> 完成后运行 test、build，并确认 GameTest。
>
> 不 commit，不 push。

---

# 33. 实际完成记录（2026-09-17 归档）

以下项目已经在 `b12b7ec` 落地，并由对应测试覆盖：

- `StarSystemDefinition` 来自 datapack registry，客户端和服务端各自维护 `UniverseCatalog`。
- 星图、`GalaxySpatialIndex`、航行控制器、舱外渲染和地表旅行均通过 entry ID 与 catalog 查询数据。
- `CurrentEntry` 是飞船位置的权威值；旧 `Planet` 字段只保留为字符串兼容信息。
- 旧存档迁移、航行行为、燃料消耗和剧情门禁保持兼容，相关回归测试已加入当前测试集。
- 当前内置宇宙包含两个恒星系、六个天体，其中五个有可登陆表面，气态巨行星仅支持轨道停靠。

视觉观感、航行手感和旧存档副本的完整客户端复核不由这份历史施工计划继续跟踪，统一记录在
[已发现问题](../known-issues.md)；后续若改变宇宙数据，应新建活动计划。
