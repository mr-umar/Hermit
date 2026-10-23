# Hermit Product Website

This directory contains the standalone static product website for **Hermit** (`https://hermit.umarmohammad.xyz`), deployed via **GitHub Pages**.

---

## Architecture & Technology Stack

- **Core:** Pure HTML5, Vanilla JavaScript, and Modern CSS.
- **Hosting & Deployment:** GitHub Pages automated via GitHub Actions (`.github/workflows/deploy-pages.yml`).
- **Custom Domain:** `hermit.umarmohammad.xyz` configured via `CNAME` targeting `mr-umar.github.io`.
- **Design Foundations:** Monastic calm aesthetic using custom design tokens:
  - Translucent navigation chrome with `backdrop-filter: blur(20px)`.
  - Spring-inspired tactile press feedback on interactive elements.
  - Dark and light theme modes with system detection and manual override.
  - Optical typographic hierarchy with clamp sizing and negative tracking on headings.
  - Accessibility-first: WCAG 2.2 AA contrast, responsive touch targets, keyboard navigation, and reduced-motion support.
- **Zero Runtime Dependencies:** No external frameworks, build tools, databases, analytics, or trackers.
- **Bilingual Localization:** Complete English and Spanish support with instant in-browser language switching and locale preservation.

---

## Directory Structure

```
website/
├── index.html           # Main bilingual landing page
├── CNAME                # Custom domain identifier for GitHub Pages (hermit.umarmohammad.xyz)
├── css/
│   └── styles.css       # Responsive CSS design system with monastic color tokens
├── js/
│   └── app.js           # Bilingual i18n, theme management & accordions
├── assets/
│   ├── favicon.svg      # Monk holding NFC tag circular favicon
│   ├── hermit-icon.svg  # Master monk vector graphic
│   ├── hermit-logo.svg  # Header brand logo
│   ├── mockup-app.svg   # Vector mockup of Active Focus screen
│   └── mockup-block.svg # Vector mockup of Mindful Block barrier
├── sitemap.xml          # Search engine sitemap with hreflang tags
├── robots.txt           # Crawler instructions
├── site.webmanifest     # Web App Manifest for mobile browsers
├── DNS.md               # Detailed DNS configuration guide (CNAME)
├── DEPLOY.md            # GitHub Pages deployment and custom domain setup guide
└── README.md            # This documentation file
```

---

## Local Development

Preview the site locally using Python's built-in web server:

```bash
python -m http.server 8080 --directory website
```

Open [http://localhost:8080](http://localhost:8080) in your browser.

---

## Deployment Documentation

- [Deployment Guide for GitHub Pages](DEPLOY.md)
- [DNS Configuration Guide](DNS.md)
