-- Create temporary tables to store data
CREATE TEMPORARY TABLE temp_income_transactions AS SELECT * FROM income_transactions;
CREATE TEMPORARY TABLE temp_expense_transactions AS SELECT * FROM expense_transactions;

-- Drop foreign key constraints
ALTER TABLE income_transactions DROP FOREIGN KEY income_transactions_ibfk_1;
ALTER TABLE income_transactions DROP FOREIGN KEY income_transactions_ibfk_2;
ALTER TABLE income_transactions DROP FOREIGN KEY income_transactions_ibfk_3;

ALTER TABLE expense_transactions DROP FOREIGN KEY expense_transactions_ibfk_1;
ALTER TABLE expense_transactions DROP FOREIGN KEY expense_transactions_ibfk_2;
ALTER TABLE expense_transactions DROP FOREIGN KEY expense_transactions_ibfk_3;

-- Drop tables
DROP TABLE income_transactions;
DROP TABLE expense_transactions;

-- Recreate table with English enum values
CREATE TABLE income_transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    customer_id INT,
    transaction_date DATE NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    payment_status ENUM('RECEIVED', 'PENDING') DEFAULT 'PENDING',
    description TEXT,
    reference_no VARCHAR(50),
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES income_categories(category_id),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE TABLE expense_transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    supplier_id INT,
    transaction_date DATE NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    payment_status ENUM('PAID', 'UNPAID') DEFAULT 'UNPAID',
    description TEXT,
    reference_no VARCHAR(50),
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES expense_categories(category_id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- Reinsert data with converted enum values
INSERT INTO income_transactions 
SELECT 
    transaction_id,
    category_id,
    customer_id,
    transaction_date,
    amount,
    CASE payment_status
        WHEN 'Đã nhận' THEN 'RECEIVED'
        WHEN 'Chưa nhận' THEN 'PENDING'
    END,
    description,
    reference_no,
    created_by,
    created_at,
    updated_at
FROM temp_income_transactions;

INSERT INTO expense_transactions 
SELECT 
    transaction_id,
    category_id,
    supplier_id,
    transaction_date,
    amount,
    CASE payment_status
        WHEN 'Đã trả' THEN 'PAID'
        WHEN 'Chưa trả' THEN 'UNPAID'
    END,
    description,
    reference_no,
    created_by,
    created_at,
    updated_at
FROM temp_expense_transactions;

-- Update financial_reports table enum
ALTER TABLE financial_reports MODIFY report_type ENUM('INCOME', 'EXPENSE', 'SUMMARY', 'TAX') NOT NULL;

-- Update data in financial_reports
UPDATE financial_reports SET report_type = 
    CASE report_type
        WHEN 'Thu nhập' THEN 'INCOME'
        WHEN 'Chi phí' THEN 'EXPENSE'
        WHEN 'Tổng hợp' THEN 'SUMMARY'
        WHEN 'Thuế' THEN 'TAX'
    END;