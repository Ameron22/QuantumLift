require('dotenv').config();

console.log('=== Environment Variables Debug ===');
console.log('DB_HOST:', process.env.DB_HOST, 'Type:', typeof process.env.DB_HOST);
console.log('DB_PORT:', process.env.DB_PORT, 'Type:', typeof process.env.DB_PORT);
console.log('DB_NAME:', process.env.DB_NAME, 'Type:', typeof process.env.DB_NAME);
console.log('DB_USER:', process.env.DB_USER, 'Type:', typeof process.env.DB_USER);
console.log('DB_PASSWORD:', process.env.DB_PASSWORD, 'Type:', typeof process.env.DB_PASSWORD);
console.log('DB_PASSWORD length:', process.env.DB_PASSWORD ? process.env.DB_PASSWORD.length : 'undefined');
console.log('DB_PASSWORD first char:', process.env.DB_PASSWORD ? process.env.DB_PASSWORD.charCodeAt(0) : 'undefined');
console.log('DB_PASSWORD last char:', process.env.DB_PASSWORD ? process.env.DB_PASSWORD.charCodeAt(process.env.DB_PASSWORD.length - 1) : 'undefined');
console.log('=================================='); 