export function initParallaxAndStickyHeader() {
  // --- Parallax ---
  const bg = document.getElementById("parallax-bg");
  if (bg) {
    document.addEventListener("scroll", () => {
      const scrollY = window.scrollY;
      const current = bg.style.transform.match(/translate\(([^)]+)\)/);
      const x = current ? current[1].split(',')[0] : "0px";
      bg.style.transform = `translate(${x}, ${scrollY * 0.2}px) scale(1.08)`;
    });
  }

  // --- Sticky/fixed tabs ---
  const tabs = document.querySelector('.tabs');
  if (!tabs) return;

  const tabsTop = tabs.getBoundingClientRect().top + window.scrollY; // position initiale dans le document
  const OFFSET = 5; // petit décalage pour les coller plus haut
  const tabsPlaceholder = document.createElement('div');
  tabsPlaceholder.style.height = `${tabs.offsetHeight}px`;

  let fixed = false;

  const onScroll = () => {
    if (window.scrollY >= tabsTop - OFFSET && !fixed) {
      // Activer mode fixe
      tabs.classList.add('sticky');
      tabs.style.position = 'fixed';
      tabs.style.top = '-31px';
      tabs.style.left = '0';
      tabs.style.right = '0';
      tabs.parentNode.insertBefore(tabsPlaceholder, tabs);
      fixed = true;
    } else if (window.scrollY < tabsTop - OFFSET && fixed) {
      // Désactiver mode fixe
      tabs.classList.remove('sticky');
      tabs.style.position = '';
      tabs.style.top = '';
      tabs.style.left = '';
      tabs.style.right = '';
      if (tabsPlaceholder.parentNode) {
        tabsPlaceholder.parentNode.removeChild(tabsPlaceholder);
      }
      fixed = false;
    }
  };

  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
}
