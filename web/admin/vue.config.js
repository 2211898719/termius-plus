const {defineConfig} = require('@vue/cli-service');
const path = require('path');
const appConfig = require('./src/config');
const CopyWebpackPlugin = require('copy-webpack-plugin')
const { codeInspectorPlugin } = require('code-inspector-plugin');

module.exports = defineConfig({
    transpileDependencies: true,
    assetsDir: "assets",
    publicPath: "/",
    lintOnSave: false,
    devServer: {
        port: 8081,
        proxy: {
            '/api-admin': {
                target: process.env.VUE_APP_PROXY_FOR_API,
                changeOrigin: true,
            },
            '/socket': {
                target: process.env.VUE_APP_PROXY_FOR_API,
                changeOrigin: true,
                ws: true,
            }
        },
        client: {
            overlay: false
        }
    },
    chainWebpack: config => {
        config
            .plugin('html')
            .tap(args => {
                args[0].title = appConfig.name;
                return args;
            })
        config.resolve.alias
            .set('@shared', path.join(__dirname, '../shared'));

        config.plugin('code-inspector-plugin').use(
            codeInspectorPlugin({
                bundler: 'webpack',
            })
        );

        config.module
            .rule('jsx')
            .test(/\.jsx$/)
            .use('babel-loader')
            .loader('babel-loader')
            .end()
            .rule('vue')
            .test(/\.vue$/)
            .use('babel-loader')
            .loader('babel-loader')
            .end()


    },
    css: {
        loaderOptions: {
            less: {
                //全局引入less文件
                additionalData: `@import "@/layouts/main.less";`,
                lessOptions: {
                    javascriptEnabled: true,
                    modifyVars: {
                        "primary-color": "#1FB568",
                        "link-color": "#1FB568",
                        "success-color": "#52c41a",
                        "warning-color": "#faad14",
                        "error-color": "#f5222d",
                        "font-size-base": "14px",
                        "heading-color": "#1E2033",
                        "text-color":" rgba(0, 0, 0, 0.65)",
                        "text-color-secondary": "rgba(0, 0, 0, 0.45)",
                        "disabled-color": "rgba(0, 0, 0, 0.25)",
                        "border-radius-base": "2px" ,
                        "border-color-base": "#d9d9d9",
                        "box-shadow-base": "0 2px 8px rgba(0, 0, 0, 0.15)",
                    },
                },
            },
        },
    }
})
