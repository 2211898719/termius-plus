const {defineConfig} = require('@vue/cli-service');
const path = require('path');
const appConfig = require('./src/config');

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
    },
    css: {
        loaderOptions: {
            less: {
                lessOptions: {
                    javascriptEnabled: true,
                },
            },
        },
    },
})
