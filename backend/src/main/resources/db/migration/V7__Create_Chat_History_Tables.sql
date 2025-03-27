-- Bảng cuộc hội thoại
CREATE TABLE chat_conversations (
    conversation_id VARCHAR(36) PRIMARY KEY,
    user_id INT NOT NULL,
    conversation_type ENUM('GENERAL', 'REVENUE_PREDICTION') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Bảng tin nhắn
CREATE TABLE chat_messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    sender ENUM('USER', 'AI') NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES chat_conversations(conversation_id) ON DELETE CASCADE
);

-- Bảng dữ liệu đầu vào cho dự đoán
CREATE TABLE prediction_inputs (
    input_id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    business_data TEXT,
    time_period VARCHAR(100),
    industry_type VARCHAR(100),
    market_conditions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES chat_conversations(conversation_id) ON DELETE CASCADE
);

-- Index để tăng tốc truy vấn
CREATE INDEX idx_conversation_user ON chat_conversations(user_id);
CREATE INDEX idx_chat_messages_conversation ON chat_messages(conversation_id);
CREATE INDEX idx_prediction_inputs_conversation ON prediction_inputs(conversation_id); 