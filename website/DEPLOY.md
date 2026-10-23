# Deployment Guide for hermit.umarmohammad.xyz

This guide provides step-by-step instructions for deploying the **Hermit** product website to a Linux server running Nginx with automated Let's Encrypt TLS certificates.

---

## 1. Prerequisites

- A Linux server (Ubuntu 22.04 / 24.04 LTS, Debian 12, or AlmaLinux/Rocky 9).
- Root or `sudo` administrative access on the server.
- Nginx web server installed: `sudo apt update && sudo apt install -y nginx`.
- Certbot installed: `sudo apt install -y certbot python3-certbot-nginx`.
- DNS record configured for `hermit.umarmohammad.xyz` (see [DNS.md](DNS.md)).

---

## 2. Local Development

The website is a clean, static, dependency-free web application. You can preview it locally using any standard static file server:

### Python (Built-in)
```bash
cd website
python -m http.server 8080
```
Open your browser to: [http://localhost:8080](http://localhost:8080)

### Node.js (Optional)
```bash
npx serve website
```

---

## 3. Production Deployment Steps

### Step 3.1: Prepare Server Web Directory
On your remote server, create the document root and set appropriate permissions:
```bash
sudo mkdir -p /var/www/hermit
sudo chown -R www-data:www-data /var/www/hermit
sudo chmod -R 755 /var/www/hermit
```

### Step 3.2: Transfer Website Files
From your local development machine, synchronize the website assets using `rsync` or `scp`:
```bash
# Using rsync (recommended)
rsync -avz --delete website/ user@your-server-ip:/var/www/hermit/

# Or using scp
scp -r website/* user@your-server-ip:/var/www/hermit/
```

### Step 3.3: Install Nginx Server Block
Copy the provided `website/nginx.conf` to Nginx's sites directory:
```bash
# Copy configuration to sites-available
sudo cp /var/www/hermit/nginx.conf /etc/nginx/sites-available/hermit.umarmohammad.xyz

# Enable site via symlink
sudo ln -sf /etc/nginx/sites-available/hermit.umarmohammad.xyz /etc/nginx/sites-enabled/
```

### Step 3.4: Obtain TLS Certificate with Let's Encrypt
Run Certbot to request and configure an SSL/TLS certificate for the subdomain:
```bash
sudo certbot --nginx -d hermit.umarmohammad.xyz
```
Certbot will verify ownership, generate the certificates at `/etc/letsencrypt/live/hermit.umarmohammad.xyz/`, and configure the SSL directives.

### Step 3.5: Test and Reload Nginx
Verify that the Nginx configuration syntax is valid:
```bash
sudo nginx -t
```
If the test succeeds (`syntax is ok` and `test is successful`), reload Nginx:
```bash
sudo systemctl reload nginx
```

---

## 4. Automated Certificate Renewal

Certbot automatically configures a systemd timer or cron job for renewal. You can verify renewal automation with a dry run:
```bash
sudo certbot renew --dry-run
```

---

## 5. Verification Checklist

After deployment, confirm the following:

- [ ] **HTTPS Redirection:** `http://hermit.umarmohammad.xyz` automatically redirects to `https://hermit.umarmohammad.xyz`.
- [ ] **TLS Certificate:** Certificate is valid, issued to `hermit.umarmohammad.xyz`, and has a modern cipher suite.
- [ ] **Security Headers:** Response includes HSTS, CSP, X-Frame-Options, and X-Content-Type-Options.
- [ ] **Bilingual Switcher:** Toggling the language button in the header switches between English and Spanish seamlessly.
- [ ] **Theme Switcher:** Toggling the theme button switches between Dark and Light modes and persists preference.
- [ ] **Static Assets:** Favicon (`assets/favicon.svg`), mockups, CSS, and JS files load with HTTP 200 and long-term cache headers.
- [ ] **Sitemap & Robots:** Accessible at `/sitemap.xml` and `/robots.txt`.

---

## 6. Troubleshooting

| Issue | Likely Cause | Solution |
| :--- | :--- | :--- |
| **DNS Not Resolving / NXDOMAIN** | DNS record hasn't propagated | Check resolution with `dig hermit.umarmohammad.xyz +short`. Wait for DNS TTL to expire. |
| **403 Forbidden** | Incorrect file permissions | Run `sudo chown -R www-data:www-data /var/www/hermit` and `sudo chmod -R 755 /var/www/hermit`. |
| **404 Not Found on Subpaths** | Missing `try_files` fallback | Ensure `try_files $uri $uri/ /index.html;` is present in `nginx.conf`. |
| **Certbot Verification Failure** | Port 80 blocked or DNS not pointing to server | Ensure firewall allows port 80/443 (`sudo ufw allow 'Nginx Full'`) and DNS points to this server's public IP. |
| **Styles / Scripts Not Updating** | Browser or CDN caching | Assets have cache headers. Hard refresh with `Ctrl + Shift + R` or `Cmd + Shift + R`. |
