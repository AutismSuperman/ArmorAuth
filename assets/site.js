(() => {
  const themeKey = 'armorauth-theme';
  const themeOrder = ['system', 'light', 'dark'];
  const themeMeta = {
    system: { label: '跟随系统', icon: '系' },
    light: { label: '浅色', icon: '浅' },
    dark: { label: '深色', icon: '深' }
  };

  const getStoredTheme = () => {
    try {
      const stored = localStorage.getItem(themeKey);
      return themeOrder.includes(stored) ? stored : 'system';
    } catch {
      return 'system';
    }
  };

  const closeThemeMenus = (exceptMenu) => {
    document.querySelectorAll('[data-theme-menu]').forEach((menu) => {
      if (menu === exceptMenu) return;
      menu.classList.remove('is-open');
      menu.querySelector('[data-theme-trigger]')?.setAttribute('aria-expanded', 'false');
    });
  };

  const applyTheme = (theme) => {
    if (theme === 'system') {
      document.documentElement.removeAttribute('data-theme');
    } else {
      document.documentElement.setAttribute('data-theme', theme);
    }

    document.querySelectorAll('[data-theme-trigger]').forEach((button) => {
      const meta = themeMeta[theme];
      button.setAttribute('aria-label', `选择主题，当前：${meta.label}`);
      button.setAttribute('title', `选择主题，当前：${meta.label}`);
      button.querySelector('[data-theme-icon]').textContent = meta.icon;
      button.querySelector('[data-theme-label]').textContent = meta.label;
    });

    document.querySelectorAll('[data-theme-option]').forEach((option) => {
      const active = option.dataset.themeOption === theme;
      option.classList.toggle('active', active);
      option.setAttribute('aria-checked', String(active));
    });
  };

  const setTheme = (theme) => {
    try {
      if (theme === 'system') {
        localStorage.removeItem(themeKey);
      } else {
        localStorage.setItem(themeKey, theme);
      }
    } catch {
      // Theme still applies for the current page when storage is unavailable.
    }
    applyTheme(theme);
  };

  applyTheme(getStoredTheme());

  document.querySelectorAll('[data-theme-menu]').forEach((menu) => {
    const trigger = menu.querySelector('[data-theme-trigger]');
    const options = menu.querySelectorAll('[data-theme-option]');

    trigger?.addEventListener('click', (event) => {
      event.stopPropagation();
      const shouldOpen = !menu.classList.contains('is-open');
      closeThemeMenus(menu);
      menu.classList.toggle('is-open', shouldOpen);
      trigger.setAttribute('aria-expanded', String(shouldOpen));
    });

    options.forEach((option) => {
      option.addEventListener('click', () => {
        setTheme(option.dataset.themeOption);
        closeThemeMenus();
        trigger?.focus();
      });
    });
  });

  document.addEventListener('click', (event) => {
    if (!event.target.closest('[data-theme-menu]')) {
      closeThemeMenus();
    }
  });

  const closeMobileNav = () => {
    document.querySelectorAll('.topbar.is-open').forEach((topbar) => {
      topbar.classList.remove('is-open');
      topbar.querySelector('[data-nav-toggle]')?.setAttribute('aria-expanded', 'false');
    });
  };

  document.querySelectorAll('[data-nav-toggle]').forEach((button) => {
    const topbar = button.closest('.topbar');
    button.addEventListener('click', (event) => {
      event.stopPropagation();
      const shouldOpen = !topbar.classList.contains('is-open');
      closeThemeMenus();
      topbar.classList.toggle('is-open', shouldOpen);
      button.setAttribute('aria-expanded', String(shouldOpen));
    });
  });

  document.querySelectorAll('.top-nav a').forEach((link) => {
    link.addEventListener('click', () => {
      closeMobileNav();
    });
  });

  window.addEventListener('resize', () => {
    if (window.innerWidth > 720) {
      closeMobileNav();
    }
  });

  window.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      closeThemeMenus();
      closeMobileNav();
    }
  });

  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    if (getStoredTheme() === 'system') {
      applyTheme('system');
    }
  });

  const sectionLinks = Array.from(document.querySelectorAll('.sidebar a[href^="#"], .toc a[href^="#"]'));
  const sectionIds = Array.from(new Set(sectionLinks.map((link) => decodeURIComponent(link.hash.slice(1))).filter(Boolean)));
  const sections = sectionIds.map((id) => document.getElementById(id)).filter(Boolean);

  const setActive = (id) => {
    sectionLinks.forEach((link) => {
      link.classList.toggle('active', decodeURIComponent(link.hash.slice(1)) === id);
    });
  };

  if (sections.length > 0) {
    const pickActiveSection = () => {
      const offset = Math.max(120, window.innerHeight * 0.24);
      let current = sections[0].id;
      for (const section of sections) {
        if (section.getBoundingClientRect().top <= offset) {
          current = section.id;
        }
      }
      setActive(current);
    };

    pickActiveSection();
    window.addEventListener('scroll', pickActiveSection, { passive: true });
    window.addEventListener('resize', pickActiveSection);
  }

  const closeLightbox = () => {
    document.querySelector('.lightbox')?.remove();
    document.body.style.overflow = '';
  };

  document.querySelectorAll('.image-link').forEach((link) => {
    link.addEventListener('click', (event) => {
      event.preventDefault();
      closeLightbox();
      const img = link.querySelector('img');
      const overlay = document.createElement('div');
      overlay.className = 'lightbox';
      overlay.setAttribute('role', 'dialog');
      overlay.setAttribute('aria-modal', 'true');
      overlay.innerHTML = `<button type="button" aria-label="关闭预览">×</button><img src="${link.href}" alt="${img?.alt || ''}" />`;
      overlay.addEventListener('click', (overlayEvent) => {
        if (overlayEvent.target === overlay || overlayEvent.target.tagName === 'BUTTON') {
          closeLightbox();
        }
      });
      document.body.appendChild(overlay);
      document.body.style.overflow = 'hidden';
    });
  });

  window.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      closeLightbox();
    }
  });
})();
