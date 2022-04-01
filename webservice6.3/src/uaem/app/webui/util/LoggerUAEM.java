package uaem.app.webui.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author OCA
 */
public class LoggerUAEM {

    Date fecha = new Date();
    Logger log;

    public LoggerUAEM(String workspace) throws IOException {
        log = Logger.getLogger(LoggerUAEM.class);
        SimpleDateFormat formato = new SimpleDateFormat("dd.MM.yyyy");
        String fechaAc = formato.format(fecha);
        PatternLayout defaultLayout = new PatternLayout("%p: %d{HH:mm:ss} --> %m%n");
        RollingFileAppender rollingFileAppender = new RollingFileAppender();
        rollingFileAppender.setFile(workspace + "logs/archivo_" + fechaAc + ".log", true, false, 0);
        rollingFileAppender.setLayout(defaultLayout);
        log.removeAllAppenders();
        log.addAppender(rollingFileAppender);
        log.setAdditivity(false);
    }
    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

}
