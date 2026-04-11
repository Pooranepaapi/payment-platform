# Deploying the Backend to Oracle Cloud Always Free

Oracle hosts the backend services (Spring Boot API, simulator, PostgreSQL).
The frontend is deployed separately on Vercel or Cloudflare Pages — see FRONTEND_DEPLOY.md.

Zero-cost hosting on Oracle's Ampere ARM VM. Backend services run via Docker Compose.

---

## 1. Create an Oracle Cloud Account

1. Go to https://cloud.oracle.com and sign up (credit card required for identity verification, but Always Free resources are never charged)
2. Choose your home region — pick one close to your users (e.g. Mumbai `ap-mumbai-1` for India)

---

## 2. Create an Always Free VM

1. In the Oracle Console, go to **Compute → Instances → Create Instance**
2. Change the image to **Ubuntu 22.04** (under "Oracle Linux" dropdown, pick Canonical Ubuntu)
3. Under **Shape**, click "Change Shape":
   - Select **Ampere** (ARM-based)
   - Choose `VM.Standard.A1.Flex`
   - Set **4 OCPUs** and **24 GB RAM** (maximum free allowance)
4. Under **Networking**, make sure a public subnet is selected and **Assign a public IPv4 address** is checked
5. Under **Add SSH keys**, upload your public key (`~/.ssh/id_rsa.pub`) or generate a new pair
6. Click **Create**

Note your instance's **Public IP address** once it's running.

---

## 3. Open Firewall Ports

Oracle has two layers of firewall. You need to open port 80 in both.

### Security List (Oracle's cloud firewall)
1. Go to **Networking → Virtual Cloud Networks → your VCN → Security Lists**
2. Add an **Ingress Rule**:
   - Source CIDR: `0.0.0.0/0`
   - IP Protocol: TCP
   - Destination Port: `80`
3. Also add port `443` for HTTPS later

### OS-level firewall (on the VM itself)
```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

---

## 4. Install Docker on the VM

```bash
ssh ubuntu@<YOUR_VM_PUBLIC_IP>

# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker ubuntu

# Install Docker Compose plugin
sudo apt-get install -y docker-compose-plugin

# Re-login for group change to take effect
exit && ssh ubuntu@<YOUR_VM_PUBLIC_IP>

# Verify
docker compose version
```

---

## 5. Clone and Configure the App

```bash
# Clone the repo
git clone https://github.com/Pooranepaapi/payment-platform.git
cd payment-platform

# Set required environment variables
export DB_PASSWORD=$(openssl rand -base64 24)   # strong random password
export APP_BASE_URL=http://<YOUR_VM_PUBLIC_IP>

# Save them so they persist across reboots
echo "export DB_PASSWORD='$DB_PASSWORD'" >> ~/.bashrc
echo "export APP_BASE_URL='$APP_BASE_URL'" >> ~/.bashrc
```

---

## 6. Build and Start (Backend Only)

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

This starts:
- `postgres` — database (internal only, port not exposed)
- `backend` — Spring Boot API on port 8080 (internal only)
- `simulator` — PSP simulator on port 8181 (internal only)
- `api-gateway` — nginx on port 80 (the only public-facing container)

The `frontend` service is skipped automatically (it has `profiles: [local]` in prod).

> **After this:** Deploy your frontend to Vercel/Cloudflare (see FRONTEND_DEPLOY.md)
> and set `VITE_API_BASE=http://<YOUR_VM_PUBLIC_IP>` in the frontend env settings.

Check everything is running:
```bash
docker compose ps
docker compose logs backend --tail=50
```

---

## 7. Verify

Test the API gateway is working:
```bash
# Health check
curl http://<YOUR_VM_PUBLIC_IP>/health

# API reachable
curl http://<YOUR_VM_PUBLIC_IP>/api/v1/payments
# Expected: 405 Method Not Allowed (correct — GET not supported, but backend is reachable)
```

Then open your Vercel/Cloudflare frontend URL in the browser to verify the full flow.

---

## 8. Keeping It Running

The `restart: always` policy in `docker-compose.prod.yml` means containers auto-start after VM reboots. To make Docker itself start on boot:

```bash
sudo systemctl enable docker
```

---

## 9. Useful Commands

```bash
# View logs
docker compose logs -f backend
docker compose logs -f frontend

# Restart a service
docker compose restart backend

# Pull latest code and redeploy
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build

# Stop everything
docker compose down

# Stop and wipe DB (fresh start)
docker compose down -v
```

---

## 10. Adding a Domain (Later)

Once you have a domain pointed at the VM's IP:

1. Install Certbot for free HTTPS:
```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d yourdomain.com
```

2. Update `APP_BASE_URL`:
```bash
export APP_BASE_URL=https://yourdomain.com
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

---

## Architecture

```
                    Oracle VM (port 80 only)
                    ┌──────────────────────────────────────────┐
Internet ─port 80──▶│  nginx api-gateway                       │
                    │    /api/*  ──▶ backend:8080               │
                    │    /health ──▶ 200 OK                     │
                    │                    └──▶ simulator:8181    │
                    │                    └──▶ postgres:5432     │
                    └──────────────────────────────────────────┘
                              ▲
                    VITE_API_BASE points here
                              │
              Vercel/Cloudflare (React SPA, CDN)
                    ◀──── User browser
```

Only port 80 is public. All other ports are internal to Docker's network.
