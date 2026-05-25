(function () {
    const THEME_KEY = "armorauth-theme";
    const LOCALE_KEY = "armorauth-locale";
    const SUPPORTED_LOCALES = ["zh", "en"];
    const DEFAULT_LOCALE = "zh";
    const I18N = {
        zh: {
            "theme.dark": "深色",
            "theme.light": "浅色",
            "theme.switchToDark": "切换到深色主题",
            "theme.switchToLight": "切换到浅色主题",
            "locale.switchToEnglish": "Switch to English",
            "locale.switchToChinese": "切换到中文",
            "locale.targetEnglish": "EN",
            "locale.targetChinese": "中",
            "欢迎登录 - ArmorAuth": "欢迎登录 - ArmorAuth",
            "二次验证 - ArmorAuth": "二次验证 - ArmorAuth",
            "控制台 - ArmorAuth": "控制台 - ArmorAuth",
            "连接设备 - ArmorAuth": "连接设备 - ArmorAuth",
            "设备已连接 - ArmorAuth": "设备已连接 - ArmorAuth",
            "授权确认 - ArmorAuth": "授权确认 - ArmorAuth",
            "账号确认 - ArmorAuth": "账号确认 - ArmorAuth",
            "page.login.title": "欢迎登录 - ArmorAuth",
            "page.mfa.title": "二次验证 - ArmorAuth",
            "page.home.title": "控制台 - ArmorAuth",
            "page.activate.title": "连接设备 - ArmorAuth",
            "page.activated.title": "设备已连接 - ArmorAuth",
            "page.consent.title": "授权确认 - ArmorAuth",
            "page.federatedConfirm.title": "账号确认 - ArmorAuth",
            "home.welcome": "欢迎，{name}",
            "consent.requestTitle": "{clientName} 正在请求访问你的账号。",
            "consent.accountText": "当前账号为 {principalName}，请确认是否开放本次访问。",
            "captcha.imageLoadFailed": "验证码加载失败",
            "captcha.imageLoadFailedRetry": "验证码加载失败，请重试。",
            "captcha.enterPhone": "请输入手机号后再获取验证码。",
            "captcha.resend": "重新发送({seconds}s)",
            "captcha.get": "获取验证码",
            "captcha.sending": "发送中...",
            "captcha.sendFailed": "验证码发送失败，请稍后重试。",
            "captcha.sent": "验证码已发送。",
            "passkey.loginFailed": "Passkey 登录失败",
            "passkey.verifyFailed": "Passkey 验证失败",
            "passkey.requestFailed": "请求失败",
            "passkey.unsupported": "当前浏览器不支持 Passkey",
            "passkey.notSelected": "未选择 Passkey",
            "关闭提示": "关闭提示",
            "页面信息": "页面信息",
            "身份能力": "身份能力",
            "刷新验证码": "刷新验证码",
            "身份云控制台": "身份云控制台",
            "用户池 / 默认租户": "用户池 / 默认租户",
            "进入统一身份工作台": "进入统一身份工作台",
            "集中处理账号认证、应用授权、MFA 与联合身份登录。": "集中处理账号认证、应用授权、MFA 与联合身份登录。",
            "认证体验": "认证体验",
            "Password / Captcha / Passkey": "Password / Captcha / Passkey",
            "授权协议": "授权协议",
            "OAuth2 / OIDC / Device Flow": "OAuth2 / OIDC / Device Flow",
            "身份源": "身份源",
            "Social / SAML / Enterprise IdP": "Social / SAML / Enterprise IdP",
            "当前入口": "当前入口",
            "Hosted Login": "Hosted Login",
            "统一登录页": "统一登录页",
            "OIDC": "OIDC",
            "SSO": "SSO",
            "MFA": "MFA",
            "RBAC": "RBAC",
            "默认用户池": "默认用户池",
            "Sign in": "Sign in",
            "登录 ArmorAuth": "登录 ArmorAuth",
            "选择一种认证方式继续访问应用。": "选择一种认证方式继续访问应用。",
            "您已成功退出当前会话。": "您已成功退出当前会话。",
            "登录模式": "登录模式",
            "账号密码": "账号密码",
            "图形验证码": "图形验证码",
            "短信验证码": "短信验证码",
            "用户名": "用户名",
            "请输入用户名": "请输入用户名",
            "密码": "密码",
            "请输入密码": "请输入密码",
            "记住我": "记住我",
            "安全会话": "安全会话",
            "登录": "登录",
            "使用 Passkey 登录": "使用 Passkey 登录",
            "获取验证码": "获取验证码",
            "手机号": "手机号",
            "请输入手机号": "请输入手机号",
            "请输入验证码": "请输入验证码",
            "验证码": "验证码",
            "图片校验": "图片校验",
            "验证并登录": "验证并登录",
            "一次性短信": "一次性短信",
            "其他登录方式": "其他登录方式",
            "更多登录方式": "更多登录方式",
            "收起登录方式": "收起登录方式",
            "登录即表示你正在使用 ArmorAuth 统一身份平台。": "登录即表示你正在使用 ArmorAuth 统一身份平台。",
            "简体中文": "简体中文",
            "安全登录": "安全登录",
            "隐私保护": "隐私保护",
            "统一身份与授权平台": "统一身份与授权平台",
            "MFA Challenge": "MFA Challenge",
            "完成二次验证": "完成二次验证",
            "当前会话需要额外验证，确认后继续进入授权流程。": "当前会话需要额外验证，确认后继续进入授权流程。",
            "二次验证状态": "二次验证状态",
            "账号已验证": "账号已验证",
            "等待 MFA": "等待 MFA",
            "TOTP / Recovery / Passkey": "TOTP / Recovery / Passkey",
            "安全验证": "安全验证",
            "Multi-factor": "Multi-factor",
            "二次验证": "二次验证",
            "使用身份验证器、恢复码或 Passkey 完成验证。": "使用身份验证器、恢复码或 Passkey 完成验证。",
            "请输入 6 位验证码": "请输入 6 位验证码",
            "验证": "验证",
            "或": "或",
            "使用 Passkey 验证": "使用 Passkey 验证",
            "如果无法使用身份验证器，请使用恢复码登录。": "如果无法使用身份验证器，请使用恢复码登录。",
            "控制台": "控制台",
            "退出登录": "退出登录",
            "首页": "首页",
            "你的账号服务已就绪。": "你的账号服务已就绪。",
            "快捷入口": "快捷入口",
            "1. 发起新的授权请求": "1. 发起新的授权请求",
            "2. 处理设备激活": "2. 处理设备激活",
            "3. 进入联邦登录流程": "3. 进入联邦登录流程",
            "账号服务": "账号服务",
            "连接设备": "连接设备",
            "设备激活": "设备激活",
            "设备登录": "设备登录",
            "输入激活码，继续完成设备登录。": "输入激活码，继续完成设备登录。",
            "适用于电视、命令行和其他需要确认登录的设备场景。": "适用于电视、命令行和其他需要确认登录的设备场景。",
            "验证地址": "验证地址",
            "输入示例": "输入示例",
            "例如 ABCD-EFGH": "例如 ABCD-EFGH",
            "输入激活码": "输入激活码",
            "验证成功后，设备将继续完成登录。": "验证成功后，设备将继续完成登录。",
            "激活码": "激活码",
            "例如：ABCD-EFGH": "例如：ABCD-EFGH",
            "继续验证": "继续验证",
            "返回登录": "返回登录",
            "验证码确认": "验证码确认",
            "安全连接": "安全连接",
            "设备已连接": "设备已连接",
            "已完成": "已完成",
            "现在可以返回设备继续操作。": "现在可以返回设备继续操作。",
            "如果设备没有自动刷新，请返回设备后重试。": "如果设备没有自动刷新，请返回设备后重试。",
            "已完成确认": "已完成确认",
            "本次激活已经生效，无需重复提交。": "本次激活已经生效，无需重复提交。",
            "设备继续登录": "设备继续登录",
            "设备获取结果后会继续完成登录。": "设备获取结果后会继续完成登录。",
            "需要手动刷新时": "需要手动刷新时",
            "返回设备页面重新进入即可。": "返回设备页面重新进入即可。",
            "返回首页": "返回首页",
            "返回登录页": "返回登录页",
            "继续登录": "继续登录",
            "会话已确认": "会话已确认",
            "授权确认": "授权确认",
            "授权中": "授权中",
            "应用标识": "应用标识",
            "请求地址": "请求地址",
            "设备代码": "设备代码",
            "权限范围": "权限范围",
            "确认本次授权内容": "确认本次授权内容",
            "选择本次允许开放的权限后继续。": "选择本次允许开放的权限后继续。",
            "当前请求的权限已经全部授权过，本次只需确认继续。": "当前请求的权限已经全部授权过，本次只需确认继续。",
            "已授权": "已授权",
            "以下权限已授权。": "以下权限已授权。",
            "同意并继续": "同意并继续",
            "拒绝授权": "拒绝授权",
            "范围可控": "范围可控",
            "访问透明": "访问透明",
            "账号确认": "账号确认",
            "第三方账号确认": "第三方账号确认",
            "第三方登录": "第三方登录",
            "确认第三方账号对应的本地身份。": "确认第三方账号对应的本地身份。",
            "确认后即可继续进入账号服务。": "确认后即可继续进入账号服务。",
            "待确认账号": "待确认账号",
            "未提供": "未提供",
            "确认当前第三方账号与本地身份的绑定关系后继续。": "确认当前第三方账号与本地身份的绑定关系后继续。",
            "第三方": "第三方",
            "本地账号": "本地账号",
            "第三方账号摘要": "第三方账号摘要",
            "第三方头像": "第三方头像",
            "登录来源": "登录来源",
            "第三方昵称": "第三方昵称",
            "第三方标识": "第三方标识",
            "确认账号": "确认账号",
            "创建新账号，或绑定到已有账号后继续。": "创建新账号，或绑定到已有账号后继续。",
            "完成一次确认后，后续可直接使用该第三方账号登录。": "完成一次确认后，后续可直接使用该第三方账号登录。",
            "确认方式": "确认方式",
            "创建新账号": "创建新账号",
            "绑定已有账号": "绑定已有账号",
            "请输入本地用户名": "请输入本地用户名",
            "请输入至少 6 位密码": "请输入至少 6 位密码",
            "确认密码": "确认密码",
            "请再次输入密码": "请再次输入密码",
            "创建并完成登录": "创建并完成登录",
            "已有用户名": "已有用户名",
            "请输入已有本地用户名": "请输入已有本地用户名",
            "请输入已有账号密码": "请输入已有账号密码",
            "绑定并完成登录": "绑定并完成登录",
            "ArmorAuth 第三方接入": "ArmorAuth 第三方接入",
            "身份确认": "身份确认",
            "绑定安全": "绑定安全"
        },
        en: {
            "theme.dark": "Dark",
            "theme.light": "Light",
            "theme.switchToDark": "Switch to dark theme",
            "theme.switchToLight": "Switch to light theme",
            "locale.switchToEnglish": "Switch to English",
            "locale.switchToChinese": "Switch to Chinese",
            "locale.targetEnglish": "EN",
            "locale.targetChinese": "中",
            "欢迎登录 - ArmorAuth": "Welcome - ArmorAuth",
            "二次验证 - ArmorAuth": "Multi-factor Verification - ArmorAuth",
            "控制台 - ArmorAuth": "Console - ArmorAuth",
            "连接设备 - ArmorAuth": "Connect Device - ArmorAuth",
            "设备已连接 - ArmorAuth": "Device Connected - ArmorAuth",
            "授权确认 - ArmorAuth": "Authorization Consent - ArmorAuth",
            "账号确认 - ArmorAuth": "Account Confirmation - ArmorAuth",
            "page.login.title": "Welcome - ArmorAuth",
            "page.mfa.title": "Multi-factor Verification - ArmorAuth",
            "page.home.title": "Console - ArmorAuth",
            "page.activate.title": "Connect Device - ArmorAuth",
            "page.activated.title": "Device Connected - ArmorAuth",
            "page.consent.title": "Authorization Consent - ArmorAuth",
            "page.federatedConfirm.title": "Account Confirmation - ArmorAuth",
            "home.welcome": "Welcome, {name}",
            "consent.requestTitle": "{clientName} is requesting access to your account.",
            "consent.accountText": "Current account: {principalName}. Confirm whether to allow this access.",
            "captcha.imageLoadFailed": "Captcha failed to load",
            "captcha.imageLoadFailedRetry": "Captcha failed to load. Please try again.",
            "captcha.enterPhone": "Enter a phone number before requesting a code.",
            "captcha.resend": "Resend ({seconds}s)",
            "captcha.get": "Get code",
            "captcha.sending": "Sending...",
            "captcha.sendFailed": "Failed to send the code. Please try again later.",
            "captcha.sent": "Code sent.",
            "passkey.loginFailed": "Passkey login failed",
            "passkey.verifyFailed": "Passkey verification failed",
            "passkey.requestFailed": "Request failed",
            "passkey.unsupported": "This browser does not support Passkey",
            "passkey.notSelected": "No Passkey selected",
            "关闭提示": "Close notice",
            "页面信息": "Page information",
            "身份能力": "Identity capabilities",
            "刷新验证码": "Refresh captcha",
            "身份云控制台": "Identity Cloud Console",
            "用户池 / 默认租户": "User Pool / Default Tenant",
            "进入统一身份工作台": "Enter the Unified Identity Workspace",
            "集中处理账号认证、应用授权、MFA 与联合身份登录。": "Handle account authentication, app authorization, MFA, and federated sign-in in one place.",
            "认证体验": "Authentication",
            "Password / Captcha / Passkey": "Password / Captcha / Passkey",
            "授权协议": "Authorization",
            "OAuth2 / OIDC / Device Flow": "OAuth2 / OIDC / Device Flow",
            "身份源": "Identity Sources",
            "Social / SAML / Enterprise IdP": "Social / SAML / Enterprise IdP",
            "当前入口": "Current Entry",
            "Hosted Login": "Hosted Login",
            "统一登录页": "Unified sign-in page",
            "OIDC": "OIDC",
            "SSO": "SSO",
            "MFA": "MFA",
            "RBAC": "RBAC",
            "默认用户池": "Default User Pool",
            "Sign in": "Sign in",
            "登录 ArmorAuth": "Sign in to ArmorAuth",
            "选择一种认证方式继续访问应用。": "Choose an authentication method to continue.",
            "您已成功退出当前会话。": "You have successfully signed out of the current session.",
            "登录模式": "Sign-in method",
            "账号密码": "Password",
            "图形验证码": "Image Code",
            "短信验证码": "SMS Code",
            "用户名": "Username",
            "请输入用户名": "Enter username",
            "密码": "Password",
            "请输入密码": "Enter password",
            "记住我": "Remember me",
            "安全会话": "Secure session",
            "登录": "Sign in",
            "使用 Passkey 登录": "Sign in with Passkey",
            "获取验证码": "Get code",
            "手机号": "Phone number",
            "请输入手机号": "Enter phone number",
            "请输入验证码": "Enter code",
            "验证码": "Code",
            "图片校验": "Image verification",
            "验证并登录": "Verify and sign in",
            "一次性短信": "One-time SMS",
            "其他登录方式": "Other sign-in methods",
            "更多登录方式": "More sign-in methods",
            "收起登录方式": "Collapse sign-in methods",
            "登录即表示你正在使用 ArmorAuth 统一身份平台。": "By signing in, you are using the ArmorAuth unified identity platform.",
            "简体中文": "English",
            "安全登录": "Secure sign-in",
            "隐私保护": "Privacy protection",
            "统一身份与授权平台": "Unified identity and authorization platform",
            "MFA Challenge": "MFA Challenge",
            "完成二次验证": "Complete multi-factor verification",
            "当前会话需要额外验证，确认后继续进入授权流程。": "This session requires an additional verification before continuing.",
            "二次验证状态": "MFA status",
            "账号已验证": "Account verified",
            "等待 MFA": "Awaiting MFA",
            "TOTP / Recovery / Passkey": "TOTP / Recovery / Passkey",
            "安全验证": "Security verification",
            "Multi-factor": "Multi-factor",
            "二次验证": "Multi-factor verification",
            "使用身份验证器、恢复码或 Passkey 完成验证。": "Use an authenticator, recovery code, or Passkey to finish verification.",
            "请输入 6 位验证码": "Enter the 6-digit code",
            "验证": "Verify",
            "或": "or",
            "使用 Passkey 验证": "Verify with Passkey",
            "如果无法使用身份验证器，请使用恢复码登录。": "Use a recovery code if your authenticator is unavailable.",
            "控制台": "Console",
            "退出登录": "Sign out",
            "首页": "Home",
            "你的账号服务已就绪。": "Your account services are ready.",
            "快捷入口": "Quick Actions",
            "1. 发起新的授权请求": "1. Start a new authorization request",
            "2. 处理设备激活": "2. Handle device activation",
            "3. 进入联邦登录流程": "3. Continue a federated sign-in flow",
            "账号服务": "Account services",
            "连接设备": "Connect Device",
            "设备激活": "Device Activation",
            "设备登录": "Device sign-in",
            "输入激活码，继续完成设备登录。": "Enter the activation code to continue device sign-in.",
            "适用于电视、命令行和其他需要确认登录的设备场景。": "For TVs, CLIs, and other devices that need sign-in confirmation.",
            "验证地址": "Verification URL",
            "输入示例": "Example",
            "例如 ABCD-EFGH": "For example ABCD-EFGH",
            "输入激活码": "Enter activation code",
            "验证成功后，设备将继续完成登录。": "After verification, the device will continue signing in.",
            "激活码": "Activation code",
            "例如：ABCD-EFGH": "Example: ABCD-EFGH",
            "继续验证": "Continue",
            "返回登录": "Back to sign-in",
            "验证码确认": "Code confirmation",
            "安全连接": "Secure connection",
            "设备已连接": "Device Connected",
            "已完成": "Completed",
            "现在可以返回设备继续操作。": "You can now return to your device.",
            "如果设备没有自动刷新，请返回设备后重试。": "If the device does not refresh automatically, return to the device and try again.",
            "已完成确认": "Confirmation complete",
            "本次激活已经生效，无需重复提交。": "This activation is complete. No need to submit again.",
            "设备继续登录": "Device continues sign-in",
            "设备获取结果后会继续完成登录。": "The device will finish sign-in after receiving the result.",
            "需要手动刷新时": "Manual refresh",
            "返回设备页面重新进入即可。": "Return to the device page and re-enter if needed.",
            "返回首页": "Back to home",
            "返回登录页": "Back to sign-in",
            "继续登录": "Continue sign-in",
            "会话已确认": "Session confirmed",
            "授权确认": "Authorization Consent",
            "授权中": "Authorizing",
            "应用标识": "Client ID",
            "请求地址": "Request URI",
            "设备代码": "Device code",
            "权限范围": "Scopes",
            "确认本次授权内容": "Confirm this authorization",
            "选择本次允许开放的权限后继续。": "Select the permissions to grant for this request.",
            "当前请求的权限已经全部授权过，本次只需确认继续。": "All requested permissions were previously approved. Confirm to continue.",
            "已授权": "Already approved",
            "以下权限已授权。": "The following permissions are already approved.",
            "同意并继续": "Allow and continue",
            "拒绝授权": "Deny",
            "范围可控": "Controlled scopes",
            "访问透明": "Transparent access",
            "账号确认": "Account Confirmation",
            "第三方账号确认": "Federated Account Confirmation",
            "第三方登录": "Federated sign-in",
            "确认第三方账号对应的本地身份。": "Confirm the local identity for this federated account.",
            "确认后即可继续进入账号服务。": "After confirmation, you can continue to account services.",
            "待确认账号": "Account to confirm",
            "未提供": "Not provided",
            "确认当前第三方账号与本地身份的绑定关系后继续。": "Confirm how this federated account maps to a local identity.",
            "第三方": "Provider",
            "本地账号": "Local account",
            "第三方账号摘要": "Federated account summary",
            "第三方头像": "Provider avatar",
            "登录来源": "Sign-in source",
            "第三方昵称": "Provider nickname",
            "第三方标识": "Provider identifier",
            "确认账号": "Confirm account",
            "创建新账号，或绑定到已有账号后继续。": "Create a new account or bind an existing one to continue.",
            "完成一次确认后，后续可直接使用该第三方账号登录。": "After confirmation, you can use this federated account directly next time.",
            "确认方式": "Confirmation method",
            "创建新账号": "Create account",
            "绑定已有账号": "Bind existing account",
            "请输入本地用户名": "Enter local username",
            "请输入至少 6 位密码": "Enter a password with at least 6 characters",
            "确认密码": "Confirm password",
            "请再次输入密码": "Enter the password again",
            "创建并完成登录": "Create and sign in",
            "已有用户名": "Existing username",
            "请输入已有本地用户名": "Enter existing local username",
            "请输入已有账号密码": "Enter existing account password",
            "绑定并完成登录": "Bind and sign in",
            "ArmorAuth 第三方接入": "ArmorAuth federated access",
            "身份确认": "Identity confirmation",
            "绑定安全": "Secure binding"
        }
    };

    let currentLocale = (() => {
        const storedLocale = localStorage.getItem(LOCALE_KEY);
        if (SUPPORTED_LOCALES.includes(storedLocale)) {
            return storedLocale;
        }
        return DEFAULT_LOCALE;
    })();

    const getTemplateValues = (element) => {
        const values = {};
        Object.keys(element.dataset).forEach((key) => {
            if (!key.startsWith("i18nValue")) {
                return;
            }
            const rawName = key.slice("i18nValue".length);
            const tokenName = rawName.charAt(0).toLowerCase() + rawName.slice(1);
            values[tokenName] = element.dataset[key];
        });
        return values;
    };

    const t = (key, values) => {
        let text = I18N[currentLocale]?.[key] || I18N[DEFAULT_LOCALE]?.[key] || key;
        if (values) {
            Object.entries(values).forEach(([name, value]) => {
                text = text.replaceAll("{" + name + "}", value ?? "");
            });
        }
        return text;
    };

    const hasTranslation = (key) => {
        return Boolean(I18N[DEFAULT_LOCALE]?.[key] || I18N.en?.[key]);
    };

    const translateTextNodes = () => {
        const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
            acceptNode(node) {
                const parent = node.parentElement;
                if (!parent || ["SCRIPT", "STYLE", "TEMPLATE"].includes(parent.tagName)) {
                    return NodeFilter.FILTER_REJECT;
                }
                return node.nodeValue.trim() ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
            }
        });
        const nodes = [];
        while (walker.nextNode()) {
            nodes.push(walker.currentNode);
        }
        nodes.forEach((node) => {
            let key = node.__armorauthI18nKey;
            if (!key) {
                key = node.nodeValue.trim();
                if (!hasTranslation(key)) {
                    return;
                }
                node.__armorauthI18nKey = key;
            }
            const leading = node.nodeValue.match(/^\s*/)?.[0] || "";
            const trailing = node.nodeValue.match(/\s*$/)?.[0] || "";
            node.nodeValue = leading + t(key) + trailing;
        });
    };

    const translateAttributes = () => {
        const attrs = ["aria-label", "placeholder", "title", "alt"];
        const translateAttributeValue = (key) => {
            const federatedLogin = key.match(/^使用 (.+) 登录$/);
            if (federatedLogin) {
                return currentLocale === "en" ? "Sign in with " + federatedLogin[1] : key;
            }
            return t(key);
        };
        document.querySelectorAll("*").forEach((element) => {
            element.__armorauthI18nAttrs = element.__armorauthI18nAttrs || {};
            attrs.forEach((attr) => {
                if (!element.hasAttribute(attr)) {
                    return;
                }
                let key = element.__armorauthI18nAttrs[attr];
                if (!key) {
                    key = element.getAttribute(attr).trim();
                    if (!hasTranslation(key) && !/^使用 .+ 登录$/.test(key)) {
                        return;
                    }
                    element.__armorauthI18nAttrs[attr] = key;
                }
                element.setAttribute(attr, translateAttributeValue(key));
            });
        });
        document.querySelectorAll("[data-i18n-attr]").forEach((element) => {
            element.dataset.i18nAttr.split(",").forEach((entry) => {
                const [attr, key] = entry.split(":").map((item) => item.trim());
                if (attr && key) {
                    element.setAttribute(attr, t(key));
                }
            });
        });
    };

    const updateLocaleControls = () => {
        const nextLocale = currentLocale === "zh" ? "en" : "zh";
        document.querySelectorAll("[data-locale-toggle]").forEach((button) => {
            button.setAttribute("aria-label", t(nextLocale === "en" ? "locale.switchToEnglish" : "locale.switchToChinese"));
            const text = button.querySelector("[data-locale-text]");
            if (text) {
                text.textContent = t(nextLocale === "en" ? "locale.targetEnglish" : "locale.targetChinese");
            }
        });
    };

    const applyLocale = (locale) => {
        currentLocale = SUPPORTED_LOCALES.includes(locale) ? locale : DEFAULT_LOCALE;
        localStorage.setItem(LOCALE_KEY, currentLocale);
        document.documentElement.lang = currentLocale === "en" ? "en" : "zh-CN";
        document.body.dataset.locale = currentLocale;
        document.querySelectorAll("[data-i18n]").forEach((element) => {
            element.textContent = t(element.dataset.i18n);
        });
        document.querySelectorAll("[data-i18n-template]").forEach((element) => {
            element.textContent = t(element.dataset.i18nTemplate, getTemplateValues(element));
        });
        translateTextNodes();
        translateAttributes();
        updateLocaleControls();
        applyTheme(document.body.dataset.theme || localStorage.getItem(THEME_KEY) || "light");
        const titleKey = document.body.dataset.pageTitleKey;
        if (titleKey) {
            document.title = t(titleKey);
        } else {
            const originalTitle = document.title.trim();
            if (!document.__armorauthTitleKey && hasTranslation(originalTitle)) {
                document.__armorauthTitleKey = originalTitle;
            }
            if (document.__armorauthTitleKey) {
                document.title = t(document.__armorauthTitleKey);
            }
        }
    };

    window.ArmorAuthI18n = {
        t,
        apply: applyLocale,
        locale: () => currentLocale
    };

    /* ===== 主题切换 ===== */
    const applyTheme = (theme) => {
        document.body.dataset.theme = theme;
        document.documentElement.style.colorScheme = theme === "light" ? "light" : "dark";

        document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
            const text = button.querySelector("[data-theme-text]");
            button.setAttribute("aria-label", theme === "light" ? t("theme.switchToDark") : t("theme.switchToLight"));
            if (text) {
                text.textContent = theme === "light" ? t("theme.dark") : t("theme.light");
            }
        });
    };

    const storedTheme = localStorage.getItem(THEME_KEY);
    applyTheme(storedTheme || "light");
    applyLocale(currentLocale);

    document.querySelectorAll("[data-locale-toggle]").forEach((button) => {
        button.addEventListener("click", () => {
            applyLocale(currentLocale === "zh" ? "en" : "zh");
        });
    });

    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
        button.addEventListener("click", () => {
            const nextTheme = document.body.dataset.theme === "light" ? "dark" : "light";
            localStorage.setItem(THEME_KEY, nextTheme);
            applyTheme(nextTheme);
        });
    });

    /* ===== 页面级提示收起 ===== */
    const dismissNotice = (notice) => {
        if (!notice || notice.hidden || notice.classList.contains("is-dismissing")) {
            return;
        }
        notice.classList.add("is-dismissing");
        window.setTimeout(() => {
            notice.hidden = true;
        }, 180);
    };

    document.querySelectorAll("[data-page-notice]").forEach((notice) => {
        const closeButton = notice.querySelector("[data-notice-close]");
        const dismissAfter = Number.parseInt(notice.dataset.dismissAfter || "", 10);

        if (closeButton) {
            closeButton.addEventListener("click", () => dismissNotice(notice));
        }
        if (Number.isFinite(dismissAfter) && dismissAfter > 0) {
            window.setTimeout(() => dismissNotice(notice), dismissAfter);
        }
    });

    const pageError = document.querySelector("[data-page-error]");
    if (pageError) {
        const dismissPageError = () => dismissNotice(pageError);
        const url = new URL(window.location.href);
        if (url.searchParams.has("error")) {
            url.searchParams.delete("error");
            window.history.replaceState({}, document.title, url.pathname + url.search + url.hash);
        }
        document.querySelectorAll("[data-tab-target], .auth-form input, .provider-link").forEach((element) => {
            element.addEventListener("click", dismissPageError, { once: true });
            element.addEventListener("input", dismissPageError, { once: true });
            element.addEventListener("focus", dismissPageError, { once: true });
        });
    }

    /* ===== Tab 切换 ===== */
    document.querySelectorAll("[data-tab-group]").forEach((group) => {
        const buttons = group.querySelectorAll("[data-tab-target]");
        buttons.forEach((button) => {
            button.addEventListener("click", () => {
                const targetId = button.dataset.tabTarget;
                buttons.forEach((item) => item.classList.remove("is-active"));
                group.querySelectorAll("[data-tab-panel]").forEach((panel) => {
                    panel.hidden = panel.id !== targetId;
                });
                button.classList.add("is-active");
            });
        });
    });

    /* ===== Toast 通知 ===== */
    let toastContainer = null;

    const getOrCreateToastContainer = () => {
        if (!toastContainer) {
            toastContainer = document.createElement("div");
            toastContainer.className = "toast-container";
            document.body.appendChild(toastContainer);
        }
        return toastContainer;
    };

    window.showToast = (message, type, duration) => {
        type = type || "info";
        duration = duration || 3500;
        const container = getOrCreateToastContainer();
        const toast = document.createElement("div");
        toast.className = "toast toast-" + type;
        toast.textContent = message;
        container.appendChild(toast);

        const remove = () => {
            toast.classList.add("is-out");
            toast.addEventListener("animationend", () => {
                toast.remove();
                if (container.children.length === 0) {
                    container.remove();
                    toastContainer = null;
                }
            });
        };

        setTimeout(remove, duration);
        toast.addEventListener("click", remove);
    };

    /* ===== 表单提交 loading 状态 ===== */
    document.querySelectorAll("form").forEach((form) => {
        form.addEventListener("submit", () => {
            const primaryBtn = form.querySelector(".button.primary, button[type='submit']");
            if (primaryBtn && !primaryBtn.disabled) {
                primaryBtn.classList.add("is-loading");
                primaryBtn.disabled = true;
            }
        });
    });

    window.addEventListener("pageshow", () => {
        document.querySelectorAll(".button.is-loading").forEach((button) => {
            button.classList.remove("is-loading");
            button.disabled = false;
        });
    });

    /* ===== 验证码相关 ===== */
    const setCaptchaFeedback = (container, message, type) => {
        if (!container) {
            return;
        }
        container.hidden = !message;
        container.textContent = message || "";
        container.classList.remove("success", "error", "warning");
        if (type) {
            container.classList.add(type);
        }
    };

    // 图形验证码刷新
    document.querySelectorAll("[data-captcha-refresh]").forEach((img) => {
        const form = img.closest("form");
        const captchaIdInput = form?.querySelector("input[name='captchaId']");
        let captchaObjectUrl = null;

        const refreshCaptcha = async () => {
            try {
                const response = await window.fetch("/login/captcha/image?t=" + Date.now(), {
                    headers: {"Accept": "image/png"},
                    cache: "no-store"
                });
                if (!response.ok) {
                    throw new Error(t("captcha.imageLoadFailed"));
                }
                const captchaId = response.headers.get("X-Captcha-Id");
                const imageBlob = await response.blob();
                if (captchaIdInput) {
                    captchaIdInput.value = captchaId || "";
                }
                if (captchaObjectUrl) {
                    URL.revokeObjectURL(captchaObjectUrl);
                }
                captchaObjectUrl = URL.createObjectURL(imageBlob);
                img.src = captchaObjectUrl;
                img.dataset.ready = "true";
            } catch (error) {
                if (captchaIdInput) {
                    captchaIdInput.value = "";
                }
                window.showToast?.(error.message || t("captcha.imageLoadFailedRetry"), "error");
            }
        };

        refreshCaptcha();
        const trigger = img.closest("[data-captcha-trigger]") || img;
        trigger.addEventListener("click", refreshCaptcha);
    });

    // 短信验证码发送
    document.querySelectorAll("[data-captcha-send]").forEach((button) => {
        const form = button.closest("form");
        const accountInput = form?.querySelector("input[name='account']");
        const feedback = form?.querySelector("[data-captcha-feedback]");
        let countdownTimer = null;
        let remainingSeconds = 0;

        const updateButton = () => {
            if (remainingSeconds > 0) {
                button.disabled = true;
                button.textContent = t("captcha.resend", {seconds: remainingSeconds});
                return;
            }
            button.disabled = false;
            button.textContent = t("captcha.get");
        };

        const startCountdown = () => {
            remainingSeconds = 60;
            updateButton();
            countdownTimer = window.setInterval(() => {
                remainingSeconds -= 1;
                if (remainingSeconds <= 0) {
                    window.clearInterval(countdownTimer);
                    countdownTimer = null;
                    remainingSeconds = 0;
                }
                updateButton();
            }, 1000);
        };

        updateButton();

        button.addEventListener("click", async () => {
            const account = accountInput?.value.trim() || "";
            if (!account) {
                setCaptchaFeedback(feedback, t("captcha.enterPhone"), "warning");
                accountInput?.focus();
                return;
            }
            if (countdownTimer) {
                return;
            }

            button.disabled = true;
            button.textContent = t("captcha.sending");
            setCaptchaFeedback(feedback, "", "");

            try {
                const body = new URLSearchParams({ account });
                const response = await window.fetch("/login/captcha/send", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
                    },
                    body
                });
                const result = await response.json();
                if (!response.ok) {
                    throw new Error(result.message || t("captcha.sendFailed"));
                }
                setCaptchaFeedback(feedback, result.message || t("captcha.sent"), "success");
                startCountdown();
            } catch (error) {
                setCaptchaFeedback(feedback, error.message || t("captcha.sendFailed"), "error");
                button.disabled = false;
                button.textContent = t("captcha.get");
            }
        });
    });
})();
