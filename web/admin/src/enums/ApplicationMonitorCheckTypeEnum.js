import Enum from "./Enum";


export default class ApplicationMonitorCheckTypeEnum extends Enum {
    static REGEX = new ApplicationMonitorCheckTypeEnum('正则表达式', 'REGEX');
    static JAVASCRIPT = new ApplicationMonitorCheckTypeEnum('JavaScript脚本', 'JAVASCRIPT');
}

