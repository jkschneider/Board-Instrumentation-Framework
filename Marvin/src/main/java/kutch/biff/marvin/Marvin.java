/*
 * ##############################################################################
 * #  Copyright (c) 2016 Intel Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * #  you may not use this file except in compliance with the License.
 * #  You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * #  Unless required by applicable law or agreed to in writing, software
 * #  distributed under the License is distributed on an "AS IS" BASIS,
 * #  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * #  See the License for the specific language governing permissions and
 * #  limitations under the License.
 * ##############################################################################
 * #    File Abstract:
 * #
 * #
 * ##############################################################################
 */
package kutch.biff.marvin;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JOptionPane;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.configuration.ConfigurationReader;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.network.Server;
import kutch.biff.marvin.splash.MySplash;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.AliasMgr;
import kutch.biff.marvin.utility.Heartbeat;
import kutch.biff.marvin.utility.JVMversion;
import kutch.biff.marvin.utility.MarvinLocalData;
import kutch.biff.marvin.version.Version;
import kutch.biff.marvin.widget.BaseWidget;
import kutch.biff.marvin.widget.Widget;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class Marvin extends Application {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static TabPane objTabPane;

    public static void DumpThreads(boolean showStack) {
        showStack = false;
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);

        LOGGER.log(Level.INFO, "******* Dumping {0} Threads from thread: {1} ****************", new Object[]{Integer.toString(threadArray.length), Thread.currentThread().getName()});

        String dumpString = "";
        for (Thread entry : threadArray) {
            // info is name,priority,threadgroup
            dumpString += "\t" + entry.toString() + " -- " + entry.getState().toString() + "\n";
            if (showStack) {
                for (StackTraceElement element : entry.getStackTrace()) {
                    dumpString += "\t\t" + element.toString() + "\n";
                }
            }
        }
        LOGGER.info(dumpString);
    }

    // returns the base tab pane - used for dynamic tabs in debug mode
    @SuppressWarnings("exports")
    public static TabPane GetBaseTabPane() {
        return objTabPane;
    }

    public static void main(final String[] args) {
        if (!JVMversion.meetsMinimumVersion()) {
            System.out.println("Not valid JVM version.  Requires 1." + JVMversion.MINIMUM_MAJOR_VERSION + " build "
                    + JVMversion.MINIMUM_BUILD_VERSION + " or newer");
            return;
        }
        try {
            Thread.currentThread().setName("Main Application Thread");
            Application.launch(args);
            System.exit(0);
        } catch (OutOfMemoryError ex) {
            JOptionPane.showMessageDialog(null, "Insufficient Memory.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            LOGGER.severe(sw.toString());
            LOGGER.severe(ex.toString());
        }
        System.exit(1);
    }

    private DataManager dataMgr;
    private ConfigurationReader _Config;
    private Configuration appConfig;
    private Server receiveServer;
    private AnimationTimer animationTimer;

    private Heartbeat heartbeat;
    private long lastTimerCall;
    private TabPane testPane;
    private long timerInterval = 2500; // nanoseconds 1ms = 1000000 ns
    private long memoryUsageReportingInterval = 10000; // 10 secs
    private long lastMemoryUsageReportingTime;
    private boolean reportMemoryUsage;

    private String strOldSuffix = "dummy";
    private int splashWait = 5000;
    private int noSplashWait = 800;
    private Stage _stage;
    private final TaskManager taskman = TaskManager.getTaskManager();
    private String configFilename = "Application.xml";
    private String logFileName = "MarvinLog.html";
    private boolean showHelp;
    @SuppressWarnings("unused")
    private boolean showVersion;
    private boolean showSplash = true;
    @SuppressWarnings("unused")
    private boolean runInDebugger;
    private boolean enforceMediaSupport;
    private boolean dumpAlias;
    private boolean dumpWidgetInfo;
    private String altSplash;
    private MarvinLocalData objLocalMarvinData;

    private MySplash splash;

    private boolean checkForSizeProblems = true;

    @SuppressWarnings("unused")
    private boolean sizeCheckWindowShowing;

    private long BeginLoadProcess() {
        long start = System.currentTimeMillis();
        if (null == _Config) {
            _Config = new ConfigurationReader();
        }

        if (null == _Config) {
            return 0;
        }

        taskman.setDataMgr(dataMgr); // kludgy I know, I know. I hang my head in shame
        appConfig = _Config.ReadAppConfigFile(configFilename);

        if (null != appConfig) {
            appConfig.setEnforceMediaSupport(enforceMediaSupport);
            if (dumpAlias) {
                AliasMgr.getAliasMgr().DumpTop();
            }

            receiveServer = new Server(dataMgr);
        }
        return System.currentTimeMillis() - start;
    }

    private void BeginServerEtc() {
        LOGGER.info("Starting Server");
        receiveServer.Start();

        TaskManager.getTaskManager().PerformOnStartupTasks(); // perform any tasks designated to be run on startup
        heartbeat = new Heartbeat(_Config.getConfiguration().getHeartbeatInterval()); // every n seconds TODO: make
        // configurable
        heartbeat.Start();
        if (_Config.getConfiguration().getMarvinLocalDatafeed()) {
            objLocalMarvinData = new MarvinLocalData(1);
        }
        _stage.setMaximized(true);
    }

    private void CheckForLogFileName() {
        Parameters params = getParameters();
        List<String> parameters = params.getRaw();

        for (int iIndex = 0; iIndex < parameters.size(); iIndex++) {
            String param = parameters.get(iIndex);
            if ("-log".equalsIgnoreCase(param)) {
                if (iIndex + 1 < parameters.size()) {
                    logFileName = parameters.get(++iIndex);
                } else {
                    System.out.println(
                            "-log command line option given, but no filename provided.  Defaulting to MarvinLog.html");
                }
                return;
            }
        }
    }

    @SuppressWarnings("unused")
    private void checkSize(Stage stage, Scene scene, GridPane objGridPane) {
        stage.centerOnScreen();
        double borderWidth = abs((scene.getWidth() - stage.getWidth()) / 2);
        _Config.getConfiguration().setAppBorderWidth(borderWidth);// = _TestPane.getHeight(); // tab + borders
        if (null != _Config.getConfiguration().getMenuBar() && _Config.getConfiguration().getShowMenuBar()) { // menu + borders +
        }

        objGridPane.getChildren().remove(testPane);
    }

    private void DisableWebCerts() {
        LOGGER.info("Disabling Web Certificates.");
        // Completely disable, by trusting everything!
        TrustManager[] myTM = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }};

        try {
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, myTM, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception ex) {
            LOGGER.severe("Error Disabling Web Certificates: " + ex.toString());
        }
    }

    private void DisplayHelp() {
        String help = "-? | -help \t\t: Display this help\n";
        help += "-i application.xml file [default Application.xml\n";
        help += "-log application.log file [default MarvinLog.html\n";
        help += "-aliasfile externalFile aliases you want to define outside of your xml\n";
        help += "-altSplash image file to use for splash screen\n";
        help += "-v | -vv |-vvv |-vvvv \t: - logging level\n";
//        help += "-version \t\t: - show version information\n";
        help += "-dumpalias - dumps top level alias to log\n";
        help += "-dumpWidgetInfo - dumps info on all widgets\n";
        help += "-ns - supresses splash screen\n";
        System.out.println(help);
        JOptionPane.showMessageDialog(null, help, "Command Line Options", JOptionPane.INFORMATION_MESSAGE);
    }

    private void DumpAllWidgetsInformation() {
        if (_Config.getConfiguration().isDebugMode()) {
            for (BaseWidget objWidget : BaseWidget.getWidgetList()) {
                if (dumpWidgetInfo) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Widget Information: ");
                    sb.append(objWidget.toString(false));
                    LOGGER.config(sb.toString());
                }
                objWidget.SetupTaskAction(); // setup shift and ctrl click actions for debug
                if (null != objWidget.getRegionObject()) {
                    // objWidget.getRegionObject().requestLayout();
                }
            }
            if (dumpWidgetInfo) {
                LOGGER.config("External Grid/Tab usage file from " + configFilename + "\n"
                        + kutch.biff.marvin.widget.widgetbuilder.WidgetBuilder.GetFileTree());
            }
        }
    }

    @SuppressWarnings("exports")
    public void FinishLoad(Stage stage) {
        stage.setIconified(true);

        long elapsedTime = BeginLoadProcess();
        LOGGER.info("Time taken to load Configuration: " + Long.toString(elapsedTime) + "ms.");

        if (null == this.appConfig) {
            Platform.exit();
            return;
        }
        _Config.getConfiguration().setAppStage(stage);

        if (null == objTabPane) {
            objTabPane = new TabPane();
            objTabPane.setSide(_Config.getConfiguration().getSide());
        }
        GridPane sceneGrid = new GridPane();

        Scene scene = null;
        Rectangle2D visualBounds = _Config.getConfiguration().getPrimaryScreen().getVisualBounds();
        int appWidth = (int) visualBounds.getWidth();
        int appHeight = (int) visualBounds.getHeight();

        if (appConfig.getWidth() > 0) {
            appWidth = appConfig.getWidth();
        } else {
            appConfig.setWidth(appWidth);
        }
        if (appConfig.getHeight() > 0) {
            appHeight = appConfig.getHeight();
        } else {
            appConfig.setHeight(appHeight);
        }

        sceneGrid.add(objTabPane, 0, 1);
        // sceneGrid.setStyle("-fx-background-color:red;");
        SetupSizeCheckPane(sceneGrid);
        // sceneGrid.setMaxHeight(340);
        if (null != _Config.getConfiguration().getMenuBar() && _Config.getConfiguration().getShowMenuBar()) {
            // vbox.getChildren().add(_Config.getConfiguration().getMenuBar());
            GridPane.setHalignment(_Config.getConfiguration().getMenuBar(), HPos.LEFT);
            GridPane.setValignment(_Config.getConfiguration().getMenuBar(), VPos.TOP);

            sceneGrid.add(_Config.getConfiguration().getMenuBar(), 0, 0);
        }

        scene = new Scene(sceneGrid);

        _Config.getConfiguration().setAppScene(scene);
        _Config.getConfiguration().getCurrentHeightProperty().bind(scene.heightProperty());
        _Config.getConfiguration().getCurrentWidthProperty().bind(scene.widthProperty());
        objTabPane.prefWidthProperty().bind(scene.widthProperty());

        objTabPane.prefHeightProperty().bind(scene.heightProperty());

        SetAppStyle(scene.getStylesheets());

        if (false == SetupGoodies(objTabPane)) {
            JOptionPane.showMessageDialog(null, "Error loading Configuation. \nCheck log file.", "Configuration Error",
                    JOptionPane.ERROR_MESSAGE);
            Platform.exit();
            return;
        }

        if (false == receiveServer.Setup(_Config.getConfiguration().getAddress(),
                _Config.getConfiguration().getPort())) {
            JOptionPane.showMessageDialog(null, "Error setting up Network Configuation. \nCheck log file.",
                    "Configuration Error", JOptionPane.ERROR_MESSAGE);
            Platform.exit();
            return;
        }
        checkSize(stage, scene, sceneGrid); // go resize based upon scaling

        stage.setTitle(_Config.getConfiguration().getAppTitle());
        stage.setIconified(false);
        stage.setScene(scene);
        stage.setMaximized(false);
        stage.setHeight(appHeight);
        stage.setWidth(appWidth);

        if (showHelp) {
            DisplayHelp();
        }

        if (_Config.getConfiguration().getIgnoreWebCerts()) {
            DisableWebCerts();
        }

        _stage = stage;

        timerInterval = _Config.getConfiguration().getTimerInterval();
        lastTimerCall = System.currentTimeMillis() + timerInterval;
        lastMemoryUsageReportingTime = lastTimerCall;

        strOldSuffix = "dummy";

        animationTimer = new AnimationTimer() // can't update the Widgets outside of GUI thread, so this is a little
                // worker to do so
        {
            boolean Showing = false;
            Configuration config = _Config.getConfiguration();

            @Override
            public void handle(long now) {
                if (config.terminating()) {
                    return;
                }

                if (!Showing && splash.isSplashClosed()) { // will only happen once
                    try {
                        Showing = true;

                        Thread.currentThread().setName("Animation Timer Thread");
                        SetupDebugToolTips();
                        BeginServerEtc();
                        splash.appVisible();
                    } catch (Exception e) {
                        StringWriter strWriter = new StringWriter();
                        PrintWriter pntWriter = new PrintWriter(strWriter);
                        e.printStackTrace(pntWriter);
                        // LOGGER.severe(strWriter.toString());
                        JOptionPane.showMessageDialog(null, "Error trying to launch application. \nCheck log file.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        Platform.exit();
                    }
                    Showing = true;
                    return;
                }
                boolean refreshRequested = config.refreshRequested();

                if (refreshRequested || System.currentTimeMillis() > lastTimerCall + timerInterval) {
                    dataMgr.PerformUpdates();
                    config.DetermineMemorex();
                    if (!strOldSuffix.equals(config.TitleSuffix)) // title could be 'recorded' 'lived'
                    {
                        _stage.setTitle(config.getAppTitle() + config.TitleSuffix);
                        strOldSuffix = config.TitleSuffix;
                    }
                    // for remote marvin admin updates, can't update gui outside of gui thread
                    TaskManager.getTaskManager().PerformDeferredTasks();
                    if (!refreshRequested) {
                        lastTimerCall = System.currentTimeMillis();
                    }
                } else if (reportMemoryUsage
                        && System.currentTimeMillis() > lastMemoryUsageReportingTime + memoryUsageReportingInterval) {
                    lastMemoryUsageReportingTime = System.currentTimeMillis();
                    long freeMem = Runtime.getRuntime().freeMemory();
                    long totalMem = Runtime.getRuntime().maxMemory();
                    long usedMem = totalMem - freeMem;
                    usedMem /= 1024.0;
                    String mBMemStr = NumberFormat.getNumberInstance(Locale.US).format(usedMem / 1024);
                    // String BytesStr = NumberFormat.getNumberInstance(Locale.US).format(usedMem);
                    // int qSize = DataManager.getDataManager().getQueuedSize();
                    // LOGGER.info("Used Memory: " + MBMemStr + " MB. Outstanding DataPoints: " +
                    // Integer.toString(qSize));
                    LOGGER.info("Used Memory: " + mBMemStr);
                }
            }
        };

        int waitBeforeRun = showSplash ? splashWait : noSplashWait;
        new java.util.Timer().schedule(// Start goodies in a few seconds
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        animationTimer.start();
                        this.cancel();
                    }
                }, waitBeforeRun);

        DumpAllWidgetsInformation();

        stage.setX(_Config.getConfiguration().getPrimaryScreen().getVisualBounds().getMinX());
        stage.setY(_Config.getConfiguration().getPrimaryScreen().getVisualBounds().getMinY());
    }

    @Override
    public void init() {
        CheckForLogFileName();
        try {
            MarvinLogger.setup(logFileName);
            MarvinLogger.setDebugLevel(Level.SEVERE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ParseCommandLineArgs();
        if (showHelp) {
            return;
        }

        splash = new MySplash(showSplash, altSplash);
    }

    private void ParseCommandLineArgs() {
        Parameters params = getParameters();
        List<String> parameters = params.getRaw();
        int verboseLevel = 0;
        String aliasFileCompare = "-aliasfile=";

        for (int iIndex = 0; iIndex < parameters.size(); iIndex++) {
            String param = parameters.get(iIndex);
            if ("-i".equalsIgnoreCase(param)) {
                if (iIndex + 1 < parameters.size()) {
                    configFilename = parameters.get(++iIndex);
                } else {
                    LOGGER.severe(
                            "-i command line option given, but no filename provided.  Defaulting to Application.xml");
                }
            } else if ("-log".equalsIgnoreCase(param)) // already handled elsewhere, but do it again for fun
            {
                if (iIndex + 1 < parameters.size()) {
                    logFileName = parameters.get(++iIndex);
                } else {
                    LOGGER.severe(
                            "-log command line option given, but no filename provided.  Defaulting to MarvinLog.html");
                }
            } else if ("-AltSplash".equalsIgnoreCase(param)) // already handled elsewhere, but do it again for fun
            {
                if (iIndex + 1 < parameters.size()) {
                    altSplash = parameters.get(++iIndex);
                } else {
                    LOGGER.severe("-AltSplash command line option given, but no filename provided.");
                }
            } else if ("-v".equalsIgnoreCase(param)) {
                verboseLevel = 1;
            } else if ("-vv".equalsIgnoreCase(param)) {
                verboseLevel = 2;
            } else if ("-vvv".equalsIgnoreCase(param)) {
                verboseLevel = 3;
            } else if ("-vvvv".equalsIgnoreCase(param)) {
                verboseLevel = 4;
                // ReportMemoryUsage = true; // super verbose mode, show memory usage
            } else if ("-enforceMediaSupport".equalsIgnoreCase(param)) {
                enforceMediaSupport = true;
            } else if ("-dumpalias".equalsIgnoreCase(param)) {
                dumpAlias = true;
            } else if ("-dumpwidgetinfo".equalsIgnoreCase(param)) {
                dumpWidgetInfo = true;
            } else if (param.length() > aliasFileCompare.length()
                    && param.substring(0, aliasFileCompare.length()).equalsIgnoreCase(aliasFileCompare)) {
                AliasMgr.getAliasMgr().LoadAliasFile(param);
            } else if ("-?".equalsIgnoreCase(param)) {
                showHelp = true;
            } else if ("-help".equalsIgnoreCase(param)) {
                showHelp = true;
            } else if ("-version".equalsIgnoreCase(param)) {
                showVersion = true;
            } else if ("-ns".equalsIgnoreCase(param)) // don't show splash
            {
                showSplash = false;
            } else if ("-db".equalsIgnoreCase(param)) // don't show splash
            {
                runInDebugger = true;
                showSplash = false;
            } else if ("-nosplash".equalsIgnoreCase(param)) // don't show splash
            {
                showSplash = false;
            } else {
                LOGGER.severe("Unknown command line parameter: " + param);
            }
        }

        MarvinLogger.setDebugLevel(Level.ALL);
        LOGGER.config("--- BIFF GUI [Marvin]  " + Version.getVersion());
        MarvinLogger.setDebugLevel(Level.SEVERE);

        if (0 == verboseLevel) {
            MarvinLogger.setDebugLevel(Level.SEVERE);
            checkForSizeProblems = false;
        }

        switch (verboseLevel) {
            case 1:
                MarvinLogger.setDebugLevel(Level.WARNING);
                checkForSizeProblems = false;
                break;
            case 2:
                MarvinLogger.setDebugLevel(Level.INFO);
                checkForSizeProblems = false;
                break;
            case 3:
                MarvinLogger.setDebugLevel(Level.CONFIG);
                break;
            case 4:
                MarvinLogger.setDebugLevel(Level.ALL);
                break;
            default:
                break;
        }
    }

    private boolean SetAppStyle(ObservableList<String> styleSheets) {
        if (null != _Config.getConfiguration().getCSSFile()) {
            String osIndepFN = BaseWidget.convertToFileOSSpecific(_Config.getConfiguration().getCSSFile());

            if (null == osIndepFN) {
                return true;
            }
            String strCSS = BaseWidget.convertToFileURL(osIndepFN);

            if (null != strCSS) {
                try {
                    if (false == styleSheets.add(strCSS)) {
                        LOGGER.severe(
                                "Problems with application stylesheet: " + _Config.getConfiguration().getCSSFile());
                        return false;
                    }
                } catch (Exception ex) {
                    LOGGER.severe("Problems with application stylesheet: " + _Config.getConfiguration().getCSSFile());
                    return false;
                }
            }
        }

        return true;
    }

    private void SetupDebugToolTips() {
        if (_Config.getConfiguration().isDebugMode()) {
            for (int iIndex = 0; iIndex < _Config.getTabs().size(); iIndex++) {
                _Config.getTabs().get(iIndex).PerformPostCreateActions(null, true);
            }
        }

        // check if a widget is bigger than it's parent grid - not working yet
        if (checkForSizeProblems) {
            for (int iIndex = 0; iIndex < _Config.getTabs().size(); iIndex++) {
                _Config.getTabs().get(iIndex).CheckSizingBounds(1);
            }
        }
    }

    private boolean SetupGoodies(TabPane pane) {
        boolean retVal = true;
        long startTime = System.currentTimeMillis();
        if (null == _Config.getTabs()) {
            return false;
        }

        for (int iIndex = 0; iIndex < _Config.getTabs().size(); iIndex++) {
            if (false == _Config.getTabs().get(iIndex).Create(pane, dataMgr, iIndex)) {
                retVal = false;
                break;
            }
        }
        if (retVal) {
            for (int iIndex = 0; iIndex < _Config.getTabs().size(); iIndex++) {
                if (false == _Config.getTabs().get(iIndex).PerformPostCreateActions(null, false)) {
                    retVal = false;
                    break;
                }
            }

        }

        // check if all have been setup with parent pane (a bit of a hack for this added
        // peekabooo feature_
        /*
         * for (BaseWidget objWidget : BaseWidget.getWidgetList()) { if
         * (!objWidget.ProperlySetup()) { RetVal = false;
         * LOGGER.severe(objWidget.getClass().toString() +
         * " didn't register with SetParent"); } }
         */
        if (TaskManager.getTaskManager().VerifyTasks()) {
            LOGGER.info("Verified all referenced tasks are defined.");
        }

        _Config.getConfiguration().setPane(pane);
        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("Time taken to initialize all widgets: " + Long.toString(elapsed) + "ms.");

        return retVal;
    }

    /**
     * Creates a dummy tab pane, so I can measure the height of the tab portion for
     * other calculations
     *
     * @param basePlane
     */
    private void SetupSizeCheckPane(GridPane basePlane) {
        testPane = new TabPane();
        Tab t = new Tab();
        t.setText("Test");
        testPane.getTabs().add(t);
        testPane.setVisible(false);

        basePlane.add(testPane, 2, 2);
    }

    @SuppressWarnings("exports")
    @Override
    public void start(Stage stage) throws Exception {
        dataMgr = new DataManager();

        if (false == testAppSize(stage)) {
            stage.close();
            stop();
            System.exit(5);
        }
        MySplash.getSplash().start(stage);

    }

    @Override
    public void stop() {
        if (null != Configuration.getConfig()) {
            Configuration.getConfig().setTerminating();
            StopWidgets();
        }

        if (null != objLocalMarvinData) {
            objLocalMarvinData.Shutdown();
        }
        if (null != receiveServer) {
            receiveServer.Stop();
        }

        if (null != heartbeat) {
            heartbeat.Stop();
        }
        /*
         * if (null != _animationTimer) { _animationTimer.stop(); }
         * LOGGER.info("Animation Timer Stopped");
         */
        // Marvin.DumpThreads(true);
    }

    private void StopWidgets() {
        if (null != _Config && null != _Config.getTabs()) {
            for (Widget tab : _Config.getTabs()) {
                tab.PrepareForAppShutdown();
            }
        }
    }

    /*
     * private void LoadApplication(Stage stage) { stage.setIconified(true); _Config
     * = new ConfigurationReader(); if (null == _Config) { return; }
     *
     * Configuration config = _Config.ReadStartupInfo(ConfigFilename); if (null ==
     * config) { return; } _objTabPane = new TabPane();
     * _objTabPane.setSide(config.getSide());
     *
     * GridPane sceneGrid = new GridPane();
     *
     * GridPane.setHalignment(_Config.getConfiguration().getMenuBar(), HPos.LEFT);
     * GridPane.setValignment(_Config.getConfiguration().getMenuBar(), VPos.TOP);
     *
     * sceneGrid.add(_Config.getConfiguration().getMenuBar(), 0, 0);
     * sceneGrid.add(_objTabPane, 0, 1);
     *
     * Scene scene = null; Rectangle2D visualBounds =
     * _Config.getConfiguration().getPrimaryScreen().getVisualBounds(); int appWidth
     * = (int) visualBounds.getWidth(); int appHeight = (int)
     * visualBounds.getHeight();
     *
     * if (config.getWidth() > 0) { appWidth = config.getWidth(); } else {
     * config.setWidth(appWidth); } if (config.getHeight() > 0) { appHeight =
     * config.getHeight(); } else { config.setHeight(appHeight); }
     *
     * scene = new Scene(sceneGrid); SetAppStyle(scene.getStylesheets());
     * stage.setScene(scene); stage.setIconified(true);
     *
     * stage.setX(_Config.getConfiguration().getPrimaryScreen().getVisualBounds().
     * getMinX());
     * stage.setY(_Config.getConfiguration().getPrimaryScreen().getVisualBounds().
     * getMinY());
     *
     * stage.setMaximized(true);
     *
     * stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new
     * EventHandler<WindowEvent>() {
     *
     * @Override public void handle(WindowEvent window) { FinishLoad(stage); } });
     *
     * while (false && !_SizeCheckWindowShowing) { try { Thread.sleep(100); } catch
     * (InterruptedException ex) {
     * Logger.getLogger(Marvin.class.getName()).log(Level.SEVERE, null, ex); } }
     *
     * FinishLoad(stage);
     *
     * }
     */
    /// pop up quick simple tab,menu and style it, then grab the size of the window
    /// so we know the canvas dimenstions
    private boolean testAppSize(Stage stage) {
        final GridPane stagePane = new GridPane();
        final GridPane canvasPane = new GridPane();
        canvasPane.setAlignment(Pos.TOP_LEFT);

        _Config = new ConfigurationReader();
        if (null == _Config) {
            return false;
        }
        final Configuration basicConfig = _Config.ReadStartupInfo(configFilename);

        if (null == basicConfig) {
            return false;
        }

        stage.setX(basicConfig.getPrimaryScreen().getVisualBounds().getMinX());
        stage.setY(basicConfig.getPrimaryScreen().getVisualBounds().getMinY());

        TabPane tabPane = new TabPane();
        Tab objTab = new Tab();
        final Side tabSide = basicConfig.getSide();
        if (tabSide == Side.TOP || tabSide == Side.BOTTOM) {
            tabPane.setSide(Side.TOP);
        } else {
            tabPane.setSide(Side.LEFT);
        }
        objTab.setText("");

        tabPane.getTabs().add(objTab);
        stagePane.add(tabPane, 0, 1);

        MenuBar objMenuBar = new MenuBar();
        if (basicConfig.getShowMenuBar()) {
            Menu objMenu = new Menu("About");
            MenuItem objItem = new MenuItem("Dummy");
            objMenu.getItems().add(objItem);
            objMenuBar.getMenus().add(objMenu);
            stagePane.add(objMenuBar, 0, 0);
        }

        if (basicConfig.getEnableScrollBars()) {
            ScrollPane testScrollPane = new ScrollPane();
            testScrollPane.setPannable(true);
            testScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            testScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            testScrollPane.setContent(canvasPane);
            objTab.setContent(testScrollPane);
        } else {
            objTab.setContent(canvasPane);
        }
        stage.setTitle(basicConfig.getAppTitle());

        Scene scene = new Scene(stagePane);
        stage.setScene(scene);
        SetAppStyle(scene.getStylesheets());

        if (_Config.getConfiguration().getKioskMode()) {
            stage.initStyle(StageStyle.UNDECORATED);
        }

        URL resource = AboutBox.class.getResource("About.png");
        Image iconImg = new Image(resource.toString());

        stage.getIcons().add(iconImg);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent window) {
                scene.heightProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight,
                                        Number newSceneHeight) {
                        scene.heightProperty().removeListener(this);
                        Point2D canvasInScene = canvasPane.localToScene(0.0, 0.0);
                        int cvHeight;
                        int cvWidth;
                        if (tabSide == Side.TOP || tabSide == Side.BOTTOM) {
                            cvHeight = (int) (scene.getHeight() - canvasInScene.getY());
                            cvWidth = (int) scene.getWidth();
                        } else {
                            cvHeight = (int) (scene.getHeight() - canvasInScene.getY());
                            cvWidth = (int) (scene.getWidth() - canvasInScene.getX());
                        }
                        basicConfig.setCanvasWidth(cvWidth);
                        basicConfig.setCanvasHeight(cvHeight);

                        stage.setIconified(true);
                        Platform.runLater(() -> {
                            FinishLoad(stage);
                        });
                    }
                });
            }
        });

        stage.show();
        if (basicConfig.getWidth() > 0) {
            stage.setWidth(basicConfig.getWidth());
            stage.setHeight(basicConfig.getHeight());
        } else {
            stage.setMaximized(true);
        }
        return true;
    }
}
