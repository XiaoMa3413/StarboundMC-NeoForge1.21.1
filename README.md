# StarboundMC

> **Explore the stars. Upgrade your ship. Build your way forward.**

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-E68A2E)
![Java](https://img.shields.io/badge/Java-21-ED8B00)
![Version](https://img.shields.io/badge/version-0.1--alpha-6C63FF)
![License](https://img.shields.io/badge/code-MPL--2.0-blue)

**StarboundMC** 是一个面向 **Minecraft 1.21.1 / NeoForge** 的太空探索与科技进度模组。

从属于自己的飞船出发，与舰载 AI **N.O.V.A.** 一同重新启动飞船系统，在星图中规划航线、补充燃料、跨越恒星系，并通过传送器登陆不同的行星与卫星。

探索未知世界、采集资源、提炼体素、打印设备与升级模块——然后继续飞向更远的地方。

> [!WARNING]
> StarboundMC 目前处于 **Alpha 开发阶段**。  
> 内容、平衡性、世界生成、剧情设定和存档格式仍可能发生变化。游玩开发版本时建议备份重要世界。

---

## 📖 背景故事

> [!NOTE]
> **Lore WIP — 背景设定尚未完善，后续可能调整。**

你在一艘状态异常的飞船上醒来，许多系统已经离线，只有舰载 AI **N.O.V.A.** 仍在尝试恢复运行。

为了让飞船重新启航，你需要探索附近的星球，寻找资源、燃料和遗失的信息，并逐步修复飞船、解锁更远的航线。

至于飞船为何会变成这样，以及这片星域曾经发生过什么——目前仍是未知。

---

## ✨ 核心体验

```text
醒来
 ↓
获得物质枪
 ↓
登上初始飞船
 ↓
重新启动 N.O.V.A. 与飞船系统
 ↓
收集燃料
 ↓
打开星图选择目的地
 ↓
执行星系内 / 跨星系航行
 ↓
从轨道传送至行星表面
 ↓
探索 · 采矿 · 收集资源
 ↓
提炼体素
 ↓
打印设备与升级模块
 ↓
强化物质枪与飞船
 ↓
继续深入宇宙
```

StarboundMC 的目标并不是简单增加几个新维度，而是让**飞船成为玩家真正的家和探索中心**。

---

## 🚀 你的飞船

每个世界都会提供一艘可供玩家使用的初始飞船。

飞船内部目前包括：

- 驾驶台与三级星图
- N.O.V.A. 舰载人工智能终端
- 行星传送器
- 飞船燃料控制器
- 体素打印站
- 储物区域
- 基础生存补给
- 舷窗与实时太空天体渲染

飞船在 Minecraft 世界中的物理空间保持稳定，而它在 StarboundMC 的宇宙坐标中可以真正移动。

航行过程中，你可以从舷窗看到行星、恒星与其他天体的位置发生变化。

---

## 🌌 宇宙与星图

StarboundMC 使用数据驱动的宇宙系统组织恒星系、天体、航线与停靠位置。

目前已经包含 **两个恒星系** 和多种可探索天体：

| 天体 | 类型 | 可登陆 |
| --- | --- | :---: |
| Lush | 翠绿星球 | ✅ |
| Barren | 荒芜星球 | ✅ |
| Molten | 熔岩卫星 | ✅ |
| Frozen | 冰冻星球 | ✅ |
| Gas Giant | 气态巨行星 | ❌ |
| Rocky Moon | 岩石卫星 | ✅ |

气态巨行星没有固体表面，但飞船可以进入其轨道。

岩石卫星拥有程序化生成的撞击坑、巨型矿脉、稀疏采矿设施与废弃建筑。

### 星图

驾驶台提供三级导航界面：

**星域 → 恒星系 → 天体**

你可以查看天体信息、选择目标并确认航行。

当前燃料消耗：

- **星系内航行：20**
- **跨星系航行：100**
- **飞船最大燃料：1000**

飞行状态、燃料和当前位置均由服务端保存。

---

## 🪐 太空视觉

StarboundMC 拥有独立的飞船太空渲染系统，而不是简单使用 Minecraft 原版天空盒。

目前支持：

- 球形行星
- 行星表面贴图
- 昼夜面
- 大气层
- 行星自转
- 气态巨行星
- 行星光环
- 恒星
- 深空星点
- 航行过程中的天体位移
- 星系尺度宇宙坐标
- 亚光速与跨星系跃迁视觉

太空视觉仍在持续迭代。

相关设计与开发计划可查看：

[`docs/ship-space-visual-enhancement-plan.md`](docs/ship-space-visual-enhancement-plan.md)

---

## 🤖 N.O.V.A.

**N.O.V.A.** 是玩家飞船上的人工智能，也是 StarboundMC 当前剧情与任务系统的重要组成部分。

它会：

- 报告飞船状态
- 引导玩家重新启动关键系统
- 提供探索提示
- 追踪任务进度
- 在登陆新世界时建立通讯
- 执行轨道扫描
- 通过舰载终端提供任务与信息

随着剧情系统继续开发，N.O.V.A. 也会成为玩家了解飞船过去、探索未知星域以及推进主线的重要角色。

---

## ⛏️ 物质枪

物质枪是 StarboundMC 的核心工具之一。

按住右键即可使用持续激光采集方块。

目前拥有四条升级路线：

- ⚡ **采集速度**
- 📡 **作用距离**
- ⛏️ **挖掘等级**
- 🍀 **时运**

升级数据直接保存在物品中，并由服务端处理实际挖掘结果。

---

## 🧊 体素系统

资源可以通过机器转换成 **Voxel / 体素**。

体素相当于 StarboundMC 科技系统中的通用制造资源。

目前基础循环为：

```text
探索与采矿
    ↓
获得材料
    ↓
体素提炼机
    ↓
Voxel
    ↓
体素打印站
    ↓
设备 / 零件 / 升级模块
```

体素存储在玩家自己的体素钱包中。

体素打印站已经支持：

- 材料检测
- 自动扣除材料
- 体素支付
- 批量打印
- FIFO 打印队列
- 打印进度
- 输出槽
- 队列取消与退款

这一系统未来会逐步成为 StarboundMC 制造体系的核心。

---

## 📡 传送网络

飞船传送器可以把玩家送往当前停靠天体的表面。

玩家自己放置并命名的传送器也可以加入共享目的地列表，从而建立：

```text
飞船
 ↕
行星表面
 ↕
基地
 ↕
其他传送节点
```

被拆除的传送节点会自动从网络中清理。

---

## 🛠️ 当前开发状态

StarboundMC 目前处于 **`0.1-alpha`**。

已经建立的主要系统包括：

- ✅ NeoForge 1.21.1 主线迁移
- ✅ 程序化初始飞船
- ✅ 数据驱动宇宙系统
- ✅ 多恒星系星图
- ✅ 服务端权威航行
- ✅ 飞船燃料系统
- ✅ 多行星维度
- ✅ 气态巨行星与岩石卫星
- ✅ 太空天体渲染
- ✅ N.O.V.A. 舰载 AI
- ✅ 基础任务 / 序章流程
- ✅ 行星传送网络
- ✅ 物质枪
- ✅ 体素提炼
- ✅ 体素打印
- ✅ 中英文支持

正在推进：

- 🚧 主线剧情与世界观
- 🚧 行星环境与 EPP 生存系统
- 🚧 飞船设备与引擎玩法
- 🚧 太空视觉继续升级
- 🚧 普通机器 UI 重构
- 🚧 行星专属资源
- 🚧 POI 与探索内容
- 🚧 更多制造与科技进度
- 🚧 武器与装备系统

详细开发计划见：

[`docs/README.md`](docs/README.md)

---

## 📦 安装

### 游戏版本

| 项目 | 版本 |
| --- | --- |
| Minecraft | **1.21.1** |
| NeoForge | **21.1.x** |
| Java | **21** |
| StarboundMC | **0.1-alpha** |

### 客户端

需要安装：

1. Minecraft 1.21.1
2. NeoForge 21.1.x
3. LDLib2 `2.2.36.a` 或兼容版本
4. StarboundMC

将模组 JAR 放入：

```text
.minecraft/mods/
```

### 服务端

StarboundMC 需要同时安装在客户端和服务器。

LDLib2 当前主要作为客户端 UI 前置使用。

---

## 🧑‍💻 从源码构建

需要：

- JDK 21
- Git

克隆仓库：

```bash
git clone https://github.com/XiaoMa3413/StarboundMC-NeoForge1.21.1.git
cd StarboundMC-NeoForge1.21.1
```

Windows：

```powershell
.\gradlew.bat build
```

Linux / macOS：

```bash
./gradlew build
```

构建后的 JAR 位于：

```text
build/libs/
```

常用开发命令：

```bash
./gradlew test
./gradlew build
./gradlew runData
./gradlew runClient
./gradlew runServer
```

Windows 下将 `./gradlew` 替换为 `.\gradlew.bat` 即可。

---

## 🧪 项目原则

StarboundMC 正在逐步形成一套比较明确的工程规则：

- 服务端负责权威游戏状态
- 尽可能保证世界存档兼容
- 注册 ID 保持稳定
- 宇宙数据尽量数据驱动
- 客户端视觉与服务端逻辑分离
- 新功能应尽可能附带自动化测试
- 大型系统先形成设计文档，再进入实现
- 已完成计划归档，当前计划与历史记录分离

这也是为什么仓库中会保留较完整的 `docs/` 目录。

---

## 🐛 已知问题

这是 Alpha 项目，目前仍存在需要继续测试或完善的内容。

最新记录请查看：

[`docs/known-issues.md`](docs/known-issues.md)

如果你遇到新的问题，可以通过 GitHub Issues 提交。

提交问题时如果可以，请附带：

- StarboundMC 版本
- NeoForge 版本
- 单人 / 局域网 / 专用服务器
- 复现步骤
- `latest.log`
- 崩溃报告（如果存在）

---

## 🤝 参与开发

StarboundMC 目前仍在快速开发中，Issue、代码修复、测试和文档改进都欢迎参与。

提交 Pull Request 前，请至少确保：

```bash
./gradlew test
./gradlew build
```

可以正常通过。

涉及以下内容的 PR，请额外说明兼容性影响：

- 注册 ID
- 存档数据
- 网络协议
- 世界生成
- 宇宙数据格式
- 玩家物品数据

---

## 📖 文档

开发文档入口：

**[`docs/README.md`](docs/README.md)**

其中包括：

- 宇宙系统设计
- EPP 环境生存
- 舰载 AI / N.O.V.A.
- 亚光速引擎
- 太空视觉
- 机器 UI
- 体素打印
- 已知问题
- 历史设计归档

---

## 📜 License

StarboundMC 对**程序代码**和**游戏素材**采用不同的许可方式。

### Code

项目程序代码：

**[Mozilla Public License 2.0](LICENSE)**

### Assets

StarboundMC 原创贴图、模型、声音、音乐、结构、艺术作品、标识和叙事内容：

**All Rights Reserved unless otherwise stated**

详见：

[`LICENSE-ASSETS.md`](LICENSE-ASSETS.md)

### Third-party content

第三方素材及其许可证信息：

[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)

部分行星素材基于 **Solar System Scope Textures**，按照 **CC BY 4.0** 使用。

---

## ⚠️ Disclaimer

StarboundMC 是一个由社区独立开发的 Minecraft 模组项目。

项目受到太空探索类游戏，包括 **Starbound** 的启发，但并非 Chucklefish 官方项目，也不存在官方关联或授权关系。

Minecraft 及相关商标属于 Mojang Studios / Microsoft。

---

<p align="center">
  <b>StarboundMC</b><br>
  From one small ship to an entire universe.
</p>
