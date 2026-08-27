# 星图重绘优化方案（实现文档）

> **归档状态：已完成。** 本文记录重构方案与实现依据。方案已于 2026-08-26 完成；最终
> 结构以当前源码和同目录的 [`starmap-redraw-todo.md`](starmap-redraw-todo.md) 为准。
> 重构保持已评审的公转速度模型与现有服务端跃迁逻辑。

## 0. 背景与范围

实施前的 `StarmapTerminalRoot.java` 一度达到 1096 行，并集中状态机、重复近邻命中和场景
绘制。实施后根元素为 743 行，场景、节点、选框、转场、边缘提示、信息板、动作判定和
静态星空缓存均有独立组件；根元素只保留跨组件状态编排、坐标模型和事件协调。已实现收益：

- 删除全部手写近邻命中测试（约 49 行）。
- 用 LDLib2 绘制原语替代逐点 `fill`（轨道、虚线航道、角标）。
- 把硬编码颜色迁入 LSS，拿到 hover/选中/disabled 态和过渡动画。
- 用 LDLib2 `UIElement`、`Label`、`Button` 和 Taffy 布局替代手绘信息板命中面板。

**硬约束（不得改动）：**
1. `StarmapOrbitMotion` 的 `sqrt(52/r)` 速度衰减和月亮 `1.45×` 系数（`starmap/StarmapOrbitMotion.java` L21-35）——已经过评审。
2. 命中逻辑与绘制必须公用同一个相位模型（`phase()`/`moonPhase()`），否则命中框和视觉点会错位（需求第 4 节明确要求）。
3. 跃迁仍走 `ModNetwork.sendToServer(new StartWarpPacket(...))`（现 `StarmapTerminalRoot.performAction` L318-327），UI 不创建第二套航行状态、不绕过服务端校验。

## 1. 目标组件结构

对应同目录 [`starmap-redraw-requirements.md`](starmap-redraw-requirements.md) 第 6 节推荐结构：

```text
StarmapTerminalScreen                       // AbstractContainerScreen（保持现状）
  └─ StarmapTerminalRoot                    // UIElement：仅做 level 状态机 + 层级切换
       ├─ StarmapSceneElement               // 星空缓存、航道和轨道导引线
       ├─ nodeLayer
       │    └─ StarmapNodeElement[]         // 节点绘制、命中和事件停止冒泡
       ├─ StarmapSelectionOverlayElement    // 连续坐标选中角标
       ├─ StarmapTransitionOverlayElement   // 层级短转场
       ├─ StarmapChromeElement              // 边框、标题、固定提示与视野按钮
       └─ StarmapInfoPanelElement           // LDLib2 信息板和操作按钮
```

### 拆分原则

- `StarmapTerminalRoot` 保留：`StarmapLevel level` 状态机、`goBack()`（L96-105）、`performAction()`（L318-327）、`isWarpAvailable()`（L329-337）、`levelLabel()`。这些是跨节点的逻辑，保留在根上;子元素通过根的方法反向查询当前选中态/燃料态。
- 删除 `StarmapTerminalRoot` 里的：`drawStars`/`drawGalaxy`/`drawSystem`/`drawPlanet`/`drawChrome`/`drawInfo`/`drawStarNode`/`drawBodyNode`/`drawCornerMarks`/`drawOrbit`/`drawDashedLine`/`galaxyPoint`/`orbitPoint`/`systemPoint`/`nearestSystem`/`nearestSystemEntry`/`nearestPlanetTarget`/`isInsideAction`/`selectedTarget`。
- `drawStars`、`drawChrome`、`drawInfo` 的绘制逻辑分别迁移到 `StarmapSceneElement`、`StarmapEdgeChrome`、`StarmapFloatingInfo`。

## 2. 具体改动步骤

### 步骤 1：坐标与命中交给框架（P0，最大收益）

为每颗恒星 / 行星 / 卫星创建子 `UIElement`，各自注册 `MOUSE_DOWN` / `CLICK` 监听器。LDLib2 会把 `MOUSE_DOWN` 派发给**光标下最深处的元素**（`ModularUIWidget` 的 `lastHoveredElement`），「点中哪个目标」由 `hitTest` / `isMouseOver` 免费给出。

- 每个节点元素覆写 `drawBackgroundAdditional(GUIContext)` 画自身图形；其 `getPositionX/getSizeWidth` 由布局算出。
- 公转时：节点物理位置随 `UIEvents.TICK` 更新 `orbitClock`，节点元素在绘制/更新时重算位置。可让父 `StarmapSceneElement` 负责在每帧把子节点的 position 设为 `center + rotate(radius, phase)`，这样 `hitTest` 实时跟随公转。
- 按钮不再手算矩形：信息板里的跃迁按钮建一个子 `UIElement`，其 `MOUSE_DOWN` 处理跃迁;「点空白区域取消选择」由根元素处理所有未被节点捕获的点击。

删除所有 `nearestSystem`/`nearestSystemEntry`/`nearestPlanetTarget` 手写近邻搜索。坐标换算用 `context.localMouseX/localMouseY`（`GUIContext` L54）而非 `event.x - getPositionX()`。

### 步骤 2：用 LDLib2 绘制原语替换逐点 fill（P1）

- `drawOrbit`（现 L421-429，逐点 `fill` 圆）→ `DrawerHelper.drawLines`（L264）或 `RenderBufferUtils.drawColorLines`（L386，带线宽、抗锯齿的渐变折线）。
- `drawDashedLine`（现 L431-443）→ 同上，用 LDLib2 折线原语。
- `drawCornerMarks` / `drawChrome` 边框（L246-257 / L211-220）→ `ColorBorderTexture` 经 `style().backgroundTexture()` 渲染，或 `DrawerHelper.drawBorder`（L110）。
- `drawStarNode` / `drawBodyNode` → `guiContext.drawTexture(IGuiTexture)`，用 `ColorRectTexture` / `SDFRectTexture` / `ColorPattern`。体上的过程式球面高光复用现有 `StarmapBodyShading`（`client/StarmapBodyShading.java`，已实现定向光照）。

### 步骤 3：复用行星缩略图

`StarmapBodyVisual` 已配好 `texture` / `focusTexture`（`textures/gui/starmap/bodies/*.png`），让行星节点元素把它们作为 `IGuiTexture` 使用。选中态用 `focusTexture`（或 LSS `__hovered__` / 自定义类切换贴图）。

### 步骤 4：颜色与样式迁入 LSS

把 `StarmapTerminalRoot` L27-35 的 9 个 `static final int` 颜色常量迁移成 LSS 等级选择器：

```text
.starmap-redraw-root          // 根背景（已存在，保留）
.starmap-panel                // 信息板底板
.starmap-orbit                // 普通轨道
.starmap-orbit-selected       // 选中轨道
.starmap-action-button        // 跃迁按钮
.starmap-action-button[disabled]  // 燃料不足 / 跃迁中 / 不可达
.starmap-action-button:hover  // 悬停
.starmap-info                 // 信息文字
.starmap-level-marker         // 层级提示
```

- 用 `addClass("...")` 给元素打类，LSS 规则自动匹配。
- `__hovered__`、`selected`、`disabled` 这些态尽量用纯 CSS + `BasicStyle.transition()` 控制，白拿过渡动画。
- **同时删除** `StarmapTerminalRoot.drawBackgroundAdditional` 里手画背景的那一行（现 L114 `graphics.fill(x, y, x+width, y+height, BACKGROUND)`），它把 LSS 的 `background: sdf(#050912, 0)` 覆盖掉了。

### 步骤 5：tooltip 与动画

- 各星体节点注册 `addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(...), null, null, null))`;框架自动渲染。替换 `drawInfo` 手绘命中面板。
- 层级提示「右键返回恒星系」「右键返回星域」也用 `HOVER_TOOLTIPS`（对应 `gui.starboundmc.starmap.redraw.back_hint`）。
- 层级切换 / 信息板滑入 / 选中高亮用 `element.animation(a -> a.duration(0.2f).ease(Eases.QUAD_IN_OUT).style(PropertyRegistry.COLOR, start, end).start())`（`UIElement.animation()` L1104，`StyleAnimation`）。

### 步骤 6：性能

- `drawStars`（现 L127-146）每帧 `new Random(0x5EEDL)` 逐点画星。缓存成一张静态 `NativeImage` + `DynamicTexture`（复用 `StarmapOverlaySprites` 的超采样 sprite 模式），或用 LDLib2 的 `IGuiTexture` 贴图一次性绘制。
- 星空背景不随缩放/平移变化，可直接用贴图或由 `StarmapEdgeChrome` 之类一次性绘制层承担。

## 3. 保留 / 删除对照

**保留在根：** `level` 状态机、`goBack()`、`performAction()`、`isWarpAvailable()`、`levelLabel`。

**删除（迁移或不再需要）：**
- `drawStars` → `StarmapSceneElement` / 缓存贴图
- `drawGalaxy` / `drawSystem` / `drawPlanet` → `StarmapSceneElement` 三层绘制器
- `drawChrome` / `drawCornerMarks` → `StarmapEdgeChrome`
- `drawInfo` → `StarmapFloatingInfo` + `HOVER_TOOLTIPS`
- `drawStarNode` / `drawBodyNode` → 节点子元素各自绘制
- `drawOrbit` / `drawDashedLine` → `DrawerHelper.drawLines` / `RenderBufferUtils.drawColorLines`
- `galaxyPoint` / `orbitPoint` / `systemPoint` → 节点 position 计算 + `context.localMouse*`
- `nearestSystem` / `nearestSystemEntry` / `nearestPlanetTarget` / `isInsideAction` / `selectedTarget` → 框架事件派发（全部删除）

## 4. 验证清单

实现后必须满足（对应同目录 [`starmap-redraw-requirements.md`](starmap-redraw-requirements.md)
第 9 节验收标准）：

- [x] 三个层级均可进入、返回，右键返回（深空星域忽略右键）和 `Esc` 关闭行为不变。
- [x] 恒星系层级不能独立选中卫星;行星系统层级可以独立选中卫星。
- [x] 行星、卫星公转连续，卫星跟随父行星，**命中区域随公转实时移动**。
- [x] 信息板不遮挡目标、包含描述与对应层级的天体数量;跃迁按钮触发服务端权威请求。
- [x] 轨道、航道、星体在不同宽高比 / GUI Scale 下不拉伸错位。
- [x] `StarmapOrbitMotion` 保留 `sqrt(52/r)` 与卫星 `1.45×` 模型，并将长时间相位归一化
      以保持 sub-tick 精度。

## 5. 风险与注意

- **相位一致性**：节点 position 和绘制必须用同一个 `StarmapOrbitMotion.phase()/moonPhase()`。建议把「给定 `orbitClock` 求某 entry 的世界坐标」抽成 `StarmapSceneElement` 里的单一静态方法，绘制与命中都调它。
- **依赖路径**：`LDLib2/` 是本机一个独立 NeoForge userdev 老工程（13730 个 Java 文件），而 `build.gradle` 的 LDLib2 依赖指向 FirstDark maven（`implementation com.lowdragmc.ldlib2:ldlib2-neoforge-1.21.1:2.2.36.a`）。实现时确认开发用哪个路径，否则改 `LDLib2/` 不会反映到测试运行。
- **不要动** `StarmapLevel`、`StarmapOrbitMotion`、`PlanetEntry`/`StarSystem`/`StarSystems` 的数据契约。
- 重构是渐进式的：可先落地步骤 1（拆子元素 + 删近邻搜索），再逐步替换绘制原语和 LSS。每一步都应能编译、能跑 `.\gradlew.bat runClient`。

## 6. 参考的 LDLib2 关键位置（路径相对 `LDLib2/src/main/java/com/lowdragmc/lowdraglib2`）

| 能力 | 位置 |
| --- | --- |
| `MOUSE_DOWN` 派发给最深元素 | `gui/ui/ModularUI.java` `ModularUIWidget`（L961-986） |
| `getLocalMouse` / `hitTest` / `isMouseOver` | `gui/ui/UIElement.java`（L1290-1399） |
| `drawBackgroundAdditional`（惯用绘制钩子） | `gui/ui/UIElement.java`（L1944-1946） |
| Layout DSL（`widthPercent`/`flex`/`gap`/`padding`） | `gui/ui/style/LayoutStyle.java` |
| `HOVER_TOOLTIPS` + `HoverTooltips` 注册范式 | `gui/ui/event/HoverTooltips.java`、`gui/ui/elements/ItemSlot.java`（L175） |
| `animation()` / `StyleAnimation` | `gui/ui/UIElement.java`（L1104）、`gui/ui/style/animation/StyleAnimation.java` |
| 折线 / 渐变原语 | `gui/util/DrawerHelper.java` `drawLines`（L264）、`client/utils/RenderBufferUtils.java` `drawColorLines`（L386） |
| 纹理 / 颜色原语 | `gui/texture/IGuiTexture.java`、`SDFRectTexture.java`、`gui/ColorPattern.java`、`gui/texture/Icons.java` |
| 现成自定义绘制元素参考 | `gui/ui/elements/GraphView.java`（用 `getContentX()` + 颜色样式） |
