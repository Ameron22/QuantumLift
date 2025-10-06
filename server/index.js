require('dotenv').config();
console.log('Loaded env:', process.env.DB_USER, process.env.DB_PASSWORD);
console.log('DATABASE_URL exists:', !!process.env.DATABASE_URL);
console.log('NODE_ENV:', process.env.NODE_ENV);

const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');

// Load environment variables
dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors()); // Enable CORS for all routes
app.use(express.json()); // Parse JSON bodies
app.use(express.urlencoded({ extended: true })); // Parse URL-encoded bodies

// Import routes
const authRoutes = require('./src/routes/auth');
const friendRoutes = require('./src/routes/friends');
const feedRoutes = require('./src/routes/feed');
const workoutRoutes = require('./src/routes/workouts');
const bodyRoutes = require('./src/routes/body');

// Basic health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'healthy', 
    timestamp: new Date().toISOString(),
    message: 'QuantumLift Server is running!'
  });
});

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/friends', friendRoutes);
app.use('/api/feed', feedRoutes);
app.use('/api/workouts', workoutRoutes);
app.use('/api/body', bodyRoutes);

// Root endpoint
app.get('/', (req, res) => {
  res.json({ 
    message: 'Welcome to QuantumLift API',
    version: '1.0.0',
    endpoints: {
      health: '/health',
      auth: '/api/auth',
      friends: '/api/friends',
      feed: '/api/feed',
      workouts: '/api/workouts',
      body: '/api/body'
    }
  });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ 
    error: 'Something went wrong!',
    message: err.message 
  });
});

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({ 
    error: 'Endpoint not found',
    path: req.originalUrl 
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 QuantumLift Server running on port ${PORT}`);
  console.log(`📊 Health check: http://localhost:${PORT}/health`);
  console.log(`🌐 API Base URL: http://localhost:${PORT}/api`);
  console.log(`🔧 Environment: ${process.env.NODE_ENV}`);
  console.log(`🗄️ Database URL: ${process.env.DATABASE_URL ? 'Set' : 'Not set'}`);
}); 