# 🚀 Vercel Deployment Guide (Free Forever)

## Why Vercel?
- ✅ **Free forever** (no time limits!)
- ✅ **Unlimited deployments**
- ✅ **Auto SSL/HTTPS**
- ✅ **Perfect for personal projects**
- ✅ **GitHub integration**
- ✅ **Cost**: $0 forever

## Step 1: Prepare Your Code

### 1.1 Push to GitHub
```bash
git add .
git commit -m "Prepare for Vercel deployment"
git push origin main
```

### 1.2 Verify Files
- ✅ `server/package.json` (with `"start": "node index.js"`)
- ✅ `server/index.js` (main server file)
- ✅ `server/vercel.json` (Vercel config)
- ✅ `server/src/config/database.js` (updated for external DB)

## Step 2: Set Up Free Database

### 2.1 Option A: PlanetScale (Recommended)
1. Go to [planetscale.com](https://planetscale.com)
2. Sign up with GitHub (free)
3. Create new database
4. Get connection string

### 2.2 Option B: Supabase (Alternative)
1. Go to [supabase.com](https://supabase.com)
2. Sign up with GitHub (free)
3. Create new project
4. Get PostgreSQL connection string

### 2.3 Option C: Railway Database (30 days free)
1. Use Railway just for database
2. Get PostgreSQL connection string
3. Use with Vercel hosting

## Step 3: Deploy to Vercel

### 3.1 Create Vercel Account
1. Go to [vercel.com](https://vercel.com)
2. Sign in with GitHub
3. Click "New Project"

### 3.2 Import Repository
1. Select your QuantumLift repository
2. Vercel will auto-detect Node.js project
3. Configure settings:
   - **Framework Preset**: Node.js
   - **Root Directory**: `server`
   - **Build Command**: `npm install`
   - **Output Directory**: Leave empty
   - **Install Command**: `npm install`

### 3.3 Set Environment Variables
Add these in Vercel dashboard:
```env
NODE_ENV=production
JWT_SECRET=your_super_secret_jwt_key_here
JWT_EXPIRES_IN=7d
DATABASE_URL=your_database_connection_string
PORT=3000
```

### 3.4 Deploy
1. Click "Deploy"
2. Vercel will build and deploy automatically
3. Get your HTTPS URL (e.g., `https://quantumlift-api.vercel.app`)

## Step 4: Update Database Configuration

### 4.1 For PlanetScale (MySQL)
Update `server/src/config/database.js`:
```javascript
const mysql = require('mysql2/promise');

const pool = mysql.createPool(process.env.DATABASE_URL);

const query = async (text, params) => {
  const [rows] = await pool.execute(text, params);
  return { rows };
};
```

### 4.2 For Supabase (PostgreSQL)
Keep current PostgreSQL configuration.

## Step 5: Initialize Database

### 5.1 For PlanetScale
1. Use PlanetScale's web interface
2. Run your schema SQL
3. Create test user

### 5.2 For Supabase
1. Use Supabase's SQL editor
2. Run your schema
3. Create test user

## Step 6: Update Android App

### 6.1 Update Production URL
Replace in `AuthRepository.kt`:
```kotlin
val baseUrl = if (BuildConfig.DEBUG) {
    "http://192.168.0.76:3000/" // Development
} else {
    "https://your-app-name.vercel.app/" // Production
}
```

### 6.2 Update Network Security Config
Add to `network_security_config.xml`:
```xml
<domain includeSubdomains="true">your-app-name.vercel.app</domain>
```

## Step 7: Test Deployment

### 7.1 Test API
```bash
# Health check
curl https://your-app-name.vercel.app/health

# Test registration
curl -X POST https://your-app-name.vercel.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"password123"}'
```

### 7.2 Test Android App
1. Build release APK
2. Install on phone
3. Test login/registration

## Cost Breakdown

| Component | Service | Cost | Free Tier |
|-----------|---------|------|-----------|
| **Hosting** | Vercel | $0 | Forever |
| **Database** | PlanetScale | $0 | 1 DB, 1B reads/month |
| **SSL** | Vercel | $0 | Automatic |
| **Domain** | Custom | $10/year | Optional |
| **Total** | | **$0-10/year** | |

## Database Options Comparison

| Database | Free Tier | Cost After | Setup |
|----------|-----------|------------|-------|
| **PlanetScale** | 1 DB, 1B reads/month | $0 | Easy |
| **Supabase** | 500MB, 50MB bandwidth | $0 | Easy |
| **Railway DB** | 30 days | $5/month | Easy |
| **Neon** | 3GB storage | $0 | Medium |

## Recommendation
**Use Vercel + PlanetScale**:
1. **Vercel** for hosting (free forever)
2. **PlanetScale** for database (free forever)
3. **Total cost**: $0 forever

## Troubleshooting

### Common Issues:
1. **Database connection**: Check `DATABASE_URL` in Vercel
2. **Build failed**: Check `package.json` has correct scripts
3. **Environment variables**: Make sure all required vars are set
4. **CORS issues**: Vercel handles CORS automatically

### Vercel Dashboard Features:
- ✅ **Real-time logs**: See what's happening
- ✅ **Environment variables**: Easy configuration
- ✅ **Auto-deploy**: Deploy on every Git push
- ✅ **Custom domains**: Add your own domain
- ✅ **Analytics**: Performance monitoring

Your QuantumLift app will be live forever for free! 🌐 