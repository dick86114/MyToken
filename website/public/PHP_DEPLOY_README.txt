=======================================================
           MyToken 官网 - PHP 环境部署指南
=======================================================

【目录结构】
- index.html           : 生产环境主页面
- index.php            : PHP 入口包装文件，用于仅允许 PHP 默认文档的主机
- .htaccess            : Apache 伪静态、Gzip 与静态资源缓存
- nginx.conf.example   : Nginx / 宝塔 / 1Panel 配置参考
- app-icon.png         : 本地应用图标
- app-banner.png       : 本地展示图
- assets/              : 构建后的 CSS 与 JavaScript 静态资源

-------------------------------------------------------
【方案一：宝塔面板 / 1Panel + Nginx】
1. 在面板中创建站点，网站根目录可设置为 /www/wwwroot/your-domain。
2. 将本压缩包内全部文件直接上传到网站根目录，并确保 assets 目录保持同级。
3. 在站点伪静态中配置：
   location / {
       try_files $uri $uri/ /index.html;
   }
4. 如果站点使用 PHP-FPM，也可以把默认首页设置为 index.php。
5. 访问域名确认首页加载。

-------------------------------------------------------
【方案二：Apache / cPanel / PHP 虚拟主机】
1. 将压缩包内全部文件上传到 public_html 或网站根目录。
2. 确保 .htaccess 已上传，并允许站点读取该文件。
3. 如果主机默认文档只启用 index.php，index.php 会自动读取并输出 index.html。
4. 直接访问域名即可。

-------------------------------------------------------
【方案三：纯静态 Nginx】
1. 将 index.html、app-icon.png、app-banner.png 和 assets 上传到网站根目录。
2. 参考 nginx.conf.example 配置 try_files 与静态资源缓存。
3. 无需 PHP 运行环境。

-------------------------------------------------------
【更新方式】
1. 备份当前网站根目录。
2. 删除旧的 assets 目录后再上传新包的 assets，避免旧哈希文件残留。
3. 覆盖 index.html、index.php、.htaccess、nginx.conf.example 和图片文件。
4. 强制刷新浏览器并检查首页、下载按钮和供应商矩阵。
