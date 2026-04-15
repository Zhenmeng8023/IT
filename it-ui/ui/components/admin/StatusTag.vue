<template>
  <el-tag :type="resolvedType" :size="size">
    <slot>{{ resolvedText }}</slot>
  </el-tag>
</template>

<script>
export default {
  name: 'StatusTag',
  props: {
    value: {
      type: [String, Number, Boolean],
      default: ''
    },
    type: {
      type: String,
      default: ''
    },
    size: {
      type: String,
      default: 'small'
    },
    textMap: {
      type: Object,
      default() {
        return {}
      }
    },
    typeMap: {
      type: Object,
      default() {
        return {}
      }
    },
    defaultText: {
      type: String,
      default: '-'
    },
    defaultType: {
      type: String,
      default: 'info'
    }
  },
  computed: {
    resolvedText() {
      if (this.textMap && Object.prototype.hasOwnProperty.call(this.textMap, this.value)) {
        return this.textMap[this.value]
      }
      if (this.value === null || this.value === undefined || this.value === '') {
        return this.defaultText
      }
      return String(this.value)
    },
    resolvedType() {
      if (this.type) {
        return this.type
      }
      if (this.typeMap && Object.prototype.hasOwnProperty.call(this.typeMap, this.value)) {
        return this.typeMap[this.value]
      }
      return this.defaultType
    }
  }
}
</script>
