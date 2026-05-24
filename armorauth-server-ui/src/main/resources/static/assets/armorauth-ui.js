(function () {
    const THEME_KEY = "armorauth-theme";

    const applyTheme = (theme) => {
        document.body.dataset.theme = theme;
        document.documentElement.style.colorScheme = theme === "light" ? "light" : "dark";

        document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
            const text = button.querySelector("[data-theme-text]");
            button.setAttribute("aria-label", theme === "light" ? "切换到深色主题" : "切换到浅色主题");
            if (text) {
                text.textContent = theme === "light" ? "深色" : "浅色";
            }
        });
    };

    const storedTheme = localStorage.getItem(THEME_KEY);
    applyTheme(storedTheme || "light");

    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
        button.addEventListener("click", () => {
            const nextTheme = document.body.dataset.theme === "light" ? "dark" : "light";
            localStorage.setItem(THEME_KEY, nextTheme);
            applyTheme(nextTheme);
        });
    });

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

        const refreshCaptcha = async () => {
            try {
                const response = await window.fetch("/login/captcha/info");
                if (response.ok) {
                    const captchaId = response.headers.get("X-Captcha-Id");
                    if (captchaIdInput && captchaId) {
                        captchaIdInput.value = captchaId;
                    }
                }
                // 添加时间戳防止缓存
                img.src = "/login/captcha/image?t=" + Date.now();
            } catch (e) {
                // 忽略错误，图片会通过 src 自动加载
            }
        };

        // 页面加载时获取 captchaId
        refreshCaptcha();

        img.addEventListener("click", () => {
            refreshCaptcha();
        });
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
                button.textContent = "重新发送(" + remainingSeconds + "s)";
                return;
            }
            button.disabled = false;
            button.textContent = "获取验证码";
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
                setCaptchaFeedback(feedback, "请输入手机号后再获取验证码。", "warning");
                accountInput?.focus();
                return;
            }
            if (countdownTimer) {
                return;
            }

            button.disabled = true;
            button.textContent = "发送中...";
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
                    throw new Error(result.message || "验证码发送失败，请稍后重试。");
                }
                setCaptchaFeedback(feedback, result.message || "验证码已发送。", "success");
                startCountdown();
            } catch (error) {
                setCaptchaFeedback(feedback, error.message || "验证码发送失败，请稍后重试。", "error");
                button.disabled = false;
                button.textContent = "获取验证码";
            }
        });
    });
})();
