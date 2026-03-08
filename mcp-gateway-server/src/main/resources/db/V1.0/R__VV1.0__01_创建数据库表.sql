--  --------------------------------------------------
-- 需求名称：接口解析
-- 版本：V1.0
-- 作者：超级管理员
-- 说明：业务系统
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to db_mcp;
commit;


CREATE TABLE IF NOT EXISTS db_mcp.t_system (
    c_id             varchar(32)      NULL     ,        --编号
    c_tenant         varchar(32)      NOT NULL ,        --租租户
    c_name           varchar(300)     NULL     ,        --名称
    c_desc           varchar(300)     NULL     ,        --简介
    c_admin          varchar(300)     NULL     ,        --管理员
    c_admin_contact  varchar(300)     NULL     ,        --管理员联系方式
    dt_create_time   timestamp        NULL     ,        --创建时间
    dt_modify_time   timestamp        NULL     ,        --最后修改时间
constraint pk_system primary key(c_id)
);
commit;

COMMENT ON TABLE  db_mcp.t_system is '业务系统';
COMMENT ON COLUMN db_mcp.t_system.c_id  is '编号';
COMMENT ON COLUMN db_mcp.t_system.c_tenant  is '租租户';
COMMENT ON COLUMN db_mcp.t_system.c_name  is '名称';
COMMENT ON COLUMN db_mcp.t_system.c_desc  is '简介';
COMMENT ON COLUMN db_mcp.t_system.c_admin  is '管理员';
COMMENT ON COLUMN db_mcp.t_system.c_admin_contact  is '管理员联系方式';
COMMENT ON COLUMN db_mcp.t_system.dt_create_time  is '创建时间';
COMMENT ON COLUMN db_mcp.t_system.dt_modify_time  is '最后修改时间';
commit;
--  --------------------------------------------------
-- 需求名称：接口解析
-- 版本：V1.0
-- 作者：超级管理员
-- 说明：系统环境
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to db_mcp;
commit;


CREATE TABLE IF NOT EXISTS db_mcp.t_system_env (
    c_id             varchar(32)      NULL     ,        --编号
    c_tenant         varchar(32)      NOT NULL ,        --租户
    c_system_id      varchar(32)      NULL     ,        --所属系统
    c_name           varchar(300)     NULL     ,        --名称
    c_desc           varchar(300)     NULL     ,        --简介
    c_base_url       varchar(900)     NULL     ,        --系统基础地址
    dt_create_time   timestamp        NULL     ,        --创建时间
    dt_modify_time   timestamp        NULL     ,        --最后修改时间
constraint pk_system_env primary key(c_id)
);
commit;

COMMENT ON TABLE  db_mcp.t_system_env is '系统环境';
COMMENT ON COLUMN db_mcp.t_system_env.c_id  is '编号';
COMMENT ON COLUMN db_mcp.t_system_env.c_tenant  is '租户';
COMMENT ON COLUMN db_mcp.t_system_env.c_system_id  is '所属系统';
COMMENT ON COLUMN db_mcp.t_system_env.c_name  is '名称';
COMMENT ON COLUMN db_mcp.t_system_env.c_desc  is '简介';
COMMENT ON COLUMN db_mcp.t_system_env.c_base_url  is '系统基础地址';
COMMENT ON COLUMN db_mcp.t_system_env.dt_create_time  is '创建时间';
COMMENT ON COLUMN db_mcp.t_system_env.dt_modify_time  is '最后修改时间';
commit;
--  --------------------------------------------------
-- 需求名称：接口解析
-- 版本：V1.0
-- 作者：超级管理员
-- 说明：API接口表
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to db_mcp;
commit;


CREATE TABLE IF NOT EXISTS db_mcp.t_api (
    c_id             varchar(32)      NULL     ,        --编号
    c_tenant         varchar(32)      NOT NULL ,        --租户
    c_system_id      varchar(32)      NULL     ,        --所属系统
    c_method         varchar(100)     NULL     ,        --HTTP方法
    c_path           varchar(500)     NULL     ,        --接口路径
    c_name           varchar(900)     NULL     ,        --接口名称
    c_description    text             NULL     ,        --接口描述
    j_query_params   json             NULL     ,        --Query参数列表（数组）
    j_headers        json             NULL     ,        --请求头列表（数组）
    j_body_param     json             NULL     ,        --Body参数
    j_url_encoded_params json             NULL     ,        --URL编码参数列表（数组）
    j_cookies        json             NULL     ,        --Cookies列表（数组）
    j_response_config json             NULL     ,        --响应配置（对象：validateResponse, successStatus）
    dt_create_time   timestamp        NULL     ,        --创建时间
    dt_modify_time   timestamp        NULL     ,        --最后修改时间
constraint pk_api primary key(c_id)
);
commit;

COMMENT ON TABLE  db_mcp.t_api is 'API接口表';
COMMENT ON COLUMN db_mcp.t_api.c_id  is '编号';
COMMENT ON COLUMN db_mcp.t_api.c_tenant  is '租户';
COMMENT ON COLUMN db_mcp.t_api.c_system_id  is '所属系统';
COMMENT ON COLUMN db_mcp.t_api.c_method  is 'HTTP方法';
COMMENT ON COLUMN db_mcp.t_api.c_path  is '接口路径';
COMMENT ON COLUMN db_mcp.t_api.c_name  is '接口名称';
COMMENT ON COLUMN db_mcp.t_api.c_description  is '接口描述';
COMMENT ON COLUMN db_mcp.t_api.j_query_params  is 'Query参数列表（数组）';
COMMENT ON COLUMN db_mcp.t_api.j_headers  is '请求头列表（数组）';
COMMENT ON COLUMN db_mcp.t_api.j_body_param  is 'Body参数';
COMMENT ON COLUMN db_mcp.t_api.j_url_encoded_params  is 'URL编码参数列表（数组）';
COMMENT ON COLUMN db_mcp.t_api.j_cookies  is 'Cookies列表（数组）';
COMMENT ON COLUMN db_mcp.t_api.j_response_config  is '响应配置（对象：validateResponse, successStatus）';
COMMENT ON COLUMN db_mcp.t_api.dt_create_time  is '创建时间';
COMMENT ON COLUMN db_mcp.t_api.dt_modify_time  is '最后修改时间';
commit;
--  --------------------------------------------------
-- 需求名称：MCP管理
-- 版本：V1.0
-- 作者：超级管理员
-- 说明：mcp服务
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to db_mcp;
commit;


CREATE TABLE IF NOT EXISTS db_mcp.t_mcp_service (
    c_id             varchar(32)      NULL     ,        --主键
    c_tenant         varchar(32)      NOT NULL ,        --租户
    c_name           varchar(300)     NULL     ,        --服务名
    c_desc           text             NULL     ,        --服务描述
    c_endpoint       varchar(300)     NULL     ,        --服务端点
    c_access_token   varchar(300)     NULL     ,        --访问授权
    n_api_count      int4             NULL     ,        --接口数量
    c_status         varchar(300)     NULL     ,        --服务状态
    dt_publish_time  timestamp        NULL     ,        --发布时间
    dt_create_time   timestamp        NULL     ,        --创建时间
    dt_modify_time   timestamp        NULL     ,        --最后修改时间
constraint pk_mcp_service primary key(c_id)
);
commit;

COMMENT ON TABLE  db_mcp.t_mcp_service is 'mcp服务';
COMMENT ON COLUMN db_mcp.t_mcp_service.c_id  is '主键';
COMMENT ON COLUMN db_mcp.t_mcp_service.c_tenant  is '租户';
COMMENT ON COLUMN db_mcp.t_mcp_service.c_name  is '服务名';
COMMENT ON COLUMN db_mcp.t_mcp_service.c_desc  is '服务描述';
COMMENT ON COLUMN db_mcp.t_mcp_service.c_endpoint  is '服务端点';
COMMENT ON COLUMN db_mcp.t_mcp_service.c_access_token  is '访问授权';
COMMENT ON COLUMN db_mcp.t_mcp_service.n_api_count  is '接口数量';
COMMENT ON COLUMN db_mcp.t_mcp_service.c_status  is '服务状态';
COMMENT ON COLUMN db_mcp.t_mcp_service.dt_publish_time  is '发布时间';
COMMENT ON COLUMN db_mcp.t_mcp_service.dt_create_time  is '创建时间';
COMMENT ON COLUMN db_mcp.t_mcp_service.dt_modify_time  is '最后修改时间';
commit;
--  --------------------------------------------------
-- 需求名称：MCP管理
-- 版本：V1.0
-- 作者：超级管理员
-- 说明：mcp服务配置API接口
--  --------------------------------------------------

set client_encoding = UTF8;
set search_path to db_mcp;
commit;


CREATE TABLE IF NOT EXISTS db_mcp.t_mcp_config_api (
    c_id             varchar(32)      NULL     ,        --主键
    c_tenant         varchar(32)      NOT NULL ,        --租户
    c_mcp_service_id varchar(32)      NULL     ,        --mcp服务ID
    c_system_id      varchar(32)      NULL     ,        --业务系统ID
    c_system_mcp_name varchar(300)     NULL     ,        --系统的MCP名字
    j_api_config     json             NULL     ,        --关联的api接口
    c_env_id         varchar(32)      NULL     ,        --关联的环境ID
    dt_create_time   timestamp        NULL     ,        --创建时间
    dt_modify_time   timestamp        NULL     ,        --最后修改时间
constraint pk_mcp_config_api primary key(c_id)
);
commit;

COMMENT ON TABLE  db_mcp.t_mcp_config_api is 'mcp服务配置API接口';
COMMENT ON COLUMN db_mcp.t_mcp_config_api.c_id  is '主键';
COMMENT ON COLUMN db_mcp.t_mcp_config_api.c_tenant  is '租户';
COMMENT ON COLUMN db_mcp.t_mcp_config_api.c_mcp_service_id  is 'mcp服务ID';
COMMENT ON COLUMN db_mcp.t_mcp_config_api.c_system_id  is '业务系统ID';
COMMENT ON COLUMN db_mcp.t_mcp_config_api.c_system_mcp_name  is '系统的MCP名字';
COMMENT ON COLUMN db_mcp.t_mcp_config_api.j_api_config  is '关联的api接口';
COMMENT ON COLUMN db_mcp.t_mcp_config_api.c_env_id  is '关联的环境ID';
COMMENT ON COLUMN db_mcp.t_mcp_config_api.dt_create_time  is '创建时间';
COMMENT ON COLUMN db_mcp.t_mcp_config_api.dt_modify_time  is '最后修改时间';
commit;
