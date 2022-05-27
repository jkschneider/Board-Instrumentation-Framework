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
package kutch.biff.marvin.configuration;

import java.util.ArrayList;
import java.util.logging.Logger;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import kutch.biff.marvin.AboutBox;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.network.OscarBullhorn;
import kutch.biff.marvin.task.OscarBullhornTask;
import kutch.biff.marvin.task.TaskManager;

/**
 * @author Patrick Kutch
 */
public class Configuration {

    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static Configuration config;

    public static Configuration getConfig() {
        return config;
    }

    private boolean _DebugMode;
    private boolean _KioskMode;
    private String _Address;
    private int _port;
    private String _AppTitle;
    private String cSSFile;
    private int _insetTop;
    private int _insetBottom;
    private int _insetLeft;
    private int _insetRight;
    private boolean legacyInsetMode;
    private Scene appScene;
    private SimpleDoubleProperty scaleProperty;
    private SimpleDoubleProperty currWidthProperty;
    private SimpleDoubleProperty currHeightProperty;
    //    private double _topOffset, _bottomOffset;
    private boolean _AutoScale;
    private int _Width;
    private int _Height;
    private int _CreationWidth;
    private int _CreationHeight;
    public String TitleSuffix;
    private MenuBar _MenuBar;
    boolean _AllowTasks;
    boolean _ShowMenuBar;
    boolean fAboutCreated;
    private int HeartbeatInterval;
    private TabPane _Pane;
    private long guiTimerUpdateInterval;
    private Stage _AppStage;
    private double _AppBorderWidth;
    private double lastLiveDataReceived;
    private double lastRecordedDataReceived;
    private String applicationID;
    private Side _Side;
    private boolean _IgnoreWebCerts;
    private int _MaxPacketSize;
    private boolean _EnableScrollBars;
    private ArrayList<OscarBullhorn> oscarBullhornList;
    private boolean _MarvinLocalDatafeed;
    private boolean shuttingDown;
    private Screen _PrimaryScreen;
    private boolean _PrimaryScreenDetermined;
    private int _CanvasWidth;
    private int _CanvasHeight;
    private boolean _RunInDebugger;
    private boolean _EnforceMediaSupport;
    private Cursor prevCursor;
    private int busyCursorRequestCount;
    private boolean doNotReportAliasErrors;
    private boolean immediateRefreshRequsted;

    public Configuration() {
        _insetTop = 0;
        _insetBottom = 0;
        _insetLeft = 0;
        _insetRight = 0;
        legacyInsetMode = false;
        _AutoScale = false;
        _Width = 0;
        _Height = 0;
        _CreationWidth = 0;
        _CreationHeight = 0;
        _CanvasWidth = _CanvasHeight = 0;
        _AllowTasks = true;
        _ShowMenuBar = false;
        TitleSuffix = "";
        fAboutCreated = false;
        HeartbeatInterval = 5; // 5 secs
        _AppBorderWidth = 8;
        guiTimerUpdateInterval = 350;
        scaleProperty = new SimpleDoubleProperty(1.0);
        currWidthProperty = new SimpleDoubleProperty();
        currHeightProperty = new SimpleDoubleProperty();
        Configuration._Config = this;
        _IgnoreWebCerts = false;
        applicationID = "";
        _Side = Side.TOP;
        _PrimaryScreen = Screen.getPrimary();
        _PrimaryScreenDetermined = false;
        shuttingDown = false;

        lastLiveDataReceived = 0;
        lastRecordedDataReceived = 0;
        _MaxPacketSize = 16 * 1024;
        _EnableScrollBars = false;
        oscarBullhornList = new ArrayList<>();
        _MarvinLocalDatafeed = false;
        _RunInDebugger = false;
        appScene = null;
        busyCursorRequestCount = 0;
        immediateRefreshRequsted = false;
        doNotReportAliasErrors = false;
    }

    void AddAbout() {
        Menu objMenu = new Menu("About");
        MenuItem item = new MenuItem("About");
        fAboutCreated = true;

        item.setOnAction((ActionEvent t) -> {
            AboutBox.ShowAboutBox();
        });
        objMenu.getItems().add(item);
        _MenuBar.getMenus().add(objMenu);
    }

    public boolean DoNotReportAliasErrors() {
        return doNotReportAliasErrors;
    }

    public void SetDoNotReportAliasErrors(boolean newVal) {
        doNotReportAliasErrors = newVal;
    }

    public void addOscarBullhornEntry(String address, int port, String key) {
        OscarBullhorn objBH = new OscarBullhorn(address, port, key);
        oscarBullhornList.add(objBH);
        TaskManager taskman = TaskManager.getTaskManager();
        if (false == taskman.TaskExists("OscarBullhornTask")) // go create a task on startup to send the announcements
        {
            taskman.AddOnStartupTask("OscarBullhornTask", new OscarBullhornTask());
        }
    }

    public void DetermineMemorex() {
        double timeCompare = 10000; // 10 seconds
        double currTime = System.currentTimeMillis();
        boolean live = false;
        boolean recorded = false;
        if (lastLiveDataReceived + timeCompare > currTime) {
            live = true;
        }
        if (lastRecordedDataReceived + timeCompare > currTime) {
            recorded = true;
        }
        if (live && recorded) {
            TitleSuffix = " {Live and Recorded}";
        } else if (live) {
            TitleSuffix = " {Live}";
        } else if (recorded) {
            TitleSuffix = " {Recorded}";
        } else {
            TitleSuffix = "";
        }
    }

    public String getAddress() {
        return _Address;
    }

    public boolean getAllowTasks() {
        return _AllowTasks;
    }

    /*
     * public double getBottomOffset() { return 0.0;//return _bottomOffset; }
     *
     * public void setBottomOffset(double _bottomOffset) { this._bottomOffset =
     * _bottomOffset; }
     *
     * public double getTopOffset() { return 0.0; //return _topOffset; }
     *
     * public void setTopOffset(double _topOffset) { this._topOffset = _topOffset; }
     */
    public double getAppBorderWidth() {
        return _AppBorderWidth;
    }

    public String GetApplicationID() {
        return applicationID;
    }

    public Scene getAppScene() {
        return appScene;
    }

    public Stage getAppStage() {
        return _AppStage;
    }

    public String getAppTitle() {
        return _AppTitle;
    }

    public int getCanvasHeight() {
        return _CanvasHeight;
    }

    public int getCanvasWidth() {
        return _CanvasWidth;
    }

    public int getCreationHeight() {
        return _CreationHeight;
    }

    public int getCreationWidth() {
        return _CreationWidth;
    }

    public String getCSSFile() {
        return cSSFile;
    }

    public SimpleDoubleProperty getCurrentHeightProperty() {
        return currHeightProperty;
    }

    public SimpleDoubleProperty getCurrentWidthProperty() {
        return currWidthProperty;
    }

    public boolean getEnableScrollBars() {
        return _EnableScrollBars;
    }

    public boolean getEnforceMediaSupport() {
        return _EnforceMediaSupport;
    }

    public int getHeartbeatInterval() {
        return HeartbeatInterval;
    }

    public int getHeight() {
        return _Height;
    }

    public boolean getIgnoreWebCerts() {
        return _IgnoreWebCerts;
    }

    public int getInsetBottom() {
        return _insetBottom;
    }

    public int getInsetLeft() {
        return _insetLeft;
    }

    public int getInsetRight() {
        return _insetRight;
    }

    public int getInsetTop() {
        return _insetTop;
    }

    public boolean getKioskMode() {
        return _KioskMode;
    }

    public boolean getLegacyInsetMode() {
        return legacyInsetMode;
    }

    public boolean getMarvinLocalDatafeed() {
        return _MarvinLocalDatafeed;
    }

    public int getMaxPacketSize() {
        return _MaxPacketSize;
    }

    public MenuBar getMenuBar() {
        if (null != _MenuBar && false == fAboutCreated) {
            AddAbout();
        }
        return _MenuBar;
    }

    public ArrayList<OscarBullhorn> getOscarBullhornList() {
        return oscarBullhornList;
    }

    public TabPane getPane() {
        return _Pane;
    }

    public int getPort() {
        return _port;
    }

    public Screen getPrimaryScreen() {
        return _PrimaryScreen;
    }

    public double getScaleFactor() {
        return scaleProperty.getValue();
    }

    public DoubleProperty getScaleProperty() {
        return scaleProperty;
    }

    public boolean getShowMenuBar() {
        return _ShowMenuBar;
    }

    public Side getSide() {
        return _Side;
    }

    public long getTimerInterval() {
        return guiTimerUpdateInterval;
    }

    public int getWidth() {
        return _Width;
    }

    public boolean isAutoScale() {
        return _AutoScale;
    }

    public boolean isDebugMode() {
        return _DebugMode;
    }

    public boolean isPrimaryScreenDetermined() {
        return _PrimaryScreenDetermined;
    }

    public boolean isRunInDebugger() {
        return _RunInDebugger;
    }

    public void OnLiveDataReceived() {
        lastLiveDataReceived = System.currentTimeMillis();
    }

    public void OnRecordedDataReceived() {
        lastRecordedDataReceived = System.currentTimeMillis();
    }

    public boolean refreshRequested() {
        if (immediateRefreshRequsted) {
            immediateRefreshRequsted = false;
            return true;
        }
        return false;
    }

    public void requestImmediateRefresh() {
        immediateRefreshRequsted = true;
    }

    public void restoreCursor() {
        busyCursorRequestCount--;
        if (busyCursorRequestCount < 1) {
            getAppScene().setCursor(prevCursor);
            prevCursor = null;
        }
    }

    public void setAddress(String address) {
        this._Address = address;
    }

    public void setAllowTasks(boolean allowTasks) {
        this._AllowTasks = allowTasks;
    }

    public void setAppBorderWidth(double appBorderWidth) {
        this._AppBorderWidth = appBorderWidth;
    }

    public void SetApplicationID(String newID) {
        applicationID = newID;
    }

    public void setAppScene(Scene scene) {
        if (null == scene) {
            LOGGER.severe("setScene received a NULL argument");
            return;
        }
        appScene = scene;
    }

    public void setAppStage(Stage appStage) {
        this._AppStage = appStage;
    }

    public void setAppTitle(String appTitle) {
        this._AppTitle = appTitle;
    }

    public void setAutoScale(boolean autoScale) {
        this._AutoScale = autoScale;
    }

    public void setCanvasHeight(int canvasHeight) {
        this._CanvasHeight = canvasHeight;
    }

    public void setCanvasWidth(int canvasWidth) {
        this._CanvasWidth = canvasWidth;
    }

    public void setCreationHeight(int creationHeight) {
        this._CreationHeight = creationHeight;
    }

    public void setCreationWidth(int creationWidth) {
        this._CreationWidth = creationWidth;
    }

    public void setCSSFile(String cSSFie) {
        this.cSSFile = cSSFie;
    }

    public void setCursorToWait() {
        if (busyCursorRequestCount++ == 0) {
            prevCursor = getAppScene().getCursor();
            getAppScene().setCursor(Cursor.WAIT);
        }
    }

    public void setDebugMode(boolean debugMode) {
        this._DebugMode = debugMode;
    }

    public void setEnableScrollBars(boolean enableScrollBars) {
        this._EnableScrollBars = enableScrollBars;
    }

    public void setEnforceMediaSupport(boolean enforceMediaSupport) {
        this._EnforceMediaSupport = enforceMediaSupport;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.HeartbeatInterval = heartbeatInterval;
    }

    public void setHeight(int height) {
        this._Height = height;
    }

    public void setIgnoreWebCerts(boolean ignoreWebCerts) {
        this._IgnoreWebCerts = ignoreWebCerts;
    }

    public void setInsetBottom(int insetBottom) {
        if (insetBottom >= 0) {
            LOGGER.config("Setting application insetBottom to: " + Integer.toString(insetBottom));
            this._insetBottom = insetBottom;
        }
    }

    public void setInsetLeft(int insetLeft) {
        if (insetLeft >= 0) {
            LOGGER.config("Setting application insetLeft to: " + Integer.toString(insetLeft));
            this._insetLeft = insetLeft;
        }
    }

    public void setInsetRight(int insetRight) {
        if (insetRight >= 0) {
            LOGGER.config("Setting application insetRight to: " + Integer.toString(insetRight));
            this._insetRight = insetRight;
        }
    }

    public void setInsetTop(int insetTop) {
        if (insetTop >= 0) {
            LOGGER.config("Setting application insetTop to: " + Integer.toString(insetTop));
            this._insetTop = insetTop;
        }
    }

    public void setKioskMode(boolean kioskMode) {
        this._KioskMode = kioskMode;
    }

    public void setLegacyInsetMode(boolean fVal) {
        legacyInsetMode = fVal;
    }

    public void setMarvinLocalDatafeed(boolean marvinLocalDatafeed) {
        this._MarvinLocalDatafeed = marvinLocalDatafeed;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        this._MaxPacketSize = maxPacketSize;
    }

    public void setMenuBar(MenuBar menuBar) {
        this._MenuBar = menuBar;
    }

    public void setPane(TabPane pane) {
        this._Pane = pane;
    }

    public void setPort(int port) {
        this._port = port;
    }

    public void setPrimaryScreen(Screen primaryScreen) {
        this._PrimaryScreen = primaryScreen;
        setPrimaryScreenDetermined(true);
    }

    public void setPrimaryScreenDetermined(boolean primaryScreenDetermined) {
        this._PrimaryScreenDetermined = primaryScreenDetermined;
    }

    public void setRunInDebugger(boolean runInDebugger) {
        this._RunInDebugger = runInDebugger;
    }

    public void setScaleFactor(double scaleFactor) {
        LOGGER.config("Setting Application Scale Factor to: " + Double.toString(scaleFactor));

        scaleProperty.setValue(scaleFactor);
    }

    public void setShowMenuBar(boolean showMenuBar) {
        this._ShowMenuBar = showMenuBar;
    }

    public void setSide(Side side) {
        this._Side = side;
    }

    public void setTerminating() {
        shuttingDown = true;
    }

    public void setTimerInterval(long newVal) {
        guiTimerUpdateInterval = newVal;
    }

    public void setWidth(int width) {
        this._Width = width;
    }

    public boolean terminating() {
        return shuttingDown;
    }

}
