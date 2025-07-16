const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { query } = require('../config/database');
const crypto = require('crypto');

const router = express.Router();

// Middleware to verify JWT token
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ 
      error: 'Access denied',
      message: 'No token provided' 
    });
  }

  jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ 
        error: 'Invalid token',
        message: 'Token is not valid' 
      });
    }
    req.user = user;
    next();
  });
};

// Send friend invitation
router.post('/invite', authenticateToken, async (req, res) => {
  try {
    const { recipientEmail } = req.body;
    const senderId = req.user.userId;

    // Validate input
    if (!recipientEmail) {
      return res.status(400).json({ 
        error: 'Missing required fields',
        message: 'Recipient email is required' 
      });
    }

    // Check if recipient exists
    const recipientResult = await query(
      'SELECT id, username FROM users WHERE email = $1',
      [recipientEmail]
    );

    if (recipientResult.rows.length === 0) {
      return res.status(404).json({ 
        error: 'User not found',
        message: 'No user found with this email address' 
      });
    }

    const recipient = recipientResult.rows[0];

    // Check if already friends
    const existingConnection = await query(
      'SELECT id FROM friend_connections WHERE ((user_id = $1 AND friend_id = $2) OR (user_id = $2 AND friend_id = $1)) AND status = $3',
      [senderId, recipient.id, 'ACCEPTED']
    );

    if (existingConnection.rows.length > 0) {
      return res.status(409).json({ 
        error: 'Already friends',
        message: 'You are already friends with this user' 
      });
    }

    // Check if invitation already exists
    const existingInvitation = await query(
      'SELECT id FROM friend_invitations WHERE sender_id = $1 AND recipient_email = $2 AND status = $3',
      [senderId, recipientEmail, 'PENDING']
    );

    if (existingInvitation.rows.length > 0) {
      return res.status(409).json({ 
        error: 'Invitation already sent',
        message: 'You have already sent an invitation to this user' 
      });
    }

    // Generate invitation code
    const invitationCode = crypto.randomBytes(16).toString('hex');
    const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000); // 7 days from now

    // Create invitation
    await query(
      'INSERT INTO friend_invitations (sender_id, recipient_email, invitation_code, expires_at) VALUES ($1, $2, $3, $4)',
      [senderId, recipientEmail, invitationCode, expiresAt]
    );

    res.status(201).json({
      message: 'Friend invitation sent successfully',
      invitation: {
        recipientEmail,
        recipientUsername: recipient.username,
        expiresAt
      }
    });

  } catch (error) {
    console.error('Send invitation error:', error);
    res.status(500).json({ 
      error: 'Failed to send invitation',
      message: 'Internal server error' 
    });
  }
});

// Get user's friends
router.get('/list', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    // Get friends with their details
    const result = await query(`
      SELECT 
        u.id,
        u.username,
        u.email,
        fc.created_at as friendship_date
      FROM friend_connections fc
      JOIN users u ON (
        CASE 
          WHEN fc.user_id = $1 THEN fc.friend_id = u.id
          WHEN fc.friend_id = $1 THEN fc.user_id = u.id
        END
      )
      WHERE (fc.user_id = $1 OR fc.friend_id = $1)
      AND fc.status = 'ACCEPTED'
      ORDER BY fc.created_at DESC
    `, [userId]);

    const friends = result.rows.map(row => ({
      id: row.id,
      username: row.username,
      email: row.email,
      friendshipDate: row.friendship_date
    }));

    res.json({
      friends
    });

  } catch (error) {
    console.error('Get friends error:', error);
    res.status(500).json({ 
      error: 'Failed to get friends',
      message: 'Internal server error' 
    });
  }
});

// Get pending invitations for current user
router.get('/invitations', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    // Get current user's email
    const userResult = await query(
      'SELECT email FROM users WHERE id = $1',
      [userId]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({ 
        error: 'User not found',
        message: 'User does not exist' 
      });
    }

    const userEmail = userResult.rows[0].email;

    // Get pending invitations for this user
    const result = await query(`
      SELECT 
        fi.id,
        fi.invitation_code,
        fi.created_at,
        fi.expires_at,
        u.username as sender_username,
        u.email as sender_email
      FROM friend_invitations fi
      JOIN users u ON fi.sender_id = u.id
      WHERE fi.recipient_email = $1 
      AND fi.status = 'PENDING'
      AND fi.expires_at > NOW()
      ORDER BY fi.created_at DESC
    `, [userEmail]);

    const invitations = result.rows.map(row => ({
      id: row.id,
      invitationCode: row.invitation_code,
      createdAt: row.created_at,
      expiresAt: row.expires_at,
      senderUsername: row.sender_username,
      senderEmail: row.sender_email
    }));

    res.json({
      invitations
    });

  } catch (error) {
    console.error('Get invitations error:', error);
    res.status(500).json({ 
      error: 'Failed to get invitations',
      message: 'Internal server error' 
    });
  }
});

// Accept friend invitation
router.post('/accept/:invitationCode', authenticateToken, async (req, res) => {
  try {
    const { invitationCode } = req.params;
    const userId = req.user.userId;

    // Find invitation
    const invitationResult = await query(
      'SELECT sender_id, recipient_email, status, expires_at FROM friend_invitations WHERE invitation_code = $1',
      [invitationCode]
    );

    if (invitationResult.rows.length === 0) {
      return res.status(404).json({ 
        error: 'Invitation not found',
        message: 'Invalid invitation code' 
      });
    }

    const invitation = invitationResult.rows[0];

    // Check if invitation is expired
    if (new Date() > new Date(invitation.expires_at)) {
      return res.status(410).json({ 
        error: 'Invitation expired',
        message: 'This invitation has expired' 
      });
    }

    // Check if invitation is already accepted
    if (invitation.status !== 'PENDING') {
      return res.status(409).json({ 
        error: 'Invitation already processed',
        message: 'This invitation has already been processed' 
      });
    }

    // Check if the current user is the recipient
    const currentUserResult = await query(
      'SELECT email FROM users WHERE id = $1',
      [userId]
    );

    if (currentUserResult.rows.length === 0 || currentUserResult.rows[0].email !== invitation.recipient_email) {
      return res.status(403).json({ 
        error: 'Not authorized',
        message: 'You are not authorized to accept this invitation' 
      });
    }

    // Create friend connection
    await query(
      'INSERT INTO friend_connections (user_id, friend_id, status) VALUES ($1, $2, $3)',
      [invitation.sender_id, userId, 'ACCEPTED']
    );

    // Update invitation status
    await query(
      'UPDATE friend_invitations SET status = $1 WHERE invitation_code = $2',
      ['ACCEPTED', invitationCode]
    );

    res.json({
      message: 'Friend invitation accepted successfully'
    });

  } catch (error) {
    console.error('Accept invitation error:', error);
    res.status(500).json({ 
      error: 'Failed to accept invitation',
      message: 'Internal server error' 
    });
  }
});

// Decline friend invitation
router.post('/decline/:invitationCode', authenticateToken, async (req, res) => {
  try {
    const { invitationCode } = req.params;
    const userId = req.user.userId;

    // Find invitation
    const invitationResult = await query(
      'SELECT sender_id, recipient_email, status, expires_at FROM friend_invitations WHERE invitation_code = $1',
      [invitationCode]
    );

    if (invitationResult.rows.length === 0) {
      return res.status(404).json({ 
        error: 'Invitation not found',
        message: 'Invalid invitation code' 
      });
    }

    const invitation = invitationResult.rows[0];

    // Check if invitation is expired
    if (new Date() > new Date(invitation.expires_at)) {
      return res.status(410).json({ 
        error: 'Invitation expired',
        message: 'This invitation has expired' 
      });
    }

    // Check if invitation is already processed
    if (invitation.status !== 'PENDING') {
      return res.status(409).json({ 
        error: 'Invitation already processed',
        message: 'This invitation has already been processed' 
      });
    }

    // Check if the current user is the recipient
    const currentUserResult = await query(
      'SELECT email FROM users WHERE id = $1',
      [userId]
    );

    if (currentUserResult.rows.length === 0 || currentUserResult.rows[0].email !== invitation.recipient_email) {
      return res.status(403).json({ 
        error: 'Not authorized',
        message: 'You are not authorized to decline this invitation' 
      });
    }

    // Update invitation status to expired (instead of deleting)
    await query(
      'UPDATE friend_invitations SET status = $1 WHERE invitation_code = $2',
      ['EXPIRED', invitationCode]
    );

    res.json({
      message: 'Friend invitation declined successfully'
    });

  } catch (error) {
    console.error('Decline invitation error:', error);
    res.status(500).json({ 
      error: 'Failed to decline invitation',
      message: 'Internal server error' 
    });
  }
});

module.exports = router; 