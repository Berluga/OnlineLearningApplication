package com.example.learning.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/auto-paper")
public class AutoPaperController {
    private final JdbcTemplate jdbcTemplate;

    public AutoPaperController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> list() {
        return Map.of(
                "subsystem", "自动组卷子系统",
                "useCases", List.of("设置组卷条件", "自动抽取题目", "校验知识点覆盖", "计算题目分值", "生成试卷", "预览试卷", "删除试卷"),
                "papers", jdbcTemplate.queryForList("select paper_id, title, total_score, difficulty, creator_id from paper order by paper_id desc"),
                "paperQuestions", jdbcTemplate.queryForList("select paper_id, question_id, score, order_no from paper_question order by paper_id desc, order_no asc")
        );
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, Object> body) {
        String title = value(body.get("title"), "自动生成试卷");
        String difficulty = value(body.get("difficulty"), "中等");
        int count = Math.max(1, number(body.get("count"), 5));
        BigDecimal totalScore = BigDecimal.valueOf(Math.max(1, number(body.get("totalScore"), 100)));

        List<Map<String, Object>> questions = jdbcTemplate.queryForList(
                "select question_id from question order by rand() limit ?",
                count
        );
        if (questions.isEmpty()) {
            return Map.of("success", false, "message", "题库暂无题目，无法组卷");
        }

        jdbcTemplate.update(
                "insert into paper(title, total_score, difficulty, creator_id) values (?, ?, ?, null)",
                title, totalScore, difficulty
        );
        Long paperId = jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
        BigDecimal score = totalScore.divide(BigDecimal.valueOf(questions.size()), 2, RoundingMode.HALF_UP);
        for (int i = 0; i < questions.size(); i++) {
            Number questionId = (Number) questions.get(i).get("question_id");
            jdbcTemplate.update(
                    "insert into paper_question(paper_id, question_id, score, order_no) values (?, ?, ?, ?)",
                    paperId, questionId.longValue(), score, i + 1
            );
        }
        return Map.of("success", true, "message", "试卷已生成", "paperId", paperId, "questionCount", questions.size());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        jdbcTemplate.update("""
                delete wq from wrong_question wq
                join exam_result er on wq.result_id = er.result_id
                join exam e on er.exam_id = e.exam_id
                where e.paper_id = ?
                """, id);
        jdbcTemplate.update("""
                delete er from exam_result er
                join exam e on er.exam_id = e.exam_id
                where e.paper_id = ?
                """, id);
        jdbcTemplate.update("delete from exam where paper_id = ?", id);
        jdbcTemplate.update("delete from paper_question where paper_id = ?", id);
        int rows = jdbcTemplate.update("delete from paper where paper_id = ?", id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "试卷已删除" : "未找到试卷");
    }

    private String value(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }

    private int number(Object value, int fallback) {
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
