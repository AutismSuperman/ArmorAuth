<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>控制台 - ArmorAuth</title>
    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
    <link rel="shortcut icon" href="/favicon.svg">
    <link rel="stylesheet" href="/assets/armorauth-ui.css?v=2026062112">
</head>
<body class="authing-login-page identity-page google-account-page" data-page-title-key="page.home.title">
<div class="page-shell">
    <main class="login-product-shell">
        <section class="login-visual-panel" data-visual="home">
            <div class="login-brand">
                <img class="login-brand-mark" src="/brand/icon.svg" alt="ArmorAuth logo">
                <span>
                    <strong>ArmorAuth 账号</strong>
                    <small>个人账号中心</small>
                </span>
            </div>

            <div class="login-visual-copy">
                <span class="login-kicker">首页</span>
                <h1>欢迎回来</h1>
                <p>查看账号安全、授权应用和登录会话。</p>
            </div>

            <div class="identity-menu" aria-label="账号服务">
                <button class="identity-menu-item is-active" type="button" data-account-section-target="home">
                    <span class="identity-menu-icon">H</span>
                    <span>
                        <strong>首页</strong>
                        <small>账号概览</small>
                    </span>
                </button>
                <button class="identity-menu-item" type="button" data-account-section-target="profile">
                    <span class="identity-menu-icon">ID</span>
                    <span>
                        <strong>个人信息</strong>
                        <small>资料与联系方式</small>
                    </span>
                </button>
                <button class="identity-menu-item" type="button" data-account-section-target="security">
                    <span class="identity-menu-icon">S</span>
                    <span>
                        <strong>安全性与登录</strong>
                        <small>MFA / Passkey / 会话</small>
                    </span>
                </button>
                <button class="identity-menu-item" type="button" data-account-section-target="apps">
                    <span class="identity-menu-icon">A</span>
                    <span>
                        <strong>关联的应用</strong>
                        <small>OAuth2 / OIDC 授权</small>
                    </span>
                </button>
                <button class="identity-menu-item" type="button" data-account-section-target="privacy">
                    <span class="identity-menu-icon">D</span>
                    <span>
                        <strong>数据和隐私设置</strong>
                        <small>活动与审计</small>
                    </span>
                </button>
            </div>

            <div class="account-side-actions">
                <a class="account-side-link" href="/login">返回登录页</a>
                <form action="/logout" method="post">
                    <button class="account-side-button account-side-button-primary" type="submit">退出登录</button>
                </form>
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
                    <div class="account-profile-hero">
                        <span class="account-profile-avatar">
                            <img src="/brand/icon.svg" alt="ArmorAuth logo">
                        </span>
                        <h1 data-i18n-template="home.welcome" data-i18n-value-name="${userName}">欢迎，${userName}</h1>
                        <p>管理你的账号、安全设置和授权应用。</p>
                    </div>
                    <div class="account-chip-row" aria-label="账号快捷入口">
                        <button type="button" data-account-section-target="profile">个人信息</button>
                        <button type="button" data-account-section-target="security">我的 MFA</button>
                        <button type="button" data-account-section-target="apps">授权应用</button>
                        <button type="button" data-account-section-target="privacy">登录活动</button>
                    </div>
                </div>

                <div class="account-center" data-account-center>
                    <section class="account-section is-active" data-account-section="home">
                        <div class="account-summary-grid">
                            <button class="account-summary-card" type="button" data-account-section-target="profile">
                                <strong>个人信息</strong>
                                <span data-overview-profile>加载资料状态...</span>
                            </button>
                            <button class="account-summary-card" type="button" data-account-section-target="security">
                                <strong>账号安全</strong>
                                <span data-overview-security>正在检查 MFA 状态...</span>
                            </button>
                            <button class="account-summary-card" type="button" data-account-section-target="apps">
                                <strong>授权应用</strong>
                                <span>查看已授权的 OAuth2/OIDC 应用。</span>
                            </button>
                            <button class="account-summary-card" type="button" data-account-section-target="privacy">
                                <strong>数据和隐私</strong>
                                <span>查看最近登录和账号安全活动。</span>
                            </button>
                        </div>
                        <div class="meta-block meta-block-compact">
                            <strong>快捷入口</strong>
                            <span>1. 发起新的授权请求</span>
                            <span>2. 处理设备激活</span>
                            <span>3. 进入联邦登录流程</span>
                        </div>
                    </section>

                    <section class="account-section" data-account-section="profile" hidden>
                        <div class="account-section-head">
                            <div>
                                <strong>个人信息</strong>
                                <span>维护显示名称、邮箱和手机号，并完成联系方式验证。</span>
                            </div>
                            <span class="account-security-badge is-muted" data-profile-status>未加载</span>
                        </div>

                        <form class="account-profile-form" data-profile-form>
                            <div class="profile-card profile-card-basic">
                                <div class="profile-card-head">
                                    <div>
                                        <strong>基础资料</strong>
                                        <span>这些信息用于账号中心展示。</span>
                                    </div>
                                    <button class="button secondary button-compact" type="button" data-profile-refresh>刷新</button>
                                </div>
                                <div class="profile-field-grid">
                                    <div class="field-group">
                                        <label for="profile-display-name">显示名称</label>
                                        <input id="profile-display-name" type="text" autocomplete="name"
                                               data-profile-field="displayName" placeholder="请输入显示名称">
                                    </div>
                                    <div class="field-group">
                                        <label for="profile-about">个人备注</label>
                                        <textarea id="profile-about" rows="3" data-profile-field="profile"
                                                  placeholder="可选，记录账号备注"></textarea>
                                    </div>
                                </div>
                                <div class="profile-form-actions">
                                    <button class="button primary button-compact" type="submit">保存资料</button>
                                </div>
                            </div>

                            <div class="profile-card">
                                <div class="profile-card-head">
                                    <div>
                                        <strong>联系方式</strong>
                                        <span>邮箱和手机号需要先保存，再发送验证码完成验证。</span>
                                    </div>
                                </div>

                                <div class="contact-verify-card" data-contact-card="email">
                                    <div class="contact-verify-main">
                                        <span class="contact-verify-icon">E</span>
                                        <div>
                                            <strong>邮箱</strong>
                                            <span data-contact-summary="email">填写并保存后可验证。</span>
                                        </div>
                                    </div>
                                    <span class="account-security-badge is-muted" data-contact-status="email">未验证</span>
                                    <div class="field-group contact-value-field">
                                        <label for="profile-email">邮箱地址</label>
                                        <input id="profile-email" type="email" autocomplete="email"
                                               data-profile-field="email" placeholder="请输入邮箱">
                                    </div>
                                    <div class="contact-verify-actions">
                                        <button class="button secondary button-compact" type="button" data-send-contact-code="email">
                                            发送验证码
                                        </button>
                                        <div class="contact-code-row" data-contact-code-row="email" hidden>
                                            <input type="text" inputmode="numeric" maxlength="8" placeholder="输入验证码"
                                                   data-contact-code="email">
                                            <button class="button primary button-compact" type="button" data-verify-contact="email">
                                                完成验证
                                            </button>
                                        </div>
                                    </div>
                                    <small data-contact-message="email"></small>
                                </div>

                                <div class="contact-verify-card" data-contact-card="phone">
                                    <div class="contact-verify-main">
                                        <span class="contact-verify-icon">P</span>
                                        <div>
                                            <strong>手机号</strong>
                                            <span data-contact-summary="phone">填写并保存后可验证。</span>
                                        </div>
                                    </div>
                                    <span class="account-security-badge is-muted" data-contact-status="phone">未验证</span>
                                    <div class="field-group contact-value-field">
                                        <label for="profile-phone">手机号</label>
                                        <input id="profile-phone" type="tel" autocomplete="tel"
                                               data-profile-field="phone" placeholder="请输入手机号">
                                    </div>
                                    <div class="contact-verify-actions">
                                        <button class="button secondary button-compact" type="button" data-send-contact-code="phone">
                                            发送验证码
                                        </button>
                                        <div class="contact-code-row" data-contact-code-row="phone" hidden>
                                            <input type="text" inputmode="numeric" maxlength="8" placeholder="输入验证码"
                                                   data-contact-code="phone">
                                            <button class="button primary button-compact" type="button" data-verify-contact="phone">
                                                完成验证
                                            </button>
                                        </div>
                                    </div>
                                    <small data-contact-message="phone"></small>
                                </div>
                            </div>
                        </form>
                    </section>

                    <section class="account-section" data-account-section="security" hidden>
                        <div class="account-security-panel" data-account-security>
                            <div class="account-security-head">
                                <div>
                                    <strong>账号安全</strong>
                                    <span data-mfa-summary>正在检查 MFA 状态...</span>
                                </div>
                                <span class="account-security-badge is-muted" data-mfa-status>检查中</span>
                            </div>

                            <div class="security-toggle-row">
                                <span>
                                    <strong>登录二次验证</strong>
                                    <small>开启后，登录时需要验证 Authenticator 或恢复码。</small>
                                </span>
                                <button class="security-switch" type="button" aria-label="切换登录二次验证"
                                        aria-pressed="false" data-mfa-toggle disabled>
                                    <span></span>
                                </button>
                            </div>

                            <div class="factor-list" data-factor-list>
                                <div class="factor-empty">正在加载验证方法...</div>
                            </div>

                            <div class="security-actions-inline">
                                <button class="button secondary button-compact" type="button" data-totp-setup>
                                    添加 Authenticator
                                </button>
                                <button class="button secondary button-compact" type="button" data-factor-refresh>
                                    刷新
                                </button>
                            </div>

                            <div class="totp-setup-panel" data-totp-panel hidden>
                                <div class="totp-setup-head">
                                    <strong>Authenticator 设置</strong>
                                    <span>使用 Authenticator 扫描二维码，或在手机上打开 otpauth 链接，然后输入 6 位验证码完成绑定。</span>
                                </div>
                                <div class="totp-setup-grid">
                                    <div class="totp-qr-card">
                                        <img src="" alt="Authenticator 二维码" data-totp-qr hidden>
                                        <span data-totp-qr-empty>二维码生成后显示在这里</span>
                                    </div>
                                    <div class="totp-manual-fields">
                                        <a class="button secondary button-compact totp-open-link" href="#" data-totp-open hidden>
                                            打开 Authenticator 链接
                                        </a>
                                        <div class="copy-field">
                                            <label>Secret</label>
                                            <code data-totp-secret>-</code>
                                            <button type="button" data-copy-field="secret">复制</button>
                                        </div>
                                        <div class="copy-field">
                                            <label>otpauth URI</label>
                                            <code data-totp-uri>-</code>
                                            <button type="button" data-copy-field="uri">复制</button>
                                        </div>
                                    </div>
                                </div>
                                <div class="field-group compact-field">
                                    <label for="totp-code">验证码</label>
                                    <input id="totp-code" type="text" inputmode="numeric" maxlength="8"
                                           autocomplete="one-time-code" placeholder="请输入 6 位验证码" data-totp-code>
                                </div>
                                <div class="security-actions-inline">
                                    <button class="button primary button-compact" type="button" data-totp-verify>
                                        验证并启用
                                    </button>
                                    <button class="button secondary button-compact" type="button" data-totp-cancel>
                                        取消
                                    </button>
                                </div>
                                <div class="recovery-code-panel" data-recovery-panel hidden>
                                    <strong>恢复码</strong>
                                    <span>请保存这些恢复码，身份验证器不可用时可用于登录。</span>
                                    <div class="recovery-code-grid" data-recovery-codes></div>
                                </div>
                            </div>
                        </div>
                    </section>

                    <section class="account-section" data-account-section="apps" hidden>
                        <div class="account-section-head">
                            <div>
                                <strong>关联的应用</strong>
                                <span>这里用于展示你授权过的 OAuth2/OIDC 应用和第三方登录绑定。</span>
                            </div>
                        </div>
                        <div class="factor-empty">应用授权和第三方绑定列表后续接入到这里。</div>
                    </section>

                    <section class="account-section" data-account-section="privacy" hidden>
                        <div class="account-section-head">
                            <div>
                                <strong>数据和隐私设置</strong>
                                <span>这里用于查看最近登录、安全审计和账号数据。</span>
                            </div>
                        </div>
                        <div class="factor-empty">最近活动和数据导出后续接入到这里。</div>
                    </section>
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
<script src="/assets/armorauth-ui.js?v=2026062113"></script>
</body>
</html>
