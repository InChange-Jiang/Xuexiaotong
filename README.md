# 学小通（Xuexiaotong）

学习通课程 / 作业管理 + 课件笔记相机的 Android 应用，全液态玻璃 UI。

> 本项目与学习通相互独立，非学习通官方产品。

## 功能特性

- **液态玻璃界面**：基于 Backdrop 组件二次开发的全套液态玻璃 UI（实时模糊 / 透镜折射 / 高光阴影），Dock 滑块支持拖动跟手、按压折射与速度形变
- **课程与作业管理**：模拟学习通 Web 登录获取会话凭证，调用官方接口抓取课程、作业与任务点进度，日历视图集中管理
- **精确提醒**：基于 Android 原生 AlarmManager，应用被清理后台后仍准时推送日程 / 作业提醒
- **课件笔记相机**：CameraX 驱动，支持多后置镜头等效焦距分类轮换（UW / WIDE / TELE）、点击对焦测光、捏合与拉杆变焦
- **相册管理**：照片按科目分类归档（本地私有目录存储）、批量移动科目、一键导出到系统相册、多选操作

## 数据与隐私

- 所有数据仅存储于设备本地，不经过任何第三方服务器；登录凭证加密存储，可一键退出清除
- 相机权限仅在你主动进入拍照功能时使用；照片导出仅在你主动触发时通过系统媒体库写入
- 完整说明见软件内《隐私声明》

## 技术栈

| 领域 | 选型 |
|---|---|
| 语言 / UI | Kotlin + Jetpack Compose（单 Activity 架构） |
| 液态玻璃渲染 | [Backdrop](https://github.com/kyant0/backdrop) + [ComposeShapes](https://github.com/kyant0/ComposeShapes)，交互移植自 [AndroidLiquidGlass](https://github.com/burulangtu/AndroidLiquidGlass) |
| 相机 | CameraX（多镜头识别 / 对焦测光 / 变焦），方案参考 [CamerAwesome](https://github.com/Apparence-io/camerawesome) 与 [OpenCamera](https://github.com/almalence/OpenCamera) |
| 数据 | 本地 JSON 存储（kotlinx.serialization） |
| 提醒 | AlarmManager + 通知（支持开机恢复） |

## 构建

环境要求：JDK 17+、Android SDK（compileSdk 37）。

```bash
./gradlew assembleDebug   # 调试包
./gradlew assembleRelease # 发布包
```

APK 输出至 `app/build/outputs/apk/{debug,release}/`。

## 开源许可

本项目基于 Apache License 2.0 开源，详见 [LICENSE](LICENSE)。

使用到的开源项目及其许可证：

- [AndroidLiquidGlass](https://github.com/burulangtu/AndroidLiquidGlass) — Apache-2.0（Dock 滑块拖拽阻尼动画与液态交互移植，见 `LiquidDockAnim.kt` 头部声明）
- [Backdrop](https://github.com/kyant0/backdrop) — Apache-2.0
- [ComposeShapes](https://github.com/kyant0/ComposeShapes) — Apache-2.0
- [CamerAwesome](https://github.com/Apparence-io/camerawesome) — MIT（多镜头识别方案参考）
- [OpenCamera](https://github.com/almalence/OpenCamera) — GPL-3.0（点击对焦测光方案参考，未使用其代码）
- [PictureSelector / Luban](https://github.com/LuckSiege/PictureSelector) — Apache-2.0（缩略图采样思想参考）

## 致谢

感谢所有为本项目提供灵感与参考的开源项目，以及每一位使用者。特别致谢见 [THANKS.md](THANKS.md)。
