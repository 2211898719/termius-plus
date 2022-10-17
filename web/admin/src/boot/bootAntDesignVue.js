import '@ant-design-vue/pro-layout/dist/style.less';

import ProLayout, { PageContainer } from '@ant-design-vue/pro-layout';
import { Button, Card, Form, Input, Space, Typography, Table, Divider, Dropdown, Menu, Drawer, Tabs, Descriptions } from 'ant-design-vue';
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
    ;
}
