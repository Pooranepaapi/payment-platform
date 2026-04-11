# Deploying the Frontend (Vercel or Cloudflare Pages)

The React frontend is a static build — deploy it free on Vercel or Cloudflare Pages.
The backend runs separately on Oracle Cloud (see ORACLE_DEPLOY.md).

---

## Architecture

```
User → Vercel/Cloudflare (React SPA, CDN)
              ↓ API calls to VITE_API_BASE
       Oracle VM → nginx (port 80) → backend:8080 → simulator:8181
                                          └──────────→ postgres:5432
```

---

## Option A — Vercel (Recommended, simpler)

### 1. Connect your repo

1. Go to https://vercel.com and sign in with GitHub
2. Click **Add New → Project**
3. Import `Pooranepaapi/payment-platform`
4. Set **Root Directory** to `frontend`
5. Framework preset will auto-detect as **Vite** ✓

### 2. Set environment variable

Under **Environment Variables**, add:

| Name | Value |
|------|-------|
| `VITE_API_BASE` | `http://<YOUR_ORACLE_VM_IP>` |

> Update this to `https://api.yourdomain.com` once you set up a domain.

### 3. Deploy

Click **Deploy**. Vercel builds `npm run build` and serves the `dist/` folder from its CDN.

Your frontend URL will be something like `https://payment-platform-xxx.vercel.app`.

### Redeploying

Every `git push` to `master` auto-triggers a redeploy on Vercel. No manual steps needed.

---

## Option B — Cloudflare Pages

### 1. Connect your repo

1. Go to https://pages.cloudflare.com
2. Click **Create a project → Connect to Git**
3. Select `Pooranepaapi/payment-platform`
4. Configure the build:
   - **Root directory**: `frontend`
   - **Build command**: `npm run build`
   - **Build output directory**: `dist`

### 2. Set environment variable

Under **Environment variables (advanced)**, add:

| Name | Value |
|------|-------|
| `VITE_API_BASE` | `http://<YOUR_ORACLE_VM_IP>` |

### 3. Deploy

Click **Save and Deploy**. Your URL will be `https://payment-platform.pages.dev`.

---

## Updating the API base URL (after domain setup)

Once you have a domain pointed at Oracle and HTTPS set up:

1. Go to your Vercel/Cloudflare project settings
2. Update `VITE_API_BASE` to `https://api.yourdomain.com` (or `https://yourdomain.com`)
3. Trigger a redeploy (Vercel: push a commit or click Redeploy)

---

## Local development

For local dev, the env var is set in `frontend/.env.local` (gitignored):

```
VITE_API_BASE=http://localhost:8080
```

This file is already created in the repo. Just run:

```bash
cd frontend
npm install
npm run dev
```
