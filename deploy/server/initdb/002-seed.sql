USE ai_assistant;

INSERT INTO manager(username, real_name, pwd, sex)
SELECT 'admin', '管理员', '$2a$10$hldTxXAVvdOLa4DpTl5jMuyJRZo0c0L/iHi0oF2qZvfObm3kYvgqy', 1
WHERE NOT EXISTS (
  SELECT 1 FROM manager WHERE username = 'admin'
);
