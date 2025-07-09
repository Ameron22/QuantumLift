require('dotenv').config();

console.log('=== Environment Variables Test ===');
console.log('DATABASE_URL exists:', !!process.env.DATABASE_URL);
console.log('JWT_SECRET exists:', !!process.env.JWT_SECRET);
console.log('JWT_EXPIRES_IN:', process.env.JWT_EXPIRES_IN || 'not set');
console.log('NODE_ENV:', process.env.NODE_ENV);

// Test database connection
const { testConnection } = require('./src/config/database');

testConnection().then(success => {
  if (success) {
    console.log('✅ Database connection successful');
  } else {
    console.log('❌ Database connection failed');
  }
}).catch(err => {
  console.error('❌ Database test error:', err);
}); 