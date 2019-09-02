package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

// IdentityData example simple Chaincode implementation
type IdentityData struct {
}

type identity struct {
	ObjectType string `json:"docType"` //docType is used to distinguish the various types of objects in state database
	Identifier string `json:"identifier"`
	Syn_data    string `json:"syn_data"`
	Id_type      string `json:"id_type"`
	Root_id     string `json:"root_id"`
	Invoke_time  int64  `json:"invoke_time"`
}

// ===================================================================================
// Main
// ===================================================================================
func main() {
	err := shim.Start(new(IdentityData))
	if err != nil {
		fmt.Printf("Error starting Simple chaincode: %s", err)
	}
}

// Init initializes chaincode
// ===========================
func (t *IdentityData) Init(stub shim.ChaincodeStubInterface) pb.Response {
	return shim.Success(nil)
}

// Invoke - Our entry point for Invocations
// ========================================
func (t *IdentityData) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	function, args := stub.GetFunctionAndParameters()
	fmt.Println("invoke is running " + function)

	// Handle different functions
	if function == "initIdentity" { //create identity
		return t.initIdentity(stub, args)
	} else if function == "delete" { //delete identity
		return t.delete(stub, args)
	} else if function == "quaryIdentityByName" {
		return t.quaryIdentityByName(stub, args)
	} else if function == "queryIdentityByType" {
		return t.queryIdentityByType(stub, args)
	}

	fmt.Println("invoke did not find func: " + function) //error
	return shim.Error("Received unknown function invocation")
}

// ============================================================
// initIdentity - create a new Identity, store into chaincode state
// ============================================================
func (t *IdentityData) initIdentity(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var err error

	//  0                     1                  2			3     4
	//  "10/ditu",    		  "www.baidu.com"    "handle",	"01"  -current time
	//  "20/weichat", 		  "www.qq.com"       "handle",	"01"
	//  "50/weibo",   		  "www.sina.com"     "handle",	"01"
	//  "1.2.156.1",  		  "www.bupt.edu.cn"  "OID",		"01"
	//  "1.2.156.1.2", 		  "www.fnl.cn"       "OID",		"01"
	//  "100036901234567892", "www.nongye.cn"    "Ecode",	"01"

	if len(args) != 4 {
		return shim.Error("Incorrect number of arguments. Expecting 4")
	}

	// ==== Input sanitation ====
	fmt.Println("- start init identity")
	if len(args[0]) <= 0 {
		return shim.Error("1st argument must be a non-empty string")
	}
	if len(args[1]) <= 0 {
		return shim.Error("2nd argument must be a non-empty string")
	}
	if len(args[2]) <= 0 {
		return shim.Error("3rd argument must be a non-empty string")
	}
	if len(args[3]) <= 0 {
		return shim.Error("4th argument must be a non-empty string")
	}
	identifier := args[0]
	syn_data := args[1]
	id_type:= strings.ToLower(args[2])
	root_id := args[3]
	invoke_time := time.Now().Unix()

	// ==== Check if identity already exists ====
	// ==== if annotation, mains can be modified ====

	// identityAsBytes, err := stub.GetState(identityName)
	// if err != nil {
	// 	return shim.Error("Failed to get identity: " + err.Error())
	// } else if identityAsBytes != nil {
	// 	fmt.Println("This identity already exists: " + identityName)
	// 	return shim.Error("This identity already exists: " + identityName)
	// }

	// ==== Create identity object and marshal to JSON ====
	objectType := "identity"
	identity := &identity{objectType, identifier, syn_data, id_type, root_id, invoke_time}
	identityJSONasBytes, err := json.Marshal(identity)
	if err != nil {
		return shim.Error(err.Error())
	}

	// === Save identity to state ===
	err = stub.PutState(identifier, identityJSONasBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	// === Index the identity to enable type-based range queries, e.g. return all handle ===
	indexName := "type~identifier"
	typeNameIndexKey, err := stub.CreateCompositeKey(indexName, []string{identity.Id_type, identity.Identifier})
	if err != nil {
		return shim.Error(err.Error())
	}
	//  Save index entry to state. Only the key name is needed, no need to store a duplicate copy of the identity.
	//  Note - passing a 'nil' value will effectively delete the key from state, therefore we pass null character as value
	value := []byte{0x00}
	stub.PutState(typeNameIndexKey, value)

	// ==== Identity saved and indexed. Return success ====
	fmt.Println("- end init identity")
	return shim.Success(nil)
}

// ===============================================
// quaryIdentityByName - read a identity from chaincode state
// ===============================================
func (t *IdentityData) quaryIdentityByName(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var identifier, jsonResp string
	var err error

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting name of the Identity to query")
	}

	identifier = args[0]
	valAsbytes, err := stub.GetState(identifier) //get the identity from chaincode state
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + identifier + "\"}"
		return shim.Error(jsonResp)
	} else if valAsbytes == nil {
		jsonResp = "{\"Error\":\"Identity does not exist: " + identifier + "\"}"
		return shim.Error(jsonResp)
	}

	return shim.Success(valAsbytes)
}

// ==================================================
// delete - remove a identity key/value pair from state
// ==================================================
func (t *IdentityData) delete(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var jsonResp string
	var identityJSON identity
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}
	identifier := args[0]

	// to maintain the type~name index, we need to read the identity first and get its type
	valAsbytes, err := stub.GetState(identifier) //get the identity from chaincode state
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + identifier + "\"}"
		return shim.Error(jsonResp)
	} else if valAsbytes == nil {
		jsonResp = "{\"Error\":\"Identity does not exist: " + identifier + "\"}"
		return shim.Error(jsonResp)
	}

	err = json.Unmarshal([]byte(valAsbytes), &identityJSON)
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to decode JSON of: " + identifier + "\"}"
		return shim.Error(jsonResp)
	}

	err = stub.DelState(identifier) //remove the identityidentity from chaincode state
	if err != nil {
		return shim.Error("Failed to delete state:" + err.Error())
	}

	// maintain the index
	indexName := "type~identifier"
	typeNameIndexKey, err := stub.CreateCompositeKey(indexName, []string{identityJSON.Id_type, identityJSON.Identifier})
	if err != nil {
		return shim.Error(err.Error())
	}

	//  Delete index entry to state.
	err = stub.DelState(typeNameIndexKey)
	if err != nil {
		return shim.Error("Failed to delete state:" + err.Error())
	}
	return shim.Success(nil)
}

// ===========================================================================================
// constructQueryResponseFromIterator constructs a JSON array containing query results from
// a given result iterator
// ===========================================================================================
func constructQueryResponseFromIterator(resultsIterator shim.StateQueryIteratorInterface) (*bytes.Buffer, error) {
	// buffer is a JSON array containing QueryResults
	var buffer bytes.Buffer
	buffer.WriteString("[")

	bArrayMemberAlreadyWritten := false
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return nil, err
		}
		// Add a comma before array members, suppress it for the first array member
		if bArrayMemberAlreadyWritten == true {
			buffer.WriteString(",")
		}
		buffer.WriteString("{\"Key\":")
		buffer.WriteString("\"")
		buffer.WriteString(queryResponse.Key)
		buffer.WriteString("\"")

		buffer.WriteString(", \"Record\":")
		// Record is a JSON object, so we write as-is
		buffer.WriteString(string(queryResponse.Value))
		buffer.WriteString("}")
		bArrayMemberAlreadyWritten = true
	}
	buffer.WriteString("]")

	return &buffer, nil
}

func getQueryResultForQueryString(stub shim.ChaincodeStubInterface, queryString string) ([]byte, error) {

	fmt.Printf("- getQueryResultForQueryString queryString:\n%s\n", queryString)

	resultsIterator, err := stub.GetQueryResult(queryString)
	if err != nil {
		return nil, err
	}
	defer resultsIterator.Close()

	buffer, err := constructQueryResponseFromIterator(resultsIterator)
	if err != nil {
		return nil, err
	}

	fmt.Printf("- getQueryResultForQueryString queryResult:\n%s\n", buffer.String())

	return buffer.Bytes(), nil
}

// =======Rich queries =========================================================================
// Two examples of rich queries are provided below (parameterized query and ad hoc query).
// Rich queries pass a query string to the state database.
// Rich queries are only supported by state database implementations
//  that support rich query (e.g. CouchDB).
// The query string is in the syntax of the underlying state database.
// With rich queries there is no guarantee that the result set hasn't changed between
//  endorsement time and commit time, aka 'phantom reads'.
// Therefore, rich queries should not be used in update transactions, unless the
// application handles the possibility of result set changes between endorsement and commit time.
// Rich queries can be used for point-in-time queries against a peer.
// ============================================================================================

// ===== Example: Parameterized rich query =================================================
// queryIdentityByType queries for identities based on a passed in type.
// This is an example of a parameterized query where the query logic is baked into the chaincode,
// and accepting a single query parameter (type).
// Only available on state databases that support rich query (e.g. CouchDB)
// =========================================================================================
func (t *IdentityData) queryIdentityByType(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	//   0
	// "handle"
	if len(args) < 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	id_type := strings.ToLower(args[0])

	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"identifier\",\"id_type\":\"%s\"}}", id_type)

	queryResults, err := getQueryResultForQueryString(stub, queryString)
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(queryResults)
}
