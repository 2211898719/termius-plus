<template>
  <div ref="outerWrapper" :class="wrapperClasses">
    <div v-if="isHorizontal" :class="`${prefix}-horizontal`">
      <div :style="{right: `${anotherOffset}%`}" class="left-pane" :class="paneClasses">
        <slot name="left"/>
      </div>
      <div :class="`${prefix}-trigger-con`" :style="{left: `${offset}%`}" @mousedown="handleMousedown">
        <slot name="trigger">
          <trigger mode="vertical"/>
        </slot>
      </div>
      <div :style="{left: `${offset}%`}" class="right-pane" :class="paneClasses">
        <slot name="right"/>
      </div>
    </div>
    <div v-else :class="`${prefix}-vertical`">
      <div :style="{bottom: `${anotherOffset}%`}" class="top-pane" :class="paneClasses">
        <slot name="top"/>
      </div>
      <div :class="`${prefix}-trigger-con`" :style="{top: `${offset}%`}" @mousedown="handleMousedown">
        <slot name="trigger">
          <trigger mode="horizontal"/>
        </slot>
      </div>
      <div :style="{top: `${offset}%`}" class="bottom-pane" :class="paneClasses">
        <slot name="bottom"/>
      </div>
    </div>
  </div>
</template>
<script>
import {nextTick} from 'vue';
import {on, off, oneOf} from './utils';
import Trigger from './trigger.vue';


export default {
  name: 'Split',
  components: {Trigger},
  emits: ['update:modelValue', 'on-move-start', 'on-moving', 'on-move-end'],
  props: {
    modelValue: {
      type: [Number, String],
      default: 0.5
    },
    mode: {
      validator(value) {
        return oneOf(value, ['horizontal', 'vertical']);
      },
      default: 'horizontal'
    },
    min: {
      type: [Number, String],
      default: '40px'
    },
    max: {
      type: [Number, String],
      default: '40px'
    }
  },
  /**
   * Events
   * @on-move-start
   * @on-moving 返回值：事件对象，但是在事件对象中加入了两个参数：atMin(当前是否在最小值处), atMax(当前是否在最大值处)
   * @on-move-end
   */
  data() {
    return {
      prefix: 'ivu-split',
      offset: 0,
      oldOffset: 0,
      isMoving: false,
      computedMin: 0,
      computedMax: 0,
      currentValue: 0.5
    };
  },
  computed: {
    wrapperClasses() {
      return [
        `${this.prefix}-wrapper`,
        this.isMoving ? 'no-select' : ''
      ];
    },
    paneClasses() {
      return [
        `${this.prefix}-pane`,
        {
          [`${this.prefix}-pane-moving`]: this.isMoving
        }
      ];
    },
    isHorizontal() {
      return this.mode === 'horizontal';
    },
    anotherOffset() {
      return 100 - this.offset;
    },
    valueIsPx() {
      return typeof this.modelValue === 'string';
    },
    offsetSize() {
      return this.isHorizontal ? 'offsetWidth' : 'offsetHeight';
    }
  },
  methods: {
    px2percent(numerator, denominator) {
      return parseFloat(numerator) / parseFloat(denominator);
    },
    getComputedThresholdValue(type) {
      let size = this.$refs.outerWrapper[this.offsetSize];
      if (this.valueIsPx) return typeof this[type] === 'string' ? this[type] : size * this[type];
      else return typeof this[type] === 'string' ? this.px2percent(this[type], size) : this[type];
    },
    getMin(value1, value2) {
      if (this.valueIsPx) return `${Math.min(parseFloat(value1), parseFloat(value2))}px`;
      else return Math.min(value1, value2);
    },
    getMax(value1, value2) {
      if (this.valueIsPx) return `${Math.max(parseFloat(value1), parseFloat(value2))}px`;
      else return Math.max(value1, value2);
    },
    getAnotherOffset(value) {
      let res = 0;
      if (this.valueIsPx) res = `${this.$refs.outerWrapper[this.offsetSize] - parseFloat(value)}px`;
      else res = 1 - value;
      return res;
    },
    handleMove(e) {
      let pageOffset = this.isHorizontal ? e.pageX : e.pageY;
      let offset = pageOffset - this.initOffset;
      let outerWidth = this.$refs.outerWrapper[this.offsetSize];
      let value = this.valueIsPx ? `${parseFloat(this.oldOffset) + offset}px` : (this.px2percent(outerWidth * this.oldOffset + offset, outerWidth));
      let anotherValue = this.getAnotherOffset(value);
      if (parseFloat(value) <= parseFloat(this.computedMin)) value = this.getMax(value, this.computedMin);
      if (parseFloat(anotherValue) <= parseFloat(this.computedMax)) value = this.getAnotherOffset(this.getMax(anotherValue, this.computedMax));
      e.atMin = this.modelValue === this.computedMin;
      e.atMax = this.valueIsPx ? this.getAnotherOffset(this.modelValue) === this.computedMax : this.getAnotherOffset(this.modelValue).toFixed(5) === this.computedMax.toFixed(5);
      this.$emit('update:modelValue', value);
      this.$emit('on-moving', e);
    },
    handleUp() {
      this.isMoving = false;
      off(document, 'mousemove', this.handleMove);
      off(document, 'mouseup', this.handleUp);
      this.$emit('on-move-end');
    },
    handleMousedown(e) {
      this.initOffset = this.isHorizontal ? e.pageX : e.pageY;
      this.oldOffset = this.modelValue;
      this.isMoving = true;
      on(document, 'mousemove', this.handleMove);
      on(document, 'mouseup', this.handleUp);
      this.$emit('on-move-start');
    },
    computeOffset() {
      nextTick(() => {
        this.computedMin = this.getComputedThresholdValue('min');
        this.computedMax = this.getComputedThresholdValue('max');
        // https://github.com/view-design/ViewUI/commit/d827b6405c365b9b7c130448f509724564cad8c1
        // todo 这里对 px 没有适配，先还原
        this.offset = (this.valueIsPx ? this.px2percent(this.modelValue, this.$refs.outerWrapper[this.offsetSize]) : this.modelValue) * 10000 / 100;
      });
    }
  },
  watch: {
    modelValue(val) {
      if (val !== this.currentValue) {
        this.currentValue = val;
        this.computeOffset();
      }
    }
  },
  mounted() {
    nextTick(() => {
      this.computeOffset();
    });

    on(window, 'resize', this.computeOffset);
  },
  beforeUnmount() {
    off(window, 'resize', this.computeOffset);
  }
};
</script>

<style scoped lang="less">
@split-prefix-cls: ~"ivu-split";
@box-shadow: 0 0 4px 0 rgba(28, 36, 56, 0.4);
@trigger-bar-background: rgba(23, 35, 61, 0.25);
@trigger-background: #f8f8f9;
@trigger-width: 6px;
@trigger-bar-width: 4px;
@trigger-bar-offset: (@trigger-width - @trigger-bar-width) / 2;
@trigger-bar-interval: 3px;
@trigger-bar-weight: 1px;
@trigger-bar-con-height: (@trigger-bar-weight + @trigger-bar-interval) * 8;

.@{split-prefix-cls} {
  &-wrapper {
    position: relative;
    width: 100%;
    height: 100%;
  }
  &-pane {
    position: absolute;
    &.left-pane, &.right-pane {
      top: 0;
      bottom: 0;
    }
    &.left-pane {
      left: 0;
    }
    &.right-pane {
      right: 0;
    }
    &.top-pane, &.bottom-pane {
      left: 0;
      right: 0;
    }
    &.top-pane {
      top: 0;
    }
    &.bottom-pane {
      bottom: 0;
    }

    &-moving{
      -webkit-user-select: none;
      -moz-user-select: none;
      -ms-user-select: none;
      user-select: none;
    }
  }
  &-trigger {
    border: 1px solid @border-color-base;
    &-con {
      position: absolute;
      transform: translate(-50%, -50%);
      z-index: 10;
    }
    &-bar-con {
      position: absolute;
      overflow: hidden;
      &.vertical {
        left: @trigger-bar-offset;
        top: 50%;
        height: @trigger-bar-con-height;
        transform: translate(0, -50%);
      }
      &.horizontal {
        left: 50%;
        top: @trigger-bar-offset;
        width: @trigger-bar-con-height;
        transform: translate(-50%, 0);
      }
    }
    &-vertical {
      width: @trigger-width;
      height: 100%;
      background: @trigger-background;
      border-top: none;
      border-bottom: none;
      cursor: col-resize;
      .@{split-prefix-cls}-trigger-bar {
        width: @trigger-bar-width;
        height: 1px;
        background: @trigger-bar-background;
        float: left;
        margin-top: @trigger-bar-interval;
      }
    }
    &-horizontal {
      height: @trigger-width;
      width: 100%;
      background: @trigger-background;
      border-left: none;
      border-right: none;
      cursor: row-resize;
      .@{split-prefix-cls}-trigger-bar {
        height: @trigger-bar-width;
        width: 1px;
        background: @trigger-bar-background;
        float: left;
        margin-right: @trigger-bar-interval;
      }
    }
  }
  &-horizontal {
    > .@{split-prefix-cls}-trigger-con {
      top: 50%;
      height: 100%;
      width: 0;
    }
  }
  &-vertical {
    > .@{split-prefix-cls}-trigger-con {
      left: 50%;
      height: 0;
      width: 100%;
    }
  }
  .no-select {
    -webkit-touch-callout: none;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
    user-select: none;
  }
}
</style>