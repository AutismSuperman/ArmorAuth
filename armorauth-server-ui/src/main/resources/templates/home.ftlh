<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>控制台 - ArmorAuth</title>
    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
    <link rel="shortcut icon" href="/favicon.svg">
    <link rel="stylesheet" href="/assets/armorauth-ui.css?v=2026052402">
</head>
<body>
<div class="page-shell">
    <main class="center-stack">
        <section class="surface-card">
            <div class="surface-inner">
                <div class="toolbar">
                    <div class="brand-chip">
                        <img class="brand-mark" src="/favicon.svg" alt="ArmorAuth logo">
                        <span class="brand-copy">
                            <strong>ArmorAuth</strong>
                            <span>控制台</span>
                        </span>
                    </div>
                    <div class="top-actions">
                        <button class="theme-toggle" type="button" data-theme-toggle>
                            <span class="theme-toggle-icon" data-theme-icon>
                                <svg class="icon-sun" viewBox="0 0 24 24">
                                    <circle cx="12" cy="12" r="5"></circle>
                                    <line x1="12" y1="1" x2="12" y2="3"></line>
                                    <line x1="12" y1="21" x2="12" y2="23"></line>
                                    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                                    <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                                    <line x1="1" y1="12" x2="3" y2="12"></line>
                                    <line x1="21" y1="12" x2="23" y2="12"></line>
                                    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                                    <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
                                </svg>
                                <svg class="icon-moon" viewBox="0 0 24 24">
                                    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
                                </svg>
                            </span>
                            <span data-theme-text>深色</span>
                        </button>
                        <form action="/logout" method="post">
                            <button class="button secondary" type="submit">退出登录</button>
                        </form>
                    </div>
                </div>

                <div class="hero-grid">
                    <div class="section-title">
                        <span class="eyebrow">首页</span>
                        <h1>欢迎，${userName}</h1>
                        <p>你的账号服务已就绪。</p>
                    </div>

                    <div class="meta-strip">
                        <strong>快捷入口</strong>
                        <br>
                        1. 发起新的授权请求
                        <br>
                        2. 处理设备激活
                        <br>
                        3. 进入联邦登录流程
                    </div>
                </div>
            </div>
        </section>
    </main>
    <footer class="page-footer">
        <div class="page-footer-brand">
            <strong>ArmorAuth</strong>
            <span>统一身份与授权平台</span>
        </div>
        <div class="page-footer-nav" aria-label="页面信息">
            <span>控制台</span>
            <span>账号服务</span>
            <span>安全会话</span>
        </div>
    </footer>
</div>
<script src="/assets/armorauth-ui.js?v=20260524"></script>
</body>
</html>
