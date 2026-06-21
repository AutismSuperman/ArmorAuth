(() => {
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
