# Matter Manipulator Upgrade UI — Design Contract v1

## SCREEN TYPE

Feature UI.

## SCREEN PURPOSE

让玩家把物质枪放入工程工作台，检查四条升级路线的当前等级、模块成本和可执行状态，并完成一次升级。

## PLAYER FANTASY

玩家不是在点 RPG 技能树，而是在舰载工程台上对一件具体的 Matter Manipulator 进行校准、模块安装和部件级改造。

## PRIMARY VISUAL

中央 Matter Manipulator 工程蓝图必须是第一视觉主体。升级节点作为枪体部件的工程引线存在，视觉权重低于枪体本身。

## INFORMATION PRIORITY

1. Matter Manipulator 蓝图与当前选中的部件方向。
2. 当前升级支路与等级进度。
3. 下一等级成本以及是否可执行。
4. 可用模块数量。
5. 物质枪校准槽位。
6. 玩家物品栏。

## INTERACTION FLOW

打开工作台 → 先看到中央设备蓝图 → 插入物质枪 → 选择某条升级支路/节点 → 对应工程引线高亮 → 底部状态条显示等级和成本 → 条件满足时执行升级 → 节点状态刷新。

## SPATIAL ORGANIZATION

- 顶部：舰载工程终端标题与可用模块数量。
- 中部：独立的绿色工程视口；中央是枪体蓝图，四条支路位于周边。
- 支路通过细工程引线终止在枪体相关位置，不使用独立卡片承载。
- 中部底缘：窄状态/执行条，包含当前支路、等级、成本、校准槽和 APPLY 操作。
- 下部：保持中性的 Minecraft 玩家物品栏区域。

## ART / CUSTOM VISUALS

- 保留 `matter_manipulator_blueprint.png` 作为核心资产。
- 使用低频工程网格、注册刻度、枪体焦点框和部件锚点作为程序化辅助视觉。
- 不增加新的大面积装饰贴图；第一轮优先验证层级和交互。

## GENERIC UI

普通 Label、Button、tooltip 和玩家物品栏槽位继续由 LDLib2 / Vanilla menu 层承担。

## MOTION / FEEDBACK

本轮不增加持续动画。选择反馈依靠支路文字、连接线、锚点和节点边框亮度变化。Glow 保持克制。

## STATES

- Locked：暗、低饱和，只保留结构。
- Unlocked：可读但不抢焦点。
- Ready：工程绿/黄绿提示升级条件已满足。
- Selected：仅当前支路获得最亮的工程引线和边框。
- Maxed：稳定完成状态，不使用警告色。
- Cost：成本使用琥珀色，与普通工程绿区分。

## DO NOT

- 不做成四块独立卡片或通用技能树面板。
- 不让所有节点、连接线和文字同时发亮。
- 不把整个机器外壳染成绿色；绿色属于工程显示层，外壳遵循深蓝石墨/钛灰母主题。
- 不为方便实现而移动或改变服务器菜单槽位语义。
- 不在本轮修改升级成本、等级上限、网络包或服务端校验。

## IMPLEMENTATION CONTRACT

### Do not change

- `UpgradeMenu` 的服务端升级逻辑、槽位语义和网络行为。
- 320×250 的总体面板尺寸与现有玩家物品栏/物质枪槽位坐标契约。
- Matter Manipulator 蓝图为第一视觉主体。
- 四条支路必须表现为与具体设备相连的工程改造方向。
- 工程绿只作为子主题；舰船外壳仍为 graphite / titanium。

### Implementation may decide

- LDLib2 元素树的细节。
- 工程网格、注册标记、连接线和锚点的具体绘制方式。
- LSS 中的边框深浅、hover 强度和非核心文字亮度。
- 在不改变槽位坐标和交互语义的前提下微调标签位置。
