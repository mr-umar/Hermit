# DNS Configuration Guide for hermit.umarmohammad.xyz

This document details the Domain Name System (DNS) configuration required to route traffic for **`hermit.umarmohammad.xyz`** to **GitHub Pages**.

---

## 1. Required DNS Record

In your DNS registrar or DNS management console (e.g., Cloudflare, Namecheap, AWS Route 53, Porkbun, Hetzner, etc.), create a single `CNAME` record with the following values:

| Field | Value | Explanation |
| :--- | :--- | :--- |
| **Record Type** | `CNAME` | Canonical Name alias record |
| **Host / Name** | `hermit` | Subdomain label (`hermit.umarmohammad.xyz`) |
| **Target / Value** | `mr-umar.github.io` | GitHub Pages hostname for this repository |
| **TTL** | `Auto` (or `300` / `3600`) | Standard Time-to-Live |

> [!IMPORTANT]
> The target hostname (`mr-umar.github.io`) must be verified against GitHub's current Pages configuration for your repository (`https://github.com/mr-umar/Hermit/settings/pages`). Do not include any trailing dots, paths, or protocols (`https://`) in the target value.

---

## 2. Understanding CNAME Rules & Conflict Resolution

### 2.1. CNAME Exclusivity (RFC 1034)
Under **RFC 1034** (Section 3.6.2), a `CNAME` record cannot coexist with any other record at the exact same hostname label:
- If `hermit.umarmohammad.xyz` has a `CNAME` record, you **cannot** have an `A`, `AAAA`, `TXT`, or `MX` record at `hermit`.
- **Action Required:** Before adding the `CNAME` record, delete any pre-existing records for `hermit` (e.g., legacy VPS IP `A` records or test records) to prevent DNS lookup conflicts.

### 2.2. Proxy Status (If Using Cloudflare)
If you manage DNS through Cloudflare:
- Set the proxy status to **DNS only** (gray cloud) during initial verification and Let's Encrypt certificate issuance by GitHub.
- Once GitHub Pages verifies the domain and issues the TLS certificate, you may enable Cloudflare proxying if desired.

---

## 3. GitHub Pages Verification & HTTPS Provisioning

GitHub Pages automates TLS certificate creation and renewal:

1. **DNS Propagation:** After saving the record in your DNS provider, wait for propagation (usually 5–30 minutes).
2. **GitHub Domain Check:** In the repository under **Settings > Pages > Custom domain**, GitHub automatically checks that `hermit.umarmohammad.xyz` resolves to `mr-umar.github.io`.
3. **Automated TLS Issuance:** GitHub requests a free cryptographic certificate from Let's Encrypt.
4. **Enforce HTTPS:** Once the certificate has been provisioned, select **"Enforce HTTPS"** in GitHub Pages settings to redirect all HTTP traffic to HTTPS.

---

## 4. Verification Commands

Use these terminal commands to verify your DNS propagation and HTTPS connectivity:

### 4.1. Verify CNAME Resolution
```bash
# Query the CNAME record directly
dig CNAME hermit.umarmohammad.xyz +short

# Windows PowerShell alternative
Resolve-DnsName -Name hermit.umarmohammad.xyz -Type CNAME
```
*Expected output: `mr-umar.github.io.`*

### 4.2. Verify Web Response
```bash
curl -I https://hermit.umarmohammad.xyz
```
*Expected response: `HTTP/2 200` with GitHub Pages headers.*

### 4.3. Verify HTTP to HTTPS Redirection
```bash
curl -I http://hermit.umarmohammad.xyz
```
*Expected response: `HTTP/1.1 301 Moved Permanently`.*
