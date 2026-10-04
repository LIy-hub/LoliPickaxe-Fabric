# Forge 1.20.1 GUI synchronization

Panels are drawn at runtime and measured using the active font and translated text.
Grey-panel labels use vanilla dark text without shadows. Configuration buttons show
full labels on hover; registry errors prevent invalid submissions. Unsaved editor,
URL and password fields survive resize. Card zoom/pan is clipped to the viewport;
closed or superseded image requests release their images instead of installing a
stale texture, and returning from link confirmation reloads the online image.

The storage server menu and client agree on the same 240x256 layout as Fabric:
nine storage rows start at y=8, inventory rows at y=174, and hotbar at y=232.
The layout leaves no spare inventory-title row. A local continuous transform fits
an overflowing panel and inverse-transforms hovering, clicks, drags and scrolling.
A 427x247 GUI viewport needs about 7 percent reduction; 427x240 needs about 9 percent.
The window GUI scale and HUD remain unchanged. Forge keeps the auto-accept button
and its ID-list blacklist, whose list scrolls and clips within a measured panel.

Empty storage displays one page. The last occupied slot determines the visible
tail; a fully occupied 81-slot tail exposes one additional insertion page, bounded
by existing tool capacity. Sparse saved slots retain their addresses. Page shrink
is clamped only after a slot transaction, and an explicit S2C page cue precedes
vanilla contents. The selected page is saved in LoliStorageCurrentPage; existing
LoliStorage entries retain their format. Open menus share their active list with
automatic pickup and drop-all so closing a menu cannot overwrite those changes.
Forge channel protocol 2 requires this update on both client and server.

## Verification

`gradlew.bat build` includes `verifyGuiLayout` (20 editor-layout scenarios and six
local container transforms) and `verifyStoragePages` (the production occupancy
calculation: empty, growth, shrink, sparse tail, capacity and drop-all). The latter
is headless and does not claim ItemStack/NBT or live-network behavior. Forge's
native class transformers require the native `runClient` for startup verification.
Package checks cover production menu/client classes, Java 17, Forge metadata,
recipe/resources and bilingual keys; test classes must not appear in the release JAR.

Build and startup are distinct from in-game/visual acceptance. In English and
Simplified Chinese, check configuration, enchantment, effect, teleport, cards,
blacklist, workbench and storage. Resize with unsaved input; test long names and
invalid values. Fill 81 slots, continue to the next page, empty the tail, reopen,
and verify page count, carried items and saved contents. Check sparse old saves,
automatic pickup while the menu is open and drop-all. The user operates and
accepts these screens; retain any crash report together with the triggering action.

## 中文

面板在初始化和窗口变化时按字体、翻译文字测量并实时绘制；灰底文字使用原版
深色、无阴影字体。长按钮可悬停查看完整文字，输入无效时阻止提交；重建界面
保留输入。卡片图片限定在视口中，过期异步加载释放图片，链接确认返回后重新
加载网络图片。储藏室统一为 Fabric 的 240x256 槽位布局，只对放不下的面板进行
约 7% 至 9% 的局部缩放，鼠标坐标同步逆变换，HUD 和原版缩放设置不变。
Forge 的自动收纳按钮和 ID 列表黑名单保留，列表在面板内滚动、裁剪。

空储藏室显示 1 页，按末尾占用格动态增减；末页 81 格全满时开放下一页，容量
上限不变。旧稀疏存档保留原位置；槽位操作完成后调整页码，页码通知先于原版
物品同步发送。自动收纳和全部丢出操作共用已打开菜单的数据，防止关闭时覆盖。
Forge 网络协议更新为 2，客户端和服务端都需要使用此更新。

构建包含布局和生产分页计算检查；分页夹具不代表真实 NBT 或联机操作验证。
客户端启动、构建和制品检查均不代表画面验收。游戏内仍需由用户检查中英文、
窗口缩放、输入保留、错误提示、卡片、翻页、槽位点击、重开后的保存内容以及
打开储藏室时自动收纳和全部丢出的结果。
