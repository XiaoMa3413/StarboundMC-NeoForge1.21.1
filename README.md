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

阶段 0 使用 `src/bootstrap/java` 中的最小 NeoForge 入口验证构建和运行。尚未迁移的 Forge 主源码与测试仍分别保存在 `src/main/java` 和 `src/test/java`，阶段 1 起按纵向切片逐步接回构建。

## 构建与运行

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

Wrapper 默认将 Gradle、Minecraft、NeoForge 依赖和运行资产缓存到仓库内的 `.gradle-home/`，避免占用系统盘；该目录不会提交。若确有需要，可在运行前显式设置 `GRADLE_USER_HOME` 覆盖此行为。

旧 Forge 项目只作为行为基线和只读参考，历史文档快照位于 `docs/legacy-forge/`。
