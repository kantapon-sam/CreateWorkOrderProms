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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CreateWorkOrderProms {

    private static final String PROMS_BASE_URL = "https://proms.truecorp.co.th/proms/";
    private static final By SIGN_IN_BUTTON_SELECTOR = By.xpath("//button[@type='submit' or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in')] | //input[(@type='submit' or @type='button') and contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in')]");
    private static final int CAPTCHA_WAIT_SECONDS = 180;
    private static final int PAGE_WAIT_SECONDS = 90;
    private static final int CHANGE_LOGIN_WAIT_SECONDS = 5;
    private static final String TARGET_WORK_ORDER_TYPE = "NT&IE";
    private static final boolean DRY_RUN = Boolean.getBoolean("createworkorderproms.dryRun");

    public static void main(String[] args) throws IOException {
        Dialog.setLAF();
        PathFolder Folder = new PathFolder();
        ProgressWindow progress = new ProgressWindow();
        progress.showWindow();
        final LoginManager.Credentials[] credentialsHolder = new LoginManager.Credentials[1];
        final StartupLoginEditGate loginEditGate = new StartupLoginEditGate(progress, credentialsHolder);

        String ListWorkOrder;
        try {
            progress.setStatus("Starting CreateWorkOrderProms " + AppVersion.displayVersion() + "...");
            progress.setEditLoginAction(new Runnable() {
                @Override
                public void run() {
                    loginEditGate.editUser();
                }
            });
            AutoUpdateChecker.checkForUpdate(progress);

            progress.setStatus("Checking saved login...");
            credentialsHolder[0] = loginEditGate.waitForCurrentEditToFinish();
            if (!hasCredentials(credentialsHolder[0])) {
                credentialsHolder[0] = LoginManager.loadOrPrompt();
            }
            if (!hasCredentials(credentialsHolder[0])) {
                progress.setStatus("Login cancelled.");
                return;
            }

            credentialsHolder[0] = loginEditGate.waitForOptionalEdit(CHANGE_LOGIN_WAIT_SECONDS);
            if (!hasCredentials(credentialsHolder[0])) {
                progress.setStatus("Login cancelled.");
                return;
            }

            progress.setStatus("Closing old ChromeDriver processes...");
            Process killProcess = Runtime.getRuntime().exec(new String[]{"cmd", "/c", "taskkill /IM chromedriver.exe /F"});
            killProcess.waitFor();

            Download_ChromeDriver download = new Download_ChromeDriver();
            download.ensureChromeDriver(Folder.getCurrent(), Folder.getChromedriver(), new Download_ChromeDriver.ProgressListener() {
                @Override
                public void onStatus(String message) {
                    progress.setStatus(message);
                }

                @Override
                public void onProgress(long bytesRead, long totalBytes) {
                    progress.setDownloadProgress(bytesRead, totalBytes);
                }
            });
            progress.setDownloadStatus("ChromeDriver ready");
            System.setProperty("webdriver.chrome.driver", Folder.getChromedriver());
            if (DRY_RUN) {
                progress.setStatus("Dry run mode: Create Work Order will not be submitted.");
            }

            credentialsHolder[0] = loginEditGate.waitForEditToFinish();
            progress.setEditLoginAction(null);
            if (!hasCredentials(credentialsHolder[0])) {
                progress.setStatus("Login cancelled.");
                return;
            }

            progress.setStatus("Opening Chrome...");
            WebDriver driver = new ChromeDriver();

            Timer keepAwakeTimer = KeepAwakeMouseMover.start(driver);
            progress.setStatus("Opening PROMs login page...");
            driver.get(PROMS_BASE_URL + "login");
            driver.manage().window().maximize();

            progress.setStatus("Logging in...");
            WebElement username = driver.findElement(By.id("username"));
            WebElement password = driver.findElement(By.id("password"));
            username.sendKeys(credentialsHolder[0].getUsername());
            password.sendKeys(credentialsHolder[0].getPassword());
            boolean loginCompletedByUser = waitForManualCaptchaIfNeeded(driver, progress);
            if (!loginCompletedByUser) {
                clickSignInOrWaitForLogin(driver, progress);
            } else {
                progress.setStatus("Login completed. Continuing...");
            }
            Thread.sleep(500);

            ListWorkOrder = System.getProperty("createworkorderproms.listWorkOrder", Folder.getListWorkOrder());
            List<String> workOrderLines = readWorkOrderLines(ListWorkOrder);
            progress.setWorkProgress(0, workOrderLines.size(), "Loaded " + workOrderLines.size() + " work order(s)");
            for (int workOrderNumber = 0; workOrderNumber < workOrderLines.size(); workOrderNumber++) {
                String line_ListWorkOrder = workOrderLines.get(workOrderNumber);
                String[] workOrderData = line_ListWorkOrder.split(",");
                if (workOrderData.length < 5) {
                    progress.setWorkProgress(workOrderNumber + 1, workOrderLines.size(), "Skipped invalid row " + (workOrderNumber + 1));
                    continue;
                }
                String siteId = workOrderData[0].trim();
                progress.setWorkProgress(workOrderNumber, workOrderLines.size(), "Processing " + siteId + " (" + (workOrderNumber + 1) + "/" + workOrderLines.size() + ")");
                WebDriverWait wait = new WebDriverWait(driver, PAGE_WAIT_SECONDS);
                progress.setStatus("Opening work order template for " + siteId + "...");
                driver.get(PROMS_BASE_URL + "workorder/getWOTemplate");
                Thread.sleep(1000);
                WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("template_sos_type")));
                Select select = new Select(dropdown);
                progress.setStatus("Selecting SOS type...");
                select.selectByVisibleText("Core Online");
                Thread.sleep(1000);

                progress.setStatus("Searching site " + siteId + "...");
                WebElement inputField = wait.until(ExpectedConditions.elementToBeClickable(By.id("siteNodeId")));
                inputField.sendKeys(siteId);
                Thread.sleep(1000);
                WebElement messageElement = driver.findElement(By.id("site_msg2_text"));

                if (messageElement.getText().contains("Site/Node ID")) {
                    File siteFoundFile = new File(Folder.getCurrent(), "site_found.txt");
                    FileWriter myWriter = new FileWriter(siteFoundFile, true);
                    myWriter.write(siteId + "\n");
                    myWriter.close();
                    progress.setWorkProgress(workOrderNumber + 1, workOrderLines.size(), "Site not found: " + siteId);
                } else {

                    progress.setStatus("Completing first section...");
                    WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Please complete first section')]")));
                    button.click();
                    Thread.sleep(1300);
                    progress.setStatus("Selecting ACC MPLS...");
                    WebElement spanElement = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(text(), 'ACC MPLS')]")));
                    spanElement.click();
                    Thread.sleep(1000);
                    progress.setStatus("Selecting project " + workOrderData[1].trim() + "...");
                    dropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlProject")));
                    selectProject(new Select(dropdown), workOrderData[1].trim());
                    Thread.sleep(1000);
                    progress.setStatus("Selecting company...");
                    Select selectCompany = waitForCompanyOptions(driver, wait);
                    selectCompany(selectCompany);
                    Thread.sleep(1000);

                    progress.setStatus("Loading work template...");
                    button = wait.until(ExpectedConditions.elementToBeClickable(By.id("template_btn_template")));
                    button.click();
                    Thread.sleep(1000);
                    progress.setStatus("Selecting only " + TARGET_WORK_ORDER_TYPE + "...");
                    String workOrderIndex = selectOnlyWorkOrderType(driver, wait, TARGET_WORK_ORDER_TYPE);
                    Thread.sleep(1000);
                    progress.setStatus("Setting plan start date...");
                    WebElement dateInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("txtPlanStartDate" + workOrderIndex)));
                    dateInput.clear();
                    Thread.sleep(1000);
                    String day = workOrderData[2].trim();
                    String month = workOrderData[3].trim();
                    String year = workOrderData[4].trim();
                    dateInput.sendKeys(day + "/" + month + "/" + year);
                    dateInput.sendKeys(Keys.TAB);
                    if (DRY_RUN) {
                        progress.setStatus("Dry run complete for " + siteId + ". Create Work Order was not submitted.");
                        progress.setWorkProgress(workOrderNumber + 1, workOrderLines.size(), "Dry run completed " + siteId);
                        continue;
                    }
                    progress.setStatus("Creating work order for " + siteId + "...");
                    WebElement createWoButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@ng-click='createWo()']")));
                    createWoButton.click();
                    Thread.sleep(1000);
                    progress.setStatus("Confirming work order...");
                    button = wait.until(ExpectedConditions.elementToBeClickable(By.id("confirmation_submit")));
                    button.click();
                    Thread.sleep(1000);
                    button = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.btn.btn-primary.ok")));
                    button.click();
                    Thread.sleep(1000);
                    progress.setWorkProgress(workOrderNumber + 1, workOrderLines.size(), "Completed " + siteId);
                }
            }

            progress.setStatus("Finished all work orders.");
            driver.quit();
            KeepAwakeMouseMover.stop(keepAwakeTimer);
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "taskkill /IM chromedriver.exe /F"});
            Thread.sleep(300);
            Dialog.Success();
        } catch (Exception ex) {
            progress.setStatus("Error: " + ex.toString());
            System.out.println(ex.toString());
            ex.printStackTrace(System.out);
        } finally {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            progress.closeWindow();
        }
    }

    private static List<String> readWorkOrderLines(String listWorkOrderPath) throws IOException {
        List<String> lines = new ArrayList<String>();
        File listWorkOrderFile = new File(listWorkOrderPath);
        if (!listWorkOrderFile.exists()) {
            listWorkOrderFile.createNewFile();
            return lines;
        }
        try ( BufferedReader reader = new BufferedReader(new FileReader(listWorkOrderPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private static boolean hasCredentials(LoginManager.Credentials credentials) {
        return credentials != null
                && credentials.getUsername() != null
                && !credentials.getUsername().trim().isEmpty()
                && credentials.getPassword() != null
                && !credentials.getPassword().trim().isEmpty();
    }

    private static class StartupLoginEditGate {

        private final ProgressWindow progress;
        private final LoginManager.Credentials[] credentialsHolder;
        private boolean editing;
        private boolean closed;
        private IOException editException;

        StartupLoginEditGate(ProgressWindow progress, LoginManager.Credentials[] credentialsHolder) {
            this.progress = progress;
            this.credentialsHolder = credentialsHolder;
        }

        void editUser() {
            synchronized (this) {
                if (closed) {
                    return;
                }
                editing = true;
            }

            try {
                progress.setStatus("Editing user. Waiting for OK...");
                LoginManager.Credentials credentials = LoginManager.editCredentials();
                synchronized (this) {
                    credentialsHolder[0] = credentials;
                }
                if (hasCredentials(credentials)) {
                    progress.setStatus("Login saved.");
                } else {
                    progress.setStatus("Login edit cancelled.");
                }
            } catch (IOException ex) {
                synchronized (this) {
                    editException = ex;
                }
                progress.setStatus("Cannot save login: " + ex.getMessage());
            } finally {
                synchronized (this) {
                    editing = false;
                    notifyAll();
                }
            }
        }

        LoginManager.Credentials waitForEditToFinish() throws IOException, InterruptedException {
            synchronized (this) {
                while (editing) {
                    wait();
                }
                closed = true;
                if (editException != null) {
                    throw editException;
                }
                return credentialsHolder[0];
            }
        }

        LoginManager.Credentials waitForOptionalEdit(int seconds) throws IOException, InterruptedException {
            long deadline = System.currentTimeMillis() + (seconds * 1000L);
            synchronized (this) {
                while (!editing && System.currentTimeMillis() < deadline) {
                    long remainingMs = deadline - System.currentTimeMillis();
                    long remainingSeconds = Math.max(1, (remainingMs + 999L) / 1000L);
                    progress.setStatus("Click Change Login to update user/password... " + remainingSeconds + "s");
                    wait(Math.min(1000L, remainingMs));
                }
            }
            return waitForCurrentEditToFinish();
        }

        LoginManager.Credentials waitForCurrentEditToFinish() throws IOException, InterruptedException {
            synchronized (this) {
                while (editing) {
                    wait();
                }
                if (editException != null) {
                    throw editException;
                }
                return credentialsHolder[0];
            }
        }
    }

    private static boolean waitForManualCaptchaIfNeeded(WebDriver driver, ProgressWindow progress) throws InterruptedException {
        if (isLoggedIn(driver)) {
            progress.setStatus("Already logged in. Continuing...");
            return true;
        }

        WebElement signInButton = findSignInButton(driver);
        if (!isCaptchaPresent(driver) && isDisplayedAndEnabled(signInButton)) {
            return false;
        }

        progress.setStatus("reCAPTCHA detected. Please verify it in Chrome.");

        long deadline = System.currentTimeMillis() + (CAPTCHA_WAIT_SECONDS * 1000L);
        while (System.currentTimeMillis() < deadline) {
            if (isLoggedIn(driver)) {
                progress.setStatus("Login completed in Chrome. Continuing...");
                return true;
            }
            signInButton = findSignInButton(driver);
            if (isDisplayedAndEnabled(signInButton)) {
                progress.setStatus("reCAPTCHA verified. Signing in...");
                return false;
            }
            long secondsLeft = Math.max(0, (deadline - System.currentTimeMillis()) / 1000L);
            progress.setStatus("Waiting for reCAPTCHA in Chrome... " + secondsLeft + "s");
            Thread.sleep(1000);
        }

        progress.setStatus("reCAPTCHA wait timed out. Trying Sign in anyway...");
        return isLoggedIn(driver);
    }

    private static void waitForLoginComplete(WebDriver driver, ProgressWindow progress) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 120000L;
        while (System.currentTimeMillis() < deadline) {
            if (isLoggedIn(driver)) {
                progress.setStatus("Login completed. Continuing...");
                return;
            }
            Thread.sleep(1000);
        }
        progress.setStatus("Login wait timed out. Continuing...");
    }

    private static void clickSignInOrWaitForLogin(WebDriver driver, ProgressWindow progress) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 300000L;
        while (System.currentTimeMillis() < deadline) {
            if (isLoggedIn(driver)) {
                progress.setStatus("Login completed. Continuing...");
                return;
            }

            WebElement signInButton = findSignInButton(driver);
            if (isDisplayedAndEnabled(signInButton)) {
                try {
                    progress.setStatus("Signing in...");
                    signInButton.click();
                    waitForLoginComplete(driver, progress);
                    return;
                } catch (StaleElementReferenceException ex) {
                    if (isLoggedIn(driver)) {
                        progress.setStatus("Login completed. Continuing...");
                        return;
                    }
                }
            }

            Thread.sleep(500);
        }
        progress.setStatus("Sign in wait timed out. Continuing...");
    }

    private static boolean isDisplayedAndEnabled(WebElement element) {
        if (element == null) {
            return false;
        }
        try {
            return element.isDisplayed() && element.isEnabled();
        } catch (StaleElementReferenceException ex) {
            return false;
        }
    }

    private static boolean isLoggedIn(WebDriver driver) {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl != null) {
            String lowerUrl = currentUrl.toLowerCase();
            if (lowerUrl.contains("/proms/home") || (lowerUrl.contains("/proms/") && !lowerUrl.contains("/login"))) {
                return true;
            }
        }
        return !driver.findElements(By.xpath("//*[contains(normalize-space(.), 'Sign Out')]")).isEmpty();
    }

    private static void selectProject(Select projectSelect, String projectCode) {
        List<String> availableOptions = new ArrayList<String>();
        for (WebElement option : projectSelect.getOptions()) {
            String text = option.getText();
            availableOptions.add(text);
            if (matchesProjectCode(text, projectCode)) {
                projectSelect.selectByValue(option.getAttribute("value"));
                return;
            }
        }
        throw new NoSuchElementException("Cannot locate project " + projectCode
                + ". Available examples: " + summarizeOptions(availableOptions));
    }

    private static boolean matchesProjectCode(String optionText, String projectCode) {
        if (optionText == null || projectCode == null) {
            return false;
        }
        String text = optionText.trim();
        String code = projectCode.trim();
        return text.equals(code)
                || text.startsWith(code + " ")
                || text.startsWith(code + "-")
                || text.startsWith(code + " -");
    }

    private static Select waitForCompanyOptions(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30000L;
        Select companySelect = null;
        while (System.currentTimeMillis() < deadline) {
            WebElement companyDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlCompany")));
            companySelect = new Select(companyDropdown);
            List<WebElement> options = companySelect.getOptions();
            if (hasRealOption(options)) {
                return companySelect;
            }
            Thread.sleep(500);
        }
        return companySelect;
    }

    private static boolean hasRealOption(List<WebElement> options) {
        for (WebElement option : options) {
            if (!isPlaceholderOption(option.getText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPlaceholderOption(String text) {
        String normalizedText = normalizeOptionText(text);
        return normalizedText.contains("PLEASE SELECT") || normalizedText.equals("--SELECT--");
    }

    private static void selectCompany(Select selectCompany) {
        WebElement trueMoveFallback = null;
        List<String> availableOptions = new ArrayList<String>();
        for (WebElement option : selectCompany.getOptions()) {
            String text = option.getText();
            availableOptions.add(text);
            String normalizedText = normalizeOptionText(text);
            if (normalizedText.contains("TRUE MOVE H")
                    && normalizedText.contains("UNIVERSAL")
                    && normalizedText.contains("COMMUNICATION")) {
                selectCompany.selectByValue(option.getAttribute("value"));
                return;
            }
            if (trueMoveFallback == null && normalizedText.contains("TRUE MOVE H")) {
                trueMoveFallback = option;
            }
        }
        if (trueMoveFallback != null) {
            selectCompany.selectByValue(trueMoveFallback.getAttribute("value"));
            return;
        }
        throw new NoSuchElementException("Cannot locate TRUE MOVE H company option. Available options: " + availableOptions);
    }

    private static String summarizeOptions(List<String> options) {
        List<String> summary = new ArrayList<String>();
        int limit = Math.min(12, options.size());
        for (int i = 0; i < limit; i++) {
            summary.add(options.get(i));
        }
        if (options.size() > limit) {
            summary.add("... total " + options.size() + " options");
        }
        return summary.toString();
    }

    private static String normalizeOptionText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim().toUpperCase();
    }

    private static WebElement findSignInButton(WebDriver driver) {
        List<WebElement> buttons = driver.findElements(SIGN_IN_BUTTON_SELECTOR);
        for (WebElement button : buttons) {
            try {
                if (button.isDisplayed()) {
                    return button;
                }
            } catch (StaleElementReferenceException ex) {
                // The login page changed while scanning buttons; retry on the next loop.
            }
        }
        return null;
    }

    private static boolean isCaptchaPresent(WebDriver driver) {
        return !driver.findElements(By.cssSelector("iframe[src*='recaptcha'], iframe[title*='reCAPTCHA']")).isEmpty()
                || !driver.findElements(By.cssSelector(".g-recaptcha, [class*='recaptcha']")).isEmpty();
    }

    private static String selectOnlyWorkOrderType(WebDriver driver, WebDriverWait wait, String workOrderType) {
        WebElement row = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//tr[.//*[contains(normalize-space(.), '" + workOrderType + "')]]")));
        WebElement targetCheckbox = row.findElement(By.xpath(
                ".//input[@type='checkbox' and starts-with(@id,'checkbox') and not(contains(@id,'Act'))]"));
        String targetCheckboxId = targetCheckbox.getAttribute("id");

        List<String> selectedCheckboxIds = new ArrayList<String>();
        List<WebElement> workOrderCheckboxes = driver.findElements(By.xpath(
                "//input[@type='checkbox' and starts-with(@id,'checkbox') and not(contains(@id,'Act'))]"));
        for (WebElement checkbox : workOrderCheckboxes) {
            String checkboxId = checkbox.getAttribute("id");
            if (checkboxId != null && !checkboxId.equals(targetCheckboxId) && checkbox.isSelected()) {
                selectedCheckboxIds.add(checkboxId);
            }
        }
        for (String checkboxId : selectedCheckboxIds) {
            setCheckbox(driver, wait, checkboxId, false);
        }

        setCheckbox(driver, wait, targetCheckboxId, true);
        return targetCheckboxId.substring("checkbox".length());
    }

    private static void setCheckbox(WebDriver driver, WebDriverWait wait, String checkboxId, boolean selected) {
        WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(checkboxId)));
        if (checkbox.isSelected() != selected) {
            WebElement label = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("label[for='" + checkboxId + "']")));
            label.click();
            wait.until(ExpectedConditions.elementSelectionStateToBe(By.id(checkboxId), selected));
        }
    }

}
