<template>
  <div class="interest-selector" :class="{ 'is-readonly': readonly }">
    <button
      v-for="interest in displayInterests"
      :key="interest.name"
      type="button"
      class="interest-card"
      :class="{
        selected: isSelected(interest.name),
        readonly,
        clickable: !readonly
      }"
      :style="{ '--interest-color': interest.color }"
      :disabled="readonly"
      @click="toggleInterest(interest.name)"
    >
      <span class="icon-circle">{{ interest.name.charAt(0).toUpperCase() }}</span>
      <span class="interest-name">{{ interest.name }}</span>
      <span v-if="!readonly && isSelected(interest.name)" class="selected-mark" aria-hidden="true">
        <el-icon><Check /></el-icon>
      </span>
    </button>
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

const allInterests = [
  { name: '篮球', color: '#f97316' },
  { name: '足球', color: '#16a34a' },
  { name: '羽毛球', color: '#0ea5e9' },
  { name: '乒乓球', color: '#eab308' },
  { name: '游泳', color: '#06b6d4' },
  { name: '跑步', color: '#f59e0b' },
  { name: '绘画', color: '#ec4899' },
  { name: '摄影', color: '#64748b' },
  { name: '书法', color: '#8b5e3c' },
  { name: '舞蹈', color: '#a855f7' },
  { name: '声乐', color: '#7c3aed' },
  { name: '乐器', color: '#4f46e5' },
  { name: '编程', color: '#374151' },
  { name: '阅读', color: '#8b5e3c' },
  { name: '英语', color: '#2563eb' },
  { name: '志愿服务', color: '#ef4444' },
  { name: '电子游戏', color: '#1e3a8a' },
  { name: '棋类', color: '#0d9488' },
  { name: '电影', color: '#475569' }
]

const displayInterests = computed(() => {
  if (props.readonly) {
    return props.modelValue.map((name) => {
      const found = allInterests.find((item) => item.name === name)
      return found || { name, color: '#7f8fa4' }
    })
  }

  return allInterests
})

const isSelected = (name) => props.modelValue.includes(name)

const toggleInterest = (name) => {
  if (props.readonly) {
    return
  }

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
  gap: 10px;
}

.interest-card {
  --interest-color: #4f46e5;

  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  border-radius: 999px;
  border: 1px solid #d7e0ea;
  background: linear-gradient(180deg, #ffffff 0%, #f5f9ff 100%);
  padding: 6px 12px 6px 8px;
  color: #314055;
  font-size: 13px;
  line-height: 1;
  user-select: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.interest-card.clickable {
  cursor: pointer;
}

.interest-card.clickable:hover {
  transform: translateY(-1px);
  border-color: rgba(79, 70, 229, 0.36);
  box-shadow: 0 8px 16px rgba(20, 33, 50, 0.08);
}

.interest-card.selected {
  border-color: color-mix(in srgb, var(--interest-color) 60%, #d7e0ea);
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--interest-color) 12%, #ffffff) 0%,
    color-mix(in srgb, var(--interest-color) 8%, #f5f9ff) 100%
  );
  box-shadow: 0 8px 16px rgba(20, 33, 50, 0.1);
}

.interest-card.readonly {
  cursor: default;
}

.icon-circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--interest-color);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.interest-name {
  white-space: nowrap;
  color: #314055;
  font-weight: 600;
}

.selected-mark {
  display: inline-flex;
  align-items: center;
  color: var(--interest-color);
  font-size: 14px;
}

.interest-selector.is-readonly {
  gap: 8px;
}

.interest-selector.is-readonly .interest-card {
  min-height: 36px;
  padding: 5px 11px 5px 7px;
  border-color: #d9e3ef;
  box-shadow: none;
}

.interest-selector.is-readonly .interest-name {
  font-size: 12px;
}

@media (max-width: 768px) {
  .interest-card {
    min-height: 38px;
    padding: 5px 10px 5px 7px;
    font-size: 12px;
  }

  .icon-circle {
    width: 22px;
    height: 22px;
    font-size: 11px;
  }
}
</style>
