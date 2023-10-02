import Enum from "./Enum";


export default class ProxyTypeEnum extends Enum {
    static HTTP = new ProxyTypeEnum('HTTP', 'HTTP');
    static SOCKET5 = new ProxyTypeEnum('SOCKET5', 'SOCKET5');
    static SOCKET4 = new ProxyTypeEnum('SOCKET4', 'SOCKET4');

}

