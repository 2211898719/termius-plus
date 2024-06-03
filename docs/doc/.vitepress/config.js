export default {
    // 应用级配置选项
    lang: 'en-US',
    title: 'Termius Plus',
    base: '/termius-plus/',
    description: '团队运维工具',
    themeConfig: {
        nav: [
            { text: 'gitee', link: 'https://gitee.com/zimehjl/termius-plus' },
            { text: 'github', link: 'https://github.com/2211898719/termius-plus' }
        ],
        sidebar: [
            {
                text: '介绍',
                items: [
                    { text: '功能介绍', link: '/info' },
                ]
            },
            {
                text: '安装文档',
                items: [
                    { text: 'Docker安装', link: '/start' },
                ]
            },
            {
                text: '开发文档',
                items: [
                    { text: '架构', link: '/feature' },
                ]
            }
        ]
    }
}
