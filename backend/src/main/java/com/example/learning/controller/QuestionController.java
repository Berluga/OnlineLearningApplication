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
@RequestMapping("/api/question")
public class QuestionController {
    private final JdbcTemplate jdbcTemplate;

    public QuestionController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, Object>> questions = jdbcTemplate.queryForList(
                "select question_id, type, stem, difficulty, course_id, chapter_id, creator_id from question order by question_id desc"
        );
        return Map.of(
                "subsystem", "题目管理子系统",
                "useCases", List.of("新增题目", "修改题目", "删除题目", "查询题目", "上传题目图片", "维护题型"),
                "items", questions
        );
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        String type = value(body.get("type"), "选择题");
        String stem = value(body.get("stem"), "未命名题目");
        String difficulty = value(body.get("difficulty"), "中等");
        jdbcTemplate.update(
                "insert into question(type, stem, difficulty, course_id, chapter_id, creator_id) values (?, ?, ?, null, null, null)",
                type, stem, difficulty
        );
        return Map.of("success", true, "message", "题目已新增");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable long id) {
        int rows = jdbcTemplate.update("delete from question where question_id = ?", id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "题目已删除" : "未找到题目");
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        String type = value(body.get("type"), "选择题");
        String stem = value(body.get("stem"), "未命名题目");
        String difficulty = value(body.get("difficulty"), "中等");
        int rows = jdbcTemplate.update(
                "update question set type = ?, stem = ?, difficulty = ? where question_id = ?",
                type, stem, difficulty, id
        );
        return Map.of("success", rows > 0, "message", rows > 0 ? "题目已修改" : "未找到题目");
    }

    private String value(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }
}
