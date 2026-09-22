# 文本、布局与人体工程学

本参考处理 StarboundMC UI 中最常见的文字错位、节点名显示不全、悬停文字飘出界面、框过多和
按钮难用等问题。API 基线为 LDLib2 2.2.36.a。

## TextWrap 的真实行为

| 模式 | 行为 | 建议 |
| --- | --- | --- |
| `NONE` | 不按容器宽度换行 | 已知短文本或允许元素自适应宽度时使用 |
| `WRAP` | 按 content width 形成多行 | 正文/说明；配 `adaptiveHeight(true)` 或明确足够高度 |
| `HIDE` | 只绘制格式化后的第一行 | 紧凑列表/名称；不会自动加省略号，配完整 tooltip |
| `ROLL` | 超宽时持续横向滚动 | 仅专用、明确要求的 ticker 区域 |
| `HOVER_ROLL` | hover 时横向滚动 | 默认避免；运动和裁切稍有错误就会飘出 UI |

`adaptiveWidth`、`adaptiveHeight` 默认都是 false。只写 `WRAP` 不会自动让固定高度容器变高。
`ROLL/HOVER_ROLL` 使用系统时间计算滚动相位，鼠标刚移入时可能直接跳到中间位置，并不会从头开始。

### 紧凑单行文本

```java
label.setOverflowVisible(false);
label.layout(l -> l.widthPercent(100).height(9));
label.textStyle(s -> s
        .adaptiveWidth(false)
        .textWrap(TextWrap.HIDE));
label.style(s -> s.tooltips(fullComponent));
```

优先让布局和 `HIDE` 控制可见范围，不要用 Java 字符数 `substring` 猜显示宽度；中英文、格式字符和
不同字体的像素宽度不同。确实需要省略号时，按当前字体渲染宽度测量后截断。

### 多行说明

```java
description.setOverflowVisible(false);
description.layout(l -> l.widthPercent(100).minHeight(18));
description.textStyle(s -> s
        .adaptiveWidth(false)
        .adaptiveHeight(true)
        .textWrap(TextWrap.WRAP));
```

父容器也必须允许增长或滚动，否则子元素自适应高度仍可能被父级固定高度裁掉。若页面高度固定，
将说明区域放入 ScrollerView，而不是减小到难以阅读的字号。

### ROLL/HOVER_ROLL 的硬条件

只有用户明确需要滚动文字时才使用，并同时满足：

1. 文本元素 `setOverflowVisible(false)`；
2. 所有可能越界的父层也有正确裁切；
3. 不与列表滚动、hover 选中或按钮动画争夺注意力；
4. 离开 hover 后位置复位且不会覆盖其他元素；
5. 中英文长文本和边缘列表行已实机验证。

若任何一项做不到，改用 HIDE + tooltip 或 WRAP。

## 字符串与本地化

- 翻译键：`setText(Component.translatable("gui.starboundmc.key"))`。
- 动态字面值：`setText(Component.literal(value))` 或 `setText(value, false)`。
- `TextElement.setText(String)` 默认 `translate=true`，直接传玩家输入或节点名会被当作翻译键。
- 句子使用完整翻译键和占位参数，不在 Java 中拼接中英文语序。
- 按钮不要只为中文宽度设计；英文通常更长，专有名词和玩家自定义名称可能远长于两种语言。

## 布局策略

- 优先 flex、min/max、gap 和 padding；绝对定位只用于画布节点、覆盖层或确实固定的装饰。
- 文字行高、容器高度和垂直居中一起检查。仅增加容器高度并不能修复错误 baseline/对齐。
- 列表文字列使用 `flex(1)` 或明确剩余宽度，图标和箭头使用固定宽度；不要让所有列都抢宽度。
- `setOverflowVisible(false)` 同时影响绘制和子元素命中范围，裁切后检查 tooltip/下拉层是否仍可用。
- 不通过持续修改 left/top 来跟随动画对象；让选择框和对象共享同一插值位置或父级 transform。

## 信息层级与框的使用

LDLib2 能画框不代表每段内容都需要框。

- 框用于：可点击项、输入框、明确分组、选中/警告状态、需要与背景分离的预览。
- 普通标题、字段名、说明和状态文字优先用字号、明度、间距和对齐建立层级。
- 页面分区数量和结构由已确定的设计/信息架构决定；本参考只要求避免无意义的“框中框中框”，不规定机器页必须采用列表、预览或固定三段式布局。
- 主操作放在稳定、易触达的位置；危险/不可逆动作与普通按钮分离并有确认或明显警示。
- 空状态告诉玩家下一步做什么，不只显示“无数据”。

## 点击目标与操作密度

以下是项目人体工程学启发式，不是 LDLib2 API 限制：

- 普通 Button 不低于库默认约 14 个逻辑像素；主要动作在空间允许时采用 16–20 高度。
- 相邻按钮保留清楚间距，避免主按钮紧贴滚动条、边框或其他危险操作。
- 图标按钮必须有 tooltip；只靠一个难辨认图形不能承载关键功能。
- disabled 状态要同时有视觉反馈和必要原因提示，不能只让按钮“没反应”。
- hover、selected、pressed 不能仅靠极小色差；至少结合明度、边框、图标或形状中的一项。
- 拖拽画布设置小阈值，避免普通点击被判成拖拽；控件区域不触发背景拖动。

## 验收维度

视觉/交互改动按受影响范围从以下维度中选择验证，不默认执行全矩阵：

1. `zh_cn` 与 `en_us`；
2. 短名称、长英文、自定义名称和空数据；
3. 常用 GUI Scale（至少两个不同档位）；
4. 默认、hover、pressed、selected、disabled、focused；
5. 窗口边缘的 tooltip、下拉层和滚动条；
6. 鼠标点击、滚轮、拖拽、ESC/关闭后重开；
7. 文本不越出外壳、不在 hover 时浮到 UI 之上。

用户负责视觉验收时，交付时只需说明打开入口、改动位置和上述相关预期，不自行制作冗余截图。
