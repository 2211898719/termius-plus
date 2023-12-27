import { reactive, ref, onMounted } from 'vue';
import {onBeforeRouteUpdate} from 'vue-router';
import _ from "lodash";

export default function usePaginationQuery(router, searchForm, searchMethod) {

    const rows = ref([]);
    const sort = ref('');

    const pagination = reactive({
        current: 1,
        pageSize: 20,
        total: 0,
        pageSizeOptions: ['10', '20', '50', '100'],
        showSizeChanger: true,
        showTotal: (total) => `共 ${total} 条`,
        showQuickJumper: true,
    });

    const pullQueryParams = (query) => {
        for (let key in searchForm) {
            searchForm[key] = query[key] ? query[key] : '';
        }

        if (query.page) {
            pagination.current = _.toInteger(query.page);
        }

        if (query.size) {
            pagination.pageSize = _.toInteger(query.size);
        }

        if (query.sort) {
            sort.value = query.sort;
        }
    }

    const buildQueryParams = () => {
        return {
            ...searchForm,
            page: pagination.current - 1, // 后端接口 page 从 0 开始计数
            size: pagination.pageSize,
            sort: sort.value,
        }
    }

    const fetchPaginationData = async () => {
        const response = await searchMethod(buildQueryParams());
        rows.value = response.data;
        pagination.total = response.total;
    }

    const onPaginationChange = async (page, filters, sorter) => {
        console.log("onPaginationChange", page, filters, sorter);
        let sortParams;
        if (sorter.field && sorter.order) {
            sort.value = sorter.field + ',' + (sorter.order === 'ascend' ? 'asc' : 'desc');
        } else {
            sort.value = '';
        }

        if (router) {
            const route = router.currentRoute.value;
            const query = _.assign({}, route.query, {page: page.current, size: page.pageSize}, {sort: sort.value});
            await router.push({name: route.name, query: query});
        } else {
            pagination.current = page.current;
            pagination.pageSize = page.pageSize;
            await fetchPaginationData();
        }
    }

    const onSearchSubmit = async () => {
        pagination.current = 1;

        if (router) {
            const route = router.currentRoute.value;
            const query = _.assign({}, searchForm, {page: _.toString(pagination.current), size: _.toString(pagination.pageSize)});

            if (_.isEqual(route.query, query)) {
                await fetchPaginationData();
            } else {
                await router.push({name: route.name, query: query});
            }
        } else {
            await fetchPaginationData();
        }
    };

    onMounted(async () => {
        if (router) {
            pullQueryParams(router.currentRoute.value.query);
        }
        await fetchPaginationData();
    });

    onBeforeRouteUpdate(async (to, from, next) => {
        next();
        pullQueryParams(to.query);
        await fetchPaginationData();
    });

    return {
        rows,
        pagination,
        fetchPaginationData,
        onPaginationChange,
        onSearchSubmit,
        pullQueryParams,
    }
}
