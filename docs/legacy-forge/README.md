# StarboundMC（星际边界）

StarboundMC 是一个面向 Minecraft Forge 1.20.1 的开发中模组，目标是在 Minecraft 中建立
“飞船基地、连续宇宙、行星跃迁、星图导航与物质枪采集”的完整玩法闭环。

当前版本已经可以从初始飞船出发，在两个恒星系与四个可达天体之间导航，并通过传送器进入
对应行星表面。项目仍处于 alpha 阶段，不应视为稳定发布版。

| 项目 | 当前值 |
| --- | --- |
| 模组版本 | `0.1-alpha` |
| Minecraft | `1.20.1` |
| Forge | `47.4.10` |
| Java | `17` |
| 映射 | Mojang official `1.20.1` |
| modId / 包名 | `starboundmc` / `com.starboundmc` |
| 网络协议 | `4` |

## 当前实现

| 系统 | 状态 |
| --- | --- |
| 初始飞船 | 21 格长的程序化紧凑探索艇；单层功能舱、收束舰首、驾驶舱罩、短龙骨和双引擎吊舱 |
| 星图 | 星域、恒星系、行星聚焦三级导航；响应式近全屏布局与 2× 高密度像素终端绘制 |
| 宇宙空间 | 100,000 单位分区坐标、连续星系影响、远景恒星 LOD、服务器权威虚拟飞行 |
| 跃迁 | 固定航线自动飞行；按距离约 11–28 秒，同星系耗油 20、跨星系耗油 100 |
| 天体 | 两个恒星系；翠绿、熔岩、冰冻、荒芜四个可达天体，另有两个锁定占位天体 |
| 行星表面 | 熔岩、冰冻、荒芜拥有独立维度；翠绿暂时使用主世界 |
| 传送器 | 飞船、当前行星表面和已命名传送器之间移动，名称与位置持久化 |
| 物质枪 | 右键激光采集；速度、射程、挖掘等级、时运四轨升级 |
| 资源链 | 钨、钛、耐钢、星核材料与钛制合金炉；矿脉世界生成暂未启用 |

当前飞船的物理结构仍固定在飞船维度中；移动的是服务器维护的宇宙坐标与朝向，客户端据此
渲染星体、星系环境和跃迁演出。自由操控飞船尚未实现，但位姿提供器和连续坐标层已经为后续
分支预留接口。

## 快速开始

### 环境

- 64 位 JDK 17
- Git
- Windows PowerShell 或能运行 Gradle Wrapper 的终端
- 首次构建需要访问 Forge、Maven Central 等依赖源

### 常用命令

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runData
```

如希望将 Gradle 缓存保留在仓库工作区内，可在命令后统一使用 `-g .gradle-home`；该目录已被
Git 忽略。

构建产物位于：

```text
build/libs/starboundmc-0.1-alpha-1.20.1forge.jar
```

`runData` 会把飞船维度的生成数据写入 `src/generated/resources/`。只有在维度注册或数据生成
逻辑变化时才需要运行并检查其差异。

## 开发存档与重置

默认开发世界为 `run/saves/新的世界`。程序化飞船只在新区块生成时放置，因此修改船体后需要：

1. 完全退出该世界。
2. 备份需要保留的测试数据。
3. 删除或移走 `run/saves/新的世界/dimensions/starboundmc/ship`。
4. 重新进入世界，让飞船维度重新生成。

燃料、当前天体、星图访问记录和飞行快照保存在主世界 `SavedData` 中，单独重建飞船维度不会
清除这些状态。若要测试真正的首次登录流程，应使用新世界或同时清理对应玩家/主世界数据。

## 代码结构

```text
src/main/java/com/starboundmc/
├── StarboundMC.java       模组入口与注册
├── block/                 飞船设备、传送器、矿石及方块实体
├── item/                  物质枪、升级组件与材料
├── menu/                  服务端容器菜单
├── client/                UI、天空、天体、激光、音效与客户端状态
│   └── space/             连续空间解析、环境混合和恒星 LOD
├── entity/                船长椅使用的 SeatEntity
├── event/                 登录、跃迁、传送门和兼容迁移事件
├── network/               SimpleChannel 数据包
├── sound/                 声音注册
├── space/                 分区宇宙坐标与位移值对象
├── warp/                  服务器飞行阶段机、航线和持久化状态
└── world/                 维度、区块生成器、船体与星图静态数据
    └── starmap/           恒星系、天体视觉与空间索引

src/main/resources/        模型、贴图、语言、声音、配方和维度 JSON
src/generated/resources/   数据生成产物
src/test/java/             JUnit 5 单元测试
tools/                     贴图与飞行几何辅助脚本
docs/                      当前系统手册与历史实施计划
```

## 验证要求

按改动风险选择验证范围：

| 改动 | 最低验证 |
| --- | --- |
| 纯文档 | `git diff --check`、链接和路径检查 |
| 纯 Java 逻辑 | `test` |
| 注册、资源、网络、维度 | `build` |
| 飞船结构或方块实体 | `build` + 重建飞船维度实机检查 |
| 星图、天空或跃迁视觉 | `test` + 对应 GUI Scale、航线和重进存档实机检查 |

提交前至少确认工作区没有把 `.gradle-home/`、`run/`、解包素材或临时截图加入 Git。

## 文档地图

### 当前实现的权威入口

- [`docs/GAMEPLAY.md`](docs/GAMEPLAY.md)：当前玩法、架构、状态所有权、兼容与已知边界。
- [`Forge-Modding-Notes-1.20.x.md`](Forge-Modding-Notes-1.20.x.md)：本项目使用 Forge 1.20.1 时必须遵守的开发护栏。

### 历史设计与阶段验收记录

以下文件记录方案选择、阶段提交和视觉验收，不作为当前代码数值的唯一依据：

- [`docs/ship-motion-warp-plan.md`](docs/ship-motion-warp-plan.md)
- [`docs/continuous-space-modular-plan.md`](docs/continuous-space-modular-plan.md)
- [`docs/starmap-high-density-ui-plan.md`](docs/starmap-high-density-ui-plan.md)
- [`docs/starmap-art-direction-plan.md`](docs/starmap-art-direction-plan.md)

当计划记录与代码、测试或 `GAMEPLAY.md` 冲突时，以当前代码和测试为准。

## 分支建议

- `main`：面向稳定整合或发布。
- `develop`：共享开发基线。
- `feature/*`、`codex/*`：范围明确、可单独验证的功能分支。

功能分支优先合并到 `develop`，确认整合结果后再由 `develop` 推进到 `main`。不要把同一功能分支
分别独立合并到两个长期分支，否则会产生内容相同但 merge commit 不同的冗余历史。

提交信息遵循 Conventional Commits，例如 `feat:`、`fix:`、`docs:`、`refactor:` 和 `chore:`。

## 资源与许可

项目元数据当前声明 `All Rights Reserved`。仓库中的 `LICENSE.txt` 是 Forge/MCP 随 MDK 提供的
第三方许可与通知，并不替代本项目自身的许可声明。

四张 4096×2048 行星表面贴图基于 Solar System Scope 提供的素材，按 CC BY 4.0 使用：

- `lush`：Earth day map 与 clouds 合成
- `molten`：Venus surface
- `frozen`：Eris fictional
- `barren`：Mars

来源：[Solar System Scope Textures](https://www.solarsystemscope.com/textures/)。Starbound 来源的参考、
解包素材、标志或声音不属于上述 CC BY 4.0 授权范围，分发前需分别确认其权利与使用条件。
