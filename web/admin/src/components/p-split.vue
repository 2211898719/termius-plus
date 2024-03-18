<script setup>
import {h, onMounted, render} from "vue";
import {GoldenLayout} from "golden-layout";
import PTerm from "@/components/p-term.vue";

class MyComponent {
  rootElement;

  constructor(container) {
    this.rootElement = container.element;
    let pTerm = h(PTerm, {server: {id: 79}})
    this.rootElement.appendChild(vNode2dom(pTerm));
    this.resizeWithContainerAutomatically = true;
  }
}


function vNode2dom(vNode) {
  let domEl = document.createElement("div");
  render(vNode, domEl);
  return domEl.firstElementChild;
}

onMounted(() => {
  const myLayout = {
    root: {
      type: 'row',
      content: [
        {
          title: 'My Component 1',
          type: 'component',
          componentType: 'MyComponent',
          width: 50,
        },
        {
          title: 'My Component 2',
          type: 'component',
          componentType: 'MyComponent',
          componentState: {text: 'Component 2'}
        }
      ]
    }
  };

  const menuContainerElement = document.querySelector('#menuContainer');
  const addMenuItemElement = document.querySelector('#addMenuItem');
  const dragMenuItem = document.querySelector('#dragMenuItem');
  const layoutElement = document.querySelector('#layoutContainer');

  addMenuItemElement.addEventListener('click', (event) => {
    goldenLayout.addComponent('MyComponent', undefined, 'Added Component');
  });

  const goldenLayout = new GoldenLayout(layoutElement);

  goldenLayout.registerComponentConstructor('MyComponent', MyComponent);

  var newItemConfig = {
    title: "11111",
    type: 'component',
    componentType: 'MyComponent',
    componentState: {text: "3333"}
  };

  goldenLayout.newDragSource(dragMenuItem, () => newItemConfig);

  goldenLayout.loadLayout(myLayout);
})

</script>

<template>
  <div id="wrapper" class="wrapper">
    <ul id="menuContainer">
      <li id="addMenuItem">Add another component</li>
      <li id="dragMenuItem">dragMenuItem another component</li>
    </ul>
    <div id="layoutContainer"></div>
  </div>
</template>

<style lang="less">
@import 'golden-layout/dist/less/goldenlayout-base.less';
@import 'golden-layout/dist/less/themes/goldenlayout-dark-theme.less';

.wrapper {
  height: calc(100vh - 100px) !important;
}

h2 {
  font: 14px Arial, sans-serif;
  color: #fff;
  padding: 10px;
  text-align: center;
}

html, body {
  height: 100%;
}

* {
  margin: 0;
  padding: 0;
  list-style-type: none;
}

#wrapper {
  display: flex;
  height: 100%
}

#menuContainer {
  flex: 0 0 auto;
  margin-right: 3px;
}

#menuContainer li {
  border-bottom: 1px solid #000;
  border-top: 1px solid #333;
  cursor: pointer;
  padding: 10px 10px;
  color: #BBB;
  background: #1a1a1a;
  font: 12px Arial, sans-serif;
}

#menuContainer li:hover {
  background: #111;
  color: #CCC;
}

#layoutContainer {
  flex: 1 1 auto;
  height: 100%;
}
</style>
