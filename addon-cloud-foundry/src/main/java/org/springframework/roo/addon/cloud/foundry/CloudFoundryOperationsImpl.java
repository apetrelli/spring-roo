package org.springframework.roo.addon.cloud.foundry;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.osgi.AbstractFlashingObject;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.StringUtils;

import com.vmware.appcloud.client.AppCloudClient;
import com.vmware.appcloud.client.ApplicationStats;
import com.vmware.appcloud.client.CloudApplication;
import com.vmware.appcloud.client.CloudInfo;
import com.vmware.appcloud.client.CloudService;
import com.vmware.appcloud.client.CrashInfo;
import com.vmware.appcloud.client.CrashesInfo;
import com.vmware.appcloud.client.InstanceStats;
import com.vmware.appcloud.client.InstancesInfo;
import com.vmware.appcloud.client.ServiceConfiguration;

/**
 * Operations for Cloud Foundry add-on. TODO Move the table rendering stuff out
 * to a separate class in org.sfw.shell so can be used elsewhere; feel free to
 * try using it in AddOnOperationsImpl (talk to him)
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
@Component
@Service
public class CloudFoundryOperationsImpl extends AbstractFlashingObject
        implements CloudFoundryOperations {

    // Constants
    private static final Logger LOGGER = HandlerUtils
            .getLogger(CloudFoundryOperationsImpl.class);

    // Fields
    @Reference private CloudFoundrySession session;
    private AppCloudClient client;

    public void info() {
        executeCommand(new CloudCommand(
                "Cloud information failed to be retrieved.") {
            @Override
            public void execute() throws Exception {
                CloudInfo cloudInfo = client.getCloudInfo();
                if (cloudInfo == null || cloudInfo.getUsage() == null
                        || cloudInfo.getLimits() == null) {
                    LOGGER.warning("Information could not be retrieved");
                    return;
                }
                LOGGER.info("\n");
                LOGGER.info(cloudInfo.getDescription());
                LOGGER.info("For support visit " + cloudInfo.getSupport());
                LOGGER.info("\n");
                LOGGER.info("Target:\t " + client.getCloudControllerUrl()
                        + " (" + cloudInfo.getVersion() + ")");
                LOGGER.info("\n");
                LOGGER.info("User:\t " + cloudInfo.getUser());
                LOGGER.info("Usage:\t Memory ("
                        + cloudInfo.getUsage().getTotalMemory() + "MB of "
                        + cloudInfo.getLimits().getMaxTotalMemory()
                        + "MB total)");
                LOGGER.info("\t Services ("
                        + cloudInfo.getUsage().getServices() + " of "
                        + cloudInfo.getLimits().getMaxServices() + " total)");
                LOGGER.info("\t Apps (" + cloudInfo.getUsage().getApps()
                        + " of " + cloudInfo.getLimits().getMaxApps()
                        + " total)");
                LOGGER.info("\n");
            }
        });
    }

    public void register(final String email, final String password) {
        String successMessage = "Registration was successful";
        String failureMessage = "Registration failed";
        executeCommand(new CloudCommand(failureMessage, successMessage) {
            @Override
            public void execute() throws Exception {
                client.register(email, password);
            }
        });
    }

    public void login(final String email, final String password,
            final String cloudControllerUrl) {
        executeCommand(new CloudCommand("Login failed") {
            @Override
            public void execute() {
                session.login(email, password, cloudControllerUrl);
                client = session.getClient();
            }
        });
    }

    public void services() {
        executeCommand(new CloudCommand("Services could not be retrieved") {
            @Override
            public void execute() throws Exception {
                List<ServiceConfiguration> globalServices = client
                        .getServiceConfigurations();
                List<CloudService> localServices = client.getServices();
                if (globalServices.isEmpty()) {
                    LOGGER.info("There are currently no services available.");
                }
                else {
                    ShellTableRenderer table = new ShellTableRenderer(
                            "System Services", "Service", "Version",
                            "Description");
                    for (ServiceConfiguration service : globalServices) {
                        table.addRow(service.getVendor(), service.getVersion(),
                                service.getDescription());
                    }
                    LOGGER.info(table.getOutput());
                }

                if (localServices.isEmpty()) {
                    LOGGER.info("There are currently no provisioned services.");
                }
                else {
                    ShellTableRenderer table = new ShellTableRenderer(
                            "Provisioned Services", "Name", "Service");

                    for (CloudService service : localServices) {
                        table.addRow(service.getName(), service.getVendor());
                    }
                    LOGGER.info(table.getOutput());
                }
            }
        });
    }

    public void createService(final String service, final String name,
            final String bind) {
        String failureMessage = "The service '" + name
                + "' failed to be created";
        String successMessage = "The service '" + name
                + "' was successfully created";
        executeCommand(new CloudCommand(failureMessage, successMessage) {
            @Override
            public void execute() throws Exception {
                CloudService cloudService = new CloudService();
                cloudService.setName(name);
                cloudService.setTier("free");
                List<ServiceConfiguration> serviceConfigurations = client
                        .getServiceConfigurations();
                for (ServiceConfiguration serviceConfiguration : serviceConfigurations) {
                    if (serviceConfiguration.getVendor().equals(service)) {
                        cloudService
                                .setVendor(serviceConfiguration.getVendor());
                        cloudService.setType(serviceConfiguration.getType());
                        cloudService.setVersion(serviceConfiguration
                                .getVersion());
                    }
                }
                client.createService(cloudService);
            }
        });
    }

    public void deleteService(final String service) {
        String failureMessage = "The service '" + service
                + "' failed to be deleted";
        String successMessage = "The service '" + service
                + "' was deleted successfully";
        executeCommand(new CloudCommand(failureMessage, successMessage) {
            @Override
            public void execute() throws Exception {
                client.deleteService(service);
            }
        });
    }

    public void bindService(final String service, final String appName) {
        String failureMessage = "The binding of the service '" + service
                + "' to the application '" + appName + "' failed";
        String successMessage = "The service '" + service
                + "' was successfully bound to the application '" + appName
                + "'";
        executeCommand(new CloudCommand(failureMessage, successMessage) {
            @Override
            public void execute() throws Exception {
                client.bindService(appName, service);
            }
        });
    }

    public void unbindService(final String service, final String appName) {
        String failureMessage = "The unbinding of the service '" + service
                + "' from the application '" + appName + "' failed";
        String successMessage = "The service '" + service
                + "' was successfully unbound from the application '" + appName
                + "'";
        executeCommand(new CloudCommand(failureMessage, successMessage) {
            @Override
            public void execute() throws Exception {
                client.unbindService(appName, service);
            }
        });
    }

    public void apps() {
        executeCommand(new CloudCommand("The applications failed to be listed.") {
            @Override
            public void execute() throws Exception {
                List<CloudApplication> applications = client.getApplications();
                if (applications.isEmpty()) {
                    LOGGER.info("No applications available.");
                    return;
                }
                ShellTableRenderer table = new ShellTableRenderer(
                        "Applications", "Name", "Status", "Instances",
                        "Services", "URLs");
                for (CloudApplication application : applications) {
                    StringBuilder uris = new StringBuilder();
                    for (int i = 0; i < application.getUris().size(); i++) {
                        uris.append(application.getUris().get(i));
                        if (i < application.getUris().size() - 1) {
                            uris.append(", ");
                        }
                    }
                    StringBuilder services = new StringBuilder();
                    for (String service : application.getServices()) {
                        services.append(service);
                    }
                    table.addRow(application.getName(), application.getState()
                            .name(),
                            String.valueOf(application.getInstances()),
                            services.toString(), uris.toString());
                }
                LOGGER.info(table.getOutput());
            }
        });
    }

    public void push(final String appName, final Integer instances,
            Integer memory, final String path, final List<String> urls) {
        if (path == null) {
            LOGGER.severe("The file path cannot be null; cannot continue");
            return;
        }
        File fileToDeploy = new File(path);
        if (!fileToDeploy.exists()) {
            LOGGER.severe("The file at path '" + path
                    + "' doesn't exist; cannot continue");
            return;
        }
        if (memory == null) {
            memory = 256;
        }
        String failureMessage = "The application '" + appName
                + "' could not be pushed";
        String successMessage = "The application '" + appName
                + "' was successfully pushed";
        final Integer finalMem = memory;
        executeCommand(new CloudCommand(failureMessage, successMessage,
                "Uploading") {
            @Override
            public void execute() throws Exception {
                CloudApplication cloudApplication = getApplication(appName);
                List<String> finalUrls = urls;
                if (finalUrls == null) {
                    finalUrls = new ArrayList<String>();
                    finalUrls.add(appName + ".cloudfoundry.com");
                }
                if (cloudApplication == null) {
                    client.createApplication(appName, CloudApplication.SPRING,
                            finalMem, finalUrls, null, false);
                }
                client.uploadApplication(appName, path);
                Integer finalInstances = instances;
                if (finalInstances == null) {
                    finalInstances = 1;
                    if (cloudApplication != null) {
                        finalInstances = cloudApplication.getInstances();
                    }
                }
                client.updateApplicationInstances(appName, finalInstances);
            }
        });
    }

    public void start(final String appName) {
        String failureMessage = "The application '" + appName
                + "' could not be started";
        String successMessage = "The application '" + appName
                + "' was successfully started";
        executeCommand(new CloudCommand(failureMessage, successMessage,
                "Starting") {
            @Override
            public void execute() throws Exception {
                if (getApplication(appName).getState() == CloudApplication.AppState.STARTED) {
                    LOGGER.info("Application '" + appName
                            + "' is already running.");
                    displaySuccessMessage = false;
                    return;
                }
                client.startApplication(appName);
            }
        });
    }

    public void stop(final String appName) {
        String failureMessage = "The application '" + appName
                + "' could not be stopped";
        String successMessage = "The application '" + appName
                + "' was successfully stopped";
        executeCommand(new CloudCommand(failureMessage, successMessage, appName) {
            @Override
            public void execute() throws Exception {
                if (getApplication(appName).getState() == CloudApplication.AppState.STOPPED) {
                    LOGGER.info("Application '" + appName + "' is not running.");
                    displaySuccessMessage = false;
                    return;
                }
                client.stopApplication(appName);
            }
        });
    }

    public void restart(final String appName) {
        String failureMessage = "The application '" + appName
                + "' could not be restarted";
        String successMessage = "The application '" + appName
                + "' was successfully restarted";
        executeCommand(new CloudCommand(failureMessage, successMessage) {
            @Override
            public void execute() throws Exception {
                client.restartApplication(appName);
            }
        });
    }

    public void delete(final String appName) {
        String failureMessage = "The application '" + appName
                + "' could not be deleted";
        String successMessage = "The application '" + appName
                + "' was successfully deleted";
        executeCommand(new CloudCommand(failureMessage, successMessage) {
            @Override
            public void execute() throws Exception {
                client.deleteApplication(appName);
            }
        });
    }

    public void update(final String appName) {

    }

    public void instances(final String appName, final String number) {
        executeCommand(new CloudCommand(
                "Retrieving instances for application '" + appName + "' failed") {
            @Override
            public void execute() throws Exception {
                Integer instances = getInteger(number);
                if (instances == null) {
                    InstancesInfo instancesInfo = client
                            .getApplicationInstances(appName);
                    if (instancesInfo.getInstances().isEmpty()) {
                        LOGGER.info("No running instances for '" + appName
                                + "'");
                    }
                }
                else {
                    client.updateApplicationInstances(appName, instances);
                }
            }
        });
    }

    public void mem(final String appName, final Integer memSize) {
        executeCommand(new CloudCommand(
                "Updating the memory allocation for application '" + appName
                        + "' failed") {
            @Override
            public void execute() throws Exception {
                if (memSize != null) {
                    client.updateApplicationMemory(appName, memSize);
                }
                ShellTableRenderer shellTable = new ShellTableRenderer(
                        "Application Memory", "Name", "Memory");
                shellTable.addRow(appName, getApplication(appName).getMemory()
                        + "MB");
                LOGGER.info(shellTable.getOutput());
            }
        });
    }

    public void crashes(final String appName) {
        executeCommand(new CloudCommand("Crashes for application '" + appName
                + "' could not be retrieved") {
            @Override
            public void execute() throws Exception {
                CrashesInfo crashes = client.getCrashes(appName);
                if (crashes == null) {
                    LOGGER.severe(this.failureMessage);
                    return;
                }
                if (crashes.getCrashes().isEmpty()) {
                    LOGGER.info("The application '" + appName
                            + "' has never crashed");
                    return;
                }
                ShellTableRenderer table = new ShellTableRenderer("Crashes",
                        "Name", "Id", "Since");
                for (CrashInfo crash : crashes.getCrashes()) {
                    table.addRow(appName, crash.getInstance(), SimpleDateFormat
                            .getDateTimeInstance().format(crash.getSince()));
                }
                LOGGER.info(table.getOutput());
            }
        });
    }

    public void crashLogs(final String appName, final String instance) {
        logs(appName, instance);
    }

    public void logs(final String appName, final String instance) {
        String failureMessage = "The logs for application '" + appName
                + "' failed to be retrieved";
        String successMessage = null;
        executeCommand(new CloudCommand(failureMessage, successMessage,
                "Loading") {
            @Override
            public void execute() throws Exception {
                Integer instanceIndex = getInteger(instance);
                if (instanceIndex == null) {
                    instanceIndex = 1;
                }

                String stderrLog = client.getFile(appName, instanceIndex,
                        "logs/stderr.log");
                String stdoutlog = client.getFile(appName, instanceIndex,
                        "logs/stdout.log");

                LOGGER.info("\n");
                LOGGER.info("==== logs/stderr.log ====");
                LOGGER.info("\n");
                LOGGER.info(stderrLog);

                LOGGER.info("\n");
                LOGGER.info("==== logs/stdout.log ====");
                LOGGER.info("\n");
                LOGGER.info(stdoutlog);
                LOGGER.info("\n");
            }
        });
    }

    public void files(final String appName, final String path,
            final String instance) {
        executeCommand(new CloudCommand("The files failed to be retrieved") {
            @Override
            public void execute() throws Exception {
                Integer instanceIndex = getInteger(instance);
                if (instanceIndex == null) {
                    instanceIndex = 1;
                }
                String file = client.getFile(appName, instanceIndex, path);
                LOGGER.info(file);
            }
        });
    }

    public void stats(final String appName) {
        executeCommand(new CloudCommand("The stats for application '" + appName
                + "' failed to be retrieved") {
            @Override
            public void execute() throws Exception {
                ApplicationStats stats = client.getApplicationStats(appName);
                if (stats.getRecords().isEmpty()) {
                    LOGGER.info("There is currently no stats for the application '"
                            + appName + "'");
                    return;
                }
                ShellTableRenderer table = new ShellTableRenderer("App. Stats",
                        "Instance", "CPU (Cores)", "Memory (limit)",
                        "Disk (limit)", "Uptime");
                for (InstanceStats instanceStats : stats.getRecords()) {
                    String instance = instanceStats.getId();
                    InstanceStats.Usage usage = instanceStats.getUsage();
                    String cpu = "N/A";
                    String memory = "N/A";
                    String disk = "N/A";
                    if (usage != null) {
                        cpu = instanceStats.getUsage().getCpu() + " ("
                                + instanceStats.getCores() + ")";
                        memory = roundTwoDecimals(instanceStats.getUsage()
                                .getMem() / 1024)
                                + "M ("
                                + instanceStats.getMemQuota()
                                / (1024 * 1024)
                                + "M)";
                        disk = roundTwoDecimals(instanceStats.getUsage()
                                .getDisk() / (1024 * 1024))
                                + "M ("
                                + instanceStats.getDiskQuota()
                                / (1024 * 1024) + "M)";
                    }
                    Double uptime = instanceStats.getUptime();
                    if (uptime == null) {
                        uptime = 0D;
                    }
                    String formattedUptime = formatDurationInSeconds(uptime);
                    table.addRow(instance, cpu, memory, disk, formattedUptime);
                }
                LOGGER.info(table.getOutput());
            }
        });
    }

    public void map(final String appName, final String url) {
        String failureMessage = "The url failed to be mapped to application '"
                + appName + "'";
        String successMessage = "The url was successfully mapped to application '"
                + appName + "'";
        executeCommand(new CloudCommand(failureMessage, successMessage) {
            @Override
            public void execute() throws Exception {
                CloudApplication application = getApplication(appName);
                if (application == null) {
                    displaySuccessMessage = false;
                    return;
                }
                List<String> uris = new ArrayList<String>(application.getUris());
                uris.add(url);
                client.updateApplicationUris(appName, uris);
            }
        });
    }

    public void unMap(final String appName, final String url) {
        String failureMessage = "The url failed to be unmapped from application '"
                + appName + "'";
        String successMessage = "The url was successfully unmapped from application '"
                + appName + "'";
        executeCommand(new CloudCommand(failureMessage, successMessage) {
            @Override
            public void execute() throws Exception {
                CloudApplication application = getApplication(appName);
                if (application == null) {
                    displaySuccessMessage = false;
                    return;
                }
                List<String> uris = new ArrayList<String>(application.getUris());
                uris.remove(url);
                client.updateApplicationUris(appName, uris);
            }
        });
    }

    public void renameApp(final String appName, final String newAppName) {
        String failureMessage = "The application '" + appName
                + "'failed to be renamed";
        String successMessage = "The application '" + appName
                + "' was successfully renamed as '" + newAppName + "'";
        executeCommand(new CloudCommand(failureMessage, successMessage) {
            @Override
            public void execute() throws Exception {
                for (CloudApplication cloudApplication : client
                        .getApplications()) {
                    if (cloudApplication.getName().equals(newAppName)) {
                        LOGGER.severe("An application of that name already exists, please choose another name");
                        displaySuccessMessage = false;
                        return;
                    }
                }
                client.rename(appName, newAppName);
            }
        });
    }

    public void clearStoredLoginDetails() {
        session.clearStoredLoginDetails();
    }

    public void setup() {
        // TODO: This is where a cloud environment profile would be added to the
        // application config
    }

    public boolean isCloudFoundryCommandAvailable() {
        return client != null;
    }

    public boolean isSetupCommandAvailable() {
        return true;
    }

    private double roundTwoDecimals(final double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(d));
    }

    private abstract class CloudCommand {
        protected String failureMessage = "";
        protected String successMessage = "";
        protected String gerund;
        protected boolean displaySuccessMessage = true;

        protected CloudCommand(final String failureMessage,
                final String successMessage, final String gerund) {
            this.failureMessage = failureMessage;
            this.successMessage = successMessage;
            this.gerund = gerund;
        }

        protected CloudCommand(final String failureMessage,
                final String successMessage) {
            this(failureMessage, successMessage, "Performing operation");
        }

        protected CloudCommand(final String failureMessage) {
            this(failureMessage, null);
        }

        public abstract void execute() throws Exception;

        public String getFailureMessage() {
            return failureMessage;
        }

        public String getSuccessMessage() {
            return successMessage;
        }

        public String getGerund() {
            return gerund;
        }

        public boolean isDisplaySuccessMessage() {
            return displaySuccessMessage;
        }
    }

    private void executeCommand(final CloudCommand command) {
        Timer timer = new Timer();
        try {
            final char[] statusIndicators = new char[] { '|', '/', '-', '\\' };
            final int[] statusCount = new int[] { 0 };
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    flash(Level.FINE, command.getGerund() + " "
                            + statusIndicators[statusCount[0]], MY_SLOT);
                    if (statusCount[0] < statusIndicators.length - 1) {
                        statusCount[0] = statusCount[0] + 1;
                    }
                    else {
                        statusCount[0] = 0;
                    }
                }
            };
            timer.scheduleAtFixedRate(timerTask, 0, 100);
            command.execute();
            if (StringUtils.hasText(command.getSuccessMessage())
                    && command.isDisplaySuccessMessage()) {
                LOGGER.info(command.getSuccessMessage());
            }
        }
        catch (Exception e) {
            throw new IllegalStateException(command.getFailureMessage() + " - "
                    + e.getMessage(), e);
        }
        finally {
            timer.cancel();
            flash(Level.FINE, "Complete!", MY_SLOT);
            flash(Level.FINE, "", MY_SLOT);
        }
    }

    private String formatDurationInSeconds(final Double seconds) {
        long secondsInMinute = 60;
        long secondsInHour = secondsInMinute ^ 2;
        long secondsInDay = secondsInHour * 24;
        StringBuilder sb = new StringBuilder();
        long days = (long) (seconds / secondsInDay);
        sb.append(days).append("d:");
        if (days > 1) {
            double remainder = seconds % secondsInDay;
            long hours = (long) ((remainder) / (secondsInHour));
            sb.append(hours).append("h:");

            remainder = remainder % (secondsInHour);
            long minutes = (long) (remainder / (60));
            sb.append(minutes).append("m:");

            remainder = remainder % (60);
            long secs = (long) (remainder);
            sb.append(secs).append("s");
        }
        return sb.toString();
    }

    private CloudApplication getApplication(final String appName) {
        try {
            return client.getApplication(appName);
        }
        catch (Exception ignored) {
        }
        return null;
    }

    private Integer getInteger(final String potentialInt) {
        if (potentialInt == null) {
            return null;
        }
        for (Character c : potentialInt.toCharArray()) {
            if (!Character.isDigit(c)) {
                return null;
            }
        }
        return Integer.valueOf(potentialInt);
    }
}
