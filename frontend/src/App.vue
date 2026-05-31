<template>
  <main class="page">
    <header class="topbar">
      <div>
        <h1>在线学习系统</h1>
        <p>题库、题目性质、自动组卷、学生管理与错题分析的一体化实验系统</p>
      </div>
      <span class="badge">{{ dbStatus }}</span>
    </header>

    <nav class="tabs" aria-label="子系统导航">
      <button v-for="item in subsystems" :key="item.key" :class="{ active: active === item.key }" type="button" @click="active = item.key">
        {{ item.name }}
      </button>
    </nav>

    <section class="workspace">
      <aside class="summary">
        <h2>{{ current.name }}</h2>
        <p>{{ current.scope }}</p>
        <ul>
          <li v-for="uc in current.useCases" :key="uc">{{ uc }}</li>
        </ul>
      </aside>

      <section class="tool">
        <div class="tool-header">
          <h3>{{ current.title }}</h3>
          <button type="button" @click="loadActive">刷新数据</button>
        </div>

        <div v-if="message" class="message">{{ message }}</div>

        <section v-if="active === 'question'">
          <form class="form" @submit.prevent="saveQuestion">
            <label>题干<input v-model="questionForm.stem" required placeholder="例如：函数极限的定义是什么？" /></label>
            <label>题型<select v-model="questionForm.type"><option>选择题</option><option>填空题</option><option>主观题</option></select></label>
            <label>难易度<select v-model="questionForm.difficulty"><option>简单</option><option>中等</option><option>困难</option></select></label>
            <button type="submit">{{ questionForm.id ? '保存修改' : '新增题目' }}</button>
          </form>
          <DataTable :columns="questionColumns" :rows="rows" empty="暂无题目">
            <template #actions="{ row }">
              <button type="button" @click="editQuestion(row)">修改</button>
              <button type="button" class="danger" @click="deleteQuestion(row.question_id)">删除</button>
            </template>
          </DataTable>
        </section>

        <section v-if="active === 'meta'" class="stack">
          <form class="form two" @submit.prevent="createChapter">
            <label>章节名称<input v-model="chapterForm.name" required placeholder="例如：函数与极限" /></label>
            <label>排序<input v-model="chapterForm.orderNo" type="number" min="1" /></label>
            <button type="submit">{{ chapterForm.id ? '保存修改' : '新增章节' }}</button>
          </form>
          <form class="form" @submit.prevent="createKnowledgePoint">
            <label>知识点名称<input v-model="pointForm.name" required placeholder="例如：极限定义" /></label>
            <label>章节ID<input v-model="pointForm.chapterId" type="number" placeholder="可选" /></label>
            <label>描述<input v-model="pointForm.description" placeholder="例如：核心要点或备注（可选）" /></label>
            <button type="submit">新增知识点</button>
          </form>
          <form class="form" @submit.prevent="createAnswer">
            <label>题目ID<input v-model="answerForm.questionId" type="number" placeholder="对应题目ID" /></label>
            <label>答案<input v-model="answerForm.answerContent" required placeholder="参考答案" /></label>
            <label>解析<input v-model="answerForm.analysis" placeholder="例如：解题思路或要点（可选）" /></label>
            <button type="submit">新增答案</button>
          </form>
          <form class="form" @submit.prevent="createMistakePoint">
            <label>题目ID<input v-model="mistakeForm.questionId" type="number" placeholder="对应题目ID" /></label>
            <label>知识点ID<input v-model="mistakeForm.pointId" type="number" placeholder="对应知识点ID" /></label>
            <label>易错点<input v-model="mistakeForm.description" required placeholder="常见错误描述" /></label>
            <button type="submit">新增易错点</button>
          </form>
          <h4>章节</h4>
          <DataTable :columns="chapterColumns" :rows="meta.chapters || []" empty="暂无章节">
            <template #actions="{ row }">
              <button type="button" @click="editChapter(row)">修改</button>
              <button type="button" class="danger" @click="deleteChapter(row.chapter_id)">删除</button>
            </template>
          </DataTable>
          <h4>知识点</h4>
          <DataTable :columns="pointColumns" :rows="meta.knowledgePoints || []" empty="暂无知识点">
            <template #actions="{ row }">
              <button type="button" @click="editKnowledgePoint(row)">修改</button>
              <button type="button" class="danger" @click="deleteKnowledgePoint(row.point_id)">删除</button>
            </template>
          </DataTable>
          <h4>答案</h4>
          <DataTable :columns="answerColumns" :rows="meta.answers || []" empty="暂无答案">
            <template #actions="{ row }">
              <button type="button" @click="editAnswer(row)">修改</button>
              <button type="button" class="danger" @click="deleteAnswer(row.answer_id)">删除</button>
            </template>
          </DataTable>
          <h4>易错点</h4>
          <DataTable :columns="mistakeColumns" :rows="meta.mistakePoints || []" empty="暂无易错点">
            <template #actions="{ row }">
              <button type="button" @click="editMistakePoint(row)">修改</button>
              <button type="button" class="danger" @click="deleteMistakePoint(row.mistake_id)">删除</button>
            </template>
          </DataTable>
        </section>

        <section v-if="active === 'paper'" class="stack">
          <form class="form" @submit.prevent="generatePaper">
            <label>试卷名称<input v-model="paperForm.title" required placeholder="例如：高数第一章测验" /></label>
            <label>总分<input v-model="paperForm.totalScore" type="number" min="1" /></label>
            <label>题数<input v-model="paperForm.count" type="number" min="1" /></label>
            <label>难易度<select v-model="paperForm.difficulty"><option>简单</option><option>中等</option><option>困难</option></select></label>
            <button type="submit">自动生成试卷</button>
          </form>
          <h4>试卷列表</h4>
          <DataTable :columns="paperColumns" :rows="paper.papers || []" empty="暂无试卷">
            <template #actions="{ row }">
              <button type="button" class="danger" @click="deletePaper(row.paper_id)">删除</button>
            </template>
          </DataTable>
          <h4>试卷题目</h4>
          <DataTable :columns="paperQuestionColumns" :rows="paper.paperQuestions || []" empty="暂无试卷题目" />
        </section>

        <section v-if="active === 'student'" class="stack">
          <form class="form" @submit.prevent="saveStudent">
            <label>学生姓名<input v-model="studentForm.name" required placeholder="例如：张三" /></label>
            <label>院系
              <select v-model="studentForm.departmentId">
                <option value="">未分配</option>
                <option v-for="dept in studentData.departments || []" :key="dept.department_id" :value="dept.department_id">{{ dept.name }}</option>
              </select>
            </label>
            <label>班级
              <select v-model="studentForm.classId">
                <option value="">未分配</option>
                <option v-for="clazz in studentData.classes || []" :key="clazz.class_id" :value="clazz.class_id">{{ clazz.name }}</option>
              </select>
            </label>
            <button type="submit">{{ studentForm.id ? '保存修改' : '新增学生' }}</button>
          </form>
          <DataTable :columns="studentColumns" :rows="studentData.students || []" empty="暂无学生">
            <template #actions="{ row }">
              <button type="button" @click="editStudent(row)">修改</button>
              <button type="button" class="danger" @click="deleteStudent(row.student_id)">删除</button>
            </template>
          </DataTable>

          <h4>院系管理</h4>
          <form class="form compact" @submit.prevent="saveDepartment">
            <label>院系名称<input v-model="departmentForm.name" required placeholder="例如：计算机学院" /></label>
            <button type="submit">{{ departmentForm.id ? '保存修改' : '新增院系' }}</button>
          </form>
          <DataTable :columns="departmentColumns" :rows="studentData.departments || []" empty="暂无院系">
            <template #actions="{ row }">
              <button type="button" @click="editDepartment(row)">修改</button>
              <button type="button" class="danger" @click="deleteDepartment(row.department_id)">删除</button>
            </template>
          </DataTable>

          <h4>班级管理</h4>
          <form class="form" @submit.prevent="saveClass">
            <label>班级名称<input v-model="classForm.name" required placeholder="例如：软件工程2301" /></label>
            <label>年级<input v-model="classForm.grade" placeholder="例如：2023" /></label>
            <label>所属院系
              <select v-model="classForm.departmentId">
                <option value="">未分配</option>
                <option v-for="dept in studentData.departments || []" :key="dept.department_id" :value="dept.department_id">{{ dept.name }}</option>
              </select>
            </label>
            <button type="submit">{{ classForm.id ? '保存修改' : '新增班级' }}</button>
          </form>
          <DataTable :columns="classColumns" :rows="studentData.classes || []" empty="暂无班级">
            <template #actions="{ row }">
              <button type="button" @click="editClass(row)">修改</button>
              <button type="button" class="danger" @click="deleteClass(row.class_id)">删除</button>
            </template>
          </DataTable>

          <h4>课程管理与班级选课</h4>
          <form class="form" @submit.prevent="saveCourse">
            <label>课程名称<input v-model="courseForm.name" required placeholder="例如：高等数学" /></label>
            <label>学科<input v-model="courseForm.subject" placeholder="例如：数学" /></label>
            <label>说明<input v-model="courseForm.description" placeholder="课程简介" /></label>
            <button type="submit">{{ courseForm.id ? '保存修改' : '新增课程' }}</button>
          </form>
          <form class="form two" @submit.prevent="selectCourseByClass">
            <label>班级
              <select v-model="selectionForm.classId" required>
                <option value="">请选择班级</option>
                <option v-for="clazz in studentData.classes || []" :key="clazz.class_id" :value="clazz.class_id">{{ clazz.name }}</option>
              </select>
            </label>
            <label>课程
              <select v-model="selectionForm.courseId" required>
                <option value="">请选择课程</option>
                <option v-for="course in studentData.courses || []" :key="course.course_id" :value="course.course_id">{{ course.name }}</option>
              </select>
            </label>
            <button type="submit">按班级选课</button>
          </form>
          <DataTable :columns="courseColumns" :rows="studentData.courses || []" empty="暂无课程">
            <template #actions="{ row }">
              <button type="button" @click="editCourse(row)">修改</button>
              <button type="button" class="danger" @click="deleteCourse(row.course_id)">删除</button>
            </template>
          </DataTable>
          <DataTable :columns="selectionColumns" :rows="studentData.selections || []" empty="暂无选课记录">
            <template #actions="{ row }">
              <button type="button" class="danger" @click="deleteCourseSelection(row.selection_id)">退课</button>
            </template>
          </DataTable>

          <h4>学生情况分析</h4>
          <form :key="analysisSelectKey" class="form two" autocomplete="off" @submit.prevent="loadStudentAnalysis">
            <label>按学生
              <select v-model="analysisFilter.studentId" autocomplete="off" @change="selectAnalysisStudent">
                <option value="all">全部学生</option>
                <option v-for="student in studentData.students || []" :key="student.student_id" :value="String(student.student_id)">{{ student.name }}</option>
              </select>
            </label>
            <label>按班级
              <select v-model="analysisFilter.classId" autocomplete="off" @change="selectAnalysisClass">
                <option value="all">全部班级</option>
                <option v-for="clazz in studentData.classes || []" :key="clazz.class_id" :value="String(clazz.class_id)">{{ clazz.name }}</option>
              </select>
            </label>
            <button type="submit" :disabled="analysisLoading">{{ analysisLoading ? '生成中...' : '生成分析' }}</button>
          </form>
          <p v-if="analysisMessage" class="analysis-note">{{ analysisMessage }}</p>
          <div class="chart" v-if="studentAnalysis.scores && studentAnalysis.scores.length">
            <div v-for="item in studentAnalysis.scores" :key="item.result_id" class="bar-row">
              <span>{{ item.student_name }} / {{ item.paper_title || '考试' }}</span>
              <div class="bar"><i :style="{ width: `${Math.min(Number(item.score || 0), 100)}%` }"></i></div>
              <strong>{{ item.score }}</strong>
            </div>
          </div>
          <p v-else class="analysis">暂无成绩记录，录入考试成绩后可查看历次成绩波动。</p>
          <DataTable :columns="weakPointColumns" :rows="studentAnalysis.weakPoints || []" empty="暂无错误知识点统计" />
        </section>

        <section v-if="active === 'wrong'" class="stack">
          <form class="form two" @submit.prevent="createWrong">
            <label>题目ID<input v-model="wrongForm.questionId" type="number" placeholder="对应题目ID" /></label>
            <label>错误原因<input v-model="wrongForm.reason" required placeholder="例如：知识点掌握不牢" /></label>
            <button type="submit">新增错题记录</button>
          </form>
          <p class="analysis">{{ summary.suggestion || '暂无分析结果。' }}</p>
          <DataTable :columns="wrongColumns" :rows="rows" empty="暂无错题记录" />
        </section>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref, watch } from 'vue'

const DataTable = defineComponent({
  props: {
    columns: { type: Array, required: true },
    rows: { type: Array, required: true },
    empty: { type: String, default: '暂无数据' },
  },
  setup(props, { slots }) {
    return () => h('table', [
      h('thead', [h('tr', [
        ...props.columns.map((col) => h('th', col.label)),
        slots.actions ? h('th', '操作') : null,
      ])]),
      h('tbody', props.rows.length
        ? props.rows.map((row) => h('tr', [
          ...props.columns.map((col) => h('td', row[col.key] ?? '-')),
          slots.actions ? h('td', slots.actions({ row })) : null,
        ]))
        : [h('tr', [h('td', { colspan: props.columns.length + (slots.actions ? 1 : 0) }, props.empty)])]),
    ])
  },
})

const apiBase = 'http://localhost:8080'
const active = ref('question')
const rows = ref([])
const meta = ref({})
const paper = ref({})
const summary = ref({})
const studentData = ref({ students: [], departments: [], classes: [], courses: [], selections: [] })
const studentAnalysis = ref({ scores: [], weakPoints: [], summary: {} })
const analysisMessage = ref('')
const analysisLoading = ref(false)
const analysisSelectKey = ref(0)
const message = ref('')
const dbStatus = ref('数据库检查中')

const questionForm = reactive({ id: '', stem: '', type: '选择题', difficulty: '中等' })
const studentForm = reactive({ id: '', name: '', departmentId: '', classId: '' })
const departmentForm = reactive({ id: '', name: '' })
const classForm = reactive({ id: '', name: '', grade: '', departmentId: '' })
const courseForm = reactive({ id: '', name: '', subject: '', description: '' })
const selectionForm = reactive({ classId: '', courseId: '' })
const analysisFilter = reactive({ studentId: 'all', classId: 'all' })
const chapterForm = reactive({ name: '', orderNo: 1 })
const pointForm = reactive({ name: '', chapterId: '', description: '' })
const answerForm = reactive({ questionId: '', answerContent: '', analysis: '' })
const mistakeForm = reactive({ questionId: '', pointId: '', description: '' })
const paperForm = reactive({ title: '自动生成试卷', totalScore: 100, count: 5, difficulty: '中等' })
const wrongForm = reactive({ resultId: '', questionId: '', pointId: '', reason: '' })

const subsystems = [
  { key: 'question', name: '题目管理子系统', title: '题目增删改查', scope: '教师维护题库中的题目，支持选择题、填空题、带贴图主观题的新增、修改、删除、查询和状态管理。', useCases: ['新增题目', '修改题目', '删除题目', '查询题目', '上传题目图片', '维护题型'] },
  { key: 'meta', name: '题目性质管理子系统', title: '答案、知识点、章节、易错点维护', scope: '维护答案、知识点、章节、难易度、出错率和易错点，为组卷、考试和错题分析提供基础数据。', useCases: ['维护标准答案', '维护知识点', '设置难易度', '维护章节', '统计出错率', '维护易错点'] },
  { key: 'paper', name: '自动组卷子系统', title: '自动组卷', scope: '根据总分、题型数量、难易度和知识点覆盖要求从题库中自动抽题并生成试卷。', useCases: ['设置组卷条件', '自动抽取题目', '校验知识点覆盖', '计算题目分值', '生成试卷', '预览试卷'] },
  { key: 'student', name: '学生管理子系统', title: '学生增删改查', scope: '学生的院系班级分配、学生以班级为单位选课、学生管理和学生情况分析。', useCases: ['学生的院系班级分配', '学生以班级为单位选课', '学生管理（增删查改）', '学生情况分析（历次考试成绩波动图、错误知识点统计报告）'] },
  { key: 'wrong', name: '错题分析子系统', title: '错题记录与分析', scope: '对考试结果进行错题归纳，统计班级和个人在章节、知识点、难易度维度上的错误情况，并给出加强建议。', useCases: ['导入考试结果', '分析班级错题', '分析个人错题', '统计知识点错误率', '生成学习建议', '查看正确答案'] },
]

const current = computed(() => subsystems.find((item) => item.key === active.value))

const questionColumns = [{ key: 'question_id', label: 'ID' }, { key: 'type', label: '题型' }, { key: 'stem', label: '题干' }, { key: 'difficulty', label: '难易度' }]
const studentColumns = [{ key: 'student_id', label: 'ID' }, { key: 'name', label: '姓名' }, { key: 'department_name', label: '院系' }, { key: 'class_name', label: '班级' }, { key: 'grade', label: '年级' }]
const departmentColumns = [{ key: 'department_id', label: 'ID' }, { key: 'name', label: '院系' }]
const classColumns = [{ key: 'class_id', label: 'ID' }, { key: 'name', label: '班级' }, { key: 'grade', label: '年级' }, { key: 'department_name', label: '院系' }]
const courseColumns = [{ key: 'course_id', label: 'ID' }, { key: 'name', label: '课程' }, { key: 'subject', label: '学科' }, { key: 'description', label: '说明' }]
const selectionColumns = [{ key: 'selection_id', label: 'ID' }, { key: 'class_name', label: '班级' }, { key: 'student_name', label: '学生' }, { key: 'course_name', label: '课程' }, { key: 'selected_time', label: '选课时间' }]
const weakPointColumns = [{ key: 'point_id', label: '知识点ID' }, { key: 'point_name', label: '错误知识点' }, { key: 'wrong_count', label: '错误次数' }, { key: 'student_names', label: '涉及学生' }]
const chapterColumns = [{ key: 'chapter_id', label: 'ID' }, { key: 'name', label: '章节' }, { key: 'order_no', label: '排序' }]
const pointColumns = [{ key: 'point_id', label: 'ID' }, { key: 'chapter_id', label: '章节ID' }, { key: 'name', label: '知识点' }, { key: 'description', label: '描述' }]
const answerColumns = [{ key: 'answer_id', label: 'ID' }, { key: 'question_id', label: '题目ID' }, { key: 'answer_content', label: '答案' }, { key: 'analysis', label: '解析' }]
const mistakeColumns = [{ key: 'mistake_id', label: 'ID' }, { key: 'question_id', label: '题目ID' }, { key: 'point_id', label: '知识点ID' }, { key: 'description', label: '易错点' }]
const paperColumns = [{ key: 'paper_id', label: 'ID' }, { key: 'title', label: '试卷' }, { key: 'total_score', label: '总分' }, { key: 'difficulty', label: '难易度' }]
const paperQuestionColumns = [{ key: 'paper_id', label: '试卷ID' }, { key: 'question_id', label: '题目ID' }, { key: 'score', label: '分值' }, { key: 'order_no', label: '序号' }]
const wrongColumns = [{ key: 'wrong_id', label: 'ID' }, { key: 'result_id', label: '考试结果ID' }, { key: 'question_id', label: '题目ID' }, { key: 'point_id', label: '知识点ID' }, { key: 'reason', label: '原因' }]

async function request(path, options) {
  const response = await fetch(`${apiBase}${path}`, { headers: { 'Content-Type': 'application/json' }, ...options })
  if (!response.ok) throw new Error(`请求失败：${response.status}`)
  return response.json()
}

async function checkDb() {
  try {
    const result = await request('/api/db/health')
    dbStatus.value = `数据库已连接：${result.tableCount} 张表`
  } catch {
    dbStatus.value = '数据库未连接'
  }
}

async function loadActive() {
  message.value = ''
  if (active.value === 'question') rows.value = (await request('/api/question')).items || []
  if (active.value === 'student') {
    studentData.value = await request('/api/student')
    Object.assign(analysisFilter, { studentId: 'all', classId: 'all' })
    analysisSelectKey.value += 1
    analysisMessage.value = ''
    await loadStudentAnalysis()
  }
  if (active.value === 'wrong') {
    const data = await request('/api/wrong-analysis')
    rows.value = data.items || []
    summary.value = data.summary || {}
  }
  if (active.value === 'meta') meta.value = await request('/api/question-meta')
  if (active.value === 'paper') paper.value = await request('/api/auto-paper')
}

async function saveQuestion() {
  const path = questionForm.id ? `/api/question/${questionForm.id}` : '/api/question'
  const method = questionForm.id ? 'PUT' : 'POST'
  const result = await request(path, { method, body: JSON.stringify(questionForm) })
  message.value = result.message
  Object.assign(questionForm, { id: '', stem: '', type: '选择题', difficulty: '中等' })
  await loadActive()
}

function editQuestion(row) {
  Object.assign(questionForm, { id: row.question_id, stem: row.stem, type: row.type, difficulty: row.difficulty })
}

async function deleteQuestion(id) {
  message.value = (await request(`/api/question/${id}`, { method: 'DELETE' })).message
  await loadActive()
}

async function saveStudent() {
  const path = studentForm.id ? `/api/student/${studentForm.id}` : '/api/student'
  const method = studentForm.id ? 'PUT' : 'POST'
  message.value = (await request(path, { method, body: JSON.stringify(studentForm) })).message
  Object.assign(studentForm, { id: '', name: '', departmentId: '', classId: '' })
  await loadActive()
}

function editStudent(row) {
  Object.assign(studentForm, { id: row.student_id, name: row.name, departmentId: row.department_id || '', classId: row.class_id || '' })
}

async function deleteStudent(id) {
  message.value = (await request(`/api/student/${id}`, { method: 'DELETE' })).message
  await loadActive()
}

async function saveDepartment() {
  const path = departmentForm.id ? `/api/student/department/${departmentForm.id}` : '/api/student/department'
  const method = departmentForm.id ? 'PUT' : 'POST'
  message.value = (await request(path, { method, body: JSON.stringify(departmentForm) })).message
  Object.assign(departmentForm, { id: '', name: '' })
  await loadActive()
}

function editDepartment(row) {
  Object.assign(departmentForm, { id: row.department_id, name: row.name })
}

async function deleteDepartment(id) {
  if (!confirm('确定删除该院系吗？相关班级会删除，学生会解除院系班级分配。')) return
  message.value = (await request(`/api/student/department/${id}`, { method: 'DELETE' })).message
  await loadActive()
}

async function saveClass() {
  const path = classForm.id ? `/api/student/class/${classForm.id}` : '/api/student/class'
  const method = classForm.id ? 'PUT' : 'POST'
  message.value = (await request(path, { method, body: JSON.stringify(classForm) })).message
  Object.assign(classForm, { id: '', name: '', grade: '', departmentId: '' })
  await loadActive()
}

function editClass(row) {
  Object.assign(classForm, { id: row.class_id, name: row.name, grade: row.grade || '', departmentId: row.department_id || '' })
}

async function deleteClass(id) {
  if (!confirm('确定删除该班级吗？学生会解除班级分配。')) return
  message.value = (await request(`/api/student/class/${id}`, { method: 'DELETE' })).message
  await loadActive()
}

async function saveCourse() {
  const path = courseForm.id ? `/api/student/course/${courseForm.id}` : '/api/student/course'
  const method = courseForm.id ? 'PUT' : 'POST'
  message.value = (await request(path, { method, body: JSON.stringify(courseForm) })).message
  Object.assign(courseForm, { id: '', name: '', subject: '', description: '' })
  await loadActive()
}

function editCourse(row) {
  Object.assign(courseForm, { id: row.course_id, name: row.name, subject: row.subject || '', description: row.description || '' })
}

async function deleteCourse(id) {
  if (!confirm('确定删除该课程吗？相关选课记录会一起删除。')) return
  message.value = (await request(`/api/student/course/${id}`, { method: 'DELETE' })).message
  await loadActive()
}

async function selectCourseByClass() {
  message.value = (await request('/api/student/class-course-selection', { method: 'POST', body: JSON.stringify(selectionForm) })).message
  Object.assign(selectionForm, { classId: '', courseId: '' })
  await loadActive()
}

async function deleteCourseSelection(id) {
  message.value = (await request(`/api/student/course-selection/${id}`, { method: 'DELETE' })).message
  await loadActive()
}

async function selectAnalysisStudent() {
  analysisFilter.classId = 'all'
  await loadStudentAnalysis()
}

async function selectAnalysisClass() {
  analysisFilter.studentId = 'all'
  await loadStudentAnalysis()
}

async function loadStudentAnalysis() {
  const params = new URLSearchParams()
  const studentId = analysisFilter.studentId
  const classId = analysisFilter.classId
  if (studentId !== 'all') {
    params.set('studentId', studentId)
  } else if (classId !== 'all') {
    params.set('classId', classId)
  }
  const scope = studentId !== 'all' ? '当前学生' : classId !== 'all' ? '当前班级' : '全部学生'
  analysisLoading.value = true
  analysisMessage.value = `${scope}分析正在生成...`
  try {
    studentAnalysis.value = await request(`/api/student/analysis${params.toString() ? `?${params}` : ''}`)
    const scoreCount = studentAnalysis.value.summary?.scoreCount || 0
    const weakPointCount = studentAnalysis.value.summary?.weakPointCount || 0
    const time = new Date().toLocaleTimeString()
    analysisMessage.value = scoreCount || weakPointCount
      ? `${scope}分析已生成：${scoreCount} 条成绩记录，${weakPointCount} 个错误知识点统计。（${time}）`
      : `${scope}分析已生成：暂无成绩或错误知识点记录。（${time}）`
  } catch (error) {
    analysisMessage.value = `分析生成失败：${error.message}`
  } finally {
    analysisLoading.value = false
  }
}

async function createChapter() {
  if (chapterForm.id) {
    message.value = (await request(`/api/question-meta/chapter/${chapterForm.id}`, { method: 'PUT', body: JSON.stringify({ name: chapterForm.name, orderNo: chapterForm.orderNo }) })).message
  } else {
    message.value = (await request('/api/question-meta/chapter', { method: 'POST', body: JSON.stringify(chapterForm) })).message
  }
  Object.assign(chapterForm, { id: '', name: '', orderNo: 1 })
  await loadActive()
}

function editChapter(row) {
  Object.assign(chapterForm, { id: row.chapter_id, name: row.name, orderNo: row.order_no })
}

async function deleteChapter(id) {
  if (!confirm('确定删除该章节吗？可能会影响关联的知识点或题目。')) return
  message.value = (await request(`/api/question-meta/chapter/${id}`, { method: 'DELETE' })).message
  await loadActive()
}

async function createKnowledgePoint() {
  if (pointForm.id) {
    message.value = (await request(`/api/question-meta/knowledge-point/${pointForm.id}`, { method: 'PUT', body: JSON.stringify({ chapterId: pointForm.chapterId, name: pointForm.name, description: pointForm.description }) })).message
  } else {
    message.value = (await request('/api/question-meta/knowledge-point', { method: 'POST', body: JSON.stringify(pointForm) })).message
  }
  Object.assign(pointForm, { id: '', name: '', chapterId: '', description: '' })
  await loadActive()
}

function editKnowledgePoint(row) {
  Object.assign(pointForm, { id: row.point_id, name: row.name, chapterId: row.chapter_id, description: row.description })
}

async function deleteKnowledgePoint(id) {
  if (!confirm('确定删除该知识点吗？可能会影响关联题目。')) return
  message.value = (await request(`/api/question-meta/knowledge-point/${id}`, { method: 'DELETE' })).message
  await loadActive()
}

async function createAnswer() {
  if (answerForm.id) {
    message.value = (await request(`/api/question-meta/answer/${answerForm.id}`, { method: 'PUT', body: JSON.stringify({ questionId: answerForm.questionId, answerContent: answerForm.answerContent, analysis: answerForm.analysis }) })).message
  } else {
    message.value = (await request('/api/question-meta/answer', { method: 'POST', body: JSON.stringify(answerForm) })).message
  }
  Object.assign(answerForm, { id: '', questionId: '', answerContent: '', analysis: '' })
  await loadActive()
}

function editAnswer(row) {
  Object.assign(answerForm, { id: row.answer_id, questionId: row.question_id, answerContent: row.answer_content, analysis: row.analysis })
}

async function deleteAnswer(id) {
  if (!confirm('确定删除该答案吗？')) return
  message.value = (await request(`/api/question-meta/answer/${id}`, { method: 'DELETE' })).message
  await loadActive()
}

async function createMistakePoint() {
  if (mistakeForm.id) {
    message.value = (await request(`/api/question-meta/mistake-point/${mistakeForm.id}`, { method: 'PUT', body: JSON.stringify({ questionId: mistakeForm.questionId, pointId: mistakeForm.pointId, description: mistakeForm.description }) })).message
  } else {
    message.value = (await request('/api/question-meta/mistake-point', { method: 'POST', body: JSON.stringify(mistakeForm) })).message
  }
  Object.assign(mistakeForm, { id: '', questionId: '', pointId: '', description: '' })
  await loadActive()
}

function editMistakePoint(row) {
  Object.assign(mistakeForm, { id: row.mistake_id, questionId: row.question_id, pointId: row.point_id, description: row.description })
}

async function deleteMistakePoint(id) {
  if (!confirm('确定删除该易错点吗？')) return
  message.value = (await request(`/api/question-meta/mistake-point/${id}`, { method: 'DELETE' })).message
  await loadActive()
}

async function generatePaper() {
  message.value = (await request('/api/auto-paper/generate', { method: 'POST', body: JSON.stringify(paperForm) })).message
  await loadActive()
}

async function deletePaper(id) {
  if (!confirm('确定删除这张试卷吗？相关试卷题目、考试、成绩和错题记录也会一起删除。')) return
  message.value = (await request(`/api/auto-paper/${id}`, { method: 'DELETE' })).message
  await loadActive()
}

async function createWrong() {
  message.value = (await request('/api/wrong-analysis', { method: 'POST', body: JSON.stringify(wrongForm) })).message
  Object.assign(wrongForm, { resultId: '', questionId: '', pointId: '', reason: '' })
  await loadActive()
}

watch(active, loadActive)
onMounted(async () => { await checkDb(); await loadActive() })
</script>

<style>
body { margin: 0; font-family: Arial, "Microsoft YaHei", sans-serif; background: #f4f7fb; color: #1f2937; }
button, input, select { font: inherit; }
.page { max-width: 1240px; margin: 0 auto; padding: 28px; }
.topbar, .tool-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
h1, h2, h3, h4 { margin: 0; }
.topbar p, .summary p, .analysis { color: #4b5563; line-height: 1.7; }
.badge { border: 1px solid #b9c6d6; border-radius: 999px; padding: 8px 14px; background: #fff; color: #31516f; white-space: nowrap; }
.tabs { display: flex; flex-wrap: wrap; gap: 8px; margin: 22px 0; }
.tabs button, .tool button, .form button { border: 1px solid #b9c6d6; border-radius: 6px; background: #fff; color: #1f2937; padding: 9px 13px; cursor: pointer; }
.tabs button.active, .form button { background: #225ea8; border-color: #225ea8; color: #fff; }
.workspace { display: grid; grid-template-columns: 330px minmax(0, 1fr); gap: 18px; }
.summary, .tool { background: #fff; border: 1px solid #d8dee9; border-radius: 8px; padding: 18px; }
.summary ul { padding-left: 20px; }
.stack { display: grid; gap: 16px; }
.form { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; align-items: end; margin: 18px 0; }
.form.two { grid-template-columns: 1fr 1fr auto; margin: 0; }
.form.compact { grid-template-columns: 1fr auto; }
label { display: grid; gap: 6px; color: #334155; }
input, select { border: 1px solid #cbd5e1; border-radius: 6px; padding: 10px 12px; min-width: 0; }
.message { margin: 12px 0; padding: 10px 12px; border: 1px solid #add8bb; border-radius: 6px; background: #effaf2; color: #17653a; }
.analysis-note { margin: -4px 0 4px; color: #31516f; }
.chart { display: grid; gap: 10px; margin: 12px 0; }
.bar-row { display: grid; grid-template-columns: minmax(160px, 1fr) minmax(160px, 2fr) 64px; gap: 10px; align-items: center; }
.bar { height: 12px; border-radius: 999px; overflow: hidden; background: #e2e8f0; }
.bar i { display: block; height: 100%; background: #225ea8; }
table { width: 100%; border-collapse: collapse; margin-top: 14px; background: #fff; }
th, td { border-bottom: 1px solid #e2e8f0; padding: 10px; text-align: left; vertical-align: top; }
th { background: #f8fafc; color: #334155; }
.danger { color: #b42318; }
@media (max-width: 900px) { .workspace, .form, .form.two, .form.compact, .bar-row { grid-template-columns: 1fr; } .topbar { align-items: flex-start; flex-direction: column; } }
</style>
