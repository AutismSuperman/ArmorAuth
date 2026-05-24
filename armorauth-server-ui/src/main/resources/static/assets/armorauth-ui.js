(function () {
    const THEME_KEY = "armorauth-theme";

    /* ===== 主题切换 ===== */
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

    /* ===== 页面级提示收起 ===== */
    const pageError = document.querySelector("[data-page-error]");
    const dismissPageError = () => {
        if (!pageError || pageError.hidden || pageError.classList.contains("is-dismissing")) {
            return;
        }
        pageError.classList.add("is-dismissing");
        window.setTimeout(() => {
            pageError.hidden = true;
        }, 180);
    };

    if (pageError) {
        const url = new URL(window.location.href);
        if (url.searchParams.has("error")) {
            url.searchParams.delete("error");
            window.history.replaceState({}, document.title, url.pathname + url.search + url.hash);
        }
        window.setTimeout(dismissPageError, 5000);
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
                    throw new Error("验证码加载失败");
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
                window.showToast?.(error.message || "验证码加载失败，请重试。", "error");
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
