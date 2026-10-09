# 当前架构

平台：Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21。基线为 PR #20 合并后的 `21c4c349664351966aaf0c3d8a4c6f53eec1339a`。

- 服务端拥有玩法、库存、燃料、剧情和事务 authority；客户端只接收投影与缓存。
- `UniverseCatalog` 索引当前加载的宇宙定义，`UniverseNavigation` 提供航行几何。归属来自数据，不从 ID 前缀推断。Catalog 构建时拒绝重复身份和非法视觉资源名；space-rendered 列表只含同时具备视觉和导航的天体。
- `UniversePosition`（sector + local double）与 `UniverseDelta` 是虚拟位置和速度的正式类型。恒星 datapack 的 `position` 也使用该 codec，不再把 sector 丢进独立 x/y/z 字段。Minecraft/render API 需要 `Vec3` 时在边界派生。
- `ShipStateData` 保存飞船持久状态：schema 3、`CurrentEntry`、sector/local pose、速度和唯一 `FlightPhase` 名称。未知 datapack body ID 与原 pose 保留，缺少几何时关闭航行；损坏存档明确拒绝。
- `ClientPlanetState` 只存一份当前/目标身份及同步 pose；`ClientNetworkState` 只存真实 UI 消费的燃料。帧内曲线采样是派生视觉状态。
- `ShipPoseProvider` 以 universe pose 为 contract。不可变 `SpaceRenderContext` 可缓存一帧的 local/view `Vec3`，它不是第二份长期 authority。
- `SpaceRenderer` 负责 pass 编排；`SpaceCoordinateFrame` 负责稳定相对坐标；`SpaceSceneTarget` 负责独立 HDR/天体深度；`SpaceRenderPassState` 负责状态边界。各专项 renderer 保持分工。
- Flight/Relay 网络 phase 和终端 action 使用显式稳定 ID，未知值拒绝；终端 action ID 4 已删除，其余 ID 不变。网络协议版本为 19，同协议客户端/服务器必须匹配。
- `RelayData` 封装状态转换、dirty 与持久 journal；世界修改前排空旧异步保存并同步写入事务，写入失败中止；成功保存 chunk 后确认完成。冲突保留现场和 snapshot，进入 ERROR 等待恢复；非法 phase/UUID 不转为新 encounter。
- 打印机只有输出槽；玩家库存 reservation、钱包与队列是当前生产模型。存储版本 1 不接受退休材料槽格式，损坏任务不跳过、不补随机身份。
- StellarView 是可选外部视觉后端；反射限定在独立 package，主项目无编译依赖，失败返回原生星场。它不提供玩法状态。
- Mixin 保持薄且 fail-fast。玩法、存档、网络和 authoring 错误 fail fast / fail closed；可选视觉和 GPU 能力失败可记录并最小降级，资源重载允许恢复。

首次公开 playtest 前不承诺内部开发快照兼容。Story 与 Nova task 只读取当前 schema，旧版和未来版本均拒绝；已序列化 attachment 必须包含全部必需字段并满足当前 invariant，钱包余额缺失或负值也拒绝。新玩家通过 attachment supplier 获得 DEFAULT，全新世界通过明确的新建路径初始化。缺失/损坏的既有 authoritative state 不解释为新世界，未初始化的航行 authority 不提供假位置或假燃料。

当前渲染合同见 [pipeline](space-render-pipeline.md)，未完成工作见 [current-work](current-work.md)。`archive/` 只供明确的历史调查，不参与普通开发决策。
