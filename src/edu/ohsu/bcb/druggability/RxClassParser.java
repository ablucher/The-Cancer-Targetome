package edu.ohsu.bcb.druggability;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import javax.lang.model.element.Element;

//added import for xml parsing here

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.reactome.r3.util.FileUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RxClassParser {

	
	//This method takes returns all the drug members of a particular class
	//Argument: query uql for GET request
	public void getRxClassDrugs(String rxClassURL, String queryResultFile, String outputFile) throws Exception{
		//create query result and final output files
		PrintStream rs = new PrintStream(queryResultFile);
		PrintStream os = new PrintStream(outputFile);
		
		//connect to RxClass url
		URL url = new URL(rxClassURL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		//we are issuing a GET request bc we want info from RxClass
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/xml");
		
		//check for error
		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
		}
		
		//read in input stream
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		String queryResults;
		while ((queryResults = br.readLine()) != null) {
			rs.println(queryResults);//print to query file
		}
		
		//output is one string of XML 
		//we need to parse and get the info we need - drug names
		
		//create document builder
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        //create document from our output
        Document doc = builder.parse(queryResultFile);//parse the query results = XMl file
        //now iterate to desired element here - we want "name" to get drug name
        NodeList nodes = doc.getElementsByTagName("name");
        for (int i = 0; i < nodes.getLength(); i++) {
        	os.println("" + nodes.item(i).getTextContent());//print just drug names to final output file
      	
        }
        
        //remember to disconnect
        conn.disconnect();
        rs.close();
        os.close();
		
		
	}
	
	
	/**
	 * Method takes query for retrieving ATC class for a single drug. 
	 * Prints ATC class and ID to designated output file. 
	 * @param rxClassURL
	 * @param queryResultFile
	 * @param os
	 * @throws Exception
	 */
	public void getRxClassATC(String rxClassURL, String queryResultFile, PrintStream os) throws Exception{
		//create query result and final output files
		PrintStream rs = new PrintStream(queryResultFile);
		
		//connect to RxClass url
		URL url = new URL(rxClassURL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		//we are issuing a GET request bc we want info from RxClass
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/xml");
		
		//check for error
		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
		}
		
		//read in input stream
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		String queryResults;
		while ((queryResults = br.readLine()) != null) {
			System.out.println(queryResults);//print for now
			rs.println(queryResults);//print to query file
		}

		//create document builder
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(queryResultFile);//parse the query results = XMl file
		
		//System.out.println("Printing queryResult file: " + queryResultFile);
		
		//go to drugName
		NodeList nodes = doc.getElementsByTagName("drugName");
		String drugName = null;
		for (int i = 0; i < nodes.getLength(); i++) {
			drugName = nodes.item(i).getTextContent();
			//System.out.println("" + nodes.item(i).getTextContent());
		}
		//go to classID
		NodeList nodeClassIDs = doc.getElementsByTagName("classId");
		String atcClassID =null;
		for (int i = 0; i < nodeClassIDs.getLength(); i++) {
			atcClassID = nodeClassIDs.item(i).getTextContent();
			//System.out.println("" + nodeClassIDs.item(i).getTextContent());
		}


		NodeList nodeClassNames = doc.getElementsByTagName("className");
		String atcClassName = null;
		for (int i = 0; i < nodeClassNames.getLength(); i++) {
			atcClassName = nodeClassNames.item(i).getTextContent();
			//System.out.println("" + nodeClassNames.item(i).getTextContent());
		}

		NodeList nodeClassTypes = doc.getElementsByTagName("classType");
		String atcClassType =null;
		for (int i = 0; i < nodeClassTypes.getLength(); i++) {
			atcClassType = nodeClassTypes.item(i).getTextContent();
			//System.out.println("" + nodeClassTypes.item(i).getTextContent());
		}

		//print to output file
		//end of line done in testGetATCClass()
		os.print(drugName + "\t" + atcClassID + "\t" + atcClassName);

		//remember to disconnect
		conn.disconnect();
		rs.close();



	}
	
	/**
	 * Method takes query for retrieving EPC/MOA class for a single drug. 
	 * Prints  class and ID to designated output file. 
	 * @param rxClassURL
	 * @param queryResultFile
	 * @param os
	 * @throws Exception
	 */
	public void getRxClassEPC(String rxClassURL, String queryResultFile, PrintStream os) throws Exception{
		//create query result and final output files
		PrintStream rs = new PrintStream(queryResultFile);
		
		//connect to RxClass url
		URL url = new URL(rxClassURL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		//we are issuing a GET request bc we want info from RxClass
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/xml");
		
		//check for error
		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
		}
		
		//read in input stream
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		String queryResults;
		while ((queryResults = br.readLine()) != null) {
			System.out.println(queryResults);//print for now
			rs.println(queryResults);//print to query file
		}
		
		//output is one string of XML 
		//we need to parse and get the info we need - drug names
		
		//create document builder
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(queryResultFile);//parse the query results = XMl file
        System.out.println("Printing queryResult file: " + queryResultFile);
        
        String classID;
        String className;
        String classType;
        String epcClassId= null;
        String epcClassName = null;
        String moaClassId = null;
        String moaClassName = null;
        //go to rxclassminConceptItem
        NodeList nodes = doc.getElementsByTagName("rxclassMinConceptItem");
        //iterate through list of rxclassMinConcept Items
        for (int i = 0; i < nodes.getLength(); i++) {//each is 1 listing
        	Node rxClassMinConceptItem =  nodes.item(i);//get firt item
        	Node classIdN = rxClassMinConceptItem.getFirstChild();//go to child
        	classID = classIdN.getTextContent();//ID
        	Node classNameN = classIdN.getNextSibling();
        	className = classNameN.getTextContent();//NAME
        	Node classTypeN = classNameN.getNextSibling();
        	classType = classTypeN.getTextContent();//TYPE = EPC/MOA
        	if (classType.equals("EPC")){
        		epcClassId = classID;
        		epcClassName = className;
        	}
        	else if (classType.equals("MOA")){
        		moaClassId = classID;
        		moaClassName = className;
        	}
       }

        //print to output file
        //end of line done in testGetATCClass()
        os.print("\t" +epcClassId + "\t" + epcClassName+ "\t" + moaClassId + "\t" + moaClassName);
//        
        //remember to disconnect
        conn.disconnect();
        rs.close();
        
		
		
	}
	
	
	/**
	 * This method returns all pharm classes listed for a particular drug
	 * using RxClass RESTful API
	 * @throws Exception 
	 */
	public void getDrugClassesForDrug(String rxClassURL, PrintStream os) throws FileNotFoundException, MalformedURLException, Exception{

		//connect to RxClass url
		URL url = new URL(rxClassURL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		//we are issuing a GET request bc we want info from RxClass
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");

		//check for error
		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
		}

		//read in input stream
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		String queryResults;
		while ((queryResults = br.readLine()) != null) {
			System.out.println(queryResults);
			os.println(queryResults);
		}
		//try with json output
		JSONTokener tokener = new JSONTokener(br);//read in inputstream
		System.out.println("class: " + tokener.getClass());
		JSONObject obj = new JSONObject(tokener);
		
//		JSONArray array= new JSONArray(tokener);
//		JSONObject data = (JSONObject) array.get(0);
		

		conn.disconnect();
	}

	/**
	 * Test method for retrieving ATC, EPC, MOA classes for our drug list
	 * currently queries each drug separately  -should improve this.
	 * Working here Mon 10/10/16, Re-ran on 10/18/16
	 * @throws Exception
	 */
	@Test
	public void testGetDrugClasses() throws Exception{
		//get ATC class for this drug
		//String query = "https://rxnav.nlm.nih.gov/REST/rxclass/class/byDrugName.xml?drugName=imatinib&relaSource=ATC";
		
		PrintStream os = new PrintStream("resources/RxClass/DrugToClass_10.18.16.txt");
		os.println("Drug" + "\t" + "ATC_ClassID" + "\t"+ "ATC_ClassName" + "\t"+ "EPC_ClassID" + "\t"+ "EPC_ClassName" + "\t"+ "MOA_ClassID" + "\t"+ "MOA_ClassName");
		
		
		//open drug list file
		FileUtility fileUt = new FileUtility();
		fileUt.setInput("resources/DruggabilityV2_AllDrugs_10.18.16.txt");
		String line = null;
		//String drugName;
		//for each drug, we want to query ATC class information
		while ((line = fileUt.readLine()) != null){
			//String[] drugNameFull = line.split("\\s");//split on space and just take first part
			//String drugName = drugNameFull[0];
			String drugName = line;
			drugName = URLEncoder.encode(drugName, "UTF-8");//in case of space in name
			System.out.println("Drug: " + drugName);
			//atc query
			String queryPart1 = "https://rxnav.nlm.nih.gov/REST/rxclass/class/byDrugName.xml?drugName=";
			String queryPart2 = "&relaSource=ATC";
			//String drugName2 = URLEncoder.encode(drugName, "UTF-8");//try this?
			String queryFull = queryPart1 + drugName + queryPart2;
			System.out.println("Full query: " + queryFull);
			//query this drug and add info to the DrugToATC running file
			
			getRxClassATC(queryFull, "resources/RxClass/Drug_ATCQuery.xml", os);
			
			//add EPC query? use fdaspl **WORKING HERE**
			String EPCqueryPart1 = "https://rxnav.nlm.nih.gov/REST/rxclass/class/byDrugName.xml?drugName=";
			String EPCqueryPart2 = "&relaSource=FDASPL&rela=has_EPC";
			String EPCqueryFull = EPCqueryPart1 + drugName + EPCqueryPart2;
			System.out.println("Full query: " + EPCqueryFull);
			//query this drug and add info to the DrugToATC running file
			getRxClassEPC(EPCqueryFull, "resources/RxClass/Drug_EPCQuery.xml", os);
			os.println();
		}
		//tested on imatinib/ceritnib/erlotinib
		//works but we have multiple results coming back depending on the drugname
		//
		
		os.close();
	}
	
	
	
	@Test
	public void getATCDrugs() throws Exception{
		//get all drugs in ATC class L01: antineoplastics
		String query = "https://rxnav.nlm.nih.gov/REST/rxclass/classMembers?classId=L01&relaSource=ATC";
		getRxClassDrugs(query, "ATC_QueryResult.xml", "ATC_Antineoplastics_10.10.16.txt");
	}
	
	@Test
	public void getMESHPADrugs() throws Exception{
		//get all drugs in MESHPA: antineoplastics
		//this returns 299 drugs
		String query = "https://rxnav.nlm.nih.gov/REST/rxclass/classMembers?classId=D000970&relaSource=MESH";
		getRxClassDrugs(query, "MESHPA_QueryResult.xml", "MESHPA_Antineoplastics_5.10.16.txt");

	}
	@Test
	//NOT WORKING for VA drugs here
	public void getVADrugs() throws Exception{
		//VA classes - antineoplastics
	    String query= "https://rxnav.nlm.nih.gov/REST/rxclass/classMembers?classId=N0000029091&relaSource=NDFRT";
	    getRxClassDrugs(query, "VA_QueryResult.xml", "VA_Antineoplastics_5.10.16.txt");
	}

}

