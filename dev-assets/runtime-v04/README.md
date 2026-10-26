# 游戏网格缓存

由 `scripts/extract_v04_meshes.py` 在 Blockbench 5.2.2 中提取已验收工程的实际渲染三角面。每个顶点为 `[x,y,z,u,v]`，UV以PNG左上为原点；每份缓存记录源工程SHA256。不要手工改缓存代替修改模型。

生产输出：`python scripts/equipment_mesh_export.py`。该脚本校验源文件哈希，转换握持坐标和防具骨骼，保留源工程与验收截图。武器普通资源生成和防具资源生成均调用此导出器。防具的概念像素骨架标定与游戏转换也在该脚本内。
