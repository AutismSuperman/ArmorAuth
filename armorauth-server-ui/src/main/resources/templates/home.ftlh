<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>控制台 - ArmorAuth</title>
    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
    <link rel="shortcut icon" href="/favicon.svg">
    <link rel="stylesheet" href="/assets/armorauth-ui.css?v=2026052408">
</head>
<body class="authing-login-page identity-page" data-page-title-key="page.home.title">
<div class="page-shell">
    <main class="login-product-shell">
        <section class="login-visual-panel" data-visual="home">
            <div class="login-brand">
                <img class="login-brand-mark" src="/brand/icon.svg" alt="ArmorAuth logo">
                <span>
                    <strong>ArmorAuth</strong>
                    <small>身份云控制台</small>
                </span>
            </div>

            <div class="login-visual-copy">
                <span class="login-kicker">控制台</span>
                <h1>统一身份与授权平台</h1>
                <p>集中处理账号认证、应用授权、MFA 与联合身份登录。</p>
            </div>

            <div class="identity-menu" aria-label="账号服务">
                <div class="identity-menu-item is-active">
                    <span class="identity-menu-icon">01</span>
                    <span>
                        <strong>账号服务</strong>
                        <small>安全会话</small>
                    </span>
                </div>
                <div class="identity-menu-item">
                    <span class="identity-menu-icon">02</span>
                    <span>
                        <strong>授权协议</strong>
                        <small>OAuth2 / OIDC / Device Flow</small>
                    </span>
                </div>
                <div class="identity-menu-item">
                    <span class="identity-menu-icon">03</span>
                    <span>
                        <strong>身份源</strong>
                        <small>Social / SAML / Enterprise IdP</small>
                    </span>
                </div>
            </div>

            <div class="login-visual-stack">
                <div class="login-preview-card">
                    <span>当前入口</span>
                    <strong>Hosted Login</strong>
                    <small>统一登录页</small>
                </div>
                <div class="login-preview-grid">
                    <span>OIDC</span>
                    <span>SSO</span>
                    <span>MFA</span>
                    <span>RBAC</span>
                </div>
            </div>
        </section>

        <section class="login-card-panel">
            <div class="login-card">
                <div class="login-card-toolbar">
                    <div class="login-card-logo">
                        <img src="/brand/icon.svg" alt="ArmorAuth logo">
                        <span>控制台</span>
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
                    <span class="login-card-kicker">首页</span>
                    <h1 data-i18n-template="home.welcome" data-i18n-value-name="${userName}">欢迎，${userName}</h1>
                    <p>你的账号服务已就绪。</p>
                </div>

                <div class="meta-block">
                    <strong>快捷入口</strong>
                    <span>1. 发起新的授权请求</span>
                    <span>2. 处理设备激活</span>
                    <span>3. 进入联邦登录流程</span>
                </div>

                <div class="action-row action-row-between">
                    <a class="button secondary" href="/login">返回登录页</a>
                    <form action="/logout" method="post">
                        <button class="button primary" type="submit">退出登录</button>
                    </form>
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
<script src="/assets/armorauth-ui.js?v=2026052404"></script>
</body>
</html>
