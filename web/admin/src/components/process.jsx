import {Progress} from 'ant-design-vue'

export const uploadFileProcess = (sourceData, data, speedStr, take, left) => {


    return <div>
        <p>{sourceData.fileName}</p>
        <p><Progress percent={Math.floor(data.progress / sourceData.fileSize * 100)} status="active"/></p>
        <p>速度：{speedStr}</p>
        <p>耗时：{formatSeconds(take)}</p>
        <p>预计剩余时间：{formatSeconds(Math.floor(left))}</p>
    </div>
}

//秒转可读时间 例如  100秒  转换为  1分40秒。需要支持天、小时、分、秒
function formatSeconds(seconds) {
    let day = Math.floor(seconds / (60 * 60 * 24));
    let hour = Math.floor(seconds / (60 * 60)) - (day * 24);
    let minute = Math.floor(seconds / 60) - (day * 24 * 60) - (hour * 60);
    let second = Math.floor(seconds) - (day * 24 * 60 * 60) - (hour * 60 * 60) - (minute * 60);
    let result = '';
    if (day) {
        result += day + '天';
    }
    if (hour) {
        result += hour + '小时';
    }
    if (minute) {
        result += minute + '分';
    }
    if (second) {
        result += second + '秒';
    }
    return result;
}
