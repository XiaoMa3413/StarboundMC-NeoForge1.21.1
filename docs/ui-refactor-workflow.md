# StarboundMC UI 重构工作流

本文件定义 UI 重构阶段的工作顺序。目标是让视觉设计与 LDLib2 实现解耦，避免“先选组件、再反推界面”的框架化倾向。

## 1. 两层职责

### `starboundmc-ui-design`

负责：

- 页面目的与玩家交互幻想
- 第一视觉主体
- 信息优先级
- 空间组织与交互流程
- 专属美术资产需求
- 动效与状态反馈
- 反 Dashboard / 反模板化评审
- 最终 Design Contract

默认不负责：

- Java 实现
- LSS 具体写法
- LDLib2 Widget 选择
- 网络、Menu、同步细节

### `ldlib2-ui`

负责：

- 将已经确认的 Design Contract 实现成 LDLib2 UI
- 生命周期、布局、交互事件、同步、性能和 API 核验
- LSS 与资源接入
- 必要的自定义 `UIElement`、贴图层、绘制和动画实现

它不应擅自改变视觉主体、信息架构或页面交互逻辑。

## 2. 标准流程

```text
需求
↓
UI 设计 / Review
↓
Design Contract
↓
LDLib2 实现
↓
compile / runClient
↓
游戏内截图与交互验证
↓
视觉 Review
↓
最小修正
```

不要使用以下流程：

```text
需求
↓
直接写 LDLib2
↓
看起来不够好
↓
反复改 LSS / 边框 / 颜色
```

## 3. Design Contract 最低内容

每个新建或明显重做的 Feature / Experience UI，在开始实现前至少明确：

- PURPOSE
- INTERACTION FANTASY
- SCREEN MODE
- PRIMARY VISUAL
- INFORMATION PRIORITY
- INTERACTION FLOW
- SPATIAL ORGANIZATION
- ART / CUSTOM VISUALS
- GENERIC UI
- MOTION / FEEDBACK
- STATES
- DO NOT
- IMPLEMENTATION CONTRACT

实现层不得在没有说明的情况下删除或弱化这些设计不变量。

## 4. UI 分类

### Utility UI

箱子、熔炉、简单控制器等。目标是紧凑、清晰、低成本，不需要每个页面都有专属美术。

### Feature UI

Voxel Printer、Matter Manipulator Upgrade、N.O.V.A.、Teleporter 等。必须存在明确视觉主体，允许专属贴图、动画、自绘和混合式 UI。

### Experience UI

Star Map、大型剧情终端、特殊事件等。体验本身就是 UI，应从空间、对象、运动、选择和导航出发，不从 Panel / Card / List / Button 出发。

## 5. Anti-AI UI 检查

设计或 Review 时检查：

- 是否每一个逻辑分组都套了框？
- 是否出现 card inside card？
- 是否所有东西都是圆角矩形？
- 是否存在无意义标题栏？
- 是否因为方便实现而默认三栏 Dashboard？
- 是否所有信息视觉权重接近？
- 是否主要依赖文字而不是视觉？
- 是否没有明确第一视觉主体？
- 是否所有 cyan 元素都在 glow？
- 如果替换成 Customers / Revenue / Tasks / Status，页面是否仍像合理的 SaaS Dashboard？

如果最后一项答案为“是”，优先重新审查信息架构，而不是继续增加装饰。

## 6. UI Playground（后续实施）

在第一批页面方向稳定之后，再提炼公共 `StarboundUI Kit`，并建立专门的 Playground Screen 展示：

- Typography
- Buttons: normal / hover / pressed / disabled
- Status: ready / warning / fault
- List row / selected row
- Scrollbar
- Resource indicator
- Item preview
- Hologram
- Progress
- Tooltip

不要在设计尚未稳定前过早抽象公共组件，否则会把当前不成熟的视觉模式固化进代码。

## 7. 当前优先顺序

1. 先将规范与 Skills 纳入仓库
2. 用 Matter Manipulator Upgrade UI 验证“Design → Contract → Implement”流程
3. 通过一到两个 Feature UI 后再提炼 StarboundUI Kit
4. 再重构 Voxel Printing Station
5. 最后清理普通机器 UI 的公共样式与重复实现
