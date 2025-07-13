# 🚀 Render Deployment Guide (Free Alternative)

## Why Render?
- ✅ **Free tier**: 750 hours/month
- ✅ **Auto-deploy**: From GitHub
- ✅ **SSL**: Automatic HTTPS
- ✅ **Easy setup**: Just connect GitHub
- ✅ **Cost**: $0 for 90 days, then $7/month

## Step 1: Prepare Your Code

### 1.1 Push to GitHub
```bash
git add .
git commit -m "Prepare for Render deployment"
git push origin main
```

### 1.2 Verify Files
- ✅ `server/package.json` (with `"start": "node index.js"`)
- ✅ `server/index.js` (main server file)
- ✅ `server/src/config/database.js` (updated for external DB)
- ✅ `server/render.yaml` (Render config)

## Step 2: Deploy to Render

### 2.1 Create Render Account
1. Go to [render.com](https://render.com)
2. Sign in with GitHub
3. Click "New" → "Web Service"

### 2.2 Connect GitHub
1. Select your QuantumLift repository
2. Render will auto-detect Node.js project
3. Configure settings:
   - **Name**: `quantumlift-api`
   - **Environment**: `Node`
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`

### 2.3 Set Environment Variables
Add these in Render dashboard:
```env
NODE_ENV=production
JWT_SECRET=your_super_secret_jwt_key_here
JWT_EXPIRES_IN=7d
DATABASE_URL=your_planetscale_url_here
PORT=3000
```

### 2.4 Deploy
1. Click "Create Web Service"
2. Render will build and deploy automatically
3. Get your HTTPS URL (e.g., `https://quantumlift-api.onrender.com`)

## Step 3: Set Up PlanetScale Database

### 3.1 Create PlanetScale Account
1. Go to [planetscale.com](https://planetscale.com)
2. Sign up with GitHub
3. Create new database

### 3.2 Get Database URL
1. In PlanetScale dashboard
2. Go to your database
3. Click "Connect" → "Connect with Prisma"
4. Copy the connection string

### 3.3 Update Render Environment
1. Go back to Render dashboard
2. Add `DATABASE_URL` with your PlanetScale connection string

## Step 4: Initialize Database

### 4.1 Update Database Config
Make sure `server/src/config/database.js` supports MySQL:
```javascript
const { Pool } = require('pg');
// For PlanetScale (MySQL), you might need mysql2 instead
// const mysql = require('mysql2/promise');
```

### 4.2 Run Schema
Use PlanetScale's web interface or CLI to run your schema.

## Step 5: Update Android App

### 5.1 Update Production URL
Replace in `AuthRepository.kt`:
```kotlin
val baseUrl = if (BuildConfig.DEBUG) {
    "http://192.168.0.76:3000/" // Development
} else {
    "https://your-app-name.onrender.com/" // Production
}
```

### 5.2 Update Network Security Config
Add to `network_security_config.xml`:
```xml
<domain includeSubdomains="true">your-app-name.onrender.com</domain>
```

## Step 6: Test Deployment

### 6.1 Test API
```bash
# Health check
curl https://your-app-name.onrender.com/health

# Test registration
curl -X POST https://your-app-name.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"password123"}'
```

### 6.2 Test Android App
1. Build release APK
2. Install on phone
3. Test login/registration

## Cost Comparison

| Platform | Free Period | After Free | Database |
|----------|-------------|------------|----------|
| **Railway** | 30 days | $5/month | Included |
| **Render** | 90 days | $7/month | External |
| **Vercel** | Forever | $0 | External |
| **Fly.io** | Forever | $0 | Included |

## Recommendation
**Start with Render** - you get 90 days free, then decide if you want to:
1. **Pay $7/month** for Render
2. **Switch to Vercel** (free forever)
3. **Switch to Fly.io** (free forever)

Your app will be live and accessible from anywhere! 🌐 