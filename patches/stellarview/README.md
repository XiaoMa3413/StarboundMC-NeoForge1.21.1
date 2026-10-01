# StarboundMC 的 StellarView HDR 定制

源仓库为 XiaoMa3413/StellarView，基线是 `feat/external-view-center` 的 `743e6b094ae35d766b9ae88347f7f96ff507e73f`。原作者 Povstalec，MIT 许可见本目录 LICENSE.txt。改动保存在独立工作树 `stellarview-external-view-center` 的 `feat/starbound-hdr-stars` 分支（提交 `88e5040`、`59fc6d8`、`f55e85f`），并以源码补丁随主项目记录。

`starbound-hdr.patch` 从上述基线生成，版本为 `0.5.4-alpha-starbound-hdr2-NeoForge`。原有 API 保留；新增 `ExternalStarField.renderLinear(...)`：

- 把星色解码为线性 HDR，通过 radianceScale 与可见度分别控制能量。
- 四种星 shader（纹理/无纹理、普通/实例化）具有光滑星点轮廓、最小像素半径和面积能量补偿。
- 在 shader.apply 的 BlendMode 之后设置 HDR 加法混合，避免继承普通天空混合状态。
- 使用调用方 viewport；退出时恢复修改的 uniform。普通维度仍采用原版输出。
- shader 不支持新增 uniform 时返回 false，让 StarboundMC 使用原生星场。
- 独立管理外部星场上传额度，退出时恢复普通天空计数，避免反复切换画质后无法上传新星场。
- HDR2 保留作者定义的暗星亮度/尺寸层次，避免旧最小尺寸与距离亮度下限把暗星变成相似的粒子；HDR 光学轮廓不再乘像素星贴图。主项目单独提供六组星型，不修改普通天空的默认星型。

## 重建

在匹配基线的 StellarView checkout 中应用并构建（Java 21）：

```powershell
git apply --check /absolute/path/to/starbound-hdr.patch
git apply /absolute/path/to/starbound-hdr.patch
.\gradlew.bat build publishToMavenLocal
```

本机使用共享缓存的已执行命令为：

```powershell
.\gradlew.bat --gradle-user-home ../.gradle-home --offline '-Dorg.gradle.java.installations.paths=E:/Develop/Program Files/Android Studio/jbr' build publishToMavenLocal
```

主项目默认使用原生星场，普通新克隆的构建与运行不需要此依赖。`gradle.properties` 保留可选版本号，不覆盖旧 externalview2 / HDR1 Maven artifact；开发验证加 `-PwithStellarView` 才加入 runtime。该定制版本尚未上传远程 Maven，因此只有请求此可选运行方式时才需要先构建并安装到本地 Maven。客户端同时设 `spaceStarfieldBackend = "STELLAR_VIEW"`（夹具用 `-PspaceSmokeStellarView=true`）才能启用。发布自定义 JAR 时保留原许可/作者说明。

主项目不导入 StellarView 类型，通过缓存的公开 API 签名和坐标接口代理连接；不依赖其私有字段或方法。旧 externalview2 API 采用 LDR 解码导入。PowerShell 可用 `-PwithStellarView '-Pstellarview_version=0.5.4-alpha-externalview2-NeoForge'` 验证旧依赖；模组缺失或接口不兼容时使用原生星场。完整太空视觉回退为 `DIRECT + LEGACY`，升级前主项目检查点为 `8eab25d`，电影感修正前检查点为 `5116f07`，原生后端默认迁移前检查点为 `9034d46`。

当前修改没有移植其黑洞透镜算法。未来卡冈图雅效果需要按主项目的曲线光线/HDR 路线单独实现。
