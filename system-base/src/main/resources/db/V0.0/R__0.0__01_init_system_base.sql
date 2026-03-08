-- 01_创建数据库表/t_user_right.sql
--  --------------------------------------------------
-- 需求名称：数据库设计
-- 版本：system_base_init
-- 作者：teddy
-- 说明：
--  --------------------------------------------------

create schema IF NOT EXISTS system_base;
alter schema system_base owner to ${flyway:user};



set client_encoding = UTF8;
set search_path to system_base;
commit;


CREATE TABLE IF NOT EXISTS ${flyway:database}.system_base.t_user_right (
    c_id             varchar(300)     NULL     ,        --主键
    c_userid         varchar(300)     NULL     ,        --用户ID
    c_type           varchar(300)     NULL     ,        --类型
    c_roleid         varchar(300)     NULL     ,        --角色ID
    c_rightkey       varchar(300)     NULL     ,        --权限key
    constraint pk_user_right primary key(c_id)
    );

commit;

COMMENT ON TABLE  ${flyway:database}.system_base.t_user_right is '用户权限';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user_right.c_id  is '主键';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user_right.c_userid  is '用户ID';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user_right.c_type  is '类型';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user_right.c_roleid  is '角色ID';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user_right.c_rightkey  is '权限key';
commit;


-- 01_创建数据库表/t_right.sql
--  --------------------------------------------------
-- 需求名称：数据库设计
-- 版本：system_base_init
-- 作者：teddy
-- 说明：
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to system_base;
commit;


CREATE TABLE IF NOT EXISTS ${flyway:database}.system_base.t_right (
    c_rightkey       varchar(300)     NULL     ,        --权限KEY
    c_name           varchar(300)     NULL     ,        --名称
    c_descript       varchar(300)     NULL     ,        --描述
    n_order          int4             NULL     ,        --序号
    constraint pk_right primary key(c_rightkey)
    );

commit;

COMMENT ON TABLE  ${flyway:database}.system_base.t_right is '权限定义';
COMMENT ON COLUMN ${flyway:database}.system_base.t_right.c_rightkey  is '权限KEY';
COMMENT ON COLUMN ${flyway:database}.system_base.t_right.c_name  is '名称';
COMMENT ON COLUMN ${flyway:database}.system_base.t_right.c_descript  is '描述';
COMMENT ON COLUMN ${flyway:database}.system_base.t_right.n_order  is '序号';
commit;


-- 01_创建数据库表/t_user.sql
--  --------------------------------------------------
-- 需求名称：数据库设计
-- 版本：system_base_init
-- 作者：teddy
-- 说明：
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to system_base;
commit;


CREATE TABLE IF NOT EXISTS ${flyway:database}.system_base.t_user (
    c_id             varchar(300)     NULL     ,        --主键
    n_external_nid   bigint           NULL     ,        --外部租户映射,
    c_external_cid   varchar(100)     NULL     ,        --外部租户映射,
    c_loginid        varchar(300)     NULL     ,        --登录名
    c_name           varchar(300)     NULL     ,        --姓名
    c_password       varchar(300)     NULL     ,        --密码
    c_mail           varchar(300)     NULL     ,        --邮箱
    c_ip             varchar(300)     NULL     ,        --IP地址
    c_tenant           varchar(300)     NULL     ,        --所属租户
    c_dept           varchar(300)     NULL     ,        --所属部门
    c_valid          varchar(300)     NULL     ,        --是否有效
    n_order          int4             NULL     ,        --序号
    c_type           varchar(300)     NULL     ,        --类型
    j_role_id        json             NULL     ,        -- 拥有的角色,
    lc_ext           text             NULL     ,        --扩展字段
    dt_create_time		 timestamp		  NULL	   ,		--创建时间
    dt_last_modify	 timestamp		  NULL	   ,		--最后修改时间
    constraint pk_user primary key(c_id)
    );

commit;

COMMENT ON TABLE  ${flyway:database}.system_base.t_user is '用户';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.c_id  is '主键';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.n_external_nid  is '外部租户映射';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.c_external_cid  is '外部租户映射';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.c_loginid  is '登录名';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.c_name  is '姓名';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.c_password  is '密码';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.c_mail  is '邮箱';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.c_ip  is 'IP地址';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.c_tenant  is '所属租户';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.c_dept  is '所属部门';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.c_valid  is '是否有效';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.n_order  is '序号';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.c_type  is '类型';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.j_role_id  is '拥有的角色';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.lc_ext  is '扩展字段';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.dt_create_time  is '创建时间';
COMMENT ON COLUMN ${flyway:database}.system_base.t_user.dt_last_modify  is '最后修改时间';
commit;


-- 01_创建数据库表/t_dept.sql
--  --------------------------------------------------
-- 需求名称：数据库设计
-- 版本：system_base_init
-- 作者：teddy
-- 说明：
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to system_base;
commit;


CREATE TABLE IF NOT EXISTS ${flyway:database}.system_base.t_dept (
    c_id             varchar(300)     NULL     ,        --主键
    n_external_nid   bigint           NULL     ,        --外部租户映射,
    c_external_cid   varchar(100)     NULL     ,        --外部租户映射,
    c_name           varchar(300)     NULL     ,        --名称
    c_pid            varchar(300)     NULL     ,        --父ID
    c_tenant           varchar(300)     NULL     ,        --所属租户
    c_alias          varchar(300)     NULL     ,        --别名
    c_valid          varchar(300)     NULL     ,        --是否有效
    n_order          int4             NULL     ,        --序号
    lc_ext           text             NULL     ,        --扩展字段
    dt_create_time		 timestamp		  NULL	   ,		--创建时间
    dt_last_modify	 timestamp		  NULL	   ,		--最后修改时间
    constraint pk_dept primary key(c_id)
    );

commit;

COMMENT ON TABLE  ${flyway:database}.system_base.t_dept is '部门';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.c_id  is '主键';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.n_external_nid  is '外部租户映射';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.c_external_cid  is '外部租户映射';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.c_name  is '名称';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.c_pid  is '父ID';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.c_tenant  is '所属租户';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.c_alias  is '别名';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.c_valid  is '是否有效';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.n_order  is '序号';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.lc_ext  is '扩展字段';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.dt_create_time  is '创建时间';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dept.dt_last_modify  is '最后修改时间';
commit;


-- 01_创建数据库表/t_tenant.sql
--  --------------------------------------------------
-- 需求名称：数据库设计
-- 版本：system_base_init
-- 作者：teddy
-- 说明：
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to system_base;
commit;


CREATE TABLE IF NOT EXISTS ${flyway:database}.system_base.t_tenant (
    c_id             varchar(300)     NULL     ,        --主键
    n_external_nid   bigint           NULL     ,        --外部租户映射,
    c_external_cid   varchar(100)     NULL     ,        --外部租户映射,
    c_name           varchar(300)     NULL     ,        --名称
    c_pid            varchar(300)     NULL     ,        --父ID
    n_level          int4             NULL     ,        --层级
    c_alias          varchar(300)     NULL     ,        --简称
    c_valid          varchar(300)     NULL     ,        --是否有效
    n_order          int4             NULL     ,        --序号
    lc_ext           text             NULL     ,        --扩展字段
    dt_create_time		 timestamp		  NULL	   ,		--创建时间
    dt_last_modify	 timestamp		  NULL	   ,		--最后修改时间
    constraint pk_tenant primary key(c_id)
    );

commit;

COMMENT ON TABLE  ${flyway:database}.system_base.t_tenant is '租户';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.c_id  is '主键';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.n_external_nid  is '外部租户映射';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.c_external_cid  is '外部租户映射';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.c_name  is '名称';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.c_pid  is '父ID';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.n_level  is '层级';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.c_alias  is '简称';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.c_valid  is '是否有效';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.n_order  is '序号';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.lc_ext  is '扩展字段';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.dt_create_time  is '创建时间';
COMMENT ON COLUMN ${flyway:database}.system_base.t_tenant.dt_last_modify  is '最后修改时间';
commit;


-- 01_创建数据库表/t_code.sql
--  --------------------------------------------------
-- 需求名称：数据库设计
-- 版本：system_base_init
-- 作者：teddy
-- 说明：
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to system_base;
commit;


CREATE TABLE IF NOT EXISTS ${flyway:database}.system_base.t_code (
    c_pid            varchar(300)     NULL     ,        --代码类型
    c_code           varchar(300)     NULL     ,        --代码值
    c_name			 varchar(300)     NULL     ,        --代码名称
    c_levelinfo      varchar(300)     NULL     ,        --层级
    c_valid          varchar(300)     NULL     ,        --是否有效
    n_order          int4             NULL     ,        --序号
    c_dmjp			 varchar(300)	  NULL	    		--代码简拼
    );

commit;

COMMENT ON TABLE  ${flyway:database}.system_base.t_code is '代码值';
COMMENT ON COLUMN ${flyway:database}.system_base.t_code.c_pid  is '代码类型';
COMMENT ON COLUMN ${flyway:database}.system_base.t_code.c_code  is '代码值';
COMMENT ON COLUMN ${flyway:database}.system_base.t_code.c_name  is '代码名称';
COMMENT ON COLUMN ${flyway:database}.system_base.t_code.c_levelinfo  is '层级';
COMMENT ON COLUMN ${flyway:database}.system_base.t_code.c_valid  is '是否有效';
COMMENT ON COLUMN ${flyway:database}.system_base.t_code.n_order  is '序号';
COMMENT ON COLUMN ${flyway:database}.system_base.t_code.c_dmjp  is '代码简拼';
commit;


-- 01_创建数据库表/t_code_type.sql
--  --------------------------------------------------
-- 需求名称：数据库设计
-- 版本：system_base_init
-- 作者：teddy
-- 说明：代码类型定义
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to system_base;
commit;


CREATE TABLE IF NOT EXISTS ${flyway:database}.system_base.t_code_type (
    c_id             varchar(300)     NULL     ,        --代码类型ID
    c_name           varchar(300)     NULL     ,        --名称
    c_valid          varchar(300)     NULL     ,        --是否有效
    c_editable       varchar(300)     NULL     ,        --是否可编辑
    constraint pk_code_type primary key(c_id)
    );

commit;

COMMENT ON TABLE  ${flyway:database}.system_base.t_code_type is '代码类型';
COMMENT ON COLUMN ${flyway:database}.system_base.t_code_type.c_id  is '代码类型ID';
COMMENT ON COLUMN ${flyway:database}.system_base.t_code_type.c_name  is '名称';
COMMENT ON COLUMN ${flyway:database}.system_base.t_code_type.c_valid  is '是否有效';
commit;


-- 02_创建数据库对象/02_CI_t_code.sql
set client_encoding = UTF8;
set search_path to system_base;
commit;


--  --------------------------------------------------
-- 需求名称：数据库设计
-- 版本：system_base_init
-- 作者：teddy
-- 说明：
--  --------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS pk_code_pid_code ON ${flyway:database}.system_base.t_code (c_pid,c_code);
commit;

CREATE UNIQUE INDEX IF NOT EXISTS "unique_user_login_id" ON ${flyway:database}.system_base.t_user USING btree ("c_loginid");



set client_encoding = UTF8;
set search_path to system_base;
commit;

CREATE TABLE IF NOT EXISTS ${flyway:database}.system_base."t_api_log"
(
    "c_id"            varchar(32) COLLATE "default" NOT NULL,
    "c_tenant"        varchar(32) COLLATE "default" NOT NULL,
    "c_ip"            varchar(900) COLLATE "default" NOT NULL,
    "c_user"          varchar(32) COLLATE "default" NOT NULL,
    "c_user_name"     varchar(900) COLLATE "default",
    "c_module"        varchar(900) COLLATE "default",
    "c_api"           varchar(900) COLLATE "default",
    "c_api_name"      varchar(900) COLLATE "default",
    "c_request"       text COLLATE "default",
    "c_status"        varchar(900) COLLATE "default",
    "c_response"      text COLLATE "default",
    "dt_request_start_time" timestamp(6),
    "dt_request_end_time" timestamp(6),
    "n_request_times" int4 NULL,
    constraint pk_api_log primary key(c_id)
    );



COMMENT ON TABLE  ${flyway:database}.system_base.t_api_log is '用户权限';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."c_id" IS '主键';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."c_tenant" IS '所属租户';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."c_ip" IS '访问IP';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."c_user" IS '访问API的用户ID';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."c_user_name" IS '用户名';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."c_module" IS '访问模块';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."c_api" IS '访问的api地址';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."c_api_name" IS '接口名';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."c_request" IS '请求内容';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."c_status" IS '请求状态';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."c_response" IS '请求结果';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."dt_request_start_time" IS '请求开始时间';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log."dt_request_end_time" IS '请求结束时间';
COMMENT ON COLUMN ${flyway:database}.system_base.t_api_log.n_request_times IS '请求耗时';


-- 01_创建数据库表/t_role.sql
--  --------------------------------------------------
-- 需求名称：角色管理
-- 版本：0.0
-- 作者：fanzhongwei
-- 说明：角色定义
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to system_base;
commit;

CREATE TABLE IF NOT EXISTS ${flyway:database}.system_base.t_role (
    c_id             varchar(32)      NOT NULL, --COMMENT '主键' ,
    c_name           varchar(300)     NOT NULL, --COMMENT '角色名称' ,
    c_descript       varchar(300)     NULL    , --COMMENT '角色描述' ,
    c_valid          varchar(100)     NULL    , --COMMENT '是否有效' ,
    n_order          int              NULL    , --COMMENT '显示顺序' ,
    constraint pk_role PRIMARY KEY(c_id)
    );


COMMENT ON TABLE  ${flyway:database}.system_base.t_role is '角色定义';
COMMENT ON COLUMN ${flyway:database}.system_base.t_role."c_id" IS '主键';
COMMENT ON COLUMN ${flyway:database}.system_base.t_role."c_name" IS '角色名称';
COMMENT ON COLUMN ${flyway:database}.system_base.t_role."c_descript" IS '角色描述';
COMMENT ON COLUMN ${flyway:database}.system_base.t_role."c_valid" IS '是否有效';
COMMENT ON COLUMN ${flyway:database}.system_base.t_role."n_order" IS '显示顺序';


-- 01_创建数据库表/t_role_right.sql
--  --------------------------------------------------
-- 需求名称：角色管理
-- 版本：0.0
-- 作者：fanzhongwei
-- 说明：角色权限映射关系
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to system_base;
commit;

CREATE TABLE IF NOT EXISTS ${flyway:database}.system_base.t_role_right (
    c_id             varchar(32)      NOT NULL   ,  --   COMMENT '主键' ,
    c_role_id        varchar(32)      NOT NULL   ,  --   COMMENT '角色ID' ,
    c_rightkey       varchar(300)     NOT NULL   ,  --   COMMENT '权限字' ,
    constraint pk_role_right PRIMARY KEY(c_id)
    );

COMMENT ON TABLE  ${flyway:database}.system_base.t_role_right is '角色权限映射';
COMMENT ON COLUMN ${flyway:database}.system_base.t_role_right."c_id" IS '主键';
COMMENT ON COLUMN ${flyway:database}.system_base.t_role_right."c_role_id" IS '角色ID';
COMMENT ON COLUMN ${flyway:database}.system_base.t_role_right."c_rightkey" IS '权限字';


-- 01_创建数据库表/t_dwjk_access.sql
--  --------------------------------------------------
-- 需求名称：对外接口鉴权
-- 版本：0.0
-- 作者：fanzhongwei
-- 说明：对外接口授权信息
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to system_base;
commit;


CREATE TABLE IF NOT EXISTS ${flyway:database}.system_base.t_dwjk_access (
    c_id             varchar(32)      NOT NULL   ,        --主键
    c_tenant           varchar(100)     NOT NULL   ,        --所属租户
    c_system_name    varchar(300)     NOT NULL   ,        --系统名称
    c_access_token   varchar(100)     NULL       ,        --系统授权
    c_ip_whitelist   text             NULL       ,        --IP白名单，多个以;隔开，如果设置则仅允许白名单内的IP访问
    c_ip_blacklist   text             NULL       ,        --IP黑名单，多个以;隔开
    dt_access_time_start timestamp         NULL       ,        --授权开始时间
    dt_access_time_end timestamp         NULL       ,        --授权结束时间
    c_contact        varchar(300)     NULL       ,        --系统联系人
    c_contact_number varchar(300)     NULL       ,        --系统联系人电话
    c_authorizer     varchar(300)     NOT NULL   ,        --授权人
    c_authorization_time timestamp         NULL       ,        --授权时间
    constraint pk_dwjk_access primary key(c_id)
    );

commit;

COMMENT ON TABLE  ${flyway:database}.system_base.t_dwjk_access is '对外接口授权';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.c_id  is '主键';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.c_tenant  is '所属租户';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.c_system_name  is '系统名称';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.c_access_token  is '系统授权';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.c_ip_whitelist  is 'IP白名单，多个以;隔开，如果设置则仅允许白名单内的IP访问';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.c_ip_blacklist  is 'IP黑名单，多个以;隔开';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.dt_access_time_start  is '授权开始时间';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.dt_access_time_end  is '授权结束时间';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.c_contact  is '系统联系人';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.c_contact_number  is '系统联系人电话';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.c_authorizer  is '授权人';
COMMENT ON COLUMN ${flyway:database}.system_base.t_dwjk_access.c_authorization_time  is '授权时间';
commit;




