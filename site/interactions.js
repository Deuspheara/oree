/* Decorative footer lighting and progressive section entrances. */
(() => {
  const motion = matchMedia('(prefers-reduced-motion: reduce)');
  const fine = matchMedia('(hover: hover) and (pointer: fine)');
  const footer = document.querySelector('.footer');
  const mark = footer.querySelector('.footer-wordmark');
  let frame = 0, last = 0;
  let x = NaN, y = 0, targetX = 0, targetY = 0;
  let footerVisible = false;
  function animate(now) {
    frame = 0;
    if (document.hidden || !footerVisible || motion.matches || !fine.matches) { last = 0; return; }
    const dt = last ? Math.min((now - last) / 1000, .05) : 1 / 60;
    last = now;
    const follow = 1 - Math.exp(-11 * dt);
    x += (targetX - x) * follow; y += (targetY - y) * follow;
    mark.style.setProperty('--light-x', `${x}px`);
    mark.style.setProperty('--light-y', `${y}px`);
    if (Math.hypot(targetX - x, targetY - y) > .4) frame = requestAnimationFrame(animate);
    else last = 0;
  }
  function wake() {
    if (!frame && footerVisible && !document.hidden && !motion.matches && fine.matches) frame = requestAnimationFrame(animate);
  }
  function rest() {
    const bounds = mark.getBoundingClientRect();
    targetX = bounds.width / 2; targetY = bounds.height * .45;
    if (!Number.isFinite(x)) { x = targetX; y = targetY; }
    wake();
  }
  footer.addEventListener('pointermove', event => {
    if (motion.matches || !fine.matches) return;
    const bounds = mark.getBoundingClientRect();
    if (!Number.isFinite(x)) { x = bounds.width / 2; y = bounds.height * .45; }
    targetX = event.clientX - bounds.left; targetY = event.clientY - bounds.top;
    wake();
  }, { passive: true });
  footer.addEventListener('pointerleave', rest);
  new IntersectionObserver(entries => {
    footerVisible = entries[0].isIntersecting;
    if (footerVisible) rest();
    else { cancelAnimationFrame(frame); frame = 0; last = 0; }
  }).observe(footer);
  document.addEventListener('visibilitychange', () => {
    cancelAnimationFrame(frame); frame = 0; last = 0;
    if (!document.hidden) rest();
  });
  function preferenceChanged() {
    cancelAnimationFrame(frame); frame = 0; last = 0;
    mark.style.removeProperty('--light-x'); mark.style.removeProperty('--light-y'); x = NaN;
    if (motion.matches) {
      document.documentElement.classList.remove('reveals-enabled');
      document.querySelectorAll('[data-reveal]').forEach(element => element.classList.add('is-shown'));
    }
    rest();
  }
  motion.addEventListener('change', preferenceChanged);
  fine.addEventListener('change', preferenceChanged);

  if (!motion.matches && 'IntersectionObserver' in window) {
    const reveals = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return;
        entry.target.classList.add('is-shown'); reveals.unobserve(entry.target);
      });
    }, { rootMargin: '0px 0px -10% 0px' });
    document.querySelectorAll('[data-reveal]').forEach(element => {
      const box = element.getBoundingClientRect();
      if (box.top < innerHeight * .9 && box.bottom > 0) element.classList.add('is-shown');
      else reveals.observe(element);
    });
    document.addEventListener('focusin', event => {
      const parent = event.target.closest('[data-reveal]');
      if (parent) { parent.classList.add('is-shown'); reveals.unobserve(parent); }
    });
    document.documentElement.classList.add('reveals-enabled');
  }
})();
