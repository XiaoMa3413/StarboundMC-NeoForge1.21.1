# 事件、状态、命中与服务器动作

本参考基于 LDLib2 2.2.36.a。目标是让一个操作只有一条可推理的事件路径，并把瞬时指针状态、
持久业务状态与服务器权威状态分开。

## 事件模型

事件按 capture（Root→target）、target、bubble（target→Root）分发。子元素只有主动
`stopPropagation()` 才会阻断祖先监听器；普通 Button 文本子元素不会自动“吃掉”父按钮点击。

常用常量：

- `UIEvents.MOUSE_DOWN` / `MOUSE_UP`；
- `UIEvents.CLICK`：按下与释放命中同一元素后产生；不存在 `MOUSE_CLICK`；
- `UIEvents.DOUBLE_CLICK`：同一元素、同一按键在约 300ms 内的第二次 click；
- `MOUSE_MOVE` / `MOUSE_ENTER` / `MOUSE_LEAVE` / `MOUSE_WHEEL`；
- `KEY_DOWN` / `KEY_UP` / `CHAR_TYPED`；
- `FOCUS` / `BLUR`；
- `DRAG_ENTER/LEAVE/UPDATE/SOURCE_UPDATE/PERFORM/END`；
- `HOVER_TOOLTIPS`。

`Button.setOnClick` 是 Button 自己监听 `MOUSE_DOWN` 的便捷 API，因此在左键按下时就执行。
若业务必须“按下并在同一控件释放后确认”，在自定义元素上监听 `UIEvents.CLICK`，不要假设
`Button.setOnClick` 是 release-confirm。

重要 Button 也可不设置 `setOnClick`，改为监听 `UIEvents.CLICK`。此时把 Button 的纯装饰文本/图标
设为不命中，保证按下与释放的最深 target 都是同一个 Button；不能同时保留两个动作回调。

## 命中测试

- `setAllowHitTest(false)` 让元素自身不成为 target，但仍会递归命中其 children；适合装饰层或容器。
- 完全不需要交互的图标、分隔线和覆盖装饰应关闭 hit test。
- 交互面板可调用 `stopInteractionEventsPropagation()`，阻止点击、滚轮或拖动穿透到背景画布；使用前
  确认它不会阻断需要的祖先行为。
- 组件式界面让具体行/按钮成为事件 target，Root 只处理背景行为。
- 自绘画布允许手动几何命中，但同一节点不要同时使用 UIElement hitbox 和另一套 nearest-search。
  若必须扩大点击容差，扩大唯一 hitbox 或统一几何函数。

排查“按钮没反应”的顺序：

1. 检查 `setActive`、`setDisplay`、尺寸和最终布局坐标；
2. 用 `hitTest`/debugger 看真正 target 与 hover 链；
3. 检查透明覆盖层和 `setAllowHitTest`；
4. 检查子元素是否 stopPropagation；
5. 检查回调是否只改客户端、实际动作是否需要 packet；
6. 最后才考虑 capture 监听，不复制未经证明的 workaround。

## Button 状态语义

LDLib2 Button 内建状态只有：

- `DEFAULT` → baseTexture；
- `HOVERED` → hoverTexture；
- `PRESSED` → pressedTexture；
- `setActive(false)` → 不响应动作，添加 `__disabled__`，绘制仍回到 baseTexture。

这些都是控件状态，不是业务选择状态。

```java
row.removeClass("destination-selected");
if (selected) row.addClass("destination-selected");
row.setActive(available);
```

- 持久 selected 使用 Toggle 或独立 class/LSS。
- disabled 需要独立文字/背景/原因 tooltip；不要依赖 Button 自动生成禁用贴图。
- `setActive(false)` 不会把元素移出命中树；自定义 UIElement 监听器仍应自行检查 active。
- hover、pressed、selected 至少在边框、明度、图标或形状之一上可区分。
- 不把 base 与 pressed 材质反过来模拟选中，这会导致按压反馈与选择反馈错位。

2.2.36.a 的 Button 默认不 focusable，也没有内建 Enter/Space 激活；ButtonStyle 也不读取
`focus-overlay`。需要键盘可达性时，显式实现 focus、KEY_DOWN 激活和可见焦点样式并实机验证，
不要仅在 LSS 写 `focus-overlay` 后假定已经生效。

## 焦点与状态选择器

- `setActive(false)` → `__disabled__`，LSS `:disabled`。
- hover 链 → `__hovered__`，LSS `:hover`。
- focus → `__focused__`，2.2.36.a 使用 `:focused`/`.__focused__`，不要使用 `:focus`。
- 自定义伪类 `:selected` 匹配 `__selected__`；项目若添加普通 class `destination-selected`，LSS 就写
  `.destination-selected`，不要混淆两种命名。

输入框打开后确认 ESC、Enter、Tab/焦点离开和点击外部的行为；不要让全屏快捷键抢走正在输入的键。

可见性区别：`setVisible(false)` 保留布局但不绘制/命中子树；`setDisplay(false)` 等同布局
`display: none`；仅把 opacity 动画到 0 不应代替明确的最终显示状态。

## 拖放与视口平移不是一回事

`element.startDrag(object, texture)` 和 `DRAG_*` 是对象拖放协议，适合把物品/节点拖到 drop target。

星图视口平移是另一类手势：

- 在 Screen/画布元素记录按下点；
- 超过小阈值后才进入 pan，避免点击抖动变成拖拽；
- `mouseDragged` 更新 view transform；
- `mouseReleased` 总是清理状态；
- 信息面板、按钮、列表和滚动条区域不启动背景 pan；
- 选择框、节点绘制和 hitbox 使用同一插值后的 view transform。

项目动态画布参考 `StarmapTerminalScreen.mouseDragged/mouseReleased` 与
`StarmapTerminalRoot.dragView/finishViewDrag`，不要把这套特化复制到普通机器 UI。

## 双击与单击

同一元素可能先收到单击，再收到双击。若“双击进入、单击选中”：

- 单击只做可逆选择；
- 双击执行进入操作并 stopPropagation；
- 不让第二次单击先触发不可逆动作；
- 列表移动/重建后不要保留指向旧元素的双击状态。

## 客户端回调与服务器权威

普通项目 Screen 的 Root 只存在客户端：

```text
Button callback -> 项目 Packet/Menu 请求 -> 服务端重新验证 -> 执行 -> 同步结果 -> UI 刷新
```

服务端至少复核玩家、菜单仍有效、目标方块/维度、距离/权限、参数范围和资源消耗。按钮 disabled 只是
可用性提示，不是安全边界。

`Button.setOnServerClick` 只适合服务器也持有对应 LDLib2 UIElement/UISyncManager 的托管 UI。
本项目普通客户端构树页面继续使用已有网络链路，除非任务明确迁移整套开屏/同步架构。
