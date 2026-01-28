<template>
  <div class="notifications-container premium-container">
    <div class="page-header">
      <h1 class="page-title">Notifications</h1>
      <el-button link @click="markAllRead">Mark all as read</el-button>
    </div>

    <el-tabs v-model="activeTab" class="notification-tabs">
      <el-tab-pane label="All" name="all">
        <div class="notification-list">
            <el-card v-for="item in notifications" :key="item.id" class="notification-item" :class="{ unread: !item.readAt }">
                <div class="notification-icon">
                    <span v-if="item.type === 'SYSTEM'">🔔</span>
                    <span v-else-if="item.type === 'CLUB'">📢</span>
                    <span v-else>✉️</span>
                </div>
                <div class="notification-content">
                    <h4 class="notification-title">{{ item.title }}</h4>
                    <p class="notification-body">{{ item.content }}</p>
                    <span class="notification-time">{{ formatDate(item.createdAt) }}</span>
                </div>
                <div class="notification-action">
                    <el-button v-if="!item.readAt" size="small" circle icon="Check" @click="markRead(item)" />
                </div>
            </el-card>
            <el-empty v-if="notifications.length === 0" description="No notifications" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="Unread" name="unread">
          <!-- Filtered view could be here -->
          <el-empty description="No unread messages" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/api/axios'

const activeTab = ref('all')
const notifications = ref([])

onMounted(async () => {
    // Mock data for now as backend endpoint might need work
    notifications.value = [
        { id: 1, type: 'SYSTEM', title: 'Welcome to Community', content: 'Explore clubs and join activities!', createdAt: new Date(), readAt: null },
        { id: 2, type: 'CLUB', title: 'Coding Club: New Event', content: 'Join our Hackathon next week.', createdAt: new Date(Date.now() - 86400000), readAt: new Date() }
    ]
    // try {
    //    notifications.value = await axios.get('/notifications')
    // } catch (e) {}
})

const formatDate = (date) => {
    return new Date(date).toLocaleString()
}

const markRead = (item) => {
    item.readAt = new Date()
}

const markAllRead = () => {
    notifications.value.forEach(n => n.readAt = new Date())
}
</script>

<style scoped>
.notifications-container {
    padding-top: 2rem;
    padding-bottom: 2rem;
    min-height: 80vh;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
}

.page-title {
    font-size: 2rem;
    font-weight: 800;
}

.notification-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.notification-item {
    border-left: 4px solid transparent;
    transition: all 0.3s;
}

.notification-item.unread {
    border-left-color: var(--color-accent);
    background-color: #FEF3C7; /* Light amber bg for unread */
}

.notification-item :deep(.el-card__body) {
    display: flex;
    align-items: flex-start;
    gap: 1rem;
    padding: 1.5rem;
}

.notification-icon {
    font-size: 1.5rem;
    background-color: white;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: var(--shadow-sm);
}

.notification-content {
    flex: 1;
}

.notification-title {
    font-size: 1.1rem;
    margin-bottom: 0.5rem;
    font-weight: 700;
}

.notification-body {
    color: var(--color-text);
    margin-bottom: 0.5rem;
}

.notification-time {
    font-size: 0.8rem;
    color: var(--color-text-light);
}
</style>
