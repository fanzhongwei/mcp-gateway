-- --------------------------------------------------
-- 需求名称：数据库设计--系统初始化数据
-- 版本：DBD_ONLINE_INIT
-- 作者：mmyf
-- 说明：
-- --------------------------------------------------

INSERT INTO system_base.t_tenant (c_id, c_name, c_pid, n_level, c_alias, c_valid, n_order, lc_ext, dt_create_time, dt_last_modify) VALUES ('0', 'MCP网关平台', NULL, NULL, 'MCP网关平台', '1', '0', NULL, NULL, NULL) ON CONFLICT (c_id) DO NOTHING;

INSERT INTO system_base.t_user (c_id, c_loginid, c_name, c_password, c_mail, c_ip, c_tenant, c_dept, c_valid, n_order, lc_ext, dt_create_time, dt_last_modify, c_type)
VALUES ('1986b812b7789355591a1871330e0420', 'mcpAdmin', 'mcp超级管理员', 'e10adc3949ba59abbe56e057f20f883e', 'fzwtingyu@163.com', NULL, '0', NULL, '1', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL) ON CONFLICT (c_id) DO NOTHING;

INSERT INTO system_base.t_code_type (c_id, c_name, c_valid) VALUES('100001', '是否', '1') ON CONFLICT (c_id) DO NOTHING;
INSERT INTO system_base.t_code (c_pid, c_code, c_name, c_levelinfo, c_valid, n_order, c_dmjp) VALUES('100001', '1', '是', NULL, '1', 1, 's') ON CONFLICT (c_pid, c_code) DO NOTHING;
INSERT INTO system_base.t_code (c_pid, c_code, c_name, c_levelinfo, c_valid, n_order, c_dmjp) VALUES('100001', '2', '否', NULL, '1', 2, 'f') ON CONFLICT (c_pid, c_code) DO NOTHING;


--  --------------------------------------------------
-- 需求名称：对接外部租户
-- 版本：1.0.0
-- 作者：
-- 说明：用户类型
--  --------------------------------------------------
INSERT INTO system_base.t_code_type(c_id,c_name,c_valid)
VALUES ('100002','用户类型','1') ON CONFLICT (c_id) DO NOTHING;

--  --------------------------------------------------
-- 需求名称：超级管理员默认权限
-- 版本：1.0.0
-- 作者：
-- 说明：用户类型
--  --------------------------------------------------
INSERT INTO system_base.t_right (c_rightkey, c_name, c_descript, n_order) VALUES
    ('system.superAdmin',	'超级管理员-所有权限点',	'超级管理员-所有权限点',	NULL) ON CONFLICT (c_rightkey) DO NOTHING;

-- 系统设置菜单权限点
INSERT INTO system_base.t_right (c_rightkey, c_name, c_descript, n_order) VALUES
    ('system.system',           '系统设置',     '系统设置',             1) ON CONFLICT (c_rightkey) DO NOTHING;
INSERT INTO system_base.t_right (c_rightkey, c_name, c_descript, n_order) VALUES
    ('system.system.user',      '租户管理',     '租户管理',             2) ON CONFLICT (c_rightkey) DO NOTHING;
INSERT INTO system_base.t_right (c_rightkey, c_name, c_descript, n_order) VALUES
    ('system.system.role',      '角色列表',     '角色列表',             3) ON CONFLICT (c_rightkey) DO NOTHING;
INSERT INTO system_base.t_right (c_rightkey, c_name, c_descript, n_order) VALUES
    ('system.system.permissions','权限点管理',  '权限点管理',            4) ON CONFLICT (c_rightkey) DO NOTHING;
INSERT INTO system_base.t_right (c_rightkey, c_name, c_descript, n_order) VALUES
    ('system.system.authorization', '对外授权管理', '对外授权管理',      5) ON CONFLICT (c_rightkey) DO NOTHING;
INSERT INTO system_base.t_right (c_rightkey, c_name, c_descript, n_order) VALUES
    ('system.system.log',       '日志',         '日志',                 6) ON CONFLICT (c_rightkey) DO NOTHING;
INSERT INTO system_base.t_right (c_rightkey, c_name, c_descript, n_order) VALUES
    ('system.system.develop',   '开发工具',     '开发工具',             7) ON CONFLICT (c_rightkey) DO NOTHING;


INSERT INTO system_base.t_user_right (c_id, c_userid, c_type, c_roleid, c_rightkey) VALUES
    ('79f35a00cfdf414883e198a063a4ba0a',	'1986b812b7789355591a1871330e0420',	'1',	null,	'system.superAdmin') ON CONFLICT (c_id) DO NOTHING;


-- 角色权限关联
INSERT INTO system_base.t_role
(c_id, c_name, c_descript, c_valid, n_order)
VALUES('c34c791df8944c028e1e3d7a201027cc', '租户管理员', '租户管理员', NULL, NULL) ON CONFLICT (c_id) DO NOTHING;

INSERT INTO system_base.t_role_right
(c_id, c_role_id, c_rightkey)
VALUES('c6197c1d9f2a4a7ebe915a46dae7d028', 'c34c791df8944c028e1e3d7a201027cc', 'system.system.role') ON CONFLICT (c_id) DO NOTHING;
INSERT INTO system_base.t_role_right
(c_id, c_role_id, c_rightkey)
VALUES('0797b4e304224f849d4225f41fcaae6b', 'c34c791df8944c028e1e3d7a201027cc', 'system.system.user') ON CONFLICT (c_id) DO NOTHING;
INSERT INTO system_base.t_role_right
(c_id, c_role_id, c_rightkey)
VALUES('f00df11ded3142a3a0621addc40d2e80', 'c34c791df8944c028e1e3d7a201027cc', 'system.system.authorization') ON CONFLICT (c_id) DO NOTHING;
INSERT INTO system_base.t_role_right
(c_id, c_role_id, c_rightkey)
VALUES('c2f05ff434f94514ae9a5806b309551f', 'c34c791df8944c028e1e3d7a201027cc', 'system.system.log') ON CONFLICT (c_id) DO NOTHING;
INSERT INTO system_base.t_role_right
(c_id, c_role_id, c_rightkey)
VALUES('10b4a59d507a4286821a0fbbe2b1c7e7', 'c34c791df8944c028e1e3d7a201027cc', 'system.system');