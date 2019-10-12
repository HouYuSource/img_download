package cn.shaines.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 可以在项目的根目录下添加配置文件config.properties
 * # -------------------------  logging config  ------------------------

 # 日志记录级别(非必填 选填) [ DEBUG INFO WARN ERROR ]
 logging.level=DEBUG
 # 日志输出到文件(非必填 选填) [ true false ]
 logging.toFile=true
 # 日志输出到控制台(非必填 选填) [ true false ]
 logging.toConsole=false
 # 保存的日志目录(非必填)
 # logging.toFilePath=C:/Users/houyu/Desktop/logs

 * # -------------------------  logging config  ------------------------
 *
 * @description 过渡项目使用, 如果找不到 org.slf4j.LoggerFactory 使用自己实现的打印, 避免修改代码
 * @date 2019-09-20 17:45:45
 * @author houyu for.houyu@foxmail.com
 */
public class LoggerFactory {

    private static Logger.Level level;
    private static boolean toFile;
    private static boolean toConsole;
    private static String toFilePath;
    static {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(System.getProperty("user.dir") + "/config.properties"));
            level = Logger.Level.valueOf(properties.getProperty("logging.level", "DEBUG"));
            toFile = Boolean.valueOf(properties.getProperty("logging.toFile", "true"));
            toConsole = Boolean.valueOf(properties.getProperty("logging.toConsole", "true"));
            toFilePath = properties.getProperty("logging.toFilePath", System.getProperty("user.dir") + "/logs");
        } catch(IOException e) {
            System.out.println("Error loading custom configuration, because :" + e.getMessage());
            level = Logger.Level.DEBUG;
            toFile = true;
            toConsole = true;
            toFilePath = System.getProperty("user.dir") + "/logs";
        }
    }
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz, level);
    }

    public static Logger getLogger(Class<?> clazz, Logger.Level level) {
        return getLogger(clazz, level, toFile, toConsole);
    }

    public static Logger getLogger(Class<?> clazz, Logger.Level level, boolean toFile, boolean toConsole) {
        return new Logger(clazz.getName(), level, toFile, toConsole, toFilePath);
    }

    public static class Logger {

        public enum Level {
            //
            DEBUG(1), INFO(2), WARN(3), ERROR(4);
            private int level;

            Level(int level) {
                this.level = level;
            }

            public int getLevel() {
                return level;
            }
        }

        private static final Pattern pattern = Pattern.compile("\\{\\}");
        private static final String formatString = "{}";
        private static final String javaFormatString = "%s";

        private static final ThreadLocal<SimpleDateFormat> dateFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        private static volatile BufferedWriter writer = null;
        private Helper helper;

        protected Logger(String className, Level level, boolean toFile, boolean toConsole, String toFilePath) {
            // String className, Level level, boolean toFile, boolean toConsole
            this.helper = new Helper(className, level, toFile, toConsole);
            // create single writer
            if(toFile && writer == null) {
                synchronized(Logger.class) {
                    this.initWriter();
                    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, r -> {
                        Thread thread = new Thread(r);
                        thread.setDaemon(true);
                        return thread;
                    });
                    scheduledThreadPoolExecutor.scheduleAtFixedRate(this::initWriter, PublicUtil.getCurrentAfterFixedTime("23_59_59").getTime() - System.currentTimeMillis(), 86400000, TimeUnit.MILLISECONDS);
                }
            }
        }

        void initWriter() {
            try {
                String fileName = toFilePath + "/" + Helper.getLogFileSuffix();
                File file = new File(fileName);
                if(!file.exists()) {
                    file.getParentFile().mkdirs();
                }
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
                writer.write("\r\n\r\n============================================ application start run time : " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        + " ============================================\r\n\r\n");
            } catch(IOException e) {
                e.printStackTrace();
            }

        }

        public void debug(Object... ps) {
            if(Level.DEBUG.getLevel() >= helper.level.getLevel()) {
                helper.println(Thread.currentThread().getName(), ps);
            }
        }

        public void info(Object... ps) {
            if(Level.INFO.getLevel() >= helper.level.getLevel()) {
                helper.println(Thread.currentThread().getName(), ps);
            }
        }

        public void warn(Object... ps) {
            if(Level.WARN.getLevel() >= helper.level.getLevel()) {
                helper.println(Thread.currentThread().getName(), ps);
            }
        }

        public void error(Object... ps) {
            if(Level.ERROR.getLevel() >= helper.level.getLevel()) {
                helper.println(Thread.currentThread().getName(), ps);
            }
        }

        private static class Helper {

            private String className;
            private Level level;
            private boolean toFile;
            private boolean toConsole;

            public Helper(String className, Level level, boolean toFile, boolean toConsole) {
                this.className = className;
                this.level = level;
                this.toFile = toFile;
                this.toConsole = toConsole;
            }

            public void println(String threadName, Object... ps) {
                if(ps.length > 0 && String.valueOf(ps[0]).contains(formatString)) {
                    String format = String.format(pattern.matcher(String.valueOf(ps[0])).replaceAll(javaFormatString),
                            Arrays.asList(ps).subList(1, ps.length).toArray());
                    singlePrintln(threadName, format);
                } else {
                    for(Object p : ps) {
                        singlePrintln(threadName, (p instanceof Throwable ? getDetailMessage((Throwable) p) : p));
                    }
                }
            }

            private void singlePrintln(String threadName, Object msg) {
                String s = new StringBuilder(1024).append(dateFormat.get().format(new Date())).append("\t  ").append(level).append("\t")
                        .append("  Thread:").append(threadName).append("\t").append(className).append("  ").append(msg).toString();
                if(this.toConsole) {
                    System.out.println(s);
                }
                if(this.toFile) {
                    try {
                        writer.write(s);
                        writer.newLine();
                        writer.flush();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            public static String getLogFileSuffix() {
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "_RUN.log";
            }

            private static String getDetailMessage(Throwable e) {
                ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                e.printStackTrace(new PrintWriter(arrayOutputStream, true));
                return arrayOutputStream.toString();
            }
        }
    }

}

