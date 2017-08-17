package edu.ohsu.bcb.druggability;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import edu.ohsu.bcb.druggability.dataModel.Drug;

import org.w3c.dom.Node;
import org.w3c.dom.Element;


public class XMLHandler {
	
	/**
	 * Method tests drugBankXMLParser, which parses full DrugBankXML file and
	 * sends interactions to file
	 * @throws FileNotFoundException 
	 */
	@Test
	public void testDrugBankXMLParser() throws FileNotFoundException{
		//just call parser on full drugbank file
		//sends all interactions to file
		//will work from this
		
		//used previously
		//String interactionFile = "DrugBank_ParsedInteractions_06.23.16_2.txt";
		//drugBankXMLParser(interactionFile);
		
		//re-run with target organism - 9/26/16
		//re-run for Necitumumab problem - extra blank reference captured
		String interactionFile = "resources/DrugBank/DrugBank_ParsedInteractions_062917.txt";
		drugBankXMLParser(interactionFile);
		
	}
	
	/**
	 * Test method for testing drugbank xml parser with  a drug set.
	 * see commented out drug checking code in drugBankXMLParser()
	 * @throws IOException
	 */
	
	public void testDrugBankXMLParser_DrugSet() throws IOException{
		//Create test drug set
		Set<Drug> drugSet = new HashSet<Drug>();
		Drug drug1 = new Drug();
		drug1.setDrugName("Cetuximab");
		drugSet.add(drug1);
		Drug drug2 = new Drug();
		drug2.setDrugName("Imatinib");
		drugSet.add(drug2);
		Drug drug3 = new Drug();
		drug3.setDrugName("Ceritinib");
		drugSet.add(drug3);
		
	}
	/**
	 * Method parses DrugBank file for Drug, Target, UniProt, Organism, and References
	 * Updated 9/26/16 to include target organism.
	 * @param interactionFile
	 * @throws FileNotFoundException
	 */
	public void drugBankXMLParser(String interactionFile) throws FileNotFoundException{
		//output file
		PrintStream ps = new PrintStream(interactionFile);
		ps.println("Drug" + "\t" + "Target_Name" + "\t" + "UniProt_ID" + "\t"+ "Target_Org" + "\t" + "PubMedIDs" );
		
		
		
		try{

			File drugBankFile = new File("resources/DrugBank/drugBank_full_06.22.16.xml");
			//File drugBankFile = new File("resources/DrugBank/drugBank_test_06.22.16.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document docm = dBuilder.parse(drugBankFile);
			docm.getDocumentElement().normalize();
			
			int drugCounter = 0;
			
			//first level = drugbank tag
			NodeList firstLevel = docm.getChildNodes();
			for (int i = 0; i<firstLevel.getLength(); i++){
				Node currentNode = firstLevel.item(i);
				//System.out.println("First level: " + currentNode.getNodeName());//node name is tag name
				
				//second level = drug items
				NodeList secondLevel  = currentNode.getChildNodes();//get drugbank children
				for (int j = 0; j<secondLevel.getLength(); j++){
					
					//boolean drugMatch= false;//try here
					
					//initialize variables
					String drugName = "";
					Node current2Node = secondLevel.item(j);
					
					//only want element nodes
					if (current2Node.getNodeType()==Node.ELEMENT_NODE){
					
						//third level = drug info (id, name, etc)
						NodeList thirdLevel = current2Node.getChildNodes();
						for (int k = 0; k<thirdLevel.getLength(); k++){
							Node current3Node = thirdLevel.item(k);
							
							//only want element nodes
							if (current3Node.getNodeType()==Node.ELEMENT_NODE){
								
								///////////
								//drug name
								///////////
								if (current3Node.getNodeName().equals("name")){
									drugName = current3Node.getTextContent();//drug name
									
//									for (Drug drug:currentDrugSet){
//										if (drug.nameIsEquivalent(drugName)){
//											System.out.println("Drug Match found!");
//											drugMatch = true;//switch to true
//										}
//									}
									
									System.out.println("Drug Name: " + drugName);
									drugCounter++;
								}
								
								/////////
								//targets
								/////////
								if (current3Node.getNodeName().equals("targets")){
									Set<String> targetSet1 = new HashSet<String>();
									//get target info
									NodeList targetFirstLevel = current3Node.getChildNodes();
									
									for (int m= 0; m<targetFirstLevel.getLength(); m++){
										//each individual target
										String targetName = null;
										String targetOrg=null;
										String uniProtID =null;
										String blockRef = "";
										
										Node currentTargetNode = targetFirstLevel.item(m);
										//make sure to get element 
										if (currentTargetNode.getNodeType()==Node.ELEMENT_NODE){
											System.out.println("Target level: " + currentTargetNode.getNodeName());//targets
											
											NodeList targetSecondLevel = currentTargetNode.getChildNodes();
											
											for (int n = 0; n<targetSecondLevel.getLength(); n++){
												Node current2TargetNode = targetSecondLevel.item(n);
												//actual target info nodes
												if (current2TargetNode.getNodeType()==Node.ELEMENT_NODE){
													//System.out.println("Target level info:" + current2TargetNode.getNodeName());
													
													/////////////
													//target NAME
													/////////////
													if(current2TargetNode.getNodeName().equals("name")){
														targetName = current2TargetNode.getTextContent();
														System.out.println("Target name: " + targetName);
														targetSet1.add(targetName);
													}
													///////////////////*WORKING HERE
													//target species
													///////////////////
													if(current2TargetNode.getNodeName().equals("organism")){
														targetOrg = current2TargetNode.getTextContent();
														System.out.println("Target org: " + targetOrg);
														
													}
													
													///////////////////
													//target REFERENCES
													///////////////////
													if(current2TargetNode.getNodeName().equals("references")){
														Set<String> pubMedIDs = drugBankGetRefs(current2TargetNode);
														//end ref set loop
														System.out.println("Num refs: " + pubMedIDs.size());
														int counter = 1;//added 6/29/17
														for (String each:pubMedIDs){
															if (counter< pubMedIDs.size()){
																blockRef = blockRef + each + "|";
															}
															else{
																blockRef = blockRef + each;//end without pipe
															}
															counter++;
														}
															
													}
													/////////////////
													//target UNIPROT ID
													/////////////////
													if (current2TargetNode.getNodeName().equals("polypeptide")){
														//get attribute
														Element current2TargetElement = (Element)current2TargetNode;
														uniProtID = current2TargetElement.getAttribute("id");
														
														System.out.println("UniProt ID: " + uniProtID);
													}
													
															
												}
												
												
											}
											
											//System.out.println("Match?" + drugMatch);
											//try here
											//if (drugMatch == true){
												ps.println(drugName + "\t" + targetName + "\t" + uniProtID + "\t" +targetOrg + "\t"+ blockRef );
											//}
										}
										
									}
									//end target set loop
									System.out.println("Num targets: " + targetSet1.size());
								}
								
								
								
								
							}
						}
						
					}
				}
			}
			//end drug set loop
			System.out.println("Total num drugs: " + drugCounter);
			
			
			
		}
		
		catch (Exception e){
			e.printStackTrace();;
		}
		ps.close();
	}
	
	
	
	private Set<String> drugBankGetRefs(Node current2TargetNode) {
		Set<String> pubMedIDs = new HashSet<String>();
		
		//ind'l reference nodes?
		NodeList articleNodes = current2TargetNode.getChildNodes();
		for (int r = 0; r<articleNodes.getLength(); r++){
			Node articleNode = articleNodes.item(r);
			NodeList article2Nodes = articleNode.getChildNodes();//each article
			//for each article element
			for (int s = 0; s<article2Nodes.getLength(); s++){
				Node article2node = article2Nodes.item(s);
				//check for element node type
				if (article2node.getNodeType()==Node.ELEMENT_NODE){
					//System.out.println("Check node level: " + article2node.getNodeName());
					
					NodeList article3Nodes = article2node.getChildNodes();
					for (int t = 0; t<article3Nodes.getLength(); t++){
						Node articleInfo = article3Nodes.item(t);
						if (articleInfo.getNodeType()==Node.ELEMENT_NODE){
							////////////////////////////
							//target reference PUBMED ID
							////////////////////////////
							if (articleInfo.getNodeName().equals("pubmed-id")){
								String pubmedID = articleInfo.getTextContent();
								System.out.println("PubMedID:"  + pubmedID);
								pubMedIDs.add(pubmedID);//add to set
							}
						}
					}
					
					
				}
			}
		
		}
		return pubMedIDs;
	}

		
}
