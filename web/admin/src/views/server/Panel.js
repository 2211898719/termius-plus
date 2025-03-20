import {defineComponent} from "vue";

export const Panel = defineComponent({
    inject: [],
    name: 'Panel',
    props: {
        params: {
            type: Object,
            required: true,
        },
    },
    data() {
        return {
            title: '',
            message:  'not found',
        };
    },
    mounted() {
        const disposable = this.params.api.onDidTitleChange(() => {
            this.title = this.params.api.title;
        });

        this.title = this.params.api.title;

        return () => {
            disposable.dispose();
        };
    },
    template: `
    <div style="height:100%; color:red;">
      Hello World
      <div>{{title}}</div>
      <div>{{message}}</div>
    </div>`,
});