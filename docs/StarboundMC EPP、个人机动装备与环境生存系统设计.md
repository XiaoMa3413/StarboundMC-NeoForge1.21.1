# StarboundMC EPP、个人机动装备与环境生存系统设计

> 建议文件名：`docs/epp-mobility-environment-design.md`
>
> 状态：核心设计基本确认 / 数值待实机测试
>
> 优先级：P0 / 第一优先级
>
> 本文档定义 StarboundMC 的 EPP、氧气、环境防护、个人机动装备、生命维持设施，以及这些系统与星球探索、EVA、废弃飞船、小行星和未来殖民地之间的关系。
>
> 目标不是制作一套复杂的生存模拟，而是让不同宇宙环境真正改变玩家的探索准备、行动范围和风险决策。

---

# 1. 核心目标

EPP 系统应成为 StarboundMC 跨行星探索的核心基础系统之一。

它不应该只是：

```text
穿上一件装备
→ 获得“免疫环境伤害”Buff
```

而应该逐渐承担：

```text
个人生命维持
氧气储备
真空作业
EVA
环境防护
环境信息反馈
远征装备配置
```

与此同时，地表二段跳、短距离推进等移动能力不应全部塞进 EPP。

因此整个个人探索装备体系划分为：

```text
Armor
→ 战斗与基础防御

EPP
→ 生命维持、氧气、环境防护、EVA

Mobility Rig
→ 地表机动能力

Portable Supplies
→ 氧气罐等远征补给
```

核心原则：

> **EPP 决定玩家能否在恶劣环境中持续作业。**

> **Mobility Rig 决定玩家如何移动。**

> **生命维持设施决定一个空间是否能够长期供氧和居住。**

---

# 2. EPP 的定位

EPP：

**Environmental Protection Pack**

是一套背负式个人环境保护与生命维持设备。

EPP 不是传统 Armor。

它不承担主要：

```text
Armor
Damage Reduction
Combat Defense
```

职责。

其主要职责是：

```text
Life Support
Oxygen Storage
Vacuum Survival
Environmental Protection
EVA Support
Environmental Monitoring
```

---

# 3. EPP 必须拥有独立装备概念

EPP 不应占用：

```text
Chest Armor Slot
```

否则玩家需要在：

```text
战斗胸甲
和
环境生存设备
```

之间二选一。

这会造成不必要的装备冲突。

因此逻辑上应存在独立：

```text
EPP Slot
```

但实现层不能硬绑定某一种槽位 API。

---

# 4. 饰品模组兼容

很多整合包会安装：

- 饰品系统；
- 背部装备系统；
- Accessory Slot；
- Back Slot；
- 类似的额外装备槽。

EPP 本身天然适合作为：

```text
Back Equipment
```

因此 StarboundMC 应从架构上支持第三方装备槽。

不要在环境判定代码中直接：

```java
player.getItemBySlot(...)
```

假设 EPP 永远位于某个固定槽。

应建立统一查询层，例如：

```text
EppEquipmentResolver
```

其职责：

```text
Player
↓
查询可用装备来源
↓
返回唯一 Active EPP
```

概念接口：

```java
Optional<ItemStack> getActiveEpp(ServerPlayer player);
```

可能的数据来源：

```text
StarboundMC 原生 EPP Slot
第三方 Back Slot
兼容模组提供的 EPP Slot
```

环境系统只依赖：

```text
getActiveEpp(player)
```

而不关心 EPP 实际在哪个槽位。

---

# 5. Active EPP 唯一性

同一玩家任何时刻只允许：

```text
1 × Active EPP
```

即使第三方饰品系统允许多个 Back / Accessory Slot：

> 也不能叠加多个 EPP 的环境防护或氧气容量。

如果检测到多个 EPP：

第一版应采用确定性规则选取一个 Active EPP。

不要：

```text
把多个 EPP 属性相加
```

否则玩家可以通过装备多个 EPP 绕过系统。

---

# 6. EPP 不使用能源值

当前设计明确：

EPP 不引入：

```text
Battery
Energy
Power
Fuel
```

等持续消耗资源。

未来 StarboundMC 可以存在：

- 能量武器；
- 近战技能能量；
- 飞船燃料；
- 其他科技能源系统。

但这些不属于当前 EPP 系统职责。

因此：

```text
EPP Energy
```

暂不设计。

---

# 7. EPP 使用 Oxygen

与能源不同，EPP 应具有：

```text
Oxygen
```

氧气是 EPP 的核心远征资源。

原因不是追求真实模拟，而是因为 StarboundMC 已规划：

```text
Rocky Moon
EVA
废弃飞船
废弃空间站
小行星
未来其他真空 POI
```

这些区域具有大量高价值探索内容。

有限氧气可以自然形成：

```text
进入
↓
探索
↓
判断剩余氧气
↓
寻找补给
↓
选择继续深入或返回
```

因此 Oxygen 的定位是：

> **真空与无氧区域的远征预算。**

---

# 8. 哪些环境消耗 Oxygen

氧气消耗与：

```text
Heat
Cold
Radiation
```

分开处理。

例如：

```text
Frozen
有可呼吸大气
+ 极寒
```

则：

```text
Oxygen
不消耗

Cold Protection
需要
```

Molten：

```text
有大气
+ 极端高温
```

则：

```text
Oxygen
不消耗

Heat Protection
需要
```

Rocky Moon：

```text
无可呼吸大气
```

则：

```text
Oxygen
持续消耗
```

废弃飞船内部：

```text
Vacuum
```

则：

```text
Oxygen
持续消耗
```

因此：

> **氧气解决“这里有没有可呼吸空气”。**

> **环境模块解决“这里的温度、辐射等环境是否安全”。**

两者是独立系统。

---

# 9. Atmosphere 不应长期只使用 boolean

项目当前存在：

```text
breathable
```

环境属性。

第一阶段可以继续兼容。

但长期不应让：

```text
breathable = false
```

同时代表：

```text
真空
有毒大气
缺氧大气
```

这些环境未来对：

- 压力；
- EVA；
- 声音；
- 火焰；
- 气闸；
- 空间站环境；

可能存在不同规则。

因此建议未来逐步演进为：

```text
AtmosphereType
```

基础类型：

```text
BREATHABLE
UNBREATHABLE
VACUUM
```

未来可扩展：

```text
TOXIC
CORROSIVE
SPECIAL
```

第一版不要求一次实现全部。

---

# 10. Oxygen 的基本行为

EPP 拥有：

```text
oxygen
maxOxygen
```

当玩家处于需要 EPP 供氧的环境：

```text
oxygen
持续下降
```

当玩家进入：

```text
Breathable Environment
```

则：

```text
停止消耗 EPP Oxygen
```

如果环境同时属于受控生命维持区域：

```text
Pressurized Life-Support Zone
```

则：

```text
EPP Oxygen
自动恢复
```

---

# 11. 氧气 UI

正常可呼吸环境下：

> 不需要永久显示氧气条。

玩家进入：

```text
VACUUM
UNBREATHABLE
```

环境后，HUD 再出现：

```text
O₂
██████████████░░ 82%
```

低氧气时逐级增强警告。

例如：

```text
30%
LOW OXYGEN

15%
OXYGEN CRITICAL

5%
N.O.V.A. 强警告
```

具体阈值属于调参项。

---

# 12. 氧气耗尽

氧气归零以后，不应瞬间死亡。

流程建议：

```text
Oxygen = 0
↓
进入 Suffocation Exposure
↓
短 Grace Period
↓
明显 HUD / 音效警告
↓
逐渐产生窒息效果
↓
持续伤害
↓
最终死亡
```

目标：

> 给玩家留下最后的返航或使用氧气罐机会。

不要设计成：

```text
O₂ = 0
→ 立即死亡
```

---

# 13. Oxygen 不应过度频繁打断玩家

氧气的目的：

> 限制远征深度。

而不是：

> 每隔几分钟强迫玩家回家。

因此基础 EPP 的氧气容量应该允许一次具有实际意义的探索活动。

初始测试可以从：

```text
约 10～15 分钟正常真空作业时间
```

这一量级开始测试。

这不是最终数值。

最终数值必须通过：

```text
Rocky Moon
EVA
废弃飞船
小行星
```

实际游戏测试确定。

---

# 14. EPP 世代升级

EPP 本体本身必须具有明确成长感。

不能设计成：

```text
一个廉价 EPP
+
后面永远只是换模块
```

否则玩家升级后外观与核心体验变化不足。

因此 EPP 建议拥有明确 Chassis Generation。

初始规划：

```text
EPP Mk.I
EPP Mk.II
EPP Mk.III
EPP Mk.IV
```

具体名称未来可调整。

---

# 15. EPP Mk.I — Life Support Pack

定位：

> 玩家第一次进入无氧环境的基础个人生命维持装置。

核心能力：

```text
✓ Oxygen Storage
✓ Life Support
✓ 无氧环境生存

✗ EVA Thruster
✗ Optional Module
✗ Advanced Environment HUD
```

这是玩家的第一台 EPP。

制作成本必须较低。

它不应该要求大量稀有材料。

真正的进度门槛应来自：

> 获得关键 Life Support 技术组件。

例如：

```text
Life Support Core
```

可以作为 Barren POI 的保底关键奖励。

概念：

```text
Life Support Core
+
基础金属
+
普通电子零件
→
EPP Mk.I
```

---

# 16. Mk.I 的玩法意义

获得 Mk.I 后：

```text
玩家第一次可以安全前往 Rocky Moon
```

Rocky Moon 应成为 EPP 的第一堂教学。

环境尽量简单：

```text
No Breathable Atmosphere
+
较低其他环境压力
```

玩家第一次明显体验：

```text
落地
↓
O₂ HUD 出现
↓
开始消耗氧气
↓
探索
↓
返回安全区域补氧
```

不要第一次使用 EPP 就同时处理：

```text
缺氧
极寒
高辐射
复杂 EVA
```

---

# 17. EPP Mk.II — EVA Pack

Mk.II 应当带来明显的新玩法，而不是单纯增加数值。

核心能力：

```text
✓ Mk.I 全部能力
✓ 更高 Oxygen Capacity
✓ Vacuum Operation
✓ EVA Thruster
✓ 1 × Optional Module Slot
```

最大的升级：

> **玩家正式获得 EVA 能力。**

---

# 18. EVA Thruster

EVA Thruster 是：

```text
EPP Built-in System
```

而不是 Optional Module。

原因：

> EVA 是高级 EPP 本身的核心能力。

玩家不应该为了安装其他环境模块：

```text
把 EVA Thruster 拆掉
```

EVA Thruster 主要工作于：

```text
Zero Gravity
Microgravity
Vacuum Local Space
```

实现：

```text
WASD
→ 水平 / 前后推进

Space
→ 上推

Shift
→ 下推
```

采用：

```text
轻惯性
+
自动阻尼
+
速度上限
```

而不是完全真实的牛顿运动。

---

# 19. EPP Mk.III — Expedition Pack

定位：

> 真正用于跨星球专业远征的主力 EPP。

核心能力：

```text
✓ Life Support
✓ 更高 Oxygen Capacity
✓ EVA Thruster
✓ Advanced Environment Monitoring
✓ 2 × Optional Module Slot
```

此阶段玩家开始真正根据目的地：

> 配置自己的环境装备。

---

# 20. EPP Mk.IV — Deep-Space Pack

定位：

> 高级深空与极端环境探索平台。

核心能力可包括：

```text
✓ Mk.III 全部基础能力
✓ 更高效 Oxygen System
✓ 更稳定 EVA Control
✓ Advanced Beacon
✓ 高级环境传感
✓ 高级模块支持
```

建议仍然保持：

```text
2 × Optional Module Slot
```

不要简单扩展成：

```text
4
6
8
```

个模块槽。

Mk.IV 的价值应该来自：

> 本体系统升级。

而不是：

> 把所有模块全部装进去。

---

# 21. EPP 升级原则

EPP 新世代应由旧设备升级而来。

例如：

```text
EPP Mk.I
+
新科技组件
+
新材料
→
EPP Mk.II
```

而不是：

```text
直接重新制作 Mk.II
然后 Mk.I 扔箱子
```

升级应尽可能保留：

```text
Custom Name
兼容模块
Cosmetic State
```

如果旧模块无法用于新设备：

> 自动卸下并返还。

绝不能静默吞掉玩家模块。

---

# 22. EPP 外观必须随世代变化

不同 EPP 不应只是同一个模型改 Tooltip。

推荐：

```text
Mk.I
笨重
工业
外露结构
临时生命维持设备感

Mk.II
加入明显 EVA 推进器
结构更加完整

Mk.III
模块化远征装备
更紧凑
出现明显模块接口

Mk.IV
高度集成
高级传感器
成熟宇航装备
```

玩家在第三人称下应该能够：

> 一眼大致判断 EPP 世代。

---

# 23. Optional Module

EPP Module 用于：

```text
环境专业化
探索专业化
```

不是 EPP 的全部成长内容。

模块建议分为：

```text
Environmental Module
Utility Module
```

但槽位本身可以保持通用。

例如 Mk.III：

```text
Slot A
Slot B
```

两个槽都可以安装合法 EPP Module。

---

# 24. Environmental Module

第一批建议：

```text
Heating Module
Cooling Module
Radiation Shield Module
```

---

# 25. Heating Module

用于：

```text
Cold Protection
```

对应当前：

```text
coldTier
```

可存在：

```text
Heating I
Heating II
Heating III
```

概念：

```text
Cold Protection 1
Cold Protection 2
Cold Protection 3
```

---

# 26. Cooling Module

用于：

```text
Heat Protection
```

对应：

```text
heatTier
```

可存在：

```text
Cooling I
Cooling II
Cooling III
```

---

# 27. Radiation Shield Module

用于：

```text
Radiation Protection
```

对应：

```text
radiationTier
```

可存在：

```text
Radiation I
Radiation II
Radiation III
```

Radiation 后续可以拥有区别于温度的：

```text
Radiation Dose
```

机制。

第一版不必一次全部实现。

---

# 28. 模块等级与 Chassis 等级

推荐：

```text
Mk.I
无模块

Mk.II
支持 Tier I Module

Mk.III
支持 Tier II Module

Mk.IV
支持 Tier III Module
```

因此：

```text
EPP Chassis
```

决定：

> 能安装多高级的模块。

而：

```text
Installed Modules
```

决定：

> 实际拥有何种防护。

高级 EPP 不自动获得：

```text
Cold III
Heat III
Radiation III
```

---

# 29. 高级 EPP 不应成为万能环境免疫

即使 Mk.IV：

```text
没有安装 Cooling
```

也不应该自动完全抵抗 Molten。

玩家仍然需要根据远征目的选择配置。

核心原则：

> **本体提供能力平台，模块提供环境专业化。**

---

# 30. Utility Module

未来可考虑：

```text
Survey Scanner
Salvage Scanner
Beacon Amplifier
Environmental Sensor
其他探索型设备
```

Utility Module 应尽量：

> 改变探索方式。

而不是只提供：

```text
+3% speed
+5% mining
```

这种存在感很弱的被动数值。

---

# 31. 模块不要野外无限热插拔

为了保留：

```text
远征准备
```

模块不建议允许玩家：

```text
随时打开背包
→ 换模块
```

推荐使用：

```text
EPP Service Station
```

或未来：

```text
Equipment Station
```

完成安装与拆卸。

可出现于：

```text
飞船
殖民地
正常空间站
其他高级安全设施
```

这样玩家出发前会：

```text
查看目标环境
↓
配置 EPP
↓
准备补给
↓
出发
```

---

# 32. Portable Oxygen Canister

加入：

```text
Emergency Oxygen Canister
```

定位：

> 真空远征中的临时氧气补给。

它不是：

```text
EPP Module
```

也不是：

```text
永久扩大 EPP 容量的装备
```

而是一件：

```text
Portable Supply
```

---

# 33. Oxygen Canister 使用方式

玩家：

```text
手持 Oxygen Canister
↓
右键 / 长按使用
↓
短暂补氧过程
↓
EPP Oxygen 增加
↓
Full Canister
变为
Empty Canister
```

玩家未装备有效 EPP：

```text
不能正常使用
```

EPP 已满：

```text
不能浪费整罐氧气
```

或者只允许在能够完整接收时使用。

第一版优先选择行为清晰的规则。

---

# 34. Oxygen Canister 不应瞬间使用

不建议：

```text
右键一下
→ 瞬间 +10 分钟
```

推荐：

```text
1～2 秒使用动作
+
充气音效
+
HUD 响应
```

未来可以加入简单第三人称动作。

使用过程中可以被：

```text
受伤
特殊移动
其他危险行为
```

打断。

防止氧气罐成为战斗中的瞬发药水。

---

# 35. Oxygen Canister 容量

设计层不要直接储存：

```text
+10 minutes
```

应该储存：

```text
oxygenUnits
```

游戏 UI 可以根据当前 EPP 消耗速率：

> 换算预计可用时间。

这样未来：

- EPP 效率；
- 环境；
- 难度；
- Buff；

发生变化时，不会把系统写死。

---

# 36. Full Oxygen Canister 不可堆叠

正式建议：

```text
Full Oxygen Canister
maxStackSize = 1
```

原因：

玩家可以通过携带更多氧气罐：

> 主动延长远征。

但必须付出：

```text
背包空间
```

成本。

这是一种自然的准备选择。

玩家带：

```text
更多氧气
```

意味着：

```text
更少战利品空间
```

---

# 37. 不强制限制玩家只能带几罐

第一版不需要：

```text
Maximum Canisters = 2
```

这种人为硬限制。

如果玩家愿意：

```text
占用大量背包格
```

携带很多氧气：

> 可以允许。

这本身就是成本。

如果实际测试证明能被严重滥用，再考虑进一步限制。

---

# 38. Empty Oxygen Canister

使用后留下：

```text
Empty Oxygen Canister
```

不要让罐子直接消失。

这样氧气罐属于：

> 可重复使用的远征装备。

Empty Canister 可以考虑：

```text
允许少量堆叠
```

例如未来测试：

```text
4
8
```

减少空罐占用过多背包空间。

具体值待测试。

---

# 39. Oxygen Canister 重新灌装

完整循环：

```text
Empty Canister
↓
Life Support Infrastructure
↓
Refill
↓
Full Oxygen Canister
```

第一版可以由：

```text
飞船 Life Support System
```

承担。

以后也可支持：

```text
殖民地 Life Support
正常空间站
修复后的废弃设施
专业补给站
```

---

# 40. 氧气罐作为探索战利品

Oxygen Canister 不只是玩家自己制作的补给。

它还应该成为：

```text
真空 POI 战利品池
```

的一部分。

例如：

```text
废弃飞船
废弃空间站
旧采矿设施
小行星采矿站
Emergency Locker
```

可能发现：

```text
Full Oxygen Canister
```

---

# 41. Oxygen Loot 的核心作用

它不是单纯：

> 奖励玩家一个消耗品。

而是：

> **给予玩家选择性继续深入探索的机会。**

典型流程：

```text
进入废弃飞船
O₂ 100%
↓
探索
O₂ 60%
↓
发现 Emergency Supply Locker
↓
获得 Oxygen Canister
↓
玩家选择：
撤退
or
使用氧气继续深入
```

---

# 42. 氧气战利品不能完全随机控制主线

关键 POI 不应出现：

```text
运气不好
→ 没刷氧气
→ 无法合理完成
```

因此区分：

```text
Guaranteed / Authored Oxygen Supply
```

和：

```text
Random Bonus Oxygen
```

第一次主线废弃飞船等关键 POI：

> 可以设计固定 Emergency Supply Locker。

额外支路：

> 可以随机生成氧气补给。

---

# 43. 氧气不应在每个箱子出现

如果：

```text
每个箱子
都有氧气
```

则玩家会默认：

> 前面肯定还有。

氧气机制会失去压力。

因此补给必须：

```text
有限
稀疏
有设计目的
```

---

# 44. 废弃飞船氧气节奏

推荐：

```text
Entry
↓
低风险区域
↓
Mid Section
↓
可能发现氧气补给
↓
玩家做风险判断
↓
Deep Section
↓
高价值战利品
```

氧气因此承担：

> 控制 Dungeon 深度。

而不是：

> 单纯死亡计时器。

---

# 45. Life Support Infrastructure

EPP 与飞船生命维持系统不能互相替代。

两者职责：

```text
EPP
= Personal Life Support

Life Support Infrastructure
= Environmental Life Support
```

EPP：

```text
保护一个玩家
```

Life Support：

```text
改变整个空间环境
```

---

# 46. 飞船 Life Support 的职责

未来飞船生命维持系统可以：

```text
为密闭舱室供氧
维持 Pressurized Zone
给 EPP 补氧
给 Empty Oxygen Canister 灌装
支持船员正常生活
```

因此即使玩家拥有 EPP：

> 飞船 Life Support 仍然具有明确价值。

---

# 47. Colony Life Support

未来殖民地：

```text
Sealed Structure
+
Life Support
=
Habitable Zone
```

可以承担：

```text
玩家补氧
EPP 补给
Oxygen Canister 灌装
NPC 居住
农业
医疗
长期生活设施
```

玩家穿 EPP：

> 可以在真空地区建设基地。

但只有 Life Support 建立以后：

> 那里才真正成为殖民地。

---

# 48. 修复废弃设施的生命维持

废弃飞船 / 空间站可以拥有：

```text
Emergency Life Support
```

初始：

```text
VACUUM
```

玩家有限氧气探索。

找到并修复设备后：

```text
部分区域
VACUUM
→
PRESSURIZED
```

效果：

```text
停止消耗 Oxygen
自动补充 EPP
形成 Safe Zone
```

玩家可以将这里作为：

> 继续深入探索的前进基地。

---

# 49. Environment Hazard

当前核心环境维度：

```text
Atmosphere / Oxygen
Cold
Heat
Radiation
Gravity
```

其中 Gravity 暂时可以只作为环境数据。

第一阶段不要求立即实现完整动态重力。

---

# 50. Environment Query 应统一

EPP 不应该直接判断：

```text
当前 dimension == Rocky Moon
```

也不应该硬编码：

```text
if Frozen
```

应存在统一环境查询。

例如：

```text
PlayerEnvironmentService
```

或等价服务。

职责：

```text
Player
↓
当前世界位置
↓
解析环境来源
↓
EnvironmentState
```

环境来源可以包括：

```text
PlanetEnvironmentProfile
Ship Pressure Zone
Space Vacuum
Station Pressure Zone
Special Area
```

---

# 51. EnvironmentState

概念数据：

```text
atmosphereType
coldTier
heatTier
radiationTier
gravityScale
pressurized
```

EPP 和 Hazard 系统只读取：

```text
EnvironmentState
```

而不关心它来自：

```text
星球
飞船
空间站
小行星
```

---

# 52. 与现有 PlanetEnvironmentProfile 的关系

当前项目已经具有类似：

```text
breathable
coldTier
heatTier
radiationTier
gravityScale
```

的数据结构。

不应推翻。

第一阶段可以直接复用。

未来 AtmosphereType 加入以后：

> 再逐步替换或兼容 `breathable`。

目标是增量演进。

---

# 53. Environmental Protection 判定

玩家最终防护属性可以概念化为：

```text
EppProtection

oxygenSupport
coldTier
heatTier
radiationTier
```

例如：

```text
Environment:
coldTier = 2

Player:
coldProtection = 1
```

则：

```text
Protection Deficit = 1
```

进入 Cold Exposure。

---

# 54. Exposure

环境防护不足时：

> 不应立刻持续扣血。

使用：

```text
Exposure
```

逐渐累积。

例如：

```text
0%
↓
Warning
↓
Minor Effect
↓
Serious Effect
↓
Damage
```

离开危险环境：

```text
Exposure
逐渐恢复
```

---

# 55. 完整防护时不应强制积累对应 Exposure

如果：

```text
Cold Tier 2
```

而玩家拥有：

```text
Cold Protection 2
```

则：

```text
Cold Exposure
不应继续增长
```

否则模块只能：

> 延迟死亡。

而不是：

> 真正解决环境问题。

---

# 56. 不同 Hazard 不应只是不同颜色的伤害

长期目标：

```text
Oxygen
→ Suffocation

Cold
→ Cold Exposure

Heat
→ Heat Exposure

Radiation
→ Radiation Dose
```

它们可以具有不同：

```text
HUD
音效
症状
恢复方式
```

第一版可以先建立通用框架，再逐步差异化。

---

# 57. EPP 不应该成为硬门票

环境不足时：

```text
N.O.V.A.
+
UI
```

应明确警告。

但普通情况下不要：

```text
禁止传送
```

例如玩家坚持裸装去 Molten：

> 可以。

但会快速遭受 Heat Exposure。

核心原则：

> **环境装备改变生存条件，而不是直接成为菜单锁。**

---

# 58. Mobility Rig

地表移动能力不归 EPP 管理。

新增独立概念：

```text
Mobility Rig
```

中文可称：

```text
个人机动装置
```

它是一件真实存在的可装备设备。

不是抽象：

```text
Personal Tech
```

---

# 59. 为什么不采用抽象 Personal Tech

StarboundMC 当前世界观中的核心能力基本都有：

```text
明确设备
明确机械来源
明确升级对象
```

例如：

```text
Ship Engine
EPP
Matter Manipulator
Navigation System
```

如果玩家突然：

```text
解锁 Double Jump Tech
```

但角色身上没有任何设备：

> 会显得过于抽象和游戏化。

因此移动能力应拥有：

> 实际硬件载体。

---

# 60. Mobility Rig 的装备方式

Mobility Rig 应拥有独立逻辑装备位：

```text
Mobility Slot
```

同样可以通过：

```text
Equipment Resolver
```

兼容饰品模组。

不要强制占用：

```text
Boots Armor Slot
```

否则玩家会在：

```text
高级战斗靴
和
移动能力
```

之间产生不必要冲突。

---

# 61. Mobility Rig 视觉表现

虽然逻辑上不是 Boots：

> 外观完全可以像火箭靴、腿部推进器或腰部推进装置。

例如：

```text
Jump Thruster
```

模型上可能体现为：

```text
脚踝喷口
腿侧推进器
腰部小型喷嘴
```

从而保证：

> 能力具有明确机械来源。

---

# 62. 第一种 Mobility Rig：Jump Thruster

第一批最推荐：

```text
Jump Thruster
```

功能类似：

> 二段跳 / 空中短脉冲推进。

玩家：

```text
正常 Jump
↓
处于空中
↓
再次按 Jump
↓
Jump Thruster 点火
↓
向上并略向移动方向推进
```

---

# 63. Jump Thruster 不使用传统 Cooldown

更自然的限制：

```text
每次离地
1 次 Air Boost Charge
```

使用：

```text
charge = 0
```

重新稳定落地：

```text
charge = 1
```

避免：

```text
玩家站在地上等待 5 秒 CD
```

这种不自然行为。

---

# 64. Jump Thruster 与 EVA Thruster 必须分开

虽然二者视觉上都可能喷气：

```text
EVA Thruster
```

属于：

```text
EPP
```

用途：

```text
零重力持续推进
```

而：

```text
Jump Thruster
```

属于：

```text
Mobility Rig
```

用途：

```text
有重力环境中的离散机动能力
```

代码上也应分离。

不要做成一个推进系统加不同参数。

---

# 65. 未来 Mobility Rig

未来可以考虑：

```text
Jump Thruster
Dash Thruster
Hover Unit
Mobility Stabilizer
其他实体机动设备
```

但不要第一版同时实现大量能力。

先验证：

```text
Jump Thruster
```

是否真正改善星球探索。

---

# 66. Mobility Rig 与世界探索

获得 Jump Thruster 后：

> 世界设计可以开始利用新的垂直和水平移动能力。

例如：

```text
断裂平台
高层入口
坍塌建筑
峡谷
竖井
岩壁缺口
空间站外壁
```

不要大量制作：

```text
Requires Jump Thruster
```

硬门。

更理想：

> 它提供新路线、捷径和探索方式。

---

# 67. Gravity 与 Mobility

项目已经存在：

```text
gravityScale
```

未来真正启用不同重力后：

```text
Jump Thruster
```

和：

```text
EVA Thruster
```

需要读取环境重力。

但不要追求严格现实物理。

目标：

> 不同星球有不同运动手感，但都保持可控。

例如低重力 Moon：

```text
Jump Thruster
```

应避免把玩家直接送到非常夸张的高度。

---

# 68. N.O.V.A. 与 EPP

N.O.V.A. 应成为 EPP / 环境系统的重要反馈渠道。

例如出发前：

```text
目标大气不可呼吸。
建议确认 EPP 氧气储量。
```

Frozen：

```text
检测到低温环境。
当前热控模块防护等级不足。
```

EVA：

```text
EPP 推进系统已就绪。
```

氧气不足：

```text
警告：EPP 氧气储量低于安全返航建议值。
```

具体文案另行设计。

---

# 69. EPP HUD

HUD 应保持：

> 情境式显示。

正常情况下不要永久霸屏。

安全宜居环境：

```text
不显示
```

进入无氧环境：

```text
显示 O₂
```

进入 Cold Hazard：

```text
显示 Cold Exposure
```

进入 Radiation：

```text
显示 Radiation
```

EVA：

```text
可额外显示 Ship Beacon
Encounter Beacon
```

---

# 70. EPP 与 Local Space / EVA

在 Space Encounter 中：

```text
Ship Interior
→ Pressurized
→ Oxygen Refill

Airlock
→ Transition

Outside Ship
→ Vacuum
→ Oxygen Consumption
→ EVA Thruster Enabled

Station
→ 根据 Pressure Zone 决定
```

这样 EPP 系统与：

```text
space-encounter-eva-design.md
```

天然对接。

---

# 71. 玩家返回飞船

玩家 EVA 返回：

```text
进入有效 Pressurized Ship Zone
↓
Oxygen consumption stops
↓
EPP oxygen rapidly refills
↓
EVA mode disabled
```

不要求玩家：

```text
脱掉 EPP
```

EPP 可以始终装备。

进入正常环境后：

```text
Life Support
自动待机
```

---

# 72. Life Support 与 EPP 的最终关系

核心公式：

```text
EPP
让恶劣环境
可探索

Life Support
让恶劣环境
可长期居住
```

以及：

```text
EPP Oxygen
= Personal Expedition Resource

Oxygen Canister
= Portable Expedition Extension

Life Support Infrastructure
= Stable Oxygen Source
```

---

# 73. 第一阶段不要实现的内容

P0 阶段暂时不要加入：

```text
EPP Battery
CO₂
湿度
过滤器耐久
氧气纯度
舱内复杂气体混合
真实压力数值
真实气体流体模拟
多种氧气瓶尺寸
十几种环境模块
复杂减压物理
Mobility Rig 技能树
EPP 随机词条
```

第一阶段必须保持系统清晰。

---

# 74. 推荐 P0 实施顺序

## P0-1 — Environment Query

建立统一玩家环境查询层。

输出：

```text
Atmosphere / Breathability
Cold Tier
Heat Tier
Radiation Tier
Gravity Scale
Pressurized State
```

---

## P0-2 — EPP Equipment Resolver

建立：

```text
EppEquipmentResolver
```

支持：

```text
原生槽位
兼容第三方槽位的扩展接口
```

只返回一个 Active EPP。

---

## P0-3 — EPP Mk.I

实现：

```text
EPP Mk.I
Oxygen Capacity
Life Support
```

暂时：

```text
无模块
无 EVA
```

---

## P0-4 — Oxygen Runtime

实现：

```text
当前氧气
最大氧气
消耗
补充
缺氧
服务端权威
同步 HUD
```

---

## P0-5 — Rocky Moon 闭环

验证：

```text
无 EPP
→ 无法长期生存

Mk.I
→ 氧气开始消耗

返回安全区
→ 自动补氧
```

第一阶段最重要验收项。

---

## P0-6 — Emergency Oxygen Canister

实现：

```text
Full Canister
Empty Canister
不可堆叠 Full
右键补氧
使用动作
Life Support Refill
```

---

## P0-7 — Oxygen Loot

在测试 POI 中加入：

```text
Emergency Supply Locker
```

验证：

```text
玩家发现氧气
→ 决定是否继续探索
```

---

## P0-8 — EPP Module Framework

实现：

```text
EppModule
Module Slot
Protection Calculation
```

先不需要完整 Heating/Cooling/Radiation 内容。

---

## P0-9 — First Environmental Module

优先实现一个环境模块。

建议：

```text
Heating
或
Cooling
```

用于验证：

```text
Environment Tier
vs
Protection Tier
```

---

## P0-10 — Exposure Framework

实现：

```text
Exposure
Grace Period
Warning
Damage
Recovery
```

先支持一个 Hazard。

再扩展其他类型。

---

# 75. 推荐 P1 实施顺序

P0 稳定后：

```text
EPP Mk.II
↓
EVA Thruster
↓
Space Vacuum
↓
Ship Beacon
↓
废弃空间站 / 废船 Oxygen Gameplay
↓
Heating / Cooling / Radiation
↓
EPP Mk.III
↓
双模块配置
```

---

# 76. Mobility Rig 实施阶段

Mobility Rig 不必阻塞 EPP P0。

建议在：

```text
基础 EPP
+
Oxygen
+
环境 Hazard
```

稳定后实现。

第一阶段只做：

```text
Mobility Slot
Jump Thruster
1 × Air Boost
Landing Recharge
基础喷射特效
```

不要同时制作 Dash / Hover。

---

# 77. 服务端权威

以下状态必须由服务端决定：

```text
Active EPP
Oxygen Amount
Oxygen Consumption
Oxygen Refill
Installed Modules
Environment Protection
Exposure
Canister Use
Mobility Equipment
Jump Boost Availability
```

客户端负责：

```text
HUD
动画
声音
粒子
预测性表现
```

不要让客户端决定：

```text
是否免疫环境
是否获得氧气
```

---

# 78. Data Component 建议

EPP 适合使用项目现有 Data Component 风格。

概念上可以拆为：

```text
EppData
```

例如：

```text
generation
oxygen
installedModules
```

以及静态 Item Definition 提供：

```text
maxOxygen
moduleSlots
maxModuleTier
builtInCapabilities
```

具体实现不要为了数据驱动而过度复杂。

---

# 79. EppProtection

环境查询最终可生成一个汇总：

```text
EppProtection
```

概念：

```text
hasLifeSupport
coldTier
heatTier
radiationTier
hasEvaThruster
```

它由：

```text
EPP Chassis
+
Installed Modules
```

计算。

不要把最终防护重复持久化到多个地方。

---

# 80. Oxygen 数据不要按“秒”存储

内部使用：

```text
Oxygen Units
```

不要：

```text
remainingSeconds
```

消耗速率由系统决定。

UI 可以换算：

```text
Estimated Remaining Time
```

这样以后便于平衡调整。

---

# 81. 可配置性

以下内容应尽量集中配置，而不是散落魔法数字：

```text
EPP oxygen capacity
oxygen consumption rate
oxygen refill rate
low oxygen thresholds
canister oxygen amount
exposure accumulation
exposure recovery
module protection tier
EVA max speed
EVA damping
Jump Thruster strength
```

---

# 82. 测试要求

至少应覆盖：

```text
无 EPP + Vacuum
EPP + Vacuum
EPP Oxygen depletion
进入 breathable 区域停止耗氧
进入 Life Support Zone 补氧
Canister refill
Canister cannot exceed capacity
Multiple EPP does not stack
Module protection calculation
Protection Tier equality
Protection deficit
Exposure recovery
```

以后 EVA 增加：

```text
EVA enable conditions
EVA disable conditions
Warp with EVA players denied
```

Mobility Rig 增加：

```text
Air Boost once per airtime
Landing resets charge
Equipment removal disables boost
```

---

# 83. 第一版完整体验目标

第一版系统真正需要证明的是：

```text
玩家修复 Sublight
↓
前往 Barren
↓
获得 Life Support 关键组件
↓
制作 EPP Mk.I
↓
前往 Rocky Moon
↓
EPP Oxygen 开始消耗
↓
探索 / 采集高价值资源
↓
发现或携带 Oxygen Canister
↓
决定是否继续深入
↓
返回安全区域
↓
EPP 自动补氧
```

如果这个闭环好玩：

> 再加入更复杂环境系统。

---

# 84. 后续 EVA 体验目标

之后升级：

```text
EPP Mk.II
↓
EVA Thruster
↓
飞船抵达 Space Encounter
↓
气闸打开
↓
进入 Vacuum
↓
Oxygen 消耗
↓
EVA 飞向废弃空间站
↓
找到氧气补给
↓
选择继续探索
↓
恢复空间站部分 Life Support
↓
建立新的安全补给点
↓
继续深入
↓
返回自己的飞船
```

这应成为 StarboundMC 最具辨识度的体验之一。

---

# 85. 核心设计约束

实现过程中必须遵守：

1. EPP 不占普通 Chest Armor Slot。
2. EPP 装备查询必须经过统一 Resolver。
3. 为饰品模组的 Back / Accessory Slot 预留兼容空间。
4. 多个 EPP 不允许叠加。
5. EPP 不使用 Energy / Battery。
6. EPP 使用有限 Oxygen。
7. Oxygen 只在无可呼吸空气的环境持续消耗。
8. Frozen / Molten 等拥有可呼吸大气的环境不因冷热自动消耗 Oxygen。
9. Heat / Cold / Radiation 与 Oxygen 是独立 Hazard。
10. EPP Mk.I 制作成本应较低。
11. EPP 本体升级必须带来明显新能力和新外观。
12. EVA Thruster 属于高级 EPP 内置系统。
13. 地表二段跳不属于 EPP。
14. 地表高级移动能力由 Mobility Rig 提供。
15. Full Oxygen Canister 不可堆叠。
16. Oxygen Canister 是临时补给，不是永久 EPP 扩容模块。
17. Oxygen Canister 可以作为 POI 战利品。
18. 关键主线 POI 的必要氧气补给不能完全依赖 RNG。
19. Life Support Infrastructure 必须能够补充 EPP 和氧气罐。
20. 模块不能无限增加，避免最终 EPP 成为万能装备。
21. 环境危险优先使用警告与 Exposure，不做突然死亡。
22. 普通危险环境不应使用硬性传送门槛。
23. 第一版优先游戏性，不做复杂真实气体模拟。

---

# 86. 最终系统关系

```text
                  PLAYER
                     │
        ┌────────────┼────────────┐
        │            │            │
      Armor         EPP      Mobility Rig
        │            │            │
      战斗         环境生存       地表机动
                     │
          ┌──────────┼───────────┐
          │          │           │
       Oxygen     Modules       EVA
          │          │           │
      真空时间    环境防护     太空移动
          │
   Oxygen Canister
          │
      临时延程
          │
   Life Support Infrastructure
          │
 ┌────────┼─────────┐
 │        │         │
飞船     殖民地    空间站
 │        │         │
供氧     宜居化    安全区
补EPP    NPC生活    远征中继
灌氧罐   补给      补给
```

---

# 87. 最终设计理念

EPP 系统不应该让玩家感觉：

> “我又多了一件必须一直穿的装备。”

它应该让玩家逐渐感受到：

```text
最初
我只能在正常星球活动

↓

Mk.I
我可以进入没有空气的世界

↓

氧气补给
我可以计划更深的真空远征

↓

Mk.II
我第一次真正离开飞船 EVA

↓

Modules
我开始为不同星球准备不同环境装备

↓

Advanced EPP
我能够执行真正的深空探索任务
```

与此同时：

```text
Mobility Rig
```

让玩家获得：

> 不依赖抽象“个人科技”的真实机械移动能力。

而：

```text
Life Support Infrastructure
```

让飞船、空间站和殖民地真正成为：

> 玩家在危险宇宙中建立的安全立足点。

最终希望形成的感觉不是：

> “这里有一个 Oxygen Debuff。”

而是：

> **“我要准备好装备和补给，然后离开安全区，去一个真正危险但值得探索的地方。”**