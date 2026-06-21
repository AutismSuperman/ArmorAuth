(() => {
  const themeKey = 'armorauth-theme';
  const themeOrder = ['system', 'dark', 'light'];
  const themeMeta = {
    system: { label: '跟随系统', icon: '系' },
    dark: { label: '深色', icon: '深' },
    light: { label: '浅色', icon: '浅' }
  };

  const getStoredTheme = () => {
    const stored = localStorage.getItem(themeKey);
    return themeOrder.includes(stored) ? stored : 'system';
  };

  const applyTheme = (theme) => {
    if (theme === 'system') {
      document.documentElement.removeAttribute('data-theme');
    } else {
      document.documentElement.setAttribute('data-theme', theme);
    }

    document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
      const meta = themeMeta[theme];
      button.setAttribute('aria-label', `切换主题，当前：${meta.label}`);
      button.setAttribute('title', `切换主题，当前：${meta.label}`);
      button.querySelector('[data-theme-icon]').textContent = meta.icon;
      button.querySelector('[data-theme-label]').textContent = meta.label;
    });
  };

  const setTheme = (theme) => {
    if (theme === 'system') {
      localStorage.removeItem(themeKey);
    } else {
      localStorage.setItem(themeKey, theme);
    }
    applyTheme(theme);
  };

  applyTheme(getStoredTheme());

  document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
    button.addEventListener('click', () => {
      const current = getStoredTheme();
      const next = themeOrder[(themeOrder.indexOf(current) + 1) % themeOrder.length];
      setTheme(next);
    });
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
