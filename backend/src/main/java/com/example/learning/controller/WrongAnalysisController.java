package com.example.learning.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> existing = jdbcTemplate.queryForMap("select wrong_id, result_id, question_id, point_id, reason from wrong_question where wrong_id = ?", id);
            Long resultId = body.containsKey("resultId") ? nullableLong(body.get("resultId")) : (existing.get("result_id") == null ? null : ((Number) existing.get("result_id")).longValue());
            Long questionId = body.containsKey("questionId") ? nullableLong(body.get("questionId")) : (existing.get("question_id") == null ? null : ((Number) existing.get("question_id")).longValue());
            Long pointId = body.containsKey("pointId") ? nullableLong(body.get("pointId")) : (existing.get("point_id") == null ? null : ((Number) existing.get("point_id")).longValue());
            String reason = body.containsKey("reason") ? value(body.get("reason"), "未填写原因") : (existing.get("reason") == null ? "" : existing.get("reason").toString());

            int updated = jdbcTemplate.update(
                    "update wrong_question set result_id = ?, question_id = ?, point_id = ?, reason = ? where wrong_id = ?",
                    resultId, questionId, pointId, reason, id
            );
            return Map.of("success", updated > 0, "message", updated > 0 ? "错题已更新" : "未找到错题");
        } catch (Exception e) {
            return Map.of("success", false, "message", "未找到错题");
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        int deleted = jdbcTemplate.update("delete from wrong_question where wrong_id = ?", id);
        return Map.of("success", deleted > 0, "message", deleted > 0 ? "错题已删除" : "未找到错题");
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Integer total = jdbcTemplate.queryForObject("select count(*) from wrong_question", Integer.class);
        if (total == null) total = 0;
        List<Map<String, Object>> byQuestion = jdbcTemplate.queryForList(
                "select question_id, count(*) as wrong_count from wrong_question group by question_id order by wrong_count desc"
        );
        List<Map<String, Object>> byPoint = jdbcTemplate.queryForList(
                "select point_id, count(*) as wrong_count from wrong_question group by point_id order by wrong_count desc"
        );
        // compute rate in Java to avoid SQL dialect issues
        for (Map<String, Object> m : byQuestion) {
            Number c = (Number) m.get("wrong_count");
            double rate = total == 0 ? 0.0 : c.doubleValue() / total.doubleValue();
            m.put("rate", rate);
        }
        for (Map<String, Object> m : byPoint) {
            Number c = (Number) m.get("wrong_count");
            double rate = total == 0 ? 0.0 : c.doubleValue() / total.doubleValue();
            m.put("rate", rate);
        }
        return Map.of(
                "subsystem", "错题分析子系统",
                "totalWrong", total,
                "byQuestion", byQuestion,
                "byPoint", byPoint
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
