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
import org.springframework.web.bind.annotation.RestController;

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
        List<Map<String, Object>> students = jdbcTemplate.queryForList(
                "select student_id, name, department_id, class_id from student order by student_id desc"
        );
        return Map.of(
                "subsystem", "学生管理子系统",
                "useCases", List.of("维护学生信息", "维护院系班级", "学生选课", "查询学生成绩", "生成成绩波动图", "生成薄弱知识点报告"),
                "items", students
        );
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        String name = value(body.get("name"), "未命名学生");
        jdbcTemplate.update(
                "insert into student(user_id, name, department_id, class_id) values (null, ?, null, null)",
                name
        );
        return Map.of("success", true, "message", "学生已新增");
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        String name = value(body.get("name"), "未命名学生");
        int rows = jdbcTemplate.update("update student set name = ? where student_id = ?", name, id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "学生已修改" : "未找到学生");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable long id) {
        int rows = jdbcTemplate.update("delete from student where student_id = ?", id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "学生已删除" : "未找到学生");
    }

    private String value(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }
}
