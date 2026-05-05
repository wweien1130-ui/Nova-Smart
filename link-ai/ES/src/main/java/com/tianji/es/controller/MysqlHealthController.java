package com.tianji.es.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/check")
@RequiredArgsConstructor
public class MysqlHealthController {

    private final DataSource dataSource;

    @Qualifier("linkaiDataSource")
    private final DataSource linkaiDataSource;

    /**
     * 检测 MySQL hmall 数据库连接状态
     * GET /check/mysql
     */
    @GetMapping("/mysql")
    public Map<String, Object> checkMysql() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "MySQL");
        result.put("host", "192.168.115.128:3307");

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            result.put("status", "✅ 连接成功");
            result.put("database", "hmall");
            result.put("databaseProductName", metaData.getDatabaseProductName());
            result.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            result.put("url", metaData.getURL());
            result.put("userName", metaData.getUserName());

            // 查询 hmall 数据库中的表
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables("hmall", null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            result.put("tables", tables);
            result.put("tableCount", tables.size());

        } catch (Exception e) {
            result.put("status", "❌ 连接失败");
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 检测 MySQL linkai 数据库连接状态
     * GET /check/linkai
     */
    @GetMapping("/linkai")
    public Map<String, Object> checkLinkai() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "MySQL");
        result.put("host", "192.168.115.128:3307");

        try (Connection connection = linkaiDataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            result.put("status", "✅ 连接成功");
            result.put("database", "linkai");
            result.put("databaseProductName", metaData.getDatabaseProductName());
            result.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            result.put("url", metaData.getURL());
            result.put("userName", metaData.getUserName());

            // 查询 linkai 数据库中的表
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables("linkai", null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            result.put("tables", tables);
            result.put("tableCount", tables.size());

            // 查询各表的记录数
            Map<String, Integer> tableRowCounts = new HashMap<>();
            for (String table : tables) {
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
                    if (rs.next()) {
                        tableRowCounts.put(table, rs.getInt(1));
                    }
                }
            }
            result.put("tableRowCounts", tableRowCounts);

        } catch (Exception e) {
            result.put("status", "❌ 连接失败");
            result.put("error", e.getMessage());
        }

        return result;
    }
}
