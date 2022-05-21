/*
 * ##############################################################################
 * #  Copyright (c) 2019 Intel Corporation
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
package kutch.biff.marvin.utility;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class MarvinPlayback implements Runnable {
    class DataSet {
        public String ID;
        public String Namespace;
        public String Data;
        public int Time;
    }

    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private String name;
    private double playbackSpeed;
    private boolean loopPlayback;
    private boolean playing;
    private int nextEntry;
    List<DataSet> _playbackData;
    private boolean terminate;
    private boolean paused;
    private Thread playbackThread;

    public MarvinPlayback(String strName) {
        name = strName;
        playbackSpeed = 10.0;
        loopPlayback = false;
        LOGGER.info("Creating new Marvin Playback with name of " + strName);
    }

    String getName() {
        return name;
    }

    public boolean loadFile(String fName) {
        if (playing) {
            stopPlayback();
        }
        List<DataSet> newData = ReadFile(fName);
        _playbackData = newData;

        if (null == newData) {
            return false;
        }
        nextEntry = 0;

        return true;
    }

    public void pausePlayback() {
        if (paused) {
            LOGGER.warning("Asked to pause Playback " + getName() + ", when already paused.");
        }
        paused = true;
    }

    public void Play() {
        if (playing) {
            LOGGER.warning("Asked to Playback Playback " + getName() + ", when already playing, starting over.");
            stopPlayback();
        }
        if (null == _playbackData) {
            LOGGER.warning("Asked to Playback Playback " + getName() + ", but no data loaded.");
            return;
        }

        nextEntry = 0;
        terminate = false;
        paused = false;
        playbackThread = new Thread(this);
        playbackThread.start();
    }

    private List<DataSet> ReadFile(String fName) {
        InputStream inputStream;
        BufferedInputStream bis;
        DataInputStream inp;
        List<DataSet> retList = new ArrayList<>();

        try {
            inputStream = new FileInputStream(fName);
            bis = new BufferedInputStream(inputStream);
            inp = new DataInputStream(bis);
        } catch (FileNotFoundException e) {
            LOGGER.severe("Invalid Playback File: " + fName);
            return null;
        }
        try {
            String fType = new String(inp.readNBytes(4));
            if (!fType.contentEquals("BIFM")) {
                LOGGER.severe("Invalid Playback File: " + fName);
                return null;
            }
            byte fileVersion = inp.readByte();

            int entries = inp.readInt();
            int len;
            LOGGER.info(
                    "Loading " + Integer.toString(entries) + " entries from file " + fName + " Version:" + fileVersion);

            for (int iLoop = 0; iLoop < entries; iLoop++) {
                DataSet ds = new DataSet();
                ds.Time = inp.readInt();
                len = inp.readInt();
                ds.ID = new String(inp.readNBytes(len));

                len = inp.readInt();
                ds.Namespace = new String(inp.readNBytes(len));

                len = inp.readInt();
                ds.Data = new String(inp.readNBytes(len));
                retList.add(ds);
                // LOGGER.info(Integer.toString(iLoop));
            }

        } catch (IOException e) {
            LOGGER.severe("Invalid Playback File: " + fName);
            return null;
        } finally {
            try {
                inp.close();
            } catch (IOException e) {
            }
        }

        return retList;

    }

    public void resumePlayback() {
        if (!paused) {
            LOGGER.warning("Asked to resume Playback " + getName() + ", when not paused.");
        }
        paused = false;
    }

    @Override
    public void run() {
        DataManager dm = DataManager.getDataManager();
        int lastInterval;

        playing = true;
        while (false == terminate) {
            lastInterval = 0;
            while (nextEntry < _playbackData.size() && !terminate) {
                DataSet dp = _playbackData.get(nextEntry);
                int interval = dp.Time;
                if (lastInterval == interval) { // multiple datapoints came it @ same time (like a group), just blast through
                    // them
                    dm.ChangeValue(dp.ID, dp.Namespace, dp.Data);
                } else {
                    try {
                        double sleepTime = (interval - lastInterval) / playbackSpeed;

                        Thread.sleep((long) sleepTime);

                        lastInterval = interval;

                        dm.ChangeValue(dp.ID, dp.Namespace, dp.Data);
                    } catch (InterruptedException e) {
                    }
                }
                while (paused && !terminate) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        // e.printStackTrace();
                    }
                }

                nextEntry++;
            }
            if (loopPlayback) {
                nextEntry = 0;
            } else {
                terminate = true;
            }
        }
        playbackThread = null;
        LOGGER.info("Playback [" + getName() + "] Finished");
        playing = false;
    }

    public void setRepeat(boolean repeat) {
        loopPlayback = repeat;
    }

    public void setSpeed(double newSpeed) {
        playbackSpeed = newSpeed;
    }

    public void stopPlayback() {
        terminate = true;
        if (paused) {
            paused = false;
        }
        while (playing) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
            }
        }
    }
}
