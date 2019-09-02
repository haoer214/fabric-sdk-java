package org.app.config;

import java.io.File;

public class Config {
	
	public static final String ORG1_MSP = "Org1MSP";

	public static final String ORG1 = "org1";

	public static final String ADMIN = "admin";

	public static final String ADMIN_PASSWORD = "adminpw";
	
	public static final String CHANNEL_CONFIG_PATH = "config/channel.tx";
	
	public static final String ORG1_USR_BASE_PATH = "crypto-config" + File.separator + "peerOrganizations" + File.separator
			+ "org1.example.com" + File.separator + "users" + File.separator + "Admin@org1.example.com"
			+ File.separator + "msp";
	
	public static final String ORG1_USR_ADMIN_PK = ORG1_USR_BASE_PATH + File.separator + "keystore";
	public static final String ORG1_USR_ADMIN_CERT = ORG1_USR_BASE_PATH + File.separator + "admincerts";
	
	public static final String CA_ORG1_URL = "http://10.108.151.51:7054";
	
	public static final String ORDERER_URL = "grpc://10.108.151.51:7050";
	
	public static final String ORDERER_NAME = "orderer.example.com";
	
	public static final String CHANNEL_NAME = "mychannel";
	
//	public static final String ORG1_PEER_0 = "peer0.org1.example.com";
//
//	public static final String ORG1_PEER_0_URL = "grpc://" + ReadTxt.ip_port.get(7) + ":" + ReadTxt.ip_port.get(8);
//
//	public static final String ORG1_PEER_1 = "peer1.org1.example.com";
//
//	public static final String ORG1_PEER_1_URL = "grpc://" + ReadTxt.ip_port.get(10) + ":" + ReadTxt.ip_port.get(11);
//
//	public static final String ORG1_PEER_2 = "peer2.org1.example.com";
//
//	public static final String ORG1_PEER_2_URL = "grpc://" + ReadTxt.ip_port.get(13) + ":" + ReadTxt.ip_port.get(14);
//
//	public static final String ORG1_PEER_3 = "peer3.org1.example.com";
//
//	public static final String ORG1_PEER_3_URL = "grpc://" + ReadTxt.ip_port.get(16) + ":" + ReadTxt.ip_port.get(17);
//
//	public static final String ORG1_PEER_4 = "peer4.org1.example.com";
//
//	public static final String ORG1_PEER_4_URL = "grpc://" + ReadTxt.ip_port.get(19) + ":" + ReadTxt.ip_port.get(20);
	
	public static final String CHAINCODE_ROOT_DIR = "chaincode";
	
	public static final String CHAINCODE_1_NAME = "identity";
	
	public static final String CHAINCODE_1_PATH = "github.com/identity";
	
	public static final String CHAINCODE_1_VERSION = "1";

}
