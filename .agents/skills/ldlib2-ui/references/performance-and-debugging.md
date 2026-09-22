# 性能与调试

本参考区分库级冷启动、项目建树/样式/布局成本和持续渲染成本。不要把“第一次卡一下”或“每帧不稳”
归成同一个问题。

## 先按时间特征分类

| 表现 | 优先检查 |
| --- | --- |
| 本次会话第一个 LDLib2 UI 卡，随后星图/传送器都顺 | 字体、字形图集及共享客户端资源冷启动 |
| 只有某个页面第一次卡，其他 LDLib2 页面已打开过 | 页面建树、资源解析、首次数据包后重建、特殊纹理 |
| 每次打开都卡 | 构造树规模、同步阻塞、重复资源加载、未缓存计算 |
| 选择一行/收到包时卡 | `clearAllChildren` 全量重建、样式注册和 Taffy 重算 |
| 界面打开后持续掉帧 | draw 中分配/布局写入、动态图元数量、过度 flush、动画叠加 |
| 动画跳动但平均帧率正常 | tick-only 坐标、插值/transform 不一致、整数取整过早 |

## 已知首次打开问题

项目 `docs/known-issues.md` 已记录：本次会话首次打开 LDLib2 页面会短暂卡顿。LDLib2 字体系统的
font set、atlas page 和 glyph bake 存在懒初始化路径，首次出现中英文字符时可能集中产生工作。

不要笼统声称“首次打开才编译 SDF shader”；2.2.36.a 的 shader 在客户端 shader 注册事件中注册。
只有 profiling/日志证明某个 SDF 资源首次创建昂贵时，才把它列为具体原因。

先运行能区分当前主要假设的最小实验。只有仍无法区分共享冷启动和页面接入层问题时，才按以下完整顺序测试：

1. 干净启动后先开传送器，记录第一次与第二次；
2. 再干净启动，先开星图，再开传送器；
3. 反向测试先传送器再星图；
4. 比较同一语言和同一 GUI Scale；
5. 若“谁第一个开谁卡”，优先判断共享冷启动；若始终只有一个页面卡，查项目接入层。

预热是最后手段。只有确认共享冷启动且预热不会延长关键加载阶段时，才在进入世界后的空闲时段加载
常用字体/字符或 UI 资源；必须比较总加载成本，而不是把卡顿藏到另一个时刻。

## 避免重复建树

Root 构造时建立稳定骨架。数据更新时：

- Label：`setText`；
- Button：`setActive`、增删状态 class；
- 显隐：`setDisplay` 或动画；
- 图片：更新已有元素的 background texture；
- 列表：按稳定 key 增删/更新受影响的行。

避免在每 tick、每次 hover、每次选择或每个数据包中：

- `clearAllChildren()` 后重建整页；
- 重复 `addEventListener`；
- 重复解析同一 ResourceLocation/创建相同贴图；
- 给所有未变化元素重设 layout/style。

传送器当前已知曾在初始客户端缓存、服务器数据包到达和选择变化时全量刷新目标列表；优化前先检查
工作树现状，保留 selection/scroll position，并改为 keyed 更新。不要把历史实现写成永久事实。

## 样式与布局

- 布局和 style setter 会标脏；只在值真实变化时调用。
- 不在 `drawBackgroundAdditional`、`drawBackgroundOverlay` 或每帧 render 中改 layout/class。
- 一帧布局循环超过 10 次会写警告；看到警告时检查 layoutChanged/styleChanged 回调是否互相写回。
- 动态位置优先由单一 view transform 或 pose translate 实现，不为每个移动节点持续改 Taffy 布局。
- 频繁显示/隐藏大量元素时，确认 `setDisplay` 与 opacity 动画没有同时争用布局和可见性。
- Stylesheet 选择器保持页面作用域清晰，但不要堆叠无必要的深层后代选择器。

## 绘制与资源

- 静态星空、网格、轨道采样点可缓存几何/随机结果；不要每帧重新撒随机星点。
- 连续动画在 tick 中推进 double 状态，渲染时加 partialTick；最后绘制前再取整。
- 选择框和目标星体共享同一插值位置/transform，避免各自计算造成一帧错位。
- 批量图元后按需要 `GuiGraphics.flush()`；每个小线段都 flush 会破坏批处理。
- 缓存解析后的 ResourceLocation/IGuiTexture；动态纹理、订阅和外部资源在 removed 时释放。
- shader、Scene、动态图像等昂贵能力必须有明确视觉收益，并在目标显卡环境验证。
- `SDFRectTexture` 绘制会刷新批次；大量小型 SDF 列表行和文字框可能比简单 sprite、rect、
  border 更贵。保留 SDF 给真正需要平滑圆角/描边的主要表面，并用实测决定。
- 大型子树 opacity < 1 或复杂 `overflow-clip` 会增加合成成本；普通矩形裁切优先使用
  `setOverflowVisible(false)` 的 scissor 路径。

## 调试工具

- `modularUI.enableDebugger(true)`：检查元素树、最终样式和布局。
- `hitTest(x,y)`：确认真实 target 与透明覆盖层。
- `modularUI.getLastHoveredElement()`：查看缓存事件目标；`hitTestAtScreen(x,y)` 可只查询屏幕坐标命中。
- 日志：搜索 unknown LSS property、dirty layout 超限、缺失资源和 shader/font 错误。
- `rg`：从控件实现查默认值、内部 class 和监听事件，不根据文档摘要猜。
- 官方测试屏：`/ldlib2_screen_test <name>`；只在开发环境按官方说明使用。

## 交付性能结论

报告至少说明：

- 冷启动还是稳定态；
- 首次/第二次打开的测试顺序；
- 归因证据来自日志、代码路径还是体感；
- 项目侧已减少了哪些重建/每帧工作；
- 剩余成本是否属于 LDLib2 上游候选问题。

没有 profiling 或对照实验时使用“初步判断”，不要把相关性写成已确认根因。
