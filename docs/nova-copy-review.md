# N.O.V.A. 文案总表（审阅稿）

> 本稿从中英文语言资源中汇总 N.O.V.A. 相关广播、终端对话、任务引导和界面文案，保留资源键，方便逐条批注和修改。
> 修改本稿不会自动改动游戏资源；确认后的文案可据此回写 `zh_cn.json` / `en_us.json`。

共整理 **189 条文案**。中文与英文并列展示；带有“资源遗留”标签的条目目前没有接入当前流程，带有“交互原型”标签的条目属于终端交互原型。

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

- **中文：** 我回来了。核心供电已恢复，传送器可以用了。
- **English:** I'm back. Core power is restored, and the teleporter is ready.

### `message.starboundmc.nova.prologue.emergency`

- **中文：** 你醒了。我只能短暂维持通信，跟随定位标记来终端找我。
- **English:** You're awake. I can only keep this link open briefly. Follow the marker to my terminal.

### `message.starboundmc.nova.prologue.first_landing_complete` 【资源遗留：当前流程未触发】

- **中文：** 已确认你抵达星球表面，通信链路现已恢复。
- **English:** I have confirmed your arrival on the planet surface. The communications link is restored.

### `message.starboundmc.nova.prologue.locate_terminal`

- **中文：** 终端的位置已经标出来了。我在这里等你。
- **English:** I've marked the terminal's location. I'll be waiting here.

### `message.starboundmc.nova.prologue.mineral_scan_result` 【资源遗留：当前流程未触发】

- **中文：** 初步扫描已经完成。浅层地层中分布着煤、铜和铁；更深处还检测到青金石、红石，以及少量钻石。
- **English:** The preliminary scan is complete. Coal, copper, and iron are present in the upper strata; deeper layers also contain lapis lazuli, redstone, and sparse diamond deposits.

### `message.starboundmc.nova.prologue.mineral_scan_started` 【资源遗留：当前流程未触发】

- **中文：** 轨道扫描设备已启动。地层矿物扫描中................
- **English:** Orbital scanners are online. Mineral survey in progress................

### `message.starboundmc.nova.prologue.mineral_scan_sublight_hint`

- **中文：** 找到钻石信号了。它能替代损坏的聚焦阵列。先把物质枪采矿等级升到 1，再采集钻石打印点火核心。
- **English:** I've found diamond traces. Diamond can replace the damaged focusing array. Upgrade your Matter Manipulator to mining tier 1, then mine some and print an ignition core.

### `message.starboundmc.nova.prologue.sublight_engine_online`

- **中文：** 亚光速引擎恢复了。打开星图，我们可以在这个恒星系内航行了。
- **English:** The sublight engine is back. Open the Starmap; we can travel within this star system now.

### `message.starboundmc.nova.prologue.sublight_ignition_started`

- **中文：** 点火核心已接入。正在启动引擎。
- **English:** The ignition core is connected. Starting the engine.

### `message.starboundmc.nova.relay.approaching`

- **中文：** 正在接近中继站，那里全域真空。带上 Mk.II EPP 和备用氧气。
- **English:** We're approaching the relay station. It's a vacuum throughout. Bring a Mk.II EPP and spare oxygen.

### `message.starboundmc.nova.relay.arrived`

- **中文：** 到了。沿信标找到破损入口，去控制室取回数据核心。储藏室里有应急氧气。
- **English:** We're here. Follow the beacon to the breached entrance and retrieve the data core from the control room. There's emergency oxygen in storage.

### `message.starboundmc.nova.relay.completed`

- **中文：** 数据核心已带回飞船。中继站的位置也记下了，以后还能再来。
- **English:** The data core is aboard. I've recorded the relay station's location so we can visit again.

### `message.starboundmc.nova.relay.departed`

- **中文：** 我记住了中继站的位置，之后还能再来。
- **English:** I've recorded the relay station's location. We can come back later.

### `message.starboundmc.nova.relay.discovered`

- **中文：** 附近轨道有异常信号。我在星图上标出了一座废弃中继站。
- **English:** There's an unusual signal in a nearby orbit. I've marked a derelict relay station on the Starmap.

### `message.starboundmc.nova.relay.interrupted` 【资源遗留：当前流程未触发】

- **中文：** 中继站接近程序中断。我已暂停展开，暂时不要进入。
- **English:** The relay approach was interrupted. I've halted deployment. Keep clear of the station for now.

### `message.starboundmc.nova.relay.no_space`

- **中文：** 找不到接近中继站的空位。清理飞船周围的障碍后再试。
- **English:** I can't find room to approach the relay station. Clear the obstacles around the ship and try again.

### `message.starboundmc.nova.relay.recovered`

- **中文：** 数据核心到手了。所有人返回飞船，我们准备离开。
- **English:** We've got the data core. Everyone back to the ship; we're ready to leave.

### `message.starboundmc.nova.task.exploration.complete`

- **中文：** 新的地表记录收到了。回来时，记得领走你的物质枪模块。
- **English:** I've received the new surface records. Pick up your Matter Manipulator Modules when you're back.

### `message.starboundmc.nova.task.life_support.complete`

- **中文：** EPP 准备好了。出发前，记得在舱内补满氧气。
- **English:** Your EPP is ready. Top up its oxygen in the cabin before you leave.

### `message.starboundmc.nova.task.lunar_sortie.complete`

- **中文：** 欢迎回来。让 EPP 补满氧气，歇一会儿吧。
- **English:** Welcome back. Let your EPP refill, and take a moment to rest.

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

- **中文：** 先收集些木材。拿起物质枪，按住右键采集。
- **English:** Let's gather some wood first. Equip your Matter Manipulator and hold right-click to collect it.

### `message.starboundmc.nova.tutorial.voxel_intro`

- **中文：** 这是体素。打印站用它补全精密结构，星际交易也用它结算。
- **English:** Those are voxels. The printer uses them to build fine structures, and they're also used as interstellar currency.

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

- **中文：** 带上物质枪，用飞船传送器登陆。我们得找些修船的材料。
- **English:** Take your Matter Manipulator and use the ship teleporter to land. We need materials to repair the ship.

### `gui.starboundmc.ship_ai.prologue.first_contact.greeting`

- **中文：** 连接恢复了。你的生命体征正常。很高兴再见到你。
- **English:** We're connected again. Your vital signs are normal. It's good to see you.

### `gui.starboundmc.ship_ai.prologue.landing.briefing`

- **中文：** 先用传送器登陆，采集一块原木。我会启动轨道矿物扫描，找找能修船的材料。
- **English:** Take the teleporter down and collect a log. I'll start an orbital mineral survey to look for ship repair materials.

### `gui.starboundmc.ship_ai.prologue.late_join.summary`

- **中文：** 你来了。传送器还能用，地表应该能找到修船的材料。
- **English:** There you are. The teleporter still works. We should be able to find repair materials on the surface.

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

- **中文：** 我们停在了一颗宜居星球的轨道上。地表有可呼吸的大气，可以在那里寻找补给。
- **English:** We ended up in orbit around a habitable planet. There's breathable air on the surface, and we can look for supplies there.

### `gui.starboundmc.ship_ai.prologue.topic.flight_capability.option`

- **中文：** 飞船还能航行吗？
- **English:** Can the ship still travel?

### `gui.starboundmc.ship_ai.prologue.topic.flight_capability.response`

- **中文：** 推进系统离线了，但传送器还能用。先修好亚光速引擎，我们就能离开这条轨道。
- **English:** Propulsion is offline, but the teleporter still works. Once we repair the sublight engine, we can leave this orbit.

### `gui.starboundmc.ship_ai.prologue.topic.incident.option`

- **中文：** 发生了什么？
- **English:** What happened?

### `gui.starboundmc.ship_ai.prologue.topic.incident.response`

- **中文：** 一场宇宙风暴让我们偏离了航线。部分导航记录在事故中丢失，我还在重建事情的经过。
- **English:** A cosmic storm knocked us off course. Some navigation records were lost in the accident. I'm still piecing together what happened.

### `gui.starboundmc.ship_ai.prologue.topic.nova_identity.option`

- **中文：** 你是谁？
- **English:** Who are you?

### `gui.starboundmc.ship_ai.prologue.topic.nova_identity.response`

- **中文：** 我是 N.O.V.A.，你的舰载人工智能。我负责导航，也会帮你把这艘船重新开起来。
- **English:** I'm N.O.V.A., your shipboard AI. I handle navigation, and I'll help you get this ship moving again.

### `gui.starboundmc.ship_ai.prologue.topic.read_marker`

- **中文：** 已阅读
- **English:** READ

### `gui.starboundmc.ship_ai.repair.complete`

- **中文：** 亚光速引擎恢复了。打开星图，我们可以在这个恒星系内航行了。
- **English:** The sublight engine is back. Open the Starmap; we can travel within this star system now.

### `gui.starboundmc.ship_ai.repair.igniting`

- **中文：** 点火序列已经开始。很快就能启程了。
- **English:** Ignition has started. We'll be ready to leave shortly.

### `gui.starboundmc.ship_ai.repair.install_core`

- **中文：** 把点火核心放进飞船引擎的维修槽。我来启动引擎。
- **English:** Put the ignition core in the ship engine's repair slot. I'll start the engine.

### `gui.starboundmc.ship_ai.repair.need_upgrade`

- **中文：** 先制作升级工作台，把物质枪的采矿等级升到 1。我们需要它开采钻石。
- **English:** Print an upgrade workbench and raise your Matter Manipulator to mining tier 1. We'll need it to mine diamonds.

### `gui.starboundmc.ship_ai.repair.print_core`

- **中文：** 到体素打印站制作亚光速引擎点火核心，做好后带回飞船。
- **English:** Print a Sublight Ignition Core at the voxel printer, then bring it back to the ship.

## 任务引导与阶段提示

### `gui.starboundmc.guidance.claim.help` 【资源遗留：当前流程未触发】

- **中文：** 你的物质枪模块准备好了，到已完成任务里领取吧。
- **English:** Your Matter Manipulator Modules are ready. Pick them up under Completed Missions.

### `gui.starboundmc.guidance.claim.next`

- **中文：** 领取物质枪模块奖励。
- **English:** Claim the Matter Manipulator Module reward.

### `gui.starboundmc.guidance.complete.help` 【资源遗留：当前流程未触发】

- **中文：** 这项工作做完了。
- **English:** That job is done.

### `gui.starboundmc.guidance.complete.next`

- **中文：** 已完成。
- **English:** Complete.

### `gui.starboundmc.guidance.contact.help`

- **中文：** 在通讯页重启核心。我恢复连接后，会带你了解飞船的状况。
- **English:** Reboot the core on the Comms page. Once we're connected, I'll help you assess the ship.

### `gui.starboundmc.guidance.contact.next`

- **中文：** 在舰载终端重启 N.O.V.A.。
- **English:** Reboot N.O.V.A. at the shipboard terminal.

### `gui.starboundmc.guidance.exploration.help`

- **中文：** 选一颗与首次登陆不同类型的星球。用星图前往它的轨道，再乘传送器去看看地表。
- **English:** Choose a different planet type from your first landing. Navigate to its orbit with the Starmap, then take the teleporter down to explore.

### `gui.starboundmc.guidance.exploration.next`

- **中文：** 通过星图前往另一种类型的星球，再登陆地表。
- **English:** Use the Starmap to visit a different planet type, then land.

### `gui.starboundmc.guidance.life_support.help`

- **中文：** 把 EPP 穿在环境装备位，然后回舱内补满氧气。生命维持补给站也能补氧。
- **English:** Wear your EPP in the environment equipment slot, then fill it with oxygen in the cabin. Life support stations can refill it too.

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

- **中文：** 月面没有空气，记得给返航留出氧气。氧气罐要用两秒，别等到最后一口才打开。
- **English:** There's no air on the moon. Save enough oxygen to get home. Canisters take two seconds to use; don't wait until your last breath.

### `gui.starboundmc.guidance.lunar_sortie.next`

- **中文：** 装备有氧气的 EPP，前往岩石卫星探索，再安全返航。
- **English:** Take a charged EPP to Rocky Moon, explore, and return safely.

### `gui.starboundmc.guidance.lunar_sortie.return.help`

- **中文：** 该回船了。留出返航的氧气，我们在舱内补给。
- **English:** Time to head back. Keep enough oxygen for the trip; we'll refill your pack in the cabin.

### `gui.starboundmc.guidance.lunar_sortie.return.next`

- **中文：** 穿着有氧气的 EPP 返回飞船舱内。
- **English:** Return to the ship cabin wearing an EPP with oxygen remaining.

### `gui.starboundmc.guidance.repair.igniting.help` 【资源遗留：当前流程未触发】

- **中文：** 点火已经开始。很快就能在这个恒星系内航行了。
- **English:** Ignition has started. We'll be able to travel within this star system shortly.

### `gui.starboundmc.guidance.repair.igniting.next`

- **中文：** 引擎点火中。
- **English:** Engine ignition in progress.

### `gui.starboundmc.guidance.repair.install.help`

- **中文：** 带上采矿 1 级的物质枪和点火核心。到飞船引擎的维修槽安装，我来接手点火。
- **English:** Bring your mining tier 1 Matter Manipulator and ignition core to the ship engine. Put the core in its repair slot; I'll handle ignition.

### `gui.starboundmc.guidance.repair.install.next`

- **中文：** 带上采矿 1 级的物质枪，将点火核心装入飞船引擎维修槽。
- **English:** Carry a mining tier 1 Matter Manipulator and insert the ignition core into the ship engine's repair slot.

### `gui.starboundmc.guidance.repair.print.help`

- **中文：** 准备好钻石和体素，在打印站选择亚光速引擎点火核心。做好后带回飞船。
- **English:** Gather diamonds and voxels, then select the Sublight Ignition Core at the printer. Bring it back to the ship when it's ready.

### `gui.starboundmc.guidance.repair.print.next`

- **中文：** 在体素打印站制作亚光速点火核心。
- **English:** Print a Sublight Ignition Core at the voxel printer.

### `gui.starboundmc.guidance.repair.scanning.help`

- **中文：** 我还在分析地层。这会儿可以继续收集木材和石料。
- **English:** I'm still analyzing the strata. You can gather more wood and stone while I work.

### `gui.starboundmc.guidance.repair.scanning.next`

- **中文：** 矿物扫描中。
- **English:** Mineral survey in progress.

### `gui.starboundmc.guidance.repair.upgrade.help`

- **中文：** 钻石得用采矿 1 级的物质枪开采。先在体素打印站制作升级工作台，再去升级物质枪。
- **English:** You'll need a mining tier 1 Matter Manipulator for diamond. Print an upgrade workbench at the voxel printer, then upgrade your manipulator.

### `gui.starboundmc.guidance.repair.upgrade.next`

- **中文：** 在升级工作台把物质枪采矿等级提升至 1。
- **English:** Upgrade your Matter Manipulator to mining tier 1 at the workbench.

### `gui.starboundmc.guidance.repair.wood.help`

- **中文：** 先采集一块原木。我会启动轨道矿物扫描，找找附近的矿藏。
- **English:** Collect a log first. I'll start an orbital mineral survey to locate nearby deposits.

### `gui.starboundmc.guidance.repair.wood.next`

- **中文：** 用物质枪采集一块原木，启动矿物扫描。
- **English:** Use the Matter Manipulator to collect a log and start the mineral survey.

### `gui.starboundmc.guidance.surface.help`

- **中文：** 在飞船传送器选择星球表面。带上物质枪，我们得找些修船的材料。
- **English:** Select the planet surface at the ship teleporter. Bring your Matter Manipulator; we need repair materials.

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

- **中文：** EPP 穿戴在独立的环境装备位，每台设备都有自己的氧气储量。在可呼吸环境中不耗氧；船舱和生命维持补给站可以补氧。补给站还能灌装氧气罐。
- **English:** Wear an EPP in the separate environment equipment slot. Each pack holds its own oxygen reserve. Breathable air stops oxygen use; the ship cabin and life support stations refill the pack. Stations also fill canisters.

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

- **中文：** 点火核心需要 3 颗钻石和 50 体素。带上采矿等级 1 的物质枪，把核心装入飞船引擎的维修槽。一枚核心就能恢复整艘船的亚光速航行。
- **English:** An ignition core costs 3 diamonds and 50 voxels. Carry a mining tier 1 Matter Manipulator and insert the core into the ship engine's repair slot. One core restores sublight travel for the whole ship.

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

- **中文：** 我该怎么做？
- **English:** What should I do?

### `gui.starboundmc.tasks.chapter_done`

- **中文：** 当前任务已全部完成。新的旅程由你决定。
- **English:** All current missions are complete. Choose your next destination.

### `gui.starboundmc.tasks.claim`

- **中文：** 领取奖励
- **English:** Claim reward

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

### `gui.starboundmc.command.close`

- **中文：** ×
- **English:** ×

### `gui.starboundmc.command.comms`

- **中文：** 通讯
- **English:** Comms

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

