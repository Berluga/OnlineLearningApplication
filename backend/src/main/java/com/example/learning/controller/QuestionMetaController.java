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
@RequestMapping("/api/question-meta")
public class QuestionMetaController {
    private final JdbcTemplate jdbcTemplate;

    public QuestionMetaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> list() {
        return Map.of(
                "subsystem", "题目性质管理子系统",
                "useCases", List.of("查询题目性质", "维护标准答案", "维护知识点", "设置难易度", "维护章节", "统计出错率", "维护易错点"),
                "chapters", jdbcTemplate.queryForList("select chapter_id, course_id, name, order_no from chapter order by chapter_id desc"),
                "knowledgePoints", jdbcTemplate.queryForList("select point_id, chapter_id, name, description from knowledge_point order by point_id desc"),
                "answers", jdbcTemplate.queryForList("select answer_id, question_id, answer_content, analysis from answer order by answer_id desc"),
                "mistakePoints", jdbcTemplate.queryForList("select mistake_id, question_id, point_id, description from mistake_point order by mistake_id desc")
        );
    }

    @PostMapping("/chapter")
    public Map<String, Object> createChapter(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
                "insert into chapter(course_id, name, order_no) values (null, ?, ?)",
                value(body.get("name"), "默认章节"),
                number(body.get("orderNo"), 1)
        );
        return Map.of("success", true, "message", "章节已新增");
    }

    @PostMapping("/knowledge-point")
    public Map<String, Object> createKnowledgePoint(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
                "insert into knowledge_point(chapter_id, name, description) values (?, ?, ?)",
                nullableLong(body.get("chapterId")),
                value(body.get("name"), "默认知识点"),
                value(body.get("description"), "暂无描述")
        );
        return Map.of("success", true, "message", "知识点已新增");
    }

    @PostMapping("/answer")
    public Map<String, Object> createAnswer(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
                "insert into answer(question_id, answer_content, analysis) values (?, ?, ?)",
                nullableLong(body.get("questionId")),
                value(body.get("answerContent"), "参考答案"),
                value(body.get("analysis"), "暂无解析")
        );
        return Map.of("success", true, "message", "答案已新增");
    }

    @PostMapping("/mistake-point")
    public Map<String, Object> createMistakePoint(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
                "insert into mistake_point(question_id, point_id, description) values (?, ?, ?)",
                nullableLong(body.get("questionId")),
                nullableLong(body.get("pointId")),
                value(body.get("description"), "常见错误")
        );
        return Map.of("success", true, "message", "易错点已新增");
    }

    @PutMapping("/chapter/{id}")
    public Map<String, Object> updateChapter(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int updated = jdbcTemplate.update(
                "update chapter set name = ?, order_no = ? where chapter_id = ?",
                value(body.get("name"), "默认章节"),
                number(body.get("orderNo"), 1),
                id
        );
        return Map.of("success", updated > 0, "message", updated > 0 ? "章节已更新" : "未找到章节");
    }

    @DeleteMapping("/chapter/{id}")
    public Map<String, Object> deleteChapter(@PathVariable Long id) {
        int deleted = jdbcTemplate.update("delete from chapter where chapter_id = ?", id);
        return Map.of("success", deleted > 0, "message", deleted > 0 ? "章节已删除" : "未找到章节");
    }

    @PutMapping("/knowledge-point/{id}")
    public Map<String, Object> updateKnowledgePoint(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int updated = jdbcTemplate.update(
                "update knowledge_point set chapter_id = ?, name = ?, description = ? where point_id = ?",
                nullableLong(body.get("chapterId")),
                value(body.get("name"), "默认知识点"),
                value(body.get("description"), "暂无描述"),
                id
        );
        return Map.of("success", updated > 0, "message", updated > 0 ? "知识点已更新" : "未找到知识点");
    }

    @DeleteMapping("/knowledge-point/{id}")
    public Map<String, Object> deleteKnowledgePoint(@PathVariable Long id) {
        int deleted = jdbcTemplate.update("delete from knowledge_point where point_id = ?", id);
        return Map.of("success", deleted > 0, "message", deleted > 0 ? "知识点已删除" : "未找到知识点");
    }

    @PutMapping("/answer/{id}")
    public Map<String, Object> updateAnswer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int updated = jdbcTemplate.update(
                "update answer set question_id = ?, answer_content = ?, analysis = ? where answer_id = ?",
                nullableLong(body.get("questionId")),
                value(body.get("answerContent"), "参考答案"),
                value(body.get("analysis"), "暂无解析"),
                id
        );
        return Map.of("success", updated > 0, "message", updated > 0 ? "答案已更新" : "未找到答案");
    }

    @DeleteMapping("/answer/{id}")
    public Map<String, Object> deleteAnswer(@PathVariable Long id) {
        int deleted = jdbcTemplate.update("delete from answer where answer_id = ?", id);
        return Map.of("success", deleted > 0, "message", deleted > 0 ? "答案已删除" : "未找到答案");
    }

    @PutMapping("/mistake-point/{id}")
    public Map<String, Object> updateMistakePoint(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int updated = jdbcTemplate.update(
                "update mistake_point set question_id = ?, point_id = ?, description = ? where mistake_id = ?",
                nullableLong(body.get("questionId")),
                nullableLong(body.get("pointId")),
                value(body.get("description"), "常见错误"),
                id
        );
        return Map.of("success", updated > 0, "message", updated > 0 ? "易错点已更新" : "未找到易错点");
    }

    @DeleteMapping("/mistake-point/{id}")
    public Map<String, Object> deleteMistakePoint(@PathVariable Long id) {
        int deleted = jdbcTemplate.update("delete from mistake_point where mistake_id = ?", id);
        return Map.of("success", deleted > 0, "message", deleted > 0 ? "易错点已删除" : "未找到易错点");
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
