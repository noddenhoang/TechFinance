-- Add Identification field to users table
ALTER TABLE users
ADD COLUMN identification VARCHAR(12) UNIQUE;

-- Add a CHECK constraint to ensure identification is exactly 12 digits when provided
ALTER TABLE users
ADD CONSTRAINT check_user_identification CHECK (identification IS NULL OR identification REGEXP '^[0-9]{12}$');

-- Add Identification field to customers table
ALTER TABLE customers
ADD COLUMN identification VARCHAR(12) UNIQUE;

-- Add a CHECK constraint to ensure identification is exactly 12 digits when provided
ALTER TABLE customers
ADD CONSTRAINT check_customer_identification CHECK (identification IS NULL OR identification REGEXP '^[0-9]{12}$');
