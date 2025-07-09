const fs = require('fs');
const path = require('path');
const { query, testConnection } = require('./database');

// Read the schema file
const schemaPath = path.join(__dirname, 'schema.sql');
const schema = fs.readFileSync(schemaPath, 'utf8');

// Initialize database function
const initializeDatabase = async () => {
  try {
    console.log('🔧 Initializing database...');
    
    // Test connection first
    const isConnected = await testConnection();
    if (!isConnected) {
      throw new Error('Cannot connect to database');
    }
    
    // Split schema into individual statements
    const statements = schema
      .split(';')
      .map(statement => statement.trim())
      .filter(statement => statement.length > 0);
    
    // Execute each statement
    for (const statement of statements) {
      if (statement.trim()) {
        await query(statement);
      }
    }
    
    console.log('✅ Database initialized successfully!');
    console.log('📋 Created tables:');
    console.log('   - users');
    console.log('   - friend_invitations');
    console.log('   - friend_connections');
    console.log('   - indexes and triggers');
    
  } catch (error) {
    console.error('❌ Database initialization failed:', error.message);
    process.exit(1);
  }
};

// Run initialization if this file is executed directly
if (require.main === module) {
  initializeDatabase();
}

module.exports = { initializeDatabase }; 