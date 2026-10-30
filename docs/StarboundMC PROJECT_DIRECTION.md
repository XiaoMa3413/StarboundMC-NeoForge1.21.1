# StarboundMC 项目方向

> 本文档用于记录 StarboundMC 已基本确认的项目定位、设计原则和长期方向。  
> 它不是具体功能实现计划，也不代表所有构想都已经进入开发阶段。  
> 具体功能落地时，应另行建立实现计划、任务或 PR。

---

# 1. 项目定位

StarboundMC 是一个以 **飞船、宇宙导航、跨世界探索和科技进度** 为核心的 Minecraft 模组。

项目不追求完整复刻《Starbound》，而是吸收其核心体验，并将其重新设计为适合 Minecraft 的玩法系统。

核心体验：

> 接任务 / 做准备  
> → 驾驶飞船前往目标  
> → 面对环境或战斗限制  
> → 探索星球与 POI  
> → 获取独特资源、装备或科技  
> → 返回飞船加工与升级  
> → 解锁更危险、更遥远的目标

飞船应逐渐成为玩家真正的“家”和整个游戏流程的中心。

---

# 2. StarboundMC 不是纯内容模组，也不是纯整合包 Glue Mod

StarboundMC 应同时具备：

1. **独立可玩的原创内容**
2. **可接入其他模组内容的宇宙探索框架**

StarboundMC 本体必须拥有足够完整的原创体验，不能依赖其他模组才能成立。

同时，宇宙系统应允许整合包或兼容模块将其他模组的世界接入 StarboundMC。

例如：

- Twilight Forest 可以被注册为一个特殊生态星球
- Aether 可以作为特殊大气世界
- 其他模组维度可以成为异常空间、遗迹世界或特殊星球

但这些外部内容不应成为 StarboundMC 核心代码的硬依赖。

原则：

> StarboundMC 负责“宇宙规则和探索结构”，其他模组负责提供可选的世界内容。

StarboundMC 是导演，其他模组是可选演员。

---

# 3. 原创内容与兼容内容的关系

StarboundMC 应至少拥有一组具有自身辨识度的核心星球。

当前主要天体方向：

## Lush

定位：

- 起始世界
- 生存与基础资源
- 相对安全
- 教会玩家基础探索与飞船系统

## Barren

定位：

- 沙漠星球
- 废弃设施
- 旧文明或工业遗迹
- Salvage / scavenging 导向

核心幻想：

> 在一个已经衰败的世界里寻找别人留下来的东西。

MVP 可包含：

- 沙漠地形
- 少量固定 POI
- Scrap / Circuit 等特殊材料
- 与生命支持或科技进度相关的关键物品

暂时不强求复杂沙尘暴、动态事件或大量定制敌人。

## Rocky Moon

定位：

- 无氧环境
- 采矿
- Fuel Crystal
- 飞船燃料与 EPP 进度

环境危险应比怪物本身更重要。

## Frozen

定位：

- 极寒环境
- 科研 / 高科技资源
- 高级装备或冷却科技的前置

## Molten

定位：

- 极端高温
- 高危险
- 高价值资源
- 允许玩家早期看到，但需要后期装备才能真正探索

可以形成“先发现，之后再回来征服”的体验。

## Gas Giant

定位：

- 不提供常规地表登陆
- 轨道探索
- 空间 POI
- 舰船、空间站、异常信号等内容

---

# 4. 环境与装备进度

StarboundMC 的星球差异不应只是：

> 不同颜色地形 + 不同矿物。

环境限制应该成为探索准备的一部分。

当前方向：

- Lush：基本安全
- Barren：轻度环境压力
- Rocky Moon：无氧
- Frozen：极寒
- Molten：极热
- Gas Giant：不可正常登陆

EPP / 环境装备应承担重要进度作用。

大致方向：

- 基础 Life Support / EPP：解决无氧
- Heating 类升级：应对 Frozen
- Cooling 类升级：应对 Molten

环境危险原则：

> 环境应迫使玩家准备，而不是单纯通过高伤害惩罚玩家。

可以考虑：

- Grace Period
- HUD 警告
- N.O.V.A. 环境提示
- 逐渐累积的危险状态

避免玩家一落地就莫名暴毙。

---

# 5. 星球内容设计原则

每颗主要星球应拥有明确身份。

优先实现：

- 环境机制
- POI
- 独特资源
- 探索奖励
- 少量特色敌人

而不是先追求：

- 巨量生物群系
- 数百种方块
- 大量随机生成星球
- 超复杂生态系统

原则：

> 10 个玩家能记住的小型故事地点，比 100 个没有意义的随机地形变化更重要。

POI / Microdungeon 是 StarboundMC 探索体验的重要内容单位。

---

# 6. 资源设计原则

不同星球的资源应产生不同用途。

不应只是：

- 铜矿
- 蓝铜矿
- 红铜矿
- 高级铜矿

资源用途可以包括：

- 飞船维修
- 电子零件
- 能源设备
- EPP
- 武器
- 扫描科技
- Matter Manipulator 升级
- N.O.V.A. 解锁
- 特殊设备制造

关键进度材料不应完全依赖随机宝箱。

如果某个资源是主线必须品，应提供可重复、可预测或保底的获得路径。

---

# 7. 飞船

飞船是整个 StarboundMC 的核心空间。

它不只是传送菜单。

飞船应逐渐承担：

- 导航
- 制造
- 装备准备
- 资源加工
- N.O.V.A.
- 主线任务
- 星际旅行
- 玩家基地

飞船升级应产生真实功能变化，而不是单纯数值升级。

例如：

- Sublight Engine
- Hyperdrive
- Life Support
- Scanner
- Fuel System
- Navigation

---

# 8. N.O.V.A.

N.O.V.A.：

**Navigation, Operations & Vessel Assistant**

N.O.V.A. 是飞船 AI，也是 StarboundMC 世界和玩法系统的重要叙事接口。

主要用途：

- 引导玩家
- 环境警告
- 飞船状态反馈
- 星图信息
- 主线任务
- 新区域或异常信号提示

N.O.V.A. 不应只是一个“任务菜单”。

她应该让飞船感觉像一个真实运行中的系统。

---

# 9. 战斗方向

StarboundMC 战斗不应只依赖 Minecraft 原版：

> 左键攻击 + 更高伤害数值。

武器应逐渐拥有：

- Primary Attack
- Secondary Skill
- Cooldown
- Energy Cost
- Animation
- Hit Profile
- Element / Effect
- Modifier

技能示例：

- Dash Slash
- Spin Slash
- Charged Shot
- Energy Wave
- Ground Slam
- Shield Bash
- Rocket Jump
- Piercing Beam
- Ricochet Shot
- Homing Barrage

优先建立通用技能框架，而不是为每把武器单独硬编码。

长期目标：

> 武器的身份来自攻击方式和技能，而不仅仅来自 DPS。

---

# 10. 技能镜头

部分大型武器技能可以使用 Cinematic Skill Camera。

例如：

- Spin Slash
- Ground Slam
- Rocket Jump
- 大剑蓄力攻击
- 长距离 Dash
- Boss Weapon Skill

技能触发时可以：

1. 平滑拉远
2. 进入第三人称
3. 展示角色动作和武器轨迹
4. 技能结束后平滑恢复原视角

重要原则：

> 人物可以旋转，但不要让摄像机跟着人物疯狂旋转。

例如 Spin Slash：

- 玩家身体旋转
- 摄像机保持相对稳定
- 玩家可以清楚看到角色和武器轨迹

技能镜头以后可以抽象为：

`SkillCameraController`

并允许玩家设置：

- Full
- Subtle
- Off

避免强制镜头导致晕动或影响战斗体验。

---

# 11. 美术方向

StarboundMC 的实体美术可以学习 Alex's Mobs / Alex's Caves 的设计思路，但不复制：

- 具体模型
- UV
- 纹理
- 动画参数
- 源码实现

重点学习：

- silhouette
- 身体比例
- cube-based modeling
- pivot 设计
- 分段尾巴
- secondary motion
- animation timing
- anticipation / impact / recovery

目标不是追求写实高模。

StarboundMC 应保持：

- Minecraft 风格
- Cuboid 为主
- Pixel Art 纹理
- 明确的轮廓
- 有辨识度的身体比例
- 不过度堆积模型细节

---

# 12. 实体动画原则

实体动画采用：

> 关键帧动画 + 程序动画

关键帧适合：

- 攻击
- 咆哮
- 跳跃
- 特殊技能
- 死亡
- 大型动作

程序动画适合：

- 呼吸
- 行走
- 尾巴摆动
- 耳朵 / 触角
- 看向玩家
- Idle 微动作

重要原则：

> Idle 状态也不能完全静止。

大型生物尤其需要：

- 身体重量感
- 头部延迟
- 尾巴 secondary motion
- 攻击前蓄势
- 命中瞬间
- 收招过程

---

# 13. AI 辅助开发工作流

Codex 可以作为主要技术执行工具，但不应承担最终设计决策。

推荐工作流：

> 用户决定方向  
> → ChatGPT 协助分析和设计  
> → 写入仓库设计文档  
> → Codex 实现  
> → 用户实际游戏内验收  
> → 根据反馈继续修改

对于模型和动画：

> Codex 创建模型 / 动画  
> → Blockbench 中查看  
> → 截图或录像  
> → 分析问题  
> → Codex 修改  
> → 反复迭代

用户不需要先掌握完整的建模和动画知识。

可以边开发边学习。

---

# 14. Codex 不应自动决定的内容

除非用户明确批准，Codex 不应自行决定：

- 新主线剧情
- 星球最终定位
- Boss 设定
- 核心玩法循环
- 大规模架构调整
- 新增大量依赖
- 修改宇宙核心数据结构
- 改变已有玩法进度
- 将讨论阶段构想直接实现

讨论中的想法不等于确认需求。

---

# 15. 开发优先级

当前建议的大方向：

1. 环境危险系统
2. EPP / 环境装备
3. 星球独特资源
4. POI / Microdungeon
5. 探索奖励
6. 武器技能框架
7. 实体与战斗内容
8. 外部模组世界接入
9. 扩展更多恒星系

不要急于实现：

- 大型殖民系统
- 数百颗程序化星球
- 超复杂派系系统
- 大量 NPC
- 巨型程序化遗迹
- 全套动态生态
- 复杂任务框架

先把一个完整探索闭环做扎实。

---

# 16. 第一个推荐完整探索闭环

一个理想的早期闭环：

> 玩家修复 Sublight Engine  
> → 前往 Barren  
> → 探索废弃设施  
> → 获取 Salvage 与关键科技材料  
> → 返回飞船  
> → 制造 Life Support / EPP  
> → 前往 Rocky Moon  
> → 获取 Fuel Crystal  
> → 解锁更远的宇宙探索

这个流程应该成为后续所有星球内容设计的参考模板。

---

# 17. 外部模组兼容原则

未来 StarboundMC 应允许通过：

- datapack
- compat module
- API

将外部维度注册为 Celestial Body。

概念上可以描述：

```text
dimension
+ celestial metadata
+ star system
+ navigation rules
+ environmental hazards
+ progression requirements
+ exploration completion conditions
```

例如：

```text
twilightforest:twilight_forest
→ StarboundMC Celestial Body
```

但 StarboundMC 核心不能直接依赖 Twilight Forest。

如果未安装对应模组：

- 该星球不出现
- 不破坏主线
- 不产生 missing dependency

整合包可以在 StarboundMC 基础上构建更大的宇宙冒险。

---

# 18. 项目设计原则总结

StarboundMC 应始终优先考虑：

**体验 > 内容数量**

**星球身份 > 方块数量**

**探索奖励 > 单纯采矿**

**准备与选择 > 硬数值门槛**

**技能表现 > 单纯 DPS**

**原创核心体验 > 对其他模组的依赖**

**可扩展框架 + 原创内容 > 单纯 Glue Mod**

最终目标：

> 让玩家感觉自己真的拥有一艘船，正在一个不断扩展的 Minecraft 宇宙中旅行，而不是通过菜单在几个维度之间传送。