const { Pool } = require('pg');
require('dotenv').config();

console.log('Testing database connection...');
console.log('Environment variables:');
console.log('DB_HOST:', process.env.DB_HOST);
console.log('DB_PORT:', process.env.DB_PORT);
console.log('DB_NAME:', process.env.DB_NAME);
console.log('DB_USER:', process.env.DB_USER);
console.log('DB_PASSWORD:', process.env.DB_PASSWORD ? '***SET***' : '***NOT SET***');

const pool = new Pool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
});

pool.on('connect', () => {
  console.log('✅ Connected to PostgreSQL database');
});

pool.on('error', (err) => {
  console.error('❌ Database connection error:', err);
});

// Test connection
const testConnection = async () => {
  try {
    const client = await pool.connect();
    console.log('✅ Database connection test successful');
    const result = await client.query('SELECT current_user, current_database()');
    console.log('Current user:', result.rows[0].current_user);
    console.log('Current database:', result.rows[0].current_database);
    client.release();
    await pool.end();
    return true;
  } catch (err) {
    console.error('❌ Database connection test failed:', err.message);
    console.error('Full error:', err);
    await pool.end();
    return false;
  }
};

testConnection(); 