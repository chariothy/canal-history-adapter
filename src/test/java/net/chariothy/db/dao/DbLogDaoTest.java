package net.chariothy.db.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.chariothy.db.config.AppConfig;
import net.chariothy.db.pojo.DbLog;
import net.chariothy.db.pojo.DdlLog;
import net.chariothy.db.pojo.DmlLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {AppConfig.class})
public class DbLogDaoTest extends AbstractJUnit4SpringContextTests {
    @Autowired
    private DbLogDao dbLogDao;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    @Rollback
    public void insert() throws JsonProcessingException {
        int id = dbLogDao.insert(new DbLog("ALTER", "TEST", new DdlLog("ALTER TEST")));
        assertTrue(id > 0);

        int id2 = dbLogDao.insert(new DbLog("UPDATE", "TEST", new DmlLog(
                "Col", "VARCHAR(20)", "old val", "new val"
        )));
        assertTrue(id2 > 0);

        int id3 = dbLogDao.insert(new DbLog("INSERT", "TEST", new DmlLog(
                "Col", "VARCHAR(20)", null, "new val"
        )));
        assertTrue(id3 > 0);

        int id4 = dbLogDao.insert(new DbLog("DELETE", "TEST", new DmlLog(
                "Col", "VARCHAR(20)", "old val", null
        )));
        assertTrue(id4 > 0);
    }

    @Test
    @Rollback
    public void batchInsert() {
        List<DbLog> dbLogs = Arrays.asList(
                new DbLog("ALTER", "TEST", new DdlLog("ALTER TEST")),
                new DbLog("UPDATE", "TEST", new DmlLog(
                        "Col", "VARCHAR(20)", "old val", "new val"
                )),
                new DbLog("INSERT", "TEST", new DmlLog(
                        "Col", "VARCHAR(20)", null, "new val"
                )),
                new DbLog("DELETE", "TEST", new DmlLog(
                        "Col", "VARCHAR(20)", "old val", null
                ))
        );
        int[] ids = dbLogDao.batchInsert(dbLogs);
        assertTrue(ids.length > 0);
    }

}