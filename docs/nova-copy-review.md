# N.O.V.A. 文案总表（审阅稿）

> 本稿从中英文语言资源中汇总 N.O.V.A. 相关广播、终端对话、任务引导和界面文案，保留资源键，方便逐条批注和修改。
> 修改本稿不会自动改动游戏资源；确认后的文案可据此回写 `zh_cn.json` / `en_us.json`。

共整理 **195 条文案**。中文与英文并列展示；带有“资源遗留”标签的条目目前没有接入当前流程，带有“交互原型”标签的条目属于终端交互原型。

资源来源：`src/main/resources/assets/starboundmc/lang/zh_cn.json`、`en_us.json`。动态任务分支也按资源键逐条列出，括号占位符保持原样。

## N.O.V.A. 广播与剧情提示

### `message.starboundmc.nova.cold.1`

- **中文：** 低温暴露正在上升。当前 EPP 加热防护不足。
- **English:** Cold exposure is rising. Your EPP heating protection is insufficient.

### `message.starboundmc.nova.cold.2`

- **中文：** 低温暴露已影响行动。请尽快返回温和环境。
- **English:** Cold exposure is impairing movement. Return to a temperate zone.

### `message.starboundmc.nova.cold.3`

- **中文：** 低温暴露严重。请立即撤离，避免冻伤。
- **English:** Severe cold exposure. Return now to avoid freezing injury.

### `message.starboundmc.nova.heat.1`

- **中文：** 热暴露正在上升。请为 EPP 安装制冷模块。
- **English:** Heat exposure is rising. Install a Cooling module in your EPP.

### `message.starboundmc.nova.heat.2`

- **中文：** 热衰竭正在削弱你的体力。请返回温和环境。
- **English:** Heat exhaustion is weakening you. Return to a temperate zone.

### `message.starboundmc.nova.heat.3`

- **中文：** 热暴露已达危险水平。立即返回安全环境，避免热损伤。
- **English:** Critical heat exposure. Return to a temperate zone now to avoid heat injury.

### `message.starboundmc.nova.oxygen.1`

- **中文：** 氧气储量低于 30%。请留意返航距离。
- **English:** Oxygen below 30%. Check your return distance.

### `message.starboundmc.nova.oxygen.2`

- **中文：** 氧气储量低于 15%。建议补氧或返回飞船。
- **English:** Oxygen below 15%. Refill or return to the ship.

### `message.starboundmc.nova.oxygen.3`

- **中文：** 氧气储量低于 5%。请立即补氧。
- **English:** Oxygen below 5%. Refill immediately.

### `message.starboundmc.nova.oxygen.4`

- **中文：** 当前没有可用供氧。请使用氧气罐，或立即返回安全区域。
- **English:** No oxygen supply available. Use a canister or return to breathable air immediately.

### `message.starboundmc.nova.prologue.core_online`

- **中文：** 核心系统已恢复。我正在评估飞船的整体状态。
- **English:** Core systems are back online. I am assessing the ship's overall status.

### `message.starboundmc.nova.prologue.emergency`

- **中文：** 检测到你已经苏醒。飞船仍有多项系统离线，我目前只能维持短暂通信。抬头显示恢复后，请跟随定位标记前往舰载人工智能终端。
- **English:** I have detected that you are awake. Several ship systems remain offline, and I can only maintain this link briefly. Once your display returns, follow the marker to the shipboard AI terminal.

### `message.starboundmc.nova.prologue.first_landing_complete` 【资源遗留：当前流程未触发】

- **中文：** 已确认你抵达星球表面，通信链路现已恢复。
- **English:** I have confirmed your arrival on the planet surface. The communications link is restored.

### `message.starboundmc.nova.prologue.locate_terminal`

- **中文：** 定位标记已经启用。请前往舰载人工智能终端，我将协助你恢复飞船系统。
- **English:** The locator is now active. Proceed to the shipboard AI terminal; I will assist you in restoring the ship's systems.

### `message.starboundmc.nova.prologue.mineral_scan_result` 【资源遗留：当前流程未触发】

- **中文：** 初步扫描已经完成。浅层地层中分布着煤、铜和铁；更深处还检测到青金石、红石，以及少量钻石。
- **English:** The preliminary scan is complete. Coal, copper, and iron are present in the upper strata; deeper layers also contain lapis lazuli, redstone, and sparse diamond deposits.

### `message.starboundmc.nova.prologue.mineral_scan_started` 【资源遗留：当前流程未触发】

- **中文：** 轨道扫描设备已启动。地层矿物扫描中................
- **English:** Orbital scanners are online. Mineral survey in progress................

### `message.starboundmc.nova.prologue.mineral_scan_sublight_hint`

- **中文：** 钻石晶体可以替代损坏的聚焦阵列。请将物质枪采矿等级提升到 1，打印亚光速引擎点火核心，然后放入飞船引擎的维修模块槽。
- **English:** Diamond crystals can replace the damaged focusing array. Upgrade your Matter Manipulator to mining tier 1, print a Sublight Ignition Core, and insert it into the ship engine's module socket.

### `message.starboundmc.nova.prologue.sublight_engine_online`

- **中文：** 亚光速引擎已上线，恒星系内导航恢复；超空间引擎仍处于损坏状态。
- **English:** Sublight engine online. In-system navigation is restored. The hyperspace engine remains damaged.

### `message.starboundmc.nova.prologue.sublight_ignition_started`

- **中文：** 聚焦阵列校准已确认。反应室开始点火，亚光速引擎将自动完成启动。
- **English:** Focusing array calibration accepted. Reactor ignition is beginning; sublight engine startup will complete automatically.

### `message.starboundmc.nova.relay.approaching`

- **中文：** 正在接近废弃中继站。目标全域真空，建议准备 Mk.II 和备用氧气。
- **English:** Approaching abandoned relay. Vacuum throughout; prepare Mk.II and spare oxygen.

### `message.starboundmc.nova.relay.arrived`

- **中文：** 已抵达中继站。沿信标从破损入口进入，回收控制室数据核心；储藏室有应急氧气。
- **English:** On station. Follow the beacon through the broken entrance. Recover the control-room core; oxygen is in storage.

### `message.starboundmc.nova.relay.completed`

- **中文：** 已回收数据核心并完成返航。中继站可再次访问。
- **English:** Data recovered and crew returned. The relay remains available for revisits.

### `message.starboundmc.nova.relay.departed`

- **中文：** 中继站已保存，之后可通过星图再次访问。
- **English:** Relay saved. You can revisit it from the starmap.

### `message.starboundmc.nova.relay.discovered`

- **中文：** N.O.V.A. 检测到附近轨道的异常信号。星图已标记一座废弃中继站。
- **English:** N.O.V.A. detected an anomalous signal in nearby orbit. An abandoned relay is marked on the starmap.

### `message.starboundmc.nova.relay.interrupted`

- **中文：** 中继站操作异常中断。现场与快照已保留，请联系管理员检查。
- **English:** Relay operation interrupted. The site and snapshot were preserved; contact an operator for inspection.

### `message.starboundmc.nova.relay.no_space`

- **中文：** 未找到安全接近位置，或目标位置已被占用。请检查飞船周边空间后重试。
- **English:** No safe approach position, or the reserved area is now occupied. Check space around the ship and retry.

### `message.starboundmc.nova.relay.recovered`

- **中文：** 数据核心已回收。请全员返回飞船，完成本次探索。
- **English:** Data core recovered. Return all crew to the ship to complete the expedition.

### `message.starboundmc.nova.task.exploration.complete`

- **中文：** 新的探索记录已收录。回来时记得领取奖励。
- **English:** New expedition recorded. Your reward will be waiting when you return.

### `message.starboundmc.nova.task.life_support.complete`

- **中文：** 生命维持设备已就绪。前往岩石卫星前，可以再准备一只应急氧气罐。
- **English:** Life support is ready. Consider taking an emergency oxygen canister before visiting Rocky Moon.

### `message.starboundmc.nova.task.lunar_sortie.complete`

- **中文：** 已返回飞船。你的第一次无氧远征完成了，舱内供氧会补充 EPP 储量。
- **English:** Welcome back. Your first airless expedition is complete. Cabin life support will replenish your EPP.

### `message.starboundmc.nova.task.repair.complete` 【资源遗留：当前流程未触发】

- **中文：** 亚光速引擎已恢复。我们可以启程了。
- **English:** Sublight engine restored. We are ready to depart.

### `message.starboundmc.nova.task.reward` 【资源遗留：当前流程未触发】

- **中文：** 已领取物质枪模块。
- **English:** Matter Manipulator Module received.

### `message.starboundmc.nova.task.surface.complete` 【资源遗留：当前流程未触发】

- **中文：** 已抵达地表。终端有一枚物质枪模块可领取。
- **English:** Surface arrival confirmed. A Matter Manipulator Module awaits at the terminal.

### `message.starboundmc.nova.tutorial.matter_manipulator`

- **中文：** 建议先探索附近区域，并使用物质枪收集基础资源。按住右键即可启动采矿激光。
- **English:** Explore the surrounding area and use the Matter Manipulator to gather basic resources. Hold right-click to activate the mining laser.

### `message.starboundmc.nova.tutorial.voxel_intro`

- **中文：** 检测到新的可用资源：体素。它是一种经过标准化编码的基础物质单位，也是星际社会常用的交换媒介。
- **English:** New usable resource detected: voxel. It is a standardized unit of matter and a common medium of exchange across settled space.

### `message.starboundmc.nova.tutorial.voxel_use` 【资源遗留：当前流程未触发】

- **中文：** 在物质打印中，基础原料提供物品的主要材质，体素则按照蓝图补全所需成分，并构造常规加工难以实现的精密结构。只需基础原料和少量体素，打印站就能制造复杂元件与工具。
- **English:** In material printing, base resources provide the primary substance while voxels complete the blueprint and construct precision structures that conventional processing cannot easily produce. With basic materials and a small amount of voxels, a printing station can fabricate complex components and tools.

### `message.starboundmc.nova.tutorial.wood_acquired` 【资源遗留：当前流程未触发】

- **中文：** 很好，你已经获得了木材。这是一种用途广泛的基础资源。根据当前数据，你的生存概率提高了 17 个百分点。
- **English:** Good. You have acquired wood. It is a versatile basic resource; based on current data, your survival probability has increased by 17 percentage points.

## 终端剧情与维修对话

### `gui.starboundmc.ship_ai.prologue.action.reboot_core`

- **中文：** 重启核心系统
- **English:** REBOOT CORE SYSTEMS

### `gui.starboundmc.ship_ai.prologue.action_pending`

- **中文：** 正在处理…
- **English:** Processing…

### `gui.starboundmc.ship_ai.prologue.boot.navigation_index_failed` 【资源遗留：当前流程未触发】

- **中文：** 航行数据索引重建失败
- **English:** Navigation data index rebuild failed

### `gui.starboundmc.ship_ai.prologue.boot.nova_online` 【资源遗留：当前流程未触发】

- **中文：** N.O.V.A. 核心已上线
- **English:** N.O.V.A. core is online

### `gui.starboundmc.ship_ai.prologue.boot.personality_matrix` 【资源遗留：当前流程未触发】

- **中文：** 人工智能人格矩阵恢复中……
- **English:** Restoring artificial-intelligence personality matrix...

### `gui.starboundmc.ship_ai.prologue.boot.restarting`

- **中文：** 核心系统重启中…
- **English:** Restarting core systems…

### `gui.starboundmc.ship_ai.prologue.boot.restore_power` 【资源遗留：当前流程未触发】

- **中文：** 核心供电恢复中……
- **English:** Restoring core power...

### `gui.starboundmc.ship_ai.prologue.current_objective.summary`

- **中文：** 当前目标：通过飞船传送器前往星球表面。抵达后，我会继续提供环境分析与行动建议。
- **English:** Current objective: use the ship teleporter to reach the planet surface. I will provide further environmental analysis and guidance after you arrive.

### `gui.starboundmc.ship_ai.prologue.first_contact.greeting`

- **中文：** 舰载系统已恢复。你的生命体征稳定。很高兴再次与你取得联系。
- **English:** Shipboard systems are back online. Your vital signs are stable. It is good to be in contact with you again.

### `gui.starboundmc.ship_ai.prologue.incompatible`

- **中文：** 该世界使用了更新的剧情状态格式。请更新模组后再操作终端。
- **English:** This world uses a newer story-state format. Update the mod before operating this terminal.

### `gui.starboundmc.ship_ai.prologue.landing.briefing`

- **中文：** 传送器仍可使用。先前往地表，再用物质枪采集一块原木，我就能启动矿物扫描。
- **English:** The teleporter is ready. Reach the surface and use the Matter Manipulator to collect a log; that will let me begin the mineral survey.

### `gui.starboundmc.ship_ai.prologue.late_join.summary`

- **中文：** 核心系统已经恢复。我正在整理飞船现状和当前可执行的任务。
- **English:** Core systems are back online. I am compiling the ship's current status and the tasks available to you.

### `gui.starboundmc.ship_ai.prologue.option.back_to_summary` 【资源遗留：当前流程未触发】

- **中文：** 返回状况总结
- **English:** Back to situation summary

### `gui.starboundmc.ship_ai.prologue.option.current_status`

- **中文：** 查看当前状态
- **English:** Review current status

### `gui.starboundmc.ship_ai.prologue.option.next`

- **中文：** 我接下来应该做什么？
- **English:** What should I do next?

### `gui.starboundmc.ship_ai.prologue.option.repair`

- **中文：** 如何安装点火核心？
- **English:** How do I install the ignition core?

### `gui.starboundmc.ship_ai.prologue.syncing`

- **中文：** 正在同步飞船状态……
- **English:** Synchronizing ship state...

### `gui.starboundmc.ship_ai.prologue.topic.current_location.option`

- **中文：** 我们现在在哪里？
- **English:** Where are we now?

### `gui.starboundmc.ship_ai.prologue.topic.current_location.response`

- **中文：** 定位已经完成。幸运的是，我们目前位于一颗宜居星球的轨道上。初步扫描表明地表环境适合活动，尚未发现会立即威胁生命的因素。
- **English:** Our position is confirmed. Fortunately, we are currently in orbit around a habitable planet. Preliminary scans indicate a viable surface, with no immediate threats to life detected.

### `gui.starboundmc.ship_ai.prologue.topic.flight_capability.option`

- **中文：** 飞船还能航行吗？
- **English:** Can the ship still travel?

### `gui.starboundmc.ship_ai.prologue.topic.flight_capability.response`

- **中文：** 推进系统已经离线，飞船暂时无法依靠自身动力离开轨道。传送系统仍可使用；建议优先恢复亚光速引擎。
- **English:** The propulsion systems are offline, and the ship cannot leave orbit under its own power. The teleporter remains operational; I recommend restoring the sublight engine first.

### `gui.starboundmc.ship_ai.prologue.topic.incident.option`

- **中文：** 发生了什么？
- **English:** What happened?

### `gui.starboundmc.ship_ai.prologue.topic.incident.response`

- **中文：** 飞船遭遇了一场高强度宇宙风暴，随后偏离原定航线。部分导航数据在事故中丢失，我仍在分析具体原因。
- **English:** The ship encountered a high-intensity cosmic storm and was forced off its original course. Part of the navigation data was lost during the incident. I am still determining the exact cause.

### `gui.starboundmc.ship_ai.prologue.topic.nova_identity.option`

- **中文：** 你是谁？
- **English:** Who are you?

### `gui.starboundmc.ship_ai.prologue.topic.nova_identity.response`

- **中文：** 我是 N.O.V.A.，全称 Navigation, Operations & Vessel Assistant（导航、运行与舰船助理）。我负责飞船导航、运行协调与舰载系统监测。
- **English:** I am N.O.V.A.: Navigation, Operations & Vessel Assistant. I coordinate navigation, vessel operations, and onboard system monitoring.

### `gui.starboundmc.ship_ai.prologue.topic.read_marker`

- **中文：** 已阅读
- **English:** READ

### `gui.starboundmc.ship_ai.repair.complete`

- **中文：** 亚光速引擎已上线，恒星系内航行可用；超空间引擎仍处于损坏状态。
- **English:** Sublight engine online. In-system travel is available; the hyperspace engine remains damaged.

### `gui.starboundmc.ship_ai.repair.igniting`

- **中文：** 亚光速引擎正在点火，序列会自动完成。
- **English:** Sublight ignition in progress. The sequence will finish automatically.

### `gui.starboundmc.ship_ai.repair.install_core`

- **中文：** 请将亚光速引擎点火核心放入飞船引擎的维修模块槽。消耗一个核心，即可为全船启动点火序列。
- **English:** Take the Sublight Ignition Core to the ship engine and insert it into the module socket. One core starts ignition for the entire ship.

### `gui.starboundmc.ship_ai.repair.need_upgrade`

- **中文：** 请先在打印出的物质枪升级工作台，将你自己的物质枪采矿等级提升到 1。
- **English:** Upgrade your own Matter Manipulator to mining tier 1 at the printed upgrade workbench.

### `gui.starboundmc.ship_ai.repair.print_core`

- **中文：** 请在体素打印站制作亚光速引擎点火核心，然后放入飞船引擎的维修模块槽。
- **English:** Print a Sublight Ignition Core at the voxel printer, then insert it into the ship engine's module socket.

## 任务引导与阶段提示

### `gui.starboundmc.guidance.claim.help`

- **中文：** 目标已完成，物质枪模块可在已完成任务中领取。
- **English:** This objective is complete. Its Matter Manipulator Module is ready to claim in Completed Missions.

### `gui.starboundmc.guidance.claim.next`

- **中文：** 领取物质枪模块奖励。
- **English:** Claim the Matter Manipulator Module reward.

### `gui.starboundmc.guidance.complete.help`

- **中文：** 已完成，无需再执行操作。
- **English:** Complete. No further action is required.

### `gui.starboundmc.guidance.complete.next`

- **中文：** 该任务已完成。
- **English:** This mission is complete.

### `gui.starboundmc.guidance.contact.help`

- **中文：** 找到舰载终端，在通讯页重启核心并确认身份。四份状况报告均为可选。
- **English:** Find the shipboard terminal, open Comms, reboot the core, and confirm your identity. The four situation reports are optional.

### `gui.starboundmc.guidance.contact.next`

- **中文：** 重启 N.O.V.A.，并确认身份。
- **English:** Reboot N.O.V.A. and confirm your identity.

### `gui.starboundmc.guidance.exploration.help`

- **中文：** 选择与首次登陆不同类型的星球；引擎修复后，新的登陆记录会推进此任务。
- **English:** Choose a type different from your first landing. A new surface visit counts after the engine is repaired.

### `gui.starboundmc.guidance.exploration.next`

- **中文：** 通过星图前往另一种类型的星球，再登陆地表。
- **English:** Use the Starmap to visit a different planet type, then land.

### `gui.starboundmc.guidance.life_support.help`

- **中文：** EPP 占用独立的环境装备位。氧气随 EPP 保存；飞船舱和补给站可以补氧。
- **English:** An EPP uses its own environment slot and stores its oxygen. The ship cabin and life support stations can refill it.

### `gui.starboundmc.guidance.life_support.next`

- **中文：** 制作并装备 EPP，然后在飞船舱内补氧。
- **English:** Print and equip an EPP, then refill it in the ship cabin.

### `gui.starboundmc.guidance.locked.exploration.help`

- **中文：** 先修复亚光速引擎。
- **English:** Restore the sublight engine first.

### `gui.starboundmc.guidance.locked.life_support.help`

- **中文：** 先完成初次地表行动。
- **English:** Complete the first surface expedition first.

### `gui.starboundmc.guidance.locked.lunar_sortie.help`

- **中文：** 先准备生命维持装备并修复亚光速引擎。
- **English:** Prepare life support and restore the sublight engine first.

### `gui.starboundmc.guidance.locked.repair.help`

- **中文：** 先完成初次地表行动。
- **English:** Complete the first surface expedition first.

### `gui.starboundmc.guidance.locked.surface.help`

- **中文：** 完成重新建立联络后即可开始地表行动。
- **English:** Complete Reestablish Contact to begin the surface expedition.

### `gui.starboundmc.guidance.lunar_sortie.help`

- **中文：** 留意氧气储量并为返航留出余量。氧气罐需要两秒使用时间，请提前补氧。
- **English:** Watch your oxygen reserve and save enough for the return trip. Canisters take two seconds to use, so refill early.

### `gui.starboundmc.guidance.lunar_sortie.next`

- **中文：** 装备有氧气的 EPP，前往岩石卫星探索，再安全返航。
- **English:** Take a charged EPP to Rocky Moon, explore, and return safely.

### `gui.starboundmc.guidance.lunar_sortie.return.help`

- **中文：** 你已抵达岩石卫星。留出氧气返航；安全返回后任务完成。
- **English:** You have reached Rocky Moon. Keep enough oxygen for the return; the task completes when you return safely.

### `gui.starboundmc.guidance.lunar_sortie.return.next`

- **中文：** 带着仍可工作的 EPP 返回飞船舱内。
- **English:** Return to the ship cabin with your EPP still working.

### `gui.starboundmc.guidance.repair.igniting.help`

- **中文：** 点火序列正在自动运行，完成后即可进行恒星系内航行。
- **English:** Ignition runs automatically. When it finishes, in-system travel will be available.

### `gui.starboundmc.guidance.repair.igniting.next`

- **中文：** 引擎点火中。等待序列自动完成。
- **English:** The engine is igniting. Wait for the sequence to finish.

### `gui.starboundmc.guidance.repair.install.help`

- **中文：** 安装时必须携带采矿等级 1 的物质枪。插入一个核心后，引擎会为全船启动点火。
- **English:** You must carry a mining tier 1 Matter Manipulator to install the core. One core starts ignition for the whole ship.

### `gui.starboundmc.guidance.repair.install.next`

- **中文：** 带上采矿等级 1 的物质枪和点火核心，到飞船引擎模块槽安装。
- **English:** Carry a mining tier 1 Matter Manipulator and the ignition core to the ship engine module socket.

### `gui.starboundmc.guidance.repair.print.help`

- **中文：** 查看打印站配方并准备材料；扫描结果已指出所需矿物。
- **English:** Check the printer recipe and gather its materials; the survey result identifies the required ore.

### `gui.starboundmc.guidance.repair.print.next`

- **中文：** 在体素打印站制作亚光速点火核心。
- **English:** Print a Sublight Ignition Core at the voxel printer.

### `gui.starboundmc.guidance.repair.scanning.help`

- **中文：** 扫描正在分析地层。完成后会报告钻石信号和修复建议。
- **English:** The survey is analyzing the planetary strata. Its result will report diamond traces and repair guidance.

### `gui.starboundmc.guidance.repair.scanning.next`

- **中文：** 等待矿物扫描完成，然后查看结果。
- **English:** Wait for the mineral survey to finish, then check its result.

### `gui.starboundmc.guidance.repair.upgrade.help`

- **中文：** 点火核心需要钻石。先制作升级工作台，再将自己的物质枪升级到采矿等级 1。
- **English:** The ignition core requires diamonds. Print an upgrade workbench, then raise your own Matter Manipulator to mining tier 1.

### `gui.starboundmc.guidance.repair.upgrade.next`

- **中文：** 在升级工作台把物质枪采矿等级提升至 1。
- **English:** Upgrade your Matter Manipulator to mining tier 1 at the workbench.

### `gui.starboundmc.guidance.repair.wood.help`

- **中文：** 首次采集原木后会开始扫描；无需先阅读状况报告。
- **English:** The survey begins after your first log pickup. You do not need to read the situation reports first.

### `gui.starboundmc.guidance.repair.wood.next`

- **中文：** 用物质枪采集一块原木，启动矿物扫描。
- **English:** Use the Matter Manipulator to collect a log and start the mineral survey.

### `gui.starboundmc.guidance.surface.help`

- **中文：** 在传送器选择星球表面。登陆后按提示使用物质枪采集木材，启动矿物扫描。
- **English:** Select the planet surface at the teleporter. After landing, follow the Matter Manipulator prompt to collect wood and start the mineral survey.

### `gui.starboundmc.guidance.surface.next`

- **中文：** 使用飞船传送器前往星球表面。
- **English:** Use the ship teleporter to reach the planet surface.

## 任务标题、说明与归档文本

### `gui.starboundmc.task.contact.archive`

- **中文：** 在任务页查看目标与奖励，在通讯页与 NOVA 交谈。追踪的任务会显示在总览中。
- **English:** View objectives and rewards in Missions, or speak with NOVA in Comms. Your tracked mission appears in Overview.

### `gui.starboundmc.task.contact.archive_title`

- **中文：** 舰载终端与任务
- **English:** Terminal and Missions

### `gui.starboundmc.task.contact.scope`

- **中文：** 主线 · 联络
- **English:** Story · Contact

### `gui.starboundmc.task.contact.title`

- **中文：** 重新建立联络
- **English:** Reestablish Contact

### `gui.starboundmc.task.exploration.archive`

- **中文：** 繁茂、荒芜、冰冻与熔融星球有着不同的地表环境。在星图导航台查看航行条件，抵达轨道后通过传送器登陆。
- **English:** Lush, barren, frozen and molten worlds have distinct surface environments. Check travel requirements at the starmap console, then use the teleporter to land after reaching orbit.

### `gui.starboundmc.task.exploration.archive_title`

- **中文：** 星图与新的星球
- **English:** Starmap and New Planets

### `gui.starboundmc.task.exploration.scope`

- **中文：** 探索 · 新的星球
- **English:** Exploration · New worlds

### `gui.starboundmc.task.exploration.title`

- **中文：** 另一片天空
- **English:** Another Sky

### `gui.starboundmc.task.life_support.archive`

- **中文：** EPP 使用独立装备位，氧气随设备保存。普通可呼吸环境停止耗氧；飞船舱内自动补氧。补给站可为 EPP 和氧气罐补氧。
- **English:** EPPs have a separate equipment slot. Oxygen stays with the device. Breathable environments stop consumption; the ship cabin refills the pack. Life support stations refill packs and canisters.

### `gui.starboundmc.task.life_support.scope`

- **中文：** 探索准备 · 个人装备
- **English:** Preparation · Personal equipment

### `gui.starboundmc.task.life_support.title`

- **中文：** 准备生命维持装备
- **English:** Prepare Life Support

### `gui.starboundmc.task.lunar_sortie.archive`

- **中文：** 氧气罐一次补充 180 单位氧气，使用后留下空罐。飞船之外的月球前哨默认无氧；补给仓中的满罐可延长探索。
- **English:** A canister provides 180 oxygen units and leaves an empty container. Lunar outposts remain airless; full canisters in supply caches can extend your expedition.

### `gui.starboundmc.task.lunar_sortie.scope`

- **中文：** 远征 · 岩石卫星
- **English:** Expedition · Rocky Moon

### `gui.starboundmc.task.lunar_sortie.title`

- **中文：** 第一次无氧远征
- **English:** First Airless Expedition

### `gui.starboundmc.task.repair.archive`

- **中文：** 点火核心需要 3 颗钻石和 50 体素，打印耗时 5 秒。安装时需携带采矿等级 1 的物质枪。将核心放入飞船引擎维修槽，等待 3 秒完成点火。全体船员共用修复后的引擎。
- **English:** Printing an ignition core costs 3 diamonds and 50 voxels and takes 5 seconds. Carry a mining tier 1 Matter Manipulator when installing it in the ship engine repair slot. Ignition takes 3 seconds. The restored engine serves the whole crew.

### `gui.starboundmc.task.repair.archive_title`

- **中文：** 点火核心与飞船引擎
- **English:** Ignition Core and Ship Engine

### `gui.starboundmc.task.repair.scope`

- **中文：** 主线 · 飞船维修
- **English:** Story · Ship repair

### `gui.starboundmc.task.repair.title`

- **中文：** 恢复亚光速航行
- **English:** Restore Sublight Travel

### `gui.starboundmc.task.surface.archive`

- **中文：** 使用飞船传送器往返地表，使用物质枪采集资源。升级工作台可在体素打印机中制作，需要 50 体素及配方材料。
- **English:** Use the ship teleporter to travel to the surface and gather resources with the Matter Manipulator. Print an upgrade workbench using 50 voxels and its recipe materials.

### `gui.starboundmc.task.surface.archive_title`

- **中文：** 传送、采集与升级
- **English:** Teleporting, Gathering and Upgrades

### `gui.starboundmc.task.surface.scope`

- **中文：** 探索 · 地表
- **English:** Exploration · Surface

### `gui.starboundmc.task.surface.title`

- **中文：** 第一次地表行动
- **English:** First Surface Expedition

## 任务面板与状态/奖励文案

### `gui.starboundmc.tasks.active`

- **中文：** 进行中
- **English:** Active

### `gui.starboundmc.tasks.ask`

- **中文：** 询问 NOVA
- **English:** Ask NOVA

### `gui.starboundmc.tasks.ask_question`

- **中文：** 这个任务我该怎么做？
- **English:** How should I approach this mission?

### `gui.starboundmc.tasks.chapter_done`

- **中文：** 当前任务已全部完成。新的旅程由你决定。
- **English:** All current missions are complete. Choose your next destination.

### `gui.starboundmc.tasks.claim`

- **中文：** 领取奖励
- **English:** Claim reward

### `gui.starboundmc.tasks.claim_hint`

- **中文：** 完成任务后可领取奖励。
- **English:** Complete this mission to claim its reward.

### `gui.starboundmc.tasks.claimed`

- **中文：** 奖励已领取
- **English:** Reward claimed

### `gui.starboundmc.tasks.complete`

- **中文：** ✓ 任务已完成
- **English:** ✓ Mission completed

### `gui.starboundmc.tasks.completed`

- **中文：** 已完成
- **English:** Completed

### `gui.starboundmc.tasks.in_progress`

- **中文：** 当前目标
- **English:** Current objective

### `gui.starboundmc.tasks.inventory_full`

- **中文：** 背包已满，请腾出空间后领取。
- **English:** Inventory full. Make room to claim your reward.

### `gui.starboundmc.tasks.locked`

- **中文：** 请先完成上一项任务
- **English:** Complete the preceding mission first

### `gui.starboundmc.tasks.main`

- **中文：** 主线
- **English:** Main missions

### `gui.starboundmc.tasks.module_reward`

- **中文：** 完成奖励：物质枪模块 ×%s
- **English:** Reward: Matter Manipulator Module ×%s

### `gui.starboundmc.tasks.open`

- **中文：** 查看任务详情
- **English:** Mission details

### `gui.starboundmc.tasks.pending_rewards`

- **中文：** 物质枪模块 ×%s
- **English:** Matter Manipulator Modules ×%s

### `gui.starboundmc.tasks.preparation`

- **中文：** 探索准备
- **English:** Expedition prep

### `gui.starboundmc.tasks.related`

- **中文：** 查看关联任务
- **English:** Related mission

### `gui.starboundmc.tasks.repair_steps`

- **中文：** 准备进度<br>%s 升级物质枪采集能力<br>%s 获得点火核心
- **English:** Preparation<br>%s Upgrade manipulator mining<br>%s Obtain an ignition core

### `gui.starboundmc.tasks.review_rewards`

- **中文：** 查看已完成任务
- **English:** Completed missions

### `gui.starboundmc.tasks.rewards`

- **中文：** 待领取奖励
- **English:** Unclaimed rewards

### `gui.starboundmc.tasks.track`

- **中文：** 追踪任务
- **English:** Track mission

### `gui.starboundmc.tasks.unlock_reward`

- **中文：** 完成后：解锁地表探索指引。
- **English:** Unlocks: surface exploration guidance.

### `gui.starboundmc.tasks.untrack`

- **中文：** 取消追踪
- **English:** Stop tracking

### `gui.starboundmc.tasks.waiting`

- **中文：** 请稍候…
- **English:** Please wait…

## 指令终端界面文案

### `gui.starboundmc.command.archive`

- **中文：** 档案
- **English:** Archive

### `gui.starboundmc.command.archive_intro`

- **中文：** 探索中收集的资料与设备说明。
- **English:** Field notes and equipment references.

### `gui.starboundmc.command.close`

- **中文：** ×
- **English:** ×

### `gui.starboundmc.command.comms`

- **中文：** 通讯
- **English:** Comms

### `gui.starboundmc.command.comms_hint`

- **中文：** 点击对话可立即显示全文。
- **English:** Click a message to reveal the full text.

### `gui.starboundmc.command.empty_tasks`

- **中文：** 暂无任务。
- **English:** No missions here.

### `gui.starboundmc.command.overview`

- **中文：** 总览
- **English:** Overview

### `gui.starboundmc.command.tasks`

- **中文：** 任务
- **English:** Missions

### `gui.starboundmc.command.welcome`

- **中文：** 当前目标
- **English:** CURRENT OBJECTIVE

## 终端通用状态与交互文案

### `gui.starboundmc.ship_ai.demo.notice` 【交互原型】

- **中文：** 本地交互原型；尚未接入剧情与飞船状态。
- **English:** Local interaction prototype. Story and ship state are not connected.

### `gui.starboundmc.ship_ai.demo.option.identity` 【交互原型】

- **中文：** 你是谁？
- **English:** Who are you?

### `gui.starboundmc.ship_ai.demo.option.projection` 【交互原型】

- **中文：** 说明当前投影。
- **English:** Explain this projection.

### `gui.starboundmc.ship_ai.demo.option.status` 【交互原型】

- **中文：** 报告终端状态。
- **English:** Report terminal status.

### `gui.starboundmc.ship_ai.demo.response.identity` 【交互原型】

- **中文：** 我是 N.O.V.A.，全称为 Navigation, Operations & Vessel Assistant（导航、运行与舰船助理）。当前原型使用本地预设对话。
- **English:** I am N.O.V.A.: Navigation, Operations & Vessel Assistant. This prototype uses scripted local dialogue.

### `gui.starboundmc.ship_ai.demo.response.projection` 【交互原型】

- **中文：** 这是一套青白色合成视觉界面。眼部亮度与投影稳定度用于表示我的当前状态。
- **English:** This cyan-white construct is a synthetic visual interface. Its eye brightness and projection stability indicate my current state.

### `gui.starboundmc.ship_ai.demo.response.status` 【交互原型】

- **中文：** 终端当前运行于本地原型模式。剧情进度与共享飞船状态尚未接入。
- **English:** The terminal is operating in local prototype mode. Story progression and shared ship state remain disconnected.

### `gui.starboundmc.ship_ai.demo.welcome` 【交互原型】

- **中文：** 舰载界面已上线。请选择一项诊断提示，以测试流式文字与终端交互。
- **English:** Shipboard interface online. Select a diagnostic prompt to test streaming text and terminal controls.

### `gui.starboundmc.ship_ai.log`

- **中文：** 与 NOVA 交谈
- **English:** Conversation with NOVA

### `gui.starboundmc.ship_ai.portrait.identity`

- **中文：** N.O.V.A. 投影
- **English:** N.O.V.A. PROJECTION

### `gui.starboundmc.ship_ai.portrait.idle`

- **中文：** 信号稳定
- **English:** SIGNAL STABLE

### `gui.starboundmc.ship_ai.portrait.incompatible`

- **中文：** 接口锁定
- **English:** INTERFACE LOCKED

### `gui.starboundmc.ship_ai.portrait.offline`

- **中文：** 信号中断
- **English:** SIGNAL LOST

### `gui.starboundmc.ship_ai.portrait.prototype`

- **中文：** 投影待接入
- **English:** PROJECTION PENDING

### `gui.starboundmc.ship_ai.portrait.recovering`

- **中文：** 正在恢复
- **English:** RECOVERING

### `gui.starboundmc.ship_ai.portrait.speaking`

- **中文：** 正在传输
- **English:** TRANSMITTING

### `gui.starboundmc.ship_ai.portrait.syncing`

- **中文：** 正在同步
- **English:** SYNCHRONIZING

### `gui.starboundmc.ship_ai.receiving`

- **中文：** 正在接收传输……
- **English:** Receiving transmission...

### `gui.starboundmc.ship_ai.remote_link`

- **中文：** 远程链路
- **English:** REMOTE LINK

### `gui.starboundmc.ship_ai.return_latest`

- **中文：** 回到最新
- **English:** LATEST

### `gui.starboundmc.ship_ai.speaker.nova`

- **中文：** N.O.V.A.
- **English:** N.O.V.A.

### `gui.starboundmc.ship_ai.speaker.player`

- **中文：** 你
- **English:** YOU

### `gui.starboundmc.ship_ai.speaker.system`

- **中文：** 系统
- **English:** SYSTEM

### `gui.starboundmc.ship_ai.status.hyperdrive.damaged` 【资源遗留：当前流程未触发】

- **中文：** 超空间引擎：损坏，跨恒星系航行不可用。
- **English:** Hyperspace engine: damaged. Interstellar travel is unavailable.

### `gui.starboundmc.ship_ai.status.hyperdrive.online` 【资源遗留：当前流程未触发】

- **中文：** 超空间引擎：在线，跨恒星系航行已恢复。
- **English:** Hyperspace engine: online. Interstellar travel is restored.

### `gui.starboundmc.ship_ai.status.incompatible`

- **中文：** 版本不兼容
- **English:** INCOMPATIBLE

### `gui.starboundmc.ship_ai.status.mineral_scan.complete` 【资源遗留：当前流程未触发】

- **中文：** 轨道矿物扫描：已完成，在深层地层发现少量钻石信号。
- **English:** Orbital mineral survey: complete; sparse diamond signatures detected in the deeper strata.

### `gui.starboundmc.ship_ai.status.mineral_scan.in_progress` 【资源遗留：当前流程未触发】

- **中文：** 轨道矿物扫描：正在分析地层结构。
- **English:** Orbital mineral survey: analyzing planetary strata.

### `gui.starboundmc.ship_ai.status.mineral_scan.waiting` 【资源遗留：当前流程未触发】

- **中文：** 轨道矿物扫描：等待地表资源数据。
- **English:** Orbital mineral survey: awaiting surface resource data.

### `gui.starboundmc.ship_ai.status.offline`

- **中文：** ● 核心离线
- **English:** ● CORE OFFLINE

### `gui.starboundmc.ship_ai.status.online`

- **中文：** ● 在线
- **English:** ● ONLINE

### `gui.starboundmc.ship_ai.status.rebooting`

- **中文：** ◐ 正在重启
- **English:** ◐ REBOOTING

### `gui.starboundmc.ship_ai.status.sublight.damaged` 【资源遗留：当前流程未触发】

- **中文：** 亚光速引擎：损坏，恒星系内航行不可用。
- **English:** Sublight engine: damaged. In-system travel is unavailable.

### `gui.starboundmc.ship_ai.status.sublight.igniting` 【资源遗留：当前流程未触发】

- **中文：** 亚光速引擎：点火序列进行中。
- **English:** Sublight engine: ignition sequence in progress.

### `gui.starboundmc.ship_ai.status.sublight.online` 【资源遗留：当前流程未触发】

- **中文：** 亚光速引擎：在线，恒星系内航行已恢复。
- **English:** Sublight engine: online. In-system travel is restored.

### `gui.starboundmc.ship_ai.status.syncing`

- **中文：** 同步中
- **English:** SYNCING

### `gui.starboundmc.ship_ai.subtitle`

- **中文：** 舰载人工智能
- **English:** SHIPBOARD ARTIFICIAL INTELLIGENCE

### `gui.starboundmc.ship_ai.title`

- **中文：** N.O.V.A.
- **English:** N.O.V.A.

