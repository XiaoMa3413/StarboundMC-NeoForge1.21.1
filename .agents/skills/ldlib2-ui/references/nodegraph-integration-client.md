# 高级参考：节点图、集成层与客户端基建

路径相对 `LDLib2/src/main/java/com/lowdragmc/lowdraglib2/`。基于 2.2.36.a 验证。
仅在任务明确涉及 NodeGraph、KubeJS/JEI·REI·EMI 集成、Scene/shader 或原生窗口时读取。

## A. nodegraphtookit——游戏内节点图编辑器

Unity Shader Graph 风格的可视化编程编辑器。**库内没有图执行器**：它提供创作 API、
文档模型与编辑器 UI，运行时求值语义由 Mod 自行实现（全包 grep 无 evaluate/execute 语义）。

### 分层结构

| 层 | 包 | 内容 |
| --- | --- | --- |
| 创作 API | `nodegraphtookit/api/` | `Graph` 基类（`getSupportNodes()`）、`Node/INode` 族（BlockNode/ContextNode/ConstantNode/VariableNode/SubgraphNode）、`@NodeAttribute` 注解、端口/选项构建器（`IPortDefinitionContext/OptionBuilder`） |
| 文档模型 | `model/` | GraphModel + 节点/端口/连线/变量声明/分组(Section)/便签(StickyNote)/子图(SubgraphNodeModel) 等可序列化模型 |
| 编辑器命令 | `gui/command/` | 可撤销命令族：GraphCommands/WireCommands/NodeCommands/VariableDeclarationCommands（含"从选区创建子图""导入外部子图"） |
| 黑板 | `gui/blackboard/Blackboard*` | 变量面板 UI |
| 编辑器集成 | `editor/` | `GraphEditorView`（挂进 editor 框架的视图）、`GraphResource`（图资产接入 Resource 系统） |

### 定义自定义节点（GraphNodeRegistry.java javadoc 范式）

```java
public class MyGraph extends Graph {
    public static final GraphNodeRegistry NODE_REGISTRY =
            GraphNodeRegistry.create(MyMod.id("my_graph"), MyGraph.class);

    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        return NODE_REGISTRY.getNodeClasses();   // 启动期扫描 @NodeAttribute 类
    }
}

@NodeAttribute(...)   // 可带 modID/environment/graphTypes 过滤
public class MyNode extends Node { /* 用 PortBuilder/OptionBuilder 声明端口 */ }
```

不要根据公开可见性或缺少内部标记断言 API 稳定。依赖版本变化、首次使用某扩展点或签名存疑时，
再核对官方测试和扩展点；风险较高且没有现成用例时才做最小概念验证。图资产可经 `GraphResource` 接入编辑器资源系统，编辑器视图使用
`GraphEditorView`。

## B. integration

- **kjs/**：`LDLibKubeJSPlugin` 向 KubeJS 暴露绑定与事件组；`ui/KJS{Block,HeldItem,Player}UIMenuType`
  让脚本直接打开三类托管 UI；`UIEventJS/UIEvents` 提供脚本侧 UI 事件。
- **xei/**：让 ModularUI 控件嵌入 JEI/REI/EMI 配方界面——每家一个 Plugin 入口
  （LDLibJEIPlugin/LDLibREIPlugin/LDLibEMIPlugin）+ `ModularUI*Widget/Hanlders`（拖放、
  配方槽位点击信息 `IngredientIO`）。Mod 想在配方类别里用 LDLib2 控件时依赖此桥。
- **moddevmcp/**：**整个包被注释禁用**——为外部 MCP 工具（AI 辅助开发）预留的 UiDriver，
  计划暴露 snapshot/query/inspect/action 操作。当前不可用，仅作了解。

## C. client 包基建速查

| 子包 | 内容 |
| --- | --- |
| `font/` | 自研字体渲染栈：LDFontManager、字形图集(LDGlyphAtlas)、skyline 打包、文本布局缓存 |
| `renderer/` | `IRenderer` 抽象 + `IBlockRendererProvider/IItemRendererProvider/ATESRRendererProvider`：以 provider 方式接管方块 BER 与物品 ISLER |
| `shader/` | LDLibShaders/LDShaderInstance/LDPostChain/LDLibRenderTypes/HDRTarget：着色器包装与自定义 RenderType |
| `scene/` | GUI 内嵌世界渲染：WorldSceneRenderer（FBOWorldSceneRenderer/ImmediateWorldSceneRenderer）、CameraEntity、场景级粒子管理——`Scene` 元素的底层 |
| `window/` | OsWindow* 族：原生 GLFW 窗口宿主（OsWindowHost/OsWindowManager），编辑器浮动窗口用它 |
| `utils/` | RenderBufferUtils（画线等缓冲工具）、MeshDataSorter、ShaderUtils |

开发命令：`/ldlib2_screen_test <name>`、`uitest` 场景运行器；
dev-only 字体调试命令。服务端 `/ldlib2 ui_editor` 打开 UI 编辑器。
