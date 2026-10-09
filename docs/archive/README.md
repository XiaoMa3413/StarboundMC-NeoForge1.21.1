# 历史归档

**Historical — not current requirements.** 这里保存提案、施工过程、验收与迁移记录，可能引用已删除 API、旧 UI、内部旧 schema 和已结束的 migration assumptions。它们不自动构成 implementation contract；普通 Agent 只在明确调查历史时阅读。

当前代码、测试和运行证据优先。稳定规则从[文档入口](../README.md)进入，剩余工作集中在 [current-work](../current-work.md)，不从历史“待办”自动提取新需求。

## 已消耗的专题计划

下列计划主体已实现，主要剩人工/联机验收或独立后续功能，因此完整历史归档；稳定规则与剩余工作分别提炼，而非丢弃。

| 历史计划 | 当前 reference | 剩余事项归入 current-work |
| --- | --- | --- |
| [EPP](epp-implementation-plan.md) | [EPP / EVA](../reference/epp.md) | 生存/联机验收、模块弹窗、创造推进器原型与未来装备 |
| [AI 终端](ship-ai-terminal-plan.md) | [N.O.V.A.](../reference/nova.md) | 完整序章、终端恢复配方、设备灯光与后续引擎 |
| [亚光速维修](sublight-engine-repair-plan.md) | [N.O.V.A.](../reference/nova.md) | 单槽/并发、正常重启与完整人工航行闭环 |
| [体素打印站](voxel-printing-station-plan.md) | [制造](../reference/manufacturing.md) | 队列/退款/输出阻塞/拆机与多人验收 |
| [机器 LDLib2](machine-ui-ldlib2-plan.md) | [制造 / 菜单](../reference/manufacturing.md) | 当前机器逐页人工验收 |
| [HUD / AR](hud-ar-refactor-plan.md) | [HUD](../reference/hud.md) | 真实联机/长期体验；独立任务 provider 与月面/EVA 联动 |

## 设计与验证历史

- [宇宙系统原设计](StarboundMC%20宇宙系统总设计.md)，当前规则见[宇宙与导航](../reference/universe.md)。
- [HUD H0 基线](hud-ar-h0-baseline.md)、[曲线设计](hud-local-curves-design.md)、[动画施工](hud-animation-language.md)、[动画交付记录](hud-animation-validation.md)。当前规则见 HUD reference，命令见[开发验证](../development/hud-testing.md)。
- [Forge → NeoForge](neoforge-migration-plan.md)与[宇宙数据驱动迁移](StarboundMC%20宇宙数据驱动技术迁移计划.md)。
- [航天飞机落地](shuttle-ship-import-plan.md)、[NOVA / 指挥设备美化](command-deck-art-pass.md)。
- [星图需求](starmap-redraw-requirements.md)、[优化](starmap-redraw-optimization.md)、[完成清单](starmap-redraw-todo.md)。
- [飞船太空视觉基线](ship-space-visual-baseline.md)、[旧渲染架构提案](space-render-architecture-proposal.md)、[视觉增强计划](ship-space-visual-enhancement-plan.md)、[渲染路线/施工](space-render-roadmap.md)、[检查点验证历史](space-render-validation-history.md)。当前合同见[渲染管线](../space-render-pipeline.md)。

历史文件可保留当时的 commit、测试数量与 benchmark；这些记录只说明对应版本和环境，不能冒充本次验证。
