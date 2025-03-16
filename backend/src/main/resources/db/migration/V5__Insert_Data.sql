-- Users
INSERT INTO users (username, password, full_name, email, phone, role, is_active) VALUES
('admin', '$2a$10$5JHB/zs265RbAoMUOY.V9edv8w.AM93bAp6wfON6WZ9FiAhzgA/8W', 'Admin User', 'admin@techfinance.com', '0123456789', 'admin', true),
('user1', '$2a$10$5JHB/zs265RbAoMUOY.V9edv8w.AM93bAp6wfON6WZ9FiAhzgA/8W', 'Standard User', 'user1@techfinance.com', '0123456788', 'user', true);
-- Note: Password is 'password' for all users, hashed with BCrypt

-- Income Categories
INSERT INTO income_categories (name, description, is_active, created_by) VALUES
('Lương cơ bản', 'Thu nhập từ lương hàng tháng', true, 1),
('Thưởng', 'Thưởng dự án, thưởng hiệu suất', true, 1),
('Đầu tư', 'Thu nhập từ đầu tư, cổ phiếu, chứng khoán', true, 1),
('Tư vấn', 'Thu nhập từ dịch vụ tư vấn', true, 1),
('Bán hàng', 'Thu nhập từ hoạt động bán hàng', true, 1);

-- Expense Categories
INSERT INTO expense_categories (name, description, is_active, created_by) VALUES
('Văn phòng phẩm', 'Chi phí văn phòng phẩm', true, 1),
('Tiền điện', 'Chi phí tiền điện văn phòng', true, 1),
('Tiền nước', 'Chi phí tiền nước văn phòng', true, 1),
('Thuê văn phòng', 'Chi phí thuê địa điểm làm việc', true, 1),
('Lương nhân viên', 'Chi phí trả lương nhân viên', true, 1),
('Marketing', 'Chi phí quảng cáo, tiếp thị', true, 1),
('Phần mềm', 'Chi phí mua license phần mềm', true, 1);

-- Customers
INSERT INTO customers (name, email, phone, address, tax_code, notes, is_active, created_by) VALUES
('Công ty TNHH ABC', 'contact@abc.com', '0987654321', 'Số 123 Đường Lê Lợi, Quận 1, TP.HCM', 'ABC123456', 'Khách hàng lớn, thanh toán đúng hạn', true, 1),
('Công ty CP XYZ', 'info@xyz.com', '0987654322', 'Số 456 Đường Nguyễn Huệ, Quận 1, TP.HCM', 'XYZ789012', 'Khách hàng thường xuyên', true, 1),
('Doanh nghiệp tư nhân MNO', 'sales@mno.com', '0987654323', 'Số 789 Đường Lê Duẩn, Quận 3, TP.HCM', 'MNO345678', 'Doanh nghiệp nhỏ, đối tác mới', true, 1);

-- Suppliers
INSERT INTO suppliers (name, email, phone, address, tax_code, notes, is_active, created_by) VALUES
('Công ty Cung ứng DEF', 'supply@def.com', '0912345678', 'Số 321 Đường Võ Văn Tần, Quận 3, TP.HCM', 'DEF123456', 'Nhà cung cấp văn phòng phẩm chính', true, 1),
('Công ty Thiết bị GHI', 'sales@ghi.com', '0912345679', 'Số 654 Đường Cách Mạng Tháng 8, Quận 3, TP.HCM', 'GHI789012', 'Nhà cung cấp thiết bị điện tử', true, 1),
('Dịch vụ JKL', 'contact@jkl.com', '0912345680', 'Số 987 Đường Điện Biên Phủ, Quận 10, TP.HCM', 'JKL345678', 'Cung cấp dịch vụ vệ sinh, bảo trì', true, 1);

-- Income Transactions for 2024
-- January
INSERT INTO income_transactions (category_id, customer_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(1, 1, '2024-01-05', 50000000, 'RECEIVED', 'Lương tháng 1/2024', 'SAL-JAN24', 1),
(2, 1, '2024-01-10', 10000000, 'RECEIVED', 'Thưởng dự án Q4/2023', 'BONUS-Q4', 1),
(3, 2, '2024-01-15', 5000000, 'RECEIVED', 'Cổ tức quý 4/2023', 'DIV-Q4', 1),
(4, 3, '2024-01-20', 15000000, 'RECEIVED', 'Tư vấn chiến lược kinh doanh', 'CONS-JAN24-01', 1),
(5, 2, '2024-01-25', 8000000, 'RECEIVED', 'Doanh số bán hàng tháng 1', 'SALES-JAN24', 1);

-- February
INSERT INTO income_transactions (category_id, customer_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(1, 1, '2024-02-05', 50000000, 'RECEIVED', 'Lương tháng 2/2024', 'SAL-FEB24', 1),
(4, 2, '2024-02-15', 18000000, 'RECEIVED', 'Tư vấn quy trình sản xuất', 'CONS-FEB24-01', 1),
(5, 3, '2024-02-22', 9500000, 'RECEIVED', 'Doanh số bán hàng tháng 2', 'SALES-FEB24', 1),
(3, 1, '2024-02-28', 4800000, 'PENDING', 'Cổ tức quý 1/2024', 'DIV-Q1', 1);

-- March 
INSERT INTO income_transactions (category_id, customer_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(1, 1, '2024-03-05', 50000000, 'RECEIVED', 'Lương tháng 3/2024', 'SAL-MAR24', 1),
(2, 2, '2024-03-12', 12000000, 'RECEIVED', 'Thưởng dự án Q1/2024', 'BONUS-Q1', 1),
(4, 3, '2024-03-18', 20000000, 'PENDING', 'Tư vấn mở rộng thị trường', 'CONS-MAR24-01', 1),
(5, 1, '2024-03-26', 10500000, 'RECEIVED', 'Doanh số bán hàng tháng 3', 'SALES-MAR24', 1);

-- Expense Transactions for 2024
-- January
INSERT INTO expense_transactions (category_id, supplier_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(1, 1, '2024-01-05', 2000000, 'PAID', 'Văn phòng phẩm tháng 1', 'EXP-JAN24-01', 1),
(2, 2, '2024-01-08', 5000000, 'PAID', 'Tiền điện tháng 1', 'EXP-JAN24-02', 1),
(3, 3, '2024-01-10', 1500000, 'PAID', 'Tiền nước tháng 1', 'EXP-JAN24-03', 1),
(4, 1, '2024-01-15', 20000000, 'PAID', 'Thuê văn phòng Q1/2024', 'EXP-JAN24-04', 1),
(5, 2, '2024-01-28', 120000000, 'PAID', 'Lương nhân viên tháng 1', 'EXP-JAN24-05', 1),
(6, 3, '2024-01-20', 15000000, 'PAID', 'Chi phí quảng cáo Facebook', 'EXP-JAN24-06', 1);

-- February
INSERT INTO expense_transactions (category_id, supplier_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(1, 1, '2024-02-05', 1800000, 'PAID', 'Văn phòng phẩm tháng 2', 'EXP-FEB24-01', 1),
(2, 2, '2024-02-08', 5200000, 'PAID', 'Tiền điện tháng 2', 'EXP-FEB24-02', 1),
(3, 3, '2024-02-10', 1600000, 'PAID', 'Tiền nước tháng 2', 'EXP-FEB24-03', 1),
(5, 1, '2024-02-28', 120000000, 'PAID', 'Lương nhân viên tháng 2', 'EXP-FEB24-04', 1),
(6, 2, '2024-02-15', 18000000, 'PAID', 'Chi phí quảng cáo Google Ads', 'EXP-FEB24-05', 1),
(7, 3, '2024-02-22', 8000000, 'UNPAID', 'Gia hạn license Microsoft Office', 'EXP-FEB24-06', 1);

-- March
INSERT INTO expense_transactions (category_id, supplier_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(1, 1, '2024-03-05', 2100000, 'PAID', 'Văn phòng phẩm tháng 3', 'EXP-MAR24-01', 1),
(2, 2, '2024-03-08', 5500000, 'PAID', 'Tiền điện tháng 3', 'EXP-MAR24-02', 1),
(3, 3, '2024-03-10', 1700000, 'PAID', 'Tiền nước tháng 3', 'EXP-MAR24-03', 1),
(5, 1, '2024-03-28', 125000000, 'UNPAID', 'Lương nhân viên tháng 3', 'EXP-MAR24-04', 1),
(6, 2, '2024-03-18', 20000000, 'PAID', 'Chi phí quảng cáo TikTok', 'EXP-MAR24-05', 1);

-- Income Budgets for 2024
INSERT INTO income_budgets (category_id, year, month, amount, notes, created_by) VALUES
(1, 2024, 1, 50000000, 'Dự toán lương tháng 1/2024', 1),
(2, 2024, 1, 15000000, 'Dự toán thưởng tháng 1/2024', 1),
(3, 2024, 1, 5000000, 'Dự toán đầu tư tháng 1/2024', 1),
(4, 2024, 1, 20000000, 'Dự toán tư vấn tháng 1/2024', 1),
(5, 2024, 1, 10000000, 'Dự toán bán hàng tháng 1/2024', 1),

(1, 2024, 2, 50000000, 'Dự toán lương tháng 2/2024', 1),
(2, 2024, 2, 0, 'Dự toán thưởng tháng 2/2024', 1),
(3, 2024, 2, 5000000, 'Dự toán đầu tư tháng 2/2024', 1),
(4, 2024, 2, 15000000, 'Dự toán tư vấn tháng 2/2024', 1),
(5, 2024, 2, 12000000, 'Dự toán bán hàng tháng 2/2024', 1),

(1, 2024, 3, 50000000, 'Dự toán lương tháng 3/2024', 1),
(2, 2024, 3, 10000000, 'Dự toán thưởng tháng 3/2024', 1),
(3, 2024, 3, 5000000, 'Dự toán đầu tư tháng 3/2024', 1),
(4, 2024, 3, 25000000, 'Dự toán tư vấn tháng 3/2024', 1),
(5, 2024, 3, 15000000, 'Dự toán bán hàng tháng 3/2024', 1);

-- Expense Budgets for 2024
INSERT INTO expense_budgets (category_id, year, month, amount, notes, created_by) VALUES
(1, 2024, 1, 2500000, 'Dự toán văn phòng phẩm tháng 1/2024', 1),
(2, 2024, 1, 5000000, 'Dự toán tiền điện tháng 1/2024', 1),
(3, 2024, 1, 2000000, 'Dự toán tiền nước tháng 1/2024', 1),
(4, 2024, 1, 20000000, 'Dự toán thuê văn phòng tháng 1/2024', 1),
(5, 2024, 1, 130000000, 'Dự toán lương nhân viên tháng 1/2024', 1),
(6, 2024, 1, 15000000, 'Dự toán marketing tháng 1/2024', 1),
(7, 2024, 1, 0, 'Dự toán phần mềm tháng 1/2024', 1),

(1, 2024, 2, 2500000, 'Dự toán văn phòng phẩm tháng 2/2024', 1),
(2, 2024, 2, 5500000, 'Dự toán tiền điện tháng 2/2024', 1),
(3, 2024, 2, 2000000, 'Dự toán tiền nước tháng 2/2024', 1),
(4, 2024, 2, 0, 'Dự toán thuê văn phòng tháng 2/2024', 1),
(5, 2024, 2, 130000000, 'Dự toán lương nhân viên tháng 2/2024', 1),
(6, 2024, 2, 20000000, 'Dự toán marketing tháng 2/2024', 1),
(7, 2024, 2, 10000000, 'Dự toán phần mềm tháng 2/2024', 1),

(1, 2024, 3, 2500000, 'Dự toán văn phòng phẩm tháng 3/2024', 1),
(2, 2024, 3, 6000000, 'Dự toán tiền điện tháng 3/2024', 1),
(3, 2024, 3, 2000000, 'Dự toán tiền nước tháng 3/2024', 1),
(4, 2024, 3, 0, 'Dự toán thuê văn phòng tháng 3/2024', 1),
(5, 2024, 3, 130000000, 'Dự toán lương nhân viên tháng 3/2024', 1),
(6, 2024, 3, 25000000, 'Dự toán marketing tháng 3/2024', 1),
(7, 2024, 3, 0, 'Dự toán phần mềm tháng 3/2024', 1);

-- Create some financial reports
INSERT INTO financial_reports (report_type, year, month, total_income, total_expense, profit_loss, receivables, payables, created_by) VALUES
('SUMMARY', 2024, 1, 88000000, 163500000, -75500000, 0, 0, 1),
('SUMMARY', 2024, 2, 82300000, 154600000, -72300000, 4800000, 8000000, 1),
('SUMMARY', 2024, 3, 92500000, 154300000, -61800000, 20000000, 125000000, 1);