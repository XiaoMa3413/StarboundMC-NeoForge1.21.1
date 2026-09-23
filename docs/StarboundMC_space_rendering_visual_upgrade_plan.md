# StarboundMC 太空渲染视效升级实施计划

> 适用仓库：`XiaoMa3413/StarboundMC-NeoForge1.21.1`  
> 目标版本：Minecraft 1.21.1 / NeoForge 21.1.x  
> 面向实现 Agent：偏速度型代码 Agent（如 DeepSeek v4.1 Flash）  
> 文档用途：**直接作为施工任务书执行，不作为纯设计讨论稿。**  
> 核心原则：**小步修改、阶段验收、避免一次性重写、优先解决当前渲染架构瓶颈。**

---

## 0. 总目标

升级 StarboundMC 的飞船太空视觉，使其从“功能完整的自定义天空与天体渲染”提升为：

- 有明显纵深的深空背景；
- 更有材质和体积感的行星；
- 近距离真正具有球体感、表面动态和日冕层次的恒星；
- 更自然的大气、发光与高亮效果；
- 更合理的 GPU / CPU 分工；
- 为未来黑洞、引力透镜、复杂天体后处理预留可扩展渲染管线。

最终目标不是追求复杂度，而是：

> **玩家关闭 Minecraft HUD，仅从飞船舷窗观察太空时，也能明显感到这是 StarboundMC 自己的视觉系统。**

---

# 1. 当前实现基线

在动手前，必须先阅读并理解当前真实实现，不允许只根据本文假设代码结构。

重点检查：

```text
src/main/java/com/starboundmc/client/PlanetRenderer.java
src/main/java/com/starboundmc/client/StellarRenderer.java
src/main/java/com/starboundmc/client/StellarPointBatchRenderer.java
src/main/java/com/starboundmc/client/RockyMoonSkyRenderer.java
src/main/java/com/starboundmc/world/universe/BodySpaceVisualProfile.java

docs/ship-space-visual-enhancement-plan.md
docs/ui-art-direction.md
```

当前已知架构特点：

- 飞船维度使用 `SkyType.NONE`。
- 太空背景、星点、行星、恒星、跃迁均由自定义 renderer 负责。
- 行星使用球体几何与球面贴图。
- 行星支持：
  - 昼夜面；
  - 大气 glow；
  - 自转；
  - 光环；
  - LOD。
- 恒星使用：
  - POINT / SIMPLIFIED / FULL LOD；
  - 圆盘；
  - glow；
  - rays；
  - radiation；
  - particles。
- 星空为固定数量星点，CPU 每帧计算旋转、billboard、闪烁、颜色等。
- 当前大量绘制仍依赖 Minecraft 自带 `PositionColor` / `PositionTexColor` shader。
- 当前行星光照存在明显架构问题：
  - 光照颜色在 CPU 端烘焙进顶点；
  - 行星自转时，为保持太阳方向固定，会随 `animationTicks` 变化重新构建表面 VBO；
  - 这不利于后续材质、云层、高光和性能扩展。

---

# 2. 本轮优先级

实现优先级如下：

```text
P0  行星静态 VBO + GPU 光照
P0  Planet Surface Shader

P1  Atmosphere Shader
P1  FULL LOD 3D Stellar Surface
P1  Stellar Surface Shader

P2  星空视觉重构
P2  GPU Starfield
P2  Procedural Space Background
P2  轻量 Space Bloom

P3  渲染职责拆分与统一 Pass
```

**不要跳阶段。**

尤其禁止：

> 一上来同时重写 PlanetRenderer、StellarRenderer、星空、Bloom、黑洞和跃迁。

---

# 3. 总体执行方式

每个阶段必须遵循：

```text
理解现有代码
↓
小规模设计
↓
修改单一职责
↓
编译
↓
单元测试
↓
客户端视觉检查
↓
记录结果
↓
再进入下一阶段
```

推荐一个阶段对应一个功能分支 / PR。

---

# PR 1：行星 GPU 光照基础重构

## 目标

解决当前行星自转时逐帧重新烘焙光照 / 上传 VBO 的问题。

这是本计划最重要的工程改造。

---

## 当前问题

当前逻辑近似：

```text
animationTicks 改变
↓
重新计算相对太阳方向
↓
CPU 逐顶点计算昼夜颜色
↓
重建 Planet Surface VBO
↓
上传 GPU
↓
绘制
```

目标改为：

```text
初始化一次静态 Sphere VBO
↓
每帧只更新 model matrix / uniform
↓
Vertex/Fragment Shader 计算真实光照
```

---

## 任务

### 1. 建立静态行星球体网格

VBO 至少提供：

```text
position
normal
uv
```

禁止：

- 把最终光照颜色写死在 VBO；
- 因 animationTicks 每帧重新上传；
- 每帧创建新的大型顶点数组。

如果现有球体 position 本身等价于 normal，可以在设计上复用，但 shader 输入必须语义清晰。

---

### 2. 新建专用 Planet Shader

推荐结构：

```text
assets/starboundmc/shaders/
```

具体资源布局按 Minecraft 1.21.1 / NeoForge 当前 shader API 的真实要求实现。

**必须先检查当前版本的 shader 注册 API，不允许照搬旧 Forge / 旧 Minecraft 示例。**

Shader 至少接收：

```text
ModelViewMat
ProjMat
Sampler0 / planet albedo texture

SunDirection
NightFloor
TerminatorWidth
GlobalAlpha
```

必要时加入：

```text
CameraDirection
MaterialParams
```

---

### 3. Fragment Shader 实现昼夜光照

第一版保持简单。

逻辑目标：

```text
N = surface normal
L = sun direction

NdotL = dot(N, L)
```

使用平滑 terminator。

不要追求完整 PBR。

目标只是将当前 CPU 光照逻辑搬到 GPU，并让 per-fragment 光照比 per-vertex 更平滑。

---

## 视觉要求

- 太阳方向必须保持固定。
- 行星自转时：
  - 地表贴图旋转；
  - 昼夜分界不跟着地表错误旋转。
- 不允许出现：
  - 亮暗面翻转；
  - shader 坐标系错误；
  - 贴图赤道与光环方向失配。

---

## 性能验收

完成后：

- 行星自转时不再每帧重建表面 VBO；
- 球体 VBO 应长期复用；
- 每帧只更新必要矩阵与 uniforms；
- 不引入持续显存增长；
- 不引入逐帧创建新的 `VertexBuffer`。

---

## 回归测试

必须检查：

```text
Lush
Barren
Frozen
Molten
Gas Giant
Rocky Moon
```

至少检查：

- 停靠观察；
- 飞船转向；
- 行星自转；
- 超空间出发；
- 超空间抵达；
- Rocky Moon 地表天空中的母行星。

---

# PR 2：Planet Material Shader V1

## 目标

让不同行星不仅依靠颜色区分，而是具有不同材质。

不要一次加入完整 PBR。

使用轻量材质参数即可。

---

## 扩展 BodySpaceVisualProfile

在确认数据兼容方案后，可逐步加入：

```text
roughness
specular_strength
emissive_strength
fresnel_strength
```

优先使用可选字段并提供默认值，避免已有 universe 数据全部报错。

必要时增加：

```text
material_mask
emissive_mask
```

但第一版最多允许新增 1–2 张遮罩纹理，禁止资源爆炸。

---

## 不同行星建议

### Lush

目标：

- 陆地较粗糙；
- 海洋具有明显但克制的镜面反射；
- 夜侧保持较暗；
- 不做整颗星球“塑料亮”。

推荐：

```text
land roughness: high
ocean roughness: low
ocean specular: medium/high
```

---

### Frozen

目标：

- 冰面有冷色高光；
- 不应只是蓝白色 Lush。

推荐：

```text
roughness: medium
specular: medium
fresnel: slightly stronger
```

---

### Barren

目标：

- 干；
- 暗；
- 粗糙；
- 低反射。

推荐：

```text
roughness: very high
specular: very low
atmosphere: none / near none
```

---

### Molten

目标：

- 夜侧岩层变暗；
- 熔岩裂缝仍发光。

优先加入：

```text
emissive mask
```

Shader：

```text
final =
surfaceLighting
+
emissiveMask * emissiveColor * emissiveStrength
```

禁止：

- 整颗 Molten 发红光；
- 大幅周期呼吸；
- 高频闪烁。

---

## 验收

即使降低整体饱和度，各类行星仍应通过：

- 高光；
- 粗糙度；
- 大气；
- 夜面；

呈现不同材质气质。

---

# PR 3：Atmosphere Shader V2

## 目标

替换 / 降级当前主要依赖 CPU 几何 alpha 的 atmosphere glow。

从：

```text
uniform color glow shell
```

升级为：

```text
view-dependent
sun-dependent
limb atmosphere
```

---

## 基础模型

第一版不做完整 Rayleigh / Mie scattering。

使用简单近似：

```text
Fresnel / limb factor
×
sun-facing factor
×
atmosphere color
×
density
```

输入：

```text
normal
view direction
sun direction
atmosphere color
atmosphere strength
```

---

## 视觉要求

### Lush

- 蓝色大气；
- 日侧边缘明显；
- 夜侧只保留弱轮廓；
- terminator 附近可以适度增强。

### Frozen

- 更薄；
- 更冷；
- 更轻。

### Barren

- 基本没有明显大气。

### Molten

- 可使用较弱暖色边缘；
- 不要做成火焰外壳。

---

## 禁止

- 全圆同亮度；
- 强加法光圈；
- 大范围 bloom 抢过表面；
- 所有星球共用完全相同参数。

---

# PR 4：Lush 云层试验

## 目标

只在一颗旗舰行星上验证多层行星架构。

**第一版仅 Lush。**

---

## 实现

增加独立 Cloud Layer：

```text
surface sphere
+
slightly larger cloud sphere
```

云层：

- 使用透明纹理；
- 慢速旋转；
- 速度与地表不同；
- 不与地表完全同步。

可选：

- 在表面产生非常弱的云影；
- 如果实现复杂，第一版可不做。

---

## 约束

- 云不能完全覆盖大陆；
- 不要高速旋转；
- 不要增加大量半透明层；
- 不要先给所有行星复制云层。

---

# PR 5：FULL LOD 3D 恒星

## 目标

将当前 FULL Stellar 从主要由：

```text
disc
glow
rays
radiation geometry
particles
```

叠加形成的 2D 效果，

升级为：

```text
真实 3D photosphere
+
独立 corona
```

---

## LOD 保留

必须保留：

```text
POINT
SIMPLIFIED
FULL
```

原则：

- POINT：保持批处理；
- SIMPLIFIED：继续使用轻量 2D；
- FULL：升级 3D。

禁止把所有远距离恒星都改为球体。

---

## Stellar Surface Shader

使用共享球体 VBO。

Fragment Shader 可以包含：

```text
baseColor
coreColor
low-frequency procedural variation
limb darkening
slow time evolution
```

---

## 噪声要求

非常重要。

必须使用：

> 大尺度、低频、缓慢变化

禁止：

- 高频火焰噪声；
- 熔岩球视觉；
- 快速 scrolling texture；
- 高频闪烁。

目标是：

> 恒星 photosphere，而不是魔法火球。

---

## Limb Darkening

建议实现：

```text
中央亮
边缘略暗
```

然后再由 corona 提供外围亮度。

这是形成球体感的重要因素。

---

# PR 6：Corona Shader

## 目标

逐步减少当前固定 ray/radiation 几何的规则感。

FULL LOD 使用专用 corona shader。

---

## 推荐方案

使用一个面向摄像机的大 quad。

Fragment Shader 根据：

```text
distance from center
angle
low-frequency noise
time
```

生成：

- 非规则外围；
- 局部 plume；
- 轻微 corona variation。

---

## 保留策略

当前几何：

```text
glowBuffer
rayBuffer
radiationBuffer
particleBuffer
```

不要一次全部删除。

建议：

- SIMPLIFIED 继续使用；
- FULL 新 shader 成熟后再逐步减少旧层。

---

# PR 7：普通星空视觉重构 S0

## 目标

解决当前星空：

> 均匀撒点、层次不足、背景较平

的问题。

优先级高于增加更多华丽特效。

---

## 星空视觉目标

星空必须存在：

```text
大片纯暗区
中密度普通区
少量小星团
极少数明亮恒星
一条非常淡的宽银河尘带
```

禁止：

```text
平均铺满
RGB 彩色星云
整屏高亮
大量同时闪烁
```

---

## 星等分布

不要让亮度均匀随机。

采用强偏态分布：

```text
大量：暗星
少量：中等亮度
极少量：高亮
```

可以使用非线性随机，例如：

```text
brightness = pow(random, N)
```

具体方向根据实际效果调整。

---

## 星色

主体：

```text
冷白
中性白
```

少量：

```text
偏蓝
偏暖
```

禁止明显红绿蓝霓虹感。

---

## 闪烁

仅少数星启用。

推荐：

```text
twinkleEnabled probability <= 10~15%
```

且振幅很低。

普通深空不应像圣诞灯。

---

# PR 8：GPU Starfield

## 目标

将普通星空从大量 CPU 每帧计算，逐步转为：

```text
static star data
+
GPU transform
```

---

## 静态星数据

初始化时生成：

```text
direction
size
brightness
color / temperature
twinkle phase
layer
```

之后长期复用。

---

## Vertex Shader 负责

```text
ship yaw
ship pitch
hyperspace convergence
billboard expansion
```

---

## Fragment Shader 负责

```text
soft star profile
brightness
twinkle
tint
```

---

## 性能目标

完成后允许未来将普通背景星数量从当前约 2000 提高，而不线性增加大量 Java 端逐星三角计算。

但本 PR **不要求立刻增加星数**。

先保证架构正确。

---

# PR 9：Procedural Space Background

## 目标

将当前简单 cube + Y gradient 升级为方向驱动的深空背景。

---

## 推荐方式

仍然可以绘制：

```text
cube 或 sphere
```

但 fragment shader 根据：

```text
normalized view direction
```

生成背景。

---

## 第一版内容

只做：

```text
near-black base
+
large-scale low-frequency variation
+
very faint galactic dust band
```

禁止：

- 彩色大星云；
- 细密噪点；
- 高频变化；
- 明显边界；
- cube seams。

---

## 银河尘带

必须：

- 很宽；
- 很淡；
- 不规则；
- 有纯黑区域；
- 不抢行星和恒星。

玩家第一眼不一定“看见银河”，但应该感到：

> 星空不再完全平。

---

# PR 10：星系环境差异 S1

## 目标

复用已有：

```text
GalaxyEnvironmentBlend
```

让不同星系在背景上拥有轻微环境差异。

---

## 可调参数

```text
background tint
dust tint
dust density
star density bias
star warm/cool bias
```

---

## 原则

变化必须连续。

禁止：

```text
进入星系 A → 突然变蓝
进入星系 B → 突然变红
```

深空区域逐渐回归中性。

---

# PR 11：轻量 Space Bloom

## 目标

只服务于 StarboundMC 太空高亮对象。

不要对整个 Minecraft 世界强制 Bloom。

---

## 建议架构

建立：

```text
SpaceEmissionTarget
```

推荐半分辨率：

```text
screen / 2
```

输出来源：

```text
stellar surface high energy
corona
Molten emissive
warp high lights
future black-hole photon ring
```

---

## 流程

```text
Emission Target
↓
horizontal blur
↓
vertical blur
↓
composite into main space pass
```

可以使用更简单的 separable blur。

---

## Bloom 原则

Bloom 是辅助，不是主体。

### 恒星

允许明显但受控。

### Molten

仅裂缝产生很弱 bloom。

### 行星大气

只允许极弱 bloom。

### HUD

不参与该 Space Bloom。

---

# PR 12：Renderer Pass 整理

仅在前面效果稳定后进行。

不要提前。

---

## 目标架构

逐渐形成：

```text
SpaceRenderer
│
├── Background Pass
│   ├── space color field
│   ├── galactic dust
│   └── background stars
│
├── Opaque Celestial Pass
│   ├── planet surface
│   └── stellar photosphere
│
├── Transparent Celestial Pass
│   ├── rings
│   ├── clouds
│   └── atmosphere
│
├── Additive / Emission Pass
│   ├── corona
│   ├── emissive
│   └── warp highlights
│
└── Space Post Process
    └── bloom
```

---

## 文件职责建议

未来逐步收敛为：

```text
SpaceRenderer
SpaceBackgroundRenderer
PlanetRenderer
AtmosphereRenderer
StellarRenderer
RingRenderer
WarpRenderer
SpacePostProcessor
```

不要为了“架构漂亮”强制一次性迁移。

遵循：

> 修改到哪，拆到哪。

---

# 4. PlanetRenderer 特别整改项

当前 `PlanetRenderer` 职责过多。

它目前同时承担或涉及：

```text
space dome
starfield
planet
atmosphere
ring
system stars
warp
LOD
coordinate transform
```

禁止继续无脑往其中添加：

```text
cloud shader
bloom
black hole
lens distortion
postprocess
```

每新增一个大型视觉系统前，先判断是否应独立 renderer。

---

# 5. 渲染状态管理

当前许多方法自行：

```text
enableBlend
disableCull
disableDepthTest
depthMask
setShader
restore
```

第一阶段不强制重构。

但每次新增 shader 时必须：

- 明确记录进入状态；
- 明确恢复状态；
- 不污染：
  - 方块；
  - 实体；
  - translucent pass；
  - HUD；
  - LDLib2 UI。

后期统一到 pass 级状态管理。

---

# 6. 性能预算

目标平台不是顶级显卡专属。

默认质量下应保证：

- 当前普通飞船场景无明显持续 FPS 回退；
- 不允许每帧生成大纹理；
- 不允许逐帧重建大型 VBO；
- 不允许大量临时集合；
- 不允许每颗远距离恒星多个 draw call；
- Bloom 使用降低分辨率的 buffer；
- procedural noise 优先少 octave、低频；
- FULL Stellar 数量继续遵守当前 LOD 限制。

---

# 7. 质量档位预留

暂时不必实现完整设置页，但新增效果应便于后续做：

```text
SPACE_VISUAL_QUALITY = LOW / MEDIUM / HIGH
```

预期：

### LOW

```text
no bloom
simple atmosphere
simplified stars
basic background
```

### MEDIUM

```text
planet shader
atmosphere shader
3D near stars
procedural background
```

### HIGH

```text
bloom
higher corona detail
cloud layer
higher background density
```

不要把质量档逻辑散落几十个 if。

---

# 8. 测试要求

每个 PR 至少运行：

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

涉及客户端 shader 时额外运行：

```powershell
.\gradlew.bat runClient
```

如仓库中已有 HUD / world smoke 参数，可复用现有运行方式。

---

# 9. 客户端视觉验收矩阵

至少检查：

```text
1920×1080
2560×1080

GUI Scale 2
GUI Scale 3
GUI Scale 4

普通 FOV
FOV 110
```

场景：

```text
Lush orbit
Barren orbit
Frozen orbit
Molten orbit
Gas Giant orbit
Rocky Moon surface

first-person
third-person
ship turning
sublight movement
hyperspace departure
hyperspace arrival
```

---

# 10. 必须检查的问题

### 行星

- 昼夜面是否正确；
- 自转是否影响太阳方向；
- 极区是否抖动；
- seam 是否明显；
- terminator 是否出现条带；
- 大气是否穿帮；
- 光环是否仍对齐赤道；
- 云层是否与地表 z-fighting。

### 恒星

- FULL / SIMPLIFIED / POINT LOD 是否跳变；
- corona 是否突然 pop；
- 边缘是否锯齿；
- 是否明显像 billboard；
- 远距离是否过曝。

### 星空

- 是否存在 seam；
- 星点是否过密；
- 银河带是否抢主体；
- 是否出现大面积闪烁；
- 快速转头是否出现跳变。

### Bloom

- 是否污染 HUD；
- 是否污染方块；
- 是否导致整屏发灰；
- resize 后 framebuffer 是否正确重建；
- Alt+Tab / resource reload 后是否恢复。

---

# 11. 资源重载与生命周期

所有新 shader / render target / VBO 必须考虑：

```text
resource reload
window resize
client disconnect
world change
renderer resource release
```

不能只保证首次启动。

必要时提供明确的：

```text
init
reload
resize
close
```

生命周期函数。

---

# 12. 数据驱动原则

视觉参数优先进入：

```text
BodySpaceVisualProfile
StellarVisualProfile
```

但不要把 shader 内部实现细节全部暴露为 JSON。

适合数据驱动：

```text
roughness
specular strength
atmosphere strength
atmosphere color
emissive strength
cloud speed
corona strength
```

不适合：

```text
shader internal mathematical constants
blur kernel array
noise implementation details
```

---

# 13. Agent 工作方式要求

本计划是给自动编码 Agent 使用的。

Agent 必须遵守：

## A. 不猜 API

涉及：

```text
Minecraft shader
NeoForge renderer
RenderTarget
resource reload
RenderSystem
```

必须先搜索当前项目或当前依赖 API。

禁止直接复制 Minecraft 1.18 / Forge 旧教程。

---

## B. 一次只完成当前 PR

例如当前任务是：

> PR 1 行星 GPU 光照

则不要顺便实现：

```text
cloud
bloom
new starfield
black hole
HUD
```

---

## C. 优先复用当前逻辑

如果旧代码已经正确解决：

```text
planet orientation
ship yaw/pitch
ring geometry
LOD
universe coordinate
```

必须复用。

不要为了 shader 重写整个宇宙坐标系统。

---

## D. 保持 fallback

Shader 编译或 profile 缺失时：

- 不应直接导致整个世界崩溃；
- 尽量提供安全 fallback；
- 至少记录明确日志。

---

## E. 修改前先写简短实施说明

每个 PR 开始前先输出：

```text
目标
涉及文件
不涉及内容
风险
验证方法
```

控制在一页以内。

---

# 14. 禁止事项

以下行为本轮明确禁止：

- 一次性重写整个 `PlanetRenderer`；
- 把所有恒星全部变为 3D sphere；
- 继续靠提高 4K 行星纹理分辨率解决材质问题；
- 每帧重新生成程序纹理；
- 每帧上传行星球面 VBO；
- 为每颗星单独创建 RenderTarget；
- 默认开启重型全屏 Bloom；
- 给所有对象加 glow；
- 大面积 RGB 星云；
- 高频闪烁；
- 过度使用动态噪声；
- 将 Warp 视觉和普通星空重构混在同一个 PR；
- 在没有实测前删除当前稳定 renderer fallback。

---

# 15. 推荐开发顺序总结

严格推荐：

```text
PR 1
Static Planet VBO
+
GPU lighting
        ↓
PR 2
Planet material V1
        ↓
PR 3
Atmosphere shader
        ↓
PR 4
Lush cloud test
        ↓
PR 5
3D FULL Stellar
        ↓
PR 6
Corona shader
        ↓
PR 7
Starfield visual distribution
        ↓
PR 8
GPU starfield
        ↓
PR 9
Procedural deep-space background
        ↓
PR 10
Galaxy environment blend
        ↓
PR 11
Space bloom
        ↓
PR 12
Renderer pass cleanup
```

---

# 16. 第一阶段完成标准

完成 PR 1–PR 6 后，应达到：

## 行星

- 自转不再触发逐帧 VBO 重建；
- 表面光照为 per-fragment；
- Lush / Frozen / Barren / Molten 有明显不同材质；
- 大气不再只是统一 glow；
- Lush 有独立云层；
- Molten 夜面裂缝可发光。

## 恒星

- FULL LOD 为真实 3D 球体；
- 表面有低频动态；
- 有 limb darkening；
- corona 更自然；
- POINT / SIMPLIFIED 性能路径保留。

---

# 17. 第二阶段完成标准

完成 PR 7–PR 11 后，应达到：

- 深空存在暗区、普通区、小型星团；
- 星等差异明显；
- 银河尘带非常淡但可感知；
- 不同星系有连续环境差异；
- 星空计算主要移向 GPU；
- 高亮天体拥有轻量 bloom；
- 普通太空整体仍保持克制。

---

# 18. 最终视觉标准

最终画面必须符合 StarboundMC 已确定的美术方向：

```text
深空
舰载
克制
真实尺度感
少量高亮
明确层次
不霓虹
不网页化
不魔法粒子化
```

最终判断标准：

> 玩家站在飞船舷窗前时，第一视觉主体永远应该是宇宙、行星和恒星。

不是 glow。

不是 shader 本身。

不是“看我用了特效”。

---

# 19. 为未来黑洞预留

本计划**不实现黑洞**。

但完成后应具备：

```text
custom celestial shader pipeline
emission target
space postprocess
procedural background
structured render passes
```

未来黑洞可独立增加：

```text
BlackHoleRenderer
BlackHoleLensShader
AccretionDiskShader
```

而不需要再次重构整个太空 renderer。

---

# 20. Agent 首个任务指令

如果 Agent 第一次接到本文，**只执行 PR 1**：

> 将 StarboundMC 当前行星表面从 CPU 逐帧烘焙光照 + 动态 VBO 重建，改为静态球体 VBO + GPU per-fragment 光照。保留当前行星朝向、自转、固定太阳方向、LOD、光环和宇宙坐标逻辑。不要实现云、Bloom、黑洞或恒星重构。完成后运行 test/build，并进行客户端 Lush/Barren/Frozen/Molten/Gas Giant/Rocky Moon 回归检查。提交前说明旧逻辑被替换的位置、VBO 是否确认静态复用、shader 生命周期和资源重载处理方式。

---

## END

任何阶段如果出现：

```text
shader API 不确定
RenderTarget 生命周期不确定
现有测试失败
画面与旧版明显退步
```

应暂停继续扩展，优先解决当前阶段。

**不要通过继续堆效果掩盖底层问题。**
