import '@ant-design-vue/pro-layout/dist/style.less';
import 'ant-design-vue/dist/antd.css';
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
    Cascader
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
        .use(Card)
        .use(Form)
        .use(Input)
        .use(Space)
        .use(Typography)
        .use(Table)
        .use(Divider)
        .use(Dropdown)
        .use(Menu)
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
        .use(Select)
        .use(Image)
        .use(Popover)
        .use(PageHeader)
        .use(Modal)
        .use(Cascader)
        .use(Collapse)
    ;
}
