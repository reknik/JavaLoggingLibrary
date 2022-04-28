package net.gsdgroup.logging;


import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logger {

  private static Destination destination = Destination.FILE;
  private static int current_log;
  private static String directory_path;

  /**
   * Checks persistent file for last used {@link #directory_path directory
   * location}, If not found default path (C:\\LoggerLogs) is loaded.
   *
   * Sets the {@link #current_log current log} to the first available in the
   * current directory, Else creates a new log file.
   */
  static {
    current_log = 1;

    // Checks and creates persist file if not exists
    if (!Files.exists(Paths.get("persistF.txt"))) {
      try {
        Files.createFile(Paths.get("persistF.txt"));
      } catch (IOException e) {
        System.out.println("Failed to create persist because of " + e);
      }
    }

    // Checks for last used directory and switches to it
    // If not exists, defaults to C:\\LoggerLogs
    try {
      if (Files.size(Paths.get("persistF.txt")) == 0) {
        directory_path = "C:\\LoggerLogs";
        System.out.println(
            "Warning : default directory path used located at C:\\LoggerLogs, please use Logger.setDirectory() to change save location");
      } else {
        directory_path = Files.readAllLines(Paths.get("persistF.txt")).get(0);
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
              System.out.println("Failed to create directory because of " + e);
              System.out.println("Changing directory path to default C:\\LoggerLogs");
              directory_path = "C:\\LoggerLogs";
              if (!Files.exists(Paths.get(directory_path))) {
                  try {
                      Files.createDirectory(Paths.get(directory_path));
                  } catch (IOException e2) {
                      System.out.println("Failed to create directory because of " + e2);
                  }
              }
          }
      }

    //Gets first available log in directory
    try {
        while (Files.exists(Paths.get(directory_path, "log" + current_log + ".txt"))
            && Files.size(Paths.get(directory_path, "log" + current_log + ".txt")) > 5120000L) {
            current_log++;
        }
        if (!Files.exists(Paths.get(directory_path, "log" + current_log + ".txt"))) {
            Files.createFile(Paths.get(directory_path, "log" + current_log + ".txt"));
        }
    } catch (IOException e) {
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
          Paths.get(directory_path, "log" + current_log + ".txt"), StandardCharsets.US_ASCII,
          StandardOpenOption.APPEND)) {
        synchronized (Logger.class) {
          if (Files.size(Paths.get(directory_path, "log" + current_log + ".txt")) >= 5120000L) {
            current_log++;
            Files.createFile(Paths.get(directory_path, "log" + current_log + ".txt"));
          }
        }
        // Substitutes enter (\n) char for "~`~" for single line log
        writer.write(time + " " + level_s + " " + message.replaceAll("\r\n|\n|\r", "~`") + "\n");
      } catch (IOException e) {
        System.out.println("Request to add log failed at log file " + current_log + " because of " + e
            + "\n\n");
        System.out.println(time + " " + level.toString() + " " + message);
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
          Logger.add("NONE", LogLevel.INFO);
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
          Logger.add("NONE", LogLevel.DEBUG);
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
          Logger.add("NONE", LogLevel.WARN);
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
          Logger.add("NONE", LogLevel.ERROR);
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
          Logger.add("NONE", LogLevel.FATAL);
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
          Files.lines(Paths.get(directory_path, "log" + i_final + ".txt"))
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
        if (!Files.exists(Paths.get(directory_path, "LoggerLogs"))) {
            try {

                Files.createDirectory(Paths.get(directory_path, "LoggerLogs"));

                //Resets current log count
                current_log = 1;

                //Gets first available log in folder
                while (Files.exists(Paths.get(directory_path, "LoggerLogs", "log" + current_log + ".txt"))
                    && Files.size(Paths.get(directory_path, "log" + current_log + ".txt")) > 5120000L) {
                    current_log++;
                }

                //Checks if available log file exists
                if (!Files.exists(Paths.get(directory_path, "LoggerLogs", "log" + current_log + ".txt"))) {
                    Files.createFile(Paths.get(directory_path, "LoggerLogs", "log" + current_log + ".txt"));
                }

                Logger.directory_path = directory_path + "\\LoggerLogs";

                //Adds directory path in persist file
                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("persistF.txt"),
                    StandardCharsets.US_ASCII)) {
                    writer.write(Logger.directory_path);
                } catch (IOException e) {
                    System.out.println("Failed to write to persist because of : " + e);
                }
            } catch (IOException e) {
                System.out.println("Failed to create directory at specific path because of : " + e);
            }
        }

    } else
      System.out.println("Directory path given is NULL");

  }

}
