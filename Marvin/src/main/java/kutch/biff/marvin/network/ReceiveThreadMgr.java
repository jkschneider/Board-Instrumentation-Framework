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

import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import kutch.biff.marvin.configuration.Configuration;
import kutch.biff.marvin.datamanager.DataManager;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.FrameworkNode;

/**
 * @author Patrick Kutch
 */
public class ReceiveThreadMgr implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private static final Configuration CONFIG = Configuration.getConfig();
    private final TaskManager taskman = TaskManager.getTaskManager();

    private boolean fStopped;
    private boolean fKillRequested;
    private final DatagramSocket socket;
    private final DataManager dataManager;
    private final HashMap<String, String> lastMarvinTaskReceived;
    private final LinkedBlockingQueue<HashMap<InetAddress, String>> dataQueue;
    private final AtomicInteger workerThreadCount;

    public ReceiveThreadMgr(DatagramSocket sock, DataManager dm) {
        fStopped = false;
        fKillRequested = false;
        socket = sock;
        dataManager = dm;
        lastMarvinTaskReceived = new HashMap<>();
        dataQueue = new LinkedBlockingQueue<>();
        workerThreadCount = new AtomicInteger();
        workerThreadCount.set(0);
    }

    private void HandleIncomingDatapoint(Node dpNode) {
        NodeList Children = dpNode.getChildNodes();
        String id = null;
        String namespace = null;
        String data = null;
        for (int iLoop = 0; iLoop < Children.getLength(); iLoop++) {
            Node node = Children.item(iLoop);
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            } else if ("Version".equalsIgnoreCase(node.getNodeName())) {

            } else if ("Namespace".equalsIgnoreCase(node.getNodeName())) {
                namespace = node.getTextContent();
            } else if ("ID".equalsIgnoreCase(node.getNodeName())) {
                id = node.getTextContent();
            } else if ("Value".equalsIgnoreCase(node.getNodeName())) {
                data = node.getTextContent();
                FrameworkNode objNode = new FrameworkNode(node);
                if (objNode.hasAttribute("LiveData")) {
                    String strLive = objNode.getAttribute("LiveData");
                    if ("True".equalsIgnoreCase(strLive)) {
                        CONFIG.OnLiveDataReceived();
                    } else if ("False".equalsIgnoreCase(strLive)) {
                        CONFIG.OnRecordedDataReceived();
                    } else {
                        LOGGER.warning("Received Data packet with unknown LiveData attribute: " + strLive);
                    }
                }

            } else {
                LOGGER.warning("Unknown Tag in received Datapoint: " + node.getNodeName());
            }
        }

        if (id != null && namespace != null && data != null) {
            dataManager.ChangeValue(id, namespace, data);
        } else {
            LOGGER.severe("Malformed Data Received: " + dpNode.getTextContent());
        }
    }

    private void HandleIncomingMarvinPacket(Node objNode) {
        FrameworkNode node = new FrameworkNode(objNode);
        if (!node.hasAttribute("Type")) {
            LOGGER.severe("Received Marvin Packet with not Type attribute");
            return;
        }
        String type = node.getAttribute("Type");
        if ("RemoteMarvinTask".equalsIgnoreCase(type)) {
            HandleIncomingRemoteMarvinTaskPacket(objNode);
        } else {
            LOGGER.severe("Received Oscar Packet with unknown Type: " + type);
        }

    }

    private void HandleIncomingOscarConnectionInfoPacket(Node adminNode, InetAddress address) {
        NodeList Children = adminNode.getChildNodes();
        String oscarID = null;
        String oscarVersion = "Unknown";
        int port = 0;

        for (int iLoop = 0; iLoop < Children.getLength(); iLoop++) {
            Node node = Children.item(iLoop);
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            } else if ("Version".equals(node.getNodeName())) {
                continue;
            } else if ("OscarVersion".equals(node.getNodeName())) {
                oscarVersion = node.getTextContent();
            } else if ("ID".equals(node.getNodeName())) {
                oscarID = node.getTextContent();
            }
            if ("Port".equalsIgnoreCase(node.getNodeName())) {
                String strPort = node.getTextContent();
                try {
                    port = Integer.parseInt(strPort);
                } catch (NumberFormatException ex) {
                    LOGGER.severe("Received invalid Connection Information packet from Oscar " + node.toString());
                }
            }
        }
        if (oscarID != null) {
            taskman.OscarAnnouncementReceived(oscarID, address.getHostAddress(), port, oscarVersion);
        }
    }

    /**
     * Parses through a grouped packet
     *
     * @param objNode
     * @param address
     */
    private void HandleIncomingOscarGroupPacket(Node objNode, InetAddress address) {
        NodeList children = objNode.getChildNodes();
        for (int iLoop = 0; iLoop < children.getLength(); iLoop++) {
            HandleIncomingOscarPacket(children.item(iLoop), address);
        }
    }

    private void HandleIncomingOscarPacket(Node objNode, InetAddress address) {
        FrameworkNode node = new FrameworkNode(objNode);
        if (!node.hasAttribute("Type")) {
            LOGGER.severe("Received Oscar Packet with not Type attribute");
            return;
        }
        String type = node.getAttribute("Type");
        if ("Data".equalsIgnoreCase(type)) {
            HandleIncomingDatapoint(objNode);
        } else if ("ConnectionInformation".equalsIgnoreCase(type)) {
            HandleIncomingOscarConnectionInfoPacket(objNode, address);
        } else {
            LOGGER.severe("Received Oscar Packet with unknown Type: " + type);
        }
    }

    private void HandleIncomingRemoteMarvinTaskPacket(Node baseNode) {
        /*
         * <?xml version=\"1.0\" encoding=\"UTF-8\"?> <RemoteMarvinTask>
         * <Version>1.0</Version> <Requester> 192.168.1.1</Requester>
         * <MarvinID>DemoApp</MarvinID> <Task>Button2Push</Task> </RemoteMarvinTask>
         */
        FrameworkNode node = new FrameworkNode(baseNode);
        try {
            String version = node.getChild("Version").getTextContent();
            String remote = node.getChild("Requester").getTextContent();
            String marvinID = node.getChild("MarvinID").getTextContent();
            String Task = node.getChild("Task").getTextContent();
//	    if (!Version.equalsIgnoreCase("1.0"))
            {
                String requestNumber = node.getChild("RequestNumber").getTextContent();
                if (lastMarvinTaskReceived.containsKey(remote)) {
                    if (lastMarvinTaskReceived.get(remote).equalsIgnoreCase(requestNumber)) {
                        return; // Already received this one, remember is UDP so it is sent a few times
                    }
                }
                lastMarvinTaskReceived.put(remote, requestNumber);
            }

            if (false == marvinID.equalsIgnoreCase(CONFIG.GetApplicationID())
                    && false == "Broadcast".equalsIgnoreCase(marvinID)) {
                LOGGER.info(
                        "Received Remote Marvin Task, but is not targeted at this Marvin, is going to :" + marvinID);
                return;
            }
            LOGGER.info("Received RemoteMarvinTask [" + Task + " ]from [" + remote + "]");
            taskman.AddDeferredTask(Task); // can't run it here, because in worker thread, so queue it up for later
        } catch (Exception ex) {
            LOGGER.warning("Received invalid RemoteMarvinTask:" + baseNode.toString());
        }
    }

    private void Process(byte[] Packet, InetAddress address) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        Document doc;
        String str = new String(Packet);

        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return;
        }

        try {
            doc = db.parse(new InputSource(new StringReader(str)));
        } catch (SAXException | IOException ex) {
            LOGGER.warning("Received Invalid packet: " + str);
            LOGGER.warning(ex.toString());

            return;
        }
        NodeList Children = doc.getChildNodes(); // convert to my node
        for (int iLoop = 0; iLoop < Children.getLength(); iLoop++) {
            Node node = Children.item(iLoop);
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#comment".equalsIgnoreCase(node.getNodeName())) {
                if ("Oscar".equalsIgnoreCase(node.getNodeName())) {
                    HandleIncomingOscarPacket(node, address);
                } else if ("OscarGroup".equalsIgnoreCase(node.getNodeName())) {
                    HandleIncomingOscarGroupPacket(node, address);
                } else if ("Marvin".equalsIgnoreCase(node.getNodeName())) {
                    HandleIncomingMarvinPacket(node);
                } else {
                    LOGGER.warning("Unknown Packet received: " + node.getNodeName());
                }
            }
        }
    }

    @SuppressWarnings({"deprecation", "serial"})
    @Override
    public void run() {
        Runnable processQueuedDataThread = () -> {
            try {
                while (false == fKillRequested) {
                    if (!dataQueue.isEmpty()) {
                        HashMap<InetAddress, String> dataItem = (HashMap<InetAddress, String>) dataQueue.take();
                        // dataItem[dataItem.keySet()[0]]
                        InetAddress addr = dataItem.keySet().iterator().next();
                        Process(dataItem.get(addr).getBytes(), addr);
                    } else {
                        if (workerThreadCount.get() > 1) {
                            workerThreadCount.decrementAndGet();
                            // LOGGER.info("Reducing processing Thread Count");
                            return;
                        }
                        try {
                            Thread.sleep(2); // didn't read anything, socket read timed out, so take a nap
                        } catch (InterruptedException ex1) {
                        }
                    }
                }
                workerThreadCount.decrementAndGet();
                // LOGGER.info("Receive Queue Processing Thread successfully terminated.");
            } catch (InterruptedException e) {
                LOGGER.severe(e.toString());
            }
        };

        Thread procThread = new Thread(processQueuedDataThread, ">>>> Base Process Queue Thread <<<<<");

        procThread.start();
        workerThreadCount.set(1);

        while (false == fKillRequested) {
            byte[] buffer = new byte[CONFIG.getMaxPacketSize()];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                if (false == fKillRequested) {
                    String trimmed = new String(packet.getData(), 0, packet.getLength());

                    dataQueue.add(new HashMap<InetAddress, String>() {
                        {
                            put(packet.getAddress(), trimmed);
                            if (workerThreadCount.get() < 1 || dataQueue.size() / workerThreadCount.get() > 200) {
                                LOGGER.info("Traffic burst - adding processing Thread Count, there are "
                                        + Integer.toString(dataQueue.size()) + " packets to process.");
                                int threadNum = workerThreadCount.incrementAndGet();
                                Thread procThread = new Thread(processQueuedDataThread,
                                        ">>>> Additionial Process Queue Thread #" + Integer.toString(threadNum)
                                                + " <<<<<");
                                procThread.start();
                            }
                        }
                    });
                    // Process(trimmed.getBytes(), packet.getAddress());
                } else {
                    return; // kill
                }

            } catch (IOException ex) {
                if (false == fKillRequested) {
                    try {
                        Thread.sleep(1); // didn't read anything, socket read timed out, so take a nap
                    } catch (InterruptedException ex1) {
                    }
                }
            }
        }

        procThread.stop();
        fStopped = true;
    }

    public void Stop() {
        fStopped = false;
        fKillRequested = true;
        try {
            Thread.sleep(50); // let the worker theads have a chance to end
        } catch (InterruptedException ex) {

        }
        int tryCount = 0;
        while (false == fStopped || workerThreadCount.get() > 1) {
            tryCount += 1;

            try {
                Thread.sleep(50);
                // LOGGER.info("Waiting:" + Boolean.toString(_fStopped)+ ":" +
                // Integer.toString(_WorkerThreadCount.get()));
            } catch (InterruptedException ex) {
            }

            if (tryCount++ > 100) // don't think this will every happen again, fixed problem elsewhere
            {
                LOGGER.severe("Problem trying to terminate Receive Thread.  Using Brute Force.");
                for (Thread threadObj : Thread.getAllStackTraces().keySet()) {
                    if (threadObj.getState() == Thread.State.RUNNABLE) {
                        threadObj.interrupt();
                    }
                }
                return;
            }
        }
    }
}
