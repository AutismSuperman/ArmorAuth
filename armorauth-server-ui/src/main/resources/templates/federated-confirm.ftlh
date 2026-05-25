<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>账号确认 - ArmorAuth</title>
    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
    <link rel="shortcut icon" href="/favicon.svg">
    <link rel="stylesheet" href="/assets/armorauth-ui.css?v=2026052408">
</head>
<body class="authing-login-page identity-page" data-page-title-key="page.federatedConfirm.title">
<div class="page-shell">
    <main class="login-product-shell is-wide">
        <section class="login-visual-panel" data-visual="federated">
            <div class="login-brand">
                <img class="login-brand-mark" src="/brand/icon.svg" alt="ArmorAuth logo">
                <span>
                    <strong>ArmorAuth</strong>
                    <small>第三方账号确认</small>
                </span>
            </div>

            <div class="login-visual-copy">
                <span class="login-kicker">第三方登录</span>
                <h1>确认第三方账号对应的本地身份。</h1>
                <p>确认后即可继续进入账号服务。</p>
            </div>

            <div class="confirm-stage compact-confirm-stage">
                <div class="confirm-badge">
                    <span class="confirm-badge-label">待确认账号</span>
                    <strong>${pending.displayName()!pending.providerUsername()!'未提供'}</strong>
                    <p>确认当前第三方账号与本地身份的绑定关系后继续。</p>
                </div>

                <div class="confirm-bridge" aria-hidden="true">
                    <div class="confirm-bridge-node">
                        <span>第三方</span>
                        <strong>${pending.registrationId()}</strong>
                    </div>
                    <div class="confirm-bridge-link"></div>
                    <div class="confirm-bridge-node">
                        <span>本地账号</span>
                        <strong>ArmorAuth</strong>
                    </div>
                </div>

                <div class="confirm-summary" aria-label="第三方账号摘要">
                    <#if pending.avatarUrl()?? && pending.avatarUrl()?has_content>
                        <div class="confirm-summary-row confirm-summary-row-avatar">
                            <span class="confirm-summary-label">第三方头像</span>
                            <div class="avatar-card">
                                <img src="${pending.avatarUrl()}" alt="第三方头像">
                            </div>
                        </div>
                    </#if>
                    <div class="confirm-summary-row">
                        <span class="confirm-summary-label">登录来源</span>
                        <strong>${pending.registrationId()}</strong>
                    </div>
                    <div class="confirm-summary-row">
                        <span class="confirm-summary-label">第三方昵称</span>
                        <strong>${pending.displayName()!pending.providerUsername()!'未提供'}</strong>
                    </div>
                    <div class="confirm-summary-row">
                        <span class="confirm-summary-label">第三方标识</span>
                        <strong>${providerUserIdSummary}</strong>
                    </div>
                </div>
            </div>
        </section>

        <section class="login-card-panel">
            <div class="login-card is-wide">
                <div class="login-card-toolbar">
                    <div class="login-card-logo">
                        <img src="/brand/icon.svg" alt="ArmorAuth logo">
                        <span>账号确认</span>
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
                    <span class="login-card-kicker">本地账号</span>
                    <h1>确认账号</h1>
                    <p>创建新账号，或绑定到已有账号后继续。</p>
                </div>

                <#if errorMessage??>
                    <div class="notice error">${errorMessage}</div>
                </#if>

                <div class="meta-block">完成一次确认后，后续可直接使用该第三方账号登录。</div>

                <div data-tab-group>
                    <div class="switcher" aria-label="确认方式">
                        <button type="button" class="<#if selectedTab == 'create'>is-active</#if>" data-tab-target="create-account">创建新账号</button>
                        <button type="button" class="<#if selectedTab == 'bind'>is-active</#if>" data-tab-target="bind-account">绑定已有账号</button>
                    </div>

                    <form id="create-account" class="auth-form" action="/federated/confirm/create" method="post" <#if selectedTab != 'create'>hidden</#if> data-tab-panel>
                        <div class="field-group">
                            <label for="create-username">用户名</label>
                            <input id="create-username" name="username" type="text" autocomplete="username" value="${createUsername!pending.suggestedUsername()}" placeholder="请输入本地用户名">
                        </div>
                        <div class="field-group">
                            <label for="create-password">密码</label>
                            <input id="create-password" name="password" type="password" autocomplete="new-password" placeholder="请输入至少 6 位密码">
                        </div>
                        <div class="field-group">
                            <label for="confirm-password">确认密码</label>
                            <input id="confirm-password" name="confirmPassword" type="password" autocomplete="new-password" placeholder="请再次输入密码">
                        </div>
                        <div class="action-row action-row-between">
                            <a class="button secondary" href="/federated/confirm/cancel">返回登录页</a>
                            <button class="button primary" type="submit">创建并完成登录</button>
                        </div>
                    </form>

                    <form id="bind-account" class="auth-form" action="/federated/confirm/bind" method="post" <#if selectedTab != 'bind'>hidden</#if> data-tab-panel>
                        <div class="field-group">
                            <label for="bind-username">已有用户名</label>
                            <input id="bind-username" name="username" type="text" autocomplete="username" value="${bindUsername!''}" placeholder="请输入已有本地用户名">
                        </div>
                        <div class="field-group">
                            <label for="bind-password">密码</label>
                            <input id="bind-password" name="password" type="password" autocomplete="current-password" placeholder="请输入已有账号密码">
                        </div>
                        <div class="action-row action-row-between">
                            <a class="button secondary" href="/federated/confirm/cancel">返回登录页</a>
                            <button class="button primary" type="submit">绑定并完成登录</button>
                        </div>
                    </form>
                </div>

                <p class="footer-copy">ArmorAuth 第三方接入</p>
            </div>
        </section>
    </main>
    <footer class="page-footer">
        <div class="page-footer-brand">
            <strong>ArmorAuth</strong>
            <span>统一身份与授权平台</span>
        </div>
        <div class="page-footer-nav" aria-label="页面信息">
            <span>第三方登录</span>
            <span>身份确认</span>
            <span>绑定安全</span>
        </div>
    </footer>
</div>
<script src="/assets/armorauth-ui.js?v=2026052404"></script>
</body>
</html>
