<template>
  <div class="academic-container">
    <div class="page-header">
      <div class="breadcrumb">
        <router-link to="/home" class="breadcrumb-link">首页</router-link>
        <span class="separator">/</span>
        <span>社团日程</span>
      </div>
      <h2>社团活动日程</h2>
    </div>

    <div class="calendar-wrapper">
       <el-calendar v-model="value">
          <template #date-cell="{ data }">
            <div class="calendar-cell" :class="{ 'has-event': getEvents(data.day).length > 0 }">
               <p class="cell-date">{{ data.day.split('-').slice(2).join('') }}</p>
               <div class="event-list">
                  <div v-for="(event, index) in getEvents(data.day)" :key="index" class="event-item">
                     <span class="event-dot"></span>
                     <span class="event-title">{{ event.title }}</span>
                  </div>
               </div>
            </div>
          </template>
       </el-calendar>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/api/axios'

const value = ref(new Date())
const activities = ref([])

const fetchAllActivities = async () => {
    try {
        // Fetch a large batch for calendar
        const res = await axios.get('/activities?size=100')
        if (res.list) activities.value = res.list
        else if (res.content) activities.value = res.content
        else activities.value = res
    } catch (e) {
        console.error(e)
    }
}

const getEvents = (dateStr) => {
    return activities.value.filter(a => {
        if (!a.startTime) return false
        return a.startTime.startsWith(dateStr)
    })
}

onMounted(() => {
    fetchAllActivities()
})
</script>

<style scoped>
.academic-container {
  padding: 20px;
  background-color: #fff;
  min-height: 80vh;
}

.page-header {
  border-bottom: 2px solid #1f2937;
  margin-bottom: 20px;
  padding-bottom: 10px;
}

.breadcrumb {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.breadcrumb-link {
  color: #606266;
  text-decoration: none;
  transition: color 0.2s;
}

.breadcrumb-link:hover {
  color: #3b82f6;
}

.separator {
  margin: 0 8px;
  color: #c0c4cc;
}

.calendar-wrapper {
    background: white;
    padding: 20px;
    border: 1px solid #ebeef5;
}

.calendar-cell {
    height: 100%;
    display: flex;
    flex-direction: column;
}

.cell-date {
    text-align: center;
    margin: 0 0 5px 0;
}

.has-event .cell-date {
    color: #3b82f6;
    font-weight: bold;
}

.event-list {
    flex: 1;
    overflow-y: auto;
    font-size: 12px;
}

.event-item {
    display: flex;
    align-items: center;
    margin-bottom: 2px;
    padding: 2px;
    background: #f0f9eb;
    border-radius: 2px;
    color: #67c23a;
}

.event-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #67c23a;
    margin-right: 4px;
}

.event-title {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
</style>