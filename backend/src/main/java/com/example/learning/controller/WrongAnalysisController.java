package com.example.learning.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/wrong-analysis")
public class WrongAnalysisController {
    private final JdbcTemplate jdbcTemplate;

    public WrongAnalysisController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "select wrong_id, result_id, question_id, point_id, reason from wrong_question order by wrong_id desc"
        );
        return Map.of(
                "subsystem", "错题分析子系统",
                "useCases", List.of("查询错题记录", "导入考试结果", "分析班级错题", "分析个人错题", "统计知识点错误率", "生成学习建议", "查看正确答案"),
                "items", records,
                "summary", Map.of("wrongCount", records.size(), "suggestion", records.isEmpty() ? "暂无错题记录，可先导入考试结果。" : "请优先复习错误次数较多的知识点。")
        );
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
                "insert into wrong_question(result_id, question_id, point_id, reason) values (?, ?, ?, ?)",
                nullableLong(body.get("resultId")),
                nullableLong(body.get("questionId")),
                nullableLong(body.get("pointId")),
                value(body.get("reason"), "未填写原因")
        );
        return Map.of("success", true, "message", "错题记录已新增");
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
