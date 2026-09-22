# StarboundMC UI / 美术风格规范 v1

适用项目：`XiaoMa3413/StarboundMC-NeoForge1.21.1`

本规范用于约束 StarboundMC 后续新增和重做的 GUI、HUD、物品图标、设备屏幕、像素素材与交互视觉。

核心目标不是完整复制《Starbound》，也不是完全遵循 Minecraft 原版 UI，而是在 Minecraft 世界中建立一套具有明确身份的舰载科幻视觉系统。

## 1. 总体视觉定位

StarboundMC 的视觉关键词：

**深空、舰载终端、工程设备、像素科幻、克制发光、清晰交互。**

整体参考关系：

**Minecraft 世界尺度 + Starbound 像素识别度 + Subnautica 的设备交互感 + 轻量 NASA / 航空电子信息层级。**

避免整体向以下方向发展：赛博朋克霓虹 UI、大面积透明玻璃卡片、手游式渐变按钮、网页 Dashboard、大面积 RGB 发光、过量圆角卡片、极细现代字体、整屏 HUD 装饰。

UI 应让玩家感觉“正在操作一艘飞船上的真实设备”，而不是“打开了一个漂亮的软件页面”。

## 2. 核心视觉语言

| 角色 | 推荐颜色 | 用途 |
| --- | --- | --- |
| 深空黑 | `#050912` | 全屏背景、星图 |
| 舰船深蓝黑 | `#0B151D` | 主界面背景 |
| 石墨蓝 | `#101E28` | 二级区域 |
| 深钛灰 | `#192D37` | 结构、标题栏 |
| 暗青灰 | `#304852` | 边框、分隔 |
| 主青色 | `#63E2DF` | 数据、选择、可交互状态 |
| 浅青白 | `#D8EDF0` | 主要文字 |
| 次级灰蓝 | `#85A5B0` | 次要信息 |
| 琥珀色 | `#E2B66F` | 能源、警告、成本 |
| 工程绿 | `#83E3A2` | 蓝图、完成、升级 |
| 故障红 | `#E46B72` | 真正错误、危险、离线 |

青色是最主要的舰载数字技术颜色。琥珀色表达燃料、资源消耗、警告与重要物理状态；绿色主要用于工程蓝图、升级完成和 READY；红色使用频率应极低，只用于真实故障、危险和硬失败。

## 3. UI 层级原则

后续 UI 禁止采用“每一个逻辑分组都加一个矩形卡片”的方式。

优先使用：留白、字体层级、对齐、细分隔线、局部背景层次。能靠留白解决就不用框，能靠颜色解决就不用第二层面板，能靠对齐解决就不用额外装饰。

典型页面尽量只保持三个明显结构层级：`SCREEN → SHELL → CONTENT`，必要功能区域再在内容层中分组。避免五六层可见嵌套面板。

## 4. 边框与圆角

舰载设备整体偏向硬边、切角、微圆角。

- 主要设备外壳：0–2 px 视觉圆角
- 按钮：2–3 px
- 浮动信息板：3–5 px
- 星图信息板允许比普通机器稍圆

星图可更数字化，实体设备应更工业化。

## 5. 字体与文字层级

建议控制为四个层级：

| 等级 | 建议 LDLib2 字号 | 用途 |
| --- | ---: | --- |
| L1 | 9–10 | 页面主标题 |
| L2 | 7–8 | 模块标题、重要名称 |
| L3 | 6–7 | 正文、按钮 |
| L4 | 5–6 | 状态、提示、辅助数据 |

大多数机器 UI 主要使用 6 / 7 / 9 三档，避免同页堆满连续字号。数字可以更亮，但不要通过巨大字号制造层级。

## 6. 图标与像素资产

所有功能图标遵循：**轮廓优先于内部细节。**

建议设计尺寸：

- 普通 UI 图标：16×16 逻辑像素
- 重要图标：24×24 或 32×32
- 核心装备展示：32×32 或更高
- 星图 Sprite：48–96 logical px
- N.O.V.A.：约 96×112
- 3D Planet：2K–4K surface texture

禁止先做高分辨率插画再简单缩到 16×16。像素素材应优先保证轮廓、比例、大色块、能源核心与主要结构，再考虑微小机械细节。

普通物品应避免平滑抗锯齿、照片纹理、随机噪点、过度渐变与伪写实高光。

## 7. 物质枪视觉规则

物质枪应成为 StarboundMC 最重要的视觉符号之一。

识别核心：

- 侧视
- 朝右
- 明显 U 形前部结构
- 上下机匣
- 中央能量区域
- 斜向握把

优先级：**轮廓 > 比例 > 大色块 > 能源核心 > 结构分区 > 小型机械细节。**

科技感主要依靠结构逻辑、颜色与能源区域，而不是不断增加线条、螺丝和电路。

## 8. 物质枪升级工作台

保留绿色工程蓝图身份，但放入统一的舰船深蓝石墨外壳体系中。

中央物质枪蓝图必须是第一视觉主体。升级节点应与枪体相关部位形成明确视觉关系，例如：

- 射程 → 枪口
- 挖掘 → 发射器
- 速度 → 能量核心
- 时运 → 扫描/传感模块

升级完成节点使用工程绿亮起；当前可升级节点使用较亮绿色边框；未来节点使用低饱和暗绿；锁定节点仅保留轮廓。避免所有节点同时发亮，避免做成通用 RPG 技能树卡片网格。

## 9. 星图 Star Map

当前“空间优先”的星图原则保留。不要恢复固定左侧栏或底部 Dashboard。

主视觉顺序：**宇宙 → 恒星/行星 → 轨道/航线 → 选中状态 → 信息板 → 导航提示。**

信息板只在需要时出现；UI 为宇宙服务，而不是宇宙成为 UI 的背景图。

### 星图天体 Sprite

世界中的高分辨率 3D 行星与星图中的天体 UI 资源应彻底区分：

- 世界：高分辨率球面材质、3D 光照、大气、云层
- 星图：48/64/96 px 像素 Sprite、强化类型特征、简化真实细节

Lush、Barren、Frozen、Molten、Gas Giant、Rocky Moon 等类型应依靠轮廓、主色、表面图案、大气、环和坑洞快速区分，而不是依赖文字标签。

### 选择状态与航道

选择反馈推荐：细环 + 四角定位标记 + 极轻微脉冲。不可达目标优先降低饱和度，不要简单变红。

普通航道保持细、低亮度、间距稳定；已选择航线才显著提高亮度。屏幕上通常应由当前选中目标而不是背景航道最亮。

## 10. N.O.V.A.

N.O.V.A. 是第二个核心视觉符号。

必须保持球体、斜轨道、独立眼睛层与青色全息质感。不要逐步把它画成传统机器人头像；它应明确是舰载智能的视觉投影。

允许的动态：轻微漂浮、眼睛变化、扫描、小型数据粒子、底部投影环、故障闪烁。

避免剧烈呼吸、整体头像缩放、大范围 glow、大量粒子和高频扫描线。N.O.V.A. 最重要的是“活着”，不是“炫”。

N.O.V.A. 对话界面应更像舰桥通讯终端，不要做成 Discord / ChatGPT 式聊天气泡。

## 11. 体素打印站

当前打印站以用户认可的 **320×240 紧凑制造终端**为基线，具体遵循
[石墨舰载制造终端风格约束](ui-fabrication-style.md)和[当前设计契约](ui-design-contracts/voxel-printing-station-polish.md)。

保留左侧配方导航、右侧物品与材料、数量和主操作、原有背包及队列切换。物品名称与唯一输出槽
承载当前制造对象，槽内颜色揭示表达打印进度，世界中的打印机负责制造表现。
历史上的大制造舱、三栏结构与额外全息预览不再作为当前界面的改造要求。

采用石墨外壳、内凹输入和槽位、微凸按键、平铺列表及克制的青色状态标记。
复用这套外观不意味着复制打印站结构或改变其他设备的专属交互。

体素本身统一为“几何晶体 + 数字物质”的视觉符号，不做成金币。Voxel 与 Fuel 必须在颜色和语义上明显区分。

## 12. 燃料系统

Voxel：青色 / 数字 / 物质。

Fuel：琥珀 / 能源 / 热量。

燃料 UI 应让玩家无需阅读文字也能知道黄色代表船舶能源。不要把燃料条也统一画成青色。

## 13. 传送器

建议逐步统一回舰载 UI 母系统，同时保留航空电子设备气质：石墨黑、钛灰边框、冷白文字、青色选中、少圆角。

列表选择优先使用左侧 Marker、亮度变化和细结构，不要给每一行套发光卡片。Transmit 可比普通按钮更有实体设备感。

## 14. 设备差异化规则

不同设备允许拥有子主题，但必须建立在同一舰载母主题之上：

- Star Map：Cyan / Black / Space
- Engineering：Green Blueprint
- Fabrication：Cyan Hologram
- Fuel：Amber Energy
- Teleport：White / Cyan Navigation
- N.O.V.A.：Cyan AI Hologram

目标是“同语言，不同口音”，而不是每个界面重新发明视觉体系。

## 15. HUD 原则

HUD 永远比设备 UI 更轻，只保留图标、数字、极短状态条、透明度和必要背景遮罩。不要把 HUD 也做成完整面板。

## 16. 动画与 Glow

动画服务于状态变化，不负责持续吸引注意力。

建议时长：Hover 80–120 ms；Panel appear 120–180 ms；页面切换 150–250 ms；重要设备启动 300–600 ms。

Glow 是稀缺资源。亮度层级建议：背景 → 结构 → 文字 → 交互 → 选中 → 重要状态。只有后两级允许明显 glow。

## 17. 现有资源处理建议

- `nova_body.png`：保留，小幅精修
- `nova_bust.png`：保留，与 body 统一光照语言
- `nova_eyes.png`：保持独立动画层，不烘焙进 body
- `matter_manipulator_blueprint.png`：升级为物质枪工作台的中央视觉主体
- `starmap/bodies/*`：逐步替换为星图专用像素 Sprite，不再把真实行星缩略图作为最终 UI 美术
- `command_deck_atlas.png`：现有深蓝石墨 / 钛灰 / Cyan / Amber 配色可作为舰载机器美术母版

## 18. 核心页面优先级

1. Matter Manipulator Upgrade UI
2. Star Map Body Sprites
3. Voxel Printing Station
4. 全局 UI 规范化

其中物质枪升级界面最适合作为第一张新的标志性截图。

## 19. Codex / Agent 设计约束

后续所有 UI / 美术任务遵循以下原则：

```text
The interface belongs to a restrained shipboard science-fiction system.

Visual identity:
- very dark navy / graphite backgrounds
- titanium gray structural borders
- cyan for navigation, data and normal interaction
- amber for fuel, cost and warning states
- green reserved primarily for engineering blueprints, upgrade-ready and completion
- red reserved for genuine faults or dangerous states

Do not design a modern web dashboard.
Do not use excessive rounded cards.
Do not put every logical group inside another visible panel.
Prefer spacing, alignment, typography and thin dividers over nested rectangles.

The UI should feel like a physical spaceship terminal inside Minecraft.

Pixel art must prioritize silhouette and large readable value groups.
Avoid unnecessary micro-detail, smooth antialiasing and noisy textures.

Glow must be sparse.
The currently selected or active element should usually be the brightest element.

Keep GUI readable at Minecraft GUI Scale 2/3/4.

When adding a new screen, reuse the existing StarboundMC design language instead of inventing a completely new visual style.
```

涉及像素图时追加：

```text
Use hard pixel edges and nearest-neighbor scaling.
Design at the intended logical pixel resolution first.
Do not create a high-resolution illustration and merely downscale it.

Prioritize:
1. silhouette
2. proportions
3. large color masses
4. functional detail
5. small accents

Avoid unnecessary scratches, random noise, tiny bolts and excessive one-pixel decoration.
```

## 20. UI 自检规则

每个页面完成后依次检查：

1. 第一眼知道这是干什么的吗？
2. 第一眼知道当前可以点什么吗？
3. 第一眼知道哪个东西最重要吗？
4. 缩小 GUI 后还能看懂吗？
5. 把文字去掉后还能理解主要信息吗？
6. 和其他 StarboundMC 页面放在一起像同一个游戏吗？
7. 如果把内容替换成 Customers / Revenue / Tasks / Status，它是否依然像一个合理的 SaaS Dashboard？如果是，应重新审查设计。

只要其中一个答案明显为否，优先修结构，不要先增加细节。

## 21. 最终目标

StarboundMC 不需要拥有最多的细节，而需要拥有非常容易识别的视觉身份。

理想状态下，即使截图中没有 Minecraft HUD 和模组 Logo，玩家仍能通过深蓝石墨设备、青色数据光、绿色工程蓝图、琥珀能源状态、N.O.V.A.、物质枪与极简星图判断：这是 StarboundMC。
