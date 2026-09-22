# 控件与贴图选择

路径相对 `LDLib2/src/main/java/com/lowdragmc/lowdraglib2/gui/`。不要从类名猜 API；本文件只列
StarboundMC UI 最常用、已由 2.2.36.a 源码确认的选择。

## 先按需求选组件

> 本表用于把**已经确定的设计**映射到 LDLib2 控件。不要从控件列表反推页面信息架构，也不要为了复用控件把专用视觉主体改造成通用列表、卡片或面板。


| 需求 | 首选 | 注意 |
| --- | --- | --- |
| 纯布局容器/面板 | `UIElement` | LDLib2 2.2.36.a 没有通用 `Panel` 类；背景由 style/LSS 提供 |
| 标题、状态、说明 | `Label` 或 `TextElement` | 文本策略见 `text-and-ergonomics.md` |
| 普通动作 | `Button` | `setOnClick` 在左键按下时调用；服务器动作另走网络 |
| 持久二态/单选 | `Toggle` / `Toggle.ToggleGroup`，或显式状态 class | 不把 Button 的 pressed 当 selected |
| 开关 | `Switch` | 明确 on/off 标签或 tooltip，不只靠颜色 |
| 选项列表 | `Selector` | 检查弹层、焦点与屏幕边界 |
| 单行输入 | `TextField` | 校验器限制业务值，服务端仍需复核 |
| 多行输入 | `TextArea` | 明确滚动、换行和最大长度 |
| 可滚动列表 | `ScrollerView` | 子项用 `addScrollViewChild`；配置横纵滚动与裁切 |
| 大量同类行 | `VirtualScrollerView` | 只有数据量确实需要虚拟化时使用 |
| 标签页 | `TabView` + `Tab` | 不用标签页掩盖本可一次读完的简单机器 UI |
| 进度 | `ProgressBar` | `setRange`/`setProgress`；避免每帧重建 |
| 物品/流体槽 | `ItemSlot` / `FluidSlot` / `InventorySlots` | 仅在它们负责绑定槽位时使用；已有 Menu 槽位见下文 |
| GUI 内世界预览 | `Scene` | 成本高，先证明 3D 预览确有价值 |

完整注册名应从源码查询：

```powershell
rg -n '@LDLRegister.*ldlib2:ui_element' LDLib2/src/main/java/com/lowdragmc/lowdraglib2/gui
```

`Dialog`、`Menu` 等部分 Java 类没有 XML 元素注册名；Java 可用不代表能直接写进 XML。

## 文本

```java
var title = new Label()
        .setText(Component.translatable("gui.starboundmc.example.title"));
title.textStyle(s -> s.fontSize(9).textShadow(false));
```

- `setText(String)` 默认将字符串作为翻译键。字面或动态文本使用 `Component.literal(...)` 或
  `setText(value, false)`。
- `Label`、`TextElement` 的换行、适应宽高与滚动行为不是一回事，选择前读文本参考。
- 完整内容无法常驻显示时，优先静态裁切/简短显示 + tooltip，不默认让文本悬停滚动。

## Button 与内部元素

```java
var button = new Button()
        .setText(Component.translatable("gui.starboundmc.example.confirm"))
        .setOnClick(event -> requestConfirm());
button.addClass("primary-action");
button.setActive(canConfirm);
```

- 文本子元素 class 是 `__button_text__`，可在 LSS 单独设置颜色、阴影和禁用态。
- Button 内部文本默认 `adaptiveWidth(true)`；固定宽 Button 的长翻译仍需裁切、tooltip 或更宽布局。
- 文本子元素的事件正常冒泡到 Button；只有子元素主动 `stopPropagation()` 才会挡住父按钮。
- `setActive(false)` 禁止 Button 动作并添加 `__disabled__`，但禁用时仍绘制 baseTexture；LSS 必须
  给禁用文字/背景或 tooltip 足够反馈。
- base/hover/pressed 只表示瞬时指针状态；持久选择态使用 Toggle 或项目 class。

## ScrollerView

常见纵向列表结构：

```java
var list = new ScrollerView();
list.scrollerStyle(s -> s
        .mode(ScrollerMode.VERTICAL)
        .verticalScrollDisplay(ScrollDisplay.AUTO)
        .horizontalScrollDisplay(ScrollDisplay.NEVER));
list.viewPort(v -> v.setOverflowVisible(false));
list.addScrollViewChild(row);
```

- 不把普通 child 直接加错层；可滚动内容进入 view container。
- 列表行及其文本也要裁切，单靠最外层框不一定约束滚动文字。
- 鼠标滚轮应被列表消费，避免同时缩放/拖动背景画布。
- 更新列表时保留现有行并按 key 增删，避免每次选择重建整个列表。

## 已有 Menu 槽位

`ItemSlot.bind(Slot)` 在元素挂载后会把尚不存在的 Slot 加入当前 Menu；`InventorySlots` 会为玩家
背包创建并绑定 36 个新 Slot。绑定完全相同的 `menu.getSlot(i)` 不会再次插入该对象，但在普通
`AbstractContainerScreen` 中仍可能叠加 LDLib2 与原版两套绘制/hover/点击路径。因此，迁移一个已经
在构造器中 `addSlot(...)` 的旧 Menu 时，不要在仅换视觉的任务中再包装同一批槽位。

- 只焕新视觉：保留 Menu 和原版容器交互，LDLib2 画无命中的槽位背板，并保持 Screen 的
  `imageWidth/imageHeight`、整数面板原点和 Menu 坐标一致；初始化与 resize 后确认槽位数量、顺序
  和 quick-move 分段完全不变。
- 完全迁移槽位：让每个逻辑槽只由一个 `ItemSlot` 绑定，重新核对索引、quick-move 范围、服务端
  同步和 resize 后坐标；这是独立的 Menu 重构，不与普通换皮混在同一步完成。

## 图片与图标

LDLib2 2.2.36.a 没有通用 `Image` 元素。普通图片用 UIElement 背景贴图：

```java
var image = new UIElement()
        .layout(l -> l.width(24).height(24))
        .style(s -> s.backgroundTexture(SpriteTexture.of(resourceLocation)));
image.setAllowHitTest(false);
```

需要独立命中或矢量动态图标时，继承 `UIElement` 并在正确绘制钩子中绘制；它仍应参加布局和事件树。

## 贴图体系

所有贴图实现 `IGuiTexture`。

| 类 | 用途 | 关键事实 |
| --- | --- | --- |
| `ColorRectTexture` | 纯色矩形 | 构造器接 ARGB |
| `ColorBorderTexture` | 直角描边 | 参数顺序是 `(width, color)`；正宽度向盒外、负宽度向盒内绘制 |
| `RectTexture` / `SDFRectTexture` | 圆角、描边、SDF 面板 | SDF 不是所有页面的默认风格 |
| `SpriteTexture` | assets 图片/九宫格 | `of(ResourceLocation)`，可 `setSprite`/`setBorder` |
| `VanillaSpriteTexture` | 原版 GUI sprite | 适合贴合原版视觉时使用 |
| `GuiTextureGroup` | 多层叠加 | `of(a,b)` 按参数顺序绘制，a 在下、b 在上 |
| `TransformTexture` | 平移、旋转、缩放包装 | 动画频繁变化时注意分配与缓存 |
| `ItemStackTexture` / `FluidStackTexture` | 物品/流体预览 | 数据更新时替换值，不每帧重建元素 |
| `AnimationTexture` / `DynamicTexture` | 动画/动态内容 | 先评估持续更新成本 |
| `ShaderTexture` | shader 效果 | 仅在设计确需时使用并实机验证 |

`UIResourceTexture` 接收编辑器资源系统的 `IResourcePath`，不是普通 assets
`ResourceLocation` 图片的通用入口；普通模组图片使用 `SpriteTexture.of(rl)`。

LSS 还支持 `rect(...)`、`border(...)`、`sdf(...)`、`sprite(...)`、`group(...)`、`color(...)`、
`scale(...)` 和 `built-in(...)` 等贴图表达式，详见 LSS 参考。

## 项目活例

- `teleporter/TeleporterRoot.java`：ScrollerView、按钮、SpriteTexture 与矢量图标组合。
- `machine_ui.lss`：`built-in(ui-mc:...)`、`group(rect,border)` 与按钮内部文字样式。
- `starmap/StarmapBodyTextureResolver.java`：天体贴图、缺失资源兜底与多层叠加。
- `starmap/StarmapInfoPanelElement.java`：Label/Button/预览区域组合；用于动态信息卡，不是普通机器模板。
