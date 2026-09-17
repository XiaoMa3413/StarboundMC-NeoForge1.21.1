# StarboundMC 宇宙系统总设计

> 项目：StarboundMC-NeoForge1.21.1  
> 平台：Minecraft 1.21.1 / NeoForge / Java 21  
> 本文用途：说明为什么要重构、最终要变成什么样。  
> 不包含具体施工步骤。

---

# 1. 为什么需要这次改造

目前项目已经拥有：

- 飞船维度
- 星图
- 亚光速飞行
- 跨星系航行基础
- 连续宇宙坐标
- 行星渲染
- 行星维度
- 飞船存档
- 服务端权威跃迁
- N.O.V.A. 剧情基础

现在的问题不是“功能太少”，而是：

> 宇宙内容继续增加以后，现有 Java 硬编码方式会越来越难维护。

目前一个普通行星可能同时涉及：

```text
Planet
StarSystems
PlanetEntry
ShipSpace
ShipFlightController
ShipWarpManager
ShipStateData
ClientPlanetState
PlanetRenderer
Stage6TravelService
Network
Starmap
```

现在只有 4 个可登陆世界，还能维护。

如果未来有：

```text
20 个星球
5 个恒星系
空间站
不可登陆气态巨行星
几十种环境
大量 POI
```

继续使用 enum + switch 会迅速失控。

---

# 2. 最终目标

以后增加一颗普通星球时，希望主要工作变成：

```text
写星球数据
↓
配置世界生成
↓
配置资源
↓
配置结构
↓
配置贴图
↓
完成
```

而不是：

```text
修改十几个 Java 类
↓
加入多个 switch
↓
修改 renderer
↓
修改 warp
↓
修改存档
↓
修改网络
```

---

# 3. 最终结构

最终宇宙结构：

```text
Datapack
   │
   ▼
StarSystemDefinition
   │
   ├─ 恒星
   ├─ 星系坐标
   └─ CelestialBodyDefinition[]
            │
            ├─ 星图信息
            ├─ 轨道信息
            ├─ 航行信息
            ├─ 太空视觉
            ├─ 可登陆表面
            └─ 环境信息
                    │
                    ▼
              UniverseCatalog
                 /      \
                /        \
             Server      Client
              │            │
          Warp/Save     Render/Starmap
          Travel        Flight Visual
```

---

# 4. 核心概念

## StarSystemDefinition

代表一个恒星系。

例如：

```text
sys1
sys2
```

负责：

```text
名称
恒星视觉
宇宙空间位置
影响范围
拥有的天体
```

---

## CelestialBodyDefinition

代表星图上的一个天体。

例如：

```text
sys1:lush
sys1:molten
sys1:gasgiant
```

一个天体不一定能够登陆。

它可以拥有三个独立能力：

```text
navigation
spaceVisual
surface
```

### navigation

表示：

> 飞船能不能把它当作航行目的地。

例如：

```text
行星
空间站
大型卫星
```

都可以有 navigation。

### spaceVisual

表示：

> 在飞船窗外是否实际渲染这个天体。

例如：

```text
行星
气态巨行星
大型卫星
```

### surface

表示：

> 玩家能不能传送到它的表面世界。

例如：

```text
Lush
Barren
Frozen
Molten
```

拥有 surface。

而：

```text
Gas Giant
```

可以：

```text
有视觉
但不能登陆
```

以后空间站则可以：

```text
可以飞过去
但没有 planet surface
```

---

# 5. UniverseCatalog

游戏代码以后不应该到处直接读取 Registry。

统一通过：

```text
UniverseCatalog
```

查询。

例如：

```text
通过 entryId 找 body
通过 body 找 system
通过 dimension 找 body
列出所有可航行 body
列出所有可登陆 body
列出附近恒星系
```

它相当于：

> 游戏运行时的宇宙数据库索引。

---

# 6. ID 规则

本轮重构必须保留当前 ID：

```text
sys1:lush
sys1:barren
sys1:molten
sys1:gasgiant
sys1:rockymoon
sys2:frozen
```

不要趁这次重构改成其他格式。

原因是这些 ID 已经存在于：

```text
存档
网络
星图
visited
flight target
测试
```

以后如果需要改 ID：

单独做一次存档迁移。

---

# 7. CurrentEntry 成为飞船当前位置

目前飞船状态同时存在：

```text
Planet
CurrentEntry
```

未来应该只有：

```text
CurrentEntry
```

作为真正的位置身份。

例如：

```text
CurrentEntry = sys1:barren
```

然后系统通过 UniverseCatalog 查询：

```text
它属于哪个星系
它的坐标
它能否登陆
它是什么维度
它有什么环境
```

---

# 8. Planet enum 最终退出生产系统

当前：

```text
Planet.LUSH
Planet.BARREN
Planet.MOLTEN
Planet.FROZEN
```

参与很多逻辑。

不能突然删除。

迁移顺序应该是：

```text
先建立新数据
↓
让旧代码兼容新数据
↓
逐模块迁移
↓
Planet 不再被生产代码使用
↓
只保留老存档兼容
↓
最终删除
```

---

# 9. 星图与真实宇宙分开

当前：

```text
assets/starboundmc/starmap/galaxy_graph.json
```

继续负责：

```text
星系在 UI 中的位置
星系之间画什么线
```

这是客户端视觉数据。

真实宇宙位置来自：

```text
StarSystemDefinition
```

例如：

```text
sys2 真正距离 sys1 四万宇宙单位
```

和：

```text
星图上 sys2 放在右下角
```

是两件事情。

不能混在一起。

---

# 10. 服务端仍然是权威

客户端可以：

```text
画星图
预测飞船运动
播放航行动画
渲染星球
```

但不能决定：

```text
是否允许跃迁
是否扣燃料
是否解锁星系
是否完成剧情
是否获得战利品
```

这些继续由服务器决定。

---

# 11. 必须保留连续宇宙坐标

现有：

```text
SectorCoordinate
+
UniversePosition
```

设计必须保留。

不要重新使用：

```text
一个超大的 Vec3
```

表示整个宇宙。

当前结构已经为未来非常遥远的恒星系解决了浮点精度问题。

---

# 12. 重构结束后的效果

完成之后：

增加普通新星球应该不需要修改：

```text
ShipWarpManager 核心逻辑
ShipFlightController 核心逻辑
ClientPlanetState 核心逻辑
Stage6TravelService switch
PlanetRenderer 主流程
```

只需要增加：

```text
宇宙数据
世界生成
环境
资源
结构
战利品
视觉资源
翻译
```

---

# 13. 最终玩家体验

技术改造最终是为了支持：

```text
修复飞船
↓
前往新星球
↓
登陆探索
↓
发现特殊环境
↓
收集当地资源
↓
探索遗迹
↓
获得新装备
↓
升级 EPP / Matter Manipulator / 飞船
↓
进入更危险的星球
↓
修复 Hyperdrive
↓
前往新的恒星系
```

目标不是单纯“能飞到更多地方”。

目标是：

> 每一个新星球都有去那里的理由。