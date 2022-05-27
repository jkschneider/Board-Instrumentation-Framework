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
package kutch.biff.marvin.network;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;

/**
 * @author Patrick Kutch
 */
public class Server {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    private DatagramSocket socket;
    private ReceiveThreadMgr recvThreadMgr;
    private Thread thread;
    private DataManager dataManager;

    public Server(DataManager dm) {
        socket = null;
        recvThreadMgr = null;
        dataManager = dm;
    }

    public boolean Setup(String ipAddr, int port) {
        InetAddress address;
        try {
            address = InetAddress.getByName(ipAddr);
        } catch (UnknownHostException ex) {
            LOGGER.severe("Problem setting up network - likely something wrong with Address or Port: " + ipAddr + ":"
                    + Integer.toString(port));
            return false;
        }

        try {
            socket = new DatagramSocket(port, address);
            socket.setSoTimeout(10);
        } catch (SocketException ex) {
            LOGGER.severe(
                    "Problem setting up network - likely something already using port: " + Integer.toString(port));
            return false;
        }
        return true;
    }

    public void Start() {
        recvThreadMgr = new ReceiveThreadMgr(socket, dataManager);
        thread = new Thread(recvThreadMgr, "Receve Thread Manager Worker");
        thread.start();
    }

    public void Stop() {
        if (null != recvThreadMgr) {
            recvThreadMgr.Stop();
        }
    }
}
