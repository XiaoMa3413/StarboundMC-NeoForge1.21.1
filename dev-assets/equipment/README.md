# 装备美术源文件

本目录对应用户已确认的 2026-10-26 V01 概念，审批图的编号、文件名及 SHA-256 见 `approved-v01.json`。原图保留在工作区 `概念图/2026-10-26_武器与防具_V01`。

- 六份武器 `.bbmodel` 可在 Blockbench 的 Java Block/Item 模式打开，包含真实方块坐标、单轴旋转与嵌入材质。
- 四套防具各有 `_outer.bbmodel` / `_inner.bbmodel`，分别对应头胸靴和护腿层；按原版骨架的头、身体、手臂和腿分组，已转换为 Blockbench 的向上坐标。
- 所有模型均使用嵌入 PNG，打开时不依赖开发机的绝对纹理路径。
- 正式可重复导出源是 `scripts/equipment_art.py`，四套防具的独立轮廓定义在 `scripts/equipment_armor_geometry.py`。修改方块尺寸、分件、像素轮廓与调色板后运行下列命令；`.bbmodel` 是相同源数据生成的可编辑交付稿，直接编辑后若需要保留，须将修改同步回源定义再重新导出。

```powershell
python scripts/generate_weapon_resources.py --stage 3
python scripts/generate_armor_resources.py
```

GUI 图标固定为 32×32、有真实透明背景；六把武器的手持/世界模型保存在 `models/item/*_hand.json`。武器使用原生 SeparateTransformsModel + ItemLayerModel 切换 GUI 与立体模型；防具使用原生 HumanoidModel 骨架与 256×256 像素材质分区，不添加独立动画运行库。

防具图中的装甲形状通过实际 cuboid 表达；显示尺寸按 Minecraft 玩家比例重新校准，概念板上的 BLOCKS 不是游戏世界距离。EPP 仍是独立槽位与独立背部显示层。

原创非代码资源适用仓库 `LICENSE-ASSETS.md`；代码适用项目代码许可。没有引用新的第三方素材。
