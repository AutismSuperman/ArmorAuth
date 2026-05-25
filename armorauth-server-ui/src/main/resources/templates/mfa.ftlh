<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>二次验证 - ArmorAuth</title>
    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
    <link rel="shortcut icon" href="/favicon.svg">
    <link rel="stylesheet" href="/assets/armorauth-ui.css?v=2026052408">
</head>
<body class="authing-login-page identity-page" data-page-title-key="page.mfa.title">
<div class="page-shell">
    <main class="login-product-shell">
        <section class="login-visual-panel" data-visual="mfa">
            <div class="login-brand">
                <img class="login-brand-mark" src="/brand/icon.svg" alt="ArmorAuth logo">
                <span>
                    <strong>ArmorAuth</strong>
                    <small>身份云控制台</small>
                </span>
            </div>
            <div class="login-visual-copy">
                <span class="login-kicker">MFA Challenge</span>
                <h1>完成二次验证</h1>
                <p>当前会话需要额外验证，确认后继续进入授权流程。</p>
            </div>
            <div class="identity-menu" aria-label="二次验证状态">
                <div class="identity-menu-item is-active">
                    <span class="identity-menu-icon">01</span>
                    <span>
                        <strong>账号已验证</strong>
                        <small>${principal!'-'}</small>
                    </span>
                </div>
                <div class="identity-menu-item is-active">
                    <span class="identity-menu-icon">02</span>
                    <span>
                        <strong>等待 MFA</strong>
                        <small>TOTP / Recovery / Passkey</small>
                    </span>
                </div>
            </div>
        </section>

        <section class="login-card-panel">
            <div class="login-card">
                <div class="login-card-toolbar">
                    <div class="login-card-logo">
                        <img src="/brand/icon.svg" alt="ArmorAuth logo">
                        <span>安全验证</span>
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
                    <span class="login-card-kicker">Multi-factor</span>
                    <h1>二次验证</h1>
                    <p>使用身份验证器、恢复码或 Passkey 完成验证。</p>
                </div>

                <#if errorMessage??>
                    <div class="notice error">${errorMessage}</div>
                </#if>

                <form class="auth-form" action="/login/mfa" method="post">
                    <input type="hidden" name="principal" value="${principal!}">
                    <div class="field-group field-with-affix">
                        <label for="mfaCode">验证码</label>
                        <span class="field-affix">OTP</span>
                        <input id="mfaCode" name="mfaCode" type="text"
                               autocomplete="one-time-code"
                               inputmode="numeric"
                               pattern="[0-9]{6}"
                               maxlength="6"
                               placeholder="请输入 6 位验证码"
                               autofocus>
                    </div>
                    <div class="action-row action-row-stacked">
                        <button class="button primary" type="submit">验证</button>
                    </div>
                </form>

                <div class="auth-divider"><span>或</span></div>
                <div id="passkeyNotice" class="notice error" style="display:none"></div>
                <div class="action-row action-row-stacked">
                    <button id="passkeyButton" class="button secondary" type="button">使用 Passkey 验证</button>
                </div>

                <p class="login-card-foot">如果无法使用身份验证器，请使用恢复码登录。</p>
            </div>
        </section>
    </main>
</div>
<script src="/assets/armorauth-ui.js?v=2026052404"></script>
<script>
    const passkeyButton = document.getElementById('passkeyButton');
    const passkeyNotice = document.getElementById('passkeyNotice');

    function showPasskeyError(message) {
        passkeyNotice.textContent = message || window.ArmorAuthI18n?.t('passkey.verifyFailed') || 'Passkey 验证失败';
        passkeyNotice.style.display = 'block';
    }

    function base64urlToBuffer(value) {
        const base64 = value.replace(/-/g, '+').replace(/_/g, '/')
            .padEnd(Math.ceil(value.length / 4) * 4, '=');
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes.buffer;
    }

    function bufferToBase64url(buffer) {
        const bytes = new Uint8Array(buffer);
        let binary = '';
        for (let i = 0; i < bytes.byteLength; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
    }

    async function postJson(url, body) {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: body ? JSON.stringify(body) : undefined
        });
        const payload = await response.json();
        if (!response.ok || payload.code !== 200) {
            throw new Error(payload.message || window.ArmorAuthI18n?.t('passkey.requestFailed') || '请求失败');
        }
        return payload.data;
    }

    passkeyButton?.addEventListener('click', async () => {
        passkeyNotice.style.display = 'none';
        passkeyButton.disabled = true;
        passkeyButton.classList.add('is-loading');
        try {
            if (!window.PublicKeyCredential || !navigator.credentials?.get) {
                throw new Error(window.ArmorAuthI18n?.t('passkey.unsupported') || '当前浏览器不支持 Passkey');
            }
            const options = await postJson('/login/passkey/assertion/options');
            const publicKey = {
                challenge: base64urlToBuffer(options.challenge),
                rpId: options.rpId,
                timeout: options.timeoutMillis || 60000,
                userVerification: options.userVerification || 'preferred',
                allowCredentials: (options.allowCredentials || []).map(item => ({
                    type: item.type || 'public-key',
                    id: base64urlToBuffer(item.id),
                    transports: item.transports || undefined
                }))
            };
            const credential = await navigator.credentials.get({ publicKey });
            if (!credential) {
                throw new Error(window.ArmorAuthI18n?.t('passkey.notSelected') || '未选择 Passkey');
            }
            const result = await postJson('/login/passkey/assertion/finish', {
                credentialId: bufferToBase64url(credential.rawId),
                authenticatorData: bufferToBase64url(credential.response.authenticatorData),
                clientDataJSON: bufferToBase64url(credential.response.clientDataJSON),
                signature: bufferToBase64url(credential.response.signature),
                userHandle: credential.response.userHandle
                    ? bufferToBase64url(credential.response.userHandle)
                    : null
            });
            window.location.href = result.redirectUrl || '/';
        } catch (error) {
            showPasskeyError(error.message);
        } finally {
            passkeyButton.disabled = false;
            passkeyButton.classList.remove('is-loading');
        }
    });
</script>
</body>
</html>
