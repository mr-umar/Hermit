/**
 * Hermit Website - Internationalization, Theme Management & Interactivity
 */

const translations = {
  en: {
    "nav.features": "Features",
    "nav.how": "How It Works",
    "nav.hardware": "NFC Hardware",
    "nav.privacy": "Privacy",
    "nav.faq": "FAQ",
    "hero.pill": "Open Source & Local First",
    "hero.title": "Mindful focus, anchored by physical NFC.",
    "hero.subtitle": "Take back your digital attention with affordable, standard NFC tags. Create focus profiles, block distracting applications through genuine Android Accessibility, and keep your data 100% private.",
    "hero.download": "Download Debug APK",
    "hero.source": "View on GitHub",
    "how.title": "How Hermit Works",
    "how.subtitle": "Transform an inexpensive NFC tag into a physical sanctuary from digital distractions.",
    "how.step1.title": "1. Configure Profile",
    "how.step1.desc": "Choose which distracting social, video, or communication apps to silence and set optional session durations.",
    "how.step2.title": "2. Write Physical Tag",
    "how.step2.desc": "Hold any standard NTAG213, NTAG215, or NTAG216 tag to your phone. Hermit writes a compact, verified NDEF record.",
    "how.step3.title": "3. Tap to Engage",
    "how.step3.desc": "Tap the tag on your desk or door. Hermit instantly activates your focus mode and raises the distraction barrier.",
    "how.step4.title": "4. Genuine App Blocking",
    "how.step4.desc": "Opening blocked apps brings you to a calm breathing pause, requiring a physical tag scan or strict reflection to exit.",
    "features.title": "Built for Intention, Not Addiction",
    "features.subtitle": "Everything you need to protect your deep work sessions without proprietary hardware or recurring subscriptions.",
    "features.f1.title": "Standard NFC Compatibility",
    "features.f1.desc": "Works with standard, inexpensive NFC Forum Type 2 tags (NTAG213, NTAG215, NTAG216) readily available anywhere.",
    "features.f2.title": "Real Accessibility Blocking",
    "features.f2.desc": "Genuine Android AccessibilityService intercepts distracting foreground app launches cleanly and reliably without root.",
    "features.f3.title": "100% Local-First & Private",
    "features.f3.desc": "Zero accounts, zero trackers, zero cloud servers, and no unnecessary internet permissions. Your habits stay on your phone.",
    "features.f4.title": "Flexible Exit Behaviors",
    "features.f4.desc": "Choose between NFC-only unlocking, timed auto-release, or emergency override with a 60-second reflection timer in strict mode.",
    "features.f5.title": "Custom Focus Profiles",
    "features.f5.desc": "Create dedicated profiles like Deep Work, Study, Rest, or Mindful Evening with tailored app selection.",
    "features.f6.title": "Session Metrics & History",
    "features.f6.desc": "Track your total focus hours, completed sessions, and prevented distraction attempts securely in local Room storage.",
    "hw.title": "Hardware Compatibility",
    "hw.subtitle": "Hermit avoids vendor lock-in. Use affordable, standard NFC tags rather than proprietary pucks.",
    "hw.col.chip": "Chip / Standard",
    "hw.col.capacity": "Usable Capacity",
    "hw.col.status": "Compatibility",
    "hw.col.notes": "Notes",
    "hw.row1.notes": "144 bytes user memory. Perfect for Hermit's compact payload (<120 bytes). Highly affordable.",
    "hw.row2.notes": "504 bytes user memory. Widely available and fully tested with read-back verification.",
    "hw.row3.notes": "888 bytes user memory. Maximum capacity for Type 2 tags.",
    "hw.row4.notes": "Standard NDEF tags supported subject to phone NFC controller compatibility.",
    "privacy.title": "A Transparent Digital Wellbeing Shield",
    "privacy.subtitle": "Unlike cloud-connected wellbeing apps, Hermit does not harvest your behavior.",
    "privacy.card1.title": "Zero Telemetry",
    "privacy.card1.desc": "Hermit contains no Google Analytics, no Firebase, no crash trackers, and no ad SDKs. Your phone never pings remote servers.",
    "privacy.card2.title": "Responsible Accessibility",
    "privacy.card2.desc": "Hermit's Accessibility Service disables window content retrieval. It only checks the top-level package name to protect your attention.",
    "privacy.card3.title": "Local Data Portability",
    "privacy.card3.desc": "Export your profile configurations and session statistics anytime in standard JSON format, or wipe everything with one tap.",
    "install.title": "Installation & Source Code",
    "install.subtitle": "Hermit is free, open source, and built entirely with native Kotlin & Jetpack Compose.",
    "install.build.title": "Build from Source",
    "install.build.step1": "Clone the repository from GitHub:",
    "install.build.step2": "Build debug APK with Gradle:",
    "install.build.step3": "Install directly via ADB to your device:",
    "faq.title": "Frequently Asked Questions",
    "faq.q1": "What NFC tags should I buy?",
    "faq.a1": "Any standard NTAG213, NTAG215, or NTAG216 sticker or card will work. NTAG215 cards/stickers are widely available, inexpensive (often under $0.50 each), and fully supported.",
    "faq.q2": "Why does Hermit require Accessibility permission?",
    "faq.a2": "Android requires an AccessibilityService to reliably detect when a foreground application opens. Hermit only observes application package names to know when to show the focus screen. It never inspects screen text, keystrokes, or personal data.",
    "faq.q3": "Does Hermit require internet access?",
    "faq.a3": "No. Hermit does not even declare the INTERNET permission in its AndroidManifest. It is physically impossible for the app to send data to any remote server.",
    "faq.q4": "Can I unlock my phone if I lose my NFC tag?",
    "faq.a4": "Yes. Each focus profile can be configured with an Emergency Exit option. In strict mode, a 60-second reflection countdown prevents impulsive unlocking while ensuring you are never permanently locked out.",
    "footer.desc": "Open-source Android application for NFC-powered mindful focus.",
    "footer.license": "Licensed under the GNU AGPLv3 License."
  },
  es: {
    "nav.features": "Funciones",
    "nav.how": "Cómo Funciona",
    "nav.hardware": "Hardware NFC",
    "nav.privacy": "Privacidad",
    "nav.faq": "Preguntas",
    "hero.pill": "Código Abierto y 100% Local",
    "hero.title": "Concentración consciente con ancla física NFC.",
    "hero.subtitle": "Recupera tu atención digital con etiquetas NFC accesibles y universales. Crea perfiles de enfoque, bloquea aplicaciones distractoras con el Servicio de Accesibilidad nativo de Android y mantén tus datos totalmente privados.",
    "hero.download": "Descargar APK de Depuración",
    "hero.source": "Ver en GitHub",
    "how.title": "Cómo Funciona Hermit",
    "how.subtitle": "Convierte una etiqueta NFC económica en un santuario físico frente a las distracciones digitales.",
    "how.step1.title": "1. Configura tu Perfil",
    "how.step1.desc": "Elige qué aplicaciones de redes sociales, vídeo o mensajería silenciar y define duraciones opcionales.",
    "how.step2.title": "2. Graba la Etiqueta Física",
    "how.step2.desc": "Acerca cualquier etiqueta estándar NTAG213, NTAG215 o NTAG216. Hermit graba y verifica un registro NDEF compacto.",
    "how.step3.title": "3. Toca para Concentrarte",
    "how.step3.desc": "Toca la etiqueta en tu escritorio. Hermit activa al instante tu modo de enfoque y levanta la barrera contra distracciones.",
    "how.step4.title": "4. Bloqueo Real de Aplicaciones",
    "how.step4.desc": "Al abrir una app bloqueada aparece una pantalla de pausa consciente que requiere escanear la etiqueta o reflexionar para salir.",
    "features.title": "Diseñado para la Intención, no para la Adicción",
    "features.subtitle": "Todo lo que necesitas para proteger tus sesiones de trabajo profundo sin accesorios propietarios ni suscripciones.",
    "features.f1.title": "Compatibilidad NFC Universal",
    "features.f1.desc": "Compatible con etiquetas estándar NFC Forum Tipo 2 (NTAG213, NTAG215, NTAG216) económicas y fáciles de conseguir.",
    "features.f2.title": "Bloqueo por Accesibilidad Real",
    "features.f2.desc": "Un genuino Servicio de Accesibilidad de Android detecta lanzamientos de apps distractoras de forma limpia y sin necesidad de root.",
    "features.f3.title": "100% Local y Privado",
    "features.f3.desc": "Sin cuentas, sin rastreadores, sin servidores remotos y sin permiso de internet innecesario. Todo se queda en tu dispositivo.",
    "features.f4.title": "Salida Flexible",
    "features.f4.desc": "Elige entre desbloqueo exclusivo por NFC, finalización por temporizador o botón de emergencia con pausa reflexiva de 60 segundos.",
    "features.f5.title": "Perfiles Personalizados",
    "features.f5.desc": "Crea perfiles como Trabajo Profundo, Estudio, Descanso o Noche Consciente con selección independiente de apps.",
    "features.f6.title": "Historial y Estadísticas",
    "features.f6.desc": "Registra tus horas totales de enfoque, sesiones completadas y distracciones evitadas en almacenamiento local Room.",
    "hw.title": "Compatibilidad de Hardware",
    "hw.subtitle": "Hermit evita el bloqueo de fabricante. Usa etiquetas NFC estándar en lugar de accesorios cerrados.",
    "hw.col.chip": "Chip / Estándar",
    "hw.col.capacity": "Capacidad Útil",
    "hw.col.status": "Compatibilidad",
    "hw.col.notes": "Notas",
    "hw.row1.notes": "144 bytes de memoria. Ideal para el payload compacto de Hermit (<120 bytes). Muy económico.",
    "hw.row2.notes": "504 bytes de memoria. Ampliamente disponible y verificado con comprobación de lectura.",
    "hw.row3.notes": "888 bytes de memoria. Máxima capacidad para etiquetas Tipo 2.",
    "hw.row4.notes": "Etiquetas estándar NDEF compatibles según el controlador NFC de tu teléfono.",
    "privacy.title": "Un Escudo de Bienestar Digital Transparente",
    "privacy.subtitle": "A diferencia de las apps conectadas a la nube, Hermit no recopila tu comportamiento.",
    "privacy.card1.title": "Cero Telemetría",
    "privacy.card1.desc": "Hermit no incluye Google Analytics, Firebase ni librerías de publicidad. Tu teléfono nunca se comunica con servidores externos.",
    "privacy.card2.title": "Accesibilidad Responsable",
    "privacy.card2.desc": "El servicio de accesibilidad tiene desactivada la lectura de pantalla. Solo consulta el nombre del paquete para proteger tu atención.",
    "privacy.card3.title": "Portabilidad Local",
    "privacy.card3.desc": "Exporta tus perfiles y estadísticas en formato JSON estándar en cualquier momento o borra todos los datos con un toque.",
    "install.title": "Instalación y Código Fuente",
    "install.subtitle": "Hermit es libre, de código abierto y desarrollado íntegramente en Kotlin nativo y Jetpack Compose.",
    "install.build.title": "Compilar desde el Código Fuente",
    "install.build.step1": "Clona el repositorio desde GitHub:",
    "install.build.step2": "Compila el APK con Gradle:",
    "install.build.step3": "Instala directamente en tu dispositivo vía ADB:",
    "faq.title": "Preguntas Frecuentes",
    "faq.q1": "¿Qué etiquetas NFC debo comprar?",
    "faq.a1": "Cualquier pegatina o tarjeta NTAG213, NTAG215 o NTAG216 estándar funcionará. Las NTAG215 son muy comunes, económicas (habitualmente menos de 0,50 €) y 100% compatibles.",
    "faq.q2": "¿Por qué Hermit necesita permiso de Accesibilidad?",
    "faq.a2": "Android requiere un Servicio de Accesibilidad para detectar con fiabilidad cuando se abre una app en primer plano. Hermit solo revisa el paquete de la app para mostrar la pantalla de enfoque. Nunca lee textos, pulsaciones ni datos personales.",
    "faq.q3": "¿Requiere Hermit conexión a Internet?",
    "faq.a3": "No. Hermit ni siquiera declara el permiso INTERNET en su AndroidManifest. Es técnicamente imposible que envíe información fuera de tu dispositivo.",
    "faq.q4": "¿Puedo desbloquear mi teléfono si pierdo la etiqueta NFC?",
    "faq.a4": "Sí. Cada perfil puede configurarse con salida de emergencia. En el modo estricto, una cuenta atrás de 60 segundos previene salidas impulsivas asegurando que nunca te quedes bloqueado permanentemente.",
    "footer.desc": "Aplicación Android de código abierto para concentración consciente mediante NFC.",
    "footer.license": "Licenciado bajo GNU AGPLv3."
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
    langBtn.setAttribute('aria-label', lang === 'en' ? 'Cambiar a Español' : 'Switch to English');
  }

  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (translations[lang][key]) {
      el.textContent = translations[lang][key];
    }
  });

  document.title = lang === 'es'
    ? "Hermit - Concentración consciente con ancla física NFC"
    : "Hermit - NFC-Powered Mindful Focus & Digital Wellbeing";
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

  // Accordion interactivity
  document.querySelectorAll('.faq-question').forEach(btn => {
    btn.addEventListener('click', () => {
      const parent = btn.parentElement;
      const wasActive = parent.classList.contains('active');
      document.querySelectorAll('.faq-item').forEach(item => item.classList.remove('active'));
      if (!wasActive) {
        parent.classList.add('active');
      }
    });
  });
});
