# 第三方素材与依赖说明

本文件记录 StarboundMC 发布包中不由项目许可证重新授权的第三方内容。

## Solar System Scope 行星贴图

以下文件基于 [Solar System Scope Textures](https://www.solarsystemscope.com/textures/)
提供的素材制作，并按 [Creative Commons Attribution 4.0 International（CC BY 4.0）](https://creativecommons.org/licenses/by/4.0/)
使用：

- `src/main/resources/assets/starboundmc/textures/planet/lush.png`：Earth day map 与 clouds 合成
- `src/main/resources/assets/starboundmc/textures/planet/molten.png`：Venus surface
- `src/main/resources/assets/starboundmc/textures/planet/frozen.png`：Eris fictional
- `src/main/resources/assets/starboundmc/textures/planet/barren.png`：Mars
- `src/main/resources/assets/starboundmc/textures/planet/gasgiant.png`：Jupiter，仅格式转换（JPEG→RGBA PNG）
- `src/main/resources/assets/starboundmc/textures/planet/rockymoon.png`：Moon，缩采样至 4096×2048 并转为 RGBA PNG
- `src/main/resources/assets/starboundmc/textures/planet/gasgiant_ring.png`：Saturn ring alpha，原分辨率 8192×500 直接使用

这些文件是项目内的修改或转码版本（表面图 4096×2048，环带条图 8192×500）。星图界面 `textures/gui/starmap/bodies/` 下的天体精灵为上述贴图的球面投影缩略图，同样基于这些素材。再分发或修改时，请保留 Solar System Scope 的署名、
来源链接、CC BY 4.0 许可证链接，并说明所做修改。

## Minecraft、NeoForge 与 LDLib2

Minecraft、NeoForge、LDLib2 和其他构建/运行时依赖遵循其各自的许可证和分发条款。它们不是
StarboundMC MIT License 的授权对象；使用本项目时请单独满足这些依赖的安装和许可要求。

跃迁和传送使用 Minecraft 原版 `SoundEvents`，本项目不重新分发对应的 Minecraft 音频文件。

## 新增素材规则

任何新的第三方代码、纹理、字体、音频、模型或其他资源，在合并前都必须确认许可证允许当前
用途，并在本文件补充作者、来源、许可证、文件路径和修改说明。无法确认权利状态的素材不得
进入仓库或发布包。
