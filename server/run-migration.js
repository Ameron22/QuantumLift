const fs = require('fs');
const path = require('path');
const { query, testConnection } = require('./src/config/database');

// Read the migration file
const migrationPath = path.join(__dirname, 'src/config/add-workout-sharing.sql');
const migration = fs.readFileSync(migrationPath, 'utf8');

// Run migration function
const runMigration = async () => {
  try {
    console.log('🔧 Running workout sharing migration...');
    
    // Test connection first
    const isConnected = await testConnection();
    if (!isConnected) {
      throw new Error('Cannot connect to database');
    }
    
    // Execute the entire migration as one statement
    console.log('Executing workout sharing migration...');
    await query(migration);
    
    console.log('✅ Workout sharing migration completed successfully!');
    console.log('📋 Added:');
    console.log('   - workout_share_data column to feed_posts');
    console.log('   - WORKOUT_SHARED to post_type constraint');
    console.log('   - indexes for workout sharing');
    console.log('   - cleanup function for expired shared workouts');
    
  } catch (error) {
    console.error('❌ Migration failed:', error.message);
    console.error('Error details:', error);
    process.exit(1);
  }
};

// Run migration if this file is executed directly
if (require.main === module) {
  runMigration();
}

module.exports = { runMigration }; 