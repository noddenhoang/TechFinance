-- Users
INSERT INTO users (username, password, full_name, role, is_active) VALUES
('admin', '$2a$10$5JHB/zs265RbAoMUOY.V9edv8w.AM93bAp6wfON6WZ9FiAhzgA/8W', 'Admin', 'admin', true),
('user', '$2a$12$qkBOl/DqlKkvRgCiyUihD.TLECiqELoAqYhkqkdwDZW8oI18bmJ7.', 'Viewer', 'user', true);