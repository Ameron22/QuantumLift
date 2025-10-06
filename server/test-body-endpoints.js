// Test script for body tracking endpoints
const axios = require('axios');

const BASE_URL = 'http://localhost:3000/api';

// You'll need to get a valid JWT token from login first
const TEST_TOKEN = 'your-jwt-token-here';

const headers = {
  'Authorization': `Bearer ${TEST_TOKEN}`,
  'Content-Type': 'application/json'
};

async function testBodyEndpoints() {
  console.log('🧪 Testing Body Tracking Endpoints...\n');

  try {
    // Test 1: Get latest physical parameters
    console.log('1️⃣ Testing GET /api/body/parameters/latest');
    try {
      const response = await axios.get(`${BASE_URL}/body/parameters/latest`, { headers });
      console.log('✅ Success:', response.data);
    } catch (error) {
      console.log('❌ Error:', error.response?.data || error.message);
    }
    console.log('');

    // Test 2: Create new physical parameters
    console.log('2️⃣ Testing POST /api/body/parameters');
    const testParameters = {
      date: Date.now(),
      weight: 75.5,
      height: 180.0,
      bmi: 23.3,
      bodyFatPercentage: 15.0,
      muscleMass: 65.0,
      notes: 'Test entry from script'
    };

    try {
      const response = await axios.post(`${BASE_URL}/body/parameters`, testParameters, { headers });
      console.log('✅ Success:', response.data);
      const parametersId = response.data.parameters?.id;
      
      if (parametersId) {
        // Test 3: Add body measurements
        console.log('3️⃣ Testing POST /api/body/measurements');
        const testMeasurements = {
          measurements: [
            {
              parametersId: parametersId,
              measurementType: 'chest',
              value: 95.0,
              unit: 'cm'
            },
            {
              parametersId: parametersId,
              measurementType: 'waist',
              value: 80.0,
              unit: 'cm'
            },
            {
              parametersId: parametersId,
              measurementType: 'biceps',
              value: 35.0,
              unit: 'cm'
            }
          ]
        };

        try {
          const response = await axios.post(`${BASE_URL}/body/measurements`, testMeasurements, { headers });
          console.log('✅ Success:', response.data);
        } catch (error) {
          console.log('❌ Error:', error.response?.data || error.message);
        }
        console.log('');

        // Test 4: Get body measurements
        console.log('4️⃣ Testing GET /api/body/measurements/{parametersId}');
        try {
          const response = await axios.get(`${BASE_URL}/body/measurements/${parametersId}`, { headers });
          console.log('✅ Success:', response.data);
        } catch (error) {
          console.log('❌ Error:', error.response?.data || error.message);
        }
        console.log('');
      }
    } catch (error) {
      console.log('❌ Error:', error.response?.data || error.message);
    }
    console.log('');

    // Test 5: Get all parameters
    console.log('5️⃣ Testing GET /api/body/parameters');
    try {
      const response = await axios.get(`${BASE_URL}/body/parameters?limit=10&offset=0`, { headers });
      console.log('✅ Success:', response.data);
    } catch (error) {
      console.log('❌ Error:', error.response?.data || error.message);
    }
    console.log('');

    // Test 6: Bulk sync
    console.log('6️⃣ Testing POST /api/body/sync');
    const syncData = {
      parameters: [testParameters],
      measurements: [
        {
          parametersId: 1, // This might need to be adjusted
          measurementType: 'thighs',
          value: 60.0,
          unit: 'cm'
        }
      ]
    };

    try {
      const response = await axios.post(`${BASE_URL}/body/sync`, syncData, { headers });
      console.log('✅ Success:', response.data);
    } catch (error) {
      console.log('❌ Error:', error.response?.data || error.message);
    }
    console.log('');

    console.log('🎉 Body tracking endpoints test completed!');

  } catch (error) {
    console.error('💥 Test failed:', error.message);
  }
}

// Instructions
console.log('📋 Instructions:');
console.log('1. Make sure your server is running on http://localhost:3000');
console.log('2. Get a valid JWT token by logging in through the app or using the auth endpoints');
console.log('3. Replace TEST_TOKEN with your actual JWT token');
console.log('4. Run: node test-body-endpoints.js');
console.log('');

// Uncomment the line below to run the tests
// testBodyEndpoints();
