ALTER TABLE invites ADD COLUMN receiver_id UUID;

ALTER TABLE invites DROP COLUMN email;