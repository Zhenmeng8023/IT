<template>
  <div class="rich-content-viewer" v-html="html"></div>
</template>

<script>
import { renderRichContent } from '@/utils/richContent'

export default {
  name: 'RichContentViewer',
  props: {
    source: {
      type: [String, Object, Array],
      default: ''
    },
    emptyText: {
      type: String,
      default: '暂无内容'
    }
  },
  computed: {
    html() {
      return renderRichContent(this.source, { emptyText: this.emptyText })
    }
  }
}
</script>

<style scoped>
.rich-content-viewer {
  width: 100%;
  min-width: 0;
  color: inherit;
  font-size: inherit;
  line-height: inherit;
  word-break: break-word;
  overflow-wrap: anywhere;
}
</style>
