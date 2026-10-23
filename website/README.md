# Hermit Product Website

This directory contains the standalone, lightweight static product website for **Hermit** (`https://hermit.umarmohammad.xyz`).

---

## Architecture & Technology Stack

- **Core:** Pure HTML5, Vanilla JavaScript, and Modern CSS.
- **Design Foundations:** Guided by Apple Human Interface Guidelines and Material 3 design tokens:
  - Translucent navigation chrome with `backdrop-filter: blur(20px)`.
  - Spring-inspired tactile press feedback on interactive elements.
  - Dark and light theme modes with system detection and manual override.
  - Optical typographic hierarchy with clamp sizing and negative tracking on headings.
  - Accessibility-first: WCAG 2.2 AA contrast, responsive touch targets, keyboard navigation, and reduced-motion support.
- **Zero Runtime Dependencies:** No external frameworks, databases, analytics, or trackers.
- **Bilingual Localization:** Complete English and Spanish support with instant in-browser language switching and locale preservation.

---

## Directory Structure

```
website/
├── index.html           # Main bilingual landing page
├── css/
│   └── styles.css       # Responsive, Apple-inspired CSS design system
├── js/
│   └── app.js           # Bilingual i18n, theme management & accordions
├── assets/
│   ├── favicon.svg      # Hermit circular logo favicon
│   ├── hermit-logo.svg  # Header brand logo
│   ├── mockup-app.svg   # Vector mockup of Active Focus screen
│   └── mockup-block.svg # Vector mockup of Mindful Block barrier
├── sitemap.xml          # Search engine sitemap with hreflang tags
├── robots.txt           # Crawler instructions
├── site.webmanifest     # Web App Manifest for mobile browsers
├── nginx.conf           # Production Nginx server block with TLS & security headers
├── DNS.md               # Detailed DNS configuration guide (CNAME)
├── DEPLOY.md            # Linux server deployment and Let's Encrypt guide
└── README.md            # This documentation file
```

---

## Local Development

Preview the site locally using Python's built-in web server:

```bash
cd website
python -m http.server 8080
```

Open [http://localhost:8080](http://localhost:8080) in your browser.

---

## Documentation Links

- [DNS Configuration Guide](DNS.md)
- [Server Deployment & Nginx Guide](DEPLOY.md)
