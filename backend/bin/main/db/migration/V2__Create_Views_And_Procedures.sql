-- Tạo view để tính tổng thu nhập theo tháng/năm
CREATE VIEW monthly_income_view AS
SELECT 
    YEAR(transaction_date) AS year,
    MONTH(transaction_date) AS month,
    SUM(CASE WHEN payment_status = 'RECEIVED' THEN amount ELSE 0 END) AS received_amount,
    SUM(CASE WHEN payment_status = 'PENDING' THEN amount ELSE 0 END) AS pending_amount,
    SUM(amount) AS total_amount
FROM income_transactions
GROUP BY YEAR(transaction_date), MONTH(transaction_date);

-- Tạo view để tính tổng chi phí theo tháng/năm
CREATE VIEW monthly_expense_view AS
SELECT 
    YEAR(transaction_date) AS year,
    MONTH(transaction_date) AS month,
    SUM(CASE WHEN payment_status = 'PAID' THEN amount ELSE 0 END) AS paid_amount,
    SUM(CASE WHEN payment_status = 'UNPAID' THEN amount ELSE 0 END) AS pending_amount,
    SUM(amount) AS total_amount
FROM expense_transactions
GROUP BY YEAR(transaction_date), MONTH(transaction_date);

-- Tạo view để tính tổng thu nhập theo danh mục và tháng/năm
CREATE VIEW category_income_view AS
SELECT 
    ic.name AS category_name,
    YEAR(it.transaction_date) AS year,
    MONTH(it.transaction_date) AS month,
    SUM(it.amount) AS total_amount,
    ROUND((SUM(it.amount) / (SELECT SUM(amount) FROM income_transactions 
                             WHERE YEAR(transaction_date) = YEAR(it.transaction_date) 
                             AND MONTH(transaction_date) = MONTH(it.transaction_date))) * 100, 2) AS percentage
FROM income_transactions it
JOIN income_categories ic ON it.category_id = ic.category_id
GROUP BY ic.category_id, YEAR(it.transaction_date), MONTH(it.transaction_date);

-- Tạo view để tính tổng chi phí theo danh mục và tháng/năm
CREATE VIEW category_expense_view AS
SELECT 
    ec.name AS category_name,
    YEAR(et.transaction_date) AS year,
    MONTH(et.transaction_date) AS month,
    SUM(et.amount) AS total_amount,
    ROUND((SUM(et.amount) / (SELECT SUM(amount) FROM expense_transactions 
                             WHERE YEAR(transaction_date) = YEAR(et.transaction_date) 
                             AND MONTH(transaction_date) = MONTH(et.transaction_date))) * 100, 2) AS percentage
FROM expense_transactions et
JOIN expense_categories ec ON et.category_id = ec.category_id
GROUP BY ec.category_id, YEAR(et.transaction_date), MONTH(et.transaction_date);

-- Tạo view để tính tổng thu nhập theo khách hàng và tháng/năm
CREATE VIEW customer_income_view AS
SELECT 
    c.name AS customer_name,
    YEAR(it.transaction_date) AS year,
    MONTH(it.transaction_date) AS month,
    SUM(it.amount) AS total_amount
FROM income_transactions it
JOIN customers c ON it.customer_id = c.customer_id
GROUP BY c.customer_id, YEAR(it.transaction_date), MONTH(it.transaction_date);

-- Tạo view để tính tổng chi phí theo nhà cung cấp và tháng/năm
CREATE VIEW supplier_expense_view AS
SELECT 
    s.name AS supplier_name,
    YEAR(et.transaction_date) AS year,
    MONTH(et.transaction_date) AS month,
    SUM(et.amount) AS total_amount
FROM expense_transactions et
JOIN suppliers s ON et.supplier_id = s.supplier_id
GROUP BY s.supplier_id, YEAR(et.transaction_date), MONTH(et.transaction_date);

-- Procedure tính tổng thu nhập và chi phí theo tháng/năm
DELIMITER //
CREATE PROCEDURE sp_generate_monthly_report(IN p_year INT, IN p_month INT)
BEGIN
    SELECT 
        p_year AS year,
        p_month AS month,
        COALESCE((SELECT SUM(amount) FROM income_transactions WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month), 0) AS total_income,
        COALESCE((SELECT SUM(amount) FROM expense_transactions WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month), 0) AS total_expense,
        COALESCE((SELECT SUM(amount) FROM income_transactions WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month), 0) -
        COALESCE((SELECT SUM(amount) FROM expense_transactions WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month), 0) AS profit_loss,
        COALESCE((SELECT SUM(amount) FROM income_transactions WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month AND payment_status = 'PENDING'), 0) AS receivables,
        COALESCE((SELECT SUM(amount) FROM expense_transactions WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month AND payment_status = 'UNPAID'), 0) AS payables;
END //
DELIMITER ;

-- Procedure tính tổng thu nhập theo danh mục, tháng/năm
DELIMITER //
CREATE PROCEDURE sp_income_by_category(IN p_year INT, IN p_month INT)
BEGIN
    SELECT 
        ic.category_id,
        ic.name AS category_name,
        COALESCE(SUM(it.amount), 0) AS total_amount,
        CASE 
            WHEN (SELECT SUM(amount) FROM income_transactions WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month) > 0 
            THEN ROUND((COALESCE(SUM(it.amount), 0) / (SELECT SUM(amount) FROM income_transactions WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month)) * 100, 2)
            ELSE 0
        END AS percentage
    FROM 
        income_categories ic
    LEFT JOIN 
        income_transactions it ON ic.category_id = it.category_id AND YEAR(it.transaction_date) = p_year AND MONTH(it.transaction_date) = p_month
    GROUP BY 
        ic.category_id;
END //
DELIMITER ;

-- Procedure tính tổng chi phí theo danh mục, tháng/năm
DELIMITER //
CREATE PROCEDURE sp_expense_by_category(IN p_year INT, IN p_month INT)
BEGIN
    SELECT 
        ec.category_id,
        ec.name AS category_name,
        COALESCE(SUM(et.amount), 0) AS total_amount,
        CASE 
            WHEN (SELECT SUM(amount) FROM expense_transactions WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month) > 0 
            THEN ROUND((COALESCE(SUM(et.amount), 0) / (SELECT SUM(amount) FROM expense_transactions WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month)) * 100, 2)
            ELSE 0
        END AS percentage
    FROM 
        expense_categories ec
    LEFT JOIN 
        expense_transactions et ON ec.category_id = et.category_id AND YEAR(et.transaction_date) = p_year AND MONTH(et.transaction_date) = p_month
    GROUP BY 
        ec.category_id;
END //
DELIMITER ;

-- Procedure tạo báo cáo thuế
DELIMITER //
CREATE PROCEDURE sp_generate_tax_report(IN p_year INT, IN p_month INT)
BEGIN
    DECLARE v_income_tax DECIMAL(15, 2);
    DECLARE v_expense_tax DECIMAL(15, 2);
    DECLARE v_difference DECIMAL(15, 2);
    
    -- Giả sử tính thuế thu nhập là 10% tổng thu nhập
    SELECT COALESCE(SUM(amount) * 0.1, 0) INTO v_income_tax
    FROM income_transactions 
    WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month;
    
    -- Giả sử tính thuế chi phí là 5% tổng chi phí
    SELECT COALESCE(SUM(amount) * 0.05, 0) INTO v_expense_tax
    FROM expense_transactions 
    WHERE YEAR(transaction_date) = p_year AND MONTH(transaction_date) = p_month;
    
    SET v_difference = v_income_tax - v_expense_tax;
    
    -- Thêm hoặc cập nhật báo cáo thuế
    INSERT INTO tax_records (year, month, income_tax, expense_tax, tax_difference, created_by)
    VALUES (p_year, p_month, v_income_tax, v_expense_tax, v_difference, 1)
    ON DUPLICATE KEY UPDATE
        income_tax = v_income_tax,
        expense_tax = v_expense_tax,
        tax_difference = v_difference,
        updated_at = CURRENT_TIMESTAMP;
        
    -- Trả về báo cáo thuế
    SELECT 
        p_year AS year,
        p_month AS month,
        v_income_tax AS income_tax,
        v_expense_tax AS expense_tax,
        v_difference AS tax_difference;
END //
DELIMITER ;

-- Procedure sao chép dữ liệu và dự đoán thu nhập
DELIMITER //
CREATE PROCEDURE sp_predict_income(IN p_source_year INT, IN p_source_month INT, IN p_target_year INT, IN p_target_month INT, IN p_growth_rate DECIMAL(5,2))
BEGIN
    DECLARE source_data TEXT;
    
    -- Lưu thông tin nguồn dữ liệu
    SET source_data = CONCAT('Data copied from ', p_source_year, '-', p_source_month, ' with growth rate of ', p_growth_rate, '%');
    
    -- Xóa dự đoán cũ nếu có
    DELETE FROM income_predictions WHERE year = p_target_year AND month = p_target_month;
    
    -- Chèn dự đoán mới dựa trên dữ liệu nguồn
    INSERT INTO income_predictions (year, month, category_id, predicted_amount, source_data, created_by)
    SELECT 
        p_target_year,
        p_target_month,
        ic.category_id,
        COALESCE(SUM(it.amount) * (1 + p_growth_rate/100), 0) AS predicted_amount,
        source_data,
        1
    FROM 
        income_categories ic
    LEFT JOIN 
        income_transactions it ON ic.category_id = it.category_id 
            AND YEAR(it.transaction_date) = p_source_year 
            AND MONTH(it.transaction_date) = p_source_month
    GROUP BY 
        ic.category_id;
        
    -- Trả về kết quả dự đoán
    SELECT 
        ip.year,
        ip.month,
        ic.name AS category_name,
        ip.predicted_amount,
        ip.source_data
    FROM 
        income_predictions ip
    JOIN 
        income_categories ic ON ip.category_id = ic.category_id
    WHERE 
        ip.year = p_target_year AND ip.month = p_target_month;
END //
DELIMITER ;