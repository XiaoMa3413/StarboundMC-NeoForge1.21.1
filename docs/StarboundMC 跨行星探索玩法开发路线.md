# StarboundMC 跨行星探索玩法开发路线

> 前置条件：宇宙数据驱动基础迁移基本稳定。  
> 本文重点：让“飞去别的星球”真正有游戏意义。

---

# 1. 最终想解决的问题

目前已经能：

```text
打开星图
↓
选择星球
↓
飞过去
```

下一步要解决：

> 玩家为什么要飞过去？

理想答案：

```text
不同环境
不同资源
不同遗迹
不同装备
不同危险
不同剧情
```

---

# 2. 第一阶段不要增加大量星球

先做好现有：

```text
Lush
Barren
Molten
Frozen
```

四个世界。

目标：

> 四个世界看起来、玩起来、奖励上都明显不同。

---

# 3. 推荐整体 progression

```text
新世界
↓
N.O.V.A.
↓
Lush
↓
Matter Manipulator / 基础资源
↓
制造 Sublight Ignition Core
↓
修复 Sublight Engine
↓
Barren
↓
获得新资源 / EPP 材料 / POI 战利品
↓
环境装备升级
↓
Molten / 更危险世界
↓
高级材料
↓
修复 Hyperdrive
↓
第二恒星系
↓
Frozen
↓
高级科技 / 高级探索
```

---

# 4. 行星环境系统

新增：

```text
PlanetHazardService
```

不要叫：

```text
ShipEnvironmentService
```

因为项目已经有这个名字，用于飞船剧情和设备状态。

---

# 5. 环境属性

第一版支持：

```text
Breathability
Cold
Heat
Radiation
```

Gravity 可以先只显示：

```text
gravityScale
```

暂时不要修改 Minecraft 玩家重力。

---

# 6. 服务端检查

服务器每：

```text
20 tick
```

左右检查玩家。

流程：

```text
玩家当前 dimension
↓
UniverseCatalog.bodyByDimension
↓
读取 environment
↓
读取玩家 protection
↓
应用环境效果
```

客户端只负责：

```text
HUD
提示
视觉
```

不能决定免疫。

---

# 7. EPP

EPP 推荐使用项目现有的：

```text
Data Component
```

方式。

新增：

```text
EppProtection
```

例如：

```text
breathing
coldTier
heatTier
radiationTier
```

---

# 8. 不要制作“一件最终套装解决全部环境”

更有意思的做法：

```text
EPP I
→ 呼吸 / 基础防护

EPP II
→ 寒冷

EPP III
→ 高温

高级模块
→ 辐射
```

也可以允许玩家根据目的地切模块。

这样探索新星球前：

> 准备装备本身也是玩法。

---

# 9. Lush 定位

Lush 是起始世界。

主要负责：

```text
基础生存
基础矿物
基础体素
Matter Manipulator
Sublight repair progression
```

不需要非常危险。

玩家在这里学习系统。

---

# 10. Barren 定位

Barren 应该成为：

> 玩家修好亚光速引擎后，第一个真正主动前往的异星世界。

推荐内容：

```text
科技废料
中级矿物
EPP 基础材料
机械遗迹
坠毁点
废弃矿区
```

危险不要太高。

---

# 11. Molten 定位

Molten：

```text
高温
高风险
高价值
```

推荐：

```text
耐热材料
高级金属
工业遗迹
地热设施
熔岩环境资源
```

让玩家明确感觉：

> 没准备好装备就不适合来。

---

# 12. Frozen 定位

Frozen 位于第二恒星系。

因此应该有更明显的高级奖励。

推荐：

```text
寒冷环境
高级科技材料
科研设施
被冻结的遗迹
Hyperdrive 后续科技
高级装备
```

让跨恒星系本身具有奖励感。

---

# 13. 行星资源

每颗星球至少有：

```text
1 个 progression 必需资源
+
1 个高价值可选资源
```

例如：

```text
Barren
→ EPP 材料

Molten
→ 高温工业材料

Frozen
→ Hyperdrive / 高阶科技材料
```

这样：

> 去另一个星球不是为了换个颜色挖铁矿。

---

# 14. 微型地牢与 POI

第一版：

每个非起始星球至少：

```text
2 种 POI
```

---

# 15. Barren POI

例如：

```text
Abandoned Mining Camp
Crash Site
```

奖励：

```text
工业材料
旧科技组件
日志
蓝图
武器
```

---

# 16. Frozen POI

例如：

```text
Frozen Laboratory
Buried Signal Station
```

奖励：

```text
高级科技组件
N.O.V.A. 剧情
高阶武器
Hyperdrive 相关材料
```

---

# 17. Molten POI

例如：

```text
Thermal Research Station
Ruined Industrial Facility
```

奖励：

```text
耐热组件
能源科技
高阶矿物
特殊装备
```

---

# 18. POI 设计原则

不要做成：

```text
建筑
+
普通 Minecraft 箱子
```

每个 POI 至少应该有一种：

```text
独特战利品
独特材料
蓝图
扫描目标
剧情
特殊敌人
生成武器
```

---

# 19. 探索节奏

希望玩家正常登陆后：

```text
5～15 分钟
```

左右有机会遇到第一个有意义 POI。

不要让玩家：

```text
跑 40 分钟
什么都没看到
```

---

# 20. 程序化武器

POI 有了以后再做。

推荐：

```text
一个 Generic Weapon Item
+
GeneratedWeaponData Data Component
```

不要为了每种随机武器注册一个 Item。

---

# 21. 第一版武器系统

只做：

```text
1 个武器 Archetype
3 个稀有度
2 种攻击模式
```

例如先做：

```text
Energy Rifle
```

稀有度：

```text
Common
Rare
Legendary
```

攻击：

```text
Primary
Alternate
```

先证明系统成立。

---

# 22. GeneratedWeaponData

可以包含：

```text
seed
level
rarity
archetype
damage
fireRate
energyCost
element
primaryAbility
alternateAbility
```

服务器生成。

客户端显示。

---

# 23. Threat Level

Threat 不应该：

```text
Threat 6
=
Threat 1 的 6 倍伤害
```

Threat 更适合影响：

```text
敌人强度范围
战利品等级
稀有度概率
能力池
资源等级
POI 类型
```

这样数值不会指数膨胀。

---

# 24. N.O.V.A. 与探索结合

N.O.V.A. 不应该只在教程里出现。

以后可以负责：

```text
第一次扫描新星球
检测危险环境
发现未知信号
提示 POI
分析新矿物
记录新恒星系
播报 Hyperdrive 状态
```

这样 N.O.V.A. 会成为：

> 玩家探索宇宙时一直存在的助手。

---

# 25. 第二恒星系

现有：

```text
sys2
```

不要急着塞很多星球。

先让：

```text
Frozen
```

成为真正值得修好 Hyperdrive 才去的目标。

等 Frozen 内容完整后再增加：

```text
sys2 新卫星
sys3
```

---

# 26. 后续可以自然加入的系统

等基础循环成熟后再考虑：

```text
Tech movement
Colony
NPC settlements
Boss planets
Asteroid fields
Space stations
Random encounters
Procedural planets
Rare anomalies
Quest chains
```

---

# 27. Colony

殖民系统比较适合 Minecraft。

以后可以做：

```text
Colony Deed
```

检查房间：

```text
封闭空间
门
床
家具
灯
职业家具
```

然后生成 NPC。

家具角色尽量使用：

```text
Tags
Data Maps
```

而不是：

```text
if block == ...
```

这样其他模组也更容易兼容。

---

# 28. 推荐开发顺序

宇宙技术迁移完成后：

```text
B1 Planet Hazard
↓
B2 EPP
↓
B3 Barren Resources
↓
B4 Barren POI
↓
B5 Molten/Frozen Resources
↓
B6 Molten/Frozen POI
↓
B7 Generated Weapon Prototype
↓
B8 Hyperdrive / Frozen progression
↓
B9 新星球
```

---

# 29. 0.2 Alpha 建议目标

0.2 不需要：

```text
几十颗星球
完整殖民系统
大量武器
Boss
随机宇宙
```

0.2 真正应该证明的是：

> 第一次主动跨行星探索值得玩家去做。

最低目标：

```text
Sublight 修复完整
Barren 有独特环境
Barren 有独特资源
Barren 有 POI
Barren 有有价值 loot

Molten / Frozen 有下一阶段目标

EPP 基础成立

Generated Weapon prototype 成立

Hyperdrive 有明确 progression
```

---

# 30. 最终体验目标

玩家应该慢慢从：

```text
我能飞去另一颗星球
```

变成：

```text
我要去 Barren 找材料。

但那里环境不好。

我要先准备装备。

到了以后还要找废弃矿区。

里面可能掉新的武器。

拿到材料以后我可以升级 EPP。

升级后就能去 Molten。

之后修复 Hyperdrive。

然后去第二恒星系。
```

当玩家开始主动：

> 为下一次远征做准备，

跨行星系统才真正成为游戏玩法。