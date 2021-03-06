package cn.csdb.portal.service;

import cn.csdb.portal.model.DataSrc;
import cn.csdb.portal.model.Subject;
import cn.csdb.portal.model.TableField;
import cn.csdb.portal.repository.CheckUserDao;
import cn.csdb.portal.utils.dataSrc.DataSourceFactory;
import cn.csdb.portal.utils.dataSrc.IDataSource;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.*;
import java.util.*;

/**
 * @ClassName FileImportService
 * @Description
 * @Author jinbao
 * @Date 2018/12/26 11:25
 * @Version 1.0
 **/
@Service
public class FileImportService {
    private Logger logger = LoggerFactory.getLogger(FileImportService.class);
    @Resource
    private CheckUserDao checkUserDao;

    @Transactional
    public JSONObject processExcel(Workbook workbook, String subjectCode) {
        // 接收excel 判断excel类型
        JSONObject jsonObject = new JSONObject();

        // 解析excel 隐藏的sheet，row跳过
        Map<String, List<List<String>>> mapSheet = new LinkedHashMap<>();
        parseExcel(workbook, mapSheet);
        int sheetSize = mapSheet.keySet().size();
        if (sheetSize == 0) {
            jsonObject.put("code", "error");
            jsonObject.put("message", "excel数据为空");
        } else if (sheetSize > 1) {
            jsonObject.put("code", "error");
            jsonObject.put("message", "excel中仅能有一个数据源");
        }

        // 获取当前用户的MySQL连接
        DataSrc dataSrc = getDataSrc(subjectCode, "mysql");
        Connection connection = getConnection(dataSrc);
        if (connection == null) {
            jsonObject.put("code", "error");
            jsonObject.put("message", "数据库连接异常");
            return jsonObject;

        }
        // sheet中  第一行为字段中文名称，第二行为数据库字段名称， sheet页名称为表名称
        //判断表是否存在 存在则判断字段是否对应   不存在则新增数据表
        List<Map<String, List<List<String>>>> resultList = new LinkedList<>();
        Iterator<Map.Entry<String, List<List<String>>>> iterator = mapSheet.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<List<String>>> next = iterator.next();
            String key = next.getKey();
            List<List<String>> value = next.getValue();
            if (value == null || value.size() == 0) {
                jsonObject.put("code", "error");
                jsonObject.put("message", key + "页签中数据为空，请查验");
                return jsonObject;
            }

            // excel当前表存在比较数据库字段 与 excel字段 顺序、字段名称
            boolean tableIsExist = tableIsExist(connection, null, dataSrc.getDatabaseName(), key);
            Map<String, Map<String, List<String>>> tableResult = new LinkedHashMap<>();
            Map<String, List<String>> stringListMap = new LinkedHashMap<>();
            if (tableIsExist) {
                stringListMap = tableField(key, connection);
            }
            // excel当前表不存在 选择字段类型、长度、是否主键
            stringListMap.put("excelField", value.get(1));
            stringListMap.put("excelComment", value.get(0));
            tableResult.put(key, stringListMap);
            Map<String, List<List<String>>> resultMap = formatterResult(tableResult);
            resultList.add(resultMap);
        }
        jsonObject.put("data", resultList);
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("Error:数据连接关闭错误");
            jsonObject.put("code", "error");
            jsonObject.put("message", "数据连接关闭错误");
            return jsonObject;
        }
        jsonObject.put("code", "success");
        return jsonObject;
    }

    @Transactional
    public JSONObject createTableAndInsertValue(String tableName, List<TableField> tableFields, Workbook workbook, String subjectCode) {
        JSONObject jsonObject = new JSONObject();
        Map<String, List<List<String>>> mapSheet = new LinkedHashMap<>();
        parseExcel(workbook, mapSheet);

        // 获取当前用户的MySQL连接
        DataSrc dataSrc = getDataSrc(subjectCode, "mysql");
        Connection connection = getConnection(dataSrc);
        if (connection == null) {
            jsonObject.put("code", "error");
            jsonObject.put("message", "数据库连接异常");
            return jsonObject;

        }
        // 创建表
        Boolean createTable = createTableSql(connection, tableName, tableFields);
        if (!createTable) {
            jsonObject.put("code", "error");
            jsonObject.put("message", "创建表失败");
            return jsonObject;
        }

        // 插入数据
        Set<Map.Entry<String, List<List<String>>>> entries = mapSheet.entrySet();
        Iterator<Map.Entry<String, List<List<String>>>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<List<String>>> next = iterator.next();
            List<List<String>> value = next.getValue();
            boolean insertValue = insertValue(connection, tableName, value);
            if (!insertValue) {
                jsonObject.put("code", "error");
                jsonObject.put("message", "数据插入失败");
                return jsonObject;
            }
        }
        jsonObject.put("code", "success");
        jsonObject.put("message", tableName + "创建成功，数据增添成功");
        return jsonObject;
    }

    @Transactional
    public JSONObject onlyInsertValue(String tableName, List<TableField> tableFields, Workbook workbook, String subjectCode) {
        JSONObject jsonObject = new JSONObject();
        Map<String, List<List<String>>> mapSheet = new LinkedHashMap<>();
        parseExcel(workbook, mapSheet);
        // 获取当前用户的MySQL连接
        DataSrc dataSrc = getDataSrc(subjectCode, "mysql");
        Connection connection = getConnection(dataSrc);
        if (connection == null) {
            jsonObject.put("code", "error");
            jsonObject.put("message", "数据库连接异常");
            return jsonObject;
        }
        Iterator<Map.Entry<String, List<List<String>>>> iterator = mapSheet.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<List<String>>> next = iterator.next();
            List<List<String>> value = next.getValue();
            boolean insertValue = onlyInsertSql(tableName, tableFields, value, connection);
            if(!insertValue){
                jsonObject.put("code","error");
                jsonObject.put("message","数据添加失败");
                return jsonObject;
            }
        }
        jsonObject.put("code","success");
        jsonObject.put("message","数据添加成功");
        return jsonObject;
    }


    /**
     * 当前表是否存在
     *
     * @param connection
     * @param catalog
     * @param schema
     * @param table
     * @return
     */
    private boolean tableIsExist(Connection connection, String catalog, String schema, String table) {
        StringBuilder sql = new StringBuilder("");
        sql.append("select COUNT(1) AS num from information_schema.TABLES t WHERE t.TABLE_SCHEMA = '" + schema + "' AND t.TABLE_NAME = '" + table + "'");
        String num = "";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());
            ResultSet res = preparedStatement.executeQuery();
            while (res.next()) {
                num = res.getString("num");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error: could not retrieve data for the sql '" + sql + "' from the backend.");
        }
        return "1".equals(num);
    }


    /**
     * 处理excel生成 Map<表名，List<行值>>
     *
     * @param workbook
     * @param mapSheet
     */
    private void parseExcel(Workbook workbook, Map<String, List<List<String>>> mapSheet) {
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        boolean equals = xssfWorkbook.getClass().equals(workbook.getClass());

        if (equals) {
            xssfWorkbook = (XSSFWorkbook) workbook;
            int numberOfSheets = xssfWorkbook.getNumberOfSheets();
            for (int s = 0; s < numberOfSheets; s++) {
                XSSFSheet sheet = xssfWorkbook.getSheetAt(s);
                if (xssfWorkbook.isSheetHidden(s) || sheet == null) {
                    continue;
                }
                List<List<String>> listString = new LinkedList<>();
                String sheetName = sheet.getSheetName();
                int physicalNumberOfRows = sheet.getPhysicalNumberOfRows();
                for (int r = 0; r < physicalNumberOfRows; r++) {
                    XSSFRow row = sheet.getRow(r);
                    if (sheet.isColumnHidden(r) || row == null) {
                        continue;
                    }
                    List<String> list = new LinkedList<>();
                    int physicalNumberOfCells = row.getPhysicalNumberOfCells();
                    for (int c = 0; c < physicalNumberOfCells; c++) {
                        XSSFCell cell = row.getCell(c);
                        String s1 = cell == null ? "" : cell.toString();
                        list.add(s1);
                    }
                    listString.add(list);
                }
                mapSheet.put(sheetName, listString);
            }
        }
    }


    /**
     * 根据当前用户获取相关连接信息
     *
     * @param subjectCode
     * @param DatabaseType
     * @return
     */
    private DataSrc getDataSrc(String subjectCode, String DatabaseType) {
        Subject subject = checkUserDao.getSubjectByCode(subjectCode);
        DataSrc datasrc = new DataSrc();
        datasrc.setDatabaseName(subject.getDbName());
        datasrc.setDatabaseType(DatabaseType);
        datasrc.setHost(subject.getDbHost());
        datasrc.setPort(subject.getDbPort());
        datasrc.setUserName(subject.getDbUserName());
        datasrc.setPassword(subject.getDbPassword());
        return datasrc;
    }


    /**
     * 获取数据库连接
     *
     * @param dataSrc
     * @return
     */
    private Connection getConnection(DataSrc dataSrc) {
        IDataSource dataSource = DataSourceFactory.getDataSource(dataSrc.getDatabaseType());
        Connection connection = dataSource.getConnection(dataSrc.getHost(), dataSrc.getPort(), dataSrc.getUserName(), dataSrc.getPassword(), dataSrc.getDatabaseName());
        return connection;
    }


    /**
     * 获取 已存在表的字段名称 & 字段注释 & 是否主键
     *
     * @param tableName
     * @param connection
     * @return
     * @throws SQLException
     */
    private Map<String, List<String>> tableField(String tableName, Connection connection) {
        Map<String, List<String>> tableField = new LinkedHashMap<>();
        List<String> fieldList = new LinkedList<>();
        List<String> commentList = new LinkedList<>();
        List<String> priKey = new LinkedList<>();
        StringBuilder sb = new StringBuilder("SHOW FULL COLUMNS FROM ");
        sb.append(tableName);
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sb.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                fieldList.add(resultSet.getString("Field"));
                commentList.add(resultSet.getString("Comment"));
                priKey.add(resultSet.getString("Key"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("获取表" + tableName + "字段属性值错误！");
            try {
                connection.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        tableField.put("tableField", fieldList);
        tableField.put("tableComment", commentList);
        tableField.put("priKey", priKey);
        return tableField;
    }


    /**
     * 将table & excel 数据格式化输出 适合页面渲染
     *
     * @param jsonObject
     * @return
     */
    private Map<String, List<List<String>>> formatterResult(Map<String, Map<String, List<String>>> jsonObject) {
        Map<String, List<List<String>>> map = new LinkedHashMap<>();
        List<List<String>> resultList = new LinkedList<>();
        Set<String> strings = jsonObject.keySet();
        for (String s : strings) {
            Map<String, List<String>> tableMap = jsonObject.get(s);
            List<String> list = tableMap.get("tableField");
            List<String> list1 = tableMap.get("tableComment");
            List<String> list2 = tableMap.get("excelField");
            List<String> list3 = tableMap.get("excelComment");
            // 主键标识
            List<String> list4 = tableMap.get("priKey");
            int tableSize = list == null ? 0 : list.size();
            int excelSize = list2 == null ? 0 : list2.size();
            for (int i = 0; i < tableSize || i < excelSize; i++) {
                List<String> l = new LinkedList<>();
                if (i >= tableSize) {
                    l.add("");
                    l.add("");
                    l.add("");
                } else {
                    l.add(list.get(i));
                    l.add(list1.get(i));
                    l.add(list4.get(i));
                }
                if (i >= excelSize) {
                    l.add("");
                    l.add("");
                } else {
                    l.add(list2.get(i));
                    l.add(list3.get(i));
                }
                resultList.add(l);
            }
            List<String> status = new LinkedList<>();
            if (list == null) {
                status.add("notExist");
                ((LinkedList<List<String>>) resultList).addFirst(status);
            } else {
                status.add("isExist");
                ((LinkedList<List<String>>) resultList).addFirst(status);
            }
            map.put(s, resultList);
        }
        return map;
    }


    /**
     * DDL create table
     *
     * @param tableName
     * @param tableFields
     * @return
     */
    private Boolean createTableSql(Connection connection, String tableName, List<TableField> tableFields) {
        StringBuffer sb = new StringBuffer("CREATE TABLE ");
        sb.append(tableName);
        sb.append("(");
        Iterator<TableField> iterator = tableFields.iterator();
        while (iterator.hasNext()) {
            TableField next = iterator.next();
            String field = next.getField();
            String type = next.getType();
            String length = next.getLength();
            String comment = next.getComment();
            String pk = next.getPk();
            sb.append("`" + field + "`  ");
            sb.append(type);
            sb.append("(" + length + ")");
            sb.append("COMMENT '" + comment + "'");
            if ("1".equals(pk)) {
                sb.append(" PRIMARY KEY");
            }
            sb.append(",");
        }
        if (sb.toString().endsWith(",")) {
            sb.replace(sb.length() - 1, sb.length(), " )");
        }
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sb.toString());
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("创建表" + tableName + "失败");
            try {
                connection.close();
            } catch (SQLException ee) {
                ee.printStackTrace();
            }
            return false;
        }
        return true;
    }


    /**
     * 插入数据
     *
     * @param connection
     * @param tableName
     * @param value
     * @return
     */
    private boolean insertValue(Connection connection, String tableName, List<List<String>> value) {
        StringBuilder sb = new StringBuilder("INSERT INTO `" + tableName + "`(");
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet res = metaData.getColumns(null, null, tableName, null);
            while (res.next()) {
                sb.append(res.getString("COLUMN_NAME"));
                sb.append(",");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(tableName + "生成insert语句获取列名称错误");
            try {
                connection.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            return false;
        }
        if (sb.toString().endsWith(",")) {
            sb.replace(sb.length() - 1, sb.length(), " )");
        }
        sb.append("values");
        List<List<String>> lists = value.subList(2, value.size());
        Iterator<List<String>> iterator = lists.iterator();
        while (iterator.hasNext()) {
            List<String> next = iterator.next();
            sb.append("( ");
            Iterator<String> iterator1 = next.iterator();
            while (iterator1.hasNext()) {
                String next1 = iterator1.next();
                sb.append("'" + next1 + "',");
            }
            if (sb.toString().endsWith(",")) {
                sb.replace(sb.length() - 1, sb.length(), " ),");
            }
        }
        if (sb.toString().endsWith(",")) {
            sb.replace(sb.length() - 1, sb.length(), " ");
        }

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sb.toString());
            int i = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("Error:" + tableName + "插入数据失败");
            try {
                // 首次插入失败 删除表
                PreparedStatement preparedStatement = connection.prepareStatement("drop table " + tableName);
                preparedStatement.execute();
            } catch (SQLException e1) {
                e1.printStackTrace();
            } finally {
                try {
                    connection.close();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
            return false;
        }
        return true;
    }


    private boolean onlyInsertSql(String tableName, List<TableField> tableFields, List<List<String>> value, Connection connection) {
        // excel字段名称
        List<String> excelField = value.get(1);
        // 插入的数据
        List<List<String>> insertValue = value.subList(2, value.size());
        // k:表字段名->v:excel字段名
        Map<String, String> fieldMapping = new LinkedHashMap<>();
        // 前端数据 原始表字段顺序
        List<String> tableFiled = new ArrayList<>();
        // 前端数据 原始表字段对应excel字段顺序
        List<String> newField = new ArrayList<>();
        // excel数据位置
        List<String> orderNum = new ArrayList<>();

        StringBuffer sb = new StringBuffer("INSERT INTO `" + tableName + "`");
        for (TableField t : tableFields) {
            tableFiled.add(t.getOldField());
            newField.add(t.getField());
            fieldMapping.put(t.getOldField(), t.getField());
        }

        Iterator<Map.Entry<String, String>> iterator = fieldMapping.entrySet().iterator();
        sb.append("( ");
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            String k = next.getKey();
            String v = next.getValue();
            if (!"-1".equals(v)) {
                sb.append(k + ",");
            }
        }

        for (String n : newField) {
            for (int i = 0; i < excelField.size(); i++) {
                String s = excelField.get(i);
                if (n.equals(s)) {
                    orderNum.add(Integer.valueOf(i).toString());
                    break;
                }
            }
        }
        if (sb.toString().endsWith(",")) {
            sb.replace(sb.length() - 1, sb.length(), " )");
        }
        sb.append(" VALUES ");
        Iterator<List<String>> iteratorV = insertValue.iterator();
        while (iteratorV.hasNext()) {
            sb.append("( ");
            List<String> next = iteratorV.next();
            for (String i : orderNum) {
                int j = Integer.parseInt(i);
                sb.append("'" + next.get(j) + "',");
            }
            if (sb.toString().endsWith(",")) {
                sb.replace(sb.length() - 1, sb.length(), "),");
            }
        }
        if (sb.toString().endsWith(",")) {
            sb.replace(sb.length() - 1, sb.length(), "");
        }
        String sql = sb.toString();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            logger.error("ERROR: "+tableName+"插入数据失败");
            return false;
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
