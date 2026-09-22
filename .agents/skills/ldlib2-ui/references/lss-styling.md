# LSS、内置皮肤与视觉语言

LSS 是 LDLib2 的 CSS 风格样式语言。解析与属性注册位于 `gui/ui/style/`，布局属性位于
`gui/ui/layout/LayoutProperties.java`。本参考基于 2.2.36.a。

## 加载与作用域

项目样式文件位于 `assets/<namespace>/lss/*.lss`，普通精确引用带 `.lss`：

```java
ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "lss/machine_ui.lss")
```

`UI.of(root, ResourceLocation...)` 可装入多个 Stylesheet。若加载一个完整内置主题再叠加项目样式：

```java
UI.of(root, StylesheetManager.MC, projectStylesheet)
```

`UI.of` 会静默过滤无法解析的 stylesheet；界面突然退回默认样式时，先检查资源路径和
`StylesheetManager.getStylesheet(...)` 结果，而不是继续加 inline 样式。

不要依赖两个独立 stylesheet 中“同 specificity 后加载者一定覆盖”的猜测；项目规则使用页面根 class
和更明确的选择器。普通项目页面也可以只加载自己的 LSS，并在其中直接引用内置贴图资源。

精确常量是 `StylesheetManager.MC/MODERN/ORE/GDP`。`*_MERGED` 会有意合并所有 namespace 下的
同路径 LSS，仅在需要资源包共同参与该路径时使用；不要把 merged 当默认。

`built-in(ui-mc:RECT_BLACK)` 只取内置贴图，不等于加载 `mc.lss` 的全部 Button/TextField 默认样式。
这是复用素材与采用完整主题之间的重要区别。

## 四套内置 LSS 的实际定位

| 样式 | 源码特征 | 适用判断 |
| --- | --- | --- |
| `mc` | `ui-mc` 九宫格/像素贴图，灰黑原版机器感，覆盖常见控件 | 紧凑机器、贴近 Minecraft 的工业 UI 起点 |
| `modern` | 深色 SDF 圆角、蓝色按钮、JetBrains Mono | 工具/编辑器风格；不是所有科幻页面的默认答案 |
| `ore` | `ui-ore` 装饰性像素贴图、厚边框、彩色确认/拒绝按钮 | 明确想采用其完整游戏化皮肤时使用 |
| `gdp` | 少量 `ui-gdp` 面板/预览规则，主要服务节点图相关 UI | 不是完整通用主题，不能期待一键覆盖所有控件 |

先在 LDLib2 官方测试屏中观察，再决定是否加载整套主题。不要同时混用多个完整主题来“试颜色”。

LDLib2 的风格能力不止主题切换。页面差异主要来自：

- 信息架构和布局密度；
- `rect/border/sdf/sprite/built-in/group/color/scale` 的组合；
- 直角或圆角、线框或实体、贴图或矢量；
- 字体层级、留白、图标语汇、选中反馈和动画节奏。

## 选择器

| 写法 | 含义 |
| --- | --- |
| `button` | `@LDLRegister` 元素名 |
| `.class` / `#id` / `*` | class / id / 通配 |
| `.screen .row` / `.screen > .header` | 后代 / 直接子级 |
| `:not(...)` | 取反 |
| `:host` / `:internal` | 组件自身 / 内部元素作用域 |
| `:hover` | 2.2.36.a 特判为 `__hovered__` |
| `:disabled` | `__disabled__`，由 `setActive(false)` 添加 |
| `:focused` | `__focused__`；本版本不要写 `:focus` |
| `:selected` | 自定义 `__selected__`；代码必须添加同名运行时 class |

也可直接选 `.__hovered__`、`.__focused__`、`.__disabled__`。项目的普通业务 class 无需双下划线，
例如 `.teleporter-destination-selected`。

这里的 `:host` 不是浏览器 Shadow DOM host：它限制匹配非 internal UI；`:internal` 用于控件内部子元素。
运行时还常见 Tab 的 `__selected__` 和 Toggle 的 `__on__`，对应 `:selected`、`:on`。

```css
.teleporter-destination:host {
  base-background: built-in(ui-mc:RECT_BORDER);
  hover-background: built-in(ui-mc:RECT_3);
  pressed-background: built-in(ui-mc:RECT_3) color(#dddddd);
}

.teleporter-destination-selected:host {
  base-background: group(rect(#d8dcdd), border(1, #ffffff));
}

.teleporter-destination.__disabled__ .__button_text__ {
  text-color: #686d6f;
  text-shadow: false;
}
```

持久选中规则必须独立于 pressed，禁用规则也要独立检查。不要通过交换 base/pressed 贴图来表达
选中，否则鼠标按下和选择状态会互相打架。

## 级联与动态值

优先级：`DEFAULT < STYLESHEET < INLINE < ANIMATION < IMPORTANT`。

- LSS：主题、字号、间距、静态布局、普通状态规则。
- Java inline：运行时尺寸、贴图、动态位置或数据决定的少量样式。
- class：持久业务状态，例如 selected/error/empty；优于反复重写多项 inline 样式。
- animation：短时视觉过渡。动画期间会盖过 inline；异常时先检查动画是否仍占用属性。

不要把同一视觉令牌同时定义在多个 Java 类和 LSS。项目当前没有真正的 LSS 变量系统，令牌可
集中在一个主题 LSS 区域或单一 Java 常量类，但必须有唯一来源。

## 常用属性

- 全局：`background`、`overlay`、`tooltips`、`z-index`、`transform`、`opacity`、
  `overflow-clip`、`transition`。
- 文本：`text-color`、`font-size`、`font`、`text-shadow`、`text-wrap`、`roll-speed`、
  `adaptive-width`、`adaptive-height`。
- Button：`base-background`、`hover-background`、`pressed-background`。2.2.36.a 的 Button
  不读取 `focus-overlay`，不要把该属性当成 Button 键盘焦点反馈。
- 支持焦点 overlay 的输入/选择类控件可使用 `focus-overlay`，仍需核对具体控件实现。
- 布局：`display`、`flex-direction`、`width/height/min-*/max-*`、`margin-*`、`padding-*`、
  `gap-*`、`aspect-rate` 等。

未知属性会被忽略并写日志。出现“样式没反应”时先查日志、选择器匹配、级联来源和元素内部 class，
不要立即复制一套 inline workaround。

## 贴图表达式

```css
.flat-panel   { background: rect(#111416); }
.line-panel   { background: group(rect(#111416), border(1, #aeb4b6)); }
.soft-panel   { background: sdf(#dd2c2c34, 5, 1, #7f8084); }
.image-panel  { background: sprite(starboundmc:textures/gui/example.png); }
.mc-panel     { background: built-in(ui-mc:RECT_BLACK); }
.tinted       { background: built-in(ui-mc:RECT_3) color(#dddddd); }
```

选择表达式应服务于设计：直角线框、贴图皮肤、纯平面和 SDF 圆角会产生完全不同的结构感，
不是简单的“换配色”。

## 项目参考

- `machine_ui.lss`：`ui-mc` 内置贴图、直角 group/rect/border、滚动条内部 class；正在迭代。
- `starmap_redraw.lss`：全屏星图分层和信息卡规则；不作为机器 UI 皮肤。
- LDLib2 `mc.lss/modern.lss/ore.lss/gdp.lss`：查看主题真实覆盖范围，不根据名字猜视觉效果。
