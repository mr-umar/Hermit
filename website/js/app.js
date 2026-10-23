/**
 * Hermit Website - Internationalization, Theme Management & Interactivity
 */

const translations = {
  en: {
    "nav.zen": "Zen Launcher",
    "nav.features": "Features",
    "nav.how": "How it works",
    "nav.adaptive": "Profiles & NFC",
    "nav.hardware": "NFC hardware",
    "nav.security": "Security",
    "nav.privacy": "Privacy",
    "nav.faq": "FAQ",
    "hero.title": "Mindful focus, anchored in the physical world.",
    "hero.subtitle": "Hermit pairs affordable, standard NFC tags with genuine Android application blocking and an integrated Zen Launcher. Zero subscriptions. Zero cloud trackers. An unyielding physical barrier for deep work.",
    "hero.download": "Download APK",
    "hero.source": "GitHub repository",
    "hero.spec.runtime": "Platform",
    "hero.spec.runtime.val": "Android 8.0+",
    "hero.spec.hardware": "Hardware",
    "hero.spec.hardware.val": "NTAG213 / 215 / 216",
    "hero.spec.safety": "Safety ceiling",
    "hero.spec.safety.val": "12 hours max",
    "hero.spec.privacy": "Telemetry",
    "hero.spec.privacy.val": "100% local (0 KB)",
    "hero.spec.license": "License",
    "hero.spec.license.val": "Free & open source (AGPLv3)",

    /* Zen Launcher */
    "zen.badge": "Integrated Zen Launcher",
    "zen.title": "A distraction-free OLED sanctuary for your phone.",
    "zen.desc": "When Zen Mode is engaged, your phone is secluded in an ultra-minimalist home environment. The home button routes directly back to Zen. Only your selected essentials remain reachable.",
    "zen.app.phone": "Phone",
    "zen.app.camera": "Camera",
    "zen.app.spotify": "Spotify",
    "zen.app.ytmusic": "YouTube Music",
    "zen.app.custom": "Up to 5 custom apps",
    "zen.hl1.title": "Strict physical NFC deactivation",
    "zen.hl1.desc": "Cannot be exited on impulse. Early deactivation strictly requires scanning your paired physical NFC tag (backed by an internal 12-hour safety ceiling).",
    "zen.hl2.title": "Accessibility Home redirection",
    "zen.hl2.desc": "Tapping Home or recent apps automatically returns to the Zen Launcher, neutralizing unconscious muscle memory.",
    "zen.hl3.title": "System essentials always preserved",
    "zen.hl3.desc": "Keyboards (IMEs), emergency dialer, phone calls, system settings, and Hermit itself are hard-whitelisted and never blocked.",

    /* How it works */
    "how.title": "How Hermit works",
    "how.subtitle": "Zero pre-baked clutter. An intelligent onboarding that adapts to whether you have an NFC tag.",
    "how.step1.title": "1. Pair your NFC tag (skippable)",
    "how.step1.desc": "Tap any standard NTAG213, NTAG215, or NTAG216 tag to pair it. Don't have one yet? Tap 'Skip for now' to continue.",
    "how.step2.title": "2. Adaptive profile creation",
    "how.step2.desc": "Hermit tailors options to your setup: timer-based locking if you have no tag, or universal NFC switch + Zen Mode if you have a tag.",
    "how.step3.title": "3. Engage your focus",
    "how.step3.desc": "Tap your physical desk tag or start the timer. Hermit enforces focus for up to 12 hours with unyielding consistency.",
    "how.step4.title": "4. Mindful barrier & Zen launcher",
    "how.step4.desc": "Distracting apps are met with a calm reflection screen or secluded within the Zen Launcher. Exit only when intention is renewed.",

    /* Adaptive Flow */
    "adaptive.title": "Intelligent adaptive setup",
    "adaptive.subtitle": "No default profiles forced upon you. Hermit unlocks tailored capabilities depending on your physical setup.",
    "adaptive.pill.timer": "Without NFC Tag (Timer Mode)",
    "adaptive.card1.title": "Timer-based focus sessions",
    "adaptive.card1.desc": "If you haven't added an NFC tag yet, you can create focused profiles guarded by a strict countdown timer.",
    "adaptive.c1.f1": "Configurable timer up to a maximum of 12 hours",
    "adaptive.c1.f2": "Emergency exit enabled by default with a 60-second reflection pause",
    "adaptive.c1.f3": "Optional: Disable emergency exit completely for an unbypassable lock",
    "adaptive.c1.f4": "Zen Launcher mode disabled (requires physical NFC to exit safely)",
    "adaptive.pill.nfc": "With NFC Tag (Physical Switch)",
    "adaptive.card2.title": "Physical friction & Zen Launcher",
    "adaptive.card2.desc": "Pairing an NFC tag anchors your commitment in the real world and unlocks the full power of Hermit.",
    "adaptive.c2.f1": "Universal physical switch: Tap to toggle active focus on/off",
    "adaptive.c2.f2": "Unlock Zen Mode: replaces home screen with OLED essentials",
    "adaptive.c2.f3": "Deactivate exclusively with physical NFC tag",
    "adaptive.c2.f4": "Internal 12-hour ceiling guarantees safety under all conditions",

    /* Security */
    "security.title": "Anti-tamper & uninstall protection",
    "security.desc": "During moments of weakness, the impulse to delete your focus app can be overwhelming. Hermit integrates Android Device Administrator protection to disable the system uninstall button. Disabling protection requires waiting out a mindful reflection timer, giving your rational self time to take over.",

    /* Features */
    "features.title": "Built for intention, not addiction",
    "features.subtitle": "Everything you need to protect your deep work sessions without proprietary pucks or recurring subscriptions.",
    "features.f1.title": "Universal NFC compatibility",
    "features.f1.desc": "Works with standard, inexpensive NFC Forum Type 2 tags (NTAG213, NTAG215, NTAG216) readily available anywhere online.",
    "features.f2.title": "Real accessibility blocking",
    "features.f2.desc": "A genuine Android AccessibilityService intercepts distracting foreground app launches and browser URLs cleanly without root.",
    "features.f3.title": "100% local-first & private",
    "features.f3.desc": "Zero accounts, zero trackers, zero cloud dependencies. Internet access is strictly used to check and download updates from GitHub Releases; your focus habits and data stay entirely on your phone.",
    "features.f4.title": "Flexible & secure exit behaviors",
    "features.f4.desc": "Choose between physical NFC tag unlocking (maximum 12 hours safety ceiling), countdown timers, or optional 60-second reflection delays.",
    "features.f5.title": "Zen Launcher & custom profiles",
    "features.f5.desc": "Transform your home screen into a distraction-free OLED launcher with only essentials (Phone, Camera, Spotify, YT Music, plus up to 5 custom apps).",
    "features.f6.title": "Local session metrics & history",
    "features.f6.desc": "Track your total focus hours, completed sessions, and prevented distraction attempts securely in encrypted local Room storage.",

    /* Hardware */
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

    /* Privacy */
    "privacy.title": "A transparent digital wellbeing shield",
    "privacy.subtitle": "Unlike cloud-connected wellbeing apps, Hermit does not harvest your behavior.",
    "privacy.card1.title": "Zero telemetry",
    "privacy.card1.desc": "Hermit contains no Google Analytics, no Firebase, no crash trackers, and no ad SDKs. Network access is strictly used to check GitHub Releases for app updates.",
    "privacy.card2.title": "Responsible accessibility",
    "privacy.card2.desc": "Hermit's accessibility service respects your privacy: it never inspects personal messages, text, or keystrokes. It only detects blocked packages.",
    "privacy.card3.title": "Local data portability",
    "privacy.card3.desc": "Export your profile configurations and session statistics anytime in standard JSON format, or wipe everything with one tap.",

    /* Install */
    "install.title": "Installation & source code",
    "install.subtitle": "Hermit is free, open source, and built entirely with native Kotlin & Jetpack Compose.",
    "install.build.title": "Build from source",
    "install.build.step1": "Clone the repository from GitHub:",
    "install.build.step2": "Build debug APK with Gradle:",
    "install.build.step3": "Install directly via ADB to your device:",

    /* FAQ */
    "faq.title": "Frequently asked questions",
    "faq.q1": "What NFC tags should I buy?",
    "faq.a1": "Any standard NTAG213, NTAG215, or NTAG216 sticker or card will work. NTAG215 cards/stickers are widely available, inexpensive (often under $0.50 each), and fully supported.",
    "faq.q2": "Why does Hermit require Accessibility permission?",
    "faq.a2": "Android requires an AccessibilityService to reliably detect when a foreground application opens or when social media is visited in browsers. Hermit only observes package names to show the focus screen. It never inspects screen text, keystrokes, or personal data.",
    "faq.q3": "Does Hermit require internet access?",
    "faq.a3": "Only to check for updates and download new APK releases directly from GitHub Releases with SHA-256 verification. None of the core focus features, NFC tag management, or session statistics require internet access. Hermit has zero telemetry, zero analytics, and all your focus data stays 100% on your device.",
    "faq.q4": "Does the app open automatically when tapping the tag while closed?",
    "faq.a4": "Yes. Every tag written with Hermit includes an Android Application Record (AAR) and a high-priority NDEF filter. Tapping the tag brings Hermit to the foreground automatically, even if the app was completely closed.",
    "faq.q5": "Can I unlock my phone if I lose my NFC tag?",
    "faq.a5": "Yes. Focus sessions enforce a maximum safety ceiling of 12 hours under all circumstances, and profiles can be configured with an Emergency Exit option featuring a 60-second reflection countdown to prevent permanent lockout.",
    "faq.q6": "What is Zen Launcher and which apps are available in it?",
    "faq.a6": "Zen Launcher is an integrated OLED black minimal launcher. While active, pressing Home automatically returns you to Zen. It only displays Phone, Camera, Spotify, YouTube Music (if installed), and optionally up to 5 user-selected custom apps. It can stay active up to 12 hours and strictly requires your physical NFC tag to deactivate early.",
    "faq.q7": "Can I use Hermit if I haven't purchased an NFC tag yet?",
    "faq.a7": "Absolutely. During onboarding you can skip adding a tag. Hermit will allow you to create timer-based focus profiles (up to 12 hours) with an emergency exit option (enabled by default after 60s of reflection, but disableable for strict focus). You can pair an NFC tag at any time later in the Tags tab to unlock Zen Launcher mode.",
    "faq.q8": "Will Hermit ever block my keyboard, dialer, or essential system features?",
    "faq.a8": "Never. Hermit's accessibility service explicitly exempts all system Input Method Editors (keyboards like Gboard), emergency calls, system UI dialogs, and the Hermit app itself. You can always type and make emergency calls safely.",
    "footer.desc": "Open-source Android application for NFC-powered mindful focus.",
    "footer.license": "Licensed under GNU AGPLv3."
  },
  es: {
    "nav.zen": "Launcher Zen",
    "nav.features": "Funciones",
    "nav.how": "Cómo funciona",
    "nav.adaptive": "Perfiles y NFC",
    "nav.hardware": "Hardware NFC",
    "nav.security": "Seguridad",
    "nav.privacy": "Privacidad",
    "nav.faq": "Preguntas frecuentes",
    "hero.title": "Concentración consciente, anclada en el mundo físico.",
    "hero.subtitle": "Hermit combina etiquetas NFC estándar y económicas con bloqueo nativo de aplicaciones en Android y un Launcher Zen integrado. Sin suscripciones. Sin servidores en la nube. Una barrera física inquebrantable para el trabajo profundo.",
    "hero.download": "Descargar APK",
    "hero.source": "Repositorio en GitHub",
    "hero.spec.runtime": "Plataforma",
    "hero.spec.runtime.val": "Android 8.0+",
    "hero.spec.hardware": "Hardware",
    "hero.spec.hardware.val": "NTAG213 / 215 / 216",
    "hero.spec.safety": "Tope de seguridad",
    "hero.spec.safety.val": "12 horas máx.",
    "hero.spec.privacy": "Telemetría",
    "hero.spec.privacy.val": "100 % local (0 KB)",
    "hero.spec.license": "Licencia",
    "hero.spec.license.val": "Libre y abierta (AGPLv3)",

    /* Zen Launcher */
    "zen.badge": "Launcher Zen Integrado",
    "zen.title": "Un santuario OLED libre de distracciones para tu teléfono.",
    "zen.desc": "Al activar el Modo Zen, tu teléfono entra en un entorno de inicio ultra-minimalista. El botón de inicio vuelve directamente a Zen. Solo tus herramientas indispensables quedan al alcance.",
    "zen.app.phone": "Teléfono",
    "zen.app.camera": "Cámara",
    "zen.app.spotify": "Spotify",
    "zen.app.ytmusic": "YouTube Music",
    "zen.app.custom": "Hasta 5 apps personalizadas",
    "zen.hl1.title": "Desactivación física estricta con NFC",
    "zen.hl1.desc": "No se puede desactivar por impulso. Salir antes de tiempo exige obligatoriamente escanear tu etiqueta NFC física vinculada (con un límite interno de seguridad de 12 horas).",
    "zen.hl2.title": "Redirección del botón Home por accesibilidad",
    "zen.hl2.desc": "Pulsar el botón de inicio o apps recientes redirige de inmediato al Launcher Zen, neutralizando la memoria muscular involuntaria.",
    "zen.hl3.title": "Herramientas básicas del sistema siempre protegidas",
    "zen.hl3.desc": "Teclados (IMEs), marcador telefónico, llamadas de emergencia, interfaz del sistema y el propio Hermit están en lista blanca protegida y jamás se bloquean.",

    /* How it works */
    "how.title": "Cómo funciona Hermit",
    "how.subtitle": "Sin perfiles basura por defecto. Un asistente inicial inteligente que se adapta a si dispones de etiqueta NFC.",
    "how.step1.title": "1. Vincula tu etiqueta NFC (omitible)",
    "how.step1.desc": "Acerca cualquier etiqueta estándar NTAG213, NTAG215 o NTAG216 para enlazarla. ¿Aún no tienes una? Pulsa 'Omitir por ahora' para continuar.",
    "how.step2.title": "2. Creación adaptativa de perfiles",
    "how.step2.desc": "Hermit adapta las opciones a tu caso: modo temporizador si no tienes etiqueta, o interruptor universal NFC + Modo Zen si has añadido una.",
    "how.step3.title": "3. Activa tu concentración",
    "how.step3.desc": "Toca tu etiqueta física en el escritorio o inicia el temporizador. Hermit mantiene la concentración hasta un máximo de 12 horas con firmeza inquebrantable.",
    "how.step4.title": "4. Barrera consciente y Launcher Zen",
    "how.step4.desc": "Las apps distractoras se topan con una pantalla de reflexión o quedan recluidas dentro del Launcher Zen. Solo sales al renovar tu intención.",

    /* Adaptive Flow */
    "adaptive.title": "Configuración adaptativa inteligente",
    "adaptive.subtitle": "Sin perfiles predeterminados impuestos. Hermit habilita funciones a medida según tu equipamiento físico.",
    "adaptive.pill.timer": "Sin etiqueta NFC (Modo Temporizador)",
    "adaptive.card1.title": "Sesiones de enfoque por tiempo",
    "adaptive.card1.desc": "Si no has añadido una etiqueta NFC, puedes crear perfiles protegidos por una cuenta atrás estricta.",
    "adaptive.c1.f1": "Temporizador configurable hasta un máximo de 12 horas",
    "adaptive.c1.f2": "Salida de emergencia activa por defecto con 60 s de pausa reflexiva",
    "adaptive.c1.f3": "Opcional: Desactivar la salida de emergencia para un bloqueo irrevocable",
    "adaptive.c1.f4": "Modo Launcher Zen desactivado (requiere NFC físico para salir con seguridad)",
    "adaptive.pill.nfc": "Con etiqueta NFC (Interruptor Físico)",
    "adaptive.card2.title": "Fricción física y Launcher Zen",
    "adaptive.card2.desc": "Vincular una etiqueta física ancla tu compromiso en el mundo real y desbloquea el potencial completo de Hermit.",
    "adaptive.c2.f1": "Interruptor físico universal: Toca para alternar (activar/desactivar)",
    "adaptive.c2.f2": "Desbloquea el Modo Zen: sustituye el launcher del sistema por esenciales OLED",
    "adaptive.c2.f3": "Desactivación exclusiva mediante la etiqueta NFC física",
    "adaptive.c2.f4": "Límite interno de 12 horas que garantiza tu seguridad ante cualquier imprevisto",

    /* Security */
    "security.title": "Seguridad y protección contra desinstalación",
    "security.desc": "En momentos de flaqueza, el impulso de desinstalar la app para acceder a distracciones puede ser muy fuerte. Hermit integra protección como Administrador de Dispositivos para deshabilitar el botón de desinstalación de Android. Desactivar esta protección requiere superar un temporizador de reflexión consciente.",

    /* Features */
    "features.title": "Diseñado para la intención, no para la adicción",
    "features.subtitle": "Todo lo que necesitas para proteger tus sesiones de trabajo profundo sin accesorios propietarios ni suscripciones.",
    "features.f1.title": "Compatibilidad NFC universal",
    "features.f1.desc": "Compatible con etiquetas estándar NFC Forum Tipo 2 (NTAG213, NTAG215, NTAG216) económicas y fáciles de conseguir en cualquier parte.",
    "features.f2.title": "Bloqueo por accesibilidad real",
    "features.f2.desc": "Un genuino servicio de accesibilidad de Android detecta lanzamientos de apps distractoras y URLs de redes sociales de forma limpia y sin necesidad de root.",
    "features.f3.title": "100 % local y privado",
    "features.f3.desc": "Sin cuentas, sin rastreadores y sin servidores en la nube. La conexión a internet solo se utiliza para buscar y descargar actualizaciones desde GitHub Releases; tus hábitos y datos se quedan íntegramente en tu dispositivo.",
    "features.f4.title": "Salida flexible y segura",
    "features.f4.desc": "Elige entre desbloqueo exclusivo por etiqueta NFC física (máximo 12 h por seguridad), temporizador o salida de emergencia opcional con 60 s reflexivos.",
    "features.f5.title": "Launcher Zen y perfiles a medida",
    "features.f5.desc": "Convierte tu pantalla de inicio en un launcher OLED libre de distracciones con solo aplicaciones esenciales (Teléfono, Cámara, Spotify, YT Music y hasta 5 apps elegidas).",
    "features.f6.title": "Historial y estadísticas locales",
    "features.f6.desc": "Registra tus horas totales de enfoque, sesiones completadas y distracciones evitadas en almacenamiento local Room cifrado.",

    /* Hardware */
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

    /* Privacy */
    "privacy.title": "Un escudo de bienestar digital transparente",
    "privacy.subtitle": "A diferencia de las aplicaciones conectadas a la nube, Hermit no recopila tu comportamiento.",
    "privacy.card1.title": "Cero telemetría",
    "privacy.card1.desc": "Hermit no incluye Google Analytics, Firebase ni librerías de publicidad. La única conexión de red que realiza la app es consultar GitHub Releases al comprobar actualizaciones.",
    "privacy.card2.title": "Accesibilidad responsable",
    "privacy.card2.desc": "El servicio de accesibilidad opera con estricto respeto a tu privacidad: jamás lee textos privados, mensajes ni contraseñas. Solo detecta paquetes bloqueados.",
    "privacy.card3.title": "Portabilidad local",
    "privacy.card3.desc": "Exporta tus perfiles y estadísticas en formato JSON estándar en cualquier momento o borra todos los datos con un solo toque.",

    /* Install */
    "install.title": "Instalación y código fuente",
    "install.subtitle": "Hermit es libre, de código abierto y desarrollado íntegramente en Kotlin nativo y Jetpack Compose.",
    "install.build.title": "Compilar desde el código fuente",
    "install.build.step1": "Clona el repositorio desde GitHub:",
    "install.build.step2": "Compila el APK con Gradle:",
    "install.build.step3": "Instala directamente en tu dispositivo mediante ADB:",

    /* FAQ */
    "faq.title": "Preguntas frecuentes",
    "faq.q1": "¿Qué etiquetas NFC debo comprar?",
    "faq.a1": "Cualquier pegatina o tarjeta NTAG213, NTAG215 o NTAG216 estándar funcionará. Las NTAG215 son muy comunes, económicas (habitualmente menos de 0,50 €) y 100 % compatibles.",
    "faq.q2": "¿Por qué Hermit necesita permiso de accesibilidad?",
    "faq.a2": "Android requiere un servicio de accesibilidad para detectar con fiabilidad cuándo se abre una aplicación en primer plano o se navega por redes sociales. Hermit solo revisa el paquete o dominio para mostrar la pantalla de enfoque. Nunca lee contenidos privados, pulsaciones ni datos personales.",
    "faq.q3": "¿Requiere Hermit conexión a internet?",
    "faq.a3": "Únicamente para comprobar e instalar actualizaciones de la aplicación directamente desde GitHub Releases con verificación de integridad SHA-256. Ninguna de las funciones de concentración, lectura/grabación NFC o estadísticas requiere conexión. Hermit tiene cero telemetría, cero analítica y todos tus datos se procesan 100 % en local en tu dispositivo.",
    "faq.q4": "¿Se abre la app automáticamente al acercar la etiqueta si está cerrada?",
    "faq.a4": "Sí. Todas las etiquetas grabadas con Hermit incluyen un registro AAR (Android Application Record) y un filtro prioritario NDEF. Al acercar la etiqueta, incluso con la app cerrada o en segundo plano, Android abre Hermit automáticamente y gestiona tu sesión.",
    "faq.q5": "¿Puedo desbloquear mi teléfono si pierdo la etiqueta NFC?",
    "faq.a5": "Sí. Por seguridad, las sesiones de enfoque tienen un tope máximo de seguridad de 12 horas en todas las circunstancias, y los perfiles pueden configurarse con salida de emergencia con pausa reflexiva de 60 segundos.",
    "faq.q6": "¿Qué es el Launcher Zen y qué aplicaciones se pueden utilizar en él?",
    "faq.a6": "El Launcher Zen es un entorno de inicio minimalista sobre fondo negro OLED. Cuando está activo, pulsar el botón de inicio redirige automáticamente a Zen. Solo muestra Teléfono, Cámara, Spotify y YouTube Music (si están instaladas), y opcionalmente hasta 5 aplicaciones adicionales elegidas por el usuario. Puede durar hasta 12 horas y desactivarlo antes exige obligatoriamente escanear tu etiqueta NFC física.",
    "faq.q7": "¿Puedo utilizar Hermit si todavía no he comprado una etiqueta NFC?",
    "faq.a7": "Por supuesto. Durante el onboarding puedes omitir el paso de añadir etiqueta. Hermit te permitirá crear perfiles con temporizador (hasta 12 horas) y salida de emergencia (activa por defecto tras 60 s de reflexión, pero desactivable para un bloqueo estricto). Puedes vincular una etiqueta NFC en cualquier momento posterior desde la pestaña Etiquetas para habilitar el Launcher Zen.",
    "faq.q8": "¿Puede Hermit bloquear mi teclado, llamadas de emergencia o utilidades del sistema?",
    "faq.a8": "Jamás. El servicio de accesibilidad de Hermit exime expresamente todos los teclados del sistema (IMEs como Gboard), el marcador de llamadas de emergencia, los cuadros de diálogo del sistema y la propia aplicación Hermit. Siempre podrás escribir y realizar llamadas de emergencia con total seguridad.",
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

function updateMetaThemeColor(theme) {
  document.documentElement.style.colorScheme = theme;
  let dynamicMeta = document.querySelector('meta[name="theme-color"][data-dynamic]');
  if (!dynamicMeta) {
    dynamicMeta = document.createElement('meta');
    dynamicMeta.setAttribute('name', 'theme-color');
    dynamicMeta.setAttribute('data-dynamic', 'true');
    document.head.appendChild(dynamicMeta);
  }
  dynamicMeta.setAttribute('content', theme === 'light' ? '#F6F3EC' : '#151614');
}

function initTheme() {
  const savedTheme = localStorage.getItem('hermit_theme');
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const theme = savedTheme || (prefersDark ? 'dark' : 'light');
  document.documentElement.setAttribute('data-theme', theme);
  updateThemeIcon(theme);
  updateMetaThemeColor(theme);
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'dark';
  const target = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', target);
  localStorage.setItem('hermit_theme', target);
  updateThemeIcon(target);
  updateMetaThemeColor(target);
}

function updateThemeIcon(theme) {
  const btn = document.getElementById('theme-toggle');
  if (btn) {
    btn.innerHTML = theme === 'dark'
      ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>`
      : `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>`;
    btn.setAttribute('aria-label', theme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
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

