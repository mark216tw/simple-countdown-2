# 版本紀錄

本文件依照 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/) 格式維護，版本號遵循 [Semantic Versioning](https://semver.org/lang/zh-TW/)。

## [Unreleased]

## [1.3.0] - 2026-08-28

### Changed

- 首頁上方的進行中倒數資訊改用該倒數的識別色顯示。
- 倒數識別色色盤由 12 色增加至 24 色。
- 測試發行 APK 改用啟用 R8 與資源壓縮、Debug 金鑰簽署的 `prerelease` Build Type。

## [1.2.0] - 2026-08-23

### Changed

- App 顯示名稱由「簡單倒數2」調整為「簡單倒數」。
- Launcher icon 改為活潑漸層背景的原創沙漏圖示，並支援 Android 13 themed icon。

## [1.1.0] - 2026-08-22

### Changed

- 改善預設卡片拖曳時的手指同步與換位動畫。
- 放大倒數畫面的時間字型。
- 將設定對話框標題簡化為「設定」。
- 強化鈴響時間已選選項的視覺對比。
- 設定改為點擊後立即套用及儲存。
- 鈴響選項調整為無聲、10 秒、30 秒、1 分鐘、5 分鐘與不自動停止。
- 深色模式會同步更新 Android 系統導覽列。

### Added

- 新增 4 種預設識別色，色盤增加至 12 色。
- 新增珊瑚紅、活力橙、薄荷綠、晴空藍、亮紫色與莓果紅六種 App 主題。

### Removed

- 移除獨立的完成提示音開關。

## [1.0.0] - 2026-08-21

### Added

- 使用 Kotlin、Jetpack Compose 與 Material 3 建立原生 Android App。
- 預設倒數的新增、編輯、複製、刪除與排序。
- 自訂倒數、圓形進度、暫停、繼續、加時、重設與停止。
- 前景服務、通知快速操作、完成鬧鈴及開機恢復。
- 完成提示音、答答聲、常亮、深色模式與動態色彩設定。
- DataStore 持久化、單元測試、Compose 啟動測試與 Android lint。

[Unreleased]: https://github.com/mark216tw/simple-countdown-2/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/mark216tw/simple-countdown-2/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/mark216tw/simple-countdown-2/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/mark216tw/simple-countdown-2/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/mark216tw/simple-countdown-2/tree/v1.0.0
