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
package kutch.biff.marvin.widget;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaMarkerEvent;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import javafx.util.Pair;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.CircularList;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public abstract class MediaPlayerWidget extends BaseWidget {
    private class TotalDurationListener implements InvalidationListener {

        @Override
        public void invalidated(javafx.beans.Observable observable) {
            SetupMarkers(_mediaPlayer, _CurrentMediaID);
        }
    }

    private HashMap<String, String> mediaURI;
    private HashMap<String, String> mediaFilesAndTags;
    protected HashMap<String, String> _TaskMap;
    private CircularList<String> listOfIDs;
    protected String _CurrentMediaID;
    private MediaPlayer _mediaPlayer;
    private boolean _RepeatList;
    private boolean _RepeatSingleMedia;
    private final String widgetType;
    private String playbackControlID;
    private String playbackControlNamespace;
    private boolean _AutoStart;
    private double _VolumeLevel;

    private HashMap<String, List<Pair<String, String>>> eventMarkerMap; // each Media (in has by ID) has a potiential
    // list of Markers

    public MediaPlayerWidget(String strType) {
        widgetType = strType;
        listOfIDs = new CircularList<>();
        mediaURI = new HashMap<>();
        _CurrentMediaID = null;
        playbackControlID = null;
        playbackControlID = null;
        _RepeatList = false;
        _RepeatSingleMedia = false;
        _mediaPlayer = null;
        mediaFilesAndTags = new HashMap<>();
        _TaskMap = new HashMap<>();
        _AutoStart = false;
        _VolumeLevel = 50;
        eventMarkerMap = new HashMap<>();
    }

    public boolean AddMediaFile(String newFile, String id) {
        if (null == newFile) {
            return false;
        }
        String strFileName = BaseWidget.convertToFileOSSpecific(newFile);

        if (null == strFileName) {
            return false;
        }

        String uriFile = VerifyFilename(strFileName);

        if (null != uriFile) {
            try {
                Media objMedia = getMedia(uriFile); // just a test

                if (null != objMedia) {
                    if (false == mediaURI.containsKey(id)) {
                        mediaURI.put(id, uriFile); // has of uri's
                        listOfIDs.add(id);
                        return true;
                    }
                    LOGGER.severe("Duplicate media ID specified for " + widgetType + " Widget:" + id);
                }
            } catch (Exception ex) {
                LOGGER.severe(newFile + " is not a valid or supported media file ");
                LOGGER.severe(ex.toString());
            }
        }
        return false;
    }

    @Override
    protected void ConfigureDimentions() {

    }

    private void ConfigurePlayback(DataManager dataMgr) {
        dataMgr.AddListener(playbackControlID, playbackControlNamespace, (ObservableValue<?> o, Object oldVal, Object newVal) -> {
            String strPlaybackCmd = newVal.toString();
            if ("Play".equalsIgnoreCase(strPlaybackCmd)) {
                OnPlay();
            } else if ("Pause".equalsIgnoreCase(strPlaybackCmd)) {
                OnPause();
            } else if ("Stop".equalsIgnoreCase(strPlaybackCmd)) {
                OnStop();
            } else if (strPlaybackCmd.contains(":")) // could be Volume or JumpTo
                {
                    String[] parts = strPlaybackCmd.split(":");
                    double dVal;
                    if (parts.length > 1) {
                        String strTask = parts[0];
                        try {
                            dVal = Double.parseDouble(parts[1]);
                        } catch (NumberFormatException ex) {
                            LOGGER.severe(widgetType + " received invalid command --> " + strPlaybackCmd);
                            return;
                        }
                        if ("Volume".equalsIgnoreCase(strTask)) {
                            OnSetVolume(dVal);
                        } else if ("JumpTo".equalsIgnoreCase(strTask)) {
                            OnJumpTo(dVal);
                        } else {
                            LOGGER.severe(widgetType + " received invalid command --> " + strPlaybackCmd);
                        }
                    } else {
                        LOGGER.severe(widgetType + " received invalid command --> " + strPlaybackCmd);
                    }
                }
        });
    }

    protected boolean Create(DataManager dataMgr) {
        if (!VerifyInputFiles()) {
            return false;
        }
        if (!VerifySupportsMedia()) {
            if (CONFIG.getEnforceMediaSupport()) {
                return false;
            }
            return true;
        }

        if (listOfIDs.size() == 1) {
            _CurrentMediaID = listOfIDs.get(0);
        }
        if (null != playbackControlID && null != playbackControlNamespace) {
            ConfigurePlayback(dataMgr);
        }

        dataMgr.AddListener(getMinionID(), getNamespace(), (ObservableValue<?> o, Object oldVal, Object newVal) -> {
            String strVal = newVal.toString().replaceAll("(\\r|\\n)", "");
            String key;

            if ("Next".equalsIgnoreCase(strVal)) // go to next media in the list
                {
                    key = listOfIDs.GetNext();
                } else if ("Previous".equalsIgnoreCase(strVal)) // go to previous media in the list
                {
                    key = listOfIDs.GetPrevious();
                } else {
                key = strVal; // expecting an ID
                listOfIDs.get(key); // just to keep next/prev alignment
            }

            key = key.toLowerCase();
            PlayMedia(key);
        });

        if (null != _CurrentMediaID) {
            listOfIDs.get(_CurrentMediaID);
            if (!PlayMedia(_CurrentMediaID)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean Create(GridPane pane, DataManager dataMgr) {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
        // Tools | Templates.
    }

    private MediaPlayer CreateMediaPlayer(String strID) {
        if (false == mediaURI.containsKey(strID)) {
            LOGGER.severe("Tried to read media with ID of " + strID);
            return null;
        }
        if (!IsValid()) {
            return null;
        }
        String strFileID = BaseWidget.convertToFileOSSpecific(strID);
        Media objMedia = getMedia(mediaURI.get(strFileID)); // just a test
        MediaPlayer objPlayer;
        try {
            objPlayer = new MediaPlayer(objMedia);
            // Duration D = objPlayer.getTotalDuration();
            objPlayer.setOnError(() ->
            {
                if (null != objMedia && null != objMedia.getError()) {
                    LOGGER.severe("Unable to play media file: " + mediaURI.get(strFileID) + ". "
                            + objMedia.getError().getMessage());
                } else {
                    LOGGER.severe("Unable to play media file: " + mediaURI.get(strFileID) + ". ");
                }
                OnErrorOcurred();
            });
            objPlayer.setOnEndOfMedia(this::OnPlaybackDone);
            objPlayer.setOnPaused(() ->
            {
                LOGGER.info(widgetType + " [" + strID + "] --> Paused");
            });
            objPlayer.setOnPlaying(() ->
            {
                LOGGER.info(widgetType + " [" + strID + "] --> Playing");
            });
            objPlayer.setOnStopped(() ->
            {
                LOGGER.info(widgetType + " [" + strID + "] --> Stopped");
            });
        } catch (Exception ex) {
            LOGGER.severe(ex.toString());
            return null;
        }
        return objPlayer;
    }

    private boolean GetMarkers(FrameworkNode mediaNode, String inputType, List<Pair<String, String>> markers) {
        if (mediaNode.hasChild("Task")) {
            for (FrameworkNode node : mediaNode.getChildNodes()) {
                if ("Task".equalsIgnoreCase(node.getNodeName())) {
                    String task = node.getTextContent();
                    if (node.hasAttribute("Marker")) {
                        String strMarker = node.getAttribute("Marker");
                        if (VerifyMarker(strMarker)) {
                            markers.add(new Pair<>(strMarker, task)); // add the marker and the task to the list
                        } else {
                            LOGGER.severe(widgetType + "has invalid Marker associated with Task: " + strMarker);
                            return false;
                        }
                    } else {
                        LOGGER.severe(widgetType + "has invalid no Marker associated with Task");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private Media getMedia(String uriFile) {
        Media objMedia = null;

        try {
            objMedia = new Media(uriFile);
        } catch (Exception ex) {
            LOGGER.severe(ex.toString());
            return null;
        }
        if (!VerifyMedia(objMedia)) {
            objMedia = null;
        }
        return objMedia;
    }

    public double getVolumeLevel() {
        return _VolumeLevel;
    }

    public String getWidgetType() {
        return widgetType;
    }

    protected boolean HandleWidgetSpecificSettings(FrameworkNode node, String inputType) {
        if ("Initial".equalsIgnoreCase(node.getNodeName())) {
            if (node.hasAttribute("ID")) {
                _CurrentMediaID = node.getAttribute("ID");
                return true;
            }
            LOGGER.config("No ID for <Initial> tag for " + widgetType + " ignoring");
        } else if ("AutoStart".equalsIgnoreCase(node.getNodeName())) {
            _AutoStart = node.getBooleanValue();
            return true;
        } else if ("PlaybackControl".equalsIgnoreCase(node.getNodeName())) {
            if (node.hasAttribute("ID")) {
                playbackControlID = node.getAttribute("ID");
            }
            if (node.hasAttribute("Namespace")) {
                playbackControlNamespace = node.getAttribute("Namespace");
            }
            if (null == playbackControlNamespace && null == playbackControlID) {
                LOGGER.severe(widgetType + "has tag invalid <PlaybackControl> tag");
                return false;
            }
            return true;
        } else if ("Repeat".equalsIgnoreCase(node.getNodeName())) {
            boolean bVal = node.getBooleanValue();
            if (bVal) {
                if (node.hasAttribute("Mode")) {
                    if ("LoopList".equalsIgnoreCase(node.getAttribute("Mode"))) {
                        _RepeatList = true;
                        _RepeatSingleMedia = false;
                    } else if ("Single".equalsIgnoreCase(node.getAttribute("Mode"))) {
                        _RepeatList = false;
                        _RepeatSingleMedia = true;
                    } else {
                        LOGGER.severe(widgetType
                                + "has tag invalid <Repeat> Mide Attribute tag, expecting either LoopList or Single, got "
                                + node.getAttribute("Mode"));
                        return false;
                    }
                } else {
                    _RepeatList = false;
                    _RepeatSingleMedia = true;

                }
            }
            return true;
        } else if (node.getNodeName().equalsIgnoreCase(inputType)) {
            String strSrc = node.getAttribute("Source");
            String strID = node.getAttribute("ID");
            if (null != strSrc && null != strID) {
                String key = strID.toLowerCase(); // store keys in lower case
                if (mediaFilesAndTags.containsKey(key)) {
                    LOGGER.severe(widgetType + " had duplicate source " + inputType + " ID of " + strID);
                    return false;
                }
                mediaFilesAndTags.put(key, strSrc);

                List<Pair<String, String>> markers = new ArrayList<>(); // list of markers of tasks
                eventMarkerMap.put(key, markers);

                if (node.hasAttribute("Task")) {
                    String taskID = node.getAttribute("task");
                    if (null != taskID) {
                        _TaskMap.put(key, taskID);
                    }
                }

                return GetMarkers(node, inputType, markers);
            }
            LOGGER.severe(widgetType + "has tag invalid <" + inputType + "> tag");
        }

        return false;
    }

    protected abstract boolean HasBeenVerified();

    public boolean isAutoStart() {
        return _AutoStart;
    }

    public boolean isRepeatList() {
        return _RepeatList;
    }

    public boolean isRepeatSingleMedia() {
        return _RepeatSingleMedia;
    }

    protected abstract boolean IsValid();

    protected void OnErrorOcurred() {
        SetIsValid(false);
    }

    protected void OnJumpTo(double newVal) {
        if (null != _mediaPlayer) {
            if (newVal < 0) {
                newVal = 0.0;
            } else if (newVal > 100) {
                newVal = 100;
            }
            while (newVal > 1) // takes range of 0.0 to 1.0.
            {
                newVal /= 100;
            }
            boolean playing = _mediaPlayer.getStatus() == Status.PLAYING;

            Duration mediaDuration = _mediaPlayer.getTotalDuration();

            Duration seekLocation = mediaDuration.multiply(newVal);

            if (_mediaPlayer.getStatus() == Status.STOPPED) {
                OnPause();
            }

            _mediaPlayer.seek(seekLocation);
            if (playing) {
                OnPlay();
            }

        }
    }

    protected abstract boolean OnNewMedia(MediaPlayer objMediaPlayer);

    protected void OnPause() {
        if (null != _mediaPlayer) {
            _mediaPlayer.pause();
        }
    }

    protected void OnPlay() {
        if (null != _mediaPlayer) {
            _mediaPlayer.play();
        }
    }

    protected void OnPlaybackDone() {
        if (!_RepeatList && !_RepeatSingleMedia) {
            // OnStop(); // reset the media to start and be able to play it again
            return;
        }
        String strNextID = "";
        if (_RepeatList && listOfIDs.size() > 1) {
            strNextID = listOfIDs.GetNext();
            PlayMedia(strNextID);
        } else // must repeat current media
        {
            // LOGGER.info("Setting repeat to infinite. [" + _mediaPlayer.toString() + "]");
            _mediaPlayer.seek(_mediaPlayer.getStartTime());
            return;
        }

        _mediaPlayer.play();
    }

    protected void OnSetVolume(double newVal) {
        if (null != _mediaPlayer) {
            if (newVal < 0) {
                newVal = 0.0;
            } else if (newVal > 100) {
                newVal = 100;
            }
            while (newVal > 1) // takes range of 0.0 to 1.0.
            {
                newVal /= 100;
            }
            _mediaPlayer.setVolume(newVal);
            _VolumeLevel = newVal;
        }

    }

    protected void OnStop() {
        if (null != _mediaPlayer) {
            _mediaPlayer.stop();
        }

    }

    private boolean PlayMedia(String strKey) {
        strKey = strKey.toLowerCase();
        if (mediaFilesAndTags.containsKey(strKey)) {
            if (!strKey.equalsIgnoreCase(_CurrentMediaID) || _mediaPlayer == null) // may be just repeating existing
            // media, no reason to re-load
            {
                OnStop();
                MediaPlayer objPlayer = CreateMediaPlayer(strKey);
                if (null == objPlayer) {
                    if (!IsValid()) {
                        LOGGER.severe("Platform does not support Media Player: " + widgetType);
                    } else {
                        LOGGER.warning("Error creating MediaPlayer ID for " + widgetType + "[" + getNamespace() + ":"
                                + getMinionID() + "] : " + strKey);
                    }
                    return false;
                }
                objPlayer.setAutoPlay(_AutoStart);
                // SetupMarkers(objPlayer, strKey);
                _mediaPlayer = objPlayer;
                _CurrentMediaID = strKey;
                listOfIDs.get(strKey);
                if (!eventMarkerMap.get(strKey).isEmpty()) {
                    _mediaPlayer.totalDurationProperty().addListener(new TotalDurationListener());
                }
            }
            OnNewMedia(_mediaPlayer); // widget specific goodies
            OnSetVolume(_VolumeLevel);
            return true;
        }
        LOGGER.warning("Received unknown ID for " + widgetType + "[" + getNamespace() + ":" + getMinionID() + "] : "
                + strKey);

        return false;
    }

    @Override
    public void PrepareForAppShutdown() {
        if (null != _mediaPlayer) {
            _mediaPlayer.stop(); // can leave a hanging thread if you don't do this
        }
    }

    public void setAutoStart(boolean autoStart) {
        this._AutoStart = autoStart;
    }

    protected abstract void setHasBeenVerified(boolean hasBeenVerified);

    protected abstract void SetIsValid(boolean flag);

    public void setRepeatList(boolean repeatList) {
        this._RepeatList = repeatList;
    }

    public void setRepeatSingleMedia(boolean repeatSingleMedia) {
        this._RepeatSingleMedia = repeatSingleMedia;
    }

    private void SetupMarkers(MediaPlayer objPlayer, String playerID) {
        if (eventMarkerMap.containsKey(playerID.toLowerCase())) // should never fail
        {
            List<Pair<String, String>> markers = eventMarkerMap.get(playerID.toLowerCase());
            if (!markers.isEmpty()) {
                for (Pair<String, String> item : markers) {
                    String strMarker = item.getKey();
                    double msMarker = 0;
                    String strTask = item.getValue();

                    double tDuration = objPlayer.getTotalDuration().toMillis();
                    try {
                        // LOGGER.severe(Double.toString(tDuration));
                        if ("end".equalsIgnoreCase(strMarker)) {
                            msMarker = tDuration;
                        } else if ("start".equalsIgnoreCase(strMarker)) {
                            msMarker = 0;
                        } else if (strMarker.contains("%")) {
                            msMarker = tDuration * (Double.parseDouble(strMarker.replace("%", "")) / 100);
                        } else {
                            msMarker = Double.parseDouble(strMarker);
                        }

                        if (msMarker > tDuration) {
                            LOGGER.config(
                                    widgetType + " has task [" + strTask + "] marker > length of media.  Ignoring");
                            continue;
                        }
                        if (this instanceof AudioPlayerWidget) // For some reason, audio won't trigger events at end of
                        // medai,
                        {
                            if (msMarker + 300 > tDuration) // so this is a work around
                            {
                                msMarker = tDuration - 300;
                            }
                        }
                        objPlayer.getMedia().getMarkers().put(strTask, Duration.millis(msMarker));
                    } catch (Exception Ex) // was verifed earlier, so should NEVER happen
                    {
                        LOGGER.severe("Error Setting up Markers, Marker= " + strMarker);
                        return;
                    }
                }
                objPlayer.setOnMarker((MediaMarkerEvent event) ->
                {
                    String strTask = event.getMarker().getKey();
                    TASKMAN.AddDeferredTask(strTask); // fire off that task!
                });
            }
        }
    }

    public void setVolumeLevel(double volumeLevel) {
        this._VolumeLevel = volumeLevel;
    }

    @Override
    public void UpdateTitle(String strTitle) {
        LOGGER.warning("Tried to update Title of a " + widgetType + " to " + strTitle);
    }

    protected String VerifyFilename(String strFile) {
        if (strFile.startsWith("http")) {
            // return strFile; // TODO: suport http targets
        }
        File file = new File(strFile);

        if (file.exists()) {
            return file.toURI().toString();
        }
        return null;
    }

    private boolean VerifyInputFiles() {
        boolean retVal = true;
        if (mediaFilesAndTags.isEmpty()) {
            LOGGER.severe(widgetType + " No Media files specified.");
            retVal = false;
        } else {
            LOGGER.info("Verifying " + widgetType + " input files");
        }
        for (String strKey : mediaFilesAndTags.keySet()) {
            String strFile = mediaFilesAndTags.get(strKey);
            if (!AddMediaFile(strFile, strKey)) {
                LOGGER.severe(widgetType + " Invalid media file: " + strFile);
                retVal = false;
            }
        }
        return retVal;
    }

    private boolean VerifyMarker(String strMarker) {
        String test = strMarker;
        if ("end".equalsIgnoreCase(test)) {
            return true;
        }
        if ("start".equalsIgnoreCase(test)) {
            return true;
        }

        if (test.contains("%")) {
            test = test.trim().replace("%", "");
        }
        try {
            int iMarker = Integer.parseInt(test);
            if (strMarker.contains("%") && iMarker > 100) {
                if (iMarker >= 0) {
                    return true;
                }

            }
        } catch (NumberFormatException ex) {
        }
        LOGGER.severe("Invalid Marker specified for " + widgetType + ": " + strMarker);

        return false;
    }

    protected abstract boolean VerifyMedia(Media objMedia);

    private boolean VerifySupportsMedia() {
        if (HasBeenVerified()) {
            return true; // should be OK, because on 1st failure below, init should stop
        }
        setHasBeenVerified(true);
        LOGGER.info("Verifying that the OS has support for " + widgetType);
        try {
            // try to create a media player with 1st file
            String strFileID = BaseWidget
                    .convertToFileOSSpecific(mediaFilesAndTags.keySet().iterator().next().toLowerCase());
            Media objMedia; // just a test
            objMedia = getMedia(mediaURI.get(strFileID));

            @SuppressWarnings("unused")
            MediaPlayer mediaPlayer = new MediaPlayer(objMedia); // will throw exception if not valid
            // LOGGER.info("Verified that the OS has support for " + _WidgetType);
            return true;
        } catch (Exception ex) {
        }
        LOGGER.severe("Unable to create " + widgetType
                + " - not supported by the Operating System (likely need to install it)");
        return false;
    }

}
