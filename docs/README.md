# StarboundMC 文档入口

当前基线：PR #20 合并后的 `main` / `21c4c349664351966aaf0c3d8a4c6f53eec1339a`，整理日期 2026-10-09。Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21。

## 权威与阅读顺序

发生冲突时，以当前代码、测试与实际运行证据为准。普通开发依次阅读：

1. 本页。
2. [当前架构](architecture.md)：稳定 authority、存储、协议与 failure policy。
3. 任务对应的 current reference。
4. [当前工作](current-work.md)：真正未完成事项。
5. [已知问题](known-issues.md)：已复现缺口与验证边界。

`archive/` 与 `legacy-forge/` 默认不作为当前需求来源；仅在明确调查历史设计原因时读取。根目录 `.doc/` 是未确认构想，不直接授权开发。

## Current reference

- [太空渲染管线](space-render-pipeline.md)与[美术方向](space-render-art-direction.md)：正式 isolated 管线、最小能力降级、资源生命周期与运行验证。
- [宇宙系统设计](StarboundMC%20宇宙系统总设计.md)：数据模型与玩法边界；authority 以 architecture 为准。
- [UI / 美术规范](ui-art-direction.md)、[制造终端风格](ui-fabrication-style.md)、[机器 UI 合同](ui-design-contracts/ship-machines-polish.md)、[UI 工作流](ui-refactor-workflow.md)。
- 现有专题计划：[EPP](epp-implementation-plan.md)、[亚光速引擎](sublight-engine-repair-plan.md)、[AI 终端](ship-ai-terminal-plan.md)、[打印站](voxel-printing-station-plan.md)、[机器界面](machine-ui-ldlib2-plan.md)。只按其中当前未完成事项继续，已结束阶段不是迁移兼容义务。

## 历史与维护

[归档索引](archive/README.md)保存迁移提案、已结束路线与验证日志，后续任务从 current-work 进入。当前 reference 不追加 dated 施工日记、分支检查点或过期测试数量。

许可与第三方素材以根目录 [LICENSE](../LICENSE) 和 [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) 为准。开发资产审计见 current-work；本地截图、缓存、运行日志不进入文档资产清单。
