# 硬件参考资料

本目录存放与机场车辆 GPS 终端相关的**原理图、模组规格与接线说明**，供选型、焊接与排障时查阅。固件工程与烧录说明见上级目录 [`../README.md`](../README.md)。

## 文件一览

| 文件 | 说明 |
|------|------|
| `YD-ESP32-S3-SCH-V1.4.pdf` | 开发板原理图（YD ESP32-S3，V1.4） |
| `ESP32-S3-WROOM-1-N16R8.pdf` | 模组数据手册（16MB Flash + 8MB PSRAM） |
| `ESP32-S3-Metric.pdf` | ESP32-S3 模组机械尺寸 |
| `PACKAGE.PDF` | 封装尺寸参考 |
| `定位模块引脚截图.png` | GPS 定位模块引脚定义（与固件 UART 接线对照） |

## 与本项目固件的对应关系

- **主控**：ESP32-S3（`idf.py set-target esp32s3`），默认使用板载 **WiFi 2.4GHz** 连接华为云 MQTT。
- **定位模块**：GPS **E108-GN03D**（NMEA），UART 默认 **GPIO10（RX）/ GPIO11（TX）**、波特率 **9600**，可在 `idf.py menuconfig` → **机场车辆系统** 中修改。
- **接线表**（摘要）见 [`../README.md`](../README.md#硬件接线gps）；引脚以 `定位模块引脚截图.png` 与原理图为准。

## 使用说明

- 资料为 **PDF/PNG**，请用系统阅读器或浏览器打开；大文件已纳入 Git，克隆仓库后即可本地查看。
- 若更换开发板或 GPS 模块型号，请核对 `menuconfig` 中的 UART 引脚与波特率。
