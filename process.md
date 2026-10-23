# StarboundMC 工作进度

## 当前执行范围（2026-10-23 用户更新，优先于早期小刀专项计划）

完整三阶段武器体系；小刀仅修必要问题。下列数值为首版可玩基线，后续按实测平衡。

| 顺序 | 武器 / 注册 ID | 定位与首版行为 | 获取门槛 | 状态 |
| --- | --- | --- | --- | --- |
| P0 | 求生小刀 survival_knife | 修复伤害提示，单件堆叠，保留 5 伤害/无限耐久 | 初始箱子与补给打印 | 已实现并完成视觉验收 |
| P1 | 钨钢军刀 tungsten_sabre | 7 伤害、2.0 攻速、600 耐久，可靠近战 | 钨锭与 30 体素 | 行为验收通过 |
| P1 | 线圈手枪 coil_pistol | 6 伤害/32 格/12 tick 冷却，每发 1 铁粒 | 钨锭、电路板、60 体素 | 行为验收通过 |
| P2 | 耐钢战戟 durasteel_glaive | 10 伤害/1.4 攻速；右键扇形震击，冷却 60 tick、额外耐久消耗 | 钛锭、耐钢、电路与 150 体素 | 行为验收通过 |
| P2 | 等离子卡宾枪 plasma_carbine | 8 伤害/40 格/8 tick 冷却，每发 1 红石；命中短暂点燃 | 钛、耐钢、电路与 200 体素 | 行为验收通过 |
| P3 | 星核裁决刃 starcore_blade | 13 伤害/1.6 攻速；右键范围弧斩，冷却 100 tick，以耐久约束 | 耐钢、星核碎片、电路与 400 体素 | 行为验收通过 |
| P3 | 星核轨道炮 starcore_railgun | 至少蓄力 20 tick、满蓄 40 tick，18–30 伤害/64 格/最多穿透 3 敌人；每发 2 红石 | 耐钢、星核碎片、电路与 500 体素 | 行为验收通过 |

- [x] R01 确认三阶段路线与环境基线：664 测试通过、真实客户端世界已验证。
- [x] R02 基础修复与 P1 实现：武器注册、服务端战斗、模型/翻译、打印配方。
- [x] R03 P2 实现：近战技能与等离子命中效果，补充获取链。
- [x] R04 P3 实现：蓄力、穿透与毕业近战技能，限制冷却和资源消耗。
- [x] R05 行为验收：伤害、遮挡、友军/PvP、弹药、冷却、蓄力取消、配方与物品持久属性。10 项服务端 GameTest 通过，见 L020。
- [x] R06 客户端逐件模型/资源验证，正常生产打包；验收证据见 L021–L024，本地提交见 L025。

所有远程攻击使用服务端射线命中，墙体截断射线；客户端不上传伤害或命中名单。近战技能同样校验距离、视线、友军和冷却。各阶段提供近战与远程选择，现有制造站承接获取，采用不同轮廓的代码原生 3D 物品模型。

### L016 · 2026-10-23 · 完整武器体系开工

- 用户明确将范围扩展为前中期、中后期、毕业武器，早期 W01-a 至 W01-d 全套小刀专项降为基础必要修复。
- 分支 feat/weapons-improvements，工作区开工前干净；环境检查点 8460680。
- 已复核 ModItems、SurvivalKnifeItem、制造合同、打印分类与现有 GameTest 接入方式。
- 本批先落地路线、资源成本及验收范围，随后依次实现 P1/P2/P3，并分阶段记录实际结果。

### L017 · 2026-10-23 18:33 · P0/P1 首批实现与编译

- 小刀：伤害提示改为按伤害属性匹配，修正注册 ID 不一致造成的缺行；堆叠上限设为 1，补 2 铁锭 + 5 体素的打印入口。
- P1：新增钨钢军刀、线圈手枪；加入服务端射线/弹药/冷却公共逻辑，钨钢合成与两件武器打印配方，双语描述和独立 3D 模型。
- 初次沙箱编译 daemon 通信失败；使用已授权本机开发环境重跑成功，`logs/weapons-p1-compile-02.log`，17s，退出码 0。
- 编译通过仅说明源码可构建，运行行为测试统一在 R05 验收，不先勾选功能完全完成。
- 材料调查纠正：钛合金炉已有耐钢/星核精炼；缺少完整可重复获取路径，因此新增打印合成入口，沿用现有精炼逻辑。
- P2 已接入战戟扇形技能、等离子燃烧和独立 projectile 伤害类型，正在补资源并编译。

### L018 · 2026-10-23 18:43 · P2/P3 落地与首轮服务端测试

- P2 编译通过：`logs/weapons-p2-compile.log`，17s。补齐钛/耐钢合成、战戟和卡宾枪打印与双语模型资源。
- P3：星核裁决刃 2400 耐久、范围 6 格/最多 5 目标/100 tick 冷却/16 耐久；轨道炮 1536 耐久、20–40 tick 蓄力、18–30 伤害/64 格/最多 3 目标、2 红石/4 耐久/30 tick 冷却。
- 星核合成需耐钢、钻石、回响碎片及燃料晶体；全部制造使用已有体素打印系统，不绕过制造队列或钱包。
- GameTest 首次离线缺少服务端 Netty 依赖（`weapons-gametest-01.log`）；联网补齐后执行 8 项测试（`weapons-gametest-02.log`），6 通过、2 失败。
- 实际缺陷：SwordItem 的 TieredItem 基类覆盖 Properties 耐久为铁剑值；改用按武器配置构造的 BladeTier。
- 测试夹具问题：aiStep 不推进 attackStrengthTicker；改成明确设置测试玩家恢复状态，同时先断言未恢复时技能拒绝。未修改生产攻击恢复规则。
- 新增独立客户端逐件截图夹具，仅 `-PweaponSmoke` 启用；普通打包不包含测试夹具。

### L019 · 2026-10-23 18:49 · 近战问题定位与 8 项通过

- 第 3/4 轮仅近战未命中；第 5 轮增加明确的目标资格、锥角、空间查询断言，确认目标不在实体空间索引可查询范围。
- 根因是夹具把实体放到 spawn 区域外、尚未 tick 的区块；射线先做墙体查询可推进区块加载，掩盖了枪械夹具的相同条件。改到每个 GameTest 的活动区域，生产命中逻辑无需为此添加绕行方案。
- 第 6 轮 `logs/weapons-gametest-06.log`：8/8 required tests passed，35s，退出码 0；验证枪械最近目标/遮挡/消耗/冷却、等离子点燃、轨道炮取消/缺弹/穿透、同队和禁用 PvP、近战前后方/恢复/冷却、六件武器属性及配方。
- 补入 swords 与耐久附魔标签，分别支持剑类附魔和枪械耐久/经验修补；不添加无效弓弩附魔。
- 稳定规则写入 `docs/reference/weapons.md`，剩余人工生存与两客户端验收写入 current-work。
- 扩展最终验收：满蓄轨道炮与墙体、战戟目标上限、ItemStack 保存读取及实际配方材料预留。

### L020 · 2026-10-23 18:50 · 武器服务端自动验收通过

- 第 7 轮 `logs/weapons-gametest-07.log`：10/10 required tests passed，34s，退出码 0。
- 新增通过项：满蓄 30 伤害、轨道炮墙体截断与冷却、战戟最多 3 目标/隔墙拒绝、六件武器 ItemStack 保存读取、配方按声明材料实际预留。
- R02–R05 标记完成；R06 客户端逐件模型/提示验证与常规产物构建正在执行。
- 客户端命令：`./scripts/dev.ps1 -PweaponSmoke runClient --offline --console=plain`，日志 `logs/weapons-client-01.log`；独立世界不修改用户存档。

### L021 · 2026-10-23 · 英文客户端通过，修正枪械持握

- 客户端第 1 轮 `logs/weapons-client-01.log` 成功，1m9s；独立世界完成七件武器模型检查与截图，`weapons-en-first/complete.txt` 为完成标记。
- 已打开小刀、军刀、手枪与轨道炮截图检查：小刀正确显示 5 攻击伤害；军刀显示 7 伤害/2 攻速；枪械伤害、弹药、射程、冷却与名称均正常。
- 视觉问题：枪械沿用了剑类持握旋转，枪管朝向画面外侧；生成脚本为枪械单独配置第一人称旋转与位移，并重建模型。
- 第 1 轮截图保留在 `run-weapon-smoke/screenshots/weapons-en-first`；第 2 轮使用中文并重载资源，日志 `logs/weapons-client-02.log`，截图 `screenshots/weapons`。
- 本批调整为显示资源与测试夹具，不改变已通过的服务端武器规则。

### L022 · 2026-10-23 · 中文客户端与模型复查通过

- 第 2 轮 `logs/weapons-client-02.log`：BUILD SUCCESSFUL，1m17s，退出码 0；切换中文并完成资源重载、新世界载入、七件武器逐件截图与正常保存退出。
- 已打开中文手枪、战戟、卡宾枪、裁决刃、轨道炮截图检查；枪口方向已修正，立体轮廓及中文名称、伤害、攻速、弹药、冷却提示可见，无缺失模型。
- 截图证据 `run-weapon-smoke/screenshots/weapons/*.png`；每件 `.txt` 记录物品 ID 与实际最大耐久；complete.txt 表示七件均完成。
- 正在执行无测试参数的 `test build`，日志 `logs/weapons-final-build.log`；随后检查 JAR 内未包含 GameTest/RenderSmoke 夹具，记录产物校验值并创建本地提交。

### L023 · 2026-10-23 · 全量回归资源清单更新

- 第一次常规 test/build 执行 666 项测试，665 通过、1 失败：ResourceIntegrityTest 仍要求旧的 38 个配方/14 个打印配方；本次实际新增 7 个武器打印（含小刀）和 4 个材料合成。
- 更新明确数量为 49/25，保留资源类型/结构检查；同时将七件武器加入客户端模型完整性清单，增强后续资源回归。
- 原失败日志保留 `logs/weapons-final-build.log`；重新执行全量 test/build，输出 `logs/weapons-final-build-02.log`。

### L024 · 2026-10-23 · 正式产物与回归验收完成

- `logs/weapons-final-build-02.log`：BUILD SUCCESSFUL，22s，退出码 0。
- XML 汇总：666 tests、0 failures、0 errors、0 skipped；新增 2 项蓄力边界单元测试，其余已有回归全部通过。另有 10 项独立服务端 GameTest 全部通过。
- 正常 JAR：`build/libs/starboundmc-0.1-alpha.jar`，82,547,909 字节。
- SHA-256：`67e90a8d49031bfa3d9ae3cc5ec1d10311a43a0b19b997314f97b1aa565f179d`。
- 检查压缩包：六件武器的模型/配方及生产战斗类均存在；无 GameTests/RenderSmoke 类，测试夹具未混入正式包。
- 中文客户端日志的 Yggdrasil 公钥请求错误来自开发账户认证网络请求；世界成功载入并保存，不是武器资源错误。未出现武器缺失模型/贴图错误。
- 首版可玩范围完成：小刀必要修复、三个阶段各一把近战/远程、材料获取与打印、修复/附魔标签、双语提示、立体模型、测试及文档。
- 保留后续人工验收：从新档完整探索/制造、两台真实客户端 PvP 与延迟体验、长时间平衡及第三人称动作。自动 GameTest 和单机截图不冒充这些人工验收。

### L025 · 2026-10-23 · 本地功能提交检查点

- 功能提交：`0195a0b feat: add three-tier weapon progression and verified combat mechanics`，40 文件，包含生产代码、资源、测试、生成脚本及过程文档。
- 分支：`feat/weapons-improvements`；环境检查点 `8460680` → 武器功能检查点 `0195a0b`。此次只创建本地提交，没有推送远端。
- 本条作为后续文档提交保存，便于从 process 定位功能 diff；测试证据与产物校验值见 L020–L024。

### L026 · 2026-10-23 · 武器收尾与 GitHub 推送准备

- 用户明确要求收尾并推送 GitHub，授权将 `feat/weapons-improvements` 推送至 origin。
- [x] 核对工作区：生产代码已包含于 `0195a0b`，本轮仅补充过程记录，没有新增玩法变动。
- [x] 复核既有验证证据：666 项回归测试零失败，10 项服务端 GameTest 通过，七件武器客户端截图完成。
- [x] 复核正式 JAR：82,547,909 字节，SHA-256 与 L024 一致，六件新武器模型/配方齐全，未包含测试夹具。
- [x] 核对远端：main 仍为 `59e9010`；目标功能分支尚不存在，可正常首次推送。
- 上轮收尾文档提交因自动审批服务用量上限未执行；本轮重新检查通过后完成提交。功能提交 `0195a0b` 已保存，不受该中断影响。
- [ ] 完成收尾文档提交并推送功能分支，核对远端 SHA 与本地 HEAD 一致；结果追加在 L027。
- 新存档完整生存流程、两客户端体验和长期平衡继续保留在 current-work，不标记为已经人工验收。

日期：2026-10-23
仓库：https://github.com/XiaoMa3413/StarboundMC-NeoForge1.21.1
本地位置：`D:\GPTWork\小马的mc模组\模组位置`

## 本次检查与落地

- [x] 01 确认仓库可访问、默认分支为 main，目标文件夹为空。
- [x] 02 将仓库克隆到“模组位置”，记录提交版本。
- [x] 03 阅读项目说明、构建配置、代码和资源，检查缺项与问题（武器重点静态审查）。
- [x] 04 尝试构建并记录实际结果，区分环境问题与代码问题（已有网络失败证据；构建成功另见 A05）。
- [x] 05 整理后续修改优先级、依据和验收条件（W01-a 至 W01-d、W04）。
- [x] 06 核对本地文件、环境验证与进度；武器修改待后续执行。

## 记录规则

用户补充：优先完善现有武器或新增武器。审查与后续待办按武器优先排序，覆盖伤害与技能、获取途径、模型贴图、联机同步及平衡性。

完成一项即标记 `[x]` 并记录证据；未实施、受阻或未经验证的项目保持 `[ ]`。

## 执行约束与验收门槛

- 先下载源码，再配置 JDK 21、项目 Gradle Wrapper 和固定依赖。
- 原始代码必须先完成测试、构建、客户端进入世界验证。门槛未通过前，不修改武器玩法代码。
- 编译成功、客户端启动、进入世界、武器实测是不同状态，分别记录，不能互相替代。
- 优先完善现有武器，再按现有制造与探索进度设计新增武器；具体名称和注册 ID 以源码盘点为准。
- 每完成一个有意义的操作批次即追加记录：时间、对象/文件、动作、结果、证据、下一步。失败尝试也保留。
- 此文件是用户要求的工作执行记录；仓库稳定规则仍以 docs/reference 为准，待开发事项与 docs/current-work 对齐。
- 不自动推送远端。代码变更按功能记录 diff；不删除或重写此前操作记录。

## A. 环境和原始版本验证（武器修改前置）

- [x] A01 确认目标文件夹初始为空，读取远端 AGENTS、文档入口、架构、当前工作和已知问题。
- [x] A02 下载完整源码，记录分支、提交 SHA、文件数量及 git 状态。
- [x] A03 配置工作区 JDK 21，验证 java 与 javac，记录来源及版本。
- [x] A04 读取 Wrapper 版本，设置独立 Gradle 缓存和可复用启动脚本。
- [x] A05 原始版本执行 test/build，保存完整日志、测试统计和 JAR 路径。
- [x] A06 启动开发客户端，确认 NeoForge/模组加载成功，保存日志。
- [x] A07 进入独立测试世界，确认世界、模组维度与渲染可运行；自动检查与人工检查的边界见 L015。
- [ ] A08 完整序章、生存交互与武器人工实测（功能验收，环境配置完成不代表这些项目已经通过）。

## B. 武器盘点与逐件修改计划

下表区分已盘点对象与待实施工作；详细证据、状态与验收见逐项执行单。

| 编号 | 具体对象 | 需要检查/完善 | 完成条件 | 状态 |
| --- | --- | --- | --- | --- |
| W01 | 求生小刀 `starboundmc:survival_knife` | 先修伤害提示；检查堆叠和补给获取；保留现有无限耐久设定 | 详见 W01-a 至 W01-d 验收 | 待环境基线 |
| W02 | MatterManipulatorItem（物质枪，当前文档定义为采集工具） | 核实是否有战斗能力；检查与武器输入/伤害边界 | 只按当前实现记录，不擅自把采集工具改为战斗武器 | 待源码盘点 |
| W03 | 其他已注册武器 | ModItems 与源码检索未发现其他战斗武器；不把矿石、EPP 或推进器列作武器 | 后续新增武器必须补登记、资源、获取、测试 | 已完成初步盘点 |
| W04 | 新增武器候选（名称待盘点后确定） | 武器定位、攻击方式、消耗/冷却、制造材料、解锁阶段、视觉音效 | 先明确设计，接入服务端伤害与获取链，再进行实测 | 待现有武器评估 |
| W05 | 武器联机与回归 | 服务端判定、重复命中、冷却绕过、死亡/重连、资源重载 | 自动测试与两客户端/独立服务器结果分别记录 | 待功能实施 |

## C. 非武器事项（低于武器优先级）

- [ ] 核实并安排 Rocky Moon 登陆任务钩子缺失（远端 known-issues 已记录，尚未本地复现）。
- [ ] 整理生存获取链缺口：亚光速单元、AI 终端恢复等，避免误把创造原型当成可用生存内容。
- [ ] 整理 LDLib2 冷启动与机器联机验收；没有性能数据前不做猜测式优化。

## 操作记录（仅追加）

### L001 · 2026-10-23 · 仓库确认与首次下载

- 动作：通过 GitHub 连接器确认 main 分支；检查本地“模组位置”为空；创建本文件。
- 下载尝试：沙箱内代理连接失败；关闭代理后 DNS 线程失败；联网权限下直连 GitHub 超时；恢复系统代理后 Git clone 开始传输。
- 结果：下载尚未完成，不能标记 A02；未修改模组代码。

### L002 · 2026-10-23 · 需求与环境预检

- 用户要求：优先武器；先配置环境并编译、进入游戏，再修改功能；每批操作都要可回溯。
- 已读：AGENTS.md、docs/README.md、docs/architecture.md、docs/current-work.md、docs/known-issues.md、README.md、gradle.properties、build.gradle（远端只读）。
- 确认版本：Minecraft 1.21.1、NeoForge 21.1.248、LDLib2 2.2.36.a、Java 21。
- 本机预检：常规 Java 路径发现 JDK 11 与 JRE 8，尚未确认可用 JDK 21。
- 动作：下载官方 ZIP 作为备用通道；随后观察到 Git pack 已增长至约 61 MB，原 clone 仍在进行，暂不破坏该目录。
- 动作：开始下载 Microsoft OpenJDK 21 到 .setup/jdk21.zip；仅准备工作区环境，不修改全局 JAVA_HOME。
- 证据：GitHub 仓库与上述文件；官方 JDK 下载说明 https://learn.microsoft.com/en-us/java/openjdk/download 。
- 下一步：确认下载完成，读取本地规范，验证 JDK 和 Wrapper，再运行原始版本构建。

### L003 · 2026-10-23 14:04–14:06（北京时间）· 下载完成与功能分支

- 结果：Git clone 成功，1359 个已跟踪文件；原始工作区干净。
- 基线：main / origin/main，提交 `59e9010e97fe6c40f1352a439ecb3fb75a5366f3`；浅克隆保留当前提交，未下载完整历史。
- 按用户要求执行 `git switch -c feat/weapons-improvements`，成功；后续全部代码修改在该分支进行。
- JDK：Microsoft OpenJDK `21.0.12.1+1`，java 与 javac 均已运行验证。
- 安装位置：工作区 `.setup/jdk21/jdk-21.0.12.1+1`；下载改用微软官方 CDN 直连后完成。
- JDK ZIP SHA256：`192441A9D27DA813BADA974BB88B4CF64D37A9589ED37F204374D411CA5CE07F`（本地完整性标识，未据此声称已与官方校验值比对）。
- 备用仓库 ZIP 已停止下载，保留部分文件；正式源码来自成功的 Git clone。
- 文件变动：将完整 process 移入仓库，父目录 process 仅保留入口；新增 `scripts/dev.ps1`，为本项目设置 JAVA_HOME 与 `.gradle-home`，退出时恢复调用环境。
- Wrapper 固定为 Gradle 9.2.1，仓库已提供 distributionSha256Sum；未更改构建依赖版本。
- 下一步：运行原始版本 test/build，并保存输出；尚未修改武器源码。

## 武器调查补充（源码证据，待运行验证）

- 当前 ModItems 中的战斗武器为 `starboundmc:survival_knife`，中文名“求生小刀”。物质枪为独立采集工具。
- 小刀注册配置：伤害增量 4、攻速增量 -1；无耐久、不可附魔、隐藏攻速是源码注释明确说明的现有设计，不作为缺陷擅自更改。
- 优先项 W01-a：`SurvivalKnifeItem.appendHoverText` 筛选 `Item.BASE_ATTACK_DAMAGE_ID`，但 ModItems 注册使用 `starboundmc:survival_knife_damage`；两者不一致，需验证并修复伤害 tooltip 缺失。
- 优先项 W01-b：核实堆叠上限（当前未设置 stacksTo(1)）、丢失后的再获取方式；设计与实际问题分开记录。
- 优先项 W01-c：已有中英文名、模型和初始箱子 GameTest 断言；继续检查材质和实际攻击测试，不能写成“完全没有资源或测试”。
- 新武器 W04 暂不选定数值或开始实现，先通过环境与游戏验证门槛。

### L004 · 2026-10-23 14:14（北京时间）· Gradle 可用，开始基线构建

- Wrapper 官方下载停滞；终止本任务的该下载进程后，使用华为分发镜像获取同版本 ZIP。
- 完整文件 SHA256 与仓库 Wrapper 固定值一致：`72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f`。放入 `.gradle-home/wrapper/dists`，没有修改项目 distributionUrl。
- 命令：`./scripts/dev.ps1 --version`；退出码 0，Gradle 9.2.1、Launcher JVM 21.0.12.1。
- 证据：`logs/baseline-gradle-version.log`。
- 已开始：`./scripts/dev.ps1 test build --console=plain --stacktrace -Djava.net.preferIPv4Stack=true`。
- 输出：`logs/baseline-build-01.log`；仍在运行，尚不标记编译通过。
- 当前改动仅 process 与开发启动脚本，生产代码保持基线原样。

### L005 · 2026-10-23 · 基线构建网络诊断与重试

- 第一次构建长时间停在依赖解析；对本任务 Gradle daemon 做只读线程采样，发现 Apache HTTP 连接等待。
- 记录：父目录 `logs/baseline-daemon-threads.txt`；`logs/baseline-build-01.log` 保留失败/中止信息。
- 处理：终止该次构建 daemon，在被 Git 忽略的 `.gradle-home/gradle.properties` 配置本机已有代理 127.0.0.1:17891 和连接超时；不修改系统代理或仓库公共配置。
- 重试：`./scripts/dev.ps1 test build --console=plain --stacktrace --info`；日志 `logs/baseline-build-02.log`。
- 只读盘点：小刀贴图 `src/main/resources/assets/starboundmc/textures/item/survival_knife.png` 和 handheld 模型存在；未找到小刀的 JSON 再制造配方。Rocky Moon 确实未列入 `ShipStoryService.isPlanetSurface`，与已有问题文档一致，尚未本地游戏复现。
- 下一步：依赖解析与基线构建；不把网络失败记成源码编译失败。

## 武器逐项执行单（环境门槛后按顺序）

### W01-a · 求生小刀：伤害提示修复（最高优先）

- [x] 定位：`src/main/java/com/starboundmc/item/SurvivalKnifeItem.java` 与 `ModItems.java` 的属性标识不匹配。
- [ ] 在原始游戏中复现“主手标题存在但伤害行缺失”，记录截图/日志。
- [ ] 修正提示逻辑，使其读取实际攻击伤害属性；不为修提示更换已注册物品 ID 或改变武器伤害。
- [ ] 增加行为测试：基础小刀提示包含正确伤害；隐藏攻速合同继续成立；修改属性时提示不显示过期固定数值。
- [ ] 再次执行 test/build；游戏中核对中文与英文提示、主副手展示、实际攻击伤害。
- [ ] 保存修改 diff、命令、退出码、证据位置，并写入本文件日志。

### W01-b · 求生小刀：物品与战斗边界

- [ ] 复现当前最大堆叠数量，并决定是否将武器约束为单件；该项是设计改善，不冒充现有崩溃问题。
- [ ] 验证主手增伤、副手不增伤；切换物品后属性及时恢复，不残留速度或伤害。
- [ ] 验证攻击冷却，不把注释中的“适合连点”误解为无限频率全额伤害。
- [ ] 验证不会意外增加耐久、附魔能力或改变初始物资箱内容。

### W01-c · 求生小刀：生存补充获取

- [x] 确认已有初始飞船箱子获取证据，存在中英文名、模型和纹理。
- [ ] 设计丢失后的补充获取方案：优先复用体素打印站；明确材料、体素和打印时间，并记录平衡依据。
- [ ] 接入配方后验证材料不足、扣款、输出阻塞、取消退款、重启持久化；复用现有制造 authority。
- [ ] 实际生存模式完成一次获取与使用，避免只在创造栏里可用。

### W01-d · 求生小刀：表现与测试

- [ ] 检查第一/第三人称、左右手、掉落物与物品栏展示；只修实际发现的问题。
- [ ] 记录攻击手感与命中反馈，现有资源合格则保留；新增视觉资源单独说明来源。
- [ ] 两客户端/独立服务器验收单独记录，不用单人截图代替联机结果。

### W04 · 新武器候选：钨制近战武器（建议，未实现）

- 目的：连接已有 `tungsten_ingot` 与武器成长，作为求生小刀之后的制造目标。
- [ ] 先确定名称、武器种类和注册 ID，明确与小刀在攻速、伤害、耐久或特殊动作上的区别。
- [ ] 记录具体数值和材料成本，再实现；不要仅复制小刀并调高伤害。
- [ ] 补物品注册、创造栏、中文/英文、模型/贴图、配方、测试；新增 Java 类还须核对 build.gradle 的源文件白名单。
- [ ] 若有主动技能，伤害/冷却/资源消耗全部由服务端核验，客户端只负责输入和表现。
- [ ] 通过构建、独立世界和多人验证后才能勾选完成。

## 每批修改日志模板

后续 L006 起按以下格式追加，禁止仅写“已优化”：

```text
编号 / 北京时间 / 分支 / 基线或提交：
对应任务：例如 W01-a 求生小刀伤害提示
问题与复现：
本批修改文件及具体行为变化：
执行命令 / 退出码：
自动测试结果 / 游戏验证结果（分别写）：
日志、截图、diff 或提交证据位置：
未完成事项 / 下一步：
```

### L006 · 2026-10-23 14:18–14:20（北京时间）· TLS 下载失败处理

- `baseline-build-02` 退出码 1，耗时 2m18s；失败在 `createMinecraftArtifacts` 的外部工具解析，尚未执行源码编译。
- 失败依赖：AutoRenamingTool 2.0.17 all、jst-cli-bundle 2.0.10、mergetool 1.1.7 fatjar、asm-commons 9.9.1；错误为 TLS 握手被远端关闭。
- 开始第三次运行：在相同 test/build 命令上增加 `--max-workers=4 -Dhttps.protocols=TLSv1.2`，记录 `logs/baseline-build-03.log`。
- 观察：AutoRenamingTool 已下载到 Gradle 缓存；仍等待 JST 工具依赖。
- 为剩余 JST 依赖准备 curl 官方下载及 `.sha1` 校验；仅为下载工具切换，不更换依赖版本或跳过 TLS 验证。
- 文档进展：W01-a 至 W01-d 已按“求生小刀”明确检查、修改和验收步骤；W04 为钨制近战武器候选，未开始开发。
- 当前生产源码修改数：0；游戏启动验证尚未执行。

### L007 · 2026-10-23 14:22（北京时间）· 工具依赖恢复

- 第三次构建已自行下载完整 JST bundle（57,946,594 字节），之前失败的 4 项均已补齐。
- 停止 curl 的重复 JST 备用下载；未将未完成备用文件注入 Gradle 缓存。
- Gradle 正继续解析 NeoForm、NeoForge userdev 和其他开发依赖，尚未结束。
- 武器配方调查补充：打印分类由 `PrintingCategory` 与 `tags/item/printing/*.json` 控制；若加入小刀补给配方，应同步列入 survival 标签，否则会回退到材料分类。现阶段未修改配方或标签。

### L008 · 2026-10-23 14:23–14:24（北京时间）· NeoForge 环境继续解析

- 从官方仓库通过 curl 额外取得 `neoforge-21.1.248-userdev.jar`，与官方 `.sha1` 一致：`dab61062b17c39eb246328576ae1bcbd59ee780b`；文件留在父目录 .setup 作为备用。
- 原构建随后自行完成该阶段并继续 Minecraft/LWJGL 等依赖解析，因此没有手工写入 Gradle 工件缓存。
- 当前缓存已有 120 个 JAR（进度快照，不代表测试数量或构建完成）。
- 第三次构建仍运行；日志仍为 `logs/baseline-build-03.log`。

## 当前环境复用方法

在仓库目录用 PowerShell 执行（这些是操作入口，不表示每项已经通过）：

```powershell
# JDK / Gradle 版本
./scripts/dev.ps1 --version
# 当前网络环境下的测试与构建
./scripts/dev.ps1 test build --console=plain --max-workers=4 '-Dhttps.protocols=TLSv1.2'
# 构建通过后：普通开发客户端
./scripts/dev.ps1 runClient --console=plain --max-workers=4 '-Dhttps.protocols=TLSv1.2'
# 独立自动测试世界：先用于确认真实客户端加载和渲染
./scripts/dev.ps1 -PspaceSmoke -PspaceSmokeLabel=baseline '-PspaceSmokeViews=sys1:lush' runClient --console=plain --max-workers=4 '-Dhttps.protocols=TLSv1.2'
```

- Java 放在父目录 `.setup/jdk21/jdk-21.0.12.1+1`，启动脚本只改变当前执行环境。
- Gradle 缓存位于仓库 `.gradle-home`，已被忽略；代理为本机现有的 127.0.0.1:17891，只记录于该缓存目录的 gradle.properties。换机器时应改为当地可用代理或移除代理设置。
- 基线分支为 `feat/weapons-improvements`；源码来源提交见 L003。
- 游戏自动场景只证明本机客户端/世界/渲染所覆盖行为；不代替求生小刀交互或两客户端联机测试。

### L009 · 2026-10-23 · 本地提交检查点

- 分支：`feat/weapons-improvements`。
- 提交：`1d843cb`，`chore: record weapon work plan and local development environment`。
- 内容：process 详细任务/日志、scripts/dev.ps1；生产源码未修改。
- 首次 commit 因未配置 Git 作者身份失败；随后仅对该命令使用 `Codex <codex@localhost>` 自动化身份完成提交，没有修改用户全局 Git 身份，也未推送远端。
- 后续每批功能修改单独记录 diff/提交与验证结果；本提交可作为环境与计划检查点。

### L010 · 2026-10-23 14:28（北京时间）· Minecraft 客户端下载恢复

- 第三次构建已完成开发依赖、映射与原版服务端下载，但原版客户端 `.dltmp` 长时间保持 0 字节。
- curl 直连 Mojang 超时；通过系统代理下载成功，SHA1 与 Mojang 清单/官方 URL 一致：`30c73b1c5da787909b2f73340419fdf13b9def88`。
- 终止本任务的 NeoForm 下载子进程；第三次构建因此以退出码 1 结束（10m5s），不是源码编译错误。
- 把已校验文件复制到 `.gradle-home/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar`，保留所有原始日志。
- 第四次运行：相同 test/build、4 workers、TLS 1.2；输出 `logs/baseline-build-04.log`。
- 运行硬件记录：AMD Radeon 780M 与 NVIDIA RTX 4060 Laptop GPU；真实 OpenGL 使用哪张卡以客户端日志为准。

### L011 · 2026-10-23 · 源码编译完成，修复测试启动环境

- 第四次构建完成 Minecraft 开发环境、模组 Java 编译与 JAR 打包；产物 `build/libs/starboundmc-0.1-alpha.jar`，82,520,442 字节。
- test 任务失败于启动 `GradleWorkerMain`（ClassNotFoundException），尚未成功执行测试用例；不能记成测试通过。
- 启动参数文件和 classpath 包含中文工作区路径，判断为 Windows Java 参数文件编码问题；JAR 内 worker 类实际存在。
- 处理：仅在忽略的 `.gradle-home/gradle.properties` 设置 Gradle JVM `-Dfile.encoding=GBK`，同时保留 4 workers 与 TLS 1.2；Java 源码编译仍由 build.gradle 显式使用 UTF-8。
- 第五次运行：test/build，日志 `logs/baseline-build-05.log`。游戏验证仍待进行，未修改武器代码。

### L012 · 2026-10-23 17:58（北京时间）· 基线构建与测试通过

- 第五次 `test build` 退出码 0：`BUILD SUCCESSFUL in 25s`，7 个任务中 4 个执行、3 个已是最新。
- 修改本地 Gradle 参数文件编码后，测试启动正常；JUnit XML 报告已生成，共 146 个测试套件报告，具体用例数量由 XML 汇总记录。
- 日志：`logs/baseline-build-05.log`；测试报告：`build/reports/tests/test/index.html`；JAR：`build/libs/starboundmc-0.1-alpha.jar`。
- 开始客户端验证：`./scripts/dev.ps1 -PspaceSmoke -PspaceSmokeLabel=baseline '-PspaceSmokeViews=sys1:lush' runClient --console=plain --stacktrace`。
- 客户端日志：`logs/baseline-client-01.log`；场景使用独立新世界，尚未标记 A06/A07。

### L013 · 2026-10-23 · 测试统计与客户端资源准备

- XML 汇总：664 个测试，0 failures、0 errors、0 skipped（本次实际运行结果）。
- 客户端验证额外编译了仓库自带 renderTest 夹具，进入 downloadAssets 阶段；正在首次下载 Minecraft 资源索引与游戏资源。
- 本批没有修改生产代码；A06/A07 仍保持未完成，等待客户端和独立世界实际运行结果。

### L014 · 2026-10-23 18:01（北京时间）· 游戏资源下载通道调整

- Minecraft 资源索引包含 3911 项，合计约 826 MB；代理下载进展缓慢。
- 已用实际索引中的文件验证 `resources.download.minecraft.net` 直连返回 HTTP 200。
- 仅在本地 Gradle 配置的 http.nonProxyHosts 添加该域名，其他 Maven 下载继续使用已有代理。
- 停止首轮资源下载子进程，客户端第 1 轮因此中止（2m32s）；日志 `logs/baseline-client-01.log` 保留。
- 启动第 2 轮相同独立世界验证，日志 `logs/baseline-client-02.log`；已有资源缓存继续复用。

### L015 · 2026-10-23 18:05（北京时间）· 环境配置与真实游戏验证完成

- 客户端第 2 轮退出码 0：`BUILD SUCCESSFUL in 4m17s`。
- 真实 Minecraft 1.21.1 / NeoForge 21.1.248 客户端成功加载 StarboundMC 与 LDLib2，创建并进入独立世界 `space-fixture-1792749906676`，玩家实际登录。
- 已加载并正常保存主世界、飞船、Rocky Moon、Frozen、Barren、Molten 等维度；测试完成后客户端正常退出。
- 渲染证据：`run-space-smoke/screenshots/baseline/complete.txt` 记录 1 个视点完成；`pass-checks.txt` 记录 298 帧生产事件状态与深度保持。
- 实际截图：`run-space-smoke/screenshots/baseline/sys1-lush.png`，已打开检查，行星与星场正常显示。
- GPU：NVIDIA GeForce RTX 4060 Laptop GPU，OpenGL 4.6 / NVIDIA 581.42；背景、星场、HDR 目标无失败标记。完整数值见同目录 `sys1-lush.txt` 与 `depth-checks.txt`。
- 环境结论：JDK、Gradle、固定依赖、编译、664 项自动测试、真实客户端进入世界均已验证，可以开展后续武器修改与测试。
- 验收边界：本次是独立自动场景和单机集成服务器，不代表完整序章、生存制作、武器交互、两客户端联机均已验收；这些保持待办。
- 生产源码仍未修改。用户最新要求环境配置完成后回复 `1`，此处完成环境阶段检查点。
