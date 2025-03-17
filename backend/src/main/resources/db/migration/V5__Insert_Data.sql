-- Users
INSERT INTO users (username, password, full_name, email, phone, role, is_active) VALUES
('admin', '$2a$10$5JHB/zs265RbAoMUOY.V9edv8w.AM93bAp6wfON6WZ9FiAhzgA/8W', 'Admin User', 'admin@techfinance.com', '0123456789', 'admin', true),
('user1', '$2a$10$5JHB/zs265RbAoMUOY.V9edv8w.AM93bAp6wfON6WZ9FiAhzgA/8W', 'Standard User', 'user1@techfinance.com', '0123456788', 'user', true);
-- Note: Password is 'password' for all users, hashed with BCrypt

-- Income Categories
INSERT INTO income_categories (name, description, is_active, created_by) VALUES
('Khoá học Java Backend', 'Thu nhập từ khoá học Java Backend', true, 1),
('Khoá học Python - AI', 'Thu nhập từ Khoá học Python - AI', true, 1),
('Khoá học VueJS', 'Thu nhập từ Khoá học VueJS', true, 1),
('Khoá học Tester thực chiến', 'Thu nhập từ Khoá học Tester thực chiến', true, 1),
('Phần mềm quản lý nhân sự', 'Thu nhập từ phần mềm quản lý nhân sự', true, 1),
('Phần mềm kế toán', 'Thu nhập từ phần mềm kế toán', true, 1),
('Dịch vụ tư vấn IT', 'Thu nhập từ dịch vụ tư vấn IT', true, 1),
('Dịch vụ bảo trì hệ thống', 'Thu nhập từ dịch vụ bảo trì hệ thống', true, 1);

-- Expense Categories
INSERT INTO expense_categories (name, description, is_active, created_by) VALUES
('Lương nhân viên', 'Chi phí lương cho nhân viên công ty', true, 1),
('Thuê văn phòng', 'Chi phí thuê văn phòng làm việc', true, 1),
('Thiết bị IT', 'Chi phí mua máy tính, thiết bị phần cứng', true, 1),
('Phần mềm và license', 'Chi phí mua license phần mềm phục vụ phát triển', true, 1),
('Marketing online', 'Chi phí quảng cáo online (Google, Facebook, etc)', true, 1),
('Đào tạo nhân viên', 'Chi phí đào tạo và phát triển nhân viên', true, 1),
('Cơ sở hạ tầng IT', 'Chi phí server, hosting, cloud services', true, 1),
('Điện nước internet', 'Chi phí tiện ích văn phòng', true, 1),
('Văn phòng phẩm', 'Chi phí văn phòng phẩm', true, 1);

-- Customers
INSERT INTO customers (name, email, phone, address, tax_code, notes, is_active, created_by) VALUES
('Công ty TNHH Phần mềm Sài Gòn', 'contact@saigonsoftware.com', '0987654321', 'Số 123 Đường Lê Lợi, Quận 1, TP.HCM', 'SGS123456', 'Khách hàng mua phần mềm quản lý nhân sự', true, 1),
('Trường Đại học Bách Khoa', 'training@hcmut.edu.vn', '0987654322', 'Số 268 Lý Thường Kiệt, Quận 10, TP.HCM', 'BKU789012', 'Đối tác đào tạo khóa học Python-AI', true, 1),
('Công ty CP Giải pháp Công nghệ TechSolutions', 'hr@techsolutions.vn', '0987654323', 'Số 56 Nguyễn Đình Chiểu, Quận 3, TP.HCM', 'TSL345678', 'Khách hàng đăng ký nhiều khóa học cho nhân viên', true, 1),
('Ngân hàng TMCP Á Châu', 'it@acb.com.vn', '0987654324', 'Số 442 Nguyễn Thị Minh Khai, Quận 3, TP.HCM', 'ACB234567', 'Khách hàng mua phần mềm kế toán', true, 1),
('Công ty TNHH Thương mại điện tử Tiki', 'dev@tiki.vn', '0987654325', 'Số 29 Lê Duẩn, Quận 1, TP.HCM', 'TIK567890', 'Khách hàng đào tạo VueJS', true, 1);

-- Suppliers
INSERT INTO suppliers (name, email, phone, address, tax_code, notes, is_active, created_by) VALUES
('Công ty TNHH Thiết bị Công nghệ HiTech', 'sales@hitechequipment.vn', '0912345678', 'Số 45 Trần Hưng Đạo, Quận 1, TP.HCM', 'HTC123456', 'Nhà cung cấp thiết bị IT', true, 1),
('Amazon Web Services', 'aws-support@amazon.com', '0912345679', 'Singapore', 'AWS789012', 'Nhà cung cấp dịch vụ cloud', true, 1),
('Microsoft Vietnam', 'msales@microsoft.com', '0912345680', 'Số 72 Lê Thánh Tôn, Quận 1, TP.HCM', 'MSV345678', 'Nhà cung cấp license phần mềm', true, 1),
('Công ty TNHH Văn phòng Phẩm Hoàng Phát', 'sales@hoangphat.com', '0912345681', 'Số 28 Pasteur, Quận 1, TP.HCM', 'HPT567890', 'Nhà cung cấp văn phòng phẩm', true, 1),
('Công ty Quảng cáo Số ADPlus', 'contact@adplus.vn', '0912345682', 'Số 15 Thái Văn Lung, Quận 1, TP.HCM', 'ADP678901', 'Đối tác marketing online', true, 1);

-- Income Transactions for 2024
-- January
INSERT INTO income_transactions (category_id, customer_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(1, 3, '2024-01-05', 25000000, 'RECEIVED', 'Khóa học Java Backend - 10 nhân viên', 'JAVA-JAN-001', 1),
(2, 2, '2024-01-10', 30000000, 'RECEIVED', 'Khóa học Python-AI - Sinh viên năm cuối', 'PY-JAN-001', 1),
(5, 1, '2024-01-15', 45000000, 'RECEIVED', 'License phần mềm quản lý nhân sự - 50 user', 'HRMS-JAN-001', 1),
(7, 4, '2024-01-20', 18000000, 'RECEIVED', 'Tư vấn hệ thống bảo mật ngân hàng', 'CONS-JAN-001', 1),
(3, 5, '2024-01-25', 20000000, 'RECEIVED', 'Khóa học VueJS - 8 nhân viên', 'VUE-JAN-001', 1);

-- February
INSERT INTO income_transactions (category_id, customer_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(1, 3, '2024-02-05', 30000000, 'RECEIVED', 'Khóa học Java Backend - 12 nhân viên', 'JAVA-FEB-001', 1),
(6, 4, '2024-02-15', 55000000, 'RECEIVED', 'License phần mềm kế toán - 20 user', 'ACCT-FEB-001', 1),
(4, 5, '2024-02-22', 18000000, 'RECEIVED', 'Khóa học Tester thực chiến - 6 nhân viên', 'TEST-FEB-001', 1),
(8, 1, '2024-02-28', 15000000, 'PENDING', 'Bảo trì hệ thống Q1/2024', 'MAINT-Q1-001', 1);

-- March 
INSERT INTO income_transactions (category_id, customer_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(2, 2, '2024-03-05', 35000000, 'RECEIVED', 'Khóa học Python-AI - Sinh viên cao học', 'PY-MAR-001', 1),
(5, 5, '2024-03-12', 40000000, 'RECEIVED', 'License phần mềm quản lý nhân sự - 45 user', 'HRMS-MAR-001', 1),
(7, 3, '2024-03-18', 22000000, 'PENDING', 'Tư vấn chuyển đổi hệ thống', 'CONS-MAR-001', 1),
(1, 4, '2024-03-26', 28000000, 'RECEIVED', 'Khóa học Java Backend - 11 nhân viên', 'JAVA-MAR-001', 1);

-- Expense Transactions for 2024
-- January
INSERT INTO expense_transactions (category_id, supplier_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(1, 1, '2024-01-05', 85000000, 'PAID', 'Lương nhân viên tháng 1/2024', 'SAL-JAN-001', 1),
(2, 2, '2024-01-08', 25000000, 'PAID', 'Thuê văn phòng Q1/2024', 'RENT-Q1-001', 1),
(8, 3, '2024-01-10', 5500000, 'PAID', 'Tiền điện nước internet tháng 1', 'UTIL-JAN-001', 1),
(3, 1, '2024-01-15', 18000000, 'PAID', 'Mua 2 laptop Dell cho nhân viên mới', 'IT-JAN-001', 1),
(5, 5, '2024-01-28', 12000000, 'PAID', 'Quảng cáo Facebook tháng 1', 'ADS-JAN-001', 1),
(4, 3, '2024-01-20', 15000000, 'PAID', 'License phần mềm Adobe Creative Cloud', 'SW-JAN-001', 1);

-- February
INSERT INTO expense_transactions (category_id, supplier_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(1, 1, '2024-02-05', 85000000, 'PAID', 'Lương nhân viên tháng 2/2024', 'SAL-FEB-001', 1),
(8, 3, '2024-02-08', 5800000, 'PAID', 'Tiền điện nước internet tháng 2', 'UTIL-FEB-001', 1),
(9, 4, '2024-02-10', 3200000, 'PAID', 'Văn phòng phẩm tháng 2', 'SUPP-FEB-001', 1),
(5, 5, '2024-02-15', 14000000, 'PAID', 'Quảng cáo Google Ads tháng 2', 'ADS-FEB-001', 1),
(7, 2, '2024-02-22', 8000000, 'UNPAID', 'Dịch vụ AWS tháng 2', 'CLOUD-FEB-001', 1),
(6, 3, '2024-02-25', 7500000, 'PAID', 'Khóa học nâng cao cho đội developer', 'TRAIN-FEB-001', 1);

-- March
INSERT INTO expense_transactions (category_id, supplier_id, transaction_date, amount, payment_status, description, reference_no, created_by) VALUES
(1, 1, '2024-03-05', 88000000, 'PAID', 'Lương nhân viên tháng 3/2024', 'SAL-MAR-001', 1),
(8, 3, '2024-03-08', 6200000, 'PAID', 'Tiền điện nước internet tháng 3', 'UTIL-MAR-001', 1),
(9, 4, '2024-03-10', 2800000, 'PAID', 'Văn phòng phẩm tháng 3', 'SUPP-MAR-001', 1),
(3, 1, '2024-03-15', 22000000, 'PAID', 'Mua thiết bị server mới', 'IT-MAR-001', 1),
(5, 5, '2024-03-18', 18000000, 'PAID', 'Quảng cáo TikTok + Facebook tháng 3', 'ADS-MAR-001', 1),
(7, 2, '2024-03-25', 9500000, 'UNPAID', 'Dịch vụ AWS tháng 3', 'CLOUD-MAR-001', 1);

-- Income Budgets for 2024
INSERT INTO income_budgets (category_id, year, month, amount, notes, created_by) VALUES
(1, 2024, 1, 30000000, 'Dự toán thu khóa học Java Backend tháng 1/2024', 1),
(2, 2024, 1, 35000000, 'Dự toán thu khóa học Python-AI tháng 1/2024', 1),
(3, 2024, 1, 25000000, 'Dự toán thu khóa học VueJS tháng 1/2024', 1),
(4, 2024, 1, 15000000, 'Dự toán thu khóa học Tester tháng 1/2024', 1),
(5, 2024, 1, 50000000, 'Dự toán thu phần mềm quản lý nhân sự tháng 1/2024', 1),
(6, 2024, 1, 40000000, 'Dự toán thu phần mềm kế toán tháng 1/2024', 1),
(7, 2024, 1, 20000000, 'Dự toán thu dịch vụ tư vấn IT tháng 1/2024', 1),
(8, 2024, 1, 10000000, 'Dự toán thu dịch vụ bảo trì tháng 1/2024', 1),

(1, 2024, 2, 35000000, 'Dự toán thu khóa học Java Backend tháng 2/2024', 1),
(2, 2024, 2, 30000000, 'Dự toán thu khóa học Python-AI tháng 2/2024', 1),
(3, 2024, 2, 20000000, 'Dự toán thu khóa học VueJS tháng 2/2024', 1),
(4, 2024, 2, 20000000, 'Dự toán thu khóa học Tester tháng 2/2024', 1),
(5, 2024, 2, 45000000, 'Dự toán thu phần mềm quản lý nhân sự tháng 2/2024', 1),
(6, 2024, 2, 50000000, 'Dự toán thu phần mềm kế toán tháng 2/2024', 1),
(7, 2024, 2, 15000000, 'Dự toán thu dịch vụ tư vấn IT tháng 2/2024', 1),
(8, 2024, 2, 20000000, 'Dự toán thu dịch vụ bảo trì tháng 2/2024', 1),

(1, 2024, 3, 30000000, 'Dự toán thu khóa học Java Backend tháng 3/2024', 1),
(2, 2024, 3, 40000000, 'Dự toán thu khóa học Python-AI tháng 3/2024', 1),
(3, 2024, 3, 25000000, 'Dự toán thu khóa học VueJS tháng 3/2024', 1),
(4, 2024, 3, 15000000, 'Dự toán thu khóa học Tester tháng 3/2024', 1),
(5, 2024, 3, 40000000, 'Dự toán thu phần mềm quản lý nhân sự tháng 3/2024', 1),
(6, 2024, 3, 35000000, 'Dự toán thu phần mềm kế toán tháng 3/2024', 1),
(7, 2024, 3, 25000000, 'Dự toán thu dịch vụ tư vấn IT tháng 3/2024', 1),
(8, 2024, 3, 15000000, 'Dự toán thu dịch vụ bảo trì tháng 3/2024', 1);

-- Expense Budgets for 2024
INSERT INTO expense_budgets (category_id, year, month, amount, notes, created_by) VALUES
(1, 2024, 1, 90000000, 'Dự toán chi lương nhân viên tháng 1/2024', 1),
(2, 2024, 1, 25000000, 'Dự toán chi thuê văn phòng tháng 1/2024', 1),
(3, 2024, 1, 20000000, 'Dự toán chi thiết bị IT tháng 1/2024', 1),
(4, 2024, 1, 15000000, 'Dự toán chi phần mềm và license tháng 1/2024', 1),
(5, 2024, 1, 15000000, 'Dự toán chi marketing online tháng 1/2024', 1),
(6, 2024, 1, 8000000, 'Dự toán chi đào tạo nhân viên tháng 1/2024', 1),
(7, 2024, 1, 10000000, 'Dự toán chi cơ sở hạ tầng IT tháng 1/2024', 1),
(8, 2024, 1, 6000000, 'Dự toán chi điện nước internet tháng 1/2024', 1),
(9, 2024, 1, 3500000, 'Dự toán chi văn phòng phẩm tháng 1/2024', 1),

(1, 2024, 2, 90000000, 'Dự toán chi lương nhân viên tháng 2/2024', 1),
(2, 2024, 2, 0, 'Dự toán chi thuê văn phòng tháng 2/2024', 1),
(3, 2024, 2, 10000000, 'Dự toán chi thiết bị IT tháng 2/2024', 1),
(4, 2024, 2, 5000000, 'Dự toán chi phần mềm và license tháng 2/2024', 1),
(5, 2024, 2, 15000000, 'Dự toán chi marketing online tháng 2/2024', 1),
(6, 2024, 2, 10000000, 'Dự toán chi đào tạo nhân viên tháng 2/2024', 1),
(7, 2024, 2, 8000000, 'Dự toán chi cơ sở hạ tầng IT tháng 2/2024', 1),
(8, 2024, 2, 6000000, 'Dự toán chi điện nước internet tháng 2/2024', 1),
(9, 2024, 2, 3000000, 'Dự toán chi văn phòng phẩm tháng 2/2024', 1),

(1, 2024, 3, 90000000, 'Dự toán chi lương nhân viên tháng 3/2024', 1),
(2, 2024, 3, 0, 'Dự toán chi thuê văn phòng tháng 3/2024', 1),
(3, 2024, 3, 25000000, 'Dự toán chi thiết bị IT tháng 3/2024', 1),
(4, 2024, 3, 8000000, 'Dự toán chi phần mềm và license tháng 3/2024', 1),
(5, 2024, 3, 20000000, 'Dự toán chi marketing online tháng 3/2024', 1),
(6, 2024, 3, 5000000, 'Dự toán chi đào tạo nhân viên tháng 3/2024', 1),
(7, 2024, 3, 10000000, 'Dự toán chi cơ sở hạ tầng IT tháng 3/2024', 1),
(8, 2024, 3, 7000000, 'Dự toán chi điện nước internet tháng 3/2024', 1),
(9, 2024, 3, 3500000, 'Dự toán chi văn phòng phẩm tháng 3/2024', 1);

-- Create some financial reports
INSERT INTO financial_reports (report_type, year, month, total_income, total_expense, profit_loss, receivables, payables, created_by) VALUES
('SUMMARY', 2024, 1, 138000000, 160500000, -22500000, 0, 0, 1),
('SUMMARY', 2024, 2, 118000000, 123500000, -5500000, 15000000, 8000000, 1),
('SUMMARY', 2024, 3, 125000000, 146500000, -21500000, 22000000, 9500000, 1);