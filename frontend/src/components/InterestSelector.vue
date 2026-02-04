<template>
  <div class="interest-selector">
    <div 
      v-for="interest in displayInterests" 
      :key="interest.name"
      class="interest-card"
      :class="{ 
        'selected': isSelected(interest.name), 
        'readonly': readonly,
        'clickable': !readonly
      }"
      :style="isSelected(interest.name) ? { borderColor: interest.color } : {}"
      @click="toggleInterest(interest.name)"
    >
      <div class="icon-circle" :style="{ 
        backgroundColor: interest.color,
        color: '#fff'
      }">
        {{ interest.name.charAt(0).toUpperCase() }}
      </div>
      <span class="interest-name">{{ interest.name }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Check } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

// Predefined interests list with colors (Club related)
const allInterests = [
  // Sports
  { name: '篮球', color: '#FF5722' },
  { name: '足球', color: '#4CAF50' },
  { name: '羽毛球', color: '#2196F3' },
  { name: '乒乓球', color: '#FFC107' },
  { name: '游泳', color: '#00BCD4' },
  { name: '跑步', color: '#FF9800' },
  
  // Arts
  { name: '绘画', color: '#E91E63' },
  { name: '摄影', color: '#607D8B' },
  { name: '书法', color: '#795548' },
  { name: '舞蹈', color: '#9C27B0' },
  { name: '声乐', color: '#673AB7' },
  { name: '乐器', color: '#3F51B5' },
  
  // Academic & Others
  { name: '编程', color: '#212121' },
  { name: '阅读', color: '#8D6E63' },
  { name: '英语', color: '#304FFE' },
  { name: '志愿服务', color: '#F44336' },
  { name: '电子竞技', color: '#1A237E' },
  { name: '棋牌', color: '#009688' },
  { name: '电影', color: '#607D8B' }
]

const displayInterests = computed(() => {
  if (props.readonly) {
    // In readonly mode, only show selected interests
    // Map selected names to full interest objects (to get colors), fallback to default color if not found
    return props.modelValue.map(name => {
      const found = allInterests.find(i => i.name === name)
      return found || { name, color: '#909399' }
    })
  } else {
    // In edit mode, show all available interests
    return allInterests
  }
})

const isSelected = (name) => {
  return props.modelValue.includes(name)
}

const toggleInterest = (name) => {
  if (props.readonly) return

  const newSelection = [...props.modelValue]
  const index = newSelection.indexOf(name)
  
  if (index === -1) {
    newSelection.push(name)
  } else {
    newSelection.splice(index, 1)
  }
  
  emit('update:modelValue', newSelection)
}
</script>

<style scoped>
.interest-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.interest-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  padding: 8px;
  
  /* Glassmorphism Style */
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.1);
  
  border-radius: 50%;
  cursor: default;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  position: relative; /* For check mark positioning */
  box-sizing: border-box;
}

.interest-card.clickable {
  cursor: pointer;
}

.interest-card.clickable:hover {
  border-color: rgba(255, 255, 255, 0.9);
  transform: translateY(-4px);
  box-shadow: 0 12px 40px 0 rgba(31, 38, 135, 0.15);
  background: rgba(255, 255, 255, 0.4);
}

.interest-card.selected {
  /* Border color is set by inline style to match interest color */
  background: rgba(255, 255, 255, 0.6);
  transform: translateY(-4px);
  box-shadow: 0 12px 40px 0 rgba(31, 38, 135, 0.15);
  border-width: 2px;
}

.icon-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px; /* Space between icon and text */
  font-size: 12px;
  font-weight: bold;
  color: white;
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.interest-name {
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  text-align: center;
}

.interest-card.selected .interest-name {
  color: #303133;
  font-weight: 600;
}
</style>
