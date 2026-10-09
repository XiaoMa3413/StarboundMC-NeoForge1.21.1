> **Historical — not current requirements.** 可能引用已删除 API 和已结束的迁移假设；不自动作为当前实施合同。当前入口见[文档索引](../README.md)。

# 太空渲染历史验证记录

以下为 PR #20 合并前的检查点证据，不是当前要求。当前合同见 [管线文档](../space-render-pipeline.md)。

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

本轮从 `5116f07` 出发，视觉方向与近似边界见 [美术方向](../space-render-art-direction.md)。最终固定视点矩阵：

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
