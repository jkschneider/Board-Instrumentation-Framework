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

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class VideoPlayerWidget extends MediaPlayerWidget {
    private static boolean _HasBeenVerified;
    private static boolean isValid = true;
    private final MediaView mediaView;
    private boolean _RetainAspectRatio;

    /**
     *
     */
    public VideoPlayerWidget() {
        super("VideoPlayerWidget");
        mediaView = new MediaView();
        _RetainAspectRatio = true;
    }

    @Override
    protected void ConfigureDimentions() {
        if (getHeight() > 0) {
            mediaView.setFitHeight(getHeight());
        }
        if (getWidth() > 0) {
            mediaView.setFitWidth(getWidth());
        }
    }

    @Override

    public boolean Create(GridPane pane, DataManager dataMgr) {
        ConfigureDimentions();
        ConfigureAlignment();

        SetupPeekaboo(dataMgr);
        mediaView.setPreserveRatio(_RetainAspectRatio);

        if (!Create(dataMgr)) {
            return false;
        }
        pane.add(mediaView, getColumn(), getRow(), getColumnSpan(), getRowSpan());

        SetupTaskAction();
        return true;
    }

    public boolean getRetainAspectRatio() {
        return _RetainAspectRatio;
    }

    @Override
    public Node getStylableObject() {
        return mediaView;
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return mediaView.getStyleClass();
    }

    @Override
    public boolean HandleWidgetSpecificSettings(FrameworkNode node) {
        return HandleWidgetSpecificSettings(node, "Video");
    }

    @Override
    public boolean HasBeenVerified() {
        return VideoPlayerWidget._HasBeenVerified;
    }

    @Override
    public boolean IsValid() {
        return isValid;
    }

    @Override
    protected boolean OnNewMedia(MediaPlayer objMediaPlayer) {
        mediaView.setMediaPlayer(objMediaPlayer);

        return true;
    }

    @Override
    public void setHasBeenVerified(boolean hasBeenVerified) {
        VideoPlayerWidget._HasBeenVerified = hasBeenVerified;
    }

    @Override
    public void SetIsValid(boolean flag) {
        isValid = flag;
    }

    public void setRetainAspectRatio(boolean retainAspectRatio) {
        this._RetainAspectRatio = retainAspectRatio;
    }

    @Override
    public EventHandler<MouseEvent> SetupTaskAction() {
        if (false == isMouseHasBeenSetup()) // quick hack, as I call this from MOST widgets, but now want it from all.
        // Will eventually remove from individual widgets.
        {
            BaseWidget objWidget = this;
            if (_TaskMap.size() > 0 || CONFIG.isDebugMode()) // only do if a task to setup, or if debug mode
            {
                EventHandler<MouseEvent> eh = (MouseEvent event) -> {
                    if (event.isShiftDown() && CONFIG.isDebugMode()) {
                        LOGGER.info(objWidget.toString(true));
                    } else if (CONFIG.getAllowTasks() && _TaskMap.containsKey(_CurrentMediaID.toLowerCase())) {
                        TASKMAN.PerformTask(_TaskMap.get(_CurrentMediaID.toLowerCase()));
                    } else if (null != getTaskID() && CONFIG.getAllowTasks()) {
                        TASKMAN.PerformTask(getTaskID());
                    }
                };
                getStylableObject().setOnMouseClicked(eh);
                setMouseHasBeenSetup(true);
                return eh;
            }
        }
        return null;
    }

    @Override
    protected boolean VerifyMedia(Media objMedia) {
        return true;
    }
}
