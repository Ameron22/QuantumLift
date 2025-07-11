const { query } = require('./src/config/database');

async function testFeedDatabase() {
  try {
    console.log('🔍 Testing feed database structure...');
    
    // Check if feed_posts table exists
    const tableCheck = await query(`
      SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name = 'feed_posts'
      );
    `);
    
    console.log('📊 feed_posts table exists:', tableCheck.rows[0].exists);
    
    if (tableCheck.rows[0].exists) {
      // Check table structure
      const structure = await query(`
        SELECT column_name, data_type, is_nullable, column_default
        FROM information_schema.columns 
        WHERE table_name = 'feed_posts'
        ORDER BY ordinal_position;
      `);
      
      console.log('🏗️ feed_posts table structure:');
      structure.rows.forEach(row => {
        console.log(`  - ${row.column_name}: ${row.data_type} (nullable: ${row.is_nullable})`);
      });
      
      // Check if user_id=1 exists in users table
      const userCheck = await query('SELECT id, username FROM users WHERE id = $1', [1]);
      console.log('👤 User with ID=1 exists:', userCheck.rows.length > 0);
      if (userCheck.rows.length > 0) {
        console.log('  User details:', userCheck.rows[0]);
      }
      
      // Try to insert a test post
      console.log('🧪 Testing post insertion...');
      const testInsert = await query(`
        INSERT INTO feed_posts (user_id, post_type, content, privacy_level)
        VALUES ($1, $2, $3, $4)
        RETURNING id, created_at
      `, [1, 'TEXT_POST', 'Test post from script', 'PUBLIC']);
      
      console.log('✅ Test post created successfully:', testInsert.rows[0]);
      
      // Clean up test post
      await query('DELETE FROM feed_posts WHERE id = $1', [testInsert.rows[0].id]);
      console.log('🧹 Test post cleaned up');
      
    } else {
      console.log('❌ feed_posts table does not exist!');
    }
    
  } catch (error) {
    console.error('❌ Database test failed:', error);
    console.error('Error details:', {
      message: error.message,
      code: error.code,
      detail: error.detail,
      hint: error.hint
    });
  } finally {
    process.exit(0);
  }
}

testFeedDatabase(); 