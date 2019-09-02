import org.app.chaincode.invocation.InvokeChaincode;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) throws JSONException {

        JSONObject configJson = new JSONObject();
        configJson.put("caUrl","http://10.108.151.51:7054");
        configJson.put("Admin","admin");
        configJson.put("Adminpw","adminpw");
        configJson.put("Eroll_Name","peer0.org1.example.com");
        configJson.put("Eroll_Address","grpc://10.108.151.51:7051");
        configJson.put("Orderer_Name","orderer.example.com");
        configJson.put("Orderer_Address","grpc://10.108.151.51:7050");

        JSONObject dataJson = new JSONObject();
        dataJson.put("identifier", "bupt");
        dataJson.put("syn_data", "10.20.30.40");
        dataJson.put("id_type", "dns");
        dataJson.put("root_id", "10");

        new InvokeChaincode().invoke(configJson, "initIdentity", dataJson);
    }
}
