# Fabric 26.2 GUI layout

The configuration, enchantment, potion, teleport and online-card editors use
runtime-drawn pixel panels. Layout is measured from the active font and translated
text when widgets are initialized or rebuilt after a window/GUI-size change.
Long labels wrap into bounded rows; hovering a label that exceeds its reserved
rows shows its full text. Drawing and outside-click checks share the panel bounds.
Card images are clipped to their viewport so zoom/pan cannot cover titles or
controls; returning from a link confirmation reloads a released online texture,
and stale image-load callbacks cannot install a texture into a reopened viewer.

Unsaved configuration values, enchantment input, teleport coordinates, online-card
URLs and workbench passwords survive widget rebuilding. Invalid enchantment
levels display an error and prevent applying or saving a stale level.

Storage and workbench slot coordinates retain their server-menu geometry. Storage
page information uses its own sidebar rows, and the player-inventory label no
longer covers the storage grid. The 9x9 storage/blacklist containers still need a
GUI viewport tall enough for the original 256-pixel slot layout; lower GUI scale
when using a small window.

## Verification

`gradlew.bat build` includes `verifyGuiLayout`: minimum 320x240 GUI dimensions,
wrapped title/name/help/error rows, four viewport sizes, panel containment and
resize/outside-click behavior. Existing release/full-port scripts validate the
packaged gameplay and resources. These checks do not constitute visual acceptance.

In the client, check configuration, enchantment, potion, teleport, online-card,
storage and password-workbench screens in English and Simplified Chinese. Change
GUI scale/window size with unsaved input, try invalid/empty enchantment levels,
and verify long names, error messages, page 100, hover text and normal slot clicks.
If a screen crashes, retain the crash report and the action that triggered it.

## 中文

配置、附魔、药水、传送与网络卡片设置界面使用运行时绘制的像素面板，按当前
字体和翻译文字测量布局；窗口或 GUI 大小变化时重建。长文字在预留区域换行，
超出行数时可悬停查看完整内容。界面绘制和外部点击判定使用同一组边界。
卡片图片限定在查看区域内，放大和拖动不会盖住标题或控件；链接确认返回后会
重新加载已释放的网络图片，旧加载回调不会覆盖重新打开的查看器。

重建控件会保留尚未保存的输入。附魔等级输入无效时显示错误并阻止添加、保存。
储藏室和密码工作台保留服务端槽位坐标，文字不再覆盖储藏室格子。9x9 储藏室
及黑名单仍使用高 256 像素的槽位布局，小窗口下需要降低 GUI 缩放。

构建中的布局检查覆盖 20 组尺寸/长文字情况及窗口变化后的点击边界；正式画面
仍需在游戏中检查中英文、GUI 缩放、输入保留、错误提示、翻页和槽位操作。
