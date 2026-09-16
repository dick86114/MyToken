<?php
/**
 * MyToken 静态部署 PHP 引导文件
 * 兼容纯 PHP 虚拟主机、宝塔面板、cPanel 等环境
 */

$htmlFile = __DIR__ . '/index.html';

if (file_exists($htmlFile)) {
    // 设置正确的响应头与字符集
    header('Content-Type: text/html; charset=utf-8');
    readfile($htmlFile);
} else {
    http_response_code(404);
    echo '<h1>404 Not Found</h1><p>index.html not found. Please verify the build artifacts.</p>';
}
exit;
