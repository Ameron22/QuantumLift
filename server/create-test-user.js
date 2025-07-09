const bcrypt = require('bcryptjs');
const { query } = require('./src/config/database');

const createTestUser = async () => {
  try {
    console.log('🔧 Creating test user...');
    
    const username = 'marcel';
    const email = 'marcel@test.com';
    const password = 'password123';
    
    // Check if user already exists
    const existingUser = await query(
      'SELECT id FROM users WHERE username = $1 OR email = $2',
      [username, email]
    );

    if (existingUser.rows.length > 0) {
      console.log('⚠️  User already exists:', username);
      return;
    }

    // Hash password
    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    // Create user
    const result = await query(
      'INSERT INTO users (username, email, password_hash) VALUES ($1, $2, $3) RETURNING id, username, email',
      [username, email, passwordHash]
    );

    const user = result.rows[0];
    console.log('✅ Test user created successfully!');
    console.log('📋 User details:');
    console.log(`   - Username: ${user.username}`);
    console.log(`   - Email: ${user.email}`);
    console.log(`   - Password: ${password}`);
    console.log(`   - ID: ${user.id}`);
    
  } catch (error) {
    console.error('❌ Failed to create test user:', error.message);
  }
};

createTestUser(); 