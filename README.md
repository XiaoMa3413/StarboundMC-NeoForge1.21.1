# StarboundMC NeoForge 1.21.1

StarboundMC 从 Forge 1.20.1 迁移到 NeoForge 1.21.1 的工作仓库。迁移目标、兼容约束和阶段顺序以 [`NEOFORGE_MIGRATION_PLAN.md`](NEOFORGE_MIGRATION_PLAN.md) 为准。

## 当前基线

- Minecraft `1.21.1`
- NeoForge `21.1.248`
- ModDevGradle `2.0.144`
- Gradle `9.2.1`
- Java toolchain `21`
- modId `starboundmc`
- 版本 `0.1-alpha`

阶段 0 的最小 NeoForge 入口已完成构建和运行基线验证。阶段 1 已将真实入口、注册表和事件总线接回 `src/main/java`；阶段 2 已恢复基础方块、物品、SeatEntity、菜单与客户端 Screen 注册；阶段 3 已将 12 个旧 Forge 网络消息迁移到 NeoForge Payload 协议；阶段 4 已用自定义 Data Component 恢复物质枪四轨升级、真实时运附魔和激光采集；阶段 5 已恢复真实方块实体以及飞船、传送器和模板状态的 1.21.1 持久化接口；阶段 6 已恢复首次礼包/回船、H 键、真实设备菜单、燃料消费、命名传送器和下界门禁用规则，并在所有 C→S 操作的业务层再次验证世界状态；阶段 7 已恢复连续宇宙坐标、确定性安全航线、持久化飞行状态和服务器权威跃迁；阶段 8 已恢复真实飞船、冰冻、荒芜、熔岩维度及其世界生成和安全落点。全部已发布注册 ID 保持不变，复杂玩法实现继续按迁移计划隔离并逐步恢复。

当前进度：阶段 0–8 已完成，下一步为阶段 9（客户端空间渲染和星图 UI）。回船、行星旅行和跃迁现已使用真实维度；翠绿天体仍按玩法基线使用主世界表面。

## 构建与运行

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

阶段 2 的专服注册对象命令烟测可在本地临时启用 RCON 后运行：

```powershell
.\scripts\stage2-rcon-smoke.ps1
```

阶段 6 的合金炉 ticker 与下界门禁用烟测：

```powershell
.\scripts\stage6-rcon-smoke.ps1
```

阶段 8 的四维度生成、程序化飞船和熔岩开放天空烟测：

```powershell
.\scripts\stage8-rcon-smoke.ps1
```

Wrapper 默认将 Gradle、Minecraft、NeoForge 依赖和运行资产缓存到仓库内的 `.gradle-home/`，避免占用系统盘；该目录不会提交。若确有需要，可在运行前显式设置 `GRADLE_USER_HOME` 覆盖此行为。

旧 Forge 项目只作为行为基线和只读参考，历史文档快照位于 `docs/legacy-forge/`。
