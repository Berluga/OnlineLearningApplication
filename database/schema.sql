CREATE DATABASE IF NOT EXISTS online_learning DEFAULT CHARACTER SET utf8mb4;
USE online_learning;

CREATE TABLE sys_user (user_id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50) NOT NULL, password VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL, status VARCHAR(20) DEFAULT 'enabled');
CREATE TABLE teacher (teacher_id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT, name VARCHAR(50), title VARCHAR(50), phone VARCHAR(30));
CREATE TABLE department (department_id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL);
CREATE TABLE class_info (class_id BIGINT PRIMARY KEY AUTO_INCREMENT, department_id BIGINT, name VARCHAR(100), grade VARCHAR(20));
CREATE TABLE student (student_id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT, name VARCHAR(50), department_id BIGINT, class_id BIGINT);
CREATE TABLE course (course_id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), subject VARCHAR(100), description TEXT);
CREATE TABLE chapter (chapter_id BIGINT PRIMARY KEY AUTO_INCREMENT, course_id BIGINT, name VARCHAR(100), order_no INT);
CREATE TABLE knowledge_point (point_id BIGINT PRIMARY KEY AUTO_INCREMENT, chapter_id BIGINT, name VARCHAR(100), description TEXT);
CREATE TABLE question_type (type_id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(30) NOT NULL UNIQUE, description TEXT);
INSERT INTO question_type(name, description) VALUES ('选择题', '系统内置题型'), ('填空题', '系统内置题型'), ('主观题', '系统内置题型')
ON DUPLICATE KEY UPDATE description = VALUES(description);
CREATE TABLE question (question_id BIGINT PRIMARY KEY AUTO_INCREMENT, type VARCHAR(30), stem TEXT, difficulty VARCHAR(20), course_id BIGINT, chapter_id BIGINT, creator_id BIGINT);
CREATE TABLE question_option (option_id BIGINT PRIMARY KEY AUTO_INCREMENT, question_id BIGINT, label VARCHAR(10), content TEXT, is_correct BOOLEAN);
CREATE TABLE question_image (image_id BIGINT PRIMARY KEY AUTO_INCREMENT, question_id BIGINT, image_url VARCHAR(255), image_role VARCHAR(20) DEFAULT 'question');
CREATE TABLE answer (answer_id BIGINT PRIMARY KEY AUTO_INCREMENT, question_id BIGINT, answer_content TEXT, analysis TEXT);
CREATE TABLE question_knowledge_point (question_id BIGINT, point_id BIGINT, PRIMARY KEY(question_id, point_id));
CREATE TABLE mistake_point (mistake_id BIGINT PRIMARY KEY AUTO_INCREMENT, question_id BIGINT, point_id BIGINT, description TEXT);
CREATE TABLE paper (paper_id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(100), total_score DECIMAL(6,2), difficulty VARCHAR(20), creator_id BIGINT);
CREATE TABLE paper_question (paper_id BIGINT, question_id BIGINT, score DECIMAL(6,2), order_no INT, PRIMARY KEY(paper_id, question_id));
CREATE TABLE exam (exam_id BIGINT PRIMARY KEY AUTO_INCREMENT, paper_id BIGINT, class_id BIGINT, start_time DATETIME, end_time DATETIME);
CREATE TABLE exam_result (result_id BIGINT PRIMARY KEY AUTO_INCREMENT, exam_id BIGINT, student_id BIGINT, score DECIMAL(6,2), submit_time DATETIME);
CREATE TABLE wrong_question (wrong_id BIGINT PRIMARY KEY AUTO_INCREMENT, result_id BIGINT, question_id BIGINT, point_id BIGINT, reason TEXT);
CREATE TABLE course_selection (selection_id BIGINT PRIMARY KEY AUTO_INCREMENT, student_id BIGINT, course_id BIGINT, selected_time DATETIME);
