import _ from 'lodash';

export const findNodePath = (tree, nodeKey, fieldNames = {
    children: "children",
    value: "id",
    label: "name"
}) => {
    let path = [];
    let find = (treeData, id) => {
        for (let i = 0; i < treeData.length; i++) {
            if (treeData[i][fieldNames.value] === id) {
                path.push(treeData[i][fieldNames.value]);
                return true;
            }

            if (treeData[i][fieldNames.children] && find(treeData[i][fieldNames.children], id)) {
                path.push(treeData[i][fieldNames.value]);
                return true;
            }
        }
        return false;
    };

    find(tree, nodeKey);
    return path.reverse();
};

export const maxDeep = (tree, nodeKey, fieldNames = {
    children: "children",
    value: "id",
    label: "name"
}) => {
    if (tree === null) {
        return 0;
    }

    if (!tree[fieldNames.children]) {
        return 1;
    }

    return 1 + _.max(tree[fieldNames.children].map(child => maxDeep(child, nodeKey, fieldNames)));
}

export const walk = (treeNodes, callback, deep = 0, childFieldName = 'children') => {
    treeNodes.forEach(node => {
        callback(node, deep);
        if (node[childFieldName]) {
            walk(node[childFieldName], callback, deep + 1, childFieldName);
        }
    })
}

export const maxDeepList = (treeList, nodeKey, fieldNames = {
    children: "children",
    value: "id",
    label: "name"
}) => {
    if (treeList === null) {
        return 0;
    }

    return _.max(treeList.map(tree => maxDeep(tree, nodeKey, fieldNames)));
}
