# 高级参考：Configurator 与编辑器框架

路径相对 `LDLib2/src/main/java/com/lowdragmc/lowdraglib2/`。基于 2.2.36.a 验证。
仅在任务明确要求自动配置面板、Inspector 或游戏内编辑器时读取；普通机器 UI 不需要本参考。

## A. @Configurable 注解 → 自动检查器面板

给类字段加注解，即可生成 Unity Inspector 风格的可编辑面板，值直接回写字段。

### 注解集（configurator/annotation/）

| 注解 | 用途 |
| --- | --- |
| `@Configurable` | 核心注解（字段+类型）。`tips/collapse/key/subConfigurable(嵌套子组)/persisted` 等 |
| `@ConfigNumber(range={min,max})` | 数值范围滑条/输入；`type` 可强制 INTEGER/FLOAT 等 |
| `@DefaultValue` | accessor 提供的默认值 |
| `@ConfigSetter(field="...")` | 用 setter 方法替代反射直写 |
| `@ConfigList(configuratorMethod=..., addDefaultMethod=...)` | 集合字段的增删排序编辑 |
| `@ConfigSelector(candidate={...})` / `@ConfigSearch(searchConfiguratorMethod=...)` | 下拉候选 / 搜索选择 |
| `@ConfigRL(value=FONT|ITEM_TAG_KEY|...)` | ResourceLocation 选择器 |
| `@ConfigColor` / `@ConfigHDR` | 颜色选择器标记 |
| `@ConfigHeader("标题")` | 分节标题行 |

没有可见性条件参数；持久化由 `persisted=true` 默认联动 PersistedParser。

### 打开面板的三种方式

```java
// 1) 类实现 IConfigurable + IPersistedSerializable（可存档、可选历史）
public class MyConfig implements IConfigurable, IPersistedSerializable {
    @Configurable @ConfigNumber(range = {0, 1000})
    private int fuel = 100;
}

// 2) 通用检查器元素（gui/ui/elements/Inspector.java，注册名 "inspector"）
new Inspector().inspect(configurable, changeListener, onClose, historyAction);

// 3) 手动组装进任意屏幕（test/ui/TestConfigurators.java 范式）
var root = new ScrollerView();
var group = new ConfiguratorGroup("root");
group.setCollapse(false);
configurable.buildConfigurator(group);           // 反射扫描注解生成控件
root.addScrollViewChild(group);
return new ModularUI(UI.of(root));
```

值**即时回写**字段（或经 `@ConfigSetter`），无需显式读取；变化经
`Configurator.CHANGE_EVENT` 冒泡事件通知。存档用 `IPersistedSerializable`
或 `PersistedParser.createCodec(Supplier)` 生成的 Codec。

### 支持的字段类型

原生类型/String/enum、Block(Item/Fluid/EntityType)、BlockPos/BlockState/AABB、
ResourceLocation、ItemStack/FluidStack、Component/CompoundTag、Range/Position/Size/
HDRColor/Transform/向量族(2f-4i)、IGuiTexture/IRenderer。数组/集合自动包装。
自定义：实现 `IConfiguratorAccessor<T>` + `@LDLRegisterClient(registry="ldlib2:configurator_accessor")`。

### 撤销/历史

- `EditAction.of(doRun, undoRun)` + `IHistoryStack.pushHistory(...)`；
- 快照式：`SerializableRecordAction`（NBT 快照）+ `IConfigurable.createHistoryRecorder()`
  （类需实现 INBTSerializable 才有历史）；
- 现成栈 UI：`editor/ui/view/HistoryView.java`（Inspector 每次改动自动入栈）。

## B. 编辑器框架（editor/）——结构概览

Unity/Blender 式游戏内工具框架，UI 编辑器（`gui/editor/UIEditor`，`/ldlib2 ui_editor` 命令打开）
就是它的第一个应用。

- **Editor**（abstract）：菜单栏 + 五个可分割锚点窗口 + 内置 InspectorView/ResourceView/
  HistoryView + 布局存取（`<storeDir>/<projectType>.nbt`）。子类只需实现
  `createNewEditorInstance()`；参考最小样例 `gui/editor/UIEditor.java`（约 40 行）。
- **项目模型**：`IProject`(INBTSerializable) + `ProjectType`(后缀/图标/创建器)，默认 NBT 存取开箱即用；
  UI 编辑器的项目格式是 **XML 文本**（schema 为仓库根 ldlib2-ui.xsd）。
- **资源系统**：`editor/resource/Resource.java` 抽象类型化资源，默认扩展名 `.<name>.nbt`
  ——可复用控件模板即 `.ui.nbt`（内容是 UITemplate 的 CompoundTag）。自带文件/包/内置三种 provider，
  浏览器 UI 在 `editor/ui/browser/AssetBrowser.java`。示例模板在
  `LDLib2/src/main/resources/assets/ldlib2/resources/examples/*.ui.nbt`。
- **可停靠视图**：`View`/`ViewContainer` + `Editor.placeView(...)`；拖出的浮动窗口走
  `FloatingViewManager`（优先原生 OS 窗口）。设置系统 `Settings extends IConfigurable`。

### 使用边界

不要依据“公开类”或缺少 `@ApiStatus` 就断言 API 稳定。2.x 仍可能演进；依赖版本变化、首次使用
某扩展点或现有签名无法从项目代码确认时，再以实际依赖版本源码核对。优先依赖示例中使用的 `IConfigurable`、`Inspector`、
`IProject/ProjectType`、`View/placeView` 等入口，避免直接操作编辑器内部窗口/布局实现。
