package net.gsdgroup.logging;


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logger {

  private final static String LOG = "log";
  private final static String LOGGER_LOGS = "LoggerLogs";
  private final static String TXT_EXTESION = ".txt";
  private final static String PERSIST = "persistLogger" + TXT_EXTESION;
  private final static String NO_MESSAGE = "NONE";

  private static Destination destination = Destination.CONSOLE;
  private static long maxLogSize = 5120000L;
  private static int current_log;
  private static String directory_path;
  private static boolean fsInialized = false;

  /**
   * Checks persistent file for last used {@link #directory_path directory
   * location}, If not found default path (C:\\LoggerLogs) is loaded.
   * <p>
   * Sets the {@link #current_log current log} to the first available in the
   * current directory, Else creates a new log file.
   */
  private static void initalizeFileSystem() {
    current_log = 1;

    // Checks and creates persist file if not exists
    if (!Files.exists(Paths.get(PERSIST))) {
      try {
        Files.createFile(Paths.get(PERSIST));
      } catch (IOException e) {
        System.out.println("Failed to create persist because of " + e);
      }
    }

    // Checks for last used directory and switches to it
    // If not exists, defaults to Working_Dir/LoggerLogs
    try {
      if (Files.size(Paths.get(PERSIST)) == 0) {
        directory_path = LOGGER_LOGS;
        System.out.println(
            "Warning : default directory path used located at " + Paths.get(".").toRealPath() + LOGGER_LOGS +
                ", please use Logger.setDirectory() to change save location");
      } else {
        directory_path = Files.readAllLines(Paths.get(PERSIST)).get(0);
      }
    } catch (IOException e) {
      System.out.println("Failed to set directory because of " + e);
    }

    //Checks if directory at directory path exists
    //If not, creates it
    //If failed to create, defaults directory path to C:\\LoggerLogs
    if (!Files.exists(Paths.get(directory_path))) {
      try {
        Files.createDirectory(Paths.get(directory_path));
      } catch (IOException e) {
        try {
          System.out.println("Failed to create directory because of " + e);
          System.out.println("Changing directory path to default " + Paths.get(".").toRealPath() + LOGGER_LOGS);
          directory_path = LOGGER_LOGS;
          Files.createDirectory(Paths.get(directory_path));
        } catch (IOException e2) {
          System.out.println("Failed to create directory because of " + e2);
        }
      }
    }
    //Gets first available log in directory
    try {
      while (Files.exists(Paths.get(directory_path, LOG + current_log + TXT_EXTESION))
          && Files.size(Paths.get(directory_path, LOG + current_log + TXT_EXTESION)) > maxLogSize) {
        current_log++;
      }
      if (!Files.exists(Paths.get(directory_path, LOG + current_log + TXT_EXTESION))) {
        Files.createFile(Paths.get(directory_path, LOG + current_log + TXT_EXTESION));
      }
    } catch (
        IOException e) {
      System.out.println("Failed to get current log because of " + e);
    }
  }

  /**
   * Adds a log to the current log file.
   *
   * @param message the message parameter, not null
   * @param level   the level parameter, not null
   */
  public static void add(String message, LogLevel level) {
    String level_s;
    if (level == null) {
      level_s = "NULL";
    } else {
      level_s = level.toString();
    }

    if (message == null) {
      message = "NULL";
    }
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy-M-d H:m:s.SSS"));

    if (Logger.destination == Destination.CONSOLE) {
      System.out.println(time + " " + level_s + " " + message);
    } else {
      // Checks if the current log file is bigger than permitted
      // If so then creates new log file


      try (BufferedWriter writer = Files.newBufferedWriter(
          Paths.get(directory_path, LOG + current_log + TXT_EXTESION), StandardCharsets.US_ASCII,
          StandardOpenOption.APPEND)) {
        synchronized (Logger.class) {
          if (Files.size(Paths.get(directory_path, LOG + current_log + TXT_EXTESION)) >= maxLogSize) {
            current_log++;
            Files.createFile(Paths.get(directory_path, LOG + current_log + TXT_EXTESION));
          }
        }
        // Substitutes enter (\n) char for "~`~" for single line log
        writer.write(time + " " + level_s + " " + message.replaceAll("\r\n|\n|\r", "~`") + "\n");
      } catch (IOException e) {
        System.out.println("Request to add log failed at log file " + current_log + " because of " + e
            + "\n\n");
        assert level != null;
        System.out.println(time + " " + level + " " + message);
      }
    }
  }

  /**
   * Adds INFO log to current log file by calling the static method
   * {@link #add(String, LogLevel) add}.
   *
   * @param message the message parameter, not null
   */
  public static void info(String message) {
    if (message != null) {
      Logger.add(message, LogLevel.INFO);
    } else {
      Logger.add(NO_MESSAGE, LogLevel.INFO);
    }
  }

  /**
   * Adds DEBUG log to current log file by calling the static method
   * {@link #add(String, LogLevel) add}.
   *
   * @param message the message parameter, not null
   */
  public static void debug(String message) {
    if (message != null) {
      Logger.add(message, LogLevel.DEBUG);
    } else {
      Logger.add(NO_MESSAGE, LogLevel.DEBUG);
    }
  }

  /**
   * Adds WARN log to current log file by calling the static method
   * {@link #add(String, LogLevel) add}.
   *
   * @param message the message parameter, not null
   */
  public static void warn(String message) {
    if (message != null) {
      Logger.add(message, LogLevel.WARN);
    } else {
      Logger.add(NO_MESSAGE, LogLevel.WARN);
    }
  }

  /**
   * Adds ERROR log to current log file by calling the static method
   * {@link #add(String, LogLevel) add}.
   *
   * @param message the message parameter, not null
   */
  public static void error(String message) {
    if (message != null) {
      Logger.add(message, LogLevel.ERROR);
    } else {
      Logger.add(NO_MESSAGE, LogLevel.ERROR);
    }
  }

  /**
   * Adds FATAL log to current log file by calling the static method
   * {@link #add(String, LogLevel) add}.
   *
   * @param message the message parameter, not null
   */
  public static void fatal(String message) {
    if (message != null) {
      Logger.add(message, LogLevel.FATAL);
    } else {
      Logger.add(NO_MESSAGE, LogLevel.FATAL);
    }
  }

  /**
   * Gets logs based of string specifier.
   *
   * @param spec, not null
   */
  private static void getLogs(String spec) {
    int i = 1;
    ExecutorService service = Executors.newCachedThreadPool();
    for (; i <= current_log; i++) {
      int i_final = i;
      service.submit(() -> {
        try {
          // Gets every line of the log containing specified time then formats it for the
          // user
          Files.lines(Paths.get(directory_path, LOG + i_final + TXT_EXTESION))
              .unordered()
              .parallel()
              .forEach(s ->
              {
                if (s.contains(spec)) {
                  System.out.println(s.replaceAll("~`~`|~`", "\n") + "\n");
                }
              })
          ;
        } catch (IOException e) {
          System.out.println("Request to get logs failed at log file " + i_final + " because of " + e);
        }
      });
    }
    service.shutdown();
  }

  /**
   * Gets all logs based on specified input time. The format is yy-M-d H:m:s.SSS
   * It accepts partial input
   *
   * @param time the time parameter, not null
   */
  public static void getAllLogs(String time) {
    if (time == null) {
      time = "NULL";
      System.out.println("Requested a NULL time\n");
    }
    getLogs(time);
  }

  /**
   * Gets all logs based on specified log level.
   *
   * @param value the value parameter, not null
   */
  public static void getAllLogs(LogLevel value) {
    if (value == null) {
      getLogs("NULL");
      System.out.println("Requested a NULL level\n");
    } else {
      getLogs(value.toString());
    }
  }

  /**
   * Sets the log destination.
   *
   * @param d the destination parameter, not null
   */
  public synchronized static void setDestination(Destination d) {
    if (d != null) {
      Logger.destination = d;
      if (d.equals(Destination.FILE) && !fsInialized) {
        initalizeFileSystem();
        fsInialized = true;
      }
    } else {
      System.out.println("Destination doesn't exist, operation did not complete");
    }
  }

  /**
   * Sets directory path to given string argument.
   *
   * @param directory_path, not null
   */
  public static synchronized void setDirectory(String directory_path) {
    if (directory_path != null) {

      //Checks if LoggerLogs directory exists at given path
      if (!Files.exists(Paths.get(directory_path, LOGGER_LOGS))) {
        try {

          Files.createDirectory(Paths.get(directory_path, LOGGER_LOGS));

          //Resets current log count
          current_log = 1;

          //Gets first available log in folder
          while (Files.exists(Paths.get(directory_path, LOGGER_LOGS, LOG + current_log + TXT_EXTESION))
              && Files.size(Paths.get(directory_path, LOG + current_log + TXT_EXTESION)) > maxLogSize) {
            current_log++;
          }

          //Checks if available log file exists
          if (!Files.exists(Paths.get(directory_path, LOGGER_LOGS, LOG + current_log + TXT_EXTESION))) {
            Files.createFile(Paths.get(directory_path, LOGGER_LOGS, LOG + current_log + TXT_EXTESION));
          }

          Logger.directory_path = directory_path + File.separator + LOGGER_LOGS;

          //Adds directory path in persist file
          try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(PERSIST),
              StandardCharsets.US_ASCII)) {
            writer.write(Logger.directory_path);
          } catch (IOException e) {
            System.out.println("Failed to write to persist because of : " + e);
          }
        } catch (IOException e) {
          System.out.println("Failed to create directory at specific path because of : " + e);
        }
      }

    } else {
      System.out.println("Directory path given is NULL");
    }
  }

  /**
   * Sets a new log file maximum size
   *
   * @param newSize , not null
   */
  public static void setMaxLogSize(long newSize) {
    if (newSize > 1L) {
      maxLogSize = newSize;
    } else {
      System.out.println("Couldn't set new max log size to " + newSize);
    }
  }
}
