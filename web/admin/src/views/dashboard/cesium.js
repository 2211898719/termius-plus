// 初始化cesium地图 JS文件
// 首先获取Cesium API
const Cesium = window.Cesium;
let viewer = null;
/**
 * 初始化地球视图函数
 */
function initCesiumMap(dom) {
    // 配置cesium专属Access Tokens,就是cesium的访问令牌，每一个使用cesium的用户都需要自己注册，然后获取自己的Access Tokens；
    Cesium.Ion.defaultAccessToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiIxZWFlYjAyYS0xN2JlLTQ0OTItOGNkOC05YWJlNGY0MjI2NmQiLCJpZCI6NDkyMjYsImlhdCI6MTYxNzM0NjA3N30.crkTg0Logk_JUA7BROy0r9RqTJWCi8NZpTyu4qI11Fo'
    viewer = new Cesium.Viewer(dom, {
        animation: false, // 是否显示动画控件
        baseLayerPicker: true, // 是否显示图层选择控件
        vrButton: false, // 是否显示VR控件
        geocoder: true, // 是否显示地名查找控件
        timeline: false, // 是否显示时间线控件
        sceneModePicker: false, // 是否显示投影方式控件
        navigationHelpButton: false, // 是否显示帮助信息控件
        navigationInstructionsInitiallyVisible: true, // 帮助按钮，初始化的时候是否展开
        infoBox: false, // 是否显示点击要素之后显示的信息
        fullscreenButton: true, // 是否显示全屏按钮
        selectionIndicator: true, // 是否显示选中指示框
        homeButton: false, // 是否显示返回主视角控件
        scene3DOnly: true, // 如果设置为true，则所有几何图形以3D模式绘制以节约GPU资源
        // terrainProvider: new Cesium.EllipsoidTerrainProvider({}) // 不显示地形
    })

    // 去掉logo
    viewer.cesiumWidget.creditContainer.style.display = "none";

    function add(lon1, lat1, lon2, lat2) {
        console.log(lon1, lat1, lon2, lat2)
        // 定义两个经纬度坐标
        const point1 = Cesium.Cartesian3.fromDegrees(lon1, lat1); // 北京
        const point2 = Cesium.Cartesian3.fromDegrees(lon2, lat2); // 上海

        // 绘制线段
        let entity = viewer.entities.add({
            polyline: {
                positions: [point1, point2],
                width: 3,
                material: Cesium.Color.RED
            }
        });

        // 流星效果
        const meteorEntity = viewer.entities.add({
            position: point1,
            point: {
                pixelSize: 10,
                color: Cesium.Color.YELLOW,
                outlineColor: Cesium.Color.BLACK,
                outlineWidth: 3,
            }
        });

        let startTime = new Cesium.JulianDate();
        let endTime = new Cesium.JulianDate();

        // 动画更新
        function updateMeteor() {
            const currentTime = Cesium.JulianDate.now();
            const elapsedTime = Cesium.JulianDate.secondsDifference(currentTime, startTime);

            // 计算流星位置
            const t = elapsedTime / 2; // 调整速度
            const position = Cesium.Cartesian3.lerp(point1, point2, t, new Cesium.Cartesian3());

            if (t < 1) {
                meteorEntity.position = position;
                requestAnimationFrame(updateMeteor);
            } else {
                // startTime = Cesium.JulianDate.now();
                // requestAnimationFrame(updateMeteor);

                //删除线和动画
                viewer.entities.remove(meteorEntity);
                viewer.entities.remove(entity);
            }
        }

        // 开始动画
        startTime = Cesium.JulianDate.now();
        updateMeteor();

        // viewer.zoomTo(viewer.entities);
    }

    viewer.dataSources.add(
        Cesium.GeoJsonDataSource.load(
            "https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json"
        )
    );

    return add
}

// 导出
export { Cesium, viewer, initCesiumMap };
