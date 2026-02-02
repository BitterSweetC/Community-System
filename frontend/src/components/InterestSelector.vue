<template>
  <div class="interest-selector">
    <div 
      v-for="interest in displayInterests" 
      :key="interest.name"
      class="interest-tag"
      :class="{ 
        'selected': isSelected(interest.name), 
        'readonly': readonly,
        'clickable': !readonly
      }"
      :style="isSelected(interest.name) ? { backgroundColor: interest.color, borderColor: interest.color } : {}"
      @click="toggleInterest(interest.name)"
    >
      <div class="icon-circle" :style="{ 
        backgroundColor: isSelected(interest.name) ? '#fff' : interest.color,
        color: isSelected(interest.name) ? interest.color : '#fff'
      }">
        {{ interest.name.charAt(0).toUpperCase() }}
      </div>
      <span class="interest-name">{{ interest.name }}</span>
      
      <!-- Check mark for selected items -->
      <div v-if="isSelected(interest.name)" class="check-mark">
        <el-icon><Check /></el-icon>
      </div>
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
  gap: 12px;
}

.interest-tag {
  display: flex;
  align-items: center;
  padding: 6px 16px 6px 6px; /* Left padding smaller for icon */
  background-color: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 9999px; /* Capsule shape */
  cursor: default;
  transition: all 0.2s ease;
  user-select: none;
  position: relative; /* For check mark positioning */
}

.interest-tag.clickable {
  cursor: pointer;
}

.interest-tag.clickable:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}

.interest-tag.selected {
  /* Default style if no inline style applied, but we use inline style for color */
  border-color: transparent;
  color: white;
  padding-right: 24px; /* Space for check mark */
}

.icon-circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  font-size: 12px;
  font-weight: bold;
  color: white;
  transition: all 0.2s ease;
}

.interest-name {
  font-size: 14px;
  font-weight: 500;
}

.check-mark {
  position: absolute;
  top: -4px;
  right: -4px;
  background-color: white;
  color: var(--color-primary, #409EFF); /* Or inherit from parent color if possible, but simple white bg is safer */
  border-radius: 50%;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
  border: 1px solid #eee;
}

/* Adjust checkmark color to match interest color? 
   Since we can't easily access the parent's inline color here, 
   we can use a neutral color or rely on the parent's text color if we use currentColor.
   Let's try using the text color of the tag (which is white) for checkmark BG, and tag BG for checkmark color?
   No, tag text is white. 
   Let's make checkmark: White Background, Gray Icon or Black Icon.
*/
.check-mark {
  color: #67c23a; /* Success green */
}
</style>
