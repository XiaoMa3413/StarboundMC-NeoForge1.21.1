> 历史V03源文件。当前生产造型来自 `../concept-rebuild-v04/`，游戏导出见 `../../scripts/equipment_mesh_export.py`。本目录保留用于回溯。

# 装备美术源文件

本目录对应用户已确认的 2026-10-26 V01 概念，审批图的编号、文件名及 SHA-256 见 `approved-v01.json`。原图保留在工作区 `概念图/2026-10-26_武器与防具_V01`。

- 六份武器 `.bbmodel` 可在 Blockbench 的 Java Block/Item 模式打开，包含真实方块坐标、单轴旋转与嵌入材质。
- 四套防具各有 `_outer.bbmodel` / `_inner.bbmodel`，分别对应头胸靴和护腿层；按原版骨架的头、身体、手臂和腿分组，已转换为 Blockbench 的向上坐标。
- 所有模型均使用嵌入 PNG，打开时不依赖开发机的绝对纹理路径。
- **本目录的14份 `.bbmodel` 是正式模型源。** 本轮已在Blockbench 5.2.2内编辑、绘制材质、原生保存并重新载入检查。直接在Blockbench修改、保存，然后运行下面的导出命令即可；不再需要把坐标抄回Python脚本，导出也不会重写工程。

```powershell
python scripts/generate_weapon_resources.py --stage 3
python scripts/generate_armor_resources.py
```

GUI 图标固定为 32×32、有真实透明背景；六把武器的手持/世界模型保存在 `models/item/*_hand.json`。武器使用原生 SeparateTransformsModel + ItemLayerModel 切换 GUI 与立体模型；防具使用原生 HumanoidModel 骨架与 256×256 像素材质分区，不添加独立动画运行库。

武器项目设置使用Blockbench的 **Minecraft 1.9–1.21.5**（内部值`1.9.0`），适配本模组1.21.1。保留原有display槽位；方块坐标在-16..32内，单轴旋转采用±22.5°/±45°，骨架组不旋转。每把武器使用自己的嵌入PNG，导出为`textures/item/*_model.png`。

防具保留七个原版骨架组及其枢轴，Cube直接放在对应骨架下；使用Box UV和256×256贴图。部件名称末尾`[m0]`–`[m7]`指定原生材质分区，保留该后缀；UV偏移为`[(m%4)*64, floor(m/4)*64]`。当前支持Cube的Z轴旋转。导出器发现不支持的骨架、坐标或旋转会明确报错，须在工具内修正后再导出。

`scripts/equipment_models.py`只转换保存的工程到游戏资源；`equipment_art.py`维护32×32物品栏像素图。`scripts/model_equipment.py`与`blockbench_equipment.js`记录本轮在真实应用内执行的建模操作，不属于正常构建步骤。它们使用仅本机的调试端口9228；Python端需要已有的`websocket-client`，游戏本身无此依赖。重复执行建模须显式选择原始`--baseline`，避免累积修改。

工具内56张分层四视图、16张整套四视图、四份整套检查工程及游戏内验证在工作区 `概念图/2026-10-26_Blockbench建模_V03`。整套检查工程只供查看内外层关系，生产修改应保存在本目录的对应分层源。

防具图中的装甲形状通过实际 cuboid 表达；显示尺寸按 Minecraft 玩家比例重新校准，概念板上的 BLOCKS 不是游戏世界距离。EPP 仍是独立槽位与独立背部显示层。

原创非代码资源适用仓库 `LICENSE-ASSETS.md`；代码适用项目代码许可。没有引用新的第三方素材。
