
--  --------------------------------------------------
-- 需求名称：网关管理员
-- 版本：1.0.0
-- 作者：
-- 说明：用户类型
--  --------------------------------------------------
INSERT INTO system_base.t_right (c_rightkey, c_name, c_descript, n_order) VALUES
    ('system.businessSystem',	'业务系统管理',	'业务系统管理',	NULL) ON CONFLICT (c_rightkey) DO NOTHING;

INSERT INTO system_base.t_right (c_rightkey, c_name, c_descript, n_order) VALUES
    ('system.apiManage',	'接口管理',	'接口管理',	NULL) ON CONFLICT (c_rightkey) DO NOTHING;

INSERT INTO system_base.t_right (c_rightkey, c_name, c_descript, n_order) VALUES
    ('system.mcpService',	'MCP服务管理',	'MCP服务管理',	NULL) ON CONFLICT (c_rightkey) DO NOTHING;

-- 角色权限关联
INSERT INTO system_base.t_role
(c_id, c_name, c_descript, c_valid, n_order)
VALUES('ae5dd4c5e50f448baac48dd477b1825d', '网关管理员', '网关管理员', NULL, NULL) ON CONFLICT (c_id) DO NOTHING;

INSERT INTO system_base.t_role_right
(c_id, c_role_id, c_rightkey)
VALUES('24398007c60646ee989e5d80cefcdd06', 'ae5dd4c5e50f448baac48dd477b1825d', 'system.businessSystem') ON CONFLICT (c_id) DO NOTHING;
INSERT INTO system_base.t_role_right
(c_id, c_role_id, c_rightkey)
VALUES('2c23206270b34499ac675ce61687609f', 'ae5dd4c5e50f448baac48dd477b1825d', 'system.mcpService') ON CONFLICT (c_id) DO NOTHING;
INSERT INTO system_base.t_role_right
(c_id, c_role_id, c_rightkey)
VALUES('b329200d92794b42b9bca3e27a7da781', 'ae5dd4c5e50f448baac48dd477b1825d', 'system.apiManage') ON CONFLICT (c_id) DO NOTHING;