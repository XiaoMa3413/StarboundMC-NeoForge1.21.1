# StarboundMC 工作进度

日期：2026-10-23
仓库：https://github.com/XiaoMa3413/StarboundMC-NeoForge1.21.1
本地位置：`D:\GPTWork\小马的mc模组\模组位置`

## 本次检查与落地

- [x] 01 确认仓库可访问、默认分支为 main，目标文件夹为空。
- [x] 02 将仓库克隆到“模组位置”，记录提交版本。
- [x] 03 阅读项目说明、构建配置、代码和资源，检查缺项与问题（武器重点静态审查）。
- [x] 04 尝试构建并记录实际结果，区分环境问题与代码问题（已有网络失败证据；构建成功另见 A05）。
- [x] 05 整理后续修改优先级、依据和验收条件（W01-a 至 W01-d、W04）。
- [ ] 06 核对本地文件与进度，交付结果。

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
- [ ] A05 原始版本执行 test/build，保存完整日志、测试统计和 JAR 路径。
- [ ] A06 启动开发客户端，确认 NeoForge/模组加载成功，保存日志。
- [ ] A07 进入独立测试世界，确认初始流程可运行；记录自动检查与人工检查的边界。

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
