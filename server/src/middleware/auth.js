const jwt = require('jsonwebtoken');

// Helper: Validate UUID format
function isValidUUID(uuid) {
  return /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(uuid);
}

// Middleware to verify JWT token
const authenticateToken = (req, res, next) => {
  console.log('[AUTH_MIDDLEWARE] 🔐 authenticateToken middleware called');
  console.log('[AUTH_MIDDLEWARE] 📝 Request headers:', req.headers);
  
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    console.log('[AUTH_MIDDLEWARE] ❌ No token provided');
    return res.status(401).json({ 
      error: 'Access denied',
      message: 'No token provided' 
    });
  }

  console.log('[AUTH_MIDDLEWARE] 🔍 Token found, verifying...');

  jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
    if (err) {
      console.log('[AUTH_MIDDLEWARE] ❌ Token verification failed:', err.message);
      return res.status(403).json({ 
        error: 'Invalid token',
        message: 'Token is not valid' 
      });
    }
    
    console.log('[AUTH_MIDDLEWARE] ✅ Token verified successfully');
    console.log('[AUTH_MIDDLEWARE] 👤 User payload:', user);
    
    // Handle both Supabase-style (sub) and custom-style (userId) JWT payloads
    if (user.sub && !user.userId) {
      user.userId = user.sub;
    }
    
    // Validate that user ID is a UUID
    const userId = user.userId || user.sub;
    if (!userId || !isValidUUID(userId)) {
      console.log('[AUTH_MIDDLEWARE] ❌ Invalid user ID format:', userId);
      return res.status(403).json({ 
        error: 'Invalid user ID',
        message: 'User ID must be a valid UUID' 
      });
    }
    
    console.log('[AUTH_MIDDLEWARE] ✅ User ID validation passed:', userId);
    req.user = user;
    next();
  });
};

module.exports = {
  authenticateToken
}; 