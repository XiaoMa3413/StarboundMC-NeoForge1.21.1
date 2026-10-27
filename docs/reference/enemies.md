# 机械敌人

首批敌人仅提供刷怪蛋入口，位于 StarboundMC 创造物品栏。未接入群系刷新、生成结构或生存获取链。世界加载不会自动投放这些敌人；刷怪蛋生成的个体持久保存，和平难度仍遵循 Monster 的移除规则。

| 实体 ID（starboundmc:） | 名称 | 生命 / 护甲 | 攻击 |
|---|---|---|---|
| scrap_skitter | 废铁掠行蛛 | 40 / 2 | 抬前足预警12tick后短扑，伤害4 |
| coil_sentry | 线圈巡逻机 | 72 / 4 | 悬浮并保持距离，预警20tick后发射速度0.55格/tick、伤害5的可躲避弹体 |
| breach_guard | 重装破拆卫 | 180 / 14 | 钻击预警24tick，基础距离2.8格，伤害8；举盾时正面伤害乘0.35 |
| quarry_warden | 矿站监工 MK-I | 480 / 12 | 横扫伤害8、基础半径3.8格；砸地伤害10、半径4.5格；黄色Boss血条 |

刷怪蛋 ID 为对应实体 ID 加 `_spawn_egg`。原版实体命令可用于管理员测试，不构成生存获取方式。

## 战斗 authority

`MachineEnemy` 的服务端状态机控制待机、预警、攻击和恢复。客户端仅根据同步阶段、动作和序列播放骨骼动画。预警开始锁定朝向/瞄准点，提供躲避时间；一次攻击只结算一次命中。寻路和追击独立于模型动画。

近战和范围攻击检查视线；远程弹体通过原版碰撞处理实体和墙壁。机械同类、盟友、创造玩家和旁观者不受其攻击。范围效果不会破坏方块或点燃地形。破拆卫侧后方和恢复阶段没有正面盾减伤；监工恢复之外受到实体来源伤害乘0.55，半血后缩短预警与间隔，仍保留恢复窗口。

当前实体掉落表为空，击杀经验分别为8、16、45、160；本轮不把新敌人材料接入既有制造进度。

## 原生模型与导出

制作源、概念和导出边界见 [敌人制作说明](../../dev-assets/enemies/README.md)。每个模型包含独立机械骨骼与真正的立体网格；生产骨骼局部三角面由保存、重开后的 Blockbench 原生工程提取。客户端渲染不会读取 bbmodel 或开发截图。

运行 `python scripts/enemy_models.py [实体ID…]` 会在运行中的 Blockbench 重建指定工程、重开并拍摄三视图；它会覆盖制作源，先把旧版本归档到概念图目录的 iterations。日常导出使用 `python scripts/export_enemy_models.py`，原生工程与缓存哈希不一致时拒绝导出。几何检查为 `python scripts/validate_enemy_models.py`。

## 验证入口

- `scripts/dev.ps1 runGameTestServer -PenemyGameTests --offline --console=plain`：真实注册表、刷怪蛋、属性、攻击/躲避/视线/盾防、死亡和存档身份。
- `scripts/dev.ps1 runClient -PenemySmoke --offline --console=plain`：真实客户端四种刷怪蛋、移动、预警、两种Boss动作、资源重载，以及四张世界视图。预警展示会暂停AI以检查姿态，不能替代连续人工战斗体验。
- `scripts/dev.ps1 test build --offline --console=plain`：默认生产构建不包含上述可选测试源、测试结构和开发模型。

多人、长时间性能、实际击杀时间与最终美术评价属于人工验收，见 [当前工作](../current-work.md)。
