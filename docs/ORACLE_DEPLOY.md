# Deploying to Oracle Cloud Always Free

Zero-cost hosting on Oracle's Ampere ARM VM. Everything runs via Docker Compose.

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

## 6. Build and Start

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

This will:
- Build all 3 service images (takes ~5-10 min on first run)
- Start postgres, backend, simulator, frontend
- Expose only port 80 publicly (nginx reverse proxies /api/ to backend internally)

Check everything is running:
```bash
docker compose ps
docker compose logs backend --tail=50
```

---

## 7. Verify

Open `http://<YOUR_VM_PUBLIC_IP>` in your browser — you should see the merchant checkout page.

To test the API directly:
```bash
curl http://<YOUR_VM_PUBLIC_IP>/api/v1/payments
```

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

## Architecture on the VM

```
Internet → port 80 → nginx (frontend container)
                         ├── /          → React SPA (static files)
                         └── /api/      → proxy → backend:8080 (internal)
                                                       └── simulator:8181 (internal)
                                                       └── postgres:5432  (internal)
```

Only port 80 is exposed. All other services communicate on Docker's internal network.
