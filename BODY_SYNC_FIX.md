# Body Sync Fix - UPSERT Logic

## Problem

The initial sync attempt failed with 0 rows updated:
```
📊 Executed query { ..., rows: 0 }
```

### Root Cause

The sync endpoint was trying to **UPDATE** records using local database IDs that don't exist in the cloud database:

```sql
UPDATE physical_parameters 
WHERE id = $1 AND user_id = $9  -- Local ID doesn't exist in cloud!
```

When users create body data locally first, the local database assigns IDs (1, 2, 3, etc.). These IDs don't exist in the cloud database, so the UPDATE fails.

## Solution

Implemented proper **UPSERT** logic using a two-step approach:

### Step 1: Check if record exists
```sql
SELECT id FROM physical_parameters 
WHERE user_id = $1 AND DATE(date) = DATE($2)
```

### Step 2: Update or Insert accordingly
- **If exists**: UPDATE using the cloud ID
- **If not exists**: INSERT new record

### Code Changes

**File**: `server/src/routes/body.js`

**Before** (broken):
```javascript
if (param.id) {
  // Try to UPDATE with local ID that doesn't exist in cloud
  result = await query('UPDATE ... WHERE id = $1 AND user_id = $9', 
    [param.id, ...]);
}
```

**After** (fixed):
```javascript
// Check if record exists for this user and date
const existingCheck = await query(
  'SELECT id FROM physical_parameters WHERE user_id = $1 AND DATE(date) = DATE($2)',
  [userId, dateObj]
);

if (existingCheck.rows.length > 0) {
  // Update existing record using cloud ID
  const existingId = existingCheck.rows[0].id;
  result = await query('UPDATE ... WHERE id = $1', [existingId, ...]);
} else {
  // Insert new record
  result = await query('INSERT INTO ...', [userId, ...]);
}
```

## Why This Works

1. **Date-based uniqueness**: The unique constraint is on `(user_id, DATE(date))`, so we check for existing records by date, not by local ID
2. **Cloud ID mapping**: When updating, we use the cloud ID from the existing record
3. **Proper INSERT**: For new records, we let the cloud database generate its own ID

## Testing

After deploying this fix, sync should work correctly:

1. **First sync**: Creates new records in cloud (INSERT)
2. **Subsequent syncs**: Updates existing records (UPDATE)
3. **Result**: `rows: 1` for each successful operation

## Deployment

1. Deploy the updated `server/src/routes/body.js` to Vercel
2. Users can try syncing again
3. Check logs for `rows: 1` instead of `rows: 0`


