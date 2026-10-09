# 太空渲染开发验证

当前渲染合同见 [pipeline](../space-render-pipeline.md)。以下描述测试方法，不代表每次提交都重新通过 GPU/视觉验收。

## 运行验证

要求 Java 21；Java 路径与 Gradle 缓存按本机环境配置。已有完整缓存时可自行添加 `--offline` 和 `--gradle-user-home`。以下为 PowerShell 示例：

```powershell
.\gradlew.bat test build
.\gradlew.bat -PspaceSmoke -PspaceSmokeLabel=isolated runClient
.\gradlew.bat -PwithStellarView -PspaceSmoke -PspaceSmokeLabel=stellarview -PspaceSmokeStellarView=true runClient
.\gradlew.bat -PspaceSmoke -PspaceSmokeLabel=minimal -PspaceSmokeMinimal runClient
```

`spaceSmoke` 显式加入 `src/renderTest/java`，普通生产构建不包含夹具。夹具创建新测试世界，运行在 `run-space-smoke`，不打开用户存档。隐藏窗口但保持渲染。

普通 `test build` / `runClient` 不解析或下载 StellarView。开发环境加 `-PwithStellarView` 才把 `gradle.properties` 指定版本放到 runtime；该定制版本尚未发布远程 Maven，仅可选验证需要先按 [StellarView 补丁说明](../../patches/stellarview/README.md)构建并安装到本地 Maven。玩家安装模组仍通过常规 mods 目录；客户端配置设为 `spaceStarfieldBackend = "STELLAR_VIEW"` 才请求该后端。

19 个视点覆盖六种行星/卫星、深空、恒星近景、银河方向、壳内近大气视点、日面/半亮面/弯月/近晨昏线/日落/夜面、玻璃舷窗、960×540 缩放及资源重载。结果写入 `run-space-smoke/screenshots/<label>`：

- PNG 为实际客户端图像；TXT 包含分辨率、GPU/驱动、样本数、CPU/GPU P50/P95 和资源计数。
- `depth-checks.txt` 使用实际 GPU shader 验证恒星在前/行星在前，分别交换绘制顺序，并检查大气对前/后景辐射的衰减、波长衰减顺序、各档掠射晨昏线的收敛、不透明天体深度、世界深度哨兵和事件状态恢复。
- 同一 GPU 检查还覆盖银河银心/外围亮度、半分辨率平均辐射、经度接缝、极区黑位，以及云层法向/掠射覆盖、前后景衰减和不透明深度保持。
- 原生星点检查直接调用生产 shader，在完整/半分辨率下分别偏移 0、0.125、0.25、0.5、0.75、1 像素，验证总通量、有限值和不透明深度保持；它不替代完整相机运动或其他 GPU 验收。
- 光学表上传检查使用真实 `NativeImage` 子区域上传留下的行跨度/偏移、外部字节交换布局及已绑定的像素上传缓冲区，逐项回读全部 65,536 个 float，验证释放后重建、缓存复用和调用者上传/纹理状态恢复；最小降级也执行此项检查。
- `complete.txt` 只在本次运行所有捕获和深度检查成功后写入。
- `pass-checks.txt` 记录真实生产渲染事件的状态与世界深度恢复帧数，并单独统计最小降级帧。
- GPU 时间用异步 timestamp 成对查询，只读取已完成结果，不使用 `glFinish`。`TOTAL` 包含事件入口状态保存和最终颜色合成；单个 pass 计时不能相加替代它。
- 生产测量可使用 JVM 参数 `-Dstarboundmc.debug.spaceProfile=true`，每 10 秒把统计写入客户端日志；默认不建立计时 query。

夹具额外参数：`-PspaceSmokeQuality=performance|balanced|high|ultra|custom`、`-PspaceSmokeWidth=1920 -PspaceSmokeHeight=1080`、`-PspaceSmokeWithoutStellarView`（从运行 classpath 排除可选模组，默认已经不加入）。四种 StellarView shader 的矩阵使用 `-PwithStellarView -PspaceSmokeStellarView=true -PspaceSmokeStellarMatrix=true`，额外捕获 4 个视点（总计 23），并检查 uniform/普通天空上传计数恢复。PowerShell 定点检查可使用 `'-PspaceSmokeViews=sys1:lush,@phase-half,@phase-crescent,@terminator-close'`；仍执行 GPU 检查，天体 ID 使用冒号。旧 API 使用 `-PwithStellarView '-Pstellarview_version=0.5.4-alpha-externalview2-NeoForge'` 运行。`spaceSmokeStellarView=true` 对应显式后端选择，除 Performance 外所有档位均尊重该选择；不安装可选模组时仍回到原生。

同一次客户端的动态画质检查可使用 `'-PspaceSmokeViews=@quality-performance,@quality-balanced,@quality-high,@quality-ultra,@resize,@reload,sys1:lush'`；四个质量视点保持相同银河方向，随后验证目标重建与资源释放。

望远镜专项使用 `'-PspaceSmokeViews=@star-wide,@spyglass-star,@spyglass-overlay,@spyglass-galaxy,@spyglass-resize,@spyglass-reload,@star-return'`。夹具通过常规创造物品包装备原版望远镜并保持使用键，不伪造投影；TXT 记录真实 `scoping` 状态与原生 `opticalZoom`。`@spyglass-overlay` 同时显示原版望远镜遮罩，其余视点隐藏 GUI 便于比较；`@star-return` 验证放下后恢复。三档数值 FOV（70° / 35° / 7°）在完整与半分辨率下各验证六个亚像素位置，包括轮廓半径、通量、有限值与深度保持。

捕获固定原生动画时间。全客户端帧时间包含限帧、世界加载和后台调度；短测试的 GPU P95 会受功耗/调度影响，不可用作跨硬件性能结论。

银河专项定点视角可用 `@galaxy`、`@galaxy-outer`、`@galaxy-side`。视角在每帧保持固定，TXT 记录 `cameraYaw` / `cameraPitch`；早期夹具只在切换视点时设定相机，首个视点可能受服务器传送回包影响，比较旧捕获时应注意这一限制。


## 证据与验证边界

数学、codec、save/load 和 GameTests 证明所覆盖的逻辑与服务端行为；固定视点/GPU smoke 证明本次机器的画面与数值，不能代替星图人工交互、航行手感、两客户端专服、连续相机运动或跨 GPU/shaderpack 验收。记录实际执行的命令、退出状态与输出，不引用旧测试数量冒充本次验证。

`@failure` 夹具视点注入一次 isolated 运行时失败，实际渲染最小降级并检查状态/世界深度保持；紧随 `@reload` 验证资源恢复。 可用 `-PspaceSmokeMinimal` 强制最小降级，普通配置不暴露此开发开关。

剩余矩阵见 [current-work](../current-work.md)。历史 benchmark 和截图仅对当时硬件、夹具与版本成立。
