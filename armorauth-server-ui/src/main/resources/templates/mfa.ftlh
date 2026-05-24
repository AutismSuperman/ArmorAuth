<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>二次验证 - ArmorAuth</title>
    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
    <link rel="shortcut icon" href="/favicon.svg">
    <link rel="stylesheet" href="/assets/armorauth-ui.css?v=20260429">
</head>
<body class="authing-login-page">
<div class="page-shell">
    <main class="login-product-shell">
        <section class="login-visual-panel">
            <div class="login-visual-decor" aria-hidden="true">
                <div class="login-decor-circle login-decor-circle-1"></div>
                <div class="login-decor-circle login-decor-circle-2"></div>
                <div class="login-decor-circle login-decor-circle-3"></div>
            </div>
            <div class="login-brand">
                <img class="login-brand-mark" src="/brand/icon.svg" alt="ArmorAuth logo">
                <span>
                    <strong>ArmorAuth</strong>
                    <small>Identity Platform</small>
                </span>
            </div>
            <div class="login-visual-copy">
                <span class="login-kicker">安全验证</span>
                <h1>多因素认证保护</h1>
                <p>请输入身份验证器应用中显示的验证码，以完成登录。</p>
            </div>
        </section>

        <section class="login-card-panel">
            <div class="login-card">
                <div class="login-card-toolbar">
                    <div class="login-card-logo">
                        <img src="/brand/icon.svg" alt="ArmorAuth logo">
                        <span>ArmorAuth</span>
                    </div>
                </div>

                <div class="login-card-title">
                    <h1>二次验证</h1>
                    <p>请输入身份验证器应用中的 6 位验证码。</p>
                </div>

                <#if errorMessage??>
                    <div class="notice error">${errorMessage}</div>
                </#if>

                <form class="auth-form" action="/login/mfa" method="post">
                    <input type="hidden" name="principal" value="${principal!}">
                    <div class="field-group">
                        <label for="mfaCode">验证码</label>
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
<script>
    const passkeyButton = document.getElementById('passkeyButton');
    const passkeyNotice = document.getElementById('passkeyNotice');

    function showPasskeyError(message) {
        passkeyNotice.textContent = message || 'Passkey 验证失败';
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
            throw new Error(payload.message || '请求失败');
        }
        return payload.data;
    }

    passkeyButton?.addEventListener('click', async () => {
        passkeyNotice.style.display = 'none';
        passkeyButton.disabled = true;
        try {
            if (!window.PublicKeyCredential || !navigator.credentials?.get) {
                throw new Error('当前浏览器不支持 Passkey');
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
                throw new Error('未选择 Passkey');
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
        }
    });
</script>
</body>
</html>
