# 开源致谢

本项目在「笔记相册缩略图」性能优化中，借鉴了开源项目 **PictureSelector**（含 **Luban** 图片压缩引擎）的核心设计思想：

- 项目地址：https://github.com/LuckSiege/PictureSelector
- 借鉴内容：
  1. **采样解码**：先按图片宽高比例计算 `inSampleSize`（2 的幂）降采样后再解码，避免全尺寸解码造成的内存与 CPU 开销；
  2. **内存缓存**：以 LruCache 缓存已解码的缩略图，滑动复用、避免反复解码。
- 实现方式：本项目未直接引用其源码或二进制文件，仅借鉴上述通用算法思想，自研实现为 `ui/notes/ThumbnailStore.kt`。
- 开源协议：PictureSelector 使用 Apache-2.0 协议（https://github.com/LuckSiege/PictureSelector/blob/master/LICENSE ），其 Luban 压缩引擎源自 Luban 开源项目（https://github.com/Curzibn/Luban ）。
