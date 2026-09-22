# 核心工作流：新建与迁移 UI

本参考面向 StarboundMC 中最常见的“客户端构建 LDLib2 元素树、Menu 负责网络上下文”的页面。
它只规定实现与生命周期范式，不规定页面视觉结构；应在设计已经确定后使用。
API 基线为 LDLib2 2.2.36.a。

## 先分类

| 页面类型 | 选择 |
| --- | --- |
| 普通方块菜单、无原版槽位背景 | 继承项目 `StarboundModularScreen`，Root 继承 `UIElement` |
| 有物品槽位且仍由项目自己构树 | 仍可复用该基类，但先确认原版槽位坐标、hover 与背景需求 |
| 星图等连续动态画布 | 专用 Screen + ModularUI；保留 partial-tick、拖拽和自绘入口 |
| 服务端下发整棵 UI 模板 | 仅明确需要时使用 `BlockUIMenuType`/`UITemplate`，并读同步参考 |
| 纯原版 Screen 且不迁移 LDLib2 | 不使用本 Skill 强行改技术栈 |

## 项目默认范式

普通页面不要复制 ModularUI 生命周期，直接复用：

```java
public final class MyScreen extends StarboundModularScreen<MyMenu, MyRoot> {
    public MyScreen(MyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected MyRoot createRoot() {
        return new MyRoot();
    }

    @Override
    protected ResourceLocation stylesheet() {
        return ResourceLocation.fromNamespaceAndPath(
                StarboundMC.MODID, "lss/my_screen.lss");
    }
}
```

`StarboundModularScreen` 已处理：

- `root` 和 `ModularUI` 字段生命周期；
- resize 时销毁旧实例再重建；
- `setMenu`、`setScreenAndInit`、桥接 Widget 与焦点；
- 每 tick 调用 `modularUI.tick()`；
- `removed()` 时 `onRemoved()`、`setScreen(null)` 和字段清理；
- 不再渲染旧原版背景与标签层。

### 保留现有 Menu 槽位时

`StarboundModularScreen` 默认把 `imageWidth/imageHeight` 设为 0，适合无原版槽位的纯组件页面。
若旧 Screen 的 Menu 已有 `Slot`，子类构造器必须恢复旧面板尺寸，否则
`AbstractContainerScreen` 计算出的 `leftPos/topPos` 会改变，物品槽与新视觉背板整体错位：

```java
private static final int PANEL_W = 176;
private static final int PANEL_H = 220;

public MyScreen(MyMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageWidth = PANEL_W;
    imageHeight = PANEL_H;
}
```

- Root 中的面板使用同一宽高，且面板左上角必须与整数 `leftPos/topPos` 精确对齐；在奇数、偶数
  窗口尺寸和 resize 后检查，不能只依赖可能产生半像素结果的视觉居中。
- 保留原版容器对现有 Slot 的绘制、hover、拖放和 shift-click；LDLib2 槽位背板应为无命中装饰。
- Menu 已经创建玩家背包或机器槽时，不直接加入 `InventorySlots`，也不新建另一批绑定 Slot 的
  `ItemSlot`。新 Slot 会在挂载后加入 Menu；绑定 `menu.getSlot(i)` 虽不会重复插入同一对象，仍会与
  原版容器形成两套绘制/命中路径。若要由 LDLib2 完全接管槽位，单独重构并验证 Menu/Slot 映射。

Root 负责元素树与界面状态：

```java
public final class MyRoot extends UIElement {
    private final Label status = new Label();
    private final Button action = new Button();

    public MyRoot() {
        addClass("my-screen");
        layout(l -> l.widthPercent(100).heightPercent(100));

        status.setText(Component.translatable("gui.starboundmc.my_screen.status"));
        action.setText(Component.translatable("gui.starboundmc.my_screen.action"));
        action.setOnClick(event -> requestAction());
        addChildren(status, action);
    }
}
```

静态布局和视觉写入 LSS；运行时数据、显示状态和动态位置留在 Java。不要把同一颜色、尺寸同时
散落在多处 Java 与 LSS 中。

## 只有特殊屏幕才手动装配

需要在 `render()` 前调用 `prepareFrame(partialTick)` 或接管 `mouseDragged` 时，可参考星图专用
Screen。手动实现时 `root` 必须是字段而不是 `init()` 局部变量，并完整保留以下顺序：

1. `init()` 开头先销毁旧 ModularUI；
2. 创建 root，`ModularUI.of(UI.of(root, stylesheet), player)`；
3. `setMenu` → `setScreenAndInit` → `addRenderableWidget` → `setFocused`；
4. `containerTick()` 调 `modularUI.tick()`；
5. `removed()` 调 `onRemoved()`、`setScreen(null)`，再把 `modularUI/root` 置空。

## 客户端与服务端边界

- Screen、GuiGraphics、贴图和可视 Root 放在客户端包，不要让专用服务端加载客户端类。
- `setOnClick` 只执行客户端回调。改变世界、方块实体、物品、传送位置等动作应发送项目现有
  Menu/Packet 请求，由服务端校验玩家、维度、方块位置、权限和参数后执行。
- `Button.setOnServerClick` 依赖 LDLib2 服务端持有相应 UI/同步上下文；本项目这种客户端自己构树的
 普通 Screen 不应想当然地改用它。先读 `sync-and-network.md` 并核对开屏方式。
- 客户端只根据服务端确认的数据刷新显示，不把按钮禁用状态当安全校验。

## 元素树与更新策略

- 构造时建立稳定骨架；数据到达后更新 `setText`、`setActive`、`setDisplay`、class 和贴图。
- 动态列表按稳定 key 比较差异，能更新行就不清空整表；只有结构真实变化时增删对应子元素。
- 不在 `render`/自绘钩子里增删元素、改变布局或重复注册监听器。
- 多个页面共享生命周期时抽基类；只共享视觉令牌时共享 LSS class，不把业务状态塞进公共组件。

## 迁移旧 Screen

1. 记录旧 UI 的业务行为、Menu/Packet、槽位和键鼠操作，视觉重做不等于删除功能。
2. 先接入 `StarboundModularScreen` 和空 Root；有既有 Slot 时先恢复面板尺寸并确认
   `leftPos/topPos`、槽位坐标、开关、关闭、resize 与 Menu 同步正常。
3. 按标题/内容/主操作拆组件树，再迁移交互；不要把旧整屏 `GuiGraphics` 绘制原样塞进一个元素。
4. 复杂动态几何保留在一个自绘 scene 元素中，文字、按钮、输入框和列表使用 LDLib2 控件。
5. 最后删除旧背景/重复事件路径，并按完成标准做本地化、缩放和交互验证。

带 Slot 的页面还要从当前 Menu 实现记录迁移前后的 `menu.slots.size()`、顺序和 quick-move 分段，并把当前值作为
本次迁移的不变量；文档中的具体数量只作示例，不作为永久事实。实机回归左右键、拖拽分配、
shift-click、双击收集、携带物品关闭以及 resize 后对齐。

## 按需参考

只读与当前任务最接近的活例：

1. `client/ui/StarboundModularScreen.java`：普通生命周期基线。
2. `client/TeleporterScreen.java` 与 `client/teleporter/TeleporterRoot.java`：普通机器结构与状态更新。
3. `client/starmap/StarmapTerminalScreen.java`：仅用于确实需要 render/drag 特化的动态画布。
