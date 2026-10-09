# 宇宙与导航

本文记录当前规则；authority 与错误策略见[架构](../architecture.md)，未完成事项见[当前工作](../current-work.md)。具体字段、资源和数值以当前 codec、datapack 与测试为准。

## 身份、位置与目录

- 恒星系与天体拥有数据驱动身份。天体的归属、导航、视觉和可登陆地表是不同信息；不从 ID 前缀推断归属，也不把“能在太空显示”等同于“有地表维度”。
- UniverseCatalog 索引当前加载定义并校验身份/资源，UniverseNavigation 提供航行几何。太空绘制候选必须同时有导航和视觉定义。
- UniversePosition（sector + local double）与 UniverseDelta 是正式位置/位移类型。先求相对位置，再在 Minecraft/GPU 边界派生局部 Vec3；巨大绝对坐标不直接转成 GPU float。
- 飞船唯一持久状态包含当前 entry、pose、速度与 flight phase。未知 datapack 身份和原 pose 保留，缺少几何时拒绝航行；损坏存档明确拒绝。客户端快照与帧内曲线只作投影，不反写 authority。
- 注册 ID 和内容身份保持稳定；内部历史 schema 不构成兼容义务，不按旧迁移计划补 adapter。

## 航行与两套星图入口

当前注册了两个真实方块/物品/菜单入口，均未退役：

| 设备 | 当前客户端职责 |
| --- | --- |
| `ship_console` / ShipConsoleScreen | 飞船控制台的手绘星图交互；保留星域/星系/轨道层级、选择与航行展示 |
| `starmap_terminal` / StarmapTerminalScreen、StarmapTerminalRoot | 独立星图终端的 LDLib2 导航界面，呈现星图层级和当前 Relay POI |

两个菜单均提供 WarpControlMenu 边界，通过同一 start-warp 请求进入 ShipWarpManager。服务端重验菜单有效性、目标、船况、燃料和船员安全，统一修改 ShipStateData；两套画面没有两套航行 authority。

代码证明两者仍可达，但不能证明产品意图是永久保留双 UI。本轮没有实际游玩确认旧控制台已被替代，因此保留；后续统一须先确认设备用途、存量入口和玩家体验。

传送由服务端目的地 registry 与当前世界状态决定，客户端选择不是落点 authority。Rocky Moon 的地表任务钩子遗漏见[已知问题](../known-issues.md)，不改变其宇宙身份。
