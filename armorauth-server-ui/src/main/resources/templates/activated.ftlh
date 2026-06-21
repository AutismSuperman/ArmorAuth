<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>设备已连接 - ArmorAuth</title>
    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
    <link rel="shortcut icon" href="/favicon.svg">
    <link rel="stylesheet" href="/assets/armorauth-ui.css?v=2026062112">
</head>
<body class="authing-login-page identity-page google-login-page auth-flow-page" data-page-title-key="page.activated.title">
<div class="page-shell">
    <main class="login-product-shell">
        <section class="login-visual-panel" data-visual="device-done">
            <div class="login-brand">
                <img class="login-brand-mark" src="/brand/icon.svg" alt="ArmorAuth logo">
                <span>
                    <strong>ArmorAuth</strong>
                    <small>设备已连接</small>
                </span>
            </div>

            <div class="login-visual-copy">
                <span class="login-kicker">已完成</span>
                <h1>设备已连接</h1>
                <p>现在可以返回设备继续操作。</p>
            </div>

            <div class="identity-menu" aria-label="设备已连接">
                <div class="identity-menu-item is-active">
                    <span class="identity-menu-icon">01</span>
                    <span>
                        <strong>已完成确认</strong>
                        <small>本次激活已经生效，无需重复提交。</small>
                    </span>
                </div>
                <div class="identity-menu-item is-active">
                    <span class="identity-menu-icon">02</span>
                    <span>
                        <strong>设备继续登录</strong>
                        <small>设备获取结果后会继续完成登录。</small>
                    </span>
                </div>
            </div>

            <div class="login-visual-stack">
                <div class="login-preview-card">
                    <span>会话已确认</span>
                    <strong>Device Flow</strong>
                    <small>继续登录</small>
                </div>
                <div class="login-preview-grid">
                    <span>OIDC</span>
                    <span>Code</span>
                    <span>SSO</span>
                    <span>MFA</span>
                </div>
            </div>
        </section>

        <section class="login-card-panel">
            <div class="login-card">
                <div class="login-card-toolbar">
                    <div class="login-card-logo">
                        <img src="/brand/icon.svg" alt="ArmorAuth logo">
                        <span>设备已连接</span>
                    </div>
                    <div class="top-actions">
                        <button class="locale-toggle" type="button" data-locale-toggle>
                            <span data-locale-text>EN</span>
                        </button>
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
                    </div>
                </div>

                <div class="login-card-title">
                    <span class="login-card-kicker">已完成</span>
                    <h1>设备已连接</h1>
                    <p>现在可以返回设备继续操作。</p>
                </div>

                <div class="notice success">
                    <span class="notice-message">如果设备没有自动刷新，请返回设备后重试。</span>
                </div>

                <div class="info-grid">
                    <div class="info-card">
                        <strong>已完成确认</strong>
                        <p>本次激活已经生效，无需重复提交。</p>
                    </div>
                    <div class="info-card">
                        <strong>需要手动刷新时</strong>
                        <p>返回设备页面重新进入即可。</p>
                    </div>
                </div>

                <div class="action-row action-row-between">
                    <a class="button secondary" href="/login">返回登录页</a>
                    <a class="button primary" href="/">返回首页</a>
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
            <span>设备已连接</span>
            <span>继续登录</span>
            <span>会话已确认</span>
        </div>
    </footer>
</div>
<script src="/assets/armorauth-ui.js?v=2026062113"></script>
</body>
</html>
