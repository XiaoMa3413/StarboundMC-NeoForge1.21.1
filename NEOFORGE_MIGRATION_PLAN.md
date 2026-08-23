# StarboundMC Forge → NeoForge 迁移计划

> 交接用途：这份文档是新 Codex 任务的首要上下文。开始任何修改前，必须完整阅读本文，并检查目标仓库的实际状态是否与“当前状态”一致。
>
> 审计日期：2026-08-23

## 1. 任务目标

将现有 Minecraft Forge 1.20.1 模组 **StarboundMC** 迁移为可长期维护的 NeoForge 版本，同时保留现有玩法设计、注册标识和核心存档语义。

推荐最终技术基线：

| 项目 | 目标 |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | 最新稳定的 `21.1.x` |
| Java | 64 位 JDK `21` |
| 构建插件 | `ModDevGradle` |
| modId | `starboundmc`，禁止更改 |
| Java 包 | `com.starboundmc`，禁止无理由重命名 |
| 当前模组版本 | 迁移期保留 `0.1-alpha`，发布前再决定是否提升 |

选择 1.21.1 是明确的产品与生态决策：该版本拥有更完善、更稳定的模组与整合包生态，兼容模组选择更多，玩家安装基数和可触达群体也更大。相比追逐最新 Minecraft 主线，1.21.1 更适合 StarboundMC 在 Alpha 阶段积累玩家、联调兼容性并完成核心玩法。

需要接受的取舍：NeoForge 官方已将 1.21.1 文档标为不再积极维护，因此迁移时必须固定到最新稳定的 `21.1.x` 构建、记录确切版本，并避免依赖尚未验证的实现细节。除非用户重新批准，不得自行把目标升级到 1.21.11、26.1 或其他版本。

迁移不是简单替换 `net.minecraftforge` 包名。Minecraft 1.20.1 到 1.21.1 跨越了物品数据组件、网络 Payload、持久化接口、注册、资源格式、世界生成和渲染 API 的多轮破坏性变更。

## 2. 两个目录的角色

### 旧 Forge 项目：只读参考与行为基线

```text
E:\Develop\doing\StarboundMC A1
```

- Minecraft 1.20.1 / Forge 47.4.10 / Java 17。
- 当前分支在审计时为 `develop`，工作区干净。
- 现有构建产物和旧测试报告可作为迁移前行为证据。
- 不在旧仓库中进行 NeoForge 迁移，不修改旧项目以“配合”新项目。
- 需要核对原行为、资源、注册名或存档字段时，从这里读取。

### 新 NeoForge 项目：唯一迁移工作区

```text
E:\Develop\doing\StarboundMC Neoforge
```

截至 2026-08-23，该目录状态为：

- 已有 `src/main/java`：137 个文件。
- 已有 `src/test/java`：27 个文件。
- 共 164 个文件已与旧项目完成 SHA-256 校验，内容一致。
- 尚无 `src/main/resources`。
- 尚无 NeoForge Gradle/Wrapper 项目骨架。
- 尚无 Git 仓库。
- 尚不可构建。

新任务开始后必须重新检查这些事实；用户可能已经在此文档写入后创建项目骨架或 Git 仓库。

## 3. 当前模组是什么

StarboundMC 是一个处于 Alpha 阶段的太空探索内容模组。当前完整玩法链：

```text
首次登录
  → 获得物质枪和传送器
  → 进入程序化初始飞船
  → 使用驾驶台打开三级星图
  → 选择天体并消耗燃料
  → 服务器推进虚拟宇宙航线
  → 抵达后通过传送器进入行星表面
  → 采集资源、返回飞船并升级物质枪
```

已实现系统：

- 21 格长的程序化初始飞船和船内设备。
- 星域、恒星系、行星聚焦三级星图 UI。
- 两个恒星系、四个可达天体、两个锁定占位天体。
- 100,000 单位分区的连续宇宙坐标。
- 服务端权威固定航线跃迁，约 11–28 秒。
- 同星系耗油 20、跨星系耗油 100、油箱上限 1000。
- 熔岩、冰冻、荒芜三个独立维度；翠绿天体暂用主世界。
- 飞船、当前行星和已命名传送器之间的传送网络。
- 物质枪持续激光采集，以及速度、射程、挖掘等级、时运四轨升级。
- 钨、钛、耐钢、星核材料和钛制合金炉。

已知未完成内容：

- 没有自由驾驶、推进和飞船碰撞。
- 翠绿天体没有独立维度。
- 四种新矿石没有自然世界生成。
- 气态巨行星和岩石卫星仍是不可达占位。
- 专用服务器尚缺稳定的完整发布验收矩阵。

迁移阶段不得顺便扩展这些未完成功能。先恢复行为等价，再单独开发新内容。

## 4. 代码规模与高风险区域

迁移前审计数据：

| 类型 | 数量 |
| --- | ---: |
| 主 Java 文件 | 137 |
| 主 Java 代码行 | 约 15,759 |
| 测试类 | 27 |
| 测试代码行 | 约 1,540 |
| 旧资源文件 | 140 |
| 直接引用 `net.minecraftforge` 的主源码 | 41 个文件 |
| Forge import | 119 条 |
| 旧网络包 | 12 种 |
| 旧测试报告 | 95 项测试，0 失败、0 错误、0 跳过 |

最大且风险最高的文件：

- `client/PlanetRenderer.java`：约 1,268 行。
- `client/ShipConsoleScreen.java`：约 1,202 行。
- `client/StarmapOverlayRenderer.java`：约 482 行。
- `client/StarMapCanvas.java`：约 470 行。
- `warp/ShipFlightController.java`：约 441 行。
- `client/space/StarSystemResolver.java`：约 357 行。
- `item/MatterManipulatorItem.java`：约 307 行。

不要从渲染器开始迁移。先建立可构建的 common/server 基线，再处理客户端。

## 5. 必须保持的兼容约束

除非用户明确批准，不得更改：

- modId `starboundmc`。
- Java 顶层包 `com.starboundmc`。
- 已发布的方块、物品、实体、菜单、音效、维度及数据资源 ID。
- 四个天体 ID：`lush`、`molten`、`frozen`、`barren`。
- 星图条目 ID，例如 `sys1:lush`、`sys2:frozen`。
- 主世界 `SavedData` 键：`starboundmc_ship` 和 `starboundmc_teleporters`。
- 玩家首次礼包标记：`starboundmc.starter_given`。
- 旧存档中的关键 NBT 字段，除非同时提供迁移读取路径。

网络协议可以重新设计，因为 Forge `SimpleChannel` 必须迁移为 NeoForge Payload；迁移后应建立新的协议版本，不得假装与 Forge 客户端兼容。

旧 Forge 1.20.1 客户端、服务器和 NeoForge 1.21.1 版本不要求网络互通。

存档兼容是“尽量保留、自定义数据显式迁移”，不是承诺原版跨多个版本后自动无损。测试旧存档前必须备份，开发早期只使用新世界。

## 6. 文件迁移规则

### 必须使用 NeoForge 1.21.1 模板重新创建

- `build.gradle` / `build.gradle.kts`。
- `settings.gradle` / `settings.gradle.kts`。
- `gradle.properties` 的构建与版本字段。
- `gradle/wrapper/**`、`gradlew`、`gradlew.bat`。
- `src/main/resources/META-INF/neoforge.mods.toml`。
- `pack.mcmeta`。
- IDE 运行配置和 datagen 配置。

禁止把旧 ForgeGradle 文件整体复制到新项目。旧 `META-INF/mods.toml` 不能继续使用。

### 可以复制但必须逐项验证

- `assets/starboundmc/textures/**`。
- `assets/starboundmc/sounds/**` 和 `sounds.json`。
- `assets/starboundmc/lang/**`。
- 方块模型、blockstate 和现有物品模型。
- 配方、战利品表、维度、dimension type 和 noise settings JSON。
- `logo.png`。
- Java 源码和纯逻辑测试。

1.21.1 资源格式需要特别检查：

- 旧 `data/starboundmc/loot_tables/` 应迁移/验证为当前单数目录 `loot_table/`。
- 1.21.1 仍以现有 `assets/starboundmc/models/item/` 模型为基础；逐个检查模型父级、纹理路径和加载警告，不引入 1.21.4+ 才使用的新物品定义格式。
- 维度、世界生成、配方和战利品表必须使用当前数据包 schema 验证。
- `src/generated/resources` 不直接复制，datagen 可运行后重新生成。

### 永远不复制

- `.gradle/`、`.gradle-home/`。
- `build/`。
- `run/`、开发世界和崩溃日志。
- IDE 缓存目录。
- 旧 Forge JAR。

## 7. 迁移文档地图与权威级别

旧项目的 7 份 Markdown 文档整体迁移到：

```text
docs/legacy-forge/
```

该目录是 Forge 1.20.1 基线的只读文档快照，不是 NeoForge 当前开发文档。保留原相对目录结构，以保证旧 README 中的链接仍然可用。

| 新项目路径 | 用途 | 权威级别 |
| --- | --- | --- |
| `docs/legacy-forge/README.md` | 旧项目总览、玩法摘要、旧构建与存档操作 | 背景入口；其中 Forge 命令和版本已过时 |
| `docs/legacy-forge/docs/GAMEPLAY.md` | 迁移前玩法、架构、状态所有权和已知限制 | **行为基线的首要文档**，但 API 说明仍是旧版 |
| `docs/legacy-forge/Forge-Modding-Notes-1.20.x.md` | Forge 1.20.1 注册、侧别、网络、存档和渲染护栏 | 仅用于理解旧实现；**不得作为 NeoForge API 指南** |
| `docs/legacy-forge/docs/continuous-space-modular-plan.md` | 连续宇宙坐标、星系解析、LOD 和模块边界的设计理由 | 历史设计与验收记录 |
| `docs/legacy-forge/docs/ship-motion-warp-plan.md` | 飞船位姿、确定性航线、跃迁阶段和恢复方案 | 历史设计与验收记录 |
| `docs/legacy-forge/docs/starmap-high-density-ui-plan.md` | 星图响应式布局、高密度绘制和 GUI Scale 验收 | UI 行为与验收参考 |
| `docs/legacy-forge/docs/starmap-art-direction-plan.md` | 星图像素终端风格、视觉身份和美术边界 | 视觉验收参考 |

发生冲突时按以下顺序判断：

1. 新项目当前代码、测试和实际运行结果。
2. `NEOFORGE_MIGRATION_PLAN.md` 中已由用户批准的迁移目标与约束。
3. NeoForge 1.21.1 官方文档和对应版本源码。
4. legacy `GAMEPLAY.md` 描述的迁移前行为。
5. 四份历史设计计划。
6. Forge 1.20.1 开发护栏。

迁移过程中产生的新架构说明应写入新项目正常的 `docs/`，不要反向修改 `docs/legacy-forge/` 快照。

## 8. 总体迁移原则

1. **模板优先**：先让空 NeoForge 模组运行，再接入旧代码。
2. **纵向切片**：每次恢复一个可验证系统，不做全仓盲目替换后一次性修错。
3. **服务端真值不变**：燃料、飞行、目的地和传送必须继续由服务端验证。
4. **客户端严格隔离**：任何 `net.minecraft.client` 引用不得泄漏到专用服务器加载路径。
5. **注册 ID 不变**：优先兼容旧世界，禁止为了方便改名。
6. **先行为等价，后重构**：迁移提交不同时承担大规模架构重写。
7. **每阶段可回滚**：每个阶段单独提交并留下验证结果。
8. **编译器驱动但不迷信机械替换**：包名可辅助替换，API 语义必须按 1.21.1 文档重写。

## 9. 分阶段实施计划

### 阶段 0：建立安全基线和 NeoForge 骨架

目标：新目录成为独立、可启动的 NeoForge 1.21.1 项目。

任务：

- 检查目标目录是否已经初始化 Git；若没有，由用户确认后初始化。
- 在临时目录使用 NeoForge Mod Generator 生成 Minecraft 1.21.1 + 最新稳定 NeoForge 21.1.x + ModDevGradle 项目。
- 将模板构建骨架合并进目标目录，不能覆盖已有 `com/starboundmc` 源码。
- 设置 `mod_id=starboundmc`、`mod_group_id=com.starboundmc`、版本和作者信息。
- 配置 Java toolchain 21。
- 保留/加入 JUnit 5 测试依赖。
- 使用模板的 `neoforge.mods.toml` 和 `pack.mcmeta`。
- 先用模板示例入口验证 `runClient`、`runServer` 和 `build`。
- 删除模板示例 mod 类和示例资源，避免注册冲突。

验收：

```powershell
java -version
.\gradlew.bat --version
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

完成条件：空 StarboundMC NeoForge 模组能在客户端和专用服务器加载，构建产出 JAR。

实施状态（2026-08-24）：**已完成**。

- 已固定 Minecraft `1.21.1`、NeoForge `21.1.248`、ModDevGradle `2.0.144`、Gradle `9.2.1` 和 Java toolchain `21`。
- 已初始化 Git 并绑定 `https://github.com/XiaoMa3413/StarboundMC-NeoForge1.21.1.git`。
- 阶段 0 通过独立的 `src/bootstrap/java` 最小入口建立运行基线；原 Forge 源码和测试保持原位且未被改写，阶段 1 再按纵向切片接回标准源集。
- `gradlew`/`gradlew.bat` 默认将大型缓存放在项目内 `.gradle-home/`，可由显式 `GRADLE_USER_HOME` 覆盖。
- 新运行验证：`gradlew.bat build` 成功并生成 `build/libs/starboundmc-0.1-alpha.jar`；`runClient` 使用 Java 21 加载 StarboundMC、进入集成世界、正常保存并以 0 退出；`runServer` 使用 Java 21 加载 StarboundMC 并达到 `Done`，烟测工具随后按 PID 结束测试服务器，因此该次 Gradle 任务本身以非零退出。

### 阶段 1：入口、注册和事件总线

目标：所有静态注册对象能被 NeoForge 识别，暂不要求完整玩法。

优先迁移：

- `StarboundMC.java`
- `block/ModBlocks.java`
- `block/entity/ModBlockEntities.java`
- `item/ModItems.java`
- `entity/ModEntities.java`
- `menu/ModMenus.java`
- `sound/ModSounds.java`
- `world/ModWorldgen.java` 的注册壳层

关键变化：

- `net.minecraftforge.*` 转为相应 `net.neoforged.*`。
- 模组入口使用 NeoForge 构造器注入的 `IEventBus`/`ModContainer`，不照搬旧 `FMLJavaModLoadingContext` 用法。
- 所有 `DeferredRegister` 和注册事件按 1.21.1 模板重建。
- common 与 client 注册入口拆开，保证专用服务器不类加载客户端类。

验收：`compileJava`、`build`、客户端创造模式中检查注册项、专用服务器启动。

实施状态（2026-08-24）：**已完成**。

- 已删除阶段 0 的临时 bootstrap 入口，并将真实 `StarboundMC.java` 及 7 个注册类、1 个世界生成注册壳接回 `src/main/java`；其余尚未迁移的 Forge 实现继续隔离，不参与本阶段编译。
- 模组入口改用 NeoForge 注入的 `IEventBus` 和 `ModContainer`，注册表改用 NeoForge `DeferredRegister`/`DeferredHolder`/`DeferredBlock`/`DeferredItem`，世界生成 Codec 通过 NeoForge `RegisterEvent` 注册。
- 保留 13 个方块、4 个方块实体、23 个物品、1 个创造模式页签、1 个实体、6 个菜单、4 个音效及 4 个世界生成 Codec 的已发布 ID；新增注册 ID 契约测试防止后续阶段意外改名。
- 本阶段使用明确命名的最小方块实体、实体和菜单壳，以及临时 passthrough 世界生成 Codec。原复杂实现未删除，将分别在阶段 2、5 和 8 恢复。
- `gradlew.bat compileJava`、`gradlew.bat test build` 均成功；客户端完成模组加载与资源重载，所有注册方块/物品进入模型解析流程。当前缺失模型和音效的警告属于阶段 10 的资源迁移范围。
- 专用服务器使用 Java 21 成功加载注册层并达到 `Done`，未发生客户端类加载错误；烟测结束时按 PID 停止服务器，因此该次 `runServer` Gradle 任务本身以非零退出。

### 阶段 2：基础方块、物品、实体和菜单

目标：恢复不依赖复杂网络/世界生成的基础对象。

迁移顺序：

1. 普通材料和矿石物品。
2. 普通方块和 BlockItem。
3. `SeatEntity`。
4. 菜单类型和基础 Screen 注册。
5. 方块实体类型，暂时只保证创建和保存壳层。

注意 Minecraft 构造器、`Item.Properties`、`BlockBehaviour.Properties`、实体同步数据和菜单接口的签名变化。

验收：所有注册对象可通过命令获得/放置；客户端和专用服务器均无类加载错误。

实施状态（2026-08-24）：**已完成**。

- 普通材料、矿石和全部 BlockItem 保持已发布 ID；真实 `SeatEntity`、`CaptainChairBlock`、`ShipEngineBlock` 已按 1.21.1 的 Codec、同步数据 Builder 和乘客位置接口迁移。
- 新增阶段 2 方块适配层，为工作台、传送器、飞船控制台、货箱、舱门、燃料控制器和合金炉恢复朝向、菜单入口及方块实体创建，同时保留旁边的旧 Forge 完整实现，等待其网络、持久化和世界逻辑阶段接替，未删除旧玩法源码。
- `ShipConsoleMenu`、`ShipCrateMenu` 和 `TeleporterMenu` 已接回真实容器实现；升级、合金炉和燃料菜单保留明确命名的阶段 2 壳，分别等待 Data Components、方块实体和跃迁系统迁移。
- 客户端通过 `Dist.CLIENT` 边界注册 6 个基础 Screen 和不可见座椅渲染器；专用服务器未扫描或加载这些客户端类。
- 4 个方块实体均可随方块创建并写出稳定类型 ID；货箱壳实现 54 格 `Container`、HolderLookup 感知的物品读写和菜单移动逻辑，后续阶段 5 再接回完整方块实体实现。
- 新增阶段 2 接线契约测试和可重复使用的 RCON 烟测脚本。`gradlew.bat test build` 成功；专服用命令验证 13 个方块可放置、23 个物品可写入容器、4 个方块实体 ID 正确、`starboundmc:seat` 可生成，清理测试块后正常保存并以 0 退出。
- 客户端完成模组加载与资源重载，Screen/Renderer 订阅无异常；验证后按 PID 停止，因此 `runClient` 任务本身以非零退出。缺失模型和音效仍属于阶段 10。

### 阶段 3：网络系统整体重写

目标：替换 Forge `SimpleChannel`，恢复全部客户端/服务端通信。

旧系统位置：`network/ModNetwork.java`，协议版本 `4`，共有 12 种包：

1. `UpgradeMatterManipulatorPacket` C→S
2. `StartWarpPacket` C→S
3. `SyncStarStatePacket` S→C
4. `SyncPlanetPacket` S→C
5. `WarpStartPacket` S→C
6. `SyncFuelPacket` S→C
7. `TeleporterListPacket` S→C
8. `TeleporterUsePacket` C→S
9. `TeleporterRenamePacket` C→S
10. `TeleportToShipPacket` C→S
11. `AddFuelPacket` C→S
12. `SyncFlightPacket` S→C

1.21.1 目标设计：

- 每个消息实现 `CustomPacketPayload`。
- 使用唯一 Payload Type 和 `StreamCodec`。
- 通过 `RegisterPayloadHandlersEvent` 按方向注册 Payload。
- 客户端 Payload handler 必须放在安全的 client-only 类边界中，具体注册方式以 1.21.1 API 为准。
- 使用新的 `PacketDistributor` / `ClientPacketDistributor` 发送。
- 建立新的网络版本，例如从 `1` 开始；记录与 Forge 协议 4 不兼容。
- 所有 C→S 请求继续在服务端检查玩家、菜单、维度、目标、燃料和权限，绝不能信任客户端字段。

优先让单个最小包贯通，再逐包迁移；不要一次性重写 12 个后才测试。

验收：为每个 StreamCodec 添加往返测试；实机验证登录同步、燃料、跃迁、传送和升级请求。

实施状态（2026-08-24）：**协议迁移已完成**。

- 旧 Forge `SimpleChannel` 协议 4 已替换为 NeoForge play Payload 协议 1；12 个消息均实现 `CustomPacketPayload`，拥有唯一的 `starboundmc:*` Type、有界 `StreamCodec` 和显式 C→S/S→C 注册方向。
- 新协议与 Forge 协议 4 明确不兼容。字符串、列表、传送目标类型、燃料快照、飞行阶段和有限数值均在构造/解码边界校验，避免客户端构造无界集合或非法请求字段。
- 客户端同步处理集中在独立 `ClientPayloadHandler` 和 `ClientNetworkState` 边界；专服成功加载同一注册表，不会执行客户端效果。传送器列表已接回其客户端状态镜像。
- 服务端请求统一由主线程 `ServerPayloadHandler` 处理，先验证 `ServerPlayer`、菜单类型、方块位置、目标格式和请求范围，再进入 `ServerPayloadActions` 权威业务端口。后续组件、持久化、跃迁与传送阶段安装真实业务动作时，仍须在变更世界前复核维度、燃料、目标和权限。
- 全部旧 `ModNetwork.CHANNEL`、Forge `PacketDistributor` 和 `net.minecraftforge.network` 调用点已迁移；发送统一使用 NeoForge 21.1.248 实际提供的 `PacketDistributor.sendToServer`、`sendToPlayer` 和 `sendToPlayersInDimension`。
- 新增 12/12 Payload 字节往返测试、Type 唯一性测试和方向/侧别/旧 API 契约测试。`gradlew.bat test` 通过；客户端完成资源重载，专服启动至 `Done`，两侧均无 Payload 重复注册、方向冲突或类加载异常。
- 登录同步、燃料消耗、跃迁、传送和升级的完整实机行为需要阶段 4–7 的真实业务实现接入 `ServerPayloadActions` 后复测；本阶段不以信任客户端或直接调用尚未迁移旧实现的方式伪造通过。

### 阶段 4：物质枪 NBT → Data Components

目标：恢复物质枪升级、附魔和激光采集。

旧 `MatterManipulatorItem` 直接读写 ItemStack NBT：

- `SpeedUpgrades`
- `RangeUpgrades`
- `MiningUpgrades`
- `FortuneUpgrades`
- 旧字段 `Upgrades`
- 旧 `Enchantments` 列表

1.21.1 应创建一个不可变记录，例如：

```java
record MatterManipulatorUpgrades(int speed, int range, int mining, int fortune) {}
```

并注册自定义 `DataComponentType<MatterManipulatorUpgrades>`，提供持久化 Codec 与网络 StreamCodec。

要求：

- 组件值不可变，修改时创建新值并重新 `stack.set(...)`。
- 保持四轨上限和升级成本不变。
- 使用现代附魔/组件 API 恢复真实时运效果。
- 设计一次性旧 NBT 读取/迁移路径；迁移成功后避免重复写旧字段。
- 为组件 Codec、边界钳制和旧字段迁移添加单元测试。

验收：四轨升级、掉落等级、射程、采集速度、重进世界和网络同步一致。

实施状态（2026-08-24）：**Data Components 迁移已完成**。

- 新增不可变 `MatterManipulatorUpgrades` 四轨记录以及 `starboundmc:matter_manipulator_upgrades` 自定义 Data Component；持久化 `Codec` 和网络 `StreamCodec` 均在构造边界钳制速度、射程、采矿等级和时运等级。
- `MatterManipulatorItem` 已停止直接读写 ItemStack 旧 NBT。首次访问时会从 `minecraft:custom_data` 读取 `SpeedUpgrades`、`RangeUpgrades`、`MiningUpgrades`、`FortuneUpgrades`、`Upgrades` 和残留 `Enchantments`，写入新组件并删除已消费的旧字段，同时保留无关自定义数据。
- 真实物质枪、升级模块、`UpgradeMenu` 和 `UpgradeScreen` 已恢复注册；升级仍保留四轨原上限与达到 1/2/3 级分别消耗 1/2/4 个模块的成本，并通过阶段 3 的服务端权威菜单边界处理请求。
- 时运升级使用 1.21.1 的附魔注册表 Holder 与 `EnchantmentHelper.updateEnchantments` 同步真实 Fortune 附魔；激光采集继续把物质枪作为掉落工具，射程、采集速度和采矿等级均从新组件读取。
- 新增组件持久化/网络往返、边界钳制、旧字段一次性迁移和接线契约测试。当前 32 项测试全部通过，完整 `test build` 成功；客户端完成资源重载，专服启动至 `Done`，两侧均无组件、菜单、Screen、事件或物品类加载异常。
- 自动验证覆盖了序列化、迁移和注册启动边界；真实玩家的升级点击、带时运掉落、保存退出后重进世界仍保留到最终整合实机回归中复测。

### 阶段 5：方块实体与持久化状态

目标：恢复容器、燃料、飞行和传送器数据的保存/读取。

重点类：

- `AlloyFurnaceBlockEntity`
- `FuelControllerBlockEntity`
- `ShipCrateBlockEntity`
- `ShipDoorBlockEntity`
- `ShipStateData`
- `TeleporterManager`
- `ShipTemplatePlacer.ShipTemplateData`
- `SeatEntity`

要求：

- 按 1.21.1 的注册表上下文和序列化接口更新方法签名。
- `SavedData` 使用当前 Factory/Codec 或官方推荐模式。
- 保留旧关键字段读取，缺失/无效字段使用安全默认值。
- 飞行快照必须原子保存：目标、elapsed/total tick、阶段、分区坐标、速度、yaw/pitch/roll。
- 容器修改继续调用正确的 changed/broadcast API。

验收：保存退出、重进、服务器重启、跃迁中断恢复、容器持久化和传送器名称持久化。

实施状态（2026-08-24）：**持久化层迁移已完成**。

- `ship_crate`、`ship_door`、`titanium_alloy_furnace` 和 `fuel_controller` 已从阶段 2 保存壳替换为真实方块实体类型；箱子实现标准 `Container`，箱子和合金炉接回容器脏标记，门与合金炉接回服务端 ticker。
- 箱子、燃料槽和合金炉库存全部使用带 `HolderLookup.Provider` 的 1.21.1 序列化接口；旧 `Items`、`FuelItems`、燃烧时间和烹饪进度键继续读取，无效负进度回落到安全范围。合金炉燃料时长改用 NeoForge ItemStack 扩展，不再依赖 Forge Hooks。
- `ShipStateData`、`TeleporterManager` 和 `ShipTemplatePlacer.ShipTemplateData` 已迁移到 `SavedData.Factory` 与注册表上下文保存接口。燃料、访问记录、传送器名称、飞行计时、阶段、分区坐标、速度和姿态均有缺失/损坏值边界；完整飞行快照仍由一次 `setFlight` 调用原子更新。
- `SeatEntity` 没有额外持久字段，保留 1.21.1 的空 `readAdditionalSaveData` / `addAdditionalSaveData` 实现；离座或座椅方块消失时仍由服务端清理实体。
- 新增完整飞行快照往返、损坏字段安全默认、传送器名称约束、模板放置标记、真实方块实体接线和现代签名测试。当前 40 项测试全部通过，专服启动至 `Done`。
- 自动专服保存—重启烟测确认：`ShipCrateBlockEntity` 中 5 个钻石与 `FuelControllerBlockEntity` 中 7 个煤均在 `save-all flush`、正常停服和重启后按原数量读回；烟测方块已清理。
- 跃迁中断的真实控制器恢复、传送器命名/失效清理和燃料菜单操作分别依赖阶段 6–7 的业务接线；本阶段已验证它们所依赖的持久化格式和损坏存档回退，不提前宣称完整玩法通过。

### 阶段 6：玩法事件、菜单与传送器

目标：恢复首次登录到行星探索前的完整服务端玩法。

重点：

- 首次礼包与首次回船。
- 无床死亡后回船。
- H 键回船请求。
- 船内菜单、安全打开容器。
- 燃料槽消耗和油箱钳制。
- 已命名传送器注册、校验、重命名、失效清理和跨维度传送。
- 原版下界传送门全局禁用规则。

验收：客户端和专用服务器分别测试，特别检查所有网络请求的服务端二次验证。

实施状态（2026-08-24）：**玩法事件、菜单与传送器迁移已完成**。

- 首次登录礼包、`starboundmc.starter_given` 标记、死亡克隆继承、首次回船、无床重生回船和 H 键请求均迁移到 NeoForge 事件；飞船维度尚未由阶段 8 注册时，阶段 6 旅行边界会安全回退到主世界出生点，不会把玩家传送到不存在的维度。
- 燃料控制器和钛制合金炉已从阶段 2 空菜单替换为真实菜单及 Screen，箱子、传送器、燃料和炉子均通过 `ContainerLevelAccess`、真实方块实体和 8 格距离规则校验。共享油箱直接由 `ShipStateData` 管理，单次补给只消费能完整放入油箱的物品，燃料始终钳制在 `0..1000`；20/100 的跃迁成本常量暂存于阶段 6 服务，阶段 7 接入真实跃迁控制器时复用。
- 新增 `Stage6ServerPayloadActions`，保留物质枪升级并为回船、燃料、传送器使用和重命名提供真实业务动作。网络 handler 和动作层都会复核菜单、距离、来源坐标、实际方块、旁观状态、目标格式及命名表授权。
- 传送器打开和重命名后会发送有界目的地列表；名称统一为 64 字符。命名表会限制总数、清除失效方块，并在传送前要求目标 key 已存在于命名表，修复了“猜到坐标即可传送到未命名传送器”的权限漏洞；有效目标继续支持跨维度传送。
- 原版下界传送门生成和前往 `minecraft:the_nether` 的实体旅行已通过 NeoForge 可取消事件全局阻止。新增可复用的 `scripts/stage6-rcon-smoke.ps1`，自动验证合金炉 ticker 与下界门禁用并清理测试区块。
- 当前 47 项测试全部通过，完整 `test build` 成功；专服启动至 `Done`，事件订阅边界无客户端类加载；RCON 烟测确认合金炉在 200 tick 后产出耐钢锭且完整黑曜石框架无法点亮下界门。客户端完成模组加载与资源重载，真实 Screen、H 键和客户端 Tick 注册成功，随后由烟测按 PID 结束，因此该次 `runClient` 任务本身以非零退出。
- 真实飞船/行星维度安全落点由阶段 8 激活；真实玩家点击菜单、首次登录礼包和 H 键的交互手感保留到最终整合实机回归，本阶段已自动验证其服务端权限、持久化、接线与启动边界。

### 阶段 7：连续宇宙与跃迁

目标：恢复服务器权威飞行，不先恢复华丽渲染。

纯逻辑优先迁移：

- `space/SectorCoordinate`
- `space/UniversePosition`
- `space/UniverseDelta`
- `warp/FlightPhase`
- `warp/UniverseRouteFrame`
- `warp/ShipFlightController`
- `warp/ShipSpace`
- `warp/ShipWarpManager`

保持阶段：

```text
DOCKED → TURN → ACCELERATE → CRUISE/HYPERSPACE → DECELERATE → ARRIVE → DOCKED
```

保持确定性航线、安全半径绕行、燃料成本、时长范围和每 5 tick/阶段变化同步策略。

验收：先运行已有纯逻辑测试，再在无复杂天空效果的情况下验证同星系、跨星系、退出重进和服务器重启恢复。

### 阶段 8：飞船与行星维度、世界生成

目标：恢复全部自定义维度和安全落点。

迁移顺序：

1. 飞船虚空维度。
2. 程序化飞船放置。
3. 自定义 `ship.nbt` 覆盖机制。
4. 冰冻维度和 BiomeSource。
5. 荒芜维度和去水后处理。
6. 熔岩维度和自定义地形。

重点类：

- `ShipDimensions`
- `ShipChunkGenerator`
- `ShipStructure`
- `StarterShipHullProfile`
- `ShipTemplatePlacer`
- `FilteredBiomeSource`
- `BarrenChunkGenerator`
- `MoltenChunkGenerator`

Codec、MapCodec、BiomeSource、NoiseGeneratorSettings、ChunkGenerator 方法签名和数据包 JSON 必须以 1.21.1 为准重新适配。

验收：每个维度使用新世界独立测试；检查安全落点、区块重建、门和方块实体、无天花板熔岩地形、冰冻群系过滤、荒芜去水。

### 阶段 9：客户端空间渲染和星图 UI

目标：在 common/server 已稳定后恢复视觉表现。

顺序：

1. Screen 和键位注册。
2. 星图纯布局、几何和点击测试。
3. 星图控件、详情和天体贴图。
4. 激光渲染。
5. 飞船维度天空和行星渲染。
6. 恒星 LOD、星点批处理和环境混合。
7. 跃迁隧道、闪光、声音与阶段过渡。
8. 主世界熔岩卫星替代显示。

所有 RenderSystem、BufferBuilder、RenderType、PoseStack、shader、纹理和 dimension effects API 都需按 1.21.1 重查。禁止通过把客户端类移入 common 路径来绕过编译问题。

验收：GUI Scale 1/2/3/4/Auto、不同宽高比、同/跨星系航线、进入中性深空、重进世界、专用服务器启动。

### 阶段 10：资源、datagen 和本地化

目标：所有资源在 1.21.1 无缺失、无解析警告。

- 从旧项目复制 113 个 assets 和 24 个 data 资源作为迁移输入。
- 使用 1.21.1 目录和 JSON schema 调整。
- 重建 `neoforge.mods.toml`、`pack.mcmeta` 和依赖声明。
- 修正物品模型定义、配方、战利品表、维度和噪声设置。
- 迁移 datagen 后重新生成 `src/generated/resources`。
- 检查 `en_us.json`、`zh_cn.json` 和所有新错误/网络提示的翻译键。
- 保留现有声音和行星贴图许可说明。

验收：构建日志中无 missing model、missing texture、unknown registry、data pack validation 错误。

### 阶段 11：测试、存档迁移与发布准备

自动验证最低要求：

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runServer
git diff --check
```

实机验收矩阵：

- 新世界首次登录和首次礼包。
- 程序化飞船、所有设备和安全落点。
- 燃料添加、保存、重进。
- 同星系与跨星系跃迁。
- 跃迁中退出重进、服务器重启。
- 四个天体的传送和返回。
- 三个独立行星维度。
- 传送器命名、拆除和失效清理。
- 物质枪四轨升级和采集掉落。
- 所有星图页面和 GUI Scale。
- 单人、局域网和专用服务器的 common/client 边界。

仅在新世界完全稳定后，复制旧存档的备份进行兼容测试。发现不兼容时编写显式迁移逻辑或声明不支持，不得在唯一存档上试错。

## 10. 推荐提交策略

每个阶段单独提交，示例：

```text
chore: bootstrap NeoForge 1.21.1 workspace
refactor: migrate registries to NeoForge
refactor: replace SimpleChannel with payload networking
refactor: store matter manipulator upgrades in data components
refactor: migrate persistent ship and teleporter data
feat: restore server-authoritative warp flow
feat: restore custom dimensions and world generation
feat: restore starmap and space rendering
chore: migrate resources and data generation
test: complete NeoForge migration verification
```

每次提交前记录运行过的验证。不要将“全仓包名替换 + 网络 + 世界生成 + 渲染”压在一个不可审查提交中。

## 11. 新任务的工作方式

新 Codex 任务应遵循：

1. 先读取本文件，再读取新仓库的 `AGENTS.md`（如存在）。
2. 检查当前 Git 状态，保留用户已有改动。
3. 检查目标项目实际 Minecraft/NeoForge/Java/Gradle 版本，不凭文档假定骨架已经创建。
4. 需要原行为时读取旧 Forge 项目，不修改旧项目。
5. 每次只声明并完成一个迁移阶段或明确子阶段。
6. 修改后执行与风险相称的测试，并明确区分“新运行”与“旧报告”。
7. 遇到 API 不确定时优先查询 NeoForge 1.21.1 官方文档和从 1.20.x 到 1.21.1 的逐版本 Primer；不要套用 1.21.4+ 或 26.1 示例。
8. 不为了消除编译错误删除玩法、弱化服务端校验、屏蔽专用服务器类加载问题或改注册 ID。

## 12. 新对话建议首条消息

```text
请完整阅读仓库根目录的 NEOFORGE_MIGRATION_PLAN.md，并把它作为本次迁移的目标与约束来源。

旧 Forge 项目是只读参考：
E:\Develop\doing\StarboundMC A1

新 NeoForge 项目是唯一写入目标：
E:\Develop\doing\StarboundMC Neoforge

固定迁移目标是 Minecraft 1.21.1、最新稳定 NeoForge 21.1.x、JDK 21 和 ModDevGradle。选择 1.21.1 是为了更完善的模组生态和更大的玩家群体，未经用户批准不得升级目标版本。迁移前行为与设计资料位于 docs/legacy-forge/，请按计划中的权威级别使用。

先检查新项目实际状态、Git 状态、AGENTS.md 和构建骨架。不要立即全仓替换 API。告诉我当前处于计划的哪个阶段、最小可验证下一步是什么，然后从阶段 0 开始执行。每完成一个阶段都运行对应测试并更新计划文档中的状态。
```

## 13. 状态清单

- [x] 旧项目完成结构和实现审计。
- [x] 137 个主 Java 文件复制到新目录。
- [x] 27 个测试文件复制到新目录。
- [x] 164 个文件 SHA-256 校验一致。
- [x] 7 份旧项目 Markdown 文档迁移为 `docs/legacy-forge/` 快照。
- [x] 初始化新 Git 仓库。
- [x] 生成并合并 Minecraft 1.21.1 / NeoForge 21.1.x ModDevGradle 骨架。
- [x] 建立空模组客户端、服务端和构建基线。
- [x] 完成入口、注册和事件迁移。
- [x] 完成基础对象和菜单迁移。
- [x] 完成 Payload 网络迁移。
- [x] 完成物质枪 Data Components 迁移。
- [x] 完成持久化和方块实体迁移。
- [x] 完成玩法事件与传送器迁移。
- [ ] 完成连续宇宙和跃迁迁移。
- [ ] 完成维度与世界生成迁移。
- [ ] 完成客户端渲染和星图迁移。
- [ ] 完成资源和 datagen 迁移。
- [ ] 完成新世界、旧存档备份和专用服务器验收。

## 14. 官方参考

- NeoForge 1.21.1 入门：https://docs.neoforged.net/docs/1.21.1/gettingstarted/
- NeoForge Mod Generator：https://neoforged.net/mod-generator/
- Mod 文件与 `neoforge.mods.toml`：https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles
- Payload 网络：https://docs.neoforged.net/docs/1.21.1/networking/payload/
- Data Components：https://docs.neoforged.net/docs/1.21.1/items/datacomponents/
- 跨版本 Primers：https://docs.neoforged.net/primer/docs/

本文描述的是迁移目标、约束和执行顺序；当具体 API 示例与 NeoForge 1.21.1 官方文档冲突时，以 1.21.1 文档、对应版本源码和实际编译结果为准，同时把差异记录回本文。不得因为最新文档默认显示 26.1，就在未告知用户的情况下使用 26.1 API 或升级目标版本。
