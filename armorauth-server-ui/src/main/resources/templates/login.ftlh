<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>欢迎登录 - ArmorAuth</title>
    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
    <link rel="shortcut icon" href="/favicon.svg">
    <link rel="stylesheet" href="/assets/armorauth-ui.css?v=2026052408">
    <script src="/assets/provider-icons.js?v=2026052501"></script>
</head>
<body class="authing-login-page identity-page" data-page-title-key="page.login.title">
<div class="page-shell">
    <main class="login-product-shell">
        <section class="login-visual-panel" data-visual="login">
            <div class="login-brand">
                <img class="login-brand-mark" src="/brand/icon.svg" alt="ArmorAuth logo">
                <span>
                    <strong>ArmorAuth</strong>
                    <small>身份云控制台</small>
                </span>
            </div>

            <div class="login-visual-copy">
                <span class="login-kicker">用户池 / 默认租户</span>
                <h1>进入统一身份工作台</h1>
                <p>集中处理账号认证、应用授权、MFA 与联合身份登录。</p>
            </div>

            <div class="identity-menu" aria-label="身份能力">
                <div class="identity-menu-item is-active">
                    <span class="identity-menu-icon">01</span>
                    <span>
                        <strong>认证体验</strong>
                        <small>Password / Captcha / Passkey</small>
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

            <div class="login-visual-stack" aria-hidden="true">
                <div class="login-preview-card is-primary identity-status-card">
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
                        <span>默认用户池</span>
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
                    <span class="login-card-kicker">Sign in</span>
                    <h1>登录 ArmorAuth</h1>
                    <p>选择一种认证方式继续访问应用。</p>
                </div>

                <#if errorMessage??>
                    <div class="notice error is-dismissible" role="alert" data-page-notice data-page-error data-dismiss-after="5000">
                        <span class="notice-message">${errorMessage}</span>
                        <button class="notice-close" type="button" aria-label="关闭提示" data-notice-close>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>
                    </div>
                </#if>
                <#if loggedOut?? && loggedOut>
                    <div class="notice success is-dismissible" role="status" data-page-notice data-dismiss-after="5000">
                        <span class="notice-message">您已成功退出当前会话。</span>
                        <button class="notice-close" type="button" aria-label="关闭提示" data-notice-close>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>
                    </div>
                </#if>

                <div class="login-methods" data-tab-group>
                    <#assign activeLoginTab = selectedLoginTab!'password'>
                    <div class="switcher" aria-label="登录模式">
                        <button type="button" class="<#if activeLoginTab == 'password'>is-active</#if>" data-tab-target="password-login">账号密码</button>
                        <#if graphicCaptcha?? && graphicCaptcha>
                            <button type="button" class="<#if activeLoginTab == 'graphic'>is-active</#if>" data-tab-target="graphic-login">图形验证码</button>
                        </#if>
                        <#if smsCaptcha?? && smsCaptcha>
                            <button type="button" class="<#if activeLoginTab == 'sms'>is-active</#if>" data-tab-target="sms-login">短信验证码</button>
                        </#if>
                    </div>

                    <form id="password-login" class="auth-form" action="/login" method="post" <#if activeLoginTab != 'password'>hidden</#if> data-tab-panel>
                        <div class="field-group field-with-affix">
                            <label for="username">用户名</label>
                            <span class="field-affix">ID</span>
                            <input id="username" name="username" type="text" autocomplete="username" placeholder="请输入用户名">
                        </div>
                        <div class="field-group field-with-affix">
                            <label for="password">密码</label>
                            <span class="field-affix">PW</span>
                            <input id="password" name="password" type="password" autocomplete="current-password" placeholder="请输入密码">
                        </div>
                        <div class="form-meta-row">
                            <label class="checkbox-row" for="remember-me-password">
                                <input id="remember-me-password" name="remember-me" type="checkbox" value="true">
                                <span>记住我</span>
                            </label>
                            <span class="form-meta-note">安全会话</span>
                        </div>
                        <div class="action-row action-row-stacked">
                            <button class="button primary" type="submit">登录</button>
                            <button class="button secondary" type="button" data-passkey-login>使用 Passkey 登录</button>
                        </div>
                    </form>

                    <#if graphicCaptcha?? && graphicCaptcha>
                    <form id="graphic-login" class="auth-form" action="/login/captcha" method="post" <#if activeLoginTab != 'graphic'>hidden</#if> data-tab-panel>
                        <div class="field-group field-with-affix">
                            <label for="graphic-account">手机号</label>
                            <span class="field-affix">TEL</span>
                            <input id="graphic-account" name="account" type="text" autocomplete="tel" placeholder="请输入手机号">
                        </div>
                        <input type="hidden" name="captchaId" id="graphic-captchaId" value="">
                        <div class="field-group field-with-affix">
                            <label for="graphic-captcha">图形验证码</label>
                            <div class="captcha-image-row">
                                <span class="field-affix">CAP</span>
                                <input id="graphic-captcha" name="captcha" type="text" autocomplete="one-time-code" placeholder="请输入验证码" maxlength="4">
                                <button class="captcha-image-button" type="button" title="刷新验证码" data-captcha-trigger>
                                    <img id="graphicCaptchaImage" class="captcha-image" src="/login/captcha/image" alt="验证码" data-captcha-refresh>
                                </button>
                            </div>
                        </div>
                        <div class="form-meta-row">
                            <label class="checkbox-row" for="remember-me-graphic">
                                <input id="remember-me-graphic" name="remember-me" type="checkbox" value="true">
                                <span>记住我</span>
                            </label>
                            <span class="form-meta-note">图片校验</span>
                        </div>
                        <div class="action-row action-row-stacked">
                            <button class="button primary" type="submit">验证并登录</button>
                        </div>
                    </form>
                    </#if>

                    <#if smsCaptcha?? && smsCaptcha>
                    <form id="sms-login" class="auth-form" action="/login/captcha" method="post" <#if activeLoginTab != 'sms'>hidden</#if> data-tab-panel>
                        <div class="notice success" hidden data-captcha-feedback></div>
                        <div class="field-group field-with-affix">
                            <label for="sms-account">手机号</label>
                            <span class="field-affix">TEL</span>
                            <input id="sms-account" name="account" type="text" autocomplete="tel" placeholder="请输入手机号">
                        </div>
                        <div class="field-group field-inline field-with-affix">
                            <label for="sms-captcha">短信验证码</label>
                            <span class="field-affix">SMS</span>
                            <input id="sms-captcha" name="captcha" type="text" autocomplete="one-time-code" placeholder="请输入验证码">
                            <button class="button secondary inline-button" type="button" data-captcha-send>获取验证码</button>
                        </div>
                        <div class="form-meta-row">
                            <label class="checkbox-row" for="remember-me-sms">
                                <input id="remember-me-sms" name="remember-me" type="checkbox" value="true">
                                <span>记住我</span>
                            </label>
                            <span class="form-meta-note">一次性短信</span>
                        </div>
                        <div class="action-row action-row-stacked">
                            <button class="button primary" type="submit">验证并登录</button>
                        </div>
                    </form>
                    </#if>
                </div>

            <#if federatedProviders?has_content>
                <div class="login-provider-section">
                    <div class="login-divider"><span>其他登录方式</span></div>
                    <#if federatedProviders?size gt 8>
                        <input class="provider-more-check" type="checkbox" id="provider-more-toggle">
                    </#if>
                    <div class="provider-list" data-provider-list>
                        <#list federatedProviders as provider>
                            <#assign providerId = provider.registrationId?lower_case>
                            <#assign providerIcon = (provider.iconKey!providerId)?lower_case>
                            <#assign providerIconUrl = provider.iconUrl!''>
                            <a class="provider-link provider-${providerId} provider-icon-${providerIcon}" href="${provider.authorizationUrl}?mode=${selectedFederatedMode}" aria-label="使用 ${provider.clientName!provider.registrationId} 登录" title="${provider.clientName!provider.registrationId}" <#if provider_index gte 8>data-provider-extra</#if>>
                                <span class="provider-icon" data-provider-icon="${providerIcon}" aria-hidden="true">
                                    <#if providerIconUrl?has_content>
                                        <img class="provider-custom-image" src="${providerIconUrl}" alt="">
                                    <#else>
                                        <svg class="provider-symbol provider-brand-symbol" focusable="false">
                                            <use href="#provider-icon-${providerIcon}"></use>
                                        </svg>
                                        <span class="provider-fallback">${provider.iconText!'ID'}</span>
                                    </#if>
                                </span>
                                <span class="provider-tooltip" role="tooltip">${provider.clientName!provider.registrationId}</span>
                            </a>
                        </#list>
                    </div>
                    <#if federatedProviders?size gt 8>
                        <label class="provider-more" for="provider-more-toggle">
                            <span class="provider-more-open">更多登录方式</span>
                            <span class="provider-more-close">收起登录方式</span>
                        </label>
                    </#if>
                </div>
            </#if>
                <p class="login-card-foot">登录即表示你正在使用 ArmorAuth 统一身份平台。</p>
            </div>
        </section>
    </main>
    <footer class="page-footer login-page-footer">
        <div class="page-footer-brand">
            <strong>ArmorAuth</strong>
            <span>统一身份与授权平台</span>
        </div>
        <div class="page-footer-nav" aria-label="页面信息">
            <span>简体中文</span>
            <span>安全登录</span>
            <span>隐私保护</span>
        </div>
    </footer>
</div>
<script src="/assets/armorauth-ui.js?v=2026052404"></script>
<script>
(() => {
    const passkeyButton = document.querySelector('[data-passkey-login]');
    const usernameInput = document.querySelector('#username');
    if (!passkeyButton || !window.PublicKeyCredential) {
        return;
    }

    const decodeBase64Url = (value) => {
        const base64 = String(value || '').replace(/-/g, '+').replace(/_/g, '/');
        const padded = base64 + '='.repeat((4 - base64.length % 4) % 4);
        const binary = atob(padded);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i += 1) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes.buffer;
    };

    const encodeBase64Url = (buffer) => {
        if (!buffer) {
            return null;
        }
        const bytes = new Uint8Array(buffer);
        let binary = '';
        for (let i = 0; i < bytes.length; i += 1) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
    };

    const postJson = async (url, body) => {
        const response = await fetch(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body || {})
        });
        const payload = await response.json();
        if (!response.ok || payload.code !== 200) {
            throw new Error(payload.message || window.ArmorAuthI18n?.t('passkey.loginFailed') || 'Passkey 登录失败');
        }
        return payload.data;
    };

    passkeyButton.addEventListener('click', async () => {
        try {
            passkeyButton.disabled = true;
            passkeyButton.classList.add('is-loading');
            const username = usernameInput && usernameInput.value ? usernameInput.value.trim() : '';
            const options = await postJson('/login/passkey/options', username ? {username} : {});
            const publicKey = {
                challenge: decodeBase64Url(options.challenge),
                rpId: options.rpId,
                timeout: options.timeoutMillis,
                userVerification: options.userVerification || 'preferred'
            };
            if (options.allowCredentials && options.allowCredentials.length) {
                publicKey.allowCredentials = options.allowCredentials.map((credential) => ({
                    type: credential.type || 'public-key',
                    id: decodeBase64Url(credential.id),
                    transports: credential.transports || undefined
                }));
            }

            const credential = await navigator.credentials.get({publicKey});
            const result = await postJson('/login/passkey/finish', {
                credentialId: credential.id,
                clientDataJSON: encodeBase64Url(credential.response.clientDataJSON),
                authenticatorData: encodeBase64Url(credential.response.authenticatorData),
                signature: encodeBase64Url(credential.response.signature),
                userHandle: encodeBase64Url(credential.response.userHandle)
            });
            window.location.assign(result.redirectUrl || '/');
        } catch (error) {
            window.location.assign('/login?error');
        } finally {
            passkeyButton.disabled = false;
            passkeyButton.classList.remove('is-loading');
        }
    });
})();
</script>
</body>
</html>
