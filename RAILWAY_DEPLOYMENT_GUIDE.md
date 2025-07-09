# 🚀 Railway Deployment Guide for GymTracker

## Prerequisites
- GitHub account
- Railway account (free at railway.app)
- Your code pushed to GitHub

## Step 1: Prepare Your Repository

### 1.1 Push to GitHub
```bash
git add .
git commit -m "Prepare for Railway deployment"
git push origin main
```

### 1.2 Verify Files
Make sure these files are in your repository:
- ✅ `server/package.json` (with `"start": "node index.js"`)
- ✅ `server/index.js` (main server file)
- ✅ `server/src/config/database.js` (updated for Railway)
- ✅ `server/railway.json` (Railway config)

## Step 2: Deploy to Railway

### 2.1 Create Railway Account
1. Go to [railway.app](https://railway.app)
2. Sign in with GitHub
3. Click "New Project"

### 2.2 Deploy from GitHub
1. Select "Deploy from GitHub repo"
2. Choose your GymTracker repository
3. Railway will auto-detect Node.js project

### 2.3 Add PostgreSQL Database
1. In your Railway project dashboard
2. Click "New" → "Database" → "PostgreSQL"
3. Railway will provide `DATABASE_URL` automatically

### 2.4 Configure Environment Variables
Add these variables in Railway dashboard:

```env
NODE_ENV=production
JWT_SECRET=your_super_secret_jwt_key_here_change_this
JWT_EXPIRES_IN=7d
PORT=3000
```

### 2.5 Deploy
1. Railway will automatically build and deploy
2. Wait for deployment to complete
3. Get your HTTPS URL (e.g., `https://gymtracker-production.up.railway.app`)

## Step 3: Initialize Database

### 3.1 Connect to Railway Database
1. In Railway dashboard, go to your PostgreSQL database
2. Click "Connect" → "Connect with psql"
3. Copy the connection command

### 3.2 Run Database Initialization
```bash
# Use the connection string from Railway
psql "your-railway-database-url"

# Run the schema
\i server/src/config/schema.sql
```

## Step 4: Update Android App

### 4.1 Update Production URL
Replace the URL in `AuthRepository.kt`:
```kotlin
val baseUrl = if (BuildConfig.DEBUG) {
    "http://192.168.0.76:3000/" // Development
} else {
    "https://your-actual-railway-url.railway.app/" // Production
}
```

### 4.2 Update Network Security Config
Add your Railway domain to `network_security_config.xml`:
```xml
<domain includeSubdomains="true">your-app-name.railway.app</domain>
```

## Step 5: Test Deployment

### 5.1 Test API Endpoints
```bash
# Health check
curl https://your-app-name.railway.app/health

# Test registration
curl -X POST https://your-app-name.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"password123"}'
```

### 5.2 Test Android App
1. Build release APK
2. Install on phone
3. Test login/registration

## Troubleshooting

### Common Issues:
1. **Database connection failed**: Check `DATABASE_URL` in Railway
2. **Build failed**: Check `package.json` has correct `start` script
3. **Environment variables**: Make sure all required vars are set in Railway
4. **SSL issues**: Railway provides SSL automatically

### Railway Dashboard Features:
- ✅ **Real-time logs**: See what's happening
- ✅ **Environment variables**: Easy configuration
- ✅ **Database management**: PostgreSQL included
- ✅ **Custom domains**: Add your own domain
- ✅ **Auto-deploy**: Deploy on every Git push

## Cost
- **Free tier**: 500 hours/month (enough for small apps)
- **Database**: Included in free tier
- **SSL**: Free and automatic
- **Total cost**: $0 for small apps

## Next Steps
1. Deploy to Railway
2. Test the API endpoints
3. Update Android app with production URL
4. Test the full app
5. Add custom domain (optional)

Your GymTracker app will be live on the internet! 🌐 