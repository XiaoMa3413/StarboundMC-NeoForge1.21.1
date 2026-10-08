# 太空渲染管线与验证

本文件记录 `refactor/space-render-v2` 新路径的实际实现合同。开发阶段和后续事项见 [技术路线](space-render-roadmap.md)。

## 当前入口与顺序

船舱维度仍在 NeoForge `AFTER_SKY` 渲染，不改变航行状态、宇宙注册表、星图或服务器位置。`SpaceRenderer` 捕获帧状态，去除原版步行晃动，随后执行：

```mermaid
flowchart LR
    A[帧状态与相对坐标] --> B[方向深空背景]
    B --> C[原生 GPU 星场 / StellarView HDR]
    C --> D[恒星光球写入天体深度]
    D --> E[全部行星表面写入深度]
    E --> F[日冕读取天体深度]
    F --> T[按远近提交云 / 环 / 大气透射]
    T --> L[HDR 辉光金字塔]
    L --> G[曝光与颜色输出]
    G --> H[原有跃迁效果]
```

独立目标仅把颜色合成回进入事件时的 framebuffer。Minecraft 方块、实体和玻璃在后续阶段继续使用原有世界深度。事件结束恢复 framebuffer、viewport、深度/混合/裁剪状态、shader、纹理绑定、矩阵排序和雾参数。

## 坐标与深度

- 宇宙坐标保持 sector + double；先相减再旋转，不把巨大绝对坐标上传给 GPU。
- 为复用网格并保持投影精度，行星仍可投到 280 的安全局部距离；这只是几何投影，不再是遮挡距离。
- `DistanceScale` 还原片元观察坐标的真实宇宙尺度。统一深度为 `log2(1 + radialDistance) / 40`，使用独立 `DEPTH_COMPONENT32F` 纹理，1 保留为空背景。
- 行星表面与恒星光球写深度；云、环、大气、日冕读取深度。背景不写深度。环在新路径中使用一个静态完整网格，不再按中心距离手工切两半。
- 有效深度范围约到 `2^40` 个当前宇宙单位。超远距离会饱和；当前候选星系范围远低于它。未来跨星系透镜不能忽略这一限制。
- 行星 LOD 使用外部观察者的准确球体角径 `2 asin(R/d)`。恒星原生路径从已有参考角径推导显示半径，按到恒星自身的距离投影，保留 72 的近景显示上限；它不是新引入的物理恒星数据。

## 颜色与材质

原生路径使用 `RGBA16F` 颜色目标。PNG 漫反射颜色按 sRGB 解码后进行局部双线性插值，不修改 Minecraft 的全局纹理过滤；材质 mask 的 R（光滑/高光权重）、G（自发光）和云 alpha 按线性数据读取。

行星新路径使用随受光角变化的漫反射、GGX 高光和独立自发光。背景辐射、云与恒星能量在合成前保持线性；最终按固定曝光和白点曲线转换到显示颜色。曝光只影响太空背景，不影响方块、实体或 GUI。

原生星场是所有画质档的默认后端。StellarView 保留为可选星场，具有两条导入路径：定制版本 `0.5.4-alpha-starbound-hdr2` 通过可选 `renderLinear` API 直接写入 HDR；旧 `externalview2` 版本仍绘入独立 LDR 目标，再解码并叠加。适配器仅通过公开 API 反射连接，签名查找只做一次，主项目及测试没有 StellarView 编译依赖。缺失模组或不兼容 shader 回到原生星场。定制 API 的输出模式/viewport uniform 在调用后恢复，普通维度继续使用原版输出。源码补丁与重建方法见 [StellarView 定制说明](../patches/stellarview/README.md)。

若旧背景或颜色适配 shader 不可用，保留显示颜色兼容模式；独立天体深度仍可工作。资源失败导致核心 shader 缺失时切到 `DIRECT` 路径，资源重载后重新尝试。

大气已使用指数高度密度、Rayleigh / Henyey–Greenstein 相函数与单次散射，按 `背景 × 透射 + 入射散射` 组合。地面截断和太阳地影采用解析交点；在地影边界分段、向高密度区域集中采样，并用有限太阳视盘可见度连接晨昏线。地表、云顶与散射共享太阳衰减。散射前 blit 一份独立 HDR 颜色/深度，禁止采样正在写入的颜色附件。大气不写天体深度，并在前景不透明天体处截断；摄像机在壳内也可渲染。系数是沿用各行星美术 profile 的艺术化近似，尚无真实单位标定和多次散射。太阳光学深度已有共享 LUT（256×128 RG32F，额外 256 KiB）；表与颜色/强度无关，CPU 数据首次使用生成并缓存，GPU 纹理在重载/退出后释放重建，极薄的自定义壳仍使用数值积分。

Bloom 使用低分辨率 `RGBA16F` 金字塔，软阈值提取高光，逐级降采样/上采样后参与曝光合成；只处理太空目标。银河方向场区分银心隆起、盘面星云和分支尘埃吸收，保持深空黑位。细星群使用固定球面随机位置和经过像素足迹过滤的 Gaussian；经度随机种子周期连续，极区避开未定义的 `atan(0,0)`。`spaceBackgroundRadiance` 显式接收角足迹，后续曲线光线采样也需提供有效足迹。Performance 不执行细星群循环。

原生独立星点将相对星等转换为浮点总通量，通过 UV 属性上传，避免 8-bit alpha 抹去暗星层次。色温来自预计算的连续黑体光谱调色板，着色器按亮度归一化。屏幕对齐的星点使用解析像素面积积分 Gaussian，稀少亮星才带弱光学翼；亚像素位移分配能量而不让整颗星忽明忽暗。星点与银河共用方向尘埃透射，前景族群不受全部尘埃遮挡；高档主要增加更暗的星。仍使用一个静态目录 VBO，不新增纹理或 framebuffer。StellarView HDR2 保留其最小像素半径与面积补偿实现。星型与辉光层次遵循 [电影美术方向](space-render-art-direction.md)。

望远镜的星点轮廓按活动投影的焦距比例放大，参照用户当前普通 FOV。焦距比例从投影纵向行向量的长度求出，避免受伤相机旋转被当作缩放；随原版 FOV 插值连续变化，放下望远镜后恢复。场景位置遵循完整倍率，未解析的背景星点轮廓按倍率平方根扩大，以保留清晰星心；通量按轮廓面积补偿，保持色温和单位面积亮度。倍率限制为 1–32，普通视角和更宽的运动 FOV 不缩小原有星点；此上限约束第三方极端投影的填充开销。仍用原有目录/目标，Direct + Procedural 也使用同一轮廓。该处理是观感设计，不表示解析了真实恒星表面或新增了物理望远镜孔径。

云壳把纹理覆盖率转换为法向光学深度，并按视线斜角计算透射；掠射路径采用有限曲率近似。薄云有受太阳透射、行星地影和云自身光学深度约束的前向散射，地表云影使用同一光学深度和太阳路径。现有 cloud opacity、漂移和资产编码保留；Direct 路径保留旧云光照。此实现不等于完整体积云或多次散射。

透明天体按 `中心距离² − 包围半径²` 从远到近提交；包围半径包含环的外缘、云壳和大气。对于不相交的球形包围体，任一共同视线的入口与出口距离之积等于这个排序键，因此能处理「小天体中心更近，却在大行星边缘后方」的案例。选择与 LOD 仍使用原中心距离，排序数组复用。物理上相交的跨天体包围体不能由此获得通用逐像素排序，仍是自定义场景的限制。

单一天体的 HDR 组合顺序为远环 → 最远大气段 → 云背面 → 中间大气段 → 云正面 → 最近大气段 → 近环。大气按云球的两次解析交点裁切，各段继续在地面和前景不透明深度处截断；无云时只执行一次完整大气。环内缘为 1.24 倍行星半径，大气/云最大半径分别为 1.10 / 1.05，因此环与局部体积不相交，可逐片元判定在整个体积前方或后方。前环不再受到后方大气的额外染色。复用现有大气读写快照目标；带云大气最多三次快照与积分，各段沿用画质采样预算。没有增加 GPU 目标或大型依赖。

环的贴图 alpha 转换成法向光学深度，沿太阳与视线斜角计算反射/透射的单次散射；相等角度使用连续极限，掠射角采用有限厚度正则项。双 HG 相函数保留冰粒前向亮边；有界漫射闭合补充密集冰环的亮面与暗面读数，尚不是数值求解的多次散射。既有贴图含烘焙压暗，采用反照率校准与去饱和，并对径向像素足迹做四点线性过滤；法向覆盖率和数据编码保持兼容。行星投影在环上保持遮挡率，只衰减入射光；环按相同密度向地表、云和大气投影太阳阴影，四方向视盘探针近似半影。环平面保持原有游戏几何，阴影显式转换纹理动画的坐标系。Direct 保留原绘制方式。

近地表纹理仍受现有 4K 资产、球体网格和薄云壳模型限制；厚云内部和多个相交体积尚需独立方案。

## 配置与回退

编辑客户端 `starboundmc-client.toml`：

| 配置 | 默认 / 用途 |
| --- | --- |
| `spacePipelineMode` | `ISOLATED`；`DIRECT` 返回原有天体绘制路径 |
| `spaceBackgroundMode` | `PROCEDURAL`；`LEGACY` 返回旧天空与 CPU 原生星点 |
| `spaceStarfieldBackend` | `NATIVE`；`STELLAR_VIEW` 请求已安装的可选 StellarView 星场，失败时回到原生；Performance 始终原生 |
| `spaceExposure` | `1.0`；原生线性颜色模式范围 0.25–4 |
| `spaceVisualQuality` | `BALANCED`；控制星数、大气采样、Bloom 预算及现有功能组合 |
| `spaceBloomStrength` | `0.32`；范围 0–1.5，0 关闭辉光；Performance / Direct 不启用 |

| 画质 | 背景星预算 | 大气观察 / 光线采样 | Bloom 层数 / 首层尺寸 |
| --- | ---: | ---: | --- |
| Performance | 2,000（原生） | 12 / LUT（极薄壳 4） | 关闭 |
| Balanced | 6,000 | 16 / LUT（极薄壳 4） | 3 层，宽高各 1/4 |
| High / Custom | 20,000 | 20 / LUT（极薄壳 6） | 4 层，宽高各 1/2 |
| Ultra | 32,000 | 28 / LUT（极薄壳 8） | 5 层，宽高各 1/2 |

原生目录固定种子，低档是高档的稳定前缀；首次使用预算或资源重载时上传。StellarView 也按画质重建目录，但其内部分类顺序不承诺与原生一样的前缀稳定。Performance 保留大气、关闭云及可选星场；Balanced 关闭云影；Custom 保留独立功能开关。后端选择与画质分开，Balanced / High / Ultra / Custom 均尊重显式选择；原来的 `stellarViewBackgroundStars` 布尔配置由新枚举替代，已有配置首次校正后使用原生默认值。

太空颜色/深度及大气输入副本共约 24 字节/像素。1920×1080 时约 47.5 MiB，Ultra 辉光额外约 5.3 MiB；不含资产、Minecraft 原有目标及驱动开销。大气副本按需建立；尺寸变化和资源重载后重建，退出会释放。

完整视觉回退组合为 `spacePipelineMode = "DIRECT"` 和 `spaceBackgroundMode = "LEGACY"`。`eae609c` 是独立深度迁移前的检查点，`8eab25d` 是扩大视觉预算前的检查点，`9034d46` 是原生星场默认迁移前、已修复光学表上传崩溃的检查点。既有主项目检查点已推送 GitHub；定制 StellarView 源码保存在独立工作树的 `feat/starbound-hdr-stars` 分支和本仓库源码补丁中。本次迁移没有玩法数据改动。

## 运行验证

要求 Java 21；本轮实机验证使用 `D:/JAVA RE/jdk21`。共享 Gradle 缓存含当前 NeoForge 和可选依赖，示例为 PowerShell（自行替换 Java 路径）：

```powershell
.\gradlew.bat --gradle-user-home ../.gradle-home --offline '-Dorg.gradle.java.installations.paths=D:/JAVA RE/jdk21' test build
.\gradlew.bat --gradle-user-home ../.gradle-home --offline '-Dorg.gradle.java.installations.paths=D:/JAVA RE/jdk21' -PspaceSmoke -PspaceSmokeLabel=isolated runClient
.\gradlew.bat --gradle-user-home ../.gradle-home --offline '-Dorg.gradle.java.installations.paths=D:/JAVA RE/jdk21' -PwithStellarView -PspaceSmoke -PspaceSmokeLabel=stellarview -PspaceSmokeStellarView=true runClient
.\gradlew.bat --gradle-user-home ../.gradle-home --offline '-Dorg.gradle.java.installations.paths=D:/JAVA RE/jdk21' -PspaceSmoke -PspaceSmokeLabel=direct -PspaceSmokePipeline=direct -PspaceSmokeBackground=legacy runClient
```

`spaceSmoke` 显式加入 `src/renderTest/java`，普通生产构建不包含夹具。夹具创建新测试世界，运行在 `run-space-smoke`，不打开用户存档。隐藏窗口但保持渲染。

普通 `test build` / `runClient` 不解析或下载 StellarView。开发环境加 `-PwithStellarView` 才把 `gradle.properties` 指定版本放到 runtime；该定制版本尚未发布远程 Maven，仅可选验证需要先按补丁说明构建并安装到本地 Maven。玩家安装模组仍通过常规 mods 目录；客户端配置设为 `spaceStarfieldBackend = "STELLAR_VIEW"` 才请求该后端。

19 个视点覆盖六种行星/卫星、深空、恒星近景、银河方向、壳内近大气视点、日面/半亮面/弯月/近晨昏线/日落/夜面、玻璃舷窗、960×540 缩放及资源重载。结果写入 `run-space-smoke/screenshots/<label>`：

- PNG 为实际客户端图像；TXT 包含分辨率、GPU/驱动、样本数、CPU/GPU P50/P95 和资源计数。
- `depth-checks.txt` 使用实际 GPU shader 验证恒星在前/行星在前，分别交换绘制顺序，并检查大气对前/后景辐射的衰减、波长衰减顺序、各档掠射晨昏线的收敛、不透明天体深度、世界深度哨兵和事件状态恢复。
- 同一 GPU 检查还覆盖银河银心/外围亮度、半分辨率平均辐射、经度接缝、极区黑位，以及云层法向/掠射覆盖、前后景衰减和不透明深度保持。
- 原生星点检查直接调用生产 shader，在完整/半分辨率下分别偏移 0、0.125、0.25、0.5、0.75、1 像素，验证总通量、有限值和不透明深度保持；它不替代完整相机运动或其他 GPU 验收。
- 光学表上传检查使用真实 `NativeImage` 子区域上传留下的行跨度/偏移、外部字节交换布局及已绑定的像素上传缓冲区，逐项回读全部 65,536 个 float，验证释放后重建、缓存复用和调用者上传/纹理状态恢复；Direct 也执行此项检查。
- `complete.txt` 只在本轮所有捕获和深度检查成功后写入。
- GPU 时间用异步 timestamp 成对查询，只读取已完成结果，不使用 `glFinish`。`TOTAL` 包含事件入口状态保存和最终颜色合成；单个 pass 计时不能相加替代它。
- 生产测量可使用 JVM 参数 `-Dstarboundmc.debug.spaceProfile=true`，每 10 秒把统计写入客户端日志；默认不建立计时 query。

夹具额外参数：`-PspaceSmokeQuality=performance|balanced|high|ultra|custom`、`-PspaceSmokeWidth=1920 -PspaceSmokeHeight=1080`、`-PspaceSmokeWithoutStellarView`（从运行 classpath 排除可选模组，默认已经不加入）。四种 StellarView shader 的矩阵使用 `-PwithStellarView -PspaceSmokeStellarView=true -PspaceSmokeStellarMatrix=true`，额外捕获 4 个视点（总计 23），并检查 uniform/普通天空上传计数恢复。PowerShell 定点检查可使用 `'-PspaceSmokeViews=sys1:lush,@phase-half,@phase-crescent,@terminator-close'`；仍执行 GPU 检查，天体 ID 使用冒号。旧 API 使用 `-PwithStellarView '-Pstellarview_version=0.5.4-alpha-externalview2-NeoForge'` 运行。`spaceSmokeStellarView=true` 对应显式后端选择，除 Performance 外所有档位均尊重该选择；不安装可选模组时仍回到原生。

同一次客户端的动态画质检查可使用 `'-PspaceSmokeViews=@quality-performance,@quality-balanced,@quality-high,@quality-ultra,@resize,@reload,sys1:lush'`；四个质量视点保持相同银河方向，随后验证目标重建与资源释放。

望远镜专项使用 `'-PspaceSmokeViews=@star-wide,@spyglass-star,@spyglass-overlay,@spyglass-galaxy,@spyglass-resize,@spyglass-reload,@star-return'`。夹具通过常规创造物品包装备原版望远镜并保持使用键，不伪造投影；TXT 记录真实 `scoping` 状态与原生 `opticalZoom`。`@spyglass-overlay` 同时显示原版望远镜遮罩，其余视点隐藏 GUI 便于比较；`@star-return` 验证放下后恢复。三档数值 FOV（70° / 35° / 7°）在完整与半分辨率下各验证六个亚像素位置，包括轮廓半径、通量、有限值与深度保持。

捕获固定原生动画时间。全客户端帧时间包含限帧、世界加载和后台调度；短测试的 GPU P95 会受功耗/调度影响，不可用作跨硬件性能结论。

银河专项定点视角可用 `@galaxy`、`@galaxy-outer`、`@galaxy-side`。视角在每帧保持固定，TXT 记录 `cameraYaw` / `cameraPitch`；早期夹具只在切换视点时设定相机，首个视点可能受服务器传送回包影响，比较旧捕获时应注意这一限制。

## 已知验证边界与下一步

已执行矩阵是 Minecraft 1.21.1、NeoForge 21.1.248、Windows、NVIDIA RTX 3060 Laptop 和当前工作树的 StellarView 构建。尚未覆盖 AMD、Intel、macOS、Iris/Oculus shaderpack、复杂多人航行与多个透明天体交叠。`DIRECT` 是明确可用的兼容回退。

程序银河与原生 GPU 星点在普通太空不闪烁；跃迁时保留方向收束及原有跃迁效果，Legacy 保留旧实现。恒星自转按长 tick 取模后生成相位。旧行星自转和 LOD 仍共享 float 动画 tick；长存档精度、完整相机运动、近大气网格/纹理表现继续专项处理。

接下来完善透明交叠、环光照与食影、恒星磁活动结构、近景资产/网格及多次散射优化，并补充跨硬件测量。黑洞使用方向背景、统一天体距离和线性颜色作为基础，曲线光线采样、背景星环境贴图及自旋解算仍是后续专项。

## 扩大预算检查点实测记录（5116f07，2026-09-30）

最终矩阵包括原生 High（StellarView 从 runtime 移除）、Performance（无可选模组）、旧 externalview2 / Balanced、完整 Direct + Legacy，以及定制 HDR / Ultra 的四种星 shader。普通夹具 13 视点，Ultra 矩阵 17 视点；全部覆盖资源重载与缩放。

| 场景 / 后端 | 分辨率 | TOTAL GPU P50 / P95（ms） | CPU P50（ms） |
| --- | --- | --- | --- |
| 近 Lush / Ultra HDR StellarView | 1920×1080 | 2.3828 / 2.7412 | 0.5863 |
| 气态巨行星 / Ultra HDR StellarView | 1920×1080 | 2.5416 / 3.1181 | 0.3856 |
| 壳内大气 / Ultra HDR StellarView | 1920×1080 | 3.8328 / 4.1185 | 0.3376 |
| 近 Lush / High 原生 | 1280×720 | 1.7285 / 2.5928 | 0.5530 |
| 近 Lush / Performance 原生 | 1280×720 | 1.3435 / 2.3808 | 0.4273 |
| 近 Lush / Balanced 旧版 StellarView | 1280×720 | 0.9421 / 2.8774 | 0.5612 |

硬件为 RTX 3060 Laptop，驱动报告 OpenGL 4.6 / NVIDIA 610.47；每个上述 GPU/CPU 分位数来自 256 样本的短捕获。不同运行的功耗与调度状态不同，表格用于定位成本，不用于断言预设速度排名。壳内视点是本轮最高的稳定成本之一。

GPU 大气检查记录：前景恒星颜色 `[4.9961,3.9355,2.2383]` 完全保持；背景恒星经夜侧气体透射为 `[4.9414,3.8047,2.0391]`。不透明天体深度、世界深度哨兵 0.37 与事件状态均保持。四种 StellarView shader 的 uniform 恢复检查通过；外部星场在普通天空上传计数已耗尽时仍能重建，且退出后保留该计数。Performance 报告 Bloom 分配/绘制均为零，旧版 StellarView 报告 LDR 导入次数大于零，定制版本报告直接 HDR 绘制且无 LDR 拷贝。

截图与完整计时文件位于本地 `run-space-smoke/screenshots/{native-high-final,performance,stellar-legacy-balanced,direct-final,ultra-final}`；运行日志与截图不提交到仓库。

最终普通生产 `test build` 成功：148 个测试套件、721 项测试，零失败/错误/跳过。生产 JAR 包含 Bloom 与散射 shader，不含三个渲染夹具 `SpaceRenderSmoke`、`SpaceDepthSmoke`、`StellarShaderSmoke`。定制 StellarView 的 `build publishToMavenLocal` 成功（原仓库没有单元测试）；源码补丁在最终 fork 上通过反向应用检查。

## 电影感修正验证（2026-10-01）

本轮从 `5116f07` 出发，视觉方向与近似边界见 [美术方向](space-render-art-direction.md)。最终固定视点矩阵：

| 路径 / 预设 | 实际捕获 | 分辨率 | 本地目录 |
| --- | --- | --- | --- |
| StellarView HDR2 / Ultra，含四种星 shader | 23 | 1920×1080；缩放后 960×540 | `film-final-ultra` |
| 原生 / High，可选模组从 runtime 移除 | 19 | 1280×720；缩放后 960×540 | `film-final-native` |
| 原生 / Performance | 7 | 1280×720；缩放后 960×540 | `film-final-performance` |
| 旧 externalview2 / Balanced | 5 | 1280×720；缩放后 960×540 | `film-final-legacy-stellar` |
| Direct + Legacy / Custom | 4 | 1280×720；缩放后 960×540 | `film-final-direct` |

合计 58 张截图；每组均成功退出并生成 `complete.txt`。表中目录位于 `run-space-smoke/screenshots`，截图和日志不提交。Direct 路径跳过独立管线的深度检查，其余四组执行实际 GPU 遮挡、气体透射、各档掠射晨昏线收敛与状态恢复检查。

Ultra 最终 GPU 质量检查的 RGB 为 `[0.03879,0.05319,0.06689]`，High 为 `[0.03937,0.05453,0.06940]`，Balanced 为 `[0.04010,0.05682,0.07001]`，Performance 为 `[0.04153,0.06116,0.08020]`，均在夹具规定的误差内。前景恒星 HDR 辐射保持不变；背景穿过掠射薄层后为 `[1.41699,0.31348,0.00652]`，波长衰减次序正确。不透明深度、Minecraft 世界深度哨兵 0.37 与事件状态保持。

Performance 的 Bloom 分配/绘制均为零。旧版 StellarView 记录显示颜色导入与非零拷贝；HDR2 保持直接线性绘制并通过四 shader 的 uniform/普通天空计数恢复检查。太阳光学表按需上传，资源重载后释放并在再次需要大气时重建。

普通生产 `test build` 成功：149 套件、725 项测试，零失败/错误/跳过。生产 JAR 检查确认新大气 include、光学深度实现及 `cinematic_stars.json` 存在，三个渲染夹具不存在。StellarView `f55e85f` 的 `build publishToMavenLocal` 成功，主项目保存的完整源码补丁通过反向应用检查。

本次 RTX 3060 Laptop 捕获时另有程序持续占用 GPU，功耗/频率状态与上一检查点不同；不据此发布当前成本改善或帧率排名。上一节毫秒数据仍只对应 `5116f07`。跨 GPU、shaderpack、运动稳定性和复杂透明交叠尚需后续验收。

## 银河摄影质感与云层光路验证（2026-10-01）

从 `1dda495` 出发完成方向场与薄云光路调整，最终生产 shader 的专项矩阵如下：

| 路径 / 预设 | 捕获数 | 本地目录 |
| --- | ---: | --- |
| HDR2 StellarView / Ultra，1920×1080 | 11 | `galaxy-photo-ultra-final` |
| 原生 / High，1280×720 | 8 | `galaxy-photo-native-final2` |
| 原生 / Performance，1280×720 | 6 | `galaxy-photo-performance-final` |
| Direct + Legacy / Custom | 3 | `galaxy-photo-direct-final` |
| 最终无平台干扰银河视点 / Ultra，1920×1080 | 1 | `galaxy-photo-hero-final` |

共 29 张实际截图，每组均成功退出并生成完成标记。前三组覆盖缩放和资源重载；第四组验证旧路径，按设计跳过独立管线 GPU 检查。最终银河视点另外执行完整 GPU 检查及新增的云线性合成等式检查。StellarView API 与依赖版本不变，此轮没有重复旧版本/四 shader 的专项矩阵；上一轮的结果保留在前节。

银河 GPU 数值检查固定采用 High，以相同角范围的中央区域比较原分辨率与半分辨率。最终 1920×1080 检查的 RGB 平均辐射为 `[0.02049,0.01927,0.01796]`，半分辨率为 `[0.02036,0.01915,0.01784]`，变化低于 1%；外围区域为 `[0.00229,0.00242,0.00267]`。经度接缝、极区黑位、有限辐射和天体深度保持通过。不同分辨率的固定像素区域覆盖不同角范围，不把跨运行的该均值当作亮度变化。

半覆盖率测试云在法向与掠射视点的 alpha 分别为 `0.49829` / `0.77881`。云保持前景恒星，衰减后景恒星，并满足线性 `背景 × 透射 + 云贡献`（包含半精度目标误差）；大气既有质量检查、世界深度哨兵与事件状态恢复均通过。Performance 捕获仍报告 2,000 原生星、Bloom 分配/绘制为零，细星群循环由画质分支关闭。

RTX 3060 Laptop / OpenGL 4.6 / NVIDIA 610.47 的 256 样本短捕获：1920×1080 Ultra 银河方向背景 GPU P50 / P95 为 0.9984 / 1.6343 ms，TOTAL 为 1.3517 / 2.1729 ms；1280×720 原生 High 背景为 0.7700 / 0.8991 ms。记录用于定位新增背景成本，不作不同预设的速度排名或持续帧率承诺。本轮没有新增 GPU 纹理/目标或第三方依赖。

普通生产 `test build` 成功：149 套件、725 项测试，零失败/错误/跳过。生产 JAR 含新云光学 include 和银河方向场，不含三个渲染夹具；shader JSON 解析与源码空白检查通过。截图和运行日志只保留在本地。仍需专项验证运动中的星点稳定性、其他 GPU / shaderpack，以及多个透明天体交叠；云内体积、多次散射、海面和近景资产仍未完成。

## 大气光学表上传原生崩溃修复（2026-10-01）

用户报告的 `hs_err_pid37796.log` 将 NVIDIA `nvoglv64.dll` 的访问违例定位在 `AtmosphereSolarLut.bind → glTexImage2D(FloatBuffer)`。原上传仅恢复纹理绑定，未设置 CPU 数据布局。Minecraft 1.21.1 的 `NativeImage._upload` 会留下 `UNPACK_ROW_LENGTH`、`UNPACK_SKIP_PIXELS` 和 `UNPACK_SKIP_ROWS`；这些状态可能使驱动读取超出 256 KiB 光学表缓冲区。原始 `hs_err` 未记录这些状态的具体值，不能据此还原当次完整布局。[OpenGL 像素存储规范](https://registry.khronos.org/OpenGL/specs/gl/glspec30.pdf)说明纹理上传按全局 unpack 状态解释 CPU 地址；绑定像素上传缓冲区时地址另作缓冲区偏移解释。

光学表现在仅在创建/重载时保存 unpack 布局与缓冲区绑定，设置紧密排列、零行跨度/偏移及关闭字节交换，暂时解绑像素上传缓冲区，上传后在 `finally` 恢复全部修改过的状态与纹理绑定。GPU 句柄在创建完成后才加入缓存，异常路径释放未完成的纹理。缓存绘制没有新增 OpenGL 状态查询，光学表格式、尺寸和视觉算法保持原值。

新增实际 GPU 回归覆盖三种输入状态：

| 条件 | Alignment / Row length / Skip pixels / Skip rows / Swap bytes | PBO |
| --- | --- | --- |
| 真实 NativeImage 子区域上传 | 4 / 1024 / 3 / 2 / 0 | 未绑定 |
| 外部 CPU 布局 | 8 / 2048 / 11 / 7 / 1 | 未绑定 |
| 外部像素上传缓冲区 | 8 / 2048 / 11 / 7 / 1 | 绑定仅 64 字节的测试缓冲区 |

每项先释放再重建光学表，通过 shader 的真实 sampler 读取 RG32F 纹理，逐项与 CPU 参考比较全部 65,536 个 float；同时检查调用者布局、缓冲区及纹理绑定恢复、再次绑定不上传、纹理尺寸/格式正确且无 OpenGL 错误。此项覆盖之前固定视点矩阵未覆盖的上传状态。

实际客户端使用与用户崩溃相同的 Oracle Java 21.0.8+12-LTS-250，RTX 3060 Laptop / NVIDIA 610.47。Ultra + HDR2 StellarView 捕获 4 个视点（行星、壳内大气、缩放、资源重载），移除 StellarView 的 Performance 捕获 3 个视点（行星、缩放、资源重载）；两组均正常退出，三个上传条件逐项精确匹配，既有大气/云/银河/深度及事件状态检查通过。目录为 `run-space-smoke/screenshots/{crash-fix-oracle-ultra,crash-fix-oracle-native-performance}`。

普通生产 `test build` 成功：149 套件、725 项测试，零失败/错误/跳过。生产 JAR 包含修正的 unpack 状态管理，不含三个渲染夹具。验证使用新建夹具世界，未打开用户存档；用户原始世界与其他 GPU 的实际运行仍不在本轮验证范围内。

## 原生星场默认与依赖解耦验证（2026-10-01）

本阶段从 `9034d46` 推进，采用用户确认的原生主导路线。生产 shader 的最终矩阵如下；运行使用 Oracle Java 21.0.8+12-LTS-250、Minecraft 1.21.1、NeoForge 21.1.248、RTX 3060 Laptop / NVIDIA 610.47：

| 路径 / 预设 | 捕获数 | 本地目录 |
| --- | ---: | --- |
| 原生 / Ultra，1920×1080 | 10 | `native-stars-v3-ultra` |
| HDR2 StellarView / Ultra，同视点与分辨率，含四 shader | 11 | `native-stars-v3-stellar-comparison` |
| 原生，动态切换 Performance / Balanced / High / Ultra | 7 | `native-stars-v3-quality` |
| 旧 externalview2 / Balanced | 4 | `native-stars-v3-legacy` |
| Direct + Procedural / Custom | 3 | `native-stars-v3-direct-procedural` |
| Direct + Legacy / Custom | 3 | `native-stars-v3-direct-legacy` |

合计 38 张实际客户端截图，六组均成功退出并生成 `complete.txt`。目录位于 `run-space-smoke/screenshots`；截图与日志不提交。原生运行 classpath 不含 StellarView。旧版记录非零显示颜色绘制与导入拷贝，HDR2 记录非零直接线性绘制、零导入拷贝，四 shader 的 uniform 和普通天空上传额度恢复通过；两者均未触发原生降级。Direct + Procedural 实际绘制原生 GPU 星点，Direct + Legacy 不分配独立场景目标。

前四组通过既有大气、云、银河与遮挡检查，以及新增星点像素积分检查。总通量目标为 2.0，两种分辨率、六个亚像素位置得到 `1.999174–1.999804`，相对变化约 0.032%，符合半精度目标误差；单像素峰值随星心位置合理变化。世界深度哨兵为 `0.37000003`，不透明深度及事件状态保持。Direct 两组按设计跳过独立深度/星点数值检查，但同样执行三个光学表 unpack 上传回归。

动态画质矩阵实际使用 2,000 / 6,000 / 20,000 / 32,000 星目录；Performance 的活动 Bloom 层数为零，其余分别为 3 / 4 / 5。切换与资源重载后无 shader 或目标失败，缩放后正常捕获；随后行星视点重建所需大气光学表。该运行的资源计数从启动累计，Performance 前已运行过 Balanced，不能把累计 Bloom 分配/绘制数解释为 Performance 的消耗。

256 样本的 1920×1080 原生 Ultra 银河捕获：星点 pass CPU P50 / P95 为 `0.0243 / 0.0392 ms`，GPU 为 `0.0573 / 0.6963 ms`；TOTAL GPU 为 `1.4121 / 2.5498 ms`。同设置的 StellarView 星点 GPU 为 `0.0543 / 0.4485 ms`。两个目录的族群不同，运行间背景 pass 也有明显功耗/调度差异；这些数据用于定位开销，不宣称原生更快或给出持续帧率结论。

最终普通生产 `test build` 不加入 StellarView，成功通过 150 套件、728 项测试，零失败/错误/跳过。生产 JAR 包含原生光度模型与星点光学 include，不包含三个渲染夹具。连续色温、暗星总通量及高画质目录稳定前缀由纯 Java 测试覆盖；公开 API 反射兼容由实际客户端矩阵覆盖。

当前默认路径已独立，StellarView fork 本阶段没有修改。完整相机运动、其他 GPU / shaderpack、透明交叠仍需专项验收；黑洞背景星目录查询与曲线光线积分尚未实现。

## 望远镜星点观感验证（2026-10-01）

基线为 `6e7d9a7`。修正原生星场固定屏幕像素轮廓，保留普通视角，缩窄视野时放大清晰星心与弱光学翼；单位面积亮度保持。目录、色温、尘埃与天体位置不变，不新增 GPU 目标、纹理或星目录上传。

最终客户端矩阵为 Ultra（1920×1080，缩放至 960×540）8 视点、Performance 3 视点、Direct + Procedural / Custom 3 视点，共 14 张实际截图；三个运行均成功退出并生成完成标记。目录为 `run-space-smoke/screenshots/{spyglass-native-final,spyglass-performance,spyglass-direct}`。夹具实际使用原版望远镜，普通与放下后记录 `scoping=false / opticalZoom=1`，使用中记录 `scoping=true / opticalZoom=11.448291`；缩放、重载和既有望远镜遮罩均已捕获。Performance 保持 2,000 星及零 Bloom 分配/绘制，Direct 未分配独立目标。

Ultra / Performance 各通过 36 个星点 GPU 数值条件：两种分辨率 × 三档 FOV × 六个亚像素位置。70° 单点 RMS 半径约 `0.828 px`，7° 为 `2.394 px`；轮廓扩展倍率为 `sqrt(11.448292) = 3.383532`，RMS 还包含像素积分足迹。7° 总通量为 `22.889304–22.892776`，亚像素位置变化约 0.015%；普通视角仍得到此前的 `1.999174–1.999804`。所有值有限，天体深度和世界深度哨兵 `0.37000003` 保持，事件状态恢复；既有银河、云、大气和 unpack 上传回归通过。Direct 按设计只执行上传回归。

最终普通生产 `test build` 成功：151 套件、731 项测试，零失败/错误/跳过。生产 JAR 包含焦距比例 helper 和 `OpticalZoom` uniform，不包含三个渲染夹具。纯 Java 检查覆盖普通 FOV 选择、宽视野、望远镜过渡、受伤相机旋转以及极端/无效投影。

本轮只验证原生后端，StellarView 代码没有改动，也没有重复其兼容矩阵。运行受其他程序与调度影响，短截图计时不作为性能改善结论；当前硬件仍为 Windows / RTX 3060 Laptop，其他 GPU 与缩放模组的实际运行尚未覆盖。
## 星环与透明层验证（2026-10-02）

基线为 `607a44b`。最终客户端使用 Oracle Java 21.0.8、NeoForge 21.1.248、RTX 3060 Laptop / NVIDIA 610.47。新建夹具世界，没有打开用户存档。

| 路径 / 预设 | 捕获数 | `run-space-smoke/screenshots` 下的目录 |
| --- | ---: | --- |
| 原生 Ultra，1920×1080，含缩放至 960×540 | 10 | `ring-native-ultra` |
| 最终原生 Ultra，阴影/云与资源重载复核 | 3 | `ring-native-final` |
| 原生 Performance，1280×720，含缩放与重载 | 6 | `ring-performance` |
| HDR2 StellarView Ultra，含四种星 shader 与重载 | 7 | `ring-stellar-hdr2` |
| Direct + Legacy / Custom，含缩放与重载 | 4 | `ring-direct` |

五个运行合计 30 次捕获，均成功退出并生成 `complete.txt`。前四组通过实际 GPU 光学检查，Direct 按设计仅执行光学表上传回归并跳过独立深度检查。StellarView 记录非零直接线性绘制、零导入拷贝、无原生降级；四 shader 的 viewport/uniform 与普通天空上传额度恢复检查通过。未修改 StellarView 源码或依赖接口。

新增 GPU 检查验证环的法向覆盖约 0.45166、掠射覆盖约 0.96826，相等角度透射有限；行星阴影移除直接光而保持环覆盖率，反向环影的地表亮度比例约 0.334。两种局部环分类互斥且覆盖完整绘制，前景恒星保持、背景按线性透射组合、不透明深度保持。云球交点分段与整个大气积分的实测 HDR 绝对差不超过约 0.001。额外交叠夹具证明大球边缘在小球之前时，含半径排序符合线性组合，原中心排序给出明显不同且错误的前景颜色。

事件检查扩展至五个实际纹理单元，涵盖新环密度 sampler 及太阳查表。世界深度哨兵 `0.37000003`、事件状态、资源重载和既有银河/望远镜/unpack 回归均通过。Performance 捕获视点保持 2,000 星及零 Bloom 分配/绘制；随后数值夹具会临时切到其他画质作收敛参考。Direct 不分配场景目标。

256 样本、1920×1080 最终原生 Ultra 半亮巨行星视点：TOTAL GPU P50 / P95 为 `1.8022 / 1.9210 ms`，透明天体 pass 为 `1.0537 / 1.1745 ms`；TOTAL CPU 为 `1.2930 / 2.0675 ms`。这些是局部短捕获，包含驱动与后台调度影响，不宣称相对旧版本更快，也不代表全客户端持续帧率。带云大气最多增加到三次既有快照/积分，仍复用同一目标，各画质保留其采样预算。

普通生产 `test build` 成功：152 套件、734 项测试，零失败/错误/跳过。生产 JAR 包含新排序与 ring optics/shadow include，不包含三个渲染夹具。CPU 测试用独立球面交点验证中心排序反例、壳内观察与扩展环包围体。截图和日志保留在本地；其他 GPU、shaderpack、厚云与跨天体相交体积尚未验收。
