# StarboundMC UI / 美术风格规范 v1

适用项目：`XiaoMa3413/StarboundMC-NeoForge1.21.1`

本规范用于约束 StarboundMC 后续新增和重做的 GUI、HUD、物品图标、设备屏幕、像素素材与交互视觉。

核心目标不是完整复制《Starbound》，也不是完全遵循 Minecraft 原版 UI，而是在 Minecraft 世界中建立一套具有明确身份的舰载科幻视觉系统。

---

# 1. 总体视觉定位

StarboundMC 的视觉关键词定义为：

**深空、舰载终端、工程设备、像素科幻、克制发光、清晰交互。**

整体参考关系：

**Minecraft 世界尺度**  
+  
**Starbound 的像素识别度**  
+  
**Subnautica 的设备交互感**  
+  
**轻量 NASA / 航空电子设备信息层级**

禁止整体向以下方向发展：

“赛博朋克霓虹灯 UI”、大量透明玻璃卡片、手游式渐变按钮、网页 Dashboard、大面积 RGB 发光、过量圆角卡片、极细现代字体、整屏 HUD 装饰。

UI 应该让玩家感觉：

> 我正在操作一艘飞船上的真实设备。

而不是：

> 我打开了一个漂亮的软件页面。

---

# 2. 核心视觉语言

StarboundMC 已经形成的舰桥视觉体系作为全模组基础：

| 角色 | 推荐颜色 | 用途 |
|---|---|---|
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

青色必须保持为最主要的“舰载数字技术”颜色。

琥珀色不要作为普通装饰色。它意味着：

燃料、资源消耗、警告、不可满足条件、重要物理状态。

绿色应主要用于：

工程蓝图、升级完成、READY、制造完成。

红色使用频率必须极低。

如果一个普通按钮也在发红光，那么真正的紧急状态就失去了视觉价值。

---

# 3. UI 层级原则

StarboundMC 后续 UI 禁止采用“每一个逻辑分组都加一个矩形卡片”的方式。

推荐视觉层级：

```text
标题

─────── 分隔线 ───────

主要内容

次级标题
内容
内容

             [ PRIMARY ACTION ]
```

而不是：

```text
┌─────────────────────┐
│ ┌─────────────────┐ │
│ │ ┌─────────────┐ │ │
│ │ │ INFORMATION │ │ │
│ │ └─────────────┘ │ │
│ └─────────────────┘ │
└─────────────────────┘
```

原则：

**能靠留白解决，就不用框。**

**能靠颜色解决，就不用第二层面板。**

**能靠对齐解决，就不用额外装饰。**

一个典型页面应尽量只存在三个真正明显的结构层级：

```text
SCREEN
 ├─ SHELL
 └─ CONTENT
      └─ 必要的功能区域
```

不要出现五六层嵌套面板全部可见。

---

# 4. 边框与圆角

舰载设备整体应该更偏：

**硬边、切角、微圆角。**

普通设备不要大量使用明显圆角卡片。

推荐：

主要设备外壳：0–2 px 视觉圆角  
按钮：2–3 px  
浮动信息板：3–5 px  
星图信息板允许比普通机器稍圆

星图可以比实体设备更加“数字化”。

舰桥终端、打印机、传送器则应该明显更硬。

因此：

**Star Map → 软件感稍强**

**Machine → 工业设备感稍强**

这是合理差异。

---

# 5. 字体与文字层级

不推荐在同一界面出现太多字号。

建议统一为四个层级：

| 等级 | 建议 LDLib2 字号 | 用途 |
|---|---:|---|
| L1 | 9–10 | 页面主标题 |
| L2 | 7–8 | 模块标题、重要名称 |
| L3 | 6–7 | 正文、按钮 |
| L4 | 5–6 | 状态、提示、辅助数据 |

原则：

大多数机器 UI 应主要使用 **6 / 7 / 9** 三档。

避免：

`5、6、7、8、9、10、11` 全部出现在同一个页面。

数字数据允许使用更亮颜色，但不要通过不断放大字号制造层级。

例如：

```text
FUEL RESERVE
742 / 1000
```

数字可以亮，但不用巨大。

---

# 6. 图标风格

所有功能图标应满足：

**轮廓优先于内部细节。**

建议设计尺寸：

普通 UI 图标：16×16 逻辑像素

重要图标：24×24 或 32×32

核心装备展示：32×32 或更高

禁止先在 512×512 中画大量细节，再缩到 16×16。

应该先考虑：

> 如果整张图只剩黑色剪影，我还能认出它吗？

这是物品图标最重要的判断标准。

---

# 7. 物品像素艺术标准

普通 Minecraft Inventory Item：

建议遵循 **16×16 / 32×32 逻辑像素级别**。

可以使用高清源文件，但最终视觉必须明显保持离散像素结构。

不要：

平滑抗锯齿  
照片纹理  
大量一像素随机噪点  
过度渐变  
伪 3D 写实高光

推荐一个物品最多拥有：

主轮廓色  
暗面  
亮面  
结构色  
能源色  
极少量高光

大多数物品控制在约 **8–16 个主要视觉颜色**即可。

---

# 8. 物质枪视觉规则

物质枪应成为 StarboundMC 最重要的视觉符号之一。

识别核心：

**侧视**

**朝右**

**明显 U 形前部结构**

**上下机匣**

**中央能量区域**

**斜向握把**

任何未来版本首先保证这一轮廓。

优先级：

```text
轮廓
>
比例
>
大色块
>
能源核心
>
结构分区
>
小型机械细节
```

不允许为了增加“科技感”不断塞线条、螺丝、电路。

科技感主要依靠：

结构逻辑 + 颜色 + 能源区域。

---

# 9. 物质枪升级工作台

当前绿色蓝图方向保留。

但最终视觉建议改成：

**舰船外壳 + 工程蓝图显示器。**

整体结构：

```text
┌──────────────────────────────────┐
│ MATTER MANIPULATOR / ENGINEERING │
├──────────────────────────────────┤
│                                  │
│ SPEED II              RANGE III  │
│    ○──○                 ○──○──○ │
│        ╲               ╱         │
│         ╲             ╱          │
│       ┌──────────────────┐       │
│       │                  │       │
│       │   MATTER GUN     │       │
│       │   BLUEPRINT      │       │
│       │                  │       │
│       └──────────────────┘       │
│         ╱             ╲          │
│        ╱               ╲         │
│    ○──○             ○──○──○     │
│ MINING              FORTUNE      │
│                                  │
├──────────────────────────────────┤
│ MODULES  ×08      COST ×03       │
│                       [ UPGRADE ] │
└──────────────────────────────────┘
```

中央必须是真正的视觉主体。

升级节点应该产生“设备模块连接到枪体”的感觉。

四条升级建议与枪体建立视觉关系：

射程 → 枪口

挖掘 → 发射器

速度 → 能量核心

时运 → 扫描/传感模块

升级完成节点：

工程绿亮起。

当前可升级节点：

较亮绿色边框。

未来节点：

低饱和暗绿。

锁定：

只保留轮廓。

不要所有节点同时发亮。

---

# 10. 星图 Star Map

当前星图布局原则保持。

不要恢复固定左侧栏或底部 Dashboard。

主视觉顺序：

```text
宇宙
↓
恒星 / 行星
↓
轨道 / 航线
↓
选中状态
↓
信息板
↓
导航提示
```

即：

**UI 为宇宙服务，而不是宇宙成为 UI 背景。**

信息板只在选中目标时出现。

非必要的信息不要常驻屏幕。

---

# 11. 星图天体 Sprite

建议将当前星图天体显示资源，与世界里的高分辨率 3D 行星贴图彻底区分。

世界：

4096×2048 行星材质  
真实球面  
3D 光照  
大气  
云层

星图：

48×48 / 64×64 / 96×96 像素 Sprite  
强化类型特征  
减少真实细节

例如：

```text
LUSH

     ░▒▒▒░
   ▒██████▒
  ▒██▓▓████▒
  ██▓██▓███
  ▒████▓██▒
   ▒██████▒
     ░▒▒░
```

不是追求真实大陆形状。

而是让玩家 0.2 秒内认出：

“这是宜居蓝绿色星球。”

各类型差异应来自：

轮廓

主色

表面图案

大气

环

坑洞

而不是依赖文本标签。

---

# 12. 星图选择状态

避免粗大的 Minecraft 白框。

推荐三层选择反馈：

```text
细环
+
四角定位标记
+
非常轻微脉冲
```

例如：

```text
      ┌       ┐

          ●

      └       ┘
```

当前目标可以增加第二层淡青环。

不可达目标：

降低饱和度。

不要简单变红。

红色意味着危险，不等同于“现在去不了”。

---

# 13. 星图航道

目前虚线浮点航道方向正确。

继续保持：

线宽较细

间距稳定

低亮度 glow

不要使用方形点阵线

普通航道应比天体暗。

已选择航线才显著提高亮度。

屏幕上最亮的东西应该通常是：

**当前选中目标。**

不是背景航道。

---

# 14. N.O.V.A.

N.O.V.A. 是第二个核心视觉符号。

必须保持：

球体

斜轨道

独立眼睛层

青色全息质感

不要把 N.O.V.A. 越画越接近传统机器人头像。

它应该明显不是“一个机器人头”，而是：

**一个舰载智能的视觉投影。**

允许的动态：

轻微漂浮

眼睛变化

扫描

小型数据粒子

底部投影环

故障闪烁

不建议：

剧烈呼吸

整个头像缩放

大范围 glow

大量粒子

高频扫描线

N.O.V.A. 最重要的不是华丽，而是“活着”。

---

# 15. N.O.V.A. 对话界面

对话界面应类似舰桥通讯终端。

推荐：

```text
┌──────────────────────────────────┐
│ N.O.V.A.  // SHIPBOARD AI        │
├───────────┬──────────────────────┤
│           │                      │
│   NOVA    │  Message             │
│ PORTRAIT  │                      │
│           │  Message             │
│           │                      │
│           │                      │
├───────────┴──────────────────────┤
│ > Response                       │
│ > Response                       │
└──────────────────────────────────┘
```

不要做成 Discord / ChatGPT 式聊天气泡。

N.O.V.A. 是舰载 AI，不是聊天软件。

---

# 16. 体素打印站

打印站最大的视觉重点应该是：

**制造。**

而不是：

**配方管理。**

布局建议：

```text
┌────────────────────────────────────────┐
│ VOXEL FABRICATION SYSTEM    ◈ 12,420  │
├────────────┬─────────────────┬─────────┤
│            │                 │         │
│ RECIPES    │    HOLOGRAM     │ REQUIRE │
│            │                 │         │
│ Module     │      ◇◇◇        │ Fe 4/4  │
│ Tool       │     ◇██◇        │ V 100   │
│ Machine    │      ◇◇◇        │         │
│            │                 │         │
│            │ PRINTING 42%    │         │
│            │ ███████░░░░     │         │
├────────────┴─────────────────┴─────────┤
│ -10  -1     ×12      +1  +10  [PRINT] │
└────────────────────────────────────────┘
```

制造中的物品预览应该至少有三个阶段：

```text
WIRE
↓
HOLOGRAM
↓
MATERIALIZED
```

最简单也可以通过透明度实现。

例如：

0–30%：仅边框

30–70%：半透明

70–99%：主体完整 + 扫描层

100%：正常物品

这样打印会成为“事件”，而不是等待进度条。

---

# 17. 体素本身

Voxel 视觉语言建议固定成：

**几何晶体 + 数字物质。**

不要做成传统金币。

推荐：

菱形

立方晶格

六边形节点

小型青蓝光点

颜色可以使用：

青色主体 + 少量紫蓝暗部。

Voxel Wallet 图标全项目保持一致。

以后：

HUD

打印机

交易

任务奖励

掉落物

都应该使用同一个视觉符号。

---

# 18. 燃料系统

燃料与体素必须视觉上完全不同。

Voxel：

**青色 / 数字 / 物质**

Fuel：

**琥珀 / 能源 / 热量**

因此燃料 UI 推荐：

```text
FUEL RESERVE

██████████████░░░░

742 / 1000

INPUT
[ Coal ×4 ]

ENERGY VALUE +40

          [ INJECT FUEL ]
```

不要把燃料条也画成青色。

玩家应该无需阅读文字就知道：

黄色 = 船的能源。

---

# 19. 传送器

建议未来统一回舰载 UI 系统，但保留一点“航空电子设备”气质。

视觉上：

石墨黑

钛灰边框

冷白文字

青色选中

尽量少圆角

Destination List 可以非常机械化：

```text
▌ SHIP
  ORBIT

▌ ROCKY MOON OUTPOST
  SURFACE

▌ PLAYER BEACON 03
  BARREN PLANET
```

选择项主要通过左侧 Marker 表示。

不要给每一行套发光卡片。

Transmit 按钮可以比普通按钮更有实体感。

---

# 20. 设备差异化规则

不同设备允许拥有“子主题”。

但必须建立在舰船母主题之上。

```text
舰载母主题
│
├─ STAR MAP
│   Cyan / Black / Space
│
├─ ENGINEERING
│   Green Blueprint
│
├─ FABRICATION
│   Cyan Hologram
│
├─ FUEL
│   Amber Energy
│
├─ TELEPORT
│   White / Cyan Navigation
│
└─ N.O.V.A.
    Cyan AI Hologram
```

这叫：

**同语言，不同口音。**

而不是每个界面重新发明一种视觉体系。

---

# 21. HUD 原则

HUD 永远比设备 UI 更轻。

HUD 不应该拥有完整面板。

推荐：

图标

数字

极短状态条

透明度

必要情况下少量背景遮罩

例如体素余额：

```text
◇ 12,420
```

足够。

不需要：

```text
┌──────────────┐
│ VOXEL WALLET │
│ ◇ 12,420     │
└──────────────┘
```

---

# 22. 动画规范

动画服务于：

**状态变化。**

不是持续吸引注意力。

推荐持续时间：

Hover：80–120 ms

Panel appear：120–180 ms

页面切换：150–250 ms

重要设备启动：300–600 ms

动画曲线尽量：

快速进入 + 柔和结束。

不要大量弹性动画。

不要网页 App 风格的滑动卡片。

StarboundMC 应更像“设备响应”，而不是“手机应用”。

---

# 23. Glow 规范

Glow 是稀缺资源。

建议亮度层级：

```text
背景
  ↓
结构
  ↓
文字
  ↓
交互
  ↓
选中
  ↓
重要状态
```

只有最后两级允许明显 glow。

不要：

所有青色东西都发光。

如果所有东西都亮：

什么都不重要。

---

# 24. Pixel Density

项目建议固定以下视觉密度：

```text
普通物品
16–32 logical px

关键武器
32 logical px

机器 UI 图标
16–24 logical px

星图 Sprite
48–96 logical px

N.O.V.A.
约 96×112

方块贴图
统一 texel / block density

3D Planet
2K–4K surface texture
```

不要根据“越高清越好”判断质量。

UI 中最重要的是：

像素尺度一致。

---

# 25. 现有资源处理建议

`nova_body.png`

保留。

只进行小幅精修。

---

`nova_bust.png`

保留。

与 body 使用同一光照语言。

---

`nova_eyes.png`

继续作为独立动画层。

不要把眼睛烘焙进 body。

---

`matter_manipulator_blueprint.png`

建议升级为物质枪工作台的真正中央视觉主体。

---

`starmap/bodies/*`

建议逐步替换为人工设计的星图专用像素 Sprite。

不要继续直接依赖真实行星缩略图作为最终 UI 美术。

---

`command_deck_atlas.png`

现有深蓝石墨 / 钛灰 / Cyan / Amber 配色可作为机器美术母版。

---

# 26. 四个核心页面的优先重做顺序

第一：

**Matter Manipulator Upgrade UI**

原因：

它最容易成为 StarboundMC 标志性截图。

---

第二：

**Star Map Body Sprites**

原因：

成本相对较低，但能够极大提高整体视觉统一性。

---

第三：

**Voxel Printing Station**

原因：

功能已经成熟，非常适合通过全息制造动画获得明显提升。

---

第四：

**全局 UI 规范化**

统一：

颜色

字号

边框

按钮

状态

间距

图标

---

# 27. Codex 开发约束

后续所有 UI / 美术任务，都应在任务开头附加以下约束：

```text
StarboundMC UI ART DIRECTION

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

---

# 28. Codex 生成美术时追加

涉及像素图时增加：

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

---

# 29. UI 自检规则

每次一个页面做完以后，不要首先问：

> 漂不漂亮？

依次检查：

**第一眼知道这是干什么的吗？**

**第一眼知道当前可以点什么吗？**

**第一眼知道哪个东西最重要吗？**

**缩小 GUI 后还能看懂吗？**

**把文字去掉后还能理解主要信息吗？**

**和其他 StarboundMC 页面放在一起像同一个游戏吗？**

只要其中一个答案明显是“不”，优先修结构。

不要先增加细节。

---

# 30. 最终目标

StarboundMC 不需要拥有最多的细节。

它需要拥有：

**非常容易认出来的视觉身份。**

理想状态下，未来看到一张没有 Minecraft HUD、没有模组 Logo 的截图，玩家仍能通过：

深蓝石墨设备

青色数据光

绿色工程蓝图

琥珀能源状态

N.O.V.A.

物质枪

极简星图

判断：

> 这是 StarboundMC。