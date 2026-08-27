# StarboundMC

StarboundMC 是一个面向 Minecraft 1.21.1 / NeoForge 的 Alpha 太空探索内容模组。玩家从一艘程序化生成的飞船出发，通过三级星图选择天体、管理共享燃料、执行服务端权威跃迁，并使用传送器探索不同的行星表面。

本仓库是 Forge 1.20.1 版本迁移后的 NeoForge 主线。模组 ID `starboundmc`、Java 包名 `com.starboundmc` 和既有注册 ID 均保持不变。

## 核心体验

~~~text
首次进入世界
  → 获得物质枪和传送器
  → 登上初始飞船
  → 为飞船补充燃料
  → 在驾驶台星图中选择天体
  → 观看飞船完成同星系或跨星系跃迁
  → 使用传送器抵达行星表面
  → 采集资源并升级物质枪
~~~

- 21 格长的程序化初始飞船，包含驾驶台、传送器、燃料控制器、储物仓、气闸门和钛制合金炉。
- 星域、恒星系、天体聚焦三级星图，支持响应式布局、天体信息、航线和跃迁动画。
- 两个恒星系、四个可达天体，以及可见但尚未开放的气态巨行星和岩石卫星。
- 服务端权威固定航线跃迁、共享燃料、飞行状态持久化和断线/重启恢复。
- 熔岩、冰冻、荒芜三个独立行星维度；翠绿天体使用主世界表面。
- 飞船、当前行星与玩家命名传送器之间的传送网络。
- 物质枪持续激光采集，以及速度、射程、挖掘等级、时运四条升级路线。
- 中英文完整本地化。

## 运行要求

| 项目 | 要求 |
| --- | --- |
| Minecraft | `1.21.1` |
| Mod Loader | NeoForge `21.1.248` 或同 Minecraft 版本的兼容 21.1.x 构建 |
| 客户端前置 | LDLib2 `2.2.36.a` 或兼容版本 |
| 模组版本 | `0.1-alpha` |
| Java | 开发和独立服务器使用 64 位 Java 21 |
| 安装侧 | 客户端与服务器均需安装 |

## 安装

1. 安装 Minecraft 1.21.1 对应的 NeoForge。
2. 客户端安装 LDLib2 `2.2.36.a` 或模组元数据允许的兼容版本。
3. 从发布页或本地构建取得 StarboundMC JAR。
4. 将 LDLib2 和 StarboundMC JAR 放入客户端的 `mods` 目录。
5. 联机或开设专用服务器时，在服务端安装相同版本的 StarboundMC；LDLib2 当前仅为客户端前置。
6. 启动前备份已有世界，尤其是从 Forge 1.20.1 升级的存档。

旧 Forge 存档的备份副本已实际完成 NeoForge 1.21.1 升级、保存和再次启动，但升级应视为单向操作。不要在没有备份的情况下试用，也不要将升级后的世界重新交给旧 Forge 版本打开。

## 快速上手

### 飞船与星图

- 新玩家首次登录会获得物质枪和传送器，并被送往初始飞船。
- 默认按 `H` 返回飞船；可在“控制 → 星际边界”中修改按键。
- 与飞船星图导航台交互打开星图。旧驾驶台仅为存档兼容保留，已标记为废弃。
- 先选择恒星系，再选择可达天体并确认跃迁。
- 星系内跃迁消耗 20 燃料，跨星系跃迁消耗 100 燃料；油箱上限为 1000。
- 飞船的物理空间保持固定，星图和舷窗展示其在连续宇宙坐标中的航行过程。

### 燃料

燃料控制器接受以下原版物品：

| 燃料 | 单件燃料值 |
| --- | ---: |
| 木炭 | 5 |
| 煤炭 | 10 |
| 烈焰粉 | 20 |

燃料由整艘飞船共享并随世界保存。控制器只会消耗能够完整加入油箱的物品。

### 行星与传送器

| 天体 | 所属星系 | 表面 | 威胁等级 |
| --- | --- | --- | ---: |
| 翠绿天体 | 第一恒星系 | 主世界 | 1/10 |
| 荒芜天体 | 第一恒星系 | 独立荒芜维度 | 2/10 |
| 熔岩卫星 | 第一恒星系 | 独立熔岩维度 | 6/10 |
| 冰冻天体 | 第二恒星系 | 独立冰冻维度 | 4/10 |

飞船传送器可前往当前停靠天体的表面。玩家放置并命名的传送器会进入共享目的地列表；目标被拆除后会自动失效并清理。

### 物质枪

- 物质枪通过持续激光完成采集，不按原版镐的左键挖掘逻辑工作。
- 在物质枪升级工作台中放入物质枪，并消耗物质枪组件提升速度、射程、挖掘等级或时运。
- 升级与时运效果保存在物品 Data Components 中，掉落计算由服务端执行。

## 当前 Alpha 限制

- 尚无自由驾驶、推进器控制或飞船碰撞。
- 翠绿天体暂时复用主世界，没有独立维度。
- 钨、钛、耐钢和星核矿石已有方块、掉落与加工链，但尚未加入自然世界生成。
- 气态巨行星和岩石卫星仅为星图占位目标，当前不可到达。
- 原版下界传送门被禁用，这是当前玩法规则。
- 仍建议在新模组组合、不同 GUI Scale、局域网和专用服务器环境中先使用备份世界验证。

## 开发与构建

固定开发基线：

- NeoForge `21.1.248`
- ModDevGradle `2.0.144`
- Gradle `9.2.1`
- Java toolchain `21`

常用命令：

~~~powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runData
.\gradlew.bat runClient
.\gradlew.bat runServer
~~~

构建产物位于 `build/libs/`。要让开发客户端启动后直接打开现有本地世界：

~~~powershell
.\gradlew.bat runClient -PquickPlayWorld=world-folder
~~~

Gradle Wrapper 默认把 Gradle、Minecraft、NeoForge 依赖和运行资产缓存到仓库内的 `.gradle-home/`，避免占用系统盘；该目录不会提交。可以通过 `GRADLE_USER_HOME` 显式覆盖。

专服维护烟测：

~~~powershell
.\scripts\stage2-rcon-smoke.ps1
.\scripts\stage6-rcon-smoke.ps1
.\scripts\stage8-rcon-smoke.ps1
~~~

`stage8-rcon-smoke.ps1 -AllowMissingConsole` 只用于已被玩家改造、固定控制台坐标为空的旧飞船备份。新世界和常规验收不得使用该开关。

## 验证状态

- 默认测试套件：195 项，0 failures、0 errors、0 skipped。
- `runData` 和完整 `build` 通过。
- 新世界已通过注册对象、设备规则、程序化飞船和四维度 RCON 烟测。
- 两份 Forge 1.20.1 旧存档备份已完成 NeoForge 升级、保存和重启。
- 客户端资源重载、声音引擎和纹理图集构建无 StarboundMC 资源错误。
- 专用服务器可启动至 `Done`，并能正常保存全部维度后停服。
- 同星系/跨星系跃迁、燃料扣除、航行动画和抵达状态已经真人复验通过。
- LDLib2 三级星图已完成客户端视觉、交互与跃迁核验。普通机器界面仍使用旧实现，并按
  [当前 UI 迁移计划](docs/machine-ui-ldlib2-plan.md) 在试验分支中逐项完善后再合入 `main`。

当前文档入口见 [docs/README.md](docs/README.md)。已完成的 NeoForge 迁移记录与星图重绘计划
位于 [docs/archive](docs/archive/)，Forge 1.20.1 历史设计快照位于
[docs/legacy-forge](docs/legacy-forge/)。

## 许可与素材

项目元数据声明为 **All Rights Reserved**。除另有明确说明外，不授予复制、修改或再分发项目代码和素材的许可。

四张 4096×2048 行星表面贴图基于 Solar System Scope 提供的素材，按 CC BY 4.0 使用：

- `lush`：Earth day map 与 clouds 合成
- `molten`：Venus surface
- `frozen`：Eris fictional
- `barren`：Mars

来源：[Solar System Scope Textures](https://www.solarsystemscope.com/textures/)。

Starbound 来源的参考、解包素材、标志或声音不属于上述 CC BY 4.0 授权范围。公开发布或分发前，必须分别确认其来源、权利状态与使用许可。
