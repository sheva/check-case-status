package com.essheva;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class CheckUSCISStatus {

    private static WebDriver driver;
    private final Configuration conf;
    private final Set<ReceiptRecord> receipts;

    private CheckUSCISStatus() throws IOException {
        conf = Configuration.INSTANCE;
        receipts = loadReceipts(conf.getReceiptNumbers());
        System.setProperty("webdriver.chrome.driver", get(conf.getDriverPath()).toFile().getAbsolutePath());
    }

    public static void main(String[] args) throws IOException {
        CheckUSCISStatus status = new CheckUSCISStatus();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println(String.format("--- Status check launched at %s. ---", getNowFormatted()));
            try {
                driver = new ChromeDriver();
                status.perform();
            } catch (Exception e) {
                System.err.println("Something wrong occurred. " + e.getMessage());
            } finally {
                driver.quit();
            }
        }, 0, status.conf.getPeriodInHours(), TimeUnit.HOURS);
    }

    private void perform() throws IOException, MessagingException {
        retrieveStatusFromUSCIS(receipts);
        flushResultsToFile(receipts, conf.getOutputFile());
        if (conf.isSendMail()) {
            sendEmail(receipts);
        }
    }

    private void retrieveStatusFromUSCIS(Set<ReceiptRecord> receiptCollection) {
        receiptCollection.forEach(this::launchStatusCheck);
    }

    private void flushResultsToFile(Set<ReceiptRecord> receipts, Path outputFile) throws IOException {
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(outputFile, CREATE, APPEND))) {

            String formatDateTime = getNowFormatted(), lineSeparator = System.getProperty("line.separator");
            out.write(String.format("--- Status check performed at %s. ---%s", formatDateTime, lineSeparator).getBytes());

            receipts.forEach(r -> {
                try {
                    out.write((r.toString() + lineSeparator).getBytes());
                } catch (IOException e) {
                    System.err.println(String.format("Attempt to write to output file '%s' failed. %s",
                            outputFile.getFileName(), e.getMessage()));
                }
            });
        }
    }

    private void sendEmail(Set<ReceiptRecord> receipts) throws MessagingException {
        Properties props = conf.getMailProps();
        final String from = props.getProperty("mail.from.user");
        InternetAddress fromAddress = new InternetAddress(from);
        InternetAddress[] toAddress;
        final String toList = props.getProperty("mail.to");
        if (toList != null) {
            toAddress = Arrays.stream(toList.split("\\s*" + Configuration.PROPERTY_VALUES_SEPARATOR + "\\s*")).
                    map(address -> {
                        try {
                            return new InternetAddress(address);
                        } catch (AddressException e) {
                            throw new RuntimeException(e);
                        }
                    }).toArray(InternetAddress[]::new);
        } else {
            System.out.println("Using 'mail.from.user' as mail recipient address.");
            toAddress = new InternetAddress[]{ new InternetAddress(from)};
        }

        Session session = Session.getDefaultInstance(props, new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, props.getProperty("mail.from.password"));
            }
        });
        Message message = new MimeMessage(session);

        message.setFrom(fromAddress);
        message.addRecipients(Message.RecipientType.TO, toAddress);
        message.setSubject("Status Update on receipts");

        StringBuilder builder = new StringBuilder();
        builder.append("<table style=\"border: 1px solid black; padding: 2px; width:100%;border-collapse: collapse;\">").
                append("<tr style=\"color: green\">" +
                            "<th>Owner</th><th>Receipt Number</th><th>Main Status</th><th>Description</th>" +
                        "</tr>");
        for (ReceiptRecord receipt : receipts) {
            builder.append("<tr>");
            builder.
                    append("<td style=\"border: 1px solid black\">").append(receipt.getOwner()).append("</td>").
                    append("<td style=\"border: 1px solid black\">").append(receipt.getReceiptNumber()).append("</td>").
                    append("<td style=\"border: 1px solid black\">").append(receipt.getMainStatus()).append("</td>").
                    append("<td style=\"border: 1px solid black\">").append(receipt.getDescription()).append("</td>");
            builder.append("</tr>");
        }
        builder.append("</table>");

        String sb = "<h4 class=\"green\">" + message.getSubject() + "</h4 >" +
                "<p>" + builder.toString() + "</p>";
        message.setContent(sb, "text/html; charset=utf-8");
        message.saveChanges();

        Transport.send(message, toAddress);
    }

    private Set<ReceiptRecord> loadReceipts(Set<String> receiptNumbers) throws IOException {
        Set<ReceiptRecord> receipts = new HashSet<>();
        for (String line : receiptNumbers) {
            String[] data = line.split("\\|");
            if (data.length != 2) {
                throw new InvalidPropertiesFormatException("File corrupted.");
            }
            receipts.add(new ReceiptRecord(data[0].trim(), data[1].trim()));
        }
        return receipts;
    }

    private void launchStatusCheck(ReceiptRecord receiptRecord) {
        driver.get("https://egov.uscis.gov/casestatus/landing.do");
        Wait<WebDriver> wait = new WebDriverWait(driver, 60);

        driver.findElement(By.id("receipt_number")).sendKeys(receiptRecord.getReceiptNumber());
        driver.findElement(By.name("initCaseSearch")).click();

        wait.until((WebDriver webDriver) -> {
            try {
                WebElement statusEl = webDriver.findElement(By.cssSelector("div.main-row h1"));
                receiptRecord.setMainStatus(statusEl.getText());
                WebElement descriptionEl = webDriver.findElement(By.cssSelector("div.main-row p"));
                receiptRecord.setDescription(descriptionEl.getText());
                return true;
            } catch (NoSuchElementException e) {
                return false;
            }
        });
    }

    private static String getNowFormatted() {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return localDateTime.format(formatter);
    }
}

