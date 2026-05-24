<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>授权确认 - ArmorAuth</title>
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
                        <span>授权确认</span>
                    </span>
                </div>
                <div class="top-actions">
                    <span class="status-pill">授权中</span>
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
                <span class="eyebrow">授权确认</span>
                <h1>${clientName} 正在请求访问你的账号。</h1>
                <p>当前账号为 ${principalName}，请确认是否开放本次访问。</p>
            </div>

                <div class="meta-grid">
                    <div class="meta-card">
                        <span>应用标识</span>
                        <strong>${clientId}</strong>
                    </div>
                    <div class="meta-card">
                        <span>请求地址</span>
                        <strong>${requestUri}</strong>
                    </div>
                    <#if userCode?? && userCode?has_content>
                        <div class="meta-card">
                            <span>设备代码</span>
                            <strong>${userCode}</strong>
                        </div>
                    </#if>
            </div>
        </section>

        <section class="content-panel">
            <div class="content-column">
                <div class="section-title">
                    <span class="eyebrow">权限范围</span>
                    <h1>确认本次授权内容</h1>
                    <p>选择本次允许开放的权限后继续。</p>
                </div>

                <form id="consent-form" class="auth-form" action="${requestUri}" method="post">
                    <input type="hidden" name="client_id" value="${clientId}">
                    <input type="hidden" name="state" value="${state}">
                    <#if userCode?? && userCode?has_content>
                        <input type="hidden" name="user_code" value="${userCode}">
                    </#if>

                    <div class="scope-list">
                        <#if scopes?has_content>
                            <#list scopes as scope>
                                <label class="scope-item">
                                    <input type="checkbox" name="scope" value="${scope.scope}" checked>
                                    <span class="scope-copy">
                                        <strong>${scope.scope}</strong>
                                        <p>${scope.description!scope.scope}</p>
                                    </span>
                                </label>
                            </#list>
                        <#else>
                            <div class="notice success">当前请求的权限已经全部授权过，本次只需确认继续。</div>
                        </#if>
                    </div>

                    <#if previouslyApprovedScopes?has_content>
                        <div class="section-title">
                            <span class="eyebrow">已授权</span>
                            <p>以下权限已授权。</p>
                        </div>
                        <div class="scope-list">
                            <#list previouslyApprovedScopes as scope>
                                <div class="scope-item">
                                    <input type="checkbox" checked disabled>
                                    <span class="scope-copy">
                                        <strong>${scope.scope}</strong>
                                        <p>${scope.description!scope.scope}</p>
                                    </span>
                                </div>
                            </#list>
                        </div>
                    </#if>

                    <div class="action-row">
                        <button class="button primary" type="submit">同意并继续</button>
                        <button class="button secondary" type="button" id="deny-button">拒绝授权</button>
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
            <span>授权确认</span>
            <span>范围可控</span>
            <span>访问透明</span>
        </div>
    </footer>
</div>
<script src="/assets/armorauth-ui.js?v=20260524"></script>
<script>
    document.getElementById('deny-button').addEventListener('click', () => {
        document.querySelectorAll('#consent-form input[name="scope"]').forEach((input) => {
            input.checked = false;
        });
        document.getElementById('consent-form').submit();
    });
</script>
</body>
</html>
