package net.chariothy.db;

import net.chariothy.db.canal.CanalHistoryAdapter;
import net.chariothy.db.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Henry Tian
 */
public class CanalHistoryApp {
    private final static Logger logger = LoggerFactory.getLogger(CanalHistoryApp.class);

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        CanalHistoryAdapter canalHistoryAdapter = context.getBean(CanalHistoryAdapter.class);
        canalHistoryAdapter.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.info("## stop the canal client");
                canalHistoryAdapter.stop();
            } catch (Throwable e) {
                logger.warn("##something goes wrong when stopping canal:", e);
            } finally {
                logger.info("## canal client is down.");
            }
        }));
    }
}