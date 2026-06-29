package com.java.myapp;

import java.io.BufferedReader;
import java.io.File;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WorkOrderProms {

    private static final String PROMS_BASE_URL = "https://proms.truecorp.co.th/proms/";
    private static final int PAGE_WAIT_SECONDS = 90;

    public static void main(String[] args) throws IOException {
        Dialog.setLAF();
        PathFolder Folder = new PathFolder();
        Dialog Dialog = new Dialog();
        String User_NCE;

        String Login;
        String ListWorkOrder;
        String Subject = "";
        String Detail = "";
        String daterequire = "";
        String Search = "";
        String Description = "";
        try {
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "taskkill /IM chromedriver.exe /F"});
            System.setProperty("webdriver.chrome.driver", Folder.getChromedriver());

            WebDriver driver = new ChromeDriver();

            Timer keepAwakeTimer = KeepAwakeMouseMover.start(driver);
            try {
                Download_ChromeDriver download = new Download_ChromeDriver();
                String chromeVersion = download.getChromeVersion();
                Process process = Runtime.getRuntime().exec("chromedriver --version");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                System.out.println(chromeVersion);
                while ((line = reader.readLine()) != null) {
                    if (!line.split("ChromeDriver ")[1].split(" ")[0].split("\\.")[0].equals(chromeVersion.split("\\.")[0])) {
                        driver.close();
                        Runtime.getRuntime().exec(new String[]{"cmd", "/c", "taskkill /IM chromedriver.exe /F"});
                        throw new IllegalStateException();
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            driver.get(PROMS_BASE_URL + "login");
            driver.manage().window().maximize();

            WebElement username = driver.findElement(By.id("username"));
            WebElement password = driver.findElement(By.id("password"));
            Login = Folder.getLogin();

            BufferedReader br_login = new BufferedReader(new FileReader(Login));
            String line_login;
            while ((line_login = br_login.readLine()) != null) {
                if (line_login.split(",")[0].contains("User")) {
                    username.sendKeys(line_login.split(",")[1]);
                } else if (line_login.split(",")[0].contains("Password")) {
                    password.sendKeys(line_login.split(",")[1]);
                    WebElement signInButton = driver.findElement(By.cssSelector("button.btn.btn-lg.btn-danger.btn-block"));
                    signInButton.click();
                    break;
                }
            }
            Thread.sleep(500);

            ListWorkOrder = Folder.getListWorkOrder();
            BufferedReader br_ListWorkOrder = new BufferedReader(new FileReader(ListWorkOrder));
            String line_ListWorkOrder;
            while ((line_ListWorkOrder = br_ListWorkOrder.readLine()) != null) {
                WebDriverWait wait = new WebDriverWait(driver, PAGE_WAIT_SECONDS);
                driver.get(PROMS_BASE_URL + "workorder/getWOTemplate");
                Thread.sleep(1000);
                WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("template_sos_type")));
                Select select = new Select(dropdown);
                select.selectByVisibleText("Core Online");
                Thread.sleep(1000);

                WebElement inputField = wait.until(ExpectedConditions.elementToBeClickable(By.id("siteNodeId")));
                inputField.sendKeys(line_ListWorkOrder.split(",")[0]);
                Thread.sleep(1000);
                WebElement messageElement = driver.findElement(By.id("site_msg2_text"));

                if (messageElement.getText().contains("Site/Node ID")) {
                    String site_found = new File(".").getCanonicalPath() + "\\site_found.txt";
                    FileWriter myWriter = new FileWriter(site_found, true);
                    myWriter.write(line_ListWorkOrder.split(",")[0] + "\n");
                    myWriter.close();
                } else {

                    WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Please complete first section')]")));
                    button.click();
                    Thread.sleep(1300);
                    WebElement spanElement = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(text(), 'ACC MPLS')]")));
                    spanElement.click();
                    Thread.sleep(1000);
                    dropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlProject")));
                    List<WebElement> options = dropdown.findElements(By.tagName("option"));
                    for (WebElement option : options) {
                        String projectId = option.getAttribute("value");
                        String projectName = option.getText();
                        if (line_ListWorkOrder.split(",")[1].equals(projectName.split(" ")[0])) {
                            dropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlProject")));
                            select = new Select(dropdown);
                            select.selectByValue(projectId);
                        }
                    }
                    Thread.sleep(1000);
                    WebElement companyDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlCompany")));
                    Select selectCompany = new Select(companyDropdown);
                    selectCompany.selectByVisibleText("TRUE MOVE H UNIVERSAL COMMUNICATION CO., LTD.");
                    Thread.sleep(1000);

                    button = wait.until(ExpectedConditions.elementToBeClickable(By.id("template_btn_template")));
                    button.click();
                    Thread.sleep(1000);
                    WebElement label = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("label[for='checkbox0']")));
                    label.click();
                    Thread.sleep(1000);
                    button = wait.until(ExpectedConditions.elementToBeClickable(By.id("btnWoType0")));
                    button.click();
                    Thread.sleep(1000);
                    label = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("label[for='checkbox0Act0']")));
                    label.click();
                    Thread.sleep(700);
                    label = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("label[for='checkbox0Act1']")));
                    label.click();
                    Thread.sleep(700);
                    label = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("label[for='checkbox0Act2']")));
                    label.click();
                    Thread.sleep(700);
                    label = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("label[for='checkbox0Act3']")));
                    label.click();
                    Thread.sleep(700);
                    label = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("label[for='checkbox0Act4']")));
                    label.click();
                    Thread.sleep(700);
                    WebElement dateInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("txtPlanStartDate0")));
                    dateInput.clear();
                    Thread.sleep(1000);
                    String day = line_ListWorkOrder.split(",")[2];
                    String month = line_ListWorkOrder.split(",")[3];
                    String year = line_ListWorkOrder.split(",")[4];
                    dateInput.sendKeys(day + "/" + month + "/" + year);
                    dateInput.sendKeys(Keys.TAB);
                    WebElement createWoButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@ng-click='createWo()']")));
                    createWoButton.click();
                    Thread.sleep(1000);
                    button = wait.until(ExpectedConditions.elementToBeClickable(By.id("confirmation_submit")));
                    button.click();
                    Thread.sleep(1000);
                    button = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.btn.btn-primary.ok")));
                    button.click();
                    Thread.sleep(1000);
                }
            }

            driver.quit();
            KeepAwakeMouseMover.stop(keepAwakeTimer);
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "taskkill /IM chromedriver.exe /F"});
            Thread.sleep(300);
            com.java.myapp.Dialog.Success();
        } catch (IllegalStateException ex) {
            new Wait();

            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "taskkill /IM chromedriver.exe /F"});
            Download_ChromeDriver download = new Download_ChromeDriver();
            String chromeVersion = download.getChromeVersion();
            String url = "https://googlechromelabs.github.io/chrome-for-testing/known-good-versions-with-downloads.json";
            String jsonString = download.readJsonFromUrl(url);

            int count = 0;
            for (int i = jsonString.split(",").length - 1; i >= 0; i--) {
                if (jsonString.split(",")[i].contains("/" + chromeVersion.split("\\.")[0] + ".") && jsonString.split(",")[i].contains("/win64/chromedriver-win64.zip")) {
                    System.out.println(jsonString.split(",")[i]);
                    count = i;
                    break;
                }
            }
            String zipFileUrl = jsonString.split(",")[count].split("\"")[3];
            String destinationFolder = Folder.getCurrent();
            String sourceFolderPath = Folder.getCurrent() + "\\chromedriver-win64\\";
            String fileName = "chromedriver.exe";

            download.downloadZipFile(zipFileUrl, destinationFolder);
            download.unzip(destinationFolder + File.separator + "chromedriver-win64.zip", destinationFolder);
            download.moveFile(sourceFolderPath, destinationFolder, fileName);

            String folderPath = Folder.getCurrent() + "\\chromedriver-win64\\";
            download.deleteFolder(new File(folderPath));
            File ZipChromedriver = new File(Folder.getZipChromedriver());
            ZipChromedriver.delete();
            Dialog.NewProgram();
            System.out.println("Please start a new program.");
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

}
