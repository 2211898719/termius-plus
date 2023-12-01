#添加一个备注字段，可以填写富文本
alter table `server`
    add `remark` longtext default null comment '备注';

