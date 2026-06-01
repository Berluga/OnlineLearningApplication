package com.example.learning;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class DemoDataInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public DemoDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            ensureSchemaPatch();
            seedQuestionTypes();
            Long teacherId = seedTeacher();
            Long courseId = seedCourse();
            Long chapterId = seedChapter(courseId);
            Long pointId = seedKnowledgePoint(chapterId);
            List<Long> questionIds = seedQuestions(courseId, chapterId, teacherId, pointId);
            completeExistingQuestionDetails(pointId);
            Long classId = seedStudentBase();
            List<Long> studentIds = seedStudents(classId);
            seedCourseSelections(studentIds, courseId);
            Long paperId = seedPaper(teacherId, questionIds);
            Long examId = seedExam(paperId, classId);
            List<Long> resultIds = seedExamResults(examId, studentIds);
            seedWrongQuestions(resultIds, questionIds, pointId);
        } catch (Exception exception) {
            System.out.println("演示数据初始化失败，已跳过，不影响系统启动：" + exception.getMessage());
        }
    }

    private void ensureSchemaPatch() {
        jdbcTemplate.execute("""
                create table if not exists question_type (
                    type_id bigint primary key auto_increment,
                    name varchar(30) not null unique,
                    description text
                )
                """);
        try {
            jdbcTemplate.execute("alter table question_image add column image_role varchar(20) default 'question'");
        } catch (Exception ignored) {
            // Existing databases already have this column.
        }
    }

    private void seedQuestionTypes() {
        jdbcTemplate.update("insert ignore into question_type(name, description) values (?, ?)", "选择题", "系统内置题型");
        jdbcTemplate.update("insert ignore into question_type(name, description) values (?, ?)", "填空题", "系统内置题型");
        jdbcTemplate.update("insert ignore into question_type(name, description) values (?, ?)", "主观题", "系统内置题型");
    }

    private Long seedTeacher() {
        Long userId = findId("select user_id from sys_user where username = ? limit 1", "teacher_demo");
        if (userId == null) {
            jdbcTemplate.update(
                    "insert into sys_user(username, password, role, status) values (?, ?, ?, ?)",
                    "teacher_demo", "123456", "teacher", "enabled"
            );
            userId = lastId();
        }
        Long teacherId = findId("select teacher_id from teacher where user_id = ? limit 1", userId);
        if (teacherId == null) {
            jdbcTemplate.update(
                    "insert into teacher(user_id, name, title, phone) values (?, ?, ?, ?)",
                    userId, "李老师", "讲师", "13800000001"
            );
            teacherId = lastId();
        }
        return teacherId;
    }

    private Long seedCourse() {
        Long courseId = findId("select course_id from course where name = ? limit 1", "高等数学");
        if (courseId == null) {
            jdbcTemplate.update(
                    "insert into course(name, subject, description) values (?, ?, ?)",
                    "高等数学", "数学", "在线学习系统演示课程"
            );
            courseId = lastId();
        }
        return courseId;
    }

    private Long seedChapter(Long courseId) {
        Long chapterId = findId("select chapter_id from chapter where course_id = ? and name = ? limit 1", courseId, "函数与极限");
        if (chapterId == null) {
            jdbcTemplate.update(
                    "insert into chapter(course_id, name, order_no) values (?, ?, ?)",
                    courseId, "函数与极限", 1
            );
            chapterId = lastId();
        }
        return chapterId;
    }

    private Long seedKnowledgePoint(Long chapterId) {
        Long pointId = findId("select point_id from knowledge_point where chapter_id = ? and name = ? limit 1", chapterId, "极限定义");
        if (pointId == null) {
            jdbcTemplate.update(
                    "insert into knowledge_point(chapter_id, name, description) values (?, ?, ?)",
                    chapterId, "极限定义", "掌握函数极限的直观含义和形式化定义"
            );
            pointId = lastId();
        }
        return pointId;
    }

    private List<Long> seedQuestions(Long courseId, Long chapterId, Long teacherId, Long pointId) {
        if (count("select count(*) from question") == 0) {
            jdbcTemplate.update(
                    "insert into question(type, stem, difficulty, course_id, chapter_id, creator_id) values (?, ?, ?, ?, ?, ?)",
                    "选择题", "当 x 趋近于 0 时，sin x / x 的极限是多少？", "简单", courseId, chapterId, teacherId
            );
            Long choiceId = lastId();
            insertOption(choiceId, "A", "0", false);
            insertOption(choiceId, "B", "1", true);
            insertOption(choiceId, "C", "不存在", false);
            insertOption(choiceId, "D", "无穷大", false);
            jdbcTemplate.update("insert into answer(question_id, answer_content, analysis) values (?, ?, ?)", choiceId, "B", "重要极限：lim sin x / x = 1。");
            jdbcTemplate.update("insert into question_knowledge_point(question_id, point_id) values (?, ?)", choiceId, pointId);

            jdbcTemplate.update(
                    "insert into question(type, stem, difficulty, course_id, chapter_id, creator_id) values (?, ?, ?, ?, ?, ?)",
                    "填空题", "函数 f(x)=x^2 在 x=2 处的导数是____。", "中等", courseId, chapterId, teacherId
            );
            Long blankId = lastId();
            jdbcTemplate.update("insert into answer(question_id, answer_content, analysis) values (?, ?, ?)", blankId, "4", "f'(x)=2x，所以 f'(2)=4。");
            jdbcTemplate.update("insert into question_knowledge_point(question_id, point_id) values (?, ?)", blankId, pointId);

            jdbcTemplate.update(
                    "insert into question(type, stem, difficulty, course_id, chapter_id, creator_id) values (?, ?, ?, ?, ?, ?)",
                    "主观题", "结合图像说明函数极限存在的含义。", "困难", courseId, chapterId, teacherId
            );
            Long subjectiveId = lastId();
            jdbcTemplate.update("insert into question_image(question_id, image_url, image_role) values (?, ?, ?)", subjectiveId, "http://localhost:8080/images/demo-limit-question.png", "question");
            jdbcTemplate.update("insert into question_image(question_id, image_url, image_role) values (?, ?, ?)", subjectiveId, "http://localhost:8080/images/demo-limit-answer.png", "answer");
            jdbcTemplate.update("insert into answer(question_id, answer_content, analysis) values (?, ?, ?)", subjectiveId, "从左右极限趋于同一数值进行说明。", "图像两侧都靠近同一个 y 值时，极限存在。");
            jdbcTemplate.update("insert into question_knowledge_point(question_id, point_id) values (?, ?)", subjectiveId, pointId);

            jdbcTemplate.update(
                    "insert into mistake_point(question_id, point_id, description) values (?, ?, ?)",
                    choiceId, pointId, "容易把函数值与极限值混淆"
            );
        }
        return jdbcTemplate.queryForList("select question_id from question order by question_id asc limit 3", Long.class);
    }

    private void insertOption(Long questionId, String label, String content, boolean correct) {
        jdbcTemplate.update(
                "insert into question_option(question_id, label, content, is_correct) values (?, ?, ?, ?)",
                questionId, label, content, correct
        );
    }

    private Long seedStudentBase() {
        Long departmentId = findId("select department_id from department where name = ? limit 1", "计算机学院");
        if (departmentId == null) {
            jdbcTemplate.update("insert into department(name) values (?)", "计算机学院");
            departmentId = lastId();
        }
        Long classId = findId("select class_id from class_info where name = ? limit 1", "软件工程2301");
        if (classId == null) {
            jdbcTemplate.update(
                    "insert into class_info(department_id, name, grade) values (?, ?, ?)",
                    departmentId, "软件工程2301", "2023"
            );
            classId = lastId();
        }
        return classId;
    }

    private List<Long> seedStudents(Long classId) {
        seedStudent("student_zhangsan", "张三", classId);
        seedStudent("student_lisi", "李四", classId);
        return jdbcTemplate.queryForList("select student_id from student order by student_id asc limit 2", Long.class);
    }

    private void seedStudent(String username, String name, Long classId) {
        Long userId = findId("select user_id from sys_user where username = ? limit 1", username);
        if (userId == null) {
            jdbcTemplate.update(
                    "insert into sys_user(username, password, role, status) values (?, ?, ?, ?)",
                    username, "123456", "student", "enabled"
            );
            userId = lastId();
        }
        if (findId("select student_id from student where user_id = ? limit 1", userId) == null) {
            Long departmentId = findId("select department_id from class_info where class_id = ? limit 1", classId);
            jdbcTemplate.update(
                    "insert into student(user_id, name, department_id, class_id) values (?, ?, ?, ?)",
                    userId, name, departmentId, classId
            );
        }
    }

    private void seedCourseSelections(List<Long> studentIds, Long courseId) {
        if (count("select count(*) from course_selection") == 0) {
            for (Long studentId : studentIds) {
                jdbcTemplate.update(
                        "insert into course_selection(student_id, course_id, selected_time) values (?, ?, ?)",
                        studentId, courseId, LocalDateTime.now().minusDays(3)
                );
            }
        }
    }

    private Long seedPaper(Long teacherId, List<Long> questionIds) {
        Long paperId = findId("select paper_id from paper where title = ? limit 1", "高数第一章演示试卷");
        if (paperId == null) {
            jdbcTemplate.update(
                    "insert into paper(title, total_score, difficulty, creator_id) values (?, ?, ?, ?)",
                    "高数第一章演示试卷", BigDecimal.valueOf(100), "中等", teacherId
            );
            paperId = lastId();
            BigDecimal score = BigDecimal.valueOf(100).divide(BigDecimal.valueOf(questionIds.size()), 2, RoundingMode.HALF_UP);
            for (int i = 0; i < questionIds.size(); i++) {
                jdbcTemplate.update(
                        "insert into paper_question(paper_id, question_id, score, order_no) values (?, ?, ?, ?)",
                        paperId, questionIds.get(i), score, i + 1
                );
            }
        }
        return paperId;
    }

    private Long seedExam(Long paperId, Long classId) {
        Long examId = findId("select exam_id from exam where paper_id = ? and class_id = ? limit 1", paperId, classId);
        if (examId == null) {
            jdbcTemplate.update(
                    "insert into exam(paper_id, class_id, start_time, end_time) values (?, ?, ?, ?)",
                    paperId, classId, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2).plusHours(2)
            );
            examId = lastId();
        }
        return examId;
    }

    private List<Long> seedExamResults(Long examId, List<Long> studentIds) {
        if (count("select count(*) from exam_result where exam_id = " + examId) == 0) {
            BigDecimal[] scores = {BigDecimal.valueOf(86), BigDecimal.valueOf(72)};
            for (int i = 0; i < studentIds.size(); i++) {
                jdbcTemplate.update(
                        "insert into exam_result(exam_id, student_id, score, submit_time) values (?, ?, ?, ?)",
                        examId, studentIds.get(i), scores[Math.min(i, scores.length - 1)], LocalDateTime.now().minusDays(2).plusMinutes(80 + i * 5)
                );
            }
        }
        return jdbcTemplate.queryForList("select result_id from exam_result where exam_id = ? order by result_id asc", Long.class, examId);
    }

    private void seedWrongQuestions(List<Long> resultIds, List<Long> questionIds, Long pointId) {
        if (resultIds.isEmpty() || questionIds.isEmpty() || count("select count(*) from wrong_question") > 0) {
            return;
        }
        jdbcTemplate.update(
                "insert into wrong_question(result_id, question_id, point_id, reason) values (?, ?, ?, ?)",
                resultIds.get(0), questionIds.get(0), pointId, "对重要极限记忆不牢"
        );
        if (resultIds.size() > 1 && questionIds.size() > 1) {
            jdbcTemplate.update(
                    "insert into wrong_question(result_id, question_id, point_id, reason) values (?, ?, ?, ?)",
                    resultIds.get(1), questionIds.get(1), pointId, "导数公式代入错误"
            );
        }
    }

    private void completeExistingQuestionDetails(Long pointId) {
        List<Map<String, Object>> questions = jdbcTemplate.queryForList("select question_id, type, stem from question order by question_id");
        for (Map<String, Object> question : questions) {
            Long questionId = ((Number) question.get("question_id")).longValue();
            String type = String.valueOf(question.get("type"));
            String stem = String.valueOf(question.get("stem"));
            if (type.contains("选择") && count("select count(*) from question_option where question_id = " + questionId) == 0) {
                insertDemoOptions(questionId, stem);
            }
            if (count("select count(*) from answer where question_id = " + questionId) == 0) {
                jdbcTemplate.update(
                        "insert into answer(question_id, answer_content, analysis) values (?, ?, ?)",
                        questionId, demoAnswer(type, stem), "演示数据：用于保证题目与标准答案一一对应，可在题目性质管理中修改。"
                );
            }
            if (pointId != null && count("select count(*) from question_knowledge_point where question_id = " + questionId) == 0) {
                try {
                    jdbcTemplate.update("insert into question_knowledge_point(question_id, point_id) values (?, ?)", questionId, pointId);
                } catch (Exception ignored) {
                    // Ignore duplicate or legacy data issues.
                }
            }
        }
    }

    private void insertDemoOptions(Long questionId, String stem) {
        if (stem.contains("1+1")) {
            insertOptions(questionId, "0", "1", "2", "3", "C");
        } else if (stem.contains("int")) {
            insertOptions(questionId, "1字节", "2字节", "4字节", "8字节", "C");
        } else if (stem.contains("输出")) {
            insertOptions(questionId, "System.out.println()", "Scanner.next()", "new Object()", "main()", "A");
        } else if (stem.contains("基本数据类型")) {
            insertOptions(questionId, "int", "double", "boolean", "String", "D");
        } else if (stem.contains("定义类")) {
            insertOptions(questionId, "class", "extends", "public", "Object", "A");
        } else if (stem.contains("访问修饰符")) {
            insertOptions(questionId, "int", "void", "public", "class", "C");
        } else if (stem.contains("继承")) {
            insertOptions(questionId, "implements", "extends", "interface", "package", "B");
        } else if (stem.contains("父类")) {
            insertOptions(questionId, "String", "System", "Class", "Object", "D");
        } else if (stem.contains("异常")) {
            insertOptions(questionId, "try/catch/finally", "class/interface", "public/private", "new/this", "A");
        } else {
            insertOptions(questionId, "选项A", "选项B", "选项C", "选项D", demoAnswer("选择题", stem));
        }
    }

    private void insertOptions(Long questionId, String a, String b, String c, String d, String correct) {
        insertOption(questionId, "A", a, "A".equals(correct));
        insertOption(questionId, "B", b, "B".equals(correct));
        insertOption(questionId, "C", c, "C".equals(correct));
        insertOption(questionId, "D", d, "D".equals(correct));
    }

    private String demoAnswer(String type, String stem) {
        if (stem.contains("1+1")) {
            return "C";
        }
        if (stem.contains("int")) {
            return "C";
        }
        if (stem.contains("输出")) {
            return "A";
        }
        if (stem.contains("基本数据类型")) {
            return "D";
        }
        if (stem.contains("定义类")) {
            return type.contains("填空") ? "class" : "A";
        }
        if (stem.contains("访问修饰符")) {
            return "C";
        }
        if (stem.contains("继承")) {
            return type.contains("填空") ? "extends" : "B";
        }
        if (stem.contains("父类")) {
            return "D";
        }
        if (stem.contains("异常")) {
            return type.contains("填空") ? "throw" : "A";
        }
        if (stem.contains("接口")) {
            return type.contains("填空") ? "implements" : "正确";
        }
        if (stem.contains("字符串")) {
            return "String";
        }
        if (stem.contains("逻辑真值")) {
            return "boolean";
        }
        if (stem.contains("数组")) {
            return "[]";
        }
        if (type.contains("判断")) {
            return "正确";
        }
        if (type.contains("简答") || type.contains("主观")) {
            return "参考答案：围绕题干关键概念作答，说明定义、特点和使用场景。";
        }
        return "参考答案";
    }

    private Long findId(String sql, Object... args) {
        List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, args);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private int count(String sql) {
        try {
            Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
            return value == null ? 0 : value;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private Long lastId() {
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }
}
