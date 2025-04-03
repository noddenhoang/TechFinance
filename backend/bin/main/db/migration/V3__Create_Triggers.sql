-- Trigger sau khi thêm giao dịch thu nhập
DELIMITER //
CREATE TRIGGER after_income_transaction_insert
AFTER INSERT ON income_transactions
FOR EACH ROW
BEGIN
    CALL sp_generate_monthly_report(YEAR(NEW.transaction_date), MONTH(NEW.transaction_date));
END //
DELIMITER ;

-- Trigger sau khi thêm giao dịch chi phí
DELIMITER //
CREATE TRIGGER after_expense_transaction_insert
AFTER INSERT ON expense_transactions
FOR EACH ROW
BEGIN
    CALL sp_generate_monthly_report(YEAR(NEW.transaction_date), MONTH(NEW.transaction_date));
END //
DELIMITER ; 