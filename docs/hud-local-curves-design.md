# HUD 全屏双曲线设计

2026-09-23 补充：本文保留共享曲线的设计与历史验证。后续动画、Boot 和 N.O.V.A. 的最终表现见 [动画交付与验证](hud-animation-validation.md)；其中旧右上日志、中央框及 H5 Compass 下移已被替换，曲线本身未变。

状态：2026-09-21 已按用户授权实施，42 项 HUD 测试及宽屏/超宽屏独立客户端渲染检查通过，待实际游玩确认。H5 已完成，H6 暂不启动。

## 1. 当前设计依据

用户反馈前一版右下曲线仍过紧，明确要求：先定义横贯屏幕的上下两条曲线，再让各组件遵循它们的弯折。顶部中央低、两侧高；底部中央高、两侧低。右下读数采样下曲线的右侧片段，不能在组件内部重新起一段弧线。

此前 main 的独立纹理合成、阅读高度与当前清晰度、稳定运动仍作为参考。前一版生存条带的 4° → 12° 局部曲线，以及 Compass / EVA 各自定义的曲率已被本次方案取代。本文件名沿用旧路径，以下全屏曲线约定为当前实施依据。

## 2. Design Contract

**SCREEN**：Starbound HUD / Screen-wide Visor Curves。

**PURPOSE / INTERACTION FANTASY**：玩家在观察世界、移动和处理环境风险时，从个人视觉设备的同一组宽缓光学轮廓上读取方向与生命支持信息。

**SCREEN MODE / PRIMARY VISUAL**：Experience；世界占主导，出现危险时已有生存读数和警告成为首要 HUD 信号。

**INFORMATION PRIORITY**：危险生存状态 → 导航方向与真实目标 → 一般生存数值 → 临时设备连接状态与操作说明。NOVA 保持 H5 通信排队规则。

**INTERACTION FLOW**：苏醒 / 设备上线 → 情境显示 → 扫视读数 → 移动、补给或处理危险 → 状态反馈 → 按现有时序淡出。

**SPATIAL ORGANIZATION**：顶部 Compass 和其下方 EVA 提示采样上曲线；右下 O₂ / Cold / Heat 采样下曲线，各行只作垂直排列。右上 H5 日志、中央启动提示保持平面；左下 NOVA 独立；World AR 使用真实世界投影。

**ART / CUSTOM VISUALS**：文字、图标、刻度、状态条及其光晕一起沿曲线变形。沿用青色、冰蓝、琥珀与危险红，保留细条和既有字形。完整曲线是几何依据，正式 HUD 不绘制装饰性横线。

**GENERIC UI / STATES**：沿用现有本地化、数据、危险/补给提示、STARTING / SAFE_MODE / LINKING / ONLINE 状态。无新增设置页或交互流程。

**MOTION / FEEDBACK**：沿用有界临界阻尼转头跟随。按静态布局采样曲线，再整体叠加小幅运动；转头不改变曲率，不引入呼吸或弹跳。

**DO NOT**：在每个组件内重启弧线；单独弯状态条；按行增强曲率；将读数重新压到底部；把 NOVA、日志或真实世界画面纳入曲线形变；改变 EPP 规则或进入 H6。

**IMPLEMENTATION CONTRACT**：共享屏幕坐标曲线、整体字形映射、阅读高度和现有信息组织必须保持。具体曲率、缓存、网格采样和合成组织由实现方确定。

## 3. 宽缓双曲线

所有量均为 GUI px，屏幕尺寸为 W × H，屏幕 y 向下为正：

```text
R = max(W, H × 16/9)
t = (screenX - W/2) / R
f(screenX) = 0.4 × H × (sqrt(1 + t²) - 1)
f'(screenX) = 0.4 × H / R × t / sqrt(1 + t²)

upper(screenX) = upperReferenceY - f(screenX)
lower(screenX) = lowerReferenceY + f(screenX)
```

同一组参数生成上下镜像的双曲线分支。16:9 及更宽屏幕上，中央至边缘起伏约为屏幕高度的 4.72%；16:9 边缘倾角约 5.75°，超宽屏更平缓。较窄比例以 16:9 对应宽度限制弯曲，避免边缘突然收紧。

在 480 × 270 GUI 视口中，右下 116 px 状态条位于 x = 342…458，左右端高度差约 8.2 px，中心倾角约 4.1°。前一版同长度条带落差约 16.4 px，新版更舒缓。此数值取决于组件实际位置和视口尺寸，不再是组件内固定参数。

Compass 与 EVA 在相同屏幕 x 处具有同样的偏移和切线；它们按布局垂直错开。生存各行重复同一片段，行间距 40 px。

## 4. 纹理映射与定位

组件仍独立绘制原生内容纹理。几何先把原生坐标转换为实际 GUI 尺寸，再按屏幕横坐标采样：

```text
localX = (u - contentWidth/2) × scaleX
normalOffset = (v - contentHeight/2) × scaleY
screenX = componentCenterX + localX
slope = branchSlope(screenX)
length = sqrt(1 + slope²)

projectedX = localX - normalOffset × slope / length
projectedY = branchOffset(screenX) + normalOffset / length
final = (componentCenterX, referenceY) + projected + cameraLag
```

字形纵向沿曲线法线映射，保持厚度。不得在 project 内减去组件中心的曲线偏移：完整条带与拆成左右相邻组件的条带应得到相同屏幕位置，包括中心线之外的文字笔画。

生存组件原生 128 × 48，条带中心位于 v = 24。布局显式补偿一次，以保留已有右下阅读高度：

```text
centerX = W - 80
referenceY = H - 86.5 - max(0, rows - 2) × 40 + rowIndex × 40
             - branchOffset(centerX)
```

主读数字行中点 (64,9) 仍接近 H - 101.33 的历史阅读高度。这个布局补偿仅决定下曲线的参考高度，不能进入通用几何函数。平面对照模式使用零偏移，避免重复补偿。顶部沿用 18 px 的布局起点、EVA 63 px 起点和 H5 的 56 × statusOpacity 避让。

## 5. 清晰度、缓存与生命周期

- 内容纹理保留四周 2 px 透明留白与 4–12 倍超采样，倍率为 ceil(guiScale × 2) 后钳制。
- 曲面网格按 2 × 2 原生像素采样。缓存键包含 GUI 宽高、组件横向中心和显示缩放；这些量变化时重新上传，单纯行间位移、H5 垂直避让与转头跟随不触发上传。
- 生存三行复用一个投影实例及网格，分别更新内容。平面仅使用一个四边形。
- 光晕纹理约为内容纹理的一半分辨率；九个平面采样一次提交，8 位权重合计 255。光晕半径约 0.7 GUI px、总强度 8%，再与清晰原图分别经过同一曲面。
- 主读数 alpha 236/255、字体阴影及生存边缘 0.72…1 衰减保持；Compass 沿用自身方向淡出，不另加一层边缘压暗。
- 使用预乘 RGB / alpha 和 Shader uniform 淡出，避免近零透明度被截断。释放时关闭纹理和 VBO，重新显示可创建。
- 保留 HudVisorMotion 的水平/垂直上限 1.8 / 1.2 GUI px、增益 0.0075 / 0.005、响应 24/s，以及隐藏、暂停、相机切换等复位。
- 性能判断来自代码路径；没有 FPS/GPU 时间对照测量，不宣称帧率提升。

## 6. 验证

几何测试覆盖：上下对称及边缘斜率、相邻组件拼接连续、Compass/EVA 同曲线、横向位置选择曲段、缩放前采样、法线厚度、网格无翻折、1–3 行高度和边界、平面对照与亮度。

独立 GPU 检查直接运行 Minecraft 字体和 HudVisorProjection，覆盖 GUI Scale 2/3/4、明暗背景、15% 淡出、窗口尺度变化下的网格重建、显式释放后重建、帧缓冲/Shader 恢复和 OpenGL 错误。使用合成内容，不进入存档；H5 时序、真实环境触发与转头手感需实际游玩确认。

2026-09-21 实施记录：

- 编译及 42 项 HUD JUnit 测试通过，其中 9 项为本次共享曲线几何测试。
- 独立客户端在 1920 × 1080（正式效果）与 2560 × 1080（参考线及网格）下各完成 GUI Scale 2/3/4 的明暗背景检查，合计 12 张截图，均无新 OpenGL 错误，帧缓冲与 Shader 恢复检查通过。
- 检查跨 GUI 尺度的缓存更新、15% 淡出与资源释放后重建；截图确认右下弯折舒缓，文字随曲线变形且未发生边界裁剪。
- 截图位于 run-hud-visor-smoke/screenshots/hud-visor-1920x1080-*.png 与 hud-visor-2560x1080-*.png；日志为 tmp/hud-visor-screen-smoke.log 和 tmp/hud-visor-screen-wide-smoke.log。旧无分辨率后缀截图属于前版，不作为本次依据。
- 上述 GPU 检查为合成内容验证，不等同于完整游玩验收；实际 H5、环境状态触发和转头体验仍按现有回归清单确认。

开发命令：

```powershell
.\gradlew.bat compileJava test --tests 'com.starboundmc.client.hud.*' --console=plain

# 独立客户端，自动捕获后退出，使用独立 run-hud-visor-smoke/ 配置
.\gradlew.bat runClient -PhudVisorSmoke
.\gradlew.bat runClient -PhudVisorSmoke -PhudVisorSmokeWidth=2560 -PhudCalibrationGrid

# 正常游戏：共享全屏参考线 + 组件采样网格；均默认关闭
.\gradlew.bat runClient -PhudCalibrationGrid
.\gradlew.bat runClient -PhudVisorProfile=flat
.\gradlew.bat runClient -PhudVisorNoGlow
```

默认 profile 为 screen；flat 仅用于几何比较，不回退文字或运动。旧 main 对照 profile 已移除。src/renderTest/ 仅在 hudVisorSmoke 开启时编译，普通模组不包含测试入口。
