
package org.app.chaincode.invocation;

import org.json.JSONException;
import org.json.JSONObject;
import org.app.client.CAClient;
import org.app.client.ChannelClient;
import org.app.client.FabricClient;
import org.app.config.Config;
import org.app.user.UserContext;
import org.app.util.Util;
import org.hyperledger.fabric.sdk.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class InvokeChaincode {

    private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
    private static final String EXPECTED_EVENT_NAME = "event";

    private static FabricClient fabClient_invoke;
    private static ChannelClient channelClient_invoke;

    // 初始化配置信息
    public void initInvoke(String eroll_name, String eroll_address){
        try {
            Util.cleanUp();
            String caUrl = Config.CA_ORG1_URL;
            CAClient caClient = new CAClient(caUrl, null);
            // Enroll Admin to Org1MSP
            UserContext adminUserContext = new UserContext();
            adminUserContext.setName(Config.ADMIN);
            adminUserContext.setAffiliation(Config.ORG1);
            adminUserContext.setMspId(Config.ORG1_MSP);
            caClient.setAdminUserContext(adminUserContext);
            adminUserContext = caClient.enrollAdminUser(Config.ADMIN,Config.ADMIN_PASSWORD);

            fabClient_invoke = new FabricClient(adminUserContext);

            channelClient_invoke = fabClient_invoke.createChannelClient(Config.CHANNEL_NAME);
            Channel channel = channelClient_invoke.getChannel();
            Peer peer = fabClient_invoke.getInstance().newPeer(eroll_name, eroll_address);
            Orderer orderer = fabClient_invoke.getInstance().newOrderer(Config.ORDERER_NAME, Config.ORDERER_URL);
            channel.addPeer(peer);
            channel.addOrderer(orderer);
            channel.initialize();
            Logger.getLogger(InvokeChaincode.class.getName()).log(Level.INFO, "准备添加数据...");
        } catch (Exception e) {
            System.out.println("配置信息初始化失败...");
            e.printStackTrace();
        }
    }
    // 添加数据
    public void invoke(JSONObject dataJson){
        try {
            TransactionProposalRequest request = fabClient_invoke.getInstance().newTransactionProposalRequest();
            ChaincodeID ccid = ChaincodeID.newBuilder().setName(Config.CHAINCODE_1_NAME).build();
            request.setChaincodeID(ccid);
            request.setFcn("initIdentity");
            String[] arguments = new String[4];
            arguments[0] = dataJson.getString("identifier");
            arguments[1] = dataJson.getString("syn_data");
            arguments[2] = dataJson.getString("id_type");
            arguments[3] = dataJson.getString("root_id");
            request.setArgs(arguments);
            request.setProposalWaitTime(1000);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
            tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
            tm2.put("result", ":)".getBytes(UTF_8));
            tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);
            request.setTransientMap(tm2);
            Collection<ProposalResponse> responses = channelClient_invoke.sendTransactionProposal(request);
            for (ProposalResponse res: responses) {
                ChaincodeResponse.Status status = res.getStatus();
                Logger.getLogger(InvokeChaincode.class.getName()).log(Level.INFO, "【系统提示】添加数据 - " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws JSONException {

//        JSONObject configJson = new JSONObject();
//        configJson.put("caUrl","http://localhost:7054");
//        configJson.put("Admin","admin");
//        configJson.put("Adminpw","adminpw");
//        configJson.put("Eroll_Name","peer0.org1.example.com");
//        configJson.put("Eroll_Address","grpc://localhost:7051");
//        configJson.put("Orderer_Name","orderer.example.com");
//        configJson.put("Orderer_Address","grpc://localhost:7050");

        JSONObject dataJson = new JSONObject();
        dataJson.put("identifier", "bupt");
        dataJson.put("syn_data", "10.20.30.40");
        dataJson.put("id_type", "dns");
        dataJson.put("root_id", "10");

        JSONObject dataJson1 = new JSONObject();
        dataJson1.put("identifier", "fnl");
        dataJson1.put("syn_data", "9.8.7.6");
        dataJson1.put("id_type", "handle");
        dataJson1.put("root_id", "22");

        InvokeChaincode invokeChaincode = new InvokeChaincode();
        invokeChaincode.initInvoke("entry0", "grpc://localhost:7051");
        invokeChaincode.invoke(dataJson);
        invokeChaincode.invoke(dataJson1);
    }

    /*
    配置信息：configJson
        包含："caUrl"、"Admin"、"Adminpw"、"Eroll_Name"、"Eroll_Address"、
        "Orderer_Name"、"Orderer_Address" 共7个条目

    数据信息：dataJson
        包含："identifier"、"syn_data"、"id_type"、"root_id" 共4个条目
    */
}
