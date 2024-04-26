import Enum from "./Enum";


export default class MethodEnum extends Enum {
    static POST = new MethodEnum('POST', 'POST');
    static GET = new MethodEnum('GET', 'GET');
    static PUT = new MethodEnum('PUT', 'PUT');
    static DELETE = new MethodEnum('DELETE', 'DELETE');
    static PATCH = new MethodEnum('PATCH', 'PATCH');
    static OPTIONS = new MethodEnum('OPTIONS', 'OPTIONS');
    static HEAD = new MethodEnum('HEAD', 'HEAD');

}

