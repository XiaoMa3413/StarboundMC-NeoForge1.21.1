# StarboundMC 文档索引

本目录把“当前活动计划”“已完成计划”和“旧 Forge 行为基线”分开维护。发生冲突时，以当前
代码、测试和实际运行结果为准。

许可和第三方素材的当前说明位于仓库根目录的 [LICENSE](../LICENSE) 与
[THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)。本目录下的归档文档可能记录迁移时期的
历史资源清单，不应被当作当前发布包的资产或许可证清单。

## 当前活动计划

- [飞船维度太空视觉增强计划](ship-space-visual-enhancement-plan.md)：增强普通停靠/航行时的深空
  层次，重新校准两个星系的空间距离，提升行星材质与自转、星系环境差异和亚光速视差，并为
  `FULL` LOD 恒星制作自发光 3D 球体核心；星系间距已视觉确认，亚光速节奏修复等待视觉确认。
- [普通机器界面 LDLib2 迁移计划](machine-ui-ldlib2-plan.md)：普通功能方块的界面迁移计划；
  传送器及其余普通机器 UI 的继续迭代暂缓。
- [体素提炼与打印合成站计划](voxel-printing-station-plan.md)：通用货币“体素”、体素提炼机
  与初始飞船自带的体素打印站；M0–M5 已完成，M6 批处理与打印队列代码已实现，等待客户端、
  多人专服与重启持久化验收。
- [舰载人工智能终端与序章剧情计划](ship-ai-terminal-plan.md)：当前优先计划；先实现 N.O.V.A. 终端
  方块与独立 UI 原型；P0 权威剧情、P2.1 环境门禁和 P4.1 广播/首次登陆链路已接入，飞船灯光、
  设备屏幕外观和终端内教程重看入口仍在后续阶段。

## 已知问题

- [首次打开 LDLib2 界面时短暂卡顿](known-issues.md)：已复现；传送器整表重复构建已经消除，
  目前继续评估 LDLib2 首次字体与渲染资源初始化成本。

## 已完成计划归档

- [Forge → NeoForge 迁移计划](archive/neoforge-migration-plan.md)：阶段 0–11、旧存档升级与
  跃迁修复记录。
- [星图重绘需求](archive/starmap-redraw-requirements.md)：三级星图最终产品与交互要求。
- [星图重绘优化方案](archive/starmap-redraw-optimization.md)：LDLib2 组件化与绘制重构依据。
- [星图重绘完成清单](archive/starmap-redraw-todo.md)：P0–P5 实现、测试和视觉验收记录。
- [飞船维度太空渲染基线](archive/ship-space-visual-baseline.md)：现有深空穹顶、程序化星点、行星
  球体、恒星三级 LOD、连续星系环境和跃迁演出的实现归档。

归档文档不再追加新任务。出现后续需求时，应新建活动计划并链接到相关归档，而不是重新打开
已经关闭的清单。

## 历史行为基线

- [Forge 1.20.1 文档快照](legacy-forge/README.md)：仅用于核对迁移前行为和设计理由，不得
  作为 NeoForge 1.21.1 API 指南，也不应反向修改。

## 维护规则

- 活动计划只保留未完成或正在验收的工作，完成后记录提交、测试和视觉核验，再移入
  `docs/archive/`。
- 不把 `.agents/`、本地 `LDLib2/` 源码检出、截图、运行日志或缓存写入项目文档清单。
- 测试数量以最近一次完整执行为准；如果源集变化，应同步更新 README 和相应活动计划。
