set client_encoding = UTF8;
set search_path to db_mcp;
commit;


--  --------------------------------------------------
-- 需求名称：接口解析
-- 版本：V1.0
-- 作者：超级管理员
-- 说明：
--  --------------------------------------------------
CREATE  INDEX IF NOT EXISTS I_system_env_system_id ON db_mcp.t_system_env USING btree (c_system_id);
commit;


set client_encoding = UTF8;
set search_path to db_mcp;
commit;


--  --------------------------------------------------
-- 需求名称：接口解析
-- 版本：V1.0
-- 作者：超级管理员
-- 说明：
--  --------------------------------------------------
CREATE  INDEX IF NOT EXISTS I_api_system_id ON db_mcp.t_api USING btree (c_system_id);
commit;

--  --------------------------------------------------
-- 需求名称：接口解析
-- 版本：V1.0
-- 作者：超级管理员
-- 说明：
--  --------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS I_api_path_method_system_id ON db_mcp.t_api USING btree (c_path,c_method,c_system_id);
commit;


set client_encoding = UTF8;
set search_path to db_mcp;
commit;


--  --------------------------------------------------
-- 需求名称：MCP管理
-- 版本：V1.0
-- 作者：超级管理员
-- 说明：
--  --------------------------------------------------
CREATE  INDEX IF NOT EXISTS I_mcp_config_api_mcp ON db_mcp.t_mcp_config_api USING btree (c_mcp_service_id);
commit;


