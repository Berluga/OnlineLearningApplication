package com.example.learning.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/student")
public class StudentController {
    private final JdbcTemplate jdbcTemplate;

    public StudentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> list() {
        ensureStudentDemoData();
        return Map.of(
                "subsystem", "学生管理子系统",
                "useCases", List.of("查询学生信息", "学生的院系班级分配", "学生以班级为单位选课", "学生管理（增删查改）", "学生情况分析（历次考试成绩波动图、错误知识点统计报告）"),
                "students", jdbcTemplate.queryForList("""
                        select s.student_id, s.name, s.department_id, d.name as department_name,
                               s.class_id, c.name as class_name, c.grade
                        from student s
                        left join department d on s.department_id = d.department_id
                        left join class_info c on s.class_id = c.class_id
                        order by s.student_id desc
                        """),
                "departments", jdbcTemplate.queryForList("select department_id, name from department order by department_id desc"),
                "classes", jdbcTemplate.queryForList("""
                        select c.class_id, c.department_id, d.name as department_name, c.name, c.grade
                        from class_info c
                        left join department d on c.department_id = d.department_id
                        order by c.class_id desc
                        """),
                "courses", jdbcTemplate.queryForList("select course_id, name, subject, description from course order by course_id desc"),
                "selections", jdbcTemplate.queryForList("""
                        select cs.selection_id, cs.student_id, s.name as student_name, s.class_id,
                               ci.name as class_name, cs.course_id, co.name as course_name, cs.selected_time
                        from course_selection cs
                        left join student s on cs.student_id = s.student_id
                        left join class_info ci on s.class_id = ci.class_id
                        left join course co on cs.course_id = co.course_id
                        order by cs.selection_id desc
                        """)
        );
    }

    @PostMapping
    public Map<String, Object> createStudent(@RequestBody Map<String, Object> body) {
        Long classId = nullableLong(body.get("classId"));
        Long departmentId = nullableLong(body.get("departmentId"));
        if (departmentId == null && classId != null) {
            departmentId = jdbcTemplate.queryForObject(
                    "select department_id from class_info where class_id = ?",
                    Long.class,
                    classId
            );
        }
        jdbcTemplate.update(
                "insert into student(user_id, name, department_id, class_id) values (null, ?, ?, ?)",
                value(body.get("name"), "未命名学生"), departmentId, classId
        );
        return Map.of("success", true, "message", "学生已新增");
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateStudent(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Long classId = nullableLong(body.get("classId"));
        Long departmentId = nullableLong(body.get("departmentId"));
        if (departmentId == null && classId != null) {
            departmentId = jdbcTemplate.queryForObject(
                    "select department_id from class_info where class_id = ?",
                    Long.class,
                    classId
            );
        }
        int rows = jdbcTemplate.update(
                "update student set name = ?, department_id = ?, class_id = ? where student_id = ?",
                value(body.get("name"), "未命名学生"), departmentId, classId, id
        );
        return Map.of("success", rows > 0, "message", rows > 0 ? "学生已修改" : "未找到学生");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteStudent(@PathVariable long id) {
        jdbcTemplate.update("delete wq from wrong_question wq join exam_result er on wq.result_id = er.result_id where er.student_id = ?", id);
        jdbcTemplate.update("delete from exam_result where student_id = ?", id);
        jdbcTemplate.update("delete from course_selection where student_id = ?", id);
        int rows = jdbcTemplate.update("delete from student where student_id = ?", id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "学生已删除" : "未找到学生");
    }

    @PostMapping("/department")
    public Map<String, Object> createDepartment(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update("insert into department(name) values (?)", value(body.get("name"), "未命名院系"));
        return Map.of("success", true, "message", "院系已新增");
    }

    @PutMapping("/department/{id}")
    public Map<String, Object> updateDepartment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int rows = jdbcTemplate.update("update department set name = ? where department_id = ?", value(body.get("name"), "未命名院系"), id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "院系已修改" : "未找到院系");
    }

    @DeleteMapping("/department/{id}")
    public Map<String, Object> deleteDepartment(@PathVariable Long id) {
        jdbcTemplate.update("update student set department_id = null, class_id = null where department_id = ?", id);
        jdbcTemplate.update("delete from class_info where department_id = ?", id);
        int rows = jdbcTemplate.update("delete from department where department_id = ?", id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "院系已删除" : "未找到院系");
    }

    @PostMapping("/class")
    public Map<String, Object> createClass(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
                "insert into class_info(department_id, name, grade) values (?, ?, ?)",
                nullableLong(body.get("departmentId")),
                value(body.get("name"), "未命名班级"),
                value(body.get("grade"), "")
        );
        return Map.of("success", true, "message", "班级已新增");
    }

    @PutMapping("/class/{id}")
    public Map<String, Object> updateClass(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int rows = jdbcTemplate.update(
                "update class_info set department_id = ?, name = ?, grade = ? where class_id = ?",
                nullableLong(body.get("departmentId")),
                value(body.get("name"), "未命名班级"),
                value(body.get("grade"), ""),
                id
        );
        jdbcTemplate.update("""
                update student s
                join class_info c on s.class_id = c.class_id
                set s.department_id = c.department_id
                where s.class_id = ?
                """, id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "班级已修改" : "未找到班级");
    }

    @DeleteMapping("/class/{id}")
    public Map<String, Object> deleteClass(@PathVariable Long id) {
        jdbcTemplate.update("update student set class_id = null where class_id = ?", id);
        jdbcTemplate.update("delete from exam where class_id = ?", id);
        int rows = jdbcTemplate.update("delete from class_info where class_id = ?", id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "班级已删除" : "未找到班级");
    }

    @PostMapping("/course")
    public Map<String, Object> createCourse(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
                "insert into course(name, subject, description) values (?, ?, ?)",
                value(body.get("name"), "未命名课程"),
                value(body.get("subject"), ""),
                value(body.get("description"), "")
        );
        return Map.of("success", true, "message", "课程已新增");
    }

    @PutMapping("/course/{id}")
    public Map<String, Object> updateCourse(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int rows = jdbcTemplate.update(
                "update course set name = ?, subject = ?, description = ? where course_id = ?",
                value(body.get("name"), "未命名课程"),
                value(body.get("subject"), ""),
                value(body.get("description"), ""),
                id
        );
        return Map.of("success", rows > 0, "message", rows > 0 ? "课程已修改" : "未找到课程");
    }

    @DeleteMapping("/course/{id}")
    public Map<String, Object> deleteCourse(@PathVariable Long id) {
        jdbcTemplate.update("delete from course_selection where course_id = ?", id);
        jdbcTemplate.update("delete from chapter where course_id = ?", id);
        int rows = jdbcTemplate.update("delete from course where course_id = ?", id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "课程已删除" : "未找到课程");
    }

    @PostMapping("/class-course-selection")
    public Map<String, Object> selectCourseByClass(@RequestBody Map<String, Object> body) {
        Long classId = nullableLong(body.get("classId"));
        Long courseId = nullableLong(body.get("courseId"));
        if (classId == null || courseId == null) {
            return Map.of("success", false, "message", "请选择班级和课程");
        }
        List<Map<String, Object>> students = jdbcTemplate.queryForList("select student_id from student where class_id = ?", classId);
        int added = 0;
        for (Map<String, Object> student : students) {
            Long studentId = ((Number) student.get("student_id")).longValue();
            Integer exists = jdbcTemplate.queryForObject(
                    "select count(*) from course_selection where student_id = ? and course_id = ?",
                    Integer.class,
                    studentId,
                    courseId
            );
            if (exists == null || exists == 0) {
                jdbcTemplate.update(
                        "insert into course_selection(student_id, course_id, selected_time) values (?, ?, ?)",
                        studentId, courseId, LocalDateTime.now()
                );
                added++;
            }
        }
        return Map.of("success", true, "message", "班级选课完成，新增 " + added + " 条选课记录", "added", added);
    }

    @DeleteMapping("/course-selection/{id}")
    public Map<String, Object> deleteCourseSelection(@PathVariable Long id) {
        int rows = jdbcTemplate.update("delete from course_selection where selection_id = ?", id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "选课记录已删除" : "未找到选课记录");
    }

    @PostMapping("/score")
    public Map<String, Object> createScore(@RequestBody Map<String, Object> body) {
        Long studentId = nullableLong(body.get("studentId"));
        if (studentId == null) {
            return Map.of("success", false, "message", "请选择学生");
        }
        Long classId = nullableLong(body.get("classId"));
        if (classId == null) {
            classId = findLong("select class_id from student where student_id = ?", studentId);
        }
        if (classId == null) {
            return Map.of("success", false, "message", "该学生没有分配班级，无法生成班级考试成绩");
        }

        String paperTitle = value(body.get("paperTitle"), "学生成绩录入");
        Long paperId = findLong("select paper_id from paper where title = ? order by paper_id desc limit 1", paperTitle);
        if (paperId == null) {
            jdbcTemplate.update("insert into paper(title, total_score, difficulty, creator_id) values (?, ?, ?, null)", paperTitle, 100, value(body.get("difficulty"), "中等"));
            paperId = findLong("select paper_id from paper where title = ? order by paper_id desc limit 1", paperTitle);
        }

        LocalDateTime submitTime = LocalDateTime.now();
        Long examId = findLong("select exam_id from exam where paper_id = ? and class_id = ? order by exam_id desc limit 1", paperId, classId);
        if (examId == null) {
            jdbcTemplate.update("insert into exam(paper_id, class_id, start_time, end_time) values (?, ?, ?, ?)", paperId, classId, submitTime.minusHours(2), submitTime);
            examId = findLong("select exam_id from exam where paper_id = ? and class_id = ? order by exam_id desc limit 1", paperId, classId);
        }

        Long resultId = findLong("select result_id from exam_result where exam_id = ? and student_id = ? order by result_id desc limit 1", examId, studentId);
        if (resultId == null) {
            jdbcTemplate.update(
                    "insert into exam_result(exam_id, student_id, score, submit_time) values (?, ?, ?, ?)",
                    examId, studentId, nullableDouble(body.get("score"), 0), submitTime
            );
        } else {
            jdbcTemplate.update(
                    "update exam_result set score = ?, submit_time = ? where result_id = ?",
                    nullableDouble(body.get("score"), 0), submitTime, resultId
            );
        }
        return Map.of("success", true, "message", resultId == null ? "成绩已录入，可在成绩波动图中查看" : "成绩已覆盖更新，可在成绩波动图中查看");
    }

    @GetMapping("/scores")
    public Map<String, Object> scores(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId
    ) {
        ensureAnalysisDemoData(studentId, classId);
        List<Map<String, Object>> items = queryScores(studentId, classId);
        return Map.of("items", items, "count", items.size());
    }

    @GetMapping("/score-trend")
    public Map<String, Object> scoreTrend(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId
    ) {
        ensureAnalysisDemoData(studentId, classId);
        List<Map<String, Object>> items = queryScores(studentId, classId);
        return Map.of("items", items, "chartType", "bar", "count", items.size());
    }

    @GetMapping("/weak-points")
    public Map<String, Object> weakPoints(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId
    ) {
        ensureAnalysisDemoData(studentId, classId);
        List<Map<String, Object>> items = queryWeakPoints(studentId, classId);
        return Map.of("items", items, "count", items.size());
    }

    @GetMapping("/weak-point-report")
    public Map<String, Object> weakPointReport(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId
    ) {
        ensureAnalysisDemoData(studentId, classId);
        List<Map<String, Object>> items = queryWeakPoints(studentId, classId);
        String suggestion = items.isEmpty() ? "暂无薄弱知识点记录。" : "请优先复习错误次数最高的知识点，并结合错题原因进行巩固。";
        return Map.of("items", items, "summary", Map.of("weakPointCount", items.size(), "suggestion", suggestion));
    }

    @GetMapping("/analysis")
    public Map<String, Object> analysis(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId
    ) {
        ensureAnalysisDemoData(studentId, classId);

        String scoreSql = """
                select er.result_id, er.student_id, s.name as student_name, s.class_id, ci.name as class_name,
                       er.exam_id, e.paper_id, p.title as paper_title, er.score, er.submit_time
                from exam_result er
                left join student s on er.student_id = s.student_id
                left join class_info ci on s.class_id = ci.class_id
                left join exam e on er.exam_id = e.exam_id
                left join paper p on e.paper_id = p.paper_id
                """;
        String weakPointSql = """
                select wq.point_id, kp.name as point_name, count(*) as wrong_count,
                       group_concat(distinct s.name order by s.name separator '、') as student_names
                from wrong_question wq
                left join exam_result er on wq.result_id = er.result_id
                left join student s on er.student_id = s.student_id
                left join knowledge_point kp on wq.point_id = kp.point_id
                """;
        List<Map<String, Object>> scores;
        List<Map<String, Object>> weakPoints;
        if (studentId != null) {
            scores = jdbcTemplate.queryForList(scoreSql + " where er.student_id = ? order by er.submit_time asc, er.result_id asc", studentId);
            weakPoints = jdbcTemplate.queryForList(weakPointSql + " where er.student_id = ? group by wq.point_id, kp.name order by wrong_count desc", studentId);
        } else if (classId != null) {
            scores = jdbcTemplate.queryForList(scoreSql + " where s.class_id = ? order by er.submit_time asc, er.result_id asc", classId);
            weakPoints = jdbcTemplate.queryForList(weakPointSql + " where s.class_id = ? group by wq.point_id, kp.name order by wrong_count desc", classId);
        } else {
            scores = jdbcTemplate.queryForList(scoreSql + " order by er.submit_time asc, er.result_id asc");
            weakPoints = jdbcTemplate.queryForList(weakPointSql + " group by wq.point_id, kp.name order by wrong_count desc");
        }
        return Map.of(
                "scores", scores,
                "weakPoints", weakPoints,
                "summary", Map.of(
                        "scoreCount", scores.size(),
                        "weakPointCount", weakPoints.size(),
                        "suggestion", weakPoints.isEmpty() ? "暂无错误知识点记录。" : "请优先复习错误次数最高的知识点。"
                )
        );
    }

    private List<Map<String, Object>> queryScores(Long studentId, Long classId) {
        String sql = """
                select er.result_id, er.student_id, s.name as student_name, s.class_id, ci.name as class_name,
                       er.exam_id, e.paper_id, p.title as paper_title, er.score, er.submit_time
                from exam_result er
                join (
                    select max(result_id) as result_id
                    from exam_result
                    group by exam_id, student_id
                ) latest on er.result_id = latest.result_id
                left join student s on er.student_id = s.student_id
                left join class_info ci on s.class_id = ci.class_id
                left join exam e on er.exam_id = e.exam_id
                left join paper p on e.paper_id = p.paper_id
                """;
        if (studentId != null) {
            return jdbcTemplate.queryForList(sql + " where er.student_id = ? order by er.submit_time asc, er.result_id asc", studentId);
        }
        if (classId != null) {
            return jdbcTemplate.queryForList(sql + " where s.class_id = ? order by er.submit_time asc, er.result_id asc", classId);
        }
        return jdbcTemplate.queryForList(sql + " order by er.submit_time asc, er.result_id asc");
    }

    private List<Map<String, Object>> queryWeakPoints(Long studentId, Long classId) {
        String sql = """
                select wq.point_id, kp.name as point_name, count(*) as wrong_count,
                       group_concat(distinct s.name order by s.name separator '、') as student_names
                from wrong_question wq
                left join exam_result er on wq.result_id = er.result_id
                left join student s on er.student_id = s.student_id
                left join knowledge_point kp on wq.point_id = kp.point_id
                """;
        if (studentId != null) {
            return jdbcTemplate.queryForList(sql + " where er.student_id = ? group by wq.point_id, kp.name order by wrong_count desc", studentId);
        }
        if (classId != null) {
            return jdbcTemplate.queryForList(sql + " where s.class_id = ? group by wq.point_id, kp.name order by wrong_count desc", classId);
        }
        return jdbcTemplate.queryForList(sql + " group by wq.point_id, kp.name order by wrong_count desc");
    }

    private void ensureStudentDemoData() {
        Long departmentId = ensureDepartment();
        Long classId = findLong("select class_id from class_info where name = ? order by class_id desc limit 1", "软件工程一班");
        if (classId == null) {
            classId = findLong("select class_id from class_info order by class_id asc limit 1");
        }
        if (classId == null) {
            jdbcTemplate.update("insert into class_info(department_id, name, grade) values (?, ?, ?)", departmentId, "软件工程一班", "2023");
            classId = findLong("select class_id from class_info where name = ? order by class_id desc limit 1", "软件工程一班");
        }
        String[] names = {"李明", "王芳", "张三", "赵敏", "刘洋"};
        for (String name : names) {
            if (count("select count(*) from student where name = ?", name) == 0) {
                jdbcTemplate.update("insert into student(user_id, name, department_id, class_id) values (null, ?, ?, ?)", name, departmentId, classId);
            }
        }
        Long courseId = ensureCourse();
        List<Map<String, Object>> students = jdbcTemplate.queryForList("select student_id from student where class_id = ?", classId);
        for (Map<String, Object> student : students) {
            Long studentId = ((Number) student.get("student_id")).longValue();
            if (count("select count(*) from course_selection where student_id = ? and course_id = ?", studentId, courseId) == 0) {
                jdbcTemplate.update("insert into course_selection(student_id, course_id, selected_time) values (?, ?, ?)", studentId, courseId, LocalDateTime.now().minusDays(1));
            }
        }
    }

    private void ensureAnalysisDemoData(Long studentId, Long classId) {
        Long targetClassId = classId;
        if (targetClassId == null && studentId != null) {
            targetClassId = findLong("select class_id from student where student_id = ?", studentId);
        }
        if (targetClassId == null) {
            targetClassId = findLong("""
                    select class_id
                    from student
                    where class_id is not null
                    group by class_id
                    order by count(*) desc, class_id asc
                    limit 1
                    """);
        }
        if (targetClassId == null) {
            Long departmentId = ensureDepartment();
            jdbcTemplate.update("insert into class_info(department_id, name, grade) values (?, ?, ?)", departmentId, "软件工程一班", "2024");
            targetClassId = findLong("select class_id from class_info where name = ? order by class_id desc limit 1", "软件工程一班");
        }

        Long departmentId = findLong("select department_id from class_info where class_id = ?", targetClassId);
        if (departmentId == null) {
            departmentId = ensureDepartment();
            jdbcTemplate.update("update class_info set department_id = ? where class_id = ?", departmentId, targetClassId);
        }

        if (count("select count(*) from student where class_id = ?", targetClassId) == 0) {
            jdbcTemplate.update("insert into student(user_id, name, department_id, class_id) values (null, ?, ?, ?)", "李明", departmentId, targetClassId);
            jdbcTemplate.update("insert into student(user_id, name, department_id, class_id) values (null, ?, ?, ?)", "王芳", departmentId, targetClassId);
        }

        Long courseId = ensureCourse();
        Long chapterId = ensureChapter(courseId);
        Long pointId = ensureKnowledgePoint(chapterId);
        Long questionId = ensureQuestion(courseId, chapterId, pointId);
        Long paperId = ensurePaper(questionId);
        Long examId = ensureExam(paperId, targetClassId);
        Long examId2 = ensureTrendExam(questionId, targetClassId, "Java阶段测验二", 6);
        Long examId3 = ensureTrendExam(questionId, targetClassId, "Java阶段测验三", 1);

        List<Map<String, Object>> students;
        if (studentId != null) {
            students = jdbcTemplate.queryForList("select student_id, name from student where student_id = ?", studentId);
        } else {
            students = jdbcTemplate.queryForList("select student_id, name from student where class_id = ? order by student_id asc", targetClassId);
        }
        int index = 0;
        for (Map<String, Object> student : students) {
            Long id = ((Number) student.get("student_id")).longValue();
            ensureResult(examId, id, index % 2 == 0 ? 78 : 68, LocalDateTime.now().minusDays(12).plusHours(index));
            ensureResult(examId2, id, index % 2 == 0 ? 84 : 75, LocalDateTime.now().minusDays(6).plusHours(index));
            ensureResult(examId3, id, index % 2 == 0 ? 91 : 82, LocalDateTime.now().minusDays(1).plusHours(index));
            Long resultId = findLong(
                    "select result_id from exam_result where exam_id = ? and student_id = ? order by result_id desc limit 1",
                    examId, id
            );
            if (resultId != null && count("select count(*) from wrong_question where result_id = ?", resultId) == 0) {
                jdbcTemplate.update(
                        "insert into wrong_question(result_id, question_id, point_id, reason) values (?, ?, ?, ?)",
                        resultId, questionId, pointId, "演示数据：概念理解不完整"
                );
            }
            index++;
        }
    }

    private Long ensureDepartment() {
        Long id = findLong("select department_id from department where name = ? order by department_id desc limit 1", "计算机学院");
        if (id == null) {
            jdbcTemplate.update("insert into department(name) values (?)", "计算机学院");
            id = findLong("select department_id from department where name = ? order by department_id desc limit 1", "计算机学院");
        }
        return id;
    }

    private Long ensureCourse() {
        Long id = findLong("select course_id from course where name = ? order by course_id desc limit 1", "Java程序设计");
        if (id == null) {
            id = findLong("select course_id from course order by course_id asc limit 1");
        }
        if (id == null) {
            jdbcTemplate.update("insert into course(name, subject, description) values (?, ?, ?)", "Java程序设计", "程序设计", "学生分析演示课程");
            id = findLong("select course_id from course where name = ? order by course_id desc limit 1", "Java程序设计");
        }
        return id;
    }

    private Long ensureChapter(Long courseId) {
        Long id = findLong("select chapter_id from chapter where course_id = ? order by chapter_id asc limit 1", courseId);
        if (id == null) {
            jdbcTemplate.update("insert into chapter(course_id, name, order_no) values (?, ?, ?)", courseId, "Java基础语法", 1);
            id = findLong("select chapter_id from chapter where course_id = ? order by chapter_id desc limit 1", courseId);
        }
        return id;
    }

    private Long ensureKnowledgePoint(Long chapterId) {
        Long id = findLong("select point_id from knowledge_point where chapter_id = ? order by point_id asc limit 1", chapterId);
        if (id == null) {
            jdbcTemplate.update("insert into knowledge_point(chapter_id, name, description) values (?, ?, ?)", chapterId, "Java基础语法", "学生分析演示知识点");
            id = findLong("select point_id from knowledge_point where chapter_id = ? order by point_id desc limit 1", chapterId);
        }
        return id;
    }

    private Long ensureQuestion(Long courseId, Long chapterId, Long pointId) {
        Long id = findLong("select question_id from question order by question_id asc limit 1");
        if (id == null) {
            jdbcTemplate.update(
                    "insert into question(type, stem, difficulty, course_id, chapter_id, creator_id) values (?, ?, ?, ?, ?, null)",
                    "选择题", "Java中用于输出内容的方法是？", "简单", courseId, chapterId
            );
            id = findLong("select question_id from question order by question_id desc limit 1");
            jdbcTemplate.update("insert into answer(question_id, answer_content, analysis) values (?, ?, ?)", id, "System.out.println()", "println可以在控制台输出内容。");
        }
        if (id != null && count("select count(*) from question_knowledge_point where question_id = ? and point_id = ?", id, pointId) == 0) {
            jdbcTemplate.update("insert into question_knowledge_point(question_id, point_id) values (?, ?)", id, pointId);
        }
        return id;
    }

    private Long ensurePaper(Long questionId) {
        Long id = findLong("select paper_id from paper where title = ? order by paper_id desc limit 1", "Java阶段测验一");
        if (id == null) {
            jdbcTemplate.update("insert into paper(title, total_score, difficulty, creator_id) values (?, ?, ?, null)", "Java阶段测验一", 100, "中等");
            id = findLong("select paper_id from paper where title = ? order by paper_id desc limit 1", "Java阶段测验一");
        }
        if (id != null && questionId != null && count("select count(*) from paper_question where paper_id = ? and question_id = ?", id, questionId) == 0) {
            jdbcTemplate.update("insert into paper_question(paper_id, question_id, score, order_no) values (?, ?, ?, ?)", id, questionId, 10, 1);
        }
        return id;
    }

    private Long ensureTrendExam(Long questionId, Long classId, String paperTitle, int daysAgo) {
        Long paperId = findLong("select paper_id from paper where title = ? order by paper_id desc limit 1", paperTitle);
        if (paperId == null) {
            jdbcTemplate.update("insert into paper(title, total_score, difficulty, creator_id) values (?, ?, ?, null)", paperTitle, 100, "中等");
            paperId = findLong("select paper_id from paper where title = ? order by paper_id desc limit 1", paperTitle);
        }
        if (paperId != null && questionId != null && count("select count(*) from paper_question where paper_id = ? and question_id = ?", paperId, questionId) == 0) {
            jdbcTemplate.update("insert into paper_question(paper_id, question_id, score, order_no) values (?, ?, ?, ?)", paperId, questionId, 100, 1);
        }
        Long examId = findLong("select exam_id from exam where paper_id = ? and class_id = ? order by exam_id desc limit 1", paperId, classId);
        if (examId == null) {
            LocalDateTime start = LocalDateTime.now().minusDays(daysAgo);
            jdbcTemplate.update("insert into exam(paper_id, class_id, start_time, end_time) values (?, ?, ?, ?)", paperId, classId, start, start.plusHours(2));
            examId = findLong("select exam_id from exam where paper_id = ? and class_id = ? order by exam_id desc limit 1", paperId, classId);
        }
        return examId;
    }

    private void ensureResult(Long examId, Long studentId, int score, LocalDateTime submitTime) {
        if (examId != null && count("select count(*) from exam_result where exam_id = ? and student_id = ?", examId, studentId) == 0) {
            jdbcTemplate.update(
                    "insert into exam_result(exam_id, student_id, score, submit_time) values (?, ?, ?, ?)",
                    examId, studentId, score, submitTime
            );
        }
    }

    private Long ensureExam(Long paperId, Long classId) {
        Long id = findLong("select exam_id from exam where paper_id = ? and class_id = ? order by exam_id desc limit 1", paperId, classId);
        if (id == null) {
            LocalDateTime start = LocalDateTime.now().minusDays(3);
            jdbcTemplate.update("insert into exam(paper_id, class_id, start_time, end_time) values (?, ?, ?, ?)", paperId, classId, start, start.plusHours(2));
            id = findLong("select exam_id from exam where paper_id = ? and class_id = ? order by exam_id desc limit 1", paperId, classId);
        }
        return id;
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private Long findLong(String sql, Object... args) {
        List<Long> values = jdbcTemplate.queryForList(sql, Long.class, args);
        return values.isEmpty() ? null : values.get(0);
    }

    private String value(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }

    private Long nullableLong(Object value) {
        try {
            if (value == null || value.toString().isBlank()) {
                return null;
            }
            return Long.parseLong(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private double nullableDouble(Object value, double fallback) {
        try {
            if (value == null || value.toString().isBlank()) {
                return fallback;
            }
            return Double.parseDouble(value.toString());
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
