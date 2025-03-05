import Enum from "./Enum";


export default class PortForWardingStatusEnum extends Enum {
    static START = new PortForWardingStatusEnum('运行中', 'START');
    static STOP = new PortForWardingStatusEnum('停止', 'STOP');

}

