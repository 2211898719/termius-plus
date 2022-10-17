module.exports = {
    base: '/docs/',
    title: 'Java项目脚手架开发文档',
    description: 'Java项目脚手架开发文档！',
    themeConfig: {
        nav: [
            { text: '项目开发', link: '/dev/' },
            { text: '编程指南', link: '/guide/' },
        ],
        sidebar: {
            '/dev/': [
                '',
            ],

            '/guide/': [
                '3-tier-architecture',
                'develop-web-layer',
                'develop-service-layer',
                'develop-repository-layer',
                'api-style',
                'validation',
            ]
        }
    }
}
