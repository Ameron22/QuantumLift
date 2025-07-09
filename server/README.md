# GymTracker Server

Backend server for the GymTracker Android app, providing authentication and social features.

## 🚀 Quick Start

### Prerequisites
- Node.js (v14 or higher)
- PostgreSQL database
- npm or yarn

### Installation

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Set up environment variables:**
   ```bash
   cp env.example .env
   ```
   Then edit `.env` with your database and JWT settings.

3. **Set up PostgreSQL database:**
   ```bash
   # Create database and user
   sudo -u postgres createdb gymtracker
   sudo -u postgres psql -c "CREATE USER gymuser WITH PASSWORD 'your_password';"
   sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE gymtracker TO gymuser;"
   ```

4. **Initialize database schema:**
   ```bash
   npm run init-db
   ```

5. **Start the server:**
   ```bash
   # Development mode (with auto-restart)
   npm run dev
   
   # Production mode
   npm start
   ```

## 📁 Project Structure

```
server/
├── index.js                 # Main server file
├── src/
│   ├── config/
│   │   ├── database.js      # Database connection
│   │   ├── schema.sql       # Database schema
│   │   └── init-db.js       # Database initialization
│   ├── routes/              # API routes (coming soon)
│   ├── models/              # Data models (coming soon)
│   └── middleware/          # Custom middleware (coming soon)
├── package.json
└── README.md
```

## 🔧 Configuration

### Environment Variables

Create a `.env` file with the following variables:

```env
# Server Configuration
PORT=3000
NODE_ENV=development

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=gymtracker
DB_USER=gymuser
DB_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your_super_secret_jwt_key_here
JWT_EXPIRES_IN=7d
```

## 📊 API Endpoints

### Health Check
- `GET /health` - Server health status

### Root
- `GET /` - API information

## 🛠️ Development

### Available Scripts
- `npm run dev` - Start development server with auto-restart
- `npm start` - Start production server
- `npm run init-db` - Initialize database schema

### Database Management
- The database schema is defined in `src/config/schema.sql`
- Run `npm run init-db` to create tables
- Database connection is handled in `src/config/database.js`

## 🔒 Security Notes

- Change the JWT_SECRET in production
- Use strong passwords for database
- Enable HTTPS in production
- Consider rate limiting for API endpoints

## 📝 Next Steps

1. ✅ Basic Express server setup
2. ✅ Database configuration
3. 🔄 Authentication routes (coming next)
4. 🔄 Friend invitation system
5. 🔄 Android app integration 