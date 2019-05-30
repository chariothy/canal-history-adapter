package net.chariothy.db.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.InvalidProtocolBufferException;
import net.chariothy.db.dao.DbLogDao;
import net.chariothy.db.pojo.DbLog;
import net.chariothy.db.pojo.DdlLog;
import net.chariothy.db.pojo.DmlLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author Henry Tian
 * @date 2019-05-19 19:52
 **/
@Component
public class CanalHistoryAdapter {
    private final static Logger logger = LoggerFactory.getLogger(CanalHistoryAdapter.class);
    private CanalConnector canalConnector;

    @Value("${canal.dest")
    private String destination;
    private DbLogDao dbLogDao;

    private ExecutorService fixedThreadPool = new ThreadPoolExecutor(
            0, 10, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("CanalHistoryApp-%d").build()
    );

    @Autowired
    public CanalHistoryAdapter(CanalConnector canalConnector, DbLogDao dbLogDao) {
        this.canalConnector = canalConnector;
        this.dbLogDao = dbLogDao;
    }

    public void start() {
        Assert.notNull(canalConnector, "connector is null");
        MDC.put("destination", destination);
        logger.info("Canal history adapter has been started.");
        fixedThreadPool.execute(this::process);
    }

    public void stop() throws InterruptedException  {
        fixedThreadPool.shutdownNow();
        while (!fixedThreadPool.isShutdown()) {
            Thread.sleep(1000);
            logger.info("Canal history adapter is waiting for being stopped.");
        }
        MDC.remove("destination");
        logger.info("Canal history adapter has been stopped.");
    }

    private void process() {
        int batchSize = 5 * 1024;
        int errorTimes = 0;
        while (!Thread.currentThread().isInterrupted()) {
            long batchId = 0;
            try {
                canalConnector.connect();
                canalConnector.subscribe();
                logger.info("Canal history adapter connected to client.");
                while (!Thread.currentThread().isInterrupted()) {
                    // 获取指定数量的数据
                    Message message = canalConnector.getWithoutAck(batchSize);
                    batchId = message.getId();
                    int size = message.getEntries().size();
                    if (batchId == -1 || size == 0) {
                         try {
                            Thread.sleep(1000);
                         } catch (InterruptedException e) {
                         }
                    } else {
                        recordHistory(message.getEntries());
                    }
                    // 提交确认
                    canalConnector.ack(batchId);
                }
            } catch (Exception e) {
                logger.error("process error!", e);
                // 处理失败, 回滚数据
                if (batchId > 0) {
                    canalConnector.rollback(batchId);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
                if(++errorTimes > 100) {
                    break;
                }
            } finally {
                canalConnector.disconnect();
                MDC.remove("destination");
            }
        }
    }

    private void recordHistory(List<CanalEntry.Entry> entries) throws InvalidProtocolBufferException {
        List<DbLog> dbLogs = new ArrayList<DbLog>(entries.size());
        for (CanalEntry.Entry entry: entries) {
            if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                CanalEntry.EventType eventType = rowChange.getEventType();
                CanalEntry.Header header = entry.getHeader();
                String dbTable = header.getSchemaName() + '.' + header.getTableName();

                if(rowChange.getIsDdl()) {
                    DbLog dbDdlLog = new DbLog("DDL", dbTable,
                            new DdlLog(rowChange.getSql())
                    );
                    dbLogs.add(dbDdlLog);
                } else {
                    for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                        DbLog dbDmlLog;
                        if (eventType == CanalEntry.EventType.DELETE) {
                            dbDmlLog = new DbLog(eventType.toString(), dbTable, buildFieldLog(rowData.getBeforeColumnsList(), null));
                        } else if (eventType == CanalEntry.EventType.INSERT) {
                            dbDmlLog = new DbLog(eventType.toString(), dbTable, buildFieldLog(null, rowData.getAfterColumnsList()));
                        } else {
                            dbDmlLog = new DbLog(eventType.toString(), dbTable, buildFieldLog(rowData.getBeforeColumnsList(), rowData.getAfterColumnsList()));
                        }
                        dbLogs.add(dbDmlLog);
                    }
                }
            }
        }
        dbLogDao.batchInsert(dbLogs);
    }

    /**
     *
     * @param beforeColumns Column value list before operation.
     * @param afterColumns Column value list after operation.
     * @return  DML log list.
     */
    private List<DmlLog> buildFieldLog(List<CanalEntry.Column> beforeColumns, List<CanalEntry.Column> afterColumns) {
        Assert.isTrue(beforeColumns != null || afterColumns != null, "Can not both be null");
        int mapSize = null == beforeColumns ? 1 : beforeColumns.size();
        Map<String, CanalEntry.Column> beforeFieldNameColumnMap = new HashMap<String, CanalEntry.Column>(mapSize);
        if (null != beforeColumns && null != afterColumns) {
            for (CanalEntry.Column column : beforeColumns) {
                beforeFieldNameColumnMap.put(column.getName(), column);
            }
        }

        List<DmlLog> dmlLogs = new ArrayList<DmlLog>();
        // DELETE
        if (null == afterColumns) {
            for (CanalEntry.Column column : beforeColumns) {
                DmlLog dmlLog = new DmlLog();
                dmlLog.setName(column.getName());
                dmlLog.setType(column.getMysqlType());
                dmlLog.setOldVal(getValueString(column));
                dmlLog.setNewVal(null);

                dmlLogs.add(dmlLog);
            }
        } else {
            for (CanalEntry.Column column : afterColumns) {
                if (column.getUpdated()) {
                    DmlLog dmlLog = new DmlLog();
                    dmlLog.setName(column.getName());
                    dmlLog.setType(column.getMysqlType());
                    dmlLog.setNewVal(getValueString(column));

                    CanalEntry.Column beforeColumn = beforeFieldNameColumnMap.getOrDefault(column.getName(), null);
                    if (beforeColumn != null) {
                        dmlLog.setOldVal(getValueString(beforeColumn));
                    }

                    dmlLogs.add(dmlLog);
                }
            }
        }
        return dmlLogs;
    }

    private String getValueString(CanalEntry.Column column) {
        try {
            String blob = "BLOB";
            if (StringUtils.containsIgnoreCase(column.getMysqlType(), blob)
                    || StringUtils.containsIgnoreCase(column.getMysqlType(), "BINARY")) {
                // get value bytes
                return new String(column.getValue().getBytes("ISO-8859-1"), "UTF-8");
            } else {
                return column.getValue();
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Can not get bytes value.", e);
            return "";
        }
    }
}
