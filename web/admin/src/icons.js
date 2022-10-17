import * as Icons from '@ant-design/icons-vue/es';

export const filterIcons = ['default', 'createFromIconfontCN', 'getTwoToneColor', 'setTwoToneColor'];

export default (app) => {
    const allIcon = Icons;
    Object.keys(Icons)
        .filter((k) => !filterIcons.includes(k))
        .forEach((k) => {
            app.component(allIcon[k].displayName, allIcon[k]);
        });
};