# GitHub Pages Deployment Guide

This guide documents the deployment strategy for the **Hermit** product website using **GitHub Pages** with the custom domain **`hermit.umarmohammad.xyz`**.

The website is a static, zero-dependency site (HTML5, Vanilla JavaScript, CSS, SVG assets) deployed directly from the `website/` directory via GitHub Actions.

---

## 1. Architecture Overview

- **Hosting Platform:** GitHub Pages (Static edge hosting).
- **Custom Domain:** `hermit.umarmohammad.xyz`
- **DNS Target:** `mr-umar.github.io` (GitHub Pages canonical host for this account).
- **Deployment Mechanism:** GitHub Actions workflow (`.github/workflows/deploy-pages.yml`).
- **Domain Mapping:** Plain-text `CNAME` file located in `website/CNAME`.
- **TLS / HTTPS:** Automated certificate provisioning and renewal handled by GitHub via Let's Encrypt.

---

## 2. DNS Configuration for Custom Domain

To route requests for `hermit.umarmohammad.xyz` to GitHub Pages, configure the following DNS record in your DNS provider's control panel (e.g., Cloudflare, Namecheap, AWS Route 53, Porkbun):

| Field | Value | Explanation |
| :--- | :--- | :--- |
| **Record Type** | `CNAME` | Canonical Name alias record |
| **Host / Name** | `hermit` | Subdomain label (`hermit.umarmohammad.xyz`) |
| **Target / Value** | `mr-umar.github.io` | GitHub Pages hostname for the account |
| **TTL** | `Auto` (or `300` / `3600`) | Standard propagation time |

### Critical DNS Constraints

1. **Remove Conflicting Records:** According to RFC 1034 (Section 3.6.2), a `CNAME` record cannot coexist with other DNS records for the exact same host. Before adding the `CNAME` record, remove any pre-existing `A`, `AAAA`, `TXT`, or `MX` records assigned to the `hermit` host.
2. **Verify Target Hostname:** Verify that `mr-umar.github.io` matches the GitHub Pages hostname specified in your GitHub account.
3. **DNS Propagation:** DNS changes may take between a few minutes and 24–48 hours to propagate globally depending on your registrar's TTL settings.

---

## 3. GitHub Repository Settings

To configure and activate GitHub Pages for this repository:

1. Navigate to your repository on GitHub: `https://github.com/mr-umar/Hermit`.
2. Click on **Settings** (top navigation bar).
3. In the left sidebar under the "Code and automation" section, click **Pages**.
4. **Source Selection:**
   - Under **Build and deployment** > **Source**, select **GitHub Actions**.
   - The workflow located at [`.github/workflows/deploy-pages.yml`](../.github/workflows/deploy-pages.yml) will build and deploy the `website/` directory automatically whenever changes are pushed to `main`.
5. **Custom Domain:**
   - Under **Custom domain**, enter:
     ```
     hermit.umarmohammad.xyz
     ```
   - Click **Save**.
   - GitHub will check the DNS record. The `website/CNAME` file included in the repository will ensure this setting is permanently preserved across future deployments.
6. **Enforce HTTPS:**
   - Once GitHub completes the DNS check and issues the TLS certificate (typically within 10–60 minutes after DNS resolves), check the box **Enforce HTTPS**.
   - This ensures all insecure HTTP traffic automatically redirects to HTTPS.

> [!NOTE]
> DNS records must be created in your domain registrar, and the domain check in GitHub Pages settings must succeed before HTTPS can be enforced. Do not attempt to enforce HTTPS before the DNS check completes.

---

## 4. GitHub Actions Deployment Workflow

The deployment workflow is defined in [`.github/workflows/deploy-pages.yml`](../.github/workflows/deploy-pages.yml):

- **Trigger:** Triggers automatically on any push to `main` modifying files in `website/**` or the workflow itself, as well as on manual trigger (`workflow_dispatch`).
- **Artifact:** Uploads the `website/` directory directly, ensuring `index.html` and `CNAME` reside at the published root.
- **Environment:** Deploys to the protected `github-pages` environment with URL reporting.

---

## 5. Local Development & Preview

You can test and preview the website locally before pushing changes:

### Using Python (Built-in)
```bash
python -m http.server 8080 --directory website
```
Open your browser to: [http://localhost:8080](http://localhost:8080)

### Using Node.js (Optional)
```bash
npx serve website
```

---

## 6. Verification Commands

Once you have added the DNS record and initiated the GitHub Pages deployment, verify the live setup using your terminal:

### 1. Verify DNS CNAME Resolution
```bash
# Query CNAME record
dig CNAME hermit.umarmohammad.xyz +short

# Windows PowerShell alternative
Resolve-DnsName -Name hermit.umarmohammad.xyz -Type CNAME
```
*Expected output: `mr-umar.github.io.`*

### 2. Verify HTTPS Response and Header Status
```bash
curl -I https://hermit.umarmohammad.xyz
```
*Expected response: `HTTP/2 200` with GitHub Pages headers.*

### 3. Verify HTTP to HTTPS Redirection
```bash
curl -I http://hermit.umarmohammad.xyz
```
*Expected response: `HTTP/1.1 301 Moved Permanently`.*
