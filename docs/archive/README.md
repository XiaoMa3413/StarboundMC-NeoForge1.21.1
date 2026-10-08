# 已完成与历史归档

这里保存已经结束的实施计划、视觉验收记录和迁移基线。归档文件用于追溯当时的决策、代码边界和
验证证据，不是当前开发指令；后续需求应新建活动计划。

## 已完成实施记录

- [Forge → NeoForge 迁移计划](neoforge-migration-plan.md)：迁移阶段、兼容处理和旧 Forge 文档快照。
- [宇宙数据驱动技术迁移计划](StarboundMC%20宇宙数据驱动技术迁移计划.md)：datapack、UniverseCatalog、
  星图、航行和旧存档迁移的落地记录。
- [航天飞机外形与内饰落地](shuttle-ship-import-plan.md)：用户确认的默认初始飞船结构和开局补给。
- [NOVA 与指挥设备美化](command-deck-art-pass.md)：NOVA、AI 终端和星图导航台的模型、贴图与 UV 记录。

## 更早的历史基线

- [星图重绘需求](starmap-redraw-requirements.md)
- [星图重绘优化方案](starmap-redraw-optimization.md)
- [星图重绘完成清单](starmap-redraw-todo.md)
- [飞船维度太空渲染基线](ship-space-visual-baseline.md)

归档中的“待讨论”“未来考虑”和旧测试数量只描述当时的上下文。当前实现以根目录 README、活动计划
和代码为准。

## PR #20 前的太空渲染提案与记录

- [迁移前架构提案](space-render-architecture-proposal.md)：基于 `2ff090d`，已被当前实现替代。
- [旧飞船视觉增强计划](ship-space-visual-enhancement-plan.md)。
- [渲染路线与施工日志](space-render-roadmap.md)。
- [渲染检查点验证历史](space-render-validation-history.md)。

这些文档中的相对链接、分支状态、测试数量和待办只描述历史上下文。普通 Agent 不从这里提取需求；当前入口为 [文档索引](../README.md)、[架构](../architecture.md)和[当前工作](../current-work.md)。
