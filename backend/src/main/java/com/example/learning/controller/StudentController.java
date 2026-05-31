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
        return Map.of(
                "subsystem", "学生管理子系统",
                "useCases", List.of("学生的院系班级分配", "学生以班级为单位选课", "学生管理（增删查改）", "学生情况分析（历次考试成绩波动图、错误知识点统计报告）"),
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

    @GetMapping("/analysis")
    public Map<String, Object> analysis(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId
    ) {
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
}
