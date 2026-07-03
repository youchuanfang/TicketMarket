<template>
  <div class="page narrow">
    <SectionHeader title="观演人管理" eyebrow="Viewers" />
    <section class="detail-section">
      <div class="viewer-toolbar">
        <p>实名观演人用于购票和入场核验，证件号仅脱敏展示。</p>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增观演人</el-button>
      </div>
      <el-table :data="viewers" border>
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column prop="idCardMasked" label="身份证号" min-width="180" />
        <el-table-column prop="phoneMasked" label="手机号" min-width="140" />
        <el-table-column label="默认" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.defaultViewer" type="success">默认</el-tag>
            <el-button v-else link type="primary" @click="makeDefault(row)">设为默认</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">修改</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <section v-if="!viewers.length" class="empty-inline">
        <el-icon><User /></el-icon>
        <span>暂无观演人</span>
      </section>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '修改观演人' : '新增观演人'" width="420px">
      <el-form :model="form" label-position="top">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import SectionHeader from '../components/SectionHeader.vue'
import { addViewer, deleteViewer, getViewers, setDefaultViewer, updateViewer } from '../api/auth'

const viewers = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const form = reactive({ name: '', idCard: '', phone: '' })

const load = async () => {
  viewers.value = await getViewers()
}

const resetForm = () => {
  Object.assign(form, { name: '', idCard: '', phone: '' })
  editingId.value = null
}

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  Object.assign(form, { name: row.name, idCard: row.idCardMasked, phone: row.phoneMasked })
  dialogVisible.value = true
}

const save = async () => {
  try {
    if (editingId.value) {
      await updateViewer(editingId.value, form)
      ElMessage.success('观演人已更新')
    } else {
      await addViewer(form)
      ElMessage.success('观演人已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除观演人「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await deleteViewer(row.id)
    ElMessage.success('观演人已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const makeDefault = async (row) => {
  try {
    await setDefaultViewer(row.id)
    ElMessage.success('默认观演人已更新')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>
