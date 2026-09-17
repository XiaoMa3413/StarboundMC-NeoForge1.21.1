# StarboundMC 文档索引

本目录按文档用途分为活动计划、架构参考、已知问题和历史归档。发生冲突时，以当前代码、测试和
实际运行结果为准。未确认的玩法构想单独放在仓库根目录的 [`.doc`](../.doc/) 中，不得直接作为
开发指令。

本次整理基线：`codex/nova-prologue` / `e608025`（2026-09-17）。

许可和第三方素材的当前说明位于仓库根目录的 [LICENSE](../LICENSE) 与
[THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)。归档文档中的历史资源清单不代表当前发布包
的资产或许可证清单。

## 当前活动计划

- [亚光速引擎维修闭环](sublight-engine-repair-plan.md)：核心打印、独立飞船引擎模块槽安装、服务端
  点火计时和同星系航行门禁已经接入；创造模式与当前界面已确认，完整多人、重启和旧存档矩阵仍需
  按计划补验。
- [舰载人工智能终端与序章剧情计划](ship-ai-terminal-plan.md)：序章、远程通信 HUD、四页任务终端、
  任务详情展开和 NOVA 左侧布局已经接入；终端重制配方、环境灯光/屏幕联动和整体验收仍在后续范围。
- [飞船维度太空视觉增强计划](ship-space-visual-enhancement-plan.md)：星系尺度、跨星系航行节奏、
  行星过渡和气态巨行星光环航线已完成当前阶段；S0 深空背景层次及后续视觉阶段尚未开始。
- [体素提炼与打印合成站计划](voxel-printing-station-plan.md)：M0–M5 已完成，M6 批处理与打印队列
  代码已落地；客户端视觉/交互验收记录仍需补齐。
- [普通机器界面 LDLib2 迁移计划](machine-ui-ldlib2-plan.md)：传送器、货箱、燃料控制器和物质枪升级
  工作台已有 LDLib2 实现；合金炉、旧控制台和最终清理仍未完成。

## 架构参考

- [宇宙系统总设计](StarboundMC%20宇宙系统总设计.md)：当前数据驱动宇宙架构的说明，不是施工清单。

## 已知问题

- [已发现问题](known-issues.md)：记录仍需实机复验的 LDLib2 冷启动、宇宙迁移视觉验收，以及 Rocky
  Moon 地表抵达任务触发缺口。

## 已完成计划归档

- [归档索引](archive/README.md)：归档规则和已结束计划列表。
- [Forge → NeoForge 迁移计划](archive/neoforge-migration-plan.md)
- [宇宙数据驱动技术迁移计划](archive/StarboundMC%20宇宙数据驱动技术迁移计划.md)
- [航天飞机外形与内饰落地](archive/shuttle-ship-import-plan.md)
- [NOVA 与指挥设备美化](archive/command-deck-art-pass.md)
- [星图重绘需求](archive/starmap-redraw-requirements.md)
- [星图重绘优化方案](archive/starmap-redraw-optimization.md)
- [星图重绘完成清单](archive/starmap-redraw-todo.md)
- [飞船维度太空渲染基线](archive/ship-space-visual-baseline.md)

归档文档只保留历史决策、实施记录和验证证据，不再追加新任务。出现后续需求时，新建活动计划并
链接相关归档。

## 历史行为基线

- [Forge 1.20.1 文档快照](legacy-forge/README.md)：只用于核对迁移前行为和设计理由，不得作为
  NeoForge 1.21.1 API 指南，也不应反向修改。

## 维护规则

- 活动计划只保留未完成或正在验收的工作；完成后记录提交、测试和视觉核验，再移入 `archive/`。
- `.doc/` 只保存明确标注为未确认的构想和讨论稿；确认后另建活动计划，不直接把构想当作需求。
- 不把 `.agents/`、本地 `LDLib2/` 源码、截图、运行日志或缓存写入项目文档清单。
- 测试数量以最近一次完整执行为准；如果源集变化，应同步更新 README 和相应活动计划。
