package com.example.learning.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        ensureQuestionTypeTable();
        ensureQuestionImageRoleColumn();
        ensureQuestionDetails();
        List<Map<String, Object>> questions = jdbcTemplate.queryForList(
                "select question_id, type, stem, difficulty, course_id, chapter_id, creator_id from question order by question_id desc"
        );
        questions.forEach(this::attachQuestionDetail);
        return Map.of(
                "subsystem", "题目管理子系统",
                "useCases", List.of("教师管理", "新增题目", "修改题目", "删除题目", "查询题目", "上传题目图片", "维护题型"),
                "types", loadQuestionTypes(),
                "items", questions
        );
    }

    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        ensureQuestionTypeTable();
        ensureQuestionImageRoleColumn();
        String type = value(body.get("type"), "选择题");
        String stem = value(body.get("stem"), "未命名题目");
        String difficulty = value(body.get("difficulty"), "中等");
        jdbcTemplate.update(
                "insert into question(type, stem, difficulty, course_id, chapter_id, creator_id) values (?, ?, ?, ?, ?, ?)",
                type, stem, difficulty, nullableLong(body.get("courseId")), nullableLong(body.get("chapterId")), nullableLong(body.get("creatorId"))
        );
        Long questionId = jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
        saveOptions(questionId, body.get("options"));
        saveAnswer(questionId, body);
        saveImages(questionId, body.get("images"), "question");
        saveImages(questionId, body.get("answerImages"), "answer");
        return Map.of("success", true, "message", "题目已新增", "questionId", questionId);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> delete(@PathVariable long id) {
        jdbcTemplate.update("delete from question_option where question_id = ?", id);
        jdbcTemplate.update("delete from question_image where question_id = ?", id);
        jdbcTemplate.update("delete from answer where question_id = ?", id);
        jdbcTemplate.update("delete from question_knowledge_point where question_id = ?", id);
        jdbcTemplate.update("delete from mistake_point where question_id = ?", id);
        jdbcTemplate.update("delete from paper_question where question_id = ?", id);
        jdbcTemplate.update("delete from wrong_question where question_id = ?", id);
        int rows = jdbcTemplate.update("delete from question where question_id = ?", id);
        return Map.of("success", rows > 0, "message", rows > 0 ? "题目已删除" : "未找到题目");
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        ensureQuestionTypeTable();
        ensureQuestionImageRoleColumn();
        String type = value(body.get("type"), "选择题");
        String stem = value(body.get("stem"), "未命名题目");
        String difficulty = value(body.get("difficulty"), "中等");
        int rows = jdbcTemplate.update(
                "update question set type = ?, stem = ?, difficulty = ?, course_id = ?, chapter_id = ?, creator_id = ? where question_id = ?",
                type, stem, difficulty, nullableLong(body.get("courseId")), nullableLong(body.get("chapterId")), nullableLong(body.get("creatorId")), id
        );
        if (rows > 0) {
            if (body.containsKey("options")) {
                jdbcTemplate.update("delete from question_option where question_id = ?", id);
                saveOptions(id, body.get("options"));
            }
            if (body.containsKey("answerContent") || body.containsKey("answer") || body.containsKey("analysis")) {
                jdbcTemplate.update("delete from answer where question_id = ?", id);
                saveAnswer(id, body);
            }
            if (body.containsKey("images")) {
                deleteImagesByRole(id, "question");
                saveImages(id, body.get("images"), "question");
            }
            if (body.containsKey("answerImages")) {
                deleteImagesByRole(id, "answer");
                saveImages(id, body.get("answerImages"), "answer");
            }
        }
        return Map.of("success", rows > 0, "message", rows > 0 ? "题目已修改" : "未找到题目");
    }

    @GetMapping("/types")
    public Map<String, Object> types() {
        ensureQuestionTypeTable();
        return Map.of("items", loadQuestionTypes());
    }

    @PostMapping("/type")
    public Map<String, Object> createType(@RequestBody Map<String, Object> body) {
        ensureQuestionTypeTable();
        String name = value(body.get("name"), "");
        if (name.isBlank()) {
            return Map.of("success", false, "message", "题型名称不能为空");
        }
        jdbcTemplate.update(
                "insert into question_type(name, description) values (?, ?) on duplicate key update description = values(description)",
                name, value(body.get("description"), "")
        );
        return Map.of("success", true, "message", "题型已维护");
    }

    @PutMapping("/type")
    public Map<String, Object> updateType(@RequestBody Map<String, Object> body) {
        ensureQuestionTypeTable();
        String oldName = value(body.get("oldName"), "");
        String name = value(body.get("name"), "");
        if (oldName.isBlank() || name.isBlank()) {
            return Map.of("success", false, "message", "原题型和新题型不能为空");
        }
        int rows = jdbcTemplate.update(
                "update question_type set name = ?, description = ? where name = ?",
                name, value(body.get("description"), ""), oldName
        );
        jdbcTemplate.update("update question set type = ? where type = ?", name, oldName);
        return Map.of("success", rows > 0, "message", rows > 0 ? "题型已修改" : "未找到题型");
    }

    @DeleteMapping("/type/{name}")
    public Map<String, Object> deleteType(@PathVariable String name) {
        ensureQuestionTypeTable();
        int inUse = jdbcTemplate.queryForObject("select count(*) from question where type = ?", Integer.class, name);
        if (inUse > 0) {
            return Map.of("success", false, "message", "该题型已有题目使用，不能删除");
        }
        int rows = jdbcTemplate.update("delete from question_type where name = ?", name);
        return Map.of("success", rows > 0, "message", rows > 0 ? "题型已删除" : "未找到题型");
    }

    @PostMapping("/{id}/image")
    public Map<String, Object> uploadImage(@PathVariable long id, @RequestBody Map<String, Object> body) {
        ensureQuestionImageRoleColumn();
        String imageUrl = value(body.get("imageUrl"), "");
        if (imageUrl.isBlank()) {
            return Map.of("success", false, "message", "图片地址不能为空");
        }
        int exists = jdbcTemplate.queryForObject("select count(*) from question where question_id = ?", Integer.class, id);
        if (exists == 0) {
            return Map.of("success", false, "message", "未找到题目");
        }
        String imageRole = value(body.get("imageRole"), "question");
        insertImage(id, imageUrl, imageRole);
        Long imageId = jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
        return Map.of("success", true, "message", "题目图片已上传", "imageId", imageId);
    }

    @DeleteMapping("/image/{imageId}")
    public Map<String, Object> deleteImage(@PathVariable long imageId) {
        int rows = jdbcTemplate.update("delete from question_image where image_id = ?", imageId);
        return Map.of("success", rows > 0, "message", rows > 0 ? "题目图片已删除" : "未找到图片");
    }

    private String value(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }

    private void attachQuestionDetail(Map<String, Object> question) {
        Long questionId = ((Number) question.get("question_id")).longValue();
        question.put("options", jdbcTemplate.queryForList(
                "select option_id, label, content, is_correct from question_option where question_id = ? order by label",
                questionId
        ));
        question.put("answers", jdbcTemplate.queryForList(
                "select answer_id, answer_content, analysis from answer where question_id = ? order by answer_id",
                questionId
        ));
        try {
            question.put("images", jdbcTemplate.queryForList(
                    "select image_id, image_url, image_role from question_image where question_id = ? order by image_id",
                    questionId
            ));
        } catch (Exception ignored) {
            List<Map<String, Object>> images = jdbcTemplate.queryForList(
                    "select image_id, image_url from question_image where question_id = ? order by image_id",
                    questionId
            );
            images.forEach(image -> image.put("image_role", "question"));
            question.put("images", images);
        }
    }

    private void ensureQuestionDetails() {
        List<Map<String, Object>> questions = jdbcTemplate.queryForList("select question_id, type, stem from question order by question_id");
        for (Map<String, Object> question : questions) {
            Long questionId = ((Number) question.get("question_id")).longValue();
            String type = value(question.get("type"), "");
            String stem = value(question.get("stem"), "");
            if (isChoice(type) && count("select count(*) from question_option where question_id = ?", questionId) == 0) {
                insertDemoOptions(questionId, stem);
            }
            if (count("select count(*) from answer where question_id = ?", questionId) == 0) {
                jdbcTemplate.update(
                        "insert into answer(question_id, answer_content, analysis) values (?, ?, ?)",
                        questionId,
                        demoAnswer(type, stem),
                        demoAnalysis(type, stem)
                );
            }
        }
    }

    private List<Map<String, Object>> loadQuestionTypes() {
        Set<String> names = new LinkedHashSet<>(List.of("选择题", "填空题", "主观题"));
        List<Map<String, Object>> customTypes = jdbcTemplate.queryForList(
                "select type as name, '' as description from question where type is not null and type <> '' group by type"
        );
        customTypes.forEach(type -> names.add(value(type.get("name"), "")));
        List<Map<String, Object>> maintainedTypes;
        try {
            maintainedTypes = jdbcTemplate.queryForList("select name, description from question_type order by type_id");
        } catch (Exception ignored) {
            maintainedTypes = List.of();
        }
        maintainedTypes.forEach(type -> names.add(value(type.get("name"), "")));

        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : names) {
            if (!name.isBlank()) {
                String description = maintainedTypes.stream()
                        .filter(type -> name.equals(value(type.get("name"), "")))
                        .map(type -> value(type.get("description"), ""))
                        .findFirst()
                        .orElse("");
                result.add(Map.of("name", name, "description", description));
            }
        }
        return result;
    }

    private void saveOptions(long questionId, Object rawOptions) {
        if (!(rawOptions instanceof List<?> options)) {
            return;
        }
        for (Object option : options) {
            if (option instanceof Map<?, ?> item) {
                jdbcTemplate.update(
                        "insert into question_option(question_id, label, content, is_correct) values (?, ?, ?, ?)",
                        questionId,
                        value(item.get("label"), ""),
                        value(item.get("content"), ""),
                        Boolean.parseBoolean(value(item.get("isCorrect"), value(item.get("is_correct"), "false")))
                );
            }
        }
    }

    private void saveAnswer(long questionId, Map<String, Object> body) {
        String answerContent = value(body.get("answerContent"), value(body.get("answer"), ""));
        if (answerContent.isBlank()) {
            answerContent = correctOptionFromPayload(body.get("options"));
        }
        if (answerContent.isBlank()) {
            answerContent = demoAnswer(value(body.get("type"), ""), value(body.get("stem"), ""));
        }
        jdbcTemplate.update(
                "insert into answer(question_id, answer_content, analysis) values (?, ?, ?)",
                questionId,
                answerContent,
                value(body.get("analysis"), demoAnalysis(value(body.get("type"), ""), value(body.get("stem"), "")))
        );
    }

    private String correctOptionFromPayload(Object rawOptions) {
        if (!(rawOptions instanceof List<?> options)) {
            return "";
        }
        for (Object option : options) {
            if (option instanceof Map<?, ?> item
                    && Boolean.parseBoolean(value(item.get("isCorrect"), value(item.get("is_correct"), "false")))) {
                return value(item.get("label"), "");
            }
        }
        return "";
    }

    private boolean isChoice(String type) {
        return type.contains("选择") || type.contains("閫夋嫨");
    }

    private void insertDemoOptions(Long questionId, String stem) {
        String answer = demoAnswer("选择题", stem);
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
            insertOptions(questionId, "选项A", "选项B", "选项C", "选项D", answer);
        }
    }

    private void insertOptions(Long questionId, String a, String b, String c, String d, String correct) {
        insertOption(questionId, "A", a, "A".equals(correct));
        insertOption(questionId, "B", b, "B".equals(correct));
        insertOption(questionId, "C", c, "C".equals(correct));
        insertOption(questionId, "D", d, "D".equals(correct));
    }

    private void insertOption(Long questionId, String label, String content, boolean correct) {
        jdbcTemplate.update(
                "insert into question_option(question_id, label, content, is_correct) values (?, ?, ?, ?)",
                questionId, label, content, correct
        );
    }

    private String demoAnswer(String type, String stem) {
        if (stem.contains("1+1")) return "C";
        if (stem.contains("int") && stem.contains("字节")) return "C";
        if (stem.contains("输出")) return "A";
        if (stem.contains("基本数据类型")) return "D";
        if (stem.contains("定义类")) return type.contains("填空") ? "class" : "A";
        if (stem.contains("访问修饰符")) return "C";
        if (stem.contains("继承") && stem.contains("关键字")) return type.contains("填空") ? "extends" : "B";
        if (stem.contains("父类")) return "D";
        if (stem.contains("异常")) return type.contains("填空") ? "throw" : "A";
        if (stem.contains("接口") && stem.contains("继承多个接口")) return "正确";
        if (stem.contains("抽象类") && stem.contains("不能被实例化")) return "正确";
        if (stem.contains("main方法")) return "正确";
        if (stem.contains("面向对象")) return "正确";
        if (stem.contains("所有类") && stem.contains("Object")) return "正确";
        if (stem.contains("继承和接口")) return "继承用于复用父类属性和方法，Java类只能单继承；接口用于定义能力规范，一个类可以实现多个接口。";
        if (stem.contains("字符串")) return "String";
        if (stem.contains("逻辑真值")) return "boolean";
        if (stem.contains("数组")) return "[]";
        if (type.contains("判断")) return "正确";
        if (type.contains("简答") || type.contains("主观")) return "参考答案：围绕题干关键概念作答，说明定义、特点和使用场景。";
        return "参考答案";
    }

    private String demoAnalysis(String type, String stem) {
        if (stem.contains("继承和接口")) {
            return "本题考查类继承和接口实现的区别，重点是单继承与多实现。";
        }
        if (type.contains("判断")) {
            return "判断题需要依据Java语法规则判断表述是否成立。";
        }
        if (isChoice(type)) {
            return "选择题根据Java基础语法和概念定位正确选项。";
        }
        return "演示解析：用于保证题目与标准答案一一对应，可在题目性质管理中继续维护。";
    }

    private void saveImages(long questionId, Object rawImages, String defaultRole) {
        if (!(rawImages instanceof List<?> images)) {
            return;
        }
        for (Object image : images) {
            if (image instanceof Map<?, ?> item) {
                String imageUrl = value(item.get("imageUrl"), value(item.get("image_url"), ""));
                if (!imageUrl.isBlank()) {
                    insertImage(questionId, imageUrl, value(item.get("imageRole"), defaultRole));
                }
            } else if (image != null && !image.toString().isBlank()) {
                insertImage(questionId, image.toString().trim(), defaultRole);
            }
        }
    }

    private void insertImage(long questionId, String imageUrl, String imageRole) {
        try {
            jdbcTemplate.update(
                    "insert into question_image(question_id, image_url, image_role) values (?, ?, ?)",
                    questionId, imageUrl, imageRole
            );
        } catch (Exception ignored) {
            jdbcTemplate.update(
                    "insert into question_image(question_id, image_url) values (?, ?)",
                    questionId, imageUrl
            );
        }
    }

    private void deleteImagesByRole(long questionId, String imageRole) {
        try {
            jdbcTemplate.update("delete from question_image where question_id = ? and image_role = ?", questionId, imageRole);
        } catch (Exception ignored) {
            jdbcTemplate.update("delete from question_image where question_id = ?", questionId);
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

    private int count(String sql, Object... args) {
        try {
            Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
            return value == null ? 0 : value;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void ensureQuestionTypeTable() {
        try {
            jdbcTemplate.execute("""
                    create table if not exists question_type (
                        type_id bigint primary key auto_increment,
                        name varchar(30) not null unique,
                        description text
                    )
                    """);
            for (String type : List.of("选择题", "填空题", "主观题")) {
                jdbcTemplate.update("insert ignore into question_type(name, description) values (?, ?)", type, "系统内置题型");
            }
        } catch (Exception ignored) {
            // Some demo databases do not grant DDL permissions. Built-in question types are still returned in memory.
        }
    }

    private void ensureQuestionImageRoleColumn() {
        try {
            jdbcTemplate.execute("alter table question_image add column image_role varchar(20) default 'question'");
        } catch (Exception ignored) {
            // Column already exists in initialized databases.
        }
    }
}
