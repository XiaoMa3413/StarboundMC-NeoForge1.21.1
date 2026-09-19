---
name: ldlib2-ui
description: 在 StarboundMC NeoForge 1.21.1 中，已经进入具体实现、迁移、调试或 API 核验阶段，并需要使用 LDLib2 2.2.36.a 时使用。纯视觉概念、信息架构、布局探索、美术方向、线框图/概念稿讨论不使用；不要让本 Skill 代替 UI 设计。
---

# LDLib2 UI

本 Skill 的职责是：**把已经确定的 UI 设计可靠地实现为 LDLib2 界面**，并处理生命周期、布局、事件、同步、性能和版本/API 问题。

LDLib2 是实现工具，不是 UI 设计来源。不要从“库里有哪些 Widget”反推页面应该长什么样，也不要把项目中的现有 LDLib2 页面自动当作视觉模板。

## 设计边界（最高优先级）

1. 如果任务仍处于“重新设计 / 更好看 / 改布局 / 做概念 / 定视觉语言 / 做美术”的阶段，本 Skill 不负责决定视觉方案。优先读取项目的 UI/美术规范、用户提供的参考或已经批准的设计稿。
2. **先有设计，再做组件映射。** 不得因为 `Button`、`ScrollerView`、`TabView`、SDF、内置主题等实现方便，就把页面改造成卡片、列表、标签页或 Dashboard。
3. 复用项目基类只代表复用生命周期和技术接入，不代表复用页面布局、视觉层级、配色、边框或信息架构。
4. `TeleporterRoot`、`machine_ui.lss`、星图等项目活例只用于核对 API、生命周期或特定实现技巧，**不得作为默认视觉模板**。
5. 允许并鼓励在设计需要时采用混合实现：贴图资产 + LDLib2 交互层、自定义 `UIElement` 绘制、动态画布、专用动画元素等。不要为了“更 LDLib2”而把具有身份感的视觉元素退化成通用矩形控件。
6. 视觉主体、独特设备外壳、蓝图、全息图、仪表、机器结构等如果已有专用美术资产，应优先忠实实现资产；LDLib2 负责布局、命中、状态、文本、动画和数据连接。
7. 若设计要求与本 Skill 的“常用范式”冲突，只要不违反 API/同步/安全约束，以设计要求为准，并选择能忠实实现它的最简单技术路径。

### 实现前快速检查

在开始写代码前，用现有设计回答以下问题；不要自行用 Widget 默认值替代答案：

- 页面视觉主体是什么？
- 主要交互路径是什么？
- 哪些部分是专用美术/自绘，哪些只是标准控件？
- 哪些状态需要 hover / selected / disabled / progress / warning？
- 哪些结构可以复用项目生命周期，哪些必须页面专用？

如果这些问题在任务/设计稿/项目规范中已有答案，直接遵循，不重新设计。

## 开始前

1. 修改 Java 实现、迁移 UI 或诊断版本/API 问题时，从仓库根运行
   `python .agents/skills/ldlib2-ui/scripts/check_baseline.py --repo .`。仅视觉资源、纯 LSS 微调、只读解释或不依赖具体版本的审查可跳过。
2. 只检查与当前任务直接相关的 Screen、Menu/网络链路、Root、LSS、资源或语言文件，再决定改动范围。
3. 将仓库内 `LDLib2/` 视为**只读 API 依据**，不要修改或提交它，除非用户明确要求开发上游库。
4. API 不确定时用 `rg` 查本地源码和 `LDLib2/.../test/ui/`，不要凭名称猜签名。源码缺失或版本不一致时，改查 Gradle 实际解析的同版本 sources/jar，并说明依据。

当前项目基线是 Minecraft 1.21.1、NeoForge 21.1.x、LDLib2 2.2.36.a；实际值始终以 `gradle.properties` 和检查脚本结果为准。

## 按任务读取参考

只读取当前任务需要的文档，不要默认加载全部 references。

| 任务 | 必读 |
| --- | --- |
| 新建普通机器 UI、迁移旧 Screen、处理生命周期 | [core-workflow.md](references/core-workflow.md) |
| 选择控件、图片、滚动容器或贴图 | [widgets-and-textures.md](references/widgets-and-textures.md) |
| 编写 LSS、选择内置皮肤、实现已确定的视觉语言 | [lss-styling.md](references/lss-styling.md) |
| 文本溢出、本地化、GUI Scale、布局与人体工程学 | [text-and-ergonomics.md](references/text-and-ergonomics.md) |
| 按钮状态、命中、焦点、双击、拖拽、服务器动作 | [interaction-and-state.md](references/interaction-and-state.md) |
| 自定义绘制、动画、查询 API、XML 或特殊开屏方式 | [api-reference.md](references/api-reference.md) |
| 首次打开卡顿、重复建树、布局抖动、渲染开销 | [performance-and-debugging.md](references/performance-and-debugging.md) |
| LDLib2 托管同步、RPC、服务端模板 UI | [sync-and-network.md](references/sync-and-network.md) |
| Configurator/Editor | 仅明确涉及时读 [configurator-and-editor.md](references/configurator-and-editor.md) |
| NodeGraph、KubeJS/配方查看器或客户端底层 | 仅明确涉及时读 [nodegraph-integration-client.md](references/nodegraph-integration-client.md) |

> 如果任务只是“设计这个页面应该长什么样”，不要因为上表存在就加载实现参考；设计完成、进入实现阶段后再使用本 Skill。

## 关键决策

- **普通菜单屏幕**：技术接入上优先复用项目的 `client/ui/StarboundModularScreen.java`，避免复制 ModularUI 初始化与清理代码；这不限制页面视觉结构。
- **动态画布**：星图这类需要 partial-tick 插值、视口平移和大量几何绘制的页面可以保留专用 Screen/自绘元素；按钮、文字等交互元素可按设计需要加入 LDLib2 组件树。
- **视觉语言**：先服从用户要求、项目美术规范和已批准设计。不要因为某个项目活例成熟，就复制其卡片、圆角、列表或面板结构。
- **资产驱动**：页面需要专用外壳、蓝图、全息图、仪表或机器结构时，优先使用对应 Sprite/自绘资产；LDLib2 不需要承担“画出所有视觉”的职责。
- **数据权威**：UI 是客户端视图。传送、重命名、配方执行等会改变游戏状态的动作必须走现有 Menu/Packet/RPC，并在服务端重新验证；`Button.setOnClick` 本身只是客户端回调。
- **保留式更新**：界面挂载后优先更新 text/class/active/display，避免每 tick 或每次选择都 `clearAllChildren()` 后重建整棵树。
- **交互路径**：组件式界面优先使用 LDLib2 的命中与冒泡。自定义画布可以手动计算几何命中，但同一对象只能有一个权威选择路径，不能再叠加容差不同的兜底搜索。

## 2.2.36.a 必须牢记的 API 事实

- `UIEvents.CLICK` 才是点击常量；不存在 `UIEvents.MOUSE_CLICK`。
- Java 布局方法是 `layout(l -> l.aspectRatio(...))`；LSS 属性才叫 `aspect-rate`。
- `selectId("foo")` 不带 `#`；CSS 查询写 `select("#foo")`。
- 悬停选择器用 `:hover`/`.__hovered__`；焦点运行时类是 `__focused__`，本版本写 `:focused`/`.__focused__`，不要写 `:focus`。
- 实际绘制层级是 `drawBackgroundTexture` → `drawBackgroundAdditional` → children → `drawBackgroundOverlay`。`drawBackgroundAdditional` 不会盖住子元素。
- `Button.setOnClick` 在左键 `MOUSE_DOWN` 时触发；base/hover/pressed 是瞬时态，持久选中态应使用 `Toggle` 或独立 class。
- `TextElement.setText(String)` 默认把字符串当翻译键；动态文字使用 `Component.literal(...)` 或 `setText(text, false)`。
- `StyleAnimation.start()` 在元素未挂载时返回 no-op subscription，不会因 `ModularUI == null` 自动抛 NPE；若未挂载时也必须呈现终态，显式写入该终态。

## 项目活例

以下均是**实现参考，不是视觉模板**：

- `client/ui/StarboundModularScreen.java`：普通 LDLib2 菜单屏幕的生命周期基类。
- `client/TeleporterScreen.java` + `client/teleporter/TeleporterRoot.java` + `assets/starboundmc/lss/machine_ui.lss`：ScrollerView、文本裁切、持久选择态等实现参考；当前视觉仍在迭代，不复制其页面结构。
- `client/starmap/StarmapTerminalScreen.java` + `client/starmap/`：全屏动态画布、插值、分层、自绘节点和动画的特例；只提取确实需要的技术模式。
- `docs/known-issues.md`：首次打开 LDLib2 UI 卡顿等已复现问题。

## 完成标准

1. 实现结果首先应忠实于任务中已确定的设计，不得因为组件方便而擅自改变信息架构或视觉主体。
2. 对照源码核验新增或不确定的 API；已在同版本项目中验证且未变更的用法不重复核验。
3. 修改 Java 或 API 用法时运行 `./gradlew compileJava`（Windows 可用 `.\gradlew.bat compileJava`）。仅修改资源或样式时只运行相关检查，不强制编译全部 Java。
4. 视觉和交互验证只覆盖受改动影响的语言、缩放、状态和操作。当前环境无法执行的实机检查列为交付清单；除非用户明确要求端到端验收，不因此阻塞完成。
5. 相关检查通过后，只有新改动、失败或未解决疑点才扩大或重复验证。
6. 用户已说明亲自视觉验证时，给出明确验证清单，不额外制作截图。

修改本 Skill 中的版本或 API 事实时，先用检查脚本确认版本，再以同版本源码和官方可运行示例为事实基线。
