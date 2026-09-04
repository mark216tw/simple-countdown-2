# 簡單倒數

以 Kotlin、Jetpack Compose 和 Material 3 實作的 Android 倒數計時器。

- App 名稱：簡單倒數
- Package ID：`com.example.simplecountdown`
- 支援版本：Android 7.0（API 24）以上

## 功能

- 建立、編輯、複製、刪除與拖曳排序倒數預設
- 24 種預設識別色、自訂倒數與識別色進行中提示
- 暫停、繼續、加 1/5 分鐘、重設與停止
- 前景服務通知及通知快速操作
- 完成鬧鈴、答答聲及自動停止時間設定
- 重新開機後恢復進行中的倒數
- 6 種活潑 App 主題色、即時深色模式、橫向排版與保持螢幕常亮

## 安裝 APK

測試用 APK：

- Debug：[`apk/simple-countdown-2-debug.apk`](apk/simple-countdown-2-debug.apk)
- Prerelease（R8 與資源壓縮、Debug 金鑰簽署）：[`apk/simple-countdown-2-prerelease.apk`](apk/simple-countdown-2-prerelease.apk)

各版本附件可從 [GitHub Releases](https://github.com/mark216tw/simple-countdown-2/releases) 下載。

上述 APK 使用 Android Debug 簽章，只適合測試，不應當作正式商店發布版本。安裝前可能需要允許瀏覽器或檔案管理員安裝未知來源 App。

## 技術

- Kotlin 2.2.20、Java 17
- Jetpack Compose、Material 3、Navigation Compose
- ViewModel、StateFlow、Preferences DataStore
- Android foreground service、SharedPreferences timer state
- minSdk 24、targetSdk 35、compileSdk 36

## 建置

需要 Java 17 與 Android SDK 36。

```powershell
.\gradlew.bat assembleDebug
```

本機建置產生的 APK 位於 `app/build/outputs/apk/debug/app-debug.apk`。

## 驗證

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebugAndroidTest
```

通知、鬧鈴、鎖屏與重新開機行為仍應在 Android 13 以上實機進行最終驗收。

## 文件

- [使用指南](docs/USER_GUIDE.md)
- [架構說明](docs/ARCHITECTURE.md)
- [測試指南](docs/TESTING.md)
- [發布流程](docs/RELEASE.md)
- [隱私權說明](PRIVACY.md)
- [版本紀錄](CHANGELOG.md)

## 隱私與授權

App 不包含網路、帳號、分析或廣告功能，資料只保存在使用者裝置內。詳細內容請參閱 [PRIVACY.md](PRIVACY.md)。

本專案採用 [MIT License](LICENSE)。
