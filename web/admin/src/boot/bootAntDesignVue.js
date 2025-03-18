import '@ant-design-vue/pro-layout/dist/style.less';

import ProLayout, {PageContainer} from '@ant-design-vue/pro-layout';
import {
    Button,
    Card,
    Form,
    Input,
    Space,
    Typography,
    Table,
    Divider,
    Dropdown,
    Menu,
    Drawer,
    Tabs,
    Descriptions,
    List,
    Breadcrumb,
    Skeleton,
    Avatar,
    Spin,
    Switch,
    Popconfirm,
    Select,
    Image,
    PageHeader,
    Popover,
    Modal,
    Collapse,
    Cascader,
    Tag,
    InputNumber,
    Pagination,
    Result,
    Badge,
    Tooltip,
    AutoComplete,
    DatePicker,
    TimePicker,
    Slider,
    Tree,
    Empty,
    Progress,
    Statistic,
    Alert
} from 'ant-design-vue';

import icons from '@/icons';

export function bootAntDesignVue(app) {

    app
        .use(ProLayout)
        .use(PageContainer)
        .use(icons)
    ;

    app
        .use(Button)
        .use(Statistic)
        .use(Tree)
        .use(Alert)
        .use(Slider)
        .use(InputNumber)
        .use(Card)
        .use(Form)
        .use(Input)
        .use(Empty)
        .use(Space)
        .use(Typography)
        .use(Table)
        .use(Divider)
        .use(Dropdown)
        .use(Menu)
        .use(DatePicker)
        .use(Drawer)
        .use(Tabs)
        .use(Descriptions)
        .use(List)
        .use(Breadcrumb)
        .use(Skeleton)
        .use(Avatar)
        .use(Spin)
        .use(Switch)
        .use(Popconfirm)
        .use(Result)
        .use(Select)
        .use(Image)
        .use(Popover)
        .use(PageHeader)
        .use(Modal)
        .use(Cascader)
        .use(Collapse)
        .use(Tag)
        .use(Pagination)
        .use(Badge)
        .use(Tooltip)
        .use(AutoComplete)
        .use(TimePicker)
        .use(Progress)
    ;
}
