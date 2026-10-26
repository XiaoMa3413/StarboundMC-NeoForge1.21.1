# 已确认装备美术

本轮依据用户确认的 V01 概念落地六把武器与四套防具。概念身份及校验值见 `dev-assets/equipment/approved-v01.json`，制作源和可编辑模型说明见 [装备源文件](../../dev-assets/equipment/README.md)。

## 武器

| 编号 | 装备 | 主要外观 |
| --- | --- | --- |
| W01 | 钨钢军刀 | 钢灰单刃、紧凑护手、琥珀识别点 |
| W02 | 线圈手枪 | 双铜线圈、笼式方枪口、青色电量指示 |
| W03 | 耐钢战戟 | 长分段杆、宽刃和短后钩、青绿导槽 |
| W04 | 等离子卡宾枪 | 后置能量匣、枪托、短护木与青绿窗口 |
| W05 | 星核裁决刃 | 宽重刃、蓝紫导槽、根部菱形星核 |
| W06 | 星核轨道炮 | 开放双导轨、加固后托、蓝紫星核仓 |

每把武器有独立 32×32 透明 GUI 像素图，第一/第三人称、掉落与展示使用实际方块模型；两侧的指示灯、能量窗口与导轨细节均保留。原生分视角模型加载器负责切换，不以 GUI 缩略立体模型替代专用图标。基础小刀维持原有资源。

## 防具

| 编号 | 套装 | 主要外观 |
| --- | --- | --- |
| A01 | 钨钢拓荒 | 矿灯头盔、琥珀标记、工装口袋、护膝与工作靴 |
| A02 | 耐钢先锋 | T 形面罩、分层厚肩甲/胸甲、加固脊板与重护胫 |
| A03 | 钛合金游骑 | 横向传感面罩、低矮侧传感器、轻型分段护甲 |
| A04 | 星核守望 | 蓝紫板甲、菱形胸核、背部导能、对称髋甲 |

16 个部件均有 32×32 透明像素图。穿戴层是按原版骨架分件的实际 Humanoid 模型，头胸靴使用外层、护腿使用内层；原版负责对应部位显示与动作姿态复制。内衬包覆玩家皮肤的外套/裤腿层，避免关节透出原有皮肤颜色。EPP 保持独立背部装备显示。

材质分区为 256×256，原生层定义与 UV 一致；资源重载后清空模型缓存并重新烘焙。资源或几何定义损坏会明确报错，不静默套用旧式盔甲模型。

## 生成与验证

```powershell
python scripts/generate_weapon_resources.py --stage 3
python scripts/generate_armor_resources.py
& .\scripts\dev.ps1 -PweaponSmoke runClient --offline --console=plain
& .\scripts\dev.ps1 -ParmorSmoke runClient --offline --console=plain
& .\scripts\dev.ps1 -PweaponEffectsSmoke runClient --offline --console=plain
```

武器客户端夹具检查实际 GUI 精灵与非空渲染面、左右手持握、完整模型展示及资源重载，输出 `run-weapon-smoke/screenshots/weapons-approved-v01-final`。防具夹具输出 `run-armor-smoke/screenshots/armor-approved-v01-final`，包含各套正侧背、16 部件中文说明及同步属性。战斗夹具输出 `run-weapon-effects-smoke/screenshots/approved-art-effects`，检查模型动作与实际攻击路径。

美术使用原有物品 ID；武器/防具数值、制造、命中、套装判定与协议仍遵循[武器](weapons.md)、[防具](armor.md)和当前架构。每轮执行结果与修复回溯见根目录 process，尚待人工长期游玩的事项见 current-work。
