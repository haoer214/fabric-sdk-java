package org.app.network;

import org.app.client.ChannelClient;
import org.app.client.FabricClient;
import org.app.config.Config;
import org.app.user.UserContext;
import org.app.util.Util;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Zhao Haoran
 *
 */

public class Manager {

    private UserContext org1Admin;
    private FabricClient fabClient;
    private Orderer orderer;
    private Channel mychannel;

    Peer[] entrys;
    String[] entrys_name;
    String[] entrys_url;

    Peer[] peers;
    String[] peers_name;
    String[] peers_url;

    JSONArray entryArr;
    JSONArray peerArr;

    public Manager(JSONObject jsonObject) throws JSONException {

        this.entryArr = jsonObject.getJSONArray("entry");
        this.entrys_name = new String[entryArr.length()];
        this.entrys_url = new String[entryArr.length()];
        for(int i = 0; i < entryArr.length(); i++){
            entrys_name[i] = "entry" + i;
            entrys_url[i] = "grpc://" + entryArr.getJSONObject(i).getString("ip") + ":" + entryArr.getJSONObject(i).getString("port");
        }
        this.peerArr = jsonObject.getJSONArray("peer");
        this.peers_name = new String[peerArr.length()];
        this.peers_url = new String[peerArr.length()];
        for(int i = 0; i < peerArr.length(); i++){
            peers_name[i] = "peer" + i;
            peers_url[i] = "grpc://" + peerArr.getJSONObject(i).getString("ip") + ":" + peerArr.getJSONObject(i).getString("port");
        }

        try {
            CryptoSuite.Factory.getCryptoSuite();
            Util.cleanUp();
            // Construct Channel
            org1Admin = new UserContext();
            File pkFolder1 = new File(Config.ORG1_USR_ADMIN_PK);
            File[] pkFiles1 = pkFolder1.listFiles();
            File certFolder1 = new File(Config.ORG1_USR_ADMIN_CERT);
            File[] certFiles1 = certFolder1.listFiles();
            Enrollment enrollOrg1Admin = Util.getEnrollment(Config.ORG1_USR_ADMIN_PK, pkFiles1[0].getName(),
                    Config.ORG1_USR_ADMIN_CERT, certFiles1[0].getName());
            org1Admin.setEnrollment(enrollOrg1Admin);
            org1Admin.setMspId(Config.ORG1_MSP);
            org1Admin.setName(Config.ADMIN);
            fabClient = new FabricClient(org1Admin);
            // Create a new channel
            orderer = fabClient.getInstance().newOrderer(Config.ORDERER_NAME, Config.ORDERER_URL);
            ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(Config.CHANNEL_CONFIG_PATH));
            byte[] channelConfigurationSignatures = fabClient.getInstance()
                    .getChannelConfigurationSignature(channelConfiguration, org1Admin);
            mychannel = fabClient.getInstance().newChannel(Config.CHANNEL_NAME, orderer, channelConfiguration,
                    channelConfigurationSignatures);
            Logger.getLogger(CreateChannel.class.getName()).log(Level.INFO, "Channel created - "+ mychannel.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Join Entrys
            Logger.getLogger(Util.class.getName()).log(Level.INFO, "Joining - Entrys ");
            entrys = new Peer[entryArr.length()];
            for (int p = 0; p < entrys.length; p++) {
                entrys[p] = fabClient.getInstance().newPeer(entrys_name[p], entrys_url[p]);
                mychannel.joinPeer(entrys[p]);
            }
            mychannel.addOrderer(orderer);
            mychannel.initialize();

            Collection peers = mychannel.getPeers();
            Iterator peerIter = peers.iterator();
            while (peerIter.hasNext())
            {
                Peer pr = (Peer) peerIter.next();
                Logger.getLogger(CreateChannel.class.getName()).log(Level.INFO,pr.getName()+ " at " + pr.getUrl());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Deploy chaincode
            List<Peer> org1Peers = new ArrayList<>();
            for (int p = 0; p < entrys.length; p++) {
                org1Peers.add(entrys[p]);
            }
            mychannel.initialize();
            Collection<ProposalResponse> response = fabClient.deployChainCode(Config.CHAINCODE_1_NAME,
                    Config.CHAINCODE_1_PATH, Config.CHAINCODE_ROOT_DIR, TransactionRequest.Type.GO_LANG.toString(),
                    Config.CHAINCODE_1_VERSION, org1Peers);
            int i = 0;
            for (ProposalResponse res : response) {
                Logger.getLogger(DeployInstantiateChaincode.class.getName()).log(Level.INFO,
                        Config.CHAINCODE_1_NAME + " - Chain code deployment at " + entrys_name[i++]+ " - " + res.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Instantiate Chaincode
            boolean instan_success = false;
            while (instan_success == false){
                ChannelClient channelClient = new ChannelClient(mychannel.getName(), mychannel, fabClient);
                String[] arguments = { "" };
                Collection<ProposalResponse> response = channelClient.instantiateChainCode(Config.CHAINCODE_1_NAME, Config.CHAINCODE_1_VERSION,
                        Config.CHAINCODE_1_PATH, TransactionRequest.Type.GO_LANG.toString(), "init", arguments, null);
                int i = 0;
                for (ProposalResponse res : response) {
                    if (res.getStatus() != ChaincodeResponse.Status.SUCCESS) {
                        instan_success = false;
                        break;
                    }
                    instan_success = true;
                    Logger.getLogger(DeployInstantiateChaincode.class.getName()).log(Level.INFO,
                            Config.CHAINCODE_1_NAME + " - Chain code instantiation at " + entrys_name[i++]+ " - " + res.getStatus());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Join Peers
            Logger.getLogger(Util.class.getName()).log(Level.INFO, "Joining - Peers ");
            peers = new Peer[peerArr.length()];
            for (int p = 0; p < peers.length; p++) {
                peers[p] = fabClient.getInstance().newPeer(peers_name[p], peers_url[p]);
                mychannel.joinPeer(peers[p]);
            }
            mychannel.addOrderer(orderer);
            mychannel.initialize();

            Collection peers = mychannel.getPeers();
            Iterator peerIter = peers.iterator();
            while (peerIter.hasNext())
            {
                Peer pr = (Peer) peerIter.next();
                Logger.getLogger(CreateChannel.class.getName()).log(Level.INFO,pr.getName()+ " at " + pr.getUrl());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isOver(){
        return true;
    }

//    public void createChannel(){
//
//    }
//
//    public void joinPeers() {
//
//    }
//
//    public void deployChaincode() {
//
//    }
//
//    public void instantiateChaincode(){
//
//    }

//     public static void main(String[] args) throws JSONException {
//
//         JSONObject jsonObject = new JSONObject();
//
//         JSONArray entryArray = new JSONArray();
//         JSONObject entry0 = new JSONObject();
//         entry0.put("ip", "10.108.151.51");
//         entry0.put("port", "7051");
//         JSONObject entry1 = new JSONObject();
//         entry1.put("ip", "10.108.151.51");
//         entry1.put("port", "7056");
//         entryArray.put(entry0);
//         entryArray.put(entry1);
//
//         JSONArray peerArray = new JSONArray();
//         JSONObject peer0 = new JSONObject();
//         peer0.put("ip", "10.108.151.51");
//         peer0.put("port", "8051");
//         JSONObject peer1 = new JSONObject();
//         peer1.put("ip", "10.108.151.51");
//         peer1.put("port", "8056");
//         JSONObject peer2 = new JSONObject();
//         peer2.put("ip", "10.108.151.51");
//         peer2.put("port", "9051");
//         peerArray.put(peer0);
//         peerArray.put(peer1);
//         peerArray.put(peer2);
//
//         jsonObject.put("entry",entryArray);
//         jsonObject.put("peer",peerArray);
//
//         new Manager(jsonObject);
//     }
}
