# 游戏骨骼尺寸适配工程

`starwarden_helmet.bbmodel` 是根据用户对头盔比例的纠正，在真实Blockbench中修改的可编辑头盔工程。权威造型基础仍是已验收V04；此工程将盔壳、冠部、太阳穴/下颌/后脑空间重塑到最终穿戴尺寸，而非在运行时整体缩放。

工程编辑坐标转换：游戏头部骨骼顶点 `[x,y,z]` 对应Blockbench `[-x,24-y,z]`。原生保存后提取实际三角面到同名JSON，生产导出直接使用该骨骼坐标。工程哈希不一致时明确拒绝导出。

- 制作与原生重开截图：`python scripts/fit_starwarden_helmet.py`（须运行Blockbench localhost:9228）。此脚本会重建本适配工程，手工改工程后不要直接运行覆盖。
- 手工编辑原生工程后的网格重新提取：`python scripts/fit_starwarden_helmet.py --extract-only`，随后运行生产导出；这个选项保留手工修改。
- 生产导出：`python scripts/equipment_mesh_export.py`。
- 骨骼外层包覆检查：`python scripts/validate_helmet_fit.py`。
- 原生轮廓节点与外廓尺寸：`starwarden_helmet_receipt.json`。
