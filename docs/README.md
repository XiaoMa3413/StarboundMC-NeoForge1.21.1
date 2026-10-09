# StarboundMC 文档入口

本文描述当前主线；发生冲突时，以当前代码、测试和实际运行证据为准。平台为 Minecraft 1.21.1 / NeoForge / Java 21。

## 默认阅读顺序

[AGENTS](../AGENTS.md) → 本页 → [当前架构](architecture.md) → 任务相关 reference → [当前工作](current-work.md) → 必要时查阅[已知问题](known-issues.md)。

- architecture 记录稳定 authority、存储、协议与 failure policy。
- reference 记录已实现的稳定规则，不追加 commit 日记或历史测试数量。
- current-work 集中未完成事项；known-issues 只记录已复现但未解决的问题。
- implementation plan 是临时施工材料，完成后提炼规则与剩余工作，再归档。

## Current reference

- [宇宙与导航](reference/universe.md)：身份、位置、航行 authority 与两套星图入口。
- [N.O.V.A. 与飞船恢复](reference/nova.md)：个人任务、共享船况、终端与亚光速维修。
- [EPP 与 EVA](reference/epp.md)：装备、生命支持、温控与 Relay 边界。
- [制造与机器界面](reference/manufacturing.md)：钱包、提炼、打印队列与菜单 authority。
- [HUD / AR](reference/hud.md)：Visor、世界标记、Boot 与通信的当前合同。
- [太空渲染管线](space-render-pipeline.md)与[美术方向](space-render-art-direction.md)：pass、坐标/颜色/深度、资源和回退。
- [UI / 美术规范](ui-art-direction.md)、[制造终端风格](ui-fabrication-style.md)、[机器 UI 合同](ui-design-contracts/ship-machines-polish.md)、[UI 工作流](ui-refactor-workflow.md)。

## Development 与历史

- [太空渲染测试](development/space-render-testing.md)：运行命令、smoke 参数、GPU 检查及证据边界。
- [HUD / AR 测试](development/hud-testing.md)：合成场景、实际世界检查与人工验收范围。
- [归档索引](archive/README.md)和 [Forge 历史](legacy-forge/README.md)只供明确的历史调查，默认不是当前需求；其中可能引用已删除 API 和已结束的迁移假设。根目录 `.doc/` 是未确认构想，不直接授权开发。
- `docs/models/` 暂存 development art assets（bbmodel、OBJ、预览、验证截图与日志），保持原位。普通 coding agent 不应为理解架构扫描这些资产；独立迁移事项见 current-work。

许可与第三方素材以 [LICENSE](../LICENSE) 和 [THIRD_PARTY_NOTICES](../THIRD_PARTY_NOTICES.md) 为准。
