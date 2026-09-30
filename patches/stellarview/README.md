# StarboundMC 的 StellarView HDR 定制

源仓库为 XiaoMa3413/StellarView，基线是 `feat/external-view-center` 的 `743e6b094ae35d766b9ae88347f7f96ff507e73f`。原作者 Povstalec，MIT 许可见本目录 LICENSE.txt。改动保存在独立工作树 `stellarview-external-view-center` 的 `feat/starbound-hdr-stars` 分支（提交 `88e5040`、`59fc6d8`），并以源码补丁随主项目记录。

`starbound-hdr.patch` 从上述基线生成，版本为 `0.5.4-alpha-starbound-hdr1-NeoForge`。原有 API 保留；新增 `ExternalStarField.renderLinear(...)`：

- 把星色解码为线性 HDR，通过 radianceScale 与可见度分别控制能量。
- 四种星 shader（纹理/无纹理、普通/实例化）具有光滑星点轮廓、最小像素半径和面积能量补偿。
- 在 shader.apply 的 BlendMode 之后设置 HDR 加法混合，避免继承普通天空混合状态。
- 使用调用方 viewport；退出时恢复修改的 uniform。普通维度仍采用原版输出。
- shader 不支持新增 uniform 时返回 false，让 StarboundMC 使用原生星场。
- 独立管理外部星场上传额度，退出时恢复普通天空计数，避免反复切换画质后无法上传新星场。

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

主项目 gradle.properties 指向新版本，不覆盖旧 externalview2 Maven artifact。新克隆需要先构建此依赖；尚未上传远程 Maven。发布自定义 JAR 时保留原许可/作者说明。

主项目通过一次性能力查找兼容旧 externalview2 API；旧版采用 LDR 解码导入。可用 `-Pstellarview_version=0.5.4-alpha-externalview2-NeoForge` 验证旧依赖；模组缺失时使用原生星场。完整太空视觉回退为 `DIRECT + LEGACY`，升级前主项目检查点为 `8eab25d`。

当前修改没有移植其黑洞透镜算法。未来卡冈图雅效果需要按主项目的曲线光线/HDR 路线单独实现。
