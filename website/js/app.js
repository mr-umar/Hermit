/**
 * Hermit Website - Internationalization, Theme Management & Interactivity
 */

const translations = {
  en: {
    "nav.features": "Features",
    "nav.how": "How it works",
    "nav.hardware": "NFC hardware",
    "nav.privacy": "Privacy",
    "nav.faq": "FAQ",
    "hero.title": "Mindful focus, anchored in the physical world.",
    "hero.subtitle": "Hermit pairs affordable, standard NFC tags with genuine Android application blocking. Tap your desk tag to create a physical sanctuary for deep work. No subscriptions. No cloud servers. Zero surveillance.",
    "hero.download": "Download APK",
    "hero.source": "GitHub repository",
    "hero.spec.runtime": "Platform",
    "hero.spec.runtime.val": "Android 8.0+",
    "hero.spec.hardware": "Hardware",
    "hero.spec.hardware.val": "NTAG213 / 215 / 216",
    "hero.spec.privacy": "Telemetry",
    "hero.spec.privacy.val": "100% local (0 KB)",
    "hero.spec.license": "License",
    "hero.spec.license.val": "Free & open source (AGPLv3)",
    "how.title": "How Hermit works",
    "how.subtitle": "Transform an inexpensive NFC tag into a physical sanctuary from digital distractions.",
    "how.step1.title": "1. Configure your profile",
    "how.step1.desc": "Choose which distracting social, video, or communication apps to silence and set optional session durations.",
    "how.step2.title": "2. Write the physical tag",
    "how.step2.desc": "Hold any standard NTAG213, NTAG215, or NTAG216 tag to your phone. Hermit writes a verified NDEF message with an Android Application Record.",
    "how.step3.title": "3. Tap to engage",
    "how.step3.desc": "Tap the tag on your desk or doorway. Hermit instantly activates your focus mode, even if the app is closed or running in the background.",
    "how.step4.title": "4. Genuine app & web blocking",
    "how.step4.desc": "Opening blocked apps or visiting social media in browsers triggers a mindful breathing pause, requiring your physical tag to exit.",
    "features.title": "Built for intention, not addiction",
    "features.subtitle": "Everything you need to protect your deep work sessions without proprietary pucks or recurring subscriptions.",
    "features.f1.title": "Universal NFC compatibility",
    "features.f1.desc": "Works with standard, inexpensive NFC Forum Type 2 tags (NTAG213, NTAG215, NTAG216) readily available anywhere online.",
    "features.f2.title": "Real accessibility blocking",
    "features.f2.desc": "A genuine Android AccessibilityService intercepts distracting foreground app launches and browser URLs cleanly without root.",
    "features.f3.title": "100% local-first & private",
    "features.f3.desc": "Zero accounts, zero trackers, zero cloud servers, and no internet permission. Your habits and data stay entirely on your phone.",
    "features.f4.title": "Flexible & secure exit behaviors",
    "features.f4.desc": "Choose between physical NFC tag unlocking (maximum 24 hours for safety), auto-timer release, or emergency reflection countdowns.",
    "features.f5.title": "Monk sanctuary & custom profiles",
    "features.f5.desc": "Includes the Monk mode profile (whitelisting only Phone, Camera, WhatsApp, Spotify, and Maps) alongside fully customizable modes.",
    "features.f6.title": "Local session metrics & history",
    "features.f6.desc": "Track your total focus hours, completed sessions, and prevented distraction attempts securely in encrypted local Room storage.",
    "hw.title": "Hardware compatibility",
    "hw.subtitle": "Hermit avoids vendor lock-in. Use affordable, standard NFC tags rather than proprietary accessories.",
    "hw.col.chip": "Chip / standard",
    "hw.col.capacity": "Usable capacity",
    "hw.col.status": "Compatibility",
    "hw.col.notes": "Notes",
    "hw.row1.notes": "144 bytes user memory. Perfect for Hermit's compact payload (<120 bytes). Highly affordable.",
    "hw.row2.notes": "504 bytes user memory. Widely available and fully tested with read-back verification.",
    "hw.row3.notes": "888 bytes user memory. Maximum capacity for Type 2 tags.",
    "hw.row4.notes": "Standard NDEF tags supported subject to phone NFC controller compatibility.",
    "privacy.title": "A transparent digital wellbeing shield",
    "privacy.subtitle": "Unlike cloud-connected wellbeing apps, Hermit does not harvest your behavior.",
    "privacy.card1.title": "Zero telemetry",
    "privacy.card1.desc": "Hermit contains no Google Analytics, no Firebase, no crash trackers, and no ad SDKs. Your phone never communicates with remote servers.",
    "privacy.card2.title": "Responsible accessibility",
    "privacy.card2.desc": "Hermit's accessibility service respects your privacy: it never inspects personal messages, text, or keystrokes. It only detects blocked packages.",
    "privacy.card3.title": "Local data portability",
    "privacy.card3.desc": "Export your profile configurations and session statistics anytime in standard JSON format, or wipe everything with one tap.",
    "install.title": "Installation & source code",
    "install.subtitle": "Hermit is free, open source, and built entirely with native Kotlin & Jetpack Compose.",
    "install.build.title": "Build from source",
    "install.build.step1": "Clone the repository from GitHub:",
    "install.build.step2": "Build debug APK with Gradle:",
    "install.build.step3": "Install directly via ADB to your device:",
    "faq.title": "Frequently asked questions",
    "faq.q1": "What NFC tags should I buy?",
    "faq.a1": "Any standard NTAG213, NTAG215, or NTAG216 sticker or card will work. NTAG215 cards/stickers are widely available, inexpensive (often under $0.50 each), and fully supported.",
    "faq.q2": "Why does Hermit require Accessibility permission?",
    "faq.a2": "Android requires an AccessibilityService to reliably detect when a foreground application opens or when social media is visited in browsers. Hermit only observes package names to show the focus screen. It never inspects screen text, keystrokes, or personal data.",
    "faq.q3": "Does Hermit require internet access?",
    "faq.a3": "No. Hermit does not even declare the INTERNET permission in its AndroidManifest. It is physically impossible for the app to send data to any remote server.",
    "faq.q4": "Does the app open automatically when tapping the tag while closed?",
    "faq.a4": "Yes. Every tag written with Hermit includes an Android Application Record (AAR) and a high-priority NDEF filter. Tapping the tag brings Hermit to the foreground automatically, even if the app was completely closed.",
    "faq.q5": "Can I unlock my phone if I lose my NFC tag?",
    "faq.a5": "Yes. Focus sessions have a safety limit of 24 hours, and profiles can be configured with an Emergency Exit option featuring a 60-second reflection countdown to prevent impulsive unlocking.",
    "footer.desc": "Open-source Android application for NFC-powered mindful focus.",
    "footer.license": "Licensed under GNU AGPLv3."
  },
  es: {
    "nav.features": "Funciones",
    "nav.how": "Cómo funciona",
    "nav.hardware": "Hardware NFC",
    "nav.privacy": "Privacidad",
    "nav.faq": "Preguntas frecuentes",
    "hero.title": "Concentración consciente, anclada en el mundo físico.",
    "hero.subtitle": "Hermit combina etiquetas NFC estándar y económicas con bloqueo nativo de aplicaciones en Android. Toca la etiqueta de tu escritorio para activar un santuario de trabajo profundo. Sin suscripciones. Sin servidores en la nube. Cero vigilancia.",
    "hero.download": "Descargar APK",
    "hero.source": "Repositorio en GitHub",
    "hero.spec.runtime": "Plataforma",
    "hero.spec.runtime.val": "Android 8.0+",
    "hero.spec.hardware": "Hardware",
    "hero.spec.hardware.val": "NTAG213 / 215 / 216",
    "hero.spec.privacy": "Telemetría",
    "hero.spec.privacy.val": "100 % local (0 KB)",
    "hero.spec.license": "Licencia",
    "hero.spec.license.val": "Libre y abierta (AGPLv3)",
    "how.title": "Cómo funciona Hermit",
    "how.subtitle": "Convierte una etiqueta NFC económica en un santuario físico frente a las distracciones digitales.",
    "how.step1.title": "1. Configura tu perfil",
    "how.step1.desc": "Elige qué aplicaciones de redes sociales, vídeo o mensajería silenciar y define duraciones opcionales.",
    "how.step2.title": "2. Graba la etiqueta física",
    "how.step2.desc": "Acerca cualquier etiqueta estándar NTAG213, NTAG215 o NTAG216. Hermit graba y verifica un registro NDEF compacto con registro AAR.",
    "how.step3.title": "3. Toca para concentrarte",
    "how.step3.desc": "Toca la etiqueta en tu escritorio. Hermit activa al instante tu modo de enfoque y levanta la barrera contra distracciones, incluso si la app está cerrada.",
    "how.step4.title": "4. Bloqueo real de aplicaciones y webs",
    "how.step4.desc": "Al abrir una app bloqueada o intentar navegar por redes sociales, aparece una pantalla de pausa consciente que requiere escanear la etiqueta física para salir.",
    "features.title": "Diseñado para la intención, no para la adicción",
    "features.subtitle": "Todo lo que necesitas para proteger tus sesiones de trabajo profundo sin accesorios propietarios ni suscripciones.",
    "features.f1.title": "Compatibilidad NFC universal",
    "features.f1.desc": "Compatible con etiquetas estándar NFC Forum Tipo 2 (NTAG213, NTAG215, NTAG216) económicas y fáciles de conseguir en cualquier parte.",
    "features.f2.title": "Bloqueo por accesibilidad real",
    "features.f2.desc": "Un genuino servicio de accesibilidad de Android detecta lanzamientos de apps distractoras y URLs de redes sociales de forma limpia y sin necesidad de root.",
    "features.f3.title": "100 % local y privado",
    "features.f3.desc": "Sin cuentas, sin rastreadores, sin servidores remotos y sin permiso de internet innecesario. Todo se queda en tu dispositivo.",
    "features.f4.title": "Salida flexible y segura",
    "features.f4.desc": "Elige entre desbloqueo exclusivo por etiqueta NFC física (máximo 24 h por seguridad), temporizador o salida de emergencia con cuenta atrás reflexiva.",
    "features.f5.title": "Santuario Monk y perfiles a medida",
    "features.f5.desc": "Incluye el perfil Monk (solo teléfono, cámara, WhatsApp, Spotify y mapas) y perfiles personalizados como Trabajo profundo, Estudio o Descanso.",
    "features.f6.title": "Historial y estadísticas locales",
    "features.f6.desc": "Registra tus horas totales de enfoque, sesiones completadas y distracciones evitadas en almacenamiento local Room cifrado.",
    "hw.title": "Compatibilidad de hardware",
    "hw.subtitle": "Hermit evita el bloqueo de fabricante. Usa etiquetas NFC estándar en lugar de accesorios cerrados.",
    "hw.col.chip": "Chip / estándar",
    "hw.col.capacity": "Capacidad útil",
    "hw.col.status": "Compatibilidad",
    "hw.col.notes": "Notas",
    "hw.row1.notes": "144 bytes de memoria. Ideal para el registro compacto de Hermit (<120 bytes). Muy económico.",
    "hw.row2.notes": "504 bytes de memoria. Ampliamente disponible y verificado con comprobación de lectura.",
    "hw.row3.notes": "888 bytes de memoria. Máxima capacidad para etiquetas Tipo 2.",
    "hw.row4.notes": "Etiquetas estándar NDEF compatibles según el controlador NFC de tu teléfono.",
    "privacy.title": "Un escudo de bienestar digital transparente",
    "privacy.subtitle": "A diferencia de las aplicaciones conectadas a la nube, Hermit no recopila tu comportamiento.",
    "privacy.card1.title": "Cero telemetría",
    "privacy.card1.desc": "Hermit no incluye Google Analytics, Firebase ni librerías de publicidad. Tu teléfono nunca se comunica con servidores externos.",
    "privacy.card2.title": "Accesibilidad responsable",
    "privacy.card2.desc": "El servicio de accesibilidad opera con estricto respeto a tu privacidad: jamás lee textos privados, mensajes ni contraseñas. Solo detecta paquetes bloqueados.",
    "privacy.card3.title": "Portabilidad local",
    "privacy.card3.desc": "Exporta tus perfiles y estadísticas en formato JSON estándar en cualquier momento o borra todos los datos con un solo toque.",
    "install.title": "Instalación y código fuente",
    "install.subtitle": "Hermit es libre, de código abierto y desarrollado íntegramente en Kotlin nativo y Jetpack Compose.",
    "install.build.title": "Compilar desde el código fuente",
    "install.build.step1": "Clona el repositorio desde GitHub:",
    "install.build.step2": "Compila el APK con Gradle:",
    "install.build.step3": "Instala directamente en tu dispositivo mediante ADB:",
    "faq.title": "Preguntas frecuentes",
    "faq.q1": "¿Qué etiquetas NFC debo comprar?",
    "faq.a1": "Cualquier pegatina o tarjeta NTAG213, NTAG215 o NTAG216 estándar funcionará. Las NTAG215 son muy comunes, económicas (habitualmente menos de 0,50 €) y 100 % compatibles.",
    "faq.q2": "¿Por qué Hermit necesita permiso de accesibilidad?",
    "faq.a2": "Android requiere un servicio de accesibilidad para detectar con fiabilidad cuándo se abre una aplicación en primer plano o se navega por redes sociales. Hermit solo revisa el paquete o dominio para mostrar la pantalla de enfoque. Nunca lee contenidos privados, pulsaciones ni datos personales.",
    "faq.q3": "¿Requiere Hermit conexión a internet?",
    "faq.a3": "No. Hermit ni siquiera declara el permiso INTERNET en su AndroidManifest. Es técnicamente imposible que envíe información fuera de tu dispositivo.",
    "faq.q4": "¿Se abre la app automáticamente al acercar la etiqueta si está cerrada?",
    "faq.a4": "Sí. Todas las etiquetas grabadas con Hermit incluyen un registro AAR (Android Application Record) y un filtro prioritario NDEF. Al acercar la etiqueta, incluso con la app cerrada o en segundo plano, Android abre Hermit automáticamente y gestiona tu sesión.",
    "faq.q5": "¿Puedo desbloquear mi teléfono si pierdo la etiqueta NFC?",
    "faq.a5": "Sí. Por seguridad, el bloqueo físico tiene un límite máximo de 24 horas y puedes habilitar la salida de emergencia con una pausa reflexiva de 60 segundos.",
    "footer.desc": "Aplicación Android de código abierto para concentración consciente mediante NFC.",
    "footer.license": "Licencia GNU AGPLv3."
  }
};

let currentLang = 'en';

function applyLanguage(lang) {
  if (!translations[lang]) lang = 'en';
  currentLang = lang;
  document.documentElement.lang = lang;
  localStorage.setItem('hermit_lang', lang);

  const langBtn = document.getElementById('lang-toggle');
  if (langBtn) {
    langBtn.textContent = lang === 'en' ? 'ES' : 'EN';
    langBtn.setAttribute('aria-label', lang === 'en' ? 'Cambiar a español' : 'Switch to English');
  }

  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (translations[lang] && translations[lang][key]) {
      el.textContent = translations[lang][key];
    }
  });

  document.title = lang === 'es'
    ? "Hermit - Concentración consciente con ancla física NFC"
    : "Hermit - NFC-powered mindful focus & digital wellbeing";
}

function initTheme() {
  const savedTheme = localStorage.getItem('hermit_theme');
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const theme = savedTheme || (prefersDark ? 'dark' : 'light');
  document.documentElement.setAttribute('data-theme', theme);
  updateThemeIcon(theme);
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'dark';
  const target = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', target);
  localStorage.setItem('hermit_theme', target);
  updateThemeIcon(target);
}

function updateThemeIcon(theme) {
  const btn = document.getElementById('theme-toggle');
  if (btn) {
    btn.innerHTML = theme === 'dark'
      ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>`
      : `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>`;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  // Check URL param or navigator language
  const urlParams = new URLSearchParams(window.location.search);
  const paramLang = urlParams.get('lang');
  const savedLang = localStorage.getItem('hermit_lang');
  const browserLang = (navigator.language || navigator.userLanguage || '').toLowerCase().startsWith('es') ? 'es' : 'en';

  const initialLang = paramLang || savedLang || browserLang;
  applyLanguage(initialLang);

  initTheme();

  // Language Toggle Button
  const langToggle = document.getElementById('lang-toggle');
  if (langToggle) {
    langToggle.addEventListener('click', () => {
      applyLanguage(currentLang === 'en' ? 'es' : 'en');
    });
  }

  // Theme Toggle Button
  const themeToggle = document.getElementById('theme-toggle');
  if (themeToggle) {
    themeToggle.addEventListener('click', toggleTheme);
  }

  // Mobile Navigation Drawer Toggle
  const menuToggle = document.getElementById('menu-toggle');
  const siteHeader = document.querySelector('.site-header');

  function closeMobileMenu() {
    if (siteHeader && siteHeader.classList.contains('menu-open')) {
      siteHeader.classList.remove('menu-open');
      if (menuToggle) {
        menuToggle.setAttribute('aria-expanded', 'false');
      }
    }
  }

  if (menuToggle && siteHeader) {
    menuToggle.addEventListener('click', (e) => {
      e.stopPropagation();
      const isOpen = siteHeader.classList.toggle('menu-open');
      menuToggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    });

    // Close on Escape key
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        closeMobileMenu();
      }
    });

    // Close on click outside the header
    document.addEventListener('click', (e) => {
      if (!siteHeader.contains(e.target)) {
        closeMobileMenu();
      }
    });
  }

  // Smooth in-page navigation without header collision or focus trapping
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
      const targetId = this.getAttribute('href');
      if (!targetId) return;

      closeMobileMenu();

      if (targetId === '#') {
        e.preventDefault();
        window.scrollTo({ top: 0, behavior: 'smooth' });
        this.blur();
        return;
      }

      const targetEl = document.querySelector(targetId);
      if (targetEl) {
        e.preventDefault();
        const headerHeight = siteHeader ? siteHeader.offsetHeight : 70;
        const targetPos = targetEl.getBoundingClientRect().top + window.pageYOffset - (headerHeight + 16);

        window.scrollTo({
          top: Math.max(0, targetPos),
          behavior: 'smooth'
        });

        // Release focus from the clicked element so it never blocks or overlaps the header
        this.blur();
      }
    });
  });

  // Accordion interactivity with accessible ARIA
  document.querySelectorAll('.faq-question').forEach(btn => {
    btn.addEventListener('click', () => {
      const parent = btn.parentElement;
      const isExpanded = btn.getAttribute('aria-expanded') === 'true';

      document.querySelectorAll('.faq-item').forEach(item => {
        item.classList.remove('active');
        const qBtn = item.querySelector('.faq-question');
        if (qBtn) qBtn.setAttribute('aria-expanded', 'false');
      });

      if (!isExpanded) {
        parent.classList.add('active');
        btn.setAttribute('aria-expanded', 'true');
      }
    });
  });

  // Interactive one-click code copy buttons
  document.querySelectorAll('.btn-copy').forEach(btn => {
    btn.addEventListener('click', async () => {
      const code = btn.getAttribute('data-copy');
      if (!code) return;
      try {
        await navigator.clipboard.writeText(code);
        const textSpan = btn.querySelector('.copy-text');
        const origText = textSpan ? textSpan.textContent : '';
        if (textSpan) {
          textSpan.textContent = currentLang === 'es' ? '¡Copiado!' : 'Copied!';
        }
        btn.classList.add('copied');
        setTimeout(() => {
          if (textSpan) textSpan.textContent = origText;
          btn.classList.remove('copied');
        }, 2200);
      } catch (_) {
        // Fallback for restricted clipboard environments
      }
    });
  });
});

