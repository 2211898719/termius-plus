import * as echarts from 'echarts/core'
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { LineChart} from 'echarts/charts'
import { UniversalTransition } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
    GridComponent,
    TooltipComponent,
    LegendComponent,
    TitleComponent,
    LineChart,
    CanvasRenderer,
    UniversalTransition,
])
// import * as echarts from 'echarts'

// 初始化语法糖
const draw = (dom, option) => {
    const chart = echarts.init(dom)
    chart.clear()
    chart.setOption(option)
    return chart
}

export default {
    ...echarts,
    draw
}
