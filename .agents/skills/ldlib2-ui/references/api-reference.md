# LDLib2 2.2.36.a 核心 API

源码相对路径均位于 `LDLib2/src/main/java/com/lowdragmc/lowdraglib2/`。本参考聚焦普通 UI 之外
容易写错的生命周期、查询、自绘、动画和特殊开屏方式。

## 核心对象

| 对象 | 作用 |
| --- | --- |
| `gui/ui/UIElement.java` | 元素树节点，持有 children、id/classes、布局、样式、事件与绘制钩子 |
| `gui/ui/UI.java` | Root、Stylesheet 列表和可选动态尺寸的装配对象；`UI.of(...)` 工厂入口 |
| `gui/ui/ModularUI.java` | 运行时：布局树、StyleEngine、AnimationEngine、同步、焦点、hover、tooltip 与桥接 Widget |
| `gui/ui/UITemplate.java` | 可序列化模板；主要用于服务端托管 UI 与编辑器资源，不是普通 Screen 的必选项 |

`UI` 的字段引用是 final，但 Root 元素和运行时样式仍可变化；不要把它理解成深度不可变对象。

## 查询与状态选择器

```java
modularUI.getElementById("save");
root.selectId("save");             // 原始 id，不带 #
root.select("#save");              // CSS 选择器
root.select("button:hover");       // 运行时 __hovered__
root.select("button:disabled");    // 运行时 __disabled__
root.select("text-field:focused"); // 运行时 __focused__
```

2.2.36.a 的伪类转换只有 `:hover` 会兼容到 `__hovered__`。焦点实际添加 `__focused__`，因此
使用 `:focused`/`.__focused__`，不要使用会匹配 `__focus__` 的 `:focus`。

命中测试：`element.hitTest(mouseX, mouseY)` 会处理元素 transform，并返回命中的元素与子索引。
`setAllowHitTest(false)` 跳过元素自身，但仍递归检查可命中的子元素。

## 生命周期

- 挂载：`ModularUI.init(w,h)` 把 Root 递归挂入运行时、初始化 Screen、标记样式/布局并计算首帧。
- 每帧：动画更新，Style 与 Taffy 布局按脏标记计算，刷新 `__hovered__` 链，再绘制。
- 每 tick：客户端 `ModularUI.tick()` → Root `screenTick()`；服务端路径使用 `tickServer()`。
- 卸载：`ModularUI.onRemoved()` 通知元素并释放 StyleEngine；Screen 还应 `setScreen(null)` 并清字段。

普通项目页面使用 `StarboundModularScreen`；只有确实需要特殊 render/drag 行为时才手动实现以上流程。

## 布局 API

`UIElement.layout(...)` 接收 `Consumer<gui.ui.style.LayoutStyle>`。常用 Java 方法包括：

- `width/height/minWidth/maxWidth/widthPercent/heightPercent`；
- `flex/flexDirection/flexGrow/flexShrink/gapAll/paddingAll/margin*`；
- `alignItems/justifyContent`；
- `positionType(TaffyPosition.ABSOLUTE)` + `left/top/right/bottom`；
- `aspectRatio(float)` / `aspectRatioAuto()`；
- grid 系列方法。

底层实现才是 `gui/ui/layout/TaffyLayoutStyle`。LSS 对应属性使用连字符，其中纵横比属性名是
`aspect-rate`，不要把它误写成 Java 方法 `aspectRate()`。

布局 setter 只标脏，计算发生在渲染帧。不要在 draw 中持续写入布局；日志提示一帧超过 10 轮
dirty layout 时，应检查 style/layout 回调是否互相触发。

## 精确绘制顺序

`UIElement.drawInBackgroundInternal` 的可见绘制顺序是：

1. `drawBackgroundTexture(context)`；
2. `drawContents(context)`，其内部先调用 `drawBackgroundAdditional(context)`；
3. 绘制 children；
4. `drawBackgroundOverlay(context)`。

因此：

- 背景几何或位于子组件下方的矢量内容放 `drawBackgroundAdditional`；
- 必须盖住自身 children 的内容放 `drawBackgroundOverlay`；
- 跨组件覆盖层优先使用独立高 z-index 元素；
- `setOverflowVisible(false)` 会影响绘制裁切和子元素命中范围。

自绘钩子中通过 `GUIContext.graphics` 取得 `GuiGraphics`。切换 pose、批次或自定义 RenderType 时按需
`flush()`，但不要把每个小图元都无条件 flush。连续运动使用 tick 状态 + `context.partialTick` 插值，
不要只在整数 tick 位置绘制。

## 动画

```java
ISubscription subscription = target.animation()
        .duration(0.15F)
        .ease(Eases.QUAD_OUT)
        .style(PropertyRegistry.OPACITY,
                FloatObjectPair.of(0F, from), FloatObjectPair.of(1F, to))
        .start();
```

- `StyleAnimation.start()` 在目标尚未挂载、`ModularUI == null` 时返回 no-op subscription。
- 普通 StyleAnimation 完成时会把最后关键帧写回配置的目标 origin；无需笼统地再次回写终态。
- 如果未挂载时业务上也必须立即显示终态，应显式写 style，因为 no-op 动画不会替你改变它。
- 保存 subscription；重播或销毁前 `unsubscribe()`，避免多个动画同时写同一属性。
- 动画层级高于内联样式。调试“样式为什么没生效”时检查当前动画和 `transition`。

官方动画示例：`test/ui/TestAnimation.java`；项目封装：`client/starmap/StarmapUiAnimations.java`。

## 特殊开屏方式

1. **项目普通客户端构树**：`StarboundModularScreen`，Root 只在客户端创建，Menu/Packet 传业务数据。
2. **LDLib2 服务端托管 Block UI**：实现 `BlockUIMenuType.BlockUI` 或 BE 的 UI provider，使用
   `BlockUIMenuType.openUI(serverPlayer, pos)`；同类还有 HeldItem/Player UI。
3. **LDLib2 容器体系**：`ModularUIContainerMenu` + `ModularUIContainerScreen`，用于其
   `IContainerUIHolder/IModularUIHolder` 契约。
4. **HUD**：`gui/hud/ModularHudLayer.java`，注册到 NeoForge GUI layer。
5. **原生窗口**：`gui/ui/window/ModularUIWindow.java`，主要供编辑器；普通模组 Screen 不使用。

不要因“库支持服务端模板”就迁移现有项目网络架构。先判断是否真的需要双方维护同一棵 UI 树。

## XML 与工具

- XML 入口为 `UI.of(Document)`，schema 在 `LDLib2/ldlib2-ui.xsd`。
- 标签名来自 `@LDLRegister(name=..., registry="ldlib2:ui_element")`。
- 通用属性包括 `id/class/style/visible/focusable/active`；样式表可内联或用 `<stylesheet>` 引用。
- 静态、编辑器产出的 UI 才优先考虑 XML；本项目多数运行时数据页面使用 Java 元素树。
- 调试器入口：`modularUI.enableDebugger(true)`；实现位于 `gui/ui/debugger/UIDebugger.java`。

## 快速查源码

```powershell
rg -n "class Button|setOnClick" LDLib2/src/main/java/com/lowdragmc/lowdraglib2/gui
rg -n "@LDLRegister.*ui_element" LDLib2/src/main/java/com/lowdragmc/lowdraglib2/gui
rg -n "PropertyRegistry\.|create\(\"" LDLib2/src/main/java/com/lowdragmc/lowdraglib2/gui/ui
rg -n "new Button|new ScrollerView" LDLib2/src/main/java/com/lowdragmc/lowdraglib2/test/ui
```

官方可运行示例位于 `test/ui/`；端到端测试说明位于 `LDLib2/uitest.md`。
