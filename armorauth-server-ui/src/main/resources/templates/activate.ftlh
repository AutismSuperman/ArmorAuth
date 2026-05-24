<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>连接设备 - ArmorAuth</title>
    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
    <link rel="shortcut icon" href="/favicon.svg">
    <link rel="stylesheet" href="/assets/armorauth-ui.css?v=2026052402">
</head>
<body>
<div class="page-shell">
    <main class="panel-frame">
        <section class="visual-panel">
            <div class="brand-row">
                <div class="brand-chip">
                    <img class="brand-mark" src="/favicon.svg" alt="ArmorAuth logo">
                    <span class="brand-copy">
                        <strong>ArmorAuth</strong>
                        <span>设备激活</span>
                    </span>
                </div>
                <div class="top-actions">
                    <span class="status-pill">设备登录</span>
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

            <div class="hero-copy">
                <span class="eyebrow">设备激活</span>
                <h1>输入激活码，继续完成设备登录。</h1>
                <p>适用于电视、命令行和其他需要确认登录的设备场景。</p>
            </div>

            <div class="summary-grid">
                <div class="summary-item">
                    <span>验证地址</span>
                    <strong>${deviceVerificationEndpoint}</strong>
                </div>
                <div class="summary-item">
                    <span>输入示例</span>
                    <strong>例如 ABCD-EFGH</strong>
                </div>
            </div>
        </section>

        <section class="content-panel">
            <div class="content-column">
                <div class="section-title">
                    <span class="eyebrow">输入激活码</span>
                    <h1>连接设备</h1>
                    <p>验证成功后，设备将继续完成登录。</p>
                </div>

                <form class="auth-form" action="${deviceVerificationEndpoint}" method="post">
                    <div class="field-group">
                        <label for="user_code">激活码</label>
                        <input id="user_code" name="user_code" type="text" placeholder="例如：ABCD-EFGH" autofocus required
                               style="text-align: center; font-size: 1.1rem; letter-spacing: 0.15em; font-weight: 600;">
                    </div>
                    <div class="action-row">
                        <button class="button primary" type="submit">继续验证</button>
                        <a class="button secondary" href="/login">返回登录</a>
                    </div>
                </form>
            </div>
        </section>
    </main>
    <footer class="page-footer">
        <div class="page-footer-brand">
            <strong>ArmorAuth</strong>
            <span>统一身份与授权平台</span>
        </div>
        <div class="page-footer-nav" aria-label="页面信息">
            <span>设备激活</span>
            <span>验证码确认</span>
            <span>安全连接</span>
        </div>
    </footer>
</div>
<script src="/assets/armorauth-ui.js?v=20260524"></script>
</body>
</html>
