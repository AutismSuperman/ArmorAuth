<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>欢迎登录 - ArmorAuth</title>
    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
    <link rel="shortcut icon" href="/favicon.svg">
    <link rel="stylesheet" href="/assets/armorauth-ui.css?v=20260429">
    <script src="/assets/iconfont.js"></script>
</head>
<body class="authing-login-page">
<div class="page-shell">
    <main class="login-product-shell">
        <section class="login-visual-panel">
            <!-- 装饰性背景元素 -->
            <div class="login-visual-decor" aria-hidden="true">
                <div class="login-decor-circle login-decor-circle-1"></div>
                <div class="login-decor-circle login-decor-circle-2"></div>
                <div class="login-decor-circle login-decor-circle-3"></div>
                <div class="login-decor-glow"></div>
            </div>

            <div class="login-brand">
                <img class="login-brand-mark" src="/brand/icon.svg" alt="ArmorAuth logo">
                <span>
                    <strong>ArmorAuth</strong>
                    <small>Identity Platform</small>
                </span>
            </div>

            <div class="login-visual-copy">
                <span class="login-kicker">统一认证入口</span>
                <h1>统一登录与授权入口</h1>
                <p>面向应用接入的身份平台登录页，支持账号密码、短信验证码和联合身份源。</p>
            </div>

            <!-- 特性亮点 -->
            <div class="login-features" aria-hidden="true">
                <div class="login-feature-item">
                    <div class="login-feature-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                        </svg>
                    </div>
                    <div class="login-feature-text">
                        <strong>安全可靠</strong>
                        <span>企业级安全保障</span>
                    </div>
                </div>
                <div class="login-feature-item">
                    <div class="login-feature-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"></path>
                        </svg>
                    </div>
                    <div class="login-feature-text">
                        <strong>快速接入</strong>
                        <span>分钟级集成部署</span>
                    </div>
                </div>
                <div class="login-feature-item">
                    <div class="login-feature-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                            <line x1="8" y1="21" x2="16" y2="21"></line>
                            <line x1="12" y1="17" x2="12" y2="21"></line>
                        </svg>
                    </div>
                    <div class="login-feature-text">
                        <strong>多端适配</strong>
                        <span>全平台无缝体验</span>
                    </div>
                </div>
            </div>

            <div class="login-visual-stack" aria-hidden="true">
                <div class="login-preview-card is-primary">
                    <span>SSO</span>
                    <strong>Single Sign-On</strong>
                    <small>统一登录会话</small>
                </div>
                <div class="login-preview-grid">
                    <span>OAuth2</span>
                    <span>OIDC</span>
                    <span>MFA</span>
                    <span>Device</span>
                </div>
            </div>
        </section>

        <section class="login-card-panel">
            <div class="login-card">
                <div class="login-card-toolbar">
                    <div class="login-card-logo">
                        <img src="/brand/icon.svg" alt="ArmorAuth logo">
                        <span>ArmorAuth</span>
                    </div>
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

                <div class="login-card-title">
                    <h1>登录账号</h1>
                    <p>继续访问已授权的应用和服务。</p>
                </div>

                <#if errorMessage??>
                    <div class="notice error">${errorMessage}</div>
                </#if>
                <#if loggedOut?? && loggedOut>
                    <div class="notice success">您已成功退出当前会话。</div>
                </#if>

                <div class="login-methods" data-tab-group>
                    <div class="switcher" aria-label="登录模式">
                        <button type="button" class="is-active" data-tab-target="password-login">账号密码</button>
                        <button type="button" data-tab-target="captcha-login"><#if graphicCaptcha?? && graphicCaptcha>验证码<#else>短信验证码</#if></button>
                    </div>

                    <form id="password-login" class="auth-form" action="/login" method="post" data-tab-panel>
                        <div class="field-group">
                            <label for="username">用户名</label>
                            <input id="username" name="username" type="text" autocomplete="username" placeholder="请输入用户名" value="admin">
                        </div>
                        <div class="field-group">
                            <label for="password">密码</label>
                            <input id="password" name="password" type="password" autocomplete="current-password" placeholder="请输入密码" value="123456">
                        </div>
                        <label class="checkbox-row" for="remember-me-password">
                            <input id="remember-me-password" name="remember-me" type="checkbox" value="true">
                            <span>记住我</span>
                        </label>
                        <div class="action-row action-row-stacked">
                            <button class="button primary" type="submit">登录</button>
                            <button class="button secondary" type="button" data-passkey-login>使用 Passkey 登录</button>
                        </div>
                    </form>

                    <form id="captcha-login" class="auth-form" action="/login/captcha" method="post" hidden data-tab-panel>
                        <div class="notice success" hidden data-captcha-feedback></div>
                        <div class="field-group">
                            <label for="account">手机号</label>
                            <input id="account" name="account" type="text" autocomplete="tel" placeholder="请输入手机号">
                        </div>
                        <#if graphicCaptcha?? && graphicCaptcha>
                        <input type="hidden" name="captchaId" id="captchaId" value="">
                        <div class="field-group">
                            <label for="captcha">验证码</label>
                            <div class="captcha-image-row">
                                <input id="captcha" name="captcha" type="text" autocomplete="one-time-code" placeholder="请输入验证码" maxlength="4">
                                <img id="captchaImage" class="captcha-image" src="/login/captcha/image" alt="验证码" title="点击刷新验证码" data-captcha-refresh style="cursor:pointer;height:40px;border-radius:4px;">
                            </div>
                        </div>
                        <#else>
                        <div class="field-group field-inline">
                            <label for="captcha">验证码</label>
                            <input id="captcha" name="captcha" type="text" autocomplete="one-time-code" placeholder="请输入验证码" value="1234">
                            <button class="button secondary inline-button" type="button" data-captcha-send>获取验证码</button>
                        </div>
                        </#if>
                        <label class="checkbox-row" for="remember-me-captcha">
                            <input id="remember-me-captcha" name="remember-me" type="checkbox" value="true">
                            <span>记住我</span>
                        </label>
                        <div class="action-row action-row-stacked">
                            <button class="button primary" type="submit">验证并登录</button>
                        </div>
                    </form>
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
                            <a class="provider-link provider-${providerId}" href="${provider.authorizationUrl}?mode=${selectedFederatedMode}" aria-label="使用 ${provider.clientName!provider.registrationId} 登录" <#if provider_index gte 8>data-provider-extra</#if>>
                                <span class="provider-icon" aria-hidden="true">
                                    <#if providerId == 'gitee'>
                                        <svg class="provider-symbol" viewBox="0 0 1024 1024" focusable="false">
                                            <use href="#icon-gitee"></use>
                                        </svg>
                                    <#elseif providerId == 'qq'>
                                        <svg class="provider-symbol" viewBox="0 0 1024 1024" focusable="false">
                                            <use href="#icon-QQ1"></use>
                                        </svg>
                                    <#elseif providerId == 'wechat' || providerId == 'weixin'>
                                        <svg class="provider-symbol" viewBox="0 0 1024 1024" focusable="false">
                                            <use href="#icon-weixin"></use>
                                        </svg>
                                    <#elseif providerId == 'github'>
                                        <svg class="provider-symbol" viewBox="0 0 1049 1024" focusable="false">
                                            <use href="#icon-GitHub"></use>
                                        </svg>
                                    <#elseif providerId == 'google'>
                                        <span class="provider-mark provider-mark-google">G</span>
                                    <#elseif providerId == 'facebook'>
                                        <span class="provider-mark provider-mark-facebook">f</span>
                                    <#elseif providerId == 'microsoft'>
                                        <span class="provider-mark provider-mark-microsoft">
                                            <span></span><span></span><span></span><span></span>
                                        </span>
                                    <#elseif providerId == 'gitlab'>
                                        <span class="provider-mark provider-mark-gitlab">GL</span>
                                    <#elseif providerId == 'discord'>
                                        <span class="provider-mark provider-mark-discord">D</span>
                                    <#elseif providerId == 'slack'>
                                        <span class="provider-mark provider-mark-slack">S</span>
                                    <#elseif providerId == 'linkedin'>
                                        <span class="provider-mark provider-mark-linkedin">in</span>
                                    <#elseif providerId == 'apple'>
                                        <span class="provider-mark provider-mark-apple">A</span>
                                    <#elseif providerId == 'weibo'>
                                        <span class="provider-mark provider-mark-weibo">WB</span>
                                    <#elseif providerId == 'baidu'>
                                        <span class="provider-mark provider-mark-baidu">du</span>
                                    <#elseif providerId == 'oschina'>
                                        <span class="provider-mark provider-mark-oschina">OS</span>
                                    <#elseif providerId == 'douyin'>
                                        <span class="provider-mark provider-mark-douyin">dy</span>
                                    <#elseif providerId == 'alipay'>
                                        <span class="provider-mark provider-mark-alipay">支</span>
                                    <#elseif providerId == 'dingtalk'>
                                        <span class="provider-mark provider-mark-dingtalk">钉</span>
                                    <#elseif providerId == 'wecom'>
                                        <span class="provider-mark provider-mark-wecom">企</span>
                                    <#elseif providerId == 'feishu'>
                                        <span class="provider-mark provider-mark-feishu">飞</span>
                                    <#else>
                                        <span class="provider-fallback">${providerId?upper_case?substring(0, 2)}</span>
                                    </#if>
                                </span>
                                <span class="provider-label">${provider.clientName!provider.registrationId}</span>
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
<script src="/assets/armorauth-ui.js?v=20260429"></script>
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
            throw new Error(payload.message || 'Passkey 登录失败');
        }
        return payload.data;
    };

    passkeyButton.addEventListener('click', async () => {
        try {
            passkeyButton.disabled = true;
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
        }
    });
})();
</script>
</body>
</html>
