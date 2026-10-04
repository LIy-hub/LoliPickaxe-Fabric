# Fabric 1.20.5 GUI layout

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

Grey-panel labels and counters use the same shadow-free vanilla font drawing.
Storage and workbench slot coordinates retain their server-menu geometry. Storage
page information uses its own sidebar rows, and the player-inventory label no
longer covers the storage grid. Storage and blacklist panels smoothly shrink only
when they exceed the viewport, by the amount needed to leave a small margin.
The window's GUI scale, HUD and saved options remain unchanged. Drawing, carried
items, hovering, clicks, releases, drags and ghost-slot editing share a local
transform. Grid tooltips are flushed under that transform. Panels that fit retain
their original size. A 427x247 GUI viewport needs about 7 percent shrinkage,
and a 427x240 viewport about 9 percent, rather than halving the whole GUI.

Storage shows one page when empty, then through the last occupied page. When that
page has all 81 slots occupied, it exposes one more insertion page, up to the
existing capacity (100 pages for the final pickaxe). Stack quantity is not a slot
count. Sparse legacy slots keep their positions and remain reachable. Removing
tail contents shrinks the count and clamps the selected page after the slot
transaction, with an S2C page cue before vanilla slot synchronization.

## Verification

`gradlew.bat build` includes `verifyGuiLayout`: minimum 320x240 GUI dimensions,
wrapped title/name/help/error rows, four viewport sizes, panel containment and
resize/outside-click behavior, plus six local-container-transform scenarios including
the reported 854x493 window at GUI scale 2. Slot-centre hit tests cover all nine grid
rows; a normal small window must retain at least 90 percent of the panel size.
`verifyStoragePages` exercises real ItemStacks/NBT in
an isolated bootstrap fixture: growth, shrinkage, sparse page 100, reopening and
drop-all. It does not exercise a live server connection. Package inspection checks menu/client classes, matching version metadata, JSON resources and bilingual keys. These checks do not constitute visual acceptance.

In the client, check configuration, enchantment, potion, teleport, online-card,
storage and password-workbench screens in English and Simplified Chinese. Change
GUI scale/window size with unsaved input, try invalid/empty enchantment levels,
and verify long names, error messages, sparse page 100, hover text and normal slot clicks.
Fill a page, continue into the next, remove the tail items and reopen the tool.
Verify that page count, carried items and saved contents agree, and that closing a
small-window screen leaves the HUD scale unchanged.
If a screen crashes, retain the crash report and the action that triggered it.

## 中文

配置、附魔、药水、传送与网络卡片设置界面使用运行时绘制的像素面板，按当前
字体和翻译文字测量布局；窗口或 GUI 大小变化时重建。长文字在预留区域换行，
超出行数时可悬停查看完整内容。界面绘制和外部点击判定使用同一组边界。
卡片图片限定在查看区域内，放大和拖动不会盖住标题或控件；链接确认返回后会
重新加载已释放的网络图片，旧加载回调不会覆盖重新打开的查看器。

重建控件会保留尚未保存的输入。附魔等级输入无效时显示错误并阻止添加、保存。
灰底文字和计数统一使用不带阴影的原版字体绘制。储藏室和密码工作台保留服务端
槽位坐标，文字不再覆盖储藏室格子。储藏室和黑名单面板超出窗口时，仅按需要
连续缩小面板，留出少量边距；不改动原版 GUI 缩放、HUD 或缩放设置。绘制、
手持物品、悬停、点击、释放、拖动及黑名单格子使用同一组变换，提示也在面板
变换内绘制。能够放下时保留原尺寸。427x247 的 GUI 视口仅缩小约 7%，427x240
约 9%，不再把整个 GUI 缩小一半。

储藏室为空显示 1 页，有物品时显示到最后一个占用页；该页 81 个格子全部占用后
开放下一页供存入，最终镐容量上限仍为 100 页。按格子占用计算，不把物品数量
当成格子数量。旧存档稀疏物品保留原位置且可访问；取空末尾页后收缩页数，槽位
操作完成后才调整当前页，并在原版槽位同步之前发送页码，防止写入错误页。

构建检查覆盖 20 组尺寸/长文字及 6 组面板变换（含截图的 854x493、GUI 缩放 2），
检查九行格子的点击映射，并要求常见小窗口保留至少 90% 的面板大小；使用真实
ItemStack/NBT 的隔离夹具检查分页增减、稀疏第 100 页、重开和全部丢出；该夹具
不代表服务端联机操作验证。正式画面
仍需在游戏中检查中英文、GUI 缩放、输入保留、错误提示、翻页和槽位操作；存满
一页后继续存入、取空末尾页并重开，检查物品和页数；检查小窗口界面打开、关闭
时 HUD 缩放均不受影响。
