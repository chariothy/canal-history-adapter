package net.chariothy.db.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import net.chariothy.db.pojo.DbLog;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author Henry Tian
 *
 * @date  2019-05-20
 **/
@Repository
@Transactional(rollbackFor = Exception.class)
public class DbLogDao {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String tableName = "db_log";
    private final String insertSql = "INSERT INTO `" + tableName + "` (`type`, `db_table`, `json`) VALUES (?,?,?);";

    @Autowired
    public DbLogDao(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) throws IOException {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;

        List<String> lines = FileUtils.readLines(new ClassPathResource("create_table.sql").getFile(), Charsets.UTF_8);
        String createTableSql = String.join("\n", lines);
        jdbcTemplate.execute(createTableSql);
    }

    public int insert(DbLog dbLog) throws JsonProcessingException {
        return jdbcTemplate.update(insertSql, dbLog.getOpType(), dbLog.getDbTable(), objectMapper.writeValueAsString(dbLog.getJson()));
    }

    public int[] batchInsert(final List<DbLog> dbLogs) {
        return jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                DbLog dbLog = dbLogs.get(i);
                ps.setString(1, dbLog.getOpType());
                ps.setString(2, dbLog.getDbTable());
                try {
                    ps.setString(3, objectMapper.writeValueAsString(dbLog.getJson()));
                } catch (JsonProcessingException e) {
                    ps.setString(3, null);
                }
            }

            @Override
            public int getBatchSize() {
                return dbLogs.size();
            }
        });
    }

    /**
     *
     * @param where Condition that includes WHERE
     * @return Count of records
     */
    public Integer count(String where) {
        String countSql;
        if (where == null || where.length() == 0) {
            countSql = "COUNT * FROM " + tableName;
        } else {
            Assert.isTrue(where.toUpperCase().trim().startsWith("WHERE"), "Must begin with WHERE.");
            countSql = "COUNT * FROM " + tableName + " " +  where;
        }
        return jdbcTemplate.queryForObject(countSql, Integer.class);
    }
}
