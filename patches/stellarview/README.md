# StellarView HDR 定制源码补丁

当前生产星场已提取为主项目内置核心，无需安装、构建或下载 StellarView。来源基线、生产文件映射、许可和修改边界见 [third_party/stellarview](../../third_party/stellarview/README.md)。本目录保留定制 fork 的历史源码差异及完整 MIT 许可，用于来源核验和明确的历史重建；不代表当前外部后端或构建参数。

源仓库为 XiaoMa3413/StellarView，基线是 `feat/external-view-center` 的 `743e6b094ae35d766b9ae88347f7f96ff507e73f`。原作者 Povstalec，MIT 许可见本目录 LICENSE.txt。改动保存在独立工作树 `stellarview-external-view-center` 的 `feat/starbound-hdr-stars` 分支（提交 `88e5040`、`59fc6d8`、`f55e85f`），并以源码补丁随主项目记录。

`starbound-hdr.patch` 从上述基线生成，版本为 `0.5.4-alpha-starbound-hdr2-NeoForge`。原有 API 保留；新增 `ExternalStarField.renderLinear(...)`：

- 把星色解码为线性 HDR，通过 radianceScale 与可见度分别控制能量。
- 四种星 shader（纹理/无纹理、普通/实例化）具有光滑星点轮廓、最小像素半径和面积能量补偿。
- 在 shader.apply 的 BlendMode 之后设置 HDR 加法混合，避免继承普通天空混合状态。
- 使用调用方 viewport；退出时恢复修改的 uniform。普通维度仍采用原版输出。
- shader 不支持新增 uniform 时返回 false，让 StarboundMC 使用原生星场。
- 独立管理外部星场上传额度，退出时恢复普通天空计数，避免反复切换画质后无法上传新星场。
- HDR2 保留作者定义的暗星亮度/尺寸层次，避免旧最小尺寸与距离亮度下限把暗星变成相似的粒子；HDR 光学轮廓不再乘像素星贴图。主项目单独提供六组星型，不修改普通天空的默认星型。

## 历史 fork 重建

在匹配 `743e6b094ae35d766b9ae88347f7f96ff507e73f` 基线的独立 StellarView checkout 中应用 `starbound-hdr.patch`，使用 Java 21 构建。此操作仅用于复核历史外部版本，不是本项目当前开发步骤。
