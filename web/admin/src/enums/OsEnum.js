import Enum from "./Enum";


export default class OsEnum extends Enum {
    static LINUX = new OsEnum('Linux', 'LINUX');
    static WINDOWS = new OsEnum('Windows', 'WINDOWS');

}

