package com.essheva;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public enum Configuration {

    INSTANCE;

    static final String resourceDirPath = "src/main/resources";
    static final char PROPERTY_VALUES_SEPARATOR = ';';

    private final Set<String> receiptNumbers;
    private final boolean sendMail;
    private final String driverPath;
    private final Path outputFile;
    private final int periodInHours;

    private Properties mailProps;

    Configuration() {
        Properties props = new Properties();
        try {
            props.load(getReader(resourceDirPath + "/app.properties"));
        } catch (IOException e) {
            System.out.println("Error occurred during execution " + e.getMessage());
        }
        driverPath = getWebDriverFolderPathByOS();
        outputFile = Paths.get(resourceDirPath + "/" + getValue(props, "output.file"));

        receiptNumbers = Arrays.stream(spiltValues(getValue(props, "receipt.numbers"))).
                distinct().
                collect(Collectors.toSet());

        periodInHours = Integer.valueOf(getValue(props,"check.period"));
        sendMail = Boolean.valueOf(getValue(props, "mail.send"));

        if (sendMail) {
            mailProps = new Properties();
            try {
                mailProps.load(getReader(resourceDirPath + "/user.secret"));
            } catch (IOException e) {
                System.out.println("Error occurred during execution " + e.getMessage());
            }
            if (!mailProps.containsKey("mail.smtp.host")
                    || !mailProps.containsKey("mail.imap.host")
                    || !mailProps.containsKey("mail.pop3.host")) {
                try {
                    mailProps.load(getReader(resourceDirPath + "/mail.properties"));
                } catch (IOException e) {
                    System.out.println("Error occurred during execution " + e.getMessage());
                }
            }

            if (!mailProps.containsKey("mail.from.user") || !mailProps.containsKey("mail.from.password")) {
                throw new IllegalArgumentException("You should configure authentication mail server credentials. " +
                        "Please, set 'mail.from.user' and 'mail.from.password'");
            }
        }
    }

    String getDriverPath() {
        return driverPath;
    }

    Set<String> getReceiptNumbers() {
        return receiptNumbers;
    }

    Properties getMailProps() {
        return mailProps;
    }

    Path getOutputFile() {
        return outputFile;
    }

    boolean isSendMail() {
        return sendMail;
    }

    int getPeriodInHours() { return periodInHours; }

    private String getWebDriverFolderPathByOS() {
        String os = System.getProperty("os.name").toLowerCase();
        final String dirName;
        if (os.contains("win")) {
            dirName = "win32";
        } else if (os.contains("mac")) {
            dirName = "mac64";
        } else if (os.contains("nux")) {
            dirName = "linux64";
        } else {
            throw new UnsupportedOperationException(os + " is not supported");
        }
        return resourceDirPath + "/chomedriver/" + dirName + "/chromedriver.exe";
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "receiptNumbers=" + receiptNumbers +
                ", periodInHours=" + periodInHours +
                ", outputFile=" + outputFile +
                ", sendMail=" + sendMail +
                '}';
    }

    private String[] spiltValues(String value) {
        return value.split("\\s*" + PROPERTY_VALUES_SEPARATOR + "\\s*");
    }

    private String getValue(Properties props, String s)  {
        final String value = props.getProperty(s);
        if (value == null) {
            throw new IllegalArgumentException("Property not set " + s);
        }
        return value;
    }

    private static FileReader getReader(String resource) throws FileNotFoundException {
        return new FileReader(Paths.get(resource).toFile());
    }
}
