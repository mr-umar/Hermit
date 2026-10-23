# DNS Configuration Guide for hermit.umarmohammad.xyz

This document details the Domain Name System (DNS) configuration required to route traffic for **`hermit.umarmohammad.xyz`** to your web server hosting the Hermit product website.

---

## 1. Example DNS Configuration

In your DNS provider's management console (e.g., Cloudflare, Namecheap, Route 53, Porkbun, Hetzner, etc.), add the following record:

| Field | Value | Notes |
| :--- | :--- | :--- |
| **Record Type** | `CNAME` | Canonical Name alias record |
| **Name / Host** | `hermit` | Subdomain label (`hermit.umarmohammad.xyz`) |
| **Target / Value** | `umarmohammad.xyz` | Hostname of the target server |
| **TTL** | `Auto` (or `300` / `3600`) | Time-to-Live |

> [!IMPORTANT]
> The target hostname (`umarmohammad.xyz`) must resolve to a server configured to serve requests for `hermit.umarmohammad.xyz`. If your hosting provider provides an explicit canonical hostname (such as an ingress controller or cloud load balancer), specify that provider-supplied target instead.

---

## 2. Understanding CNAME Rules and Constraints

### 2.1. CNAME Exclusivity
According to **RFC 1034** (Section 3.6.2), a `CNAME` record cannot coexist with other DNS records at the exact same node name:
- If `hermit.umarmohammad.xyz` has a `CNAME` record, you **cannot** have an `A`, `AAAA`, `MX`, or `TXT` record at `hermit`.
- Before adding the `CNAME` record, ensure any pre-existing records for `hermit` are removed to prevent DNS conflicts.

### 2.2. When to Use an `A` or `AAAA` Record Instead
If your server has a dedicated, static public IP address and you do not route traffic through another canonical hostname, configuring direct address records is often preferable:

- **IPv4 (`A` Record):**
  - Host: `hermit`
  - Value: `YOUR_SERVER_IPV4_ADDRESS` (e.g., `198.51.100.24`)
- **IPv6 (`AAAA` Record):**
  - Host: `hermit`
  - Value: `YOUR_SERVER_IPV6_ADDRESS` (e.g., `2001:db8::1`)

---

## 3. The Three Pillars of Production Deployment

Configuring DNS is only the first of three distinct steps required for a live, secure website:

1. **DNS Resolution:** Maps the domain name to the server's network address.
2. **Web Server Configuration (Nginx):** The web server must listen for `server_name hermit.umarmohammad.xyz` and route incoming HTTP/HTTPS requests to the local website directory (`/var/www/hermit`).
3. **TLS Certificate Issuance:** An authorized Certificate Authority (e.g., Let's Encrypt via Certbot) must issue a cryptographic TLS certificate matching `hermit.umarmohammad.xyz`.

---

## 4. Verification Commands

Once configured, verify propagation and server response using the following terminal commands:

### 4.1. Verify DNS Resolution (CNAME & IP)
```bash
# Query the CNAME record directly
dig CNAME hermit.umarmohammad.xyz +short

# Query the resolved IPv4 address
dig A hermit.umarmohammad.xyz +short

# Windows alternative
nslookup -type=CNAME hermit.umarmohammad.xyz
nslookup hermit.umarmohammad.xyz
```

### 4.2. Verify HTTP to HTTPS Redirection
```bash
curl -I http://hermit.umarmohammad.xyz
```
*Expected response: `HTTP/1.1 301 Moved Permanently` pointing to `https://hermit.umarmohammad.xyz/`.*

### 4.3. Verify HTTPS Response and Headers
```bash
curl -I https://hermit.umarmohammad.xyz
```
*Expected response: `HTTP/2 200` with `Strict-Transport-Security`, `X-Content-Type-Options`, and `Content-Security-Policy` headers.*

### 4.4. Verify SSL/TLS Certificate Validity
```bash
openssl s_client -connect hermit.umarmohammad.xyz:443 -servername hermit.umarmohammad.xyz < /dev/null | openssl x509 -noout -dates -subject
```
