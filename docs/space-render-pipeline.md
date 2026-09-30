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

StellarView 的可选星场具有两条导入路径：定制版本 `0.5.4-alpha-starbound-hdr2` 通过可选 `renderLinear` API 直接写入 HDR；旧 `externalview2` 版本仍绘入独立 LDR 目标，再解码并叠加。能力查找只做一次；缺失模组或不兼容 shader 回到原生星场。定制 API 的输出模式/viewport uniform 在调用后恢复，普通维度继续使用原版输出。源码补丁与重建方法见 [StellarView 定制说明](../patches/stellarview/README.md)。

若旧背景或颜色适配 shader 不可用，保留显示颜色兼容模式；独立天体深度仍可工作。资源失败导致核心 shader 缺失时切到 `DIRECT` 路径，资源重载后重新尝试。

大气已使用指数高度密度、Rayleigh / Henyey–Greenstein 相函数与单次散射，按 `背景 × 透射 + 入射散射` 组合。地面截断和太阳地影采用解析交点；在地影边界分段、向高密度区域集中采样，并用有限太阳视盘可见度连接晨昏线。地表、云顶与散射共享太阳衰减。散射前 blit 一份独立 HDR 颜色/深度，禁止采样正在写入的颜色附件。大气不写天体深度，并在前景不透明天体处截断；摄像机在壳内也可渲染。系数是沿用各行星美术 profile 的艺术化近似，尚无真实单位标定和多次散射。太阳光学深度已有共享 LUT（256×128 RG32F，额外 256 KiB）；表与颜色/强度无关，CPU 数据首次使用生成并缓存，GPU 纹理在重载/退出后释放重建，极薄的自定义壳仍使用数值积分。

Bloom 使用低分辨率 `RGBA16F` 金字塔，软阈值提取高光，逐级降采样/上采样后参与曝光合成；只处理太空目标。银河方向场区分银心隆起、盘面星云和分支尘埃吸收，保持深空黑位。细星群使用固定球面随机位置和经过像素足迹过滤的 Gaussian；经度随机种子周期连续，极区避开未定义的 `atan(0,0)`。`spaceBackgroundRadiance` 显式接收角足迹，后续曲线光线采样也需提供有效足迹。Performance 不执行细星群循环。原生与定制亮星仍采用最小像素半径及面积能量补偿。星型与辉光层次遵循 [电影美术方向](space-render-art-direction.md)，保持大量暗星、少量高亮星；HDR2 去除像素星贴图产生的菱形轮廓。

云壳把纹理覆盖率转换为法向光学深度，并按视线斜角计算透射；掠射路径采用有限曲率近似。薄云有受太阳透射、行星地影和云自身光学深度约束的前向散射，地表云影使用同一光学深度和太阳路径。现有 cloud opacity、漂移和资产编码保留；Direct 路径保留旧云光照。此实现不等于完整体积云或多次散射。

透明层按天体中心距离从远到近提交，单一天体内仍是云、环、大气。相交的云/环/不同大气层尚无完整逐片元排序；环在大气前方时可能被额外衰减。近地表纹理虽已过滤，仍受现有 4K 资产、球体网格和云壳模型限制。

## 配置与回退

编辑客户端 `starboundmc-client.toml`：

| 配置 | 默认 / 用途 |
| --- | --- |
| `spacePipelineMode` | `ISOLATED`；`DIRECT` 返回原有天体绘制路径 |
| `spaceBackgroundMode` | `PROCEDURAL`；`LEGACY` 返回旧天空与 CPU 原生星点 |
| `spaceExposure` | `1.0`；原生线性颜色模式范围 0.25–4 |
| `spaceVisualQuality` | `BALANCED`；控制星数、大气采样、Bloom 预算及现有功能组合 |
| `spaceBloomStrength` | `0.32`；范围 0–1.5，0 关闭辉光；Performance / Direct 不启用 |

| 画质 | 背景星预算 | 大气观察 / 光线采样 | Bloom 层数 / 首层尺寸 |
| --- | ---: | ---: | --- |
| Performance | 2,000（原生） | 12 / LUT（极薄壳 4） | 关闭 |
| Balanced | 6,000 | 16 / LUT（极薄壳 4） | 3 层，宽高各 1/4 |
| High / Custom | 20,000 | 20 / LUT（极薄壳 6） | 4 层，宽高各 1/2 |
| Ultra | 32,000 | 28 / LUT（极薄壳 8） | 5 层，宽高各 1/2 |

原生目录固定种子，低档是高档的稳定前缀；首次使用预算或资源重载时上传。StellarView 也按画质重建目录，但其内部分类顺序不承诺与原生一样的前缀稳定。Performance 保留大气、关闭云及可选星场；Balanced 关闭云影；Custom 保留独立功能开关。

太空颜色/深度及大气输入副本共约 24 字节/像素。1920×1080 时约 47.5 MiB，Ultra 辉光额外约 5.3 MiB；不含资产、Minecraft 原有目标及驱动开销。大气副本按需建立；尺寸变化和资源重载后重建，退出会释放。

完整视觉回退组合为 `spacePipelineMode = "DIRECT"` 和 `spaceBackgroundMode = "LEGACY"`。本地提交 `eae609c` 是独立深度迁移前的检查点。本轮另在独立 StellarView 工作树的 `feat/starbound-hdr-stars` 分支修改其渲染源码；无远程推送或玩法数据迁移。`8eab25d` 是本轮扩大视觉预算前的主项目检查点。

## 运行验证

要求 Java 21；本机安装路径 `E:/Develop/Program Files/Android Studio/jbr`。共享 Gradle 缓存含当前 NeoForge 和可选依赖，示例为 PowerShell：

```powershell
.\gradlew.bat --gradle-user-home ../.gradle-home --offline '-Dorg.gradle.java.installations.paths=E:/Develop/Program Files/Android Studio/jbr' test build
.\gradlew.bat --gradle-user-home ../.gradle-home --offline '-Dorg.gradle.java.installations.paths=E:/Develop/Program Files/Android Studio/jbr' -PspaceSmoke -PspaceSmokeLabel=isolated runClient
.\gradlew.bat --gradle-user-home ../.gradle-home --offline '-Dorg.gradle.java.installations.paths=E:/Develop/Program Files/Android Studio/jbr' -PspaceSmoke -PspaceSmokeLabel=stellarview -PspaceSmokeStellarView=true runClient
.\gradlew.bat --gradle-user-home ../.gradle-home --offline '-Dorg.gradle.java.installations.paths=E:/Develop/Program Files/Android Studio/jbr' -PspaceSmoke -PspaceSmokeLabel=direct -PspaceSmokePipeline=direct -PspaceSmokeBackground=legacy runClient
```

`spaceSmoke` 显式加入 `src/renderTest/java`，普通生产构建不包含夹具。夹具创建新测试世界，运行在 `run-space-smoke`，不打开用户存档。隐藏窗口但保持渲染。

19 个视点覆盖六种行星/卫星、深空、恒星近景、银河方向、壳内近大气视点、日面/半亮面/弯月/近晨昏线/日落/夜面、玻璃舷窗、960×540 缩放及资源重载。结果写入 `run-space-smoke/screenshots/<label>`：

- PNG 为实际客户端图像；TXT 包含分辨率、GPU/驱动、样本数、CPU/GPU P50/P95 和资源计数。
- `depth-checks.txt` 使用实际 GPU shader 验证恒星在前/行星在前，分别交换绘制顺序，并检查大气对前/后景辐射的衰减、波长衰减顺序、各档掠射晨昏线的收敛、不透明天体深度、世界深度哨兵和事件状态恢复。
- 同一 GPU 检查还覆盖银河银心/外围亮度、半分辨率平均辐射、经度接缝、极区黑位，以及云层法向/掠射覆盖、前后景衰减和不透明深度保持。
- `complete.txt` 只在本轮所有捕获和深度检查成功后写入。
- GPU 时间用异步 timestamp 成对查询，只读取已完成结果，不使用 `glFinish`。`TOTAL` 包含事件入口状态保存和最终颜色合成；单个 pass 计时不能相加替代它。
- 生产测量可使用 JVM 参数 `-Dstarboundmc.debug.spaceProfile=true`，每 10 秒把统计写入客户端日志；默认不建立计时 query。

夹具额外参数：`-PspaceSmokeQuality=performance|balanced|high|ultra|custom`、`-PspaceSmokeWidth=1920 -PspaceSmokeHeight=1080`、`-PspaceSmokeWithoutStellarView`（仅从运行 classpath 排除可选模组）。四种 StellarView shader 的矩阵可使用 `-PspaceSmokeStellarMatrix=true`，额外捕获 4 个视点（总计 23），并检查 uniform/普通天空上传计数恢复。PowerShell 定点检查可使用 `'-PspaceSmokeViews=sys1:lush,@phase-half,@phase-crescent,@terminator-close'`；仍执行 GPU 检查，天体 ID 使用冒号。旧 API 可用 `'-Pstellarview_version=0.5.4-alpha-externalview2-NeoForge'` 构建/运行。预设启用 StellarView 时，`spaceSmokeStellarView` 仅作为 Custom 的功能开关；其他档位遵循产品配置。

捕获固定原生动画时间。全客户端帧时间包含限帧、世界加载和后台调度；短测试的 GPU P95 会受功耗/调度影响，不可用作跨硬件性能结论。

银河专项定点视角可用 `@galaxy`、`@galaxy-outer`、`@galaxy-side`。视角在每帧保持固定，TXT 记录 `cameraYaw` / `cameraPitch`；早期夹具只在切换视点时设定相机，首个视点可能受服务器传送回包影响，比较旧捕获时应注意这一限制。

## 已知验证边界与下一步

已执行矩阵是 Minecraft 1.21.1、NeoForge 21.1.248、Windows、NVIDIA RTX 3060 Laptop 和当前工作树的 StellarView 构建。尚未覆盖 AMD、Intel、macOS、Iris/Oculus shaderpack、复杂多人航行与多个透明天体交叠。`DIRECT` 是明确可用的兼容回退。

原生背景在普通太空保持稳定，跃迁时才启用轻微闪烁；恒星自转按长 tick 取模后生成相位。旧行星自转和 LOD 仍共享 float 动画 tick；长存档精度、运动中星点稳定性、近大气网格/纹理表现继续专项处理。

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
