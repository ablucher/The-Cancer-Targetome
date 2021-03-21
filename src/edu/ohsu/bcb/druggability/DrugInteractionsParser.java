package edu.ohsu.bcb.druggability;


import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.jsoup.select.Evaluator.IsEmpty;
import org.reactome.r3.util.FileUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ohsu.bcb.druggability.dataModel.DatabaseRef;
import edu.ohsu.bcb.druggability.dataModel.Drug;
import edu.ohsu.bcb.druggability.dataModel.ExpEvidence;
import edu.ohsu.bcb.druggability.dataModel.Interaction;
import edu.ohsu.bcb.druggability.dataModel.LitEvidence;
import edu.ohsu.bcb.druggability.dataModel.Source;
import edu.ohsu.bcb.druggability.dataModel.Target;


public class DrugInteractionsParser {

	public DrugInteractionsParser(){
		
	}
	
	/**
	 * This method loads a drug set from a file. Takes num header lines, colum number for drug names, seperator type. 
	 * Returns set of drugs. 
	 * @param fileName
	 * @param headerLines
	 * @param drugCol
	 * @param sep
	 * @return
	 * @throws IOException
	 */
	
	public Set<String> loadDrugSetFromFile(String fileName, int headerLines, int drugCol, String sep) throws IOException{
		FileUtility fileUt = new FileUtility();
		fileUt.setInput(fileName);
		//skip over headers
		for (int i = 0; i < headerLines; i++){
			//System.out.println(fileUt.readLine());
			fileUt.readLine();
		}

		//create set for drugs
		Set<String> drugSet = new HashSet<String>();//just added comparator
		//list for repeats
		List<String> repeatDrugs = new ArrayList<String>();
		
		//output files for combo, duplicate by formulation drugs
		PrintStream cs = new PrintStream("results_beta_V2/NCIDrugs_CombosExcluded_12.02.16.txt");
		cs.println("Drug");
		
		PrintStream ds = new PrintStream("results_beta_V2/NCIDrugs_Excluded_12.02.16.txt");
		ds.println("Drug");
		
		
		String line = null;
		while ((line = fileUt.readLine()) != null){
			//System.out.println(line);
			String[] tokens = line.split(sep);
			String drugName = tokens[drugCol];
			String brandName = tokens[1];
			String fdaApp = tokens[2];
			
			//System.out.println("Drug: " +drugName);
			if (drugSet.contains(drugName)){
				repeatDrugs.add(drugName);
			}
			else{
				//check for combo drugs
				//System.out.println("BrandName: " + brandName);
				//System.out.println("FDA App: " + brandName);
				if (brandName.equals("NA") && fdaApp.equals("NA")){
					//System.out.println("COMBO found: " + drugName);
					cs.println(drugName);
					continue;//skip drug
				}
				else if (drugName.contains(" and ")){
					//print these to combo file
					cs.println(drugName);
					continue;//skip drug
				}
				//exclude talc
				//exclude if keyword: liposome/implant/topical/formulation/vaccine
				else if (drugName.contains("Liposome") || drugName.contains("Implant") || drugName.contains("Topical") || drugName.contains("Formulation") || drugName.contains("Vaccine") || drugName.equals("Talc")){
					//print these to exclusions file
					ds.println(drugName);
					continue;//skip drug
				}
				else if (drugName.contains("Injection")){
					//then strip off injection and just keep first part 
					drugName=drugName.substring(0, drugName.length()-10);
					drugSet.add(drugName);
				}
				
				else{
					drugSet.add(drugName);
				}
				
				
				//System.out.println(drugName);
			}
			
		}
		//System.out.println("Num repeat drugs: " + repeatDrugs.size());
		cs.close();
		ds.close();
		
		return drugSet;		
	}
	
	/**
	 * Test method for loading drug set from file
	 * 
	 * CHECKED 03/07/21
	 * @throws Exception 
	 */
	@Test
	public void test_beta_LoadDrugSetFromFile() throws Exception{
		//String drugFile = "drug_sets/testDrugSet.txt";//for testing
		String drugFile = "resources/drug_sets/scrapedNCIDrugs_05.11.16.txt";//for testing, 236 unique listings
		//load drug set from file
		//1 header, col[0] for drugs
		Set<String> drugSet = loadDrugSetFromFile(drugFile, 1, 0, "\t");
		System.out.println("Num Drugs: " + drugSet.size());
		for (String drug: drugSet){
			System.out.println(drug);
		}
		
	}
	
	/**
	 * This method takes a drug set and retrieves all of the NCI synonyms for those drugs
	 * from the NCIThesauraus file.
	 * @throws IOException 
	 */
	public Map<String, Set<String>> loadNCISynonyms(Set<String> drugSet, String nciThesFile) throws IOException{
		//create map for drug -->synonym
		Map<String, Set<String>> drugToNCISynonym = new HashMap<String, Set<String>>();
		
		//open nci thesauraus
		FileUtility fileUt = new FileUtility();
		fileUt.setInput(nciThesFile);		
		String line;
		Set<String> foundDrugs = new HashSet<String>();//keep track of found drugs
		while ((line = fileUt.readLine()) != null){
			Set<String> synonymSet = new HashSet<String>();
			String[] tokens = line.split("\t");//split on tab
			String drugName = tokens[1];//second column is drug name
			String[] synonyms = tokens[3].split("\\|");//fourth column is synonyms
            										   //split on pipe
			
			
			for (String synonym:synonyms){
				//System.out.println("synonym: " + synonym);
				synonymSet.add(synonym);
			}
			synonymSet.add(drugName); //add drugname into synonym set
			
			//check if drug is in synonym column
			for (String synonym:synonyms){//use full synonymSetso that we include drugname here
				for(String drug: drugSet){//NCI Drug Set
					//added check for drugSection on 10/5/16**
					//we also want to check first part of drug name in case compound name
					//example: Afatinib Dimaleate			
					String[] drugSections = drug.split("\\s+");
					String drugSection = drugSections[0];//just need first
					//fine because if no space than whole drug name will be checked later in loop
					//System.out.println("Drugname:" + drugName + "section: " + drugSection);
					//try this **|| drugSection.equalsIgnoreCase(synonym) 
					
					//previously just checked drug
					//we need to check drugSection  - just the first part of drug name
					//
					if (drug.equalsIgnoreCase(synonym) || drugSection.equalsIgnoreCase(synonym)){
						if (drugToNCISynonym.containsKey(drug)){
							Set<String> runningSet = drugToNCISynonym.get(drug);
							//update running set here
							runningSet.addAll(synonymSet);
						}
						else if (drugToNCISynonym.containsKey(drugSection)){
							Set<String> runningSet = drugToNCISynonym.get(drugSection);
							//update running set here
							runningSet.addAll(synonymSet);
						}
						
						else{
						//then put drug and synonym set into our map
						drugToNCISynonym.put(drug, synonymSet);
						foundDrugs.add(drug);//mark as found
						}
					}
				}

			}

			
		}
		
		//if no synonyms were found, we still need those drugs
		for (String drug: drugSet){
			//if the drug is not marked in our found set
			//then we need to add to synonym map, but with null entry
			if (!foundDrugs.contains(drug)){
				drugToNCISynonym.put(drug,  null);//does this work?
			}
		}
	
		//return synonym map
		return drugToNCISynonym;
		
	}
	
	/**
	 * This method reads in a set of drugs and their synonyms from a file
	 * String thesaurusFile - our input file to read
	 * indexDrug - column to use for drug name
	 * indexSynonym - column to use for drug synonym
	 * 
	 * 03/07/21 - to run on Sophia's updated drug thesaurus sets
	 * @throws IOException 
	 */
	public Map<String, Set<String>> loadDrugsAndSynonymDeckFromFile(String thesaurusFile, boolean header, int indexDrug, int indexSynonym) throws IOException{
		//create map for drug -->synonym
		Map<String, Set<String>> drugToSynonym = new HashMap<String, Set<String>>();
		
		//open file
		FileUtility fileUt = new FileUtility();
		fileUt.setInput(thesaurusFile);		
		String line;
		Set<String> foundDrugs = new HashSet<String>();//keep track of found drugs
		//System.out.println("Reading in thesaurus file: MMTERT");
		if (header) {
			fileUt.readLine();//skip header
		}
		while ((line = fileUt.readLine()) != null){
			String[] tokens = line.split("\t");//split on tab
			String drugName = tokens[indexDrug];//first column is drug name
			String synonym = tokens[indexSynonym];//second column is synonym
			//System.out.println("Drug: " + drugName);
			//System.out.println("Synonym: "+ synonym);
			
			//do we already have this drug?
			if (drugToSynonym.keySet().contains(drugName)) {
				//System.out.println("Drug already in map, just update");
				Set<String> existingSynonymDeck = drugToSynonym.get(drugName);
				existingSynonymDeck.add(synonym);
				drugToSynonym.put(drugName, existingSynonymDeck);
			}
			else {
				//System.out.println("Create new map entry: ");
				Set<String> newSynonymSet= new HashSet<String>();
				newSynonymSet.add(synonym); 
				drugToSynonym.put(drugName, newSynonymSet);
				foundDrugs.add(drugName);//mark as found
			}	
		}//while
		
		//System.out.println("Synonym deck size: " + drugToSynonym.keySet().size());
		
		//return synonym map
		return drugToSynonym;
		
	}
	/**
	 * Overloaded method to load drugs and synonym deck from a file
	 * Input is an existing map, builds off of that one
	 * @param existingMap
	 * @param thesaurusFile
	 * @return
	 * @throws IOException
	 */
	public Map<String, Set<String>> loadDrugsAndSynonymDeckFromFile(Map<String, Set<String>> existingMap, String thesaurusFile, boolean header, int indexDrug, int indexSynonym) throws IOException{
		//use existing map here
		
		//open file
		FileUtility fileUt = new FileUtility();
		fileUt.setInput(thesaurusFile);		
		String line;
		Set<String> foundDrugs = new HashSet<String>();//keep track of found drugs
		if(header) {
			fileUt.readLine(); //skip header
		}
		while ((line = fileUt.readLine()) != null){
			String[] tokens = line.split("\t");//split on tab	
			if(tokens.length<2) {//need to add?
				continue;
			}
			String drugName = tokens[indexDrug];//first column is drug name
			
			String synonym = tokens[indexSynonym];//second column is synonym
			//System.out.println("Drug: " + drugName);
			//System.out.println("Synonym: "+ synonym);
			
			//do we already have this drug?
			if (existingMap.keySet().contains(drugName)) {
				//System.out.println("Drug already in map, just update");
				Set<String> existingSynonymDeck = existingMap.get(drugName);
				existingSynonymDeck.add(synonym);
				existingMap.put(drugName, existingSynonymDeck);
			}
			else {
				//System.out.println("Create new map entry: ");
				Set<String> newSynonymSet= new HashSet<String>();
				newSynonymSet.add(synonym); 
				existingMap.put(drugName, newSynonymSet);
				foundDrugs.add(drugName);//mark as found
			}	
		}//while
		//System.out.println("Synonym deck size: " + existingMap.keySet().size());
		
		//return synonym map
		return existingMap;
		
	}
	
	/**
	 * Test method to run a drug set (created from other methods) and retrieve all target interactions.
	 * @throws IOException
	 */
	@Test
	public void testLoadNCISynonyms() throws IOException{
		//output file for drug->synonym map
		//PrintStream ps = new PrintStream("NCIDrugSyn_06.03.16.txt");
		//PrintStream ps = new PrintStream("ATCDrugSyn_06.03.16.txt");
		PrintStream ps = new PrintStream("MESHPADrugSyn_06.03.16.txt");
		
		//define drug file here - ncidrugs, atc, mesh for now
		//String drugFile = "drug_sets/testDrugSet.txt";//test NCI drug set
		//String drugFile = "drug_sets/scrapedNCIDrugs_05.11.16.txt"; //full NCI
		//String drugFile = "drug_sets/ATC_Antineoplastics_5.10.16.txt";//ATC Drugs
		String drugFile = "drug_sets/MESHPA_Antineoplastics_5.10.16.txt";//MESHPA drugs
		
		//load drug set from file
		//1 header, col[0] for drugs
		//Set<String> drugSet = loadDrugSetFromFile(drugFile, 1, 0, "\t");//NCI
		Set<String> drugSet = loadDrugSetFromFile(drugFile, 0, 0, "\t");//ATC & MESHPA
		System.out.println("Num Drugs: " + drugSet.size());
		
		//get synonyms from nci thesauraus, load into map
		Map<String, Set<String>> drugSynonyms = loadNCISynonyms(drugSet, "resources/NCIThesaurus_v04.25.16.txt");
		System.out.println("Printing drug set and synonyms: ");
		System.out.println("Num Drugs with Syn: "+ drugSynonyms.size());
		int counterNA = 0;
		for (String key:drugSynonyms.keySet()){
			System.out.println("Drug: " + key);
			ps.print(key + "\t");
			Set<String> synonyms = drugSynonyms.get(key);
			if (synonyms==null){
				System.out.println("No NCI synonyms found");
				ps.println("NA");
				counterNA++;
			}
			else{
				System.out.println("Num synonyms: " + synonyms.size());
				int counter = 1;
				for(String syn:synonyms){
					if (counter == synonyms.size()-1){
						ps.print(syn);//no pipe at end
					}
					else{
						System.out.println("Syn: "+ syn); 
						ps.print(syn + "|");
						
					}
					
					counter++;
				}
				ps.println();
			}

		}
		System.out.println("Num NAs: " + counterNA);
		ps.close();
	}
	

	
	/**
	 * Method iterates through IUPHAR interactions file and pulls all interactions involving a drug
	 * in our drug set. For each interaction, an Interaction object is created between an existing Drug
	 * object and a Target object. If no Target object pre-exists, then a new one is created. 
	 * Evidence for Interactions ** in progress
	 * Method returns set of Interactions. 
	 * @throws IOException 
	 * 
	 */
//	private Set<Interaction> loadIUPHARInteractions(Set<Drug> drugSet, Set<Target> targetSet, String iupharFile) throws IOException{
//		//set for IUPHAR interaction objects
//		Set<Interaction> iupharInteractions = new HashSet<Interaction>();
//		
//		//open iuphar interactions file
//		FileUtility fileUt = new FileUtility();
//		fileUt.setInput(iupharFile);
//		String[] headers = fileUt.readLine().split(",");//headers
//		
//		int lineCounter = 2;
//		String line = null;
//		while ((line = fileUt.readLine()) != null){
//			String[] tokens = fileUt.readLine().split(",");
//			//get relevant columns here
//			String target = tokens[0];
//			String ligand = tokens[10];
//			
//			//WORKING HERE***need to compare DRUG OBJECT with DRUGNAME
//			//if ligand is in our drug set
//			//then retrieve and store pertinent information in Interaction object
//			if (drugSet.contains(ligand)){//*also need to check synonyms
//				Interaction interaction = new Interaction();
//				interaction.setIntDrug(ligand);//should be from drug set?
//				interaction.setIntTarget(target);
//				
//			}
//
//			//check ligand against drug set /drug synonyms
//			//if match, pick up the following:
//			//ligand, target, species, target info, ref
//			//if no target object in target set
//			//create target object from target info
//			//interaction stores the relationship between drug and target
//			//as well as evidence, source, etc. information
//			
//			lineCounter++;
//		}
//		
//
//		fileUt.close();
//		
//		
//		
//		return iupharInteractions;
//	}
	
//	@Test
//	public void testLoadIUPHARInteractions(){
//		//create drug set fro small drug test file
//		Set<String> drugSet = loadDrugSetFromFile("testDrugSet.txt", 1, 0, "\t")
//		Set<Drug> drugObjects = loadDrugObjects(drugSet);//creates Drug Objects with synonyms
//		//define empty Target set
//		Set<Target> targetSet = new HashSet<Target>();
//		
//		//load these
//		//using small test file with 4 drugs
//		
//		Set<Interaction> iupharInts = loadIUPHARInteractions(drugObjects, targetSet);
//		
//	}
//	
	
	
	/**
	 * This method queries the druggability database and returns the full drug set.
	 * Note that this returns a set of Drug objects.
	 * Pass a session factory to method**?
	 */
	public Set<Drug> queryDrugSet(Session currentSession){
		//create query
		Query query = currentSession.createQuery("from Drug");
		
		//create drug set
		Set<Drug> drugSet = new HashSet<Drug>();
		
		List<Drug> allDrugs = query.list();
		for (Drug drug: allDrugs){
			//System.out.println("Drug: " + drug.getDrugName());
			drugSet.add(drug);//add to set
		}
		//System.out.println("Num total drugs: " + drugSet.size());
		return drugSet;
	}
	
	/**
	 * This method queries the druggability database and returns full target set
	 */
	public Set<Target> queryTargetSet(Session currentSession){
		//create query
		Query query = currentSession.createQuery("from Target");
		
		//create drug set
		Set<Target> targetSet = new HashSet<Target>();
		
		List<Target> allTargets= query.list();
		for (Target target: allTargets){
			targetSet.add(target);
		}
		return targetSet;
	}
	
	
	/**
	 * This method queries the druggability database and returns interactions
	 */
	public Set<Interaction> queryInteractionSet(Session currentSession){
		//create query
		Query query = currentSession.createQuery("from Interaction");
		
		//create drug set
		Set<Interaction> interactionSet = new HashSet<Interaction>();
		
		List<Interaction> ints= query.list();
		for (Interaction interaction: ints){
			interactionSet.add(interaction);
		}
		return interactionSet;
	}
	
	
	/**
	 * This method queries the druggability database and returns litEvidence
	 */
	public Set<LitEvidence> queryLitEvidenceSet(Session currentSession){
		//create query
		Query query = currentSession.createQuery("from LitEvidence");
		
		//create drug set
		Set<LitEvidence> literatureSet = new HashSet<LitEvidence>();
		
		List<LitEvidence> allLit= query.list();
		for (LitEvidence lit: allLit){
			literatureSet.add(lit);
		}
		return literatureSet;
	}
	
	
	
	/**
	 * This method queries the druggability database and returns experimental evidence
	 */
	public Set<ExpEvidence> queryExpEvidenceSet(Session currentSession){
		//create query
		Query query = currentSession.createQuery("from ExpEvidence");
		
		//create drug set
		Set<ExpEvidence> experimentSet = new HashSet<ExpEvidence>();
		
		List<ExpEvidence> allExp= query.list();
		for (ExpEvidence exp: allExp){
			experimentSet.add(exp);
		}
		return experimentSet;
	}
	
	/**
	 * This method queries the druggability database and returns database ref set
	 */
	public Set<DatabaseRef> queryDatabaseSet(Session currentSession){
		//create query
		Query query = currentSession.createQuery("from DatabaseRef");
		
		//create drug set
		Set<DatabaseRef> databaseSet = new HashSet<DatabaseRef>();
		
		List<DatabaseRef> allDatabases= query.list();
		for (DatabaseRef database: allDatabases){
			databaseSet.add(database);
		}
		return databaseSet;
	}
	
	/**
	 * This method queries the druggability database and returns source set
	 */
	public Set<Source> querySourceSet(Session currentSession){
		//create query
		Query query = currentSession.createQuery("from Source");
		
		//create drug set
		Set<Source> sourceSet = new HashSet<Source>();
		
		List<Source> allSources= query.list();
		for (Source source: allSources){
			sourceSet.add(source);
		}
		return sourceSet;
	}
	
	/**
	 * This method takes a list of drugs, loads synonyms, and persists
	 * them to hibernate database. This method opens the hibernate session.
	 * 
	 * Updated 03/7/21, beta work, remove persist functionality for time being
	 * -returns a drug set, not a session
	 * @throws IOException
	 * @throws ParseException 
	 */
	public Set<Drug> beta_persistDrugSet(String drugFile, int headers, int drugCol, String sep) throws IOException, ParseException{
		//load drug set; CHECKED
		Set<String> testDrugs = loadDrugSetFromFile(drugFile, headers, drugCol, sep);
		//load NCI synonyms
		Map<String, Set<String>> drugSyns = loadNCISynonyms(testDrugs, "resources/NCIThesaurus_v04.25.16.txt");
		System.out.println("NCI drug syn map size:" + drugSyns.size());
		
//		//hibernate config file - 
//		//open hibernate session and transaction
//		String configFileName = "resources/drugHibernateV2.cfg.xml";//updated 9/7/16
//		Session currentSession = DruggabilityHibernatePersist.openSession(configFileName);
//		currentSession.beginTransaction();

		//create set for all drugs
		Set<Drug> fullDrugList = new HashSet<Drug>();
		
		//CREATE Drug Objects
		//iterate through synonym map, get drugs
		int pastCounter=0;
		int foundCounter=0;
		int nullCounter=0;
		for (String drugName: drugSyns.keySet()){
			//create new drug object
			Drug currentDrug = new Drug();
			currentDrug.setDrugName(drugName);
			Set<String> currentDrugSyns = drugSyns.get(drugName);
			if (currentDrugSyns==null){
				currentDrug.setDrugSynonyms(null);
//				//check for approval date
//				String approvalDate = getFDAApprovalDate(currentDrug);
//				currentDrug.setApprovalDate(approvalDate);
//				//ps.println(drugName + "\t" + approvalDate);
//				//counters
//				if (approvalDate!=null){
//						foundCounter++;
//				}
//				else{
//					nullCounter++;
//				}
//				
//				//currentSession.save(currentDrug);
				fullDrugList.add(currentDrug);//add to set here
				continue;
			}
			//set synonyms from map
			currentDrug.setDrugSynonyms(currentDrugSyns);
//			//retrieve fda approval date
//			String approvalDate = getFDAApprovalDate(currentDrug);
//			currentDrug.setApprovalDate(approvalDate);
//			//currentSession.save(currentDrug);
//			if (approvalDate!=null){//increment our counters
//					foundCounter++;
//			}
//			else{
//				nullCounter++;
//			}
			//add to fullDrugset
			fullDrugList.add(currentDrug);
		}
		System.out.println("Number all drugs: " + fullDrugList.size());
		//Now we need to check for any duplicates because of name/synonym
		Set<Drug> checkedDrugs = new HashSet<Drug>();
		for (Drug drug: fullDrugList){
			if (drug.containedInSet(checkedDrugs)){//if already in set, skip
				System.out.println("Skipped drug: " + drug.getDrugName());
				continue;
			}
			else{
				checkedDrugs.add(drug);//add to set
			}
		}
		System.out.println("Number unique drugs: " + checkedDrugs.size());
		//commit these drugs to database
//		for (Drug drug: checkedDrugs){
//			currentSession.save(drug);
//		}
//		
//		System.out.println("Commit DRUG SET to database complete.");
//		System.out.println("Num Approval Dates found: " + foundCounter);
//		System.out.println("Num Approval Dates null: " + nullCounter);
//		
//		return currentSession;
		return checkedDrugs;

	}
	
	
	

	@Test
	public void testDrugsPersistAndQuery() throws IOException, ParseException{
		//drugsPersist("drug_sets/testDrugSet.txt", 1, 0, "\t");
		Session currentSession = beta_persistDrugSet("drug_sets/scrapedNCIDrugs_05.11.16.txt", 1, 0, "\t" );
		
		//now query drugs
		Set<Drug> drugSet = queryDrugSet(currentSession);
		System.out.println("Num total drugs: " + drugSet.size());
		for (Drug drug:drugSet){
			System.out.println(drug.getDrugName());
			if (drug.getDrugSynonyms()!=null){
				System.out.println("Num syns: " + drug.getDrugSynonyms().size());
			}
			else{
				System.out.println("Num syns: 0");
			}
		}
		
		//close session
		currentSession.getTransaction().commit();
		currentSession.close();
		SessionFactory thisFactory = currentSession.getSessionFactory();
		thisFactory.close();
		
	}
	
	
	

	/**Helper method for parsing DrugBank XML file
	 * Given the target node, returns set of PubMed IDs for the target.
	 * @param current2TargetNode
	 * @return
	 */
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
	
	/**
	 * Method checks target name and uniprot, returns target Type.
	 * If UniProt available -> Protein
	 * If no UniProt but name contains DNA ->DNA
	 * If no UniProt but name contains RNA ->RNA
	 * This should be polished in the future, currently we have limited
	 * information for DNA/RNA target type nodes.
	 * @param targetName
	 * @param targetUniProt
	 * @return
	 */
	public String assignTargetType(String targetName, String targetUniProt){
	
		String targetType="";
		if (targetUniProt== null || targetUniProt.equals("null") || targetUniProt.length()==0){
			//DNA target type
			if (targetName.contains("DNA")){
				targetType = "DNA";
			}
			//RNA target type
			else if (targetName.contains("RNA")){
				targetType = "RNA";
			}
			else{
				targetType = "NA";//cannot assign
			}
		}
		//else default target type = Protein.
		else{
			targetType="Protein";
		}
		
		return targetType;
	}

	/**
	 * Method persists DrugBank interactions to druggability database.
	 * @param currentSession
	 * @return
	 * @throws IOException
	 */
	public Session persistDrugBank(Session currentSession) throws IOException{
		
		//query session for current sets in our database
		Set<Drug> drugSet = queryDrugSet(currentSession);
		System.out.println("Drug set size check: " + drugSet.size());
		Set<Target> targetSet = queryTargetSet(currentSession);
		Set<Interaction> interactionSet = queryInteractionSet(currentSession);
		Set<LitEvidence> literatureSet = queryLitEvidenceSet(currentSession);
		Set<ExpEvidence> experimentalSet = queryExpEvidenceSet(currentSession);
		Set<Source> sourceSet= querySourceSet(currentSession);
		Set<DatabaseRef> databaseSet = queryDatabaseSet(currentSession);

		//use previously parsed file from drugBankXMLParser
		FileUtility drugBankFile = new FileUtility();
		//drugBankFile.setInput("resources/DrugBank/DrugBank_ParsedInteractions_06.23.16_2.txt");
		//updated 9/26/16
		//drugBankFile.setInput("resources/DrugBank/DrugBank_ParsedInteractions_09.26.16.txt");
		drugBankFile.setInput("resources/DrugBank/DrugBank_ParsedInteractions_062917.txt");
		
		//for debugging only, drugbank file
		//PrintStream ps = new PrintStream("resources/DrugBank/DrugBank_Reference_Debugging_062917.txt");
		
		//create DatabaseRef for DrugBank
		DatabaseRef drugBank = new DatabaseRef();
		drugBank.setDatabaseName("DrugBank");
		drugBank.setDownloadDate("06.21.2016");
		drugBank.setDownloadURL("NA");
		drugBank.setVersion("5.0.0");
		databaseSet.add(drugBank);//prob not necessary
		currentSession.save(drugBank);
		
		//initialized empty ref counter
		int emptyRefCounter=0;
		
		//parse file
		String[] headers = drugBankFile.readLine().split("\t");
		int lineCounter = 2;
		String line = null;
		while ((line = drugBankFile.readLine()) != null){
			String[] tokens = line.split("\t");
			
			String drugName = tokens[0];
			
			//include escape for IMC-11F8 listing, no reference attached and drugbank now 
			//has this resolved with necitumumab
			if (drugName.equals("IMC-11F8")){
				continue;
			}
			String targetName= tokens[1];
			String targetUniProt =tokens[2];
			//assign target type based on name/uniprot entries
			String targetType = assignTargetType(targetName, targetUniProt);
			//check target type, if NA then just continue
			if (targetType.equals("NA")){
				continue;//then skip this entry in drugbank file
			}
					
			String targetOrg=tokens[3];
			targetOrg= getSpeciesName(targetOrg);

			String interactionType = null; //FIX this in future updates, add interaction info
			
			//iterate through drug set
			for (Drug drug: drugSet){
				
				//check for match to drug set
				if (drug.nameIsEquivalent(drugName)){//then persist
					
					
					System.out.println("Drug match found");
					System.out.println("Drug: " + drug.getDrugName());
					
					
					//clean up references here - only if match found
					//fixed 12/2/16
					String refBlock="";
					System.out.println("Num tokens: " + tokens.length);
					if (tokens.length < 5){//no reference
						
						refBlock="NA_DrugBank";
						//ps.println("Found as: " + drugName + " matched to: "+ drug.getDrugName() + "\t" + refBlock + "\t" + "because less than 5 tokens");
						//ps.println("Printing original tokens" + tokens);
						emptyRefCounter++;
					}
					else{
						refBlock = tokens[4];
						//ps.println("Found as: " + drugName + " matched to: "+ drug.getDrugName() + "\t" + refBlock );
						//ps.println("Printing original tokens" + tokens);
						
					}
					//create target
					Target target = createTarget(currentSession, targetSet, targetName, targetUniProt, targetType, targetOrg);
					System.out.println("Target: " + target.getTargetName());
					currentSession.save(target);
					
					//create source set (sources are saved to session within method)
					Set<Source> interactionSourceSet = createSourceSet(currentSession, sourceSet, literatureSet, drugBank, refBlock);
					//added debugging
					//ps.println("Checking sources:  ");
					//for (Source sourceFound:interactionSourceSet){
					//	ps.println(sourceFound.getSourceDatabase() + " " + sourceFound.getSourceLiterature().getPubMedID());
					//}
					
					//create interaction **CENTERPIECE OF DATA MODEL
					Interaction currentInteraction = createInteraction(currentSession, drug, target, interactionType, interactionSet, interactionSourceSet);
					currentSession.save(currentInteraction);

				}//end drug match if statement
				else{
					continue;
				}
			}//end drug loop	
			
		}//end file loop
		//for checking the empty refs from drugbank, added 12/2/16
		System.out.println("Num interactions no ref: " + emptyRefCounter);
		
		return currentSession;
	}//end method

	/**
	 * Method persists TTD interactions to druggability database. Iterates through
	 * set of TTDObjects returned from TTDParse() and checks each target's list of drugs
	 * against our ongoing drug set to find match. Retrieves all interactions involving drugs 
	 * in our overall set.
	 * @param currentSession
	 * @return
	 * @throws IOException
	 */
	public Session persistTTD(Session currentSession) throws IOException{
		
		//query session for current sets in our database
		Set<Drug> drugSet = queryDrugSet(currentSession);
		Set<Target> targetSet = queryTargetSet(currentSession);
		Set<Interaction> interactionSet = queryInteractionSet(currentSession);
		Set<LitEvidence> literatureSet = queryLitEvidenceSet(currentSession);
		Set<ExpEvidence> experimentalSet = queryExpEvidenceSet(currentSession);
		Set<Source> sourceSet= querySourceSet(currentSession);
		Set<DatabaseRef> databaseSet = queryDatabaseSet(currentSession);

		//Set TTD file
		//String ttdFileName="resources/TTD/TTD_download.txt";//actually the new version - may be incomplete though, so not using
		String ttdFileName="resources/TTD/TTD_download_06.20.16.txt";//older version
																	 //we get more targets here
		
		//create DatabaseRef for TTD
		DatabaseRef ttd = new DatabaseRef();
		ttd.setDatabaseName("Therapeutic Target Database");
		ttd.setDownloadDate("06.20.2016");
		ttd.setDownloadURL("http://bidd.nus.edu.sg/group/ttd/TTD_Download.asp");
		ttd.setVersion("Version 4.3.02 (2013.10.18)");
		databaseSet.add(ttd);
		currentSession.save(ttd);
		
		//call parseTTD method here
		Map<String, TTDObject> setTTD = parseTTD(ttdFileName);
		
		//for each TTD entry
		for (String entry: setTTD.keySet()){
			//get TTD object	
			TTDObject object = setTTD.get(entry);
			String entryTarget = object.getTargetName();
			String entryUniprot = object.getTargetUniprot();
			String targetType=assignTargetType(entryTarget, entryUniprot);
			//WORKING HERE
			//we want to skip over if we do not have uniprot for TTD entry
//			if (entryUniprot==null || entryUniprot.equals("null")){
//				continue;
//			}
			//skip this entry if no assigned target Type
			if (targetType.equals("NA")){
				continue;
			}
					
			Set<String> entryDrugSet = object.getTargetDrugs();
			
			//iterate through drug set
			//make sure there are drugs listed for targets
			if (entryDrugSet!=null){
			
				for (String entryDrug: entryDrugSet){
				//compare to our running drug set in database
				for (Drug drug:drugSet){
					//if drug match found
					if (drug.nameIsEquivalent(entryDrug)){
						//match found
						//now grab information and persist
						Target target = createTarget(currentSession, targetSet, entryTarget, entryUniprot, targetType,  null);
						currentSession.save(target);
						
						//create int source set - a placeholder **FOR NOW**
						Set<Source> interactionSourceSet = new HashSet<Source>();
						
						//create placeholder lit object - since we do not have references from TTD
						//check for existing lit here
						String refPlaceholder = "NA_TTD";
						//check for existing lit
						if (checkLiteratureSet(literatureSet,refPlaceholder)){
							//then get and add
							LitEvidence lit = getLiteratureFromSet(literatureSet, refPlaceholder);
							currentSession.save(lit);//prob not necc
							
							Source currentSource = createSource(currentSession, sourceSet, ttd, lit);
							currentSession.save(currentSource);
							sourceSet.add(currentSource);//add to full set
							interactionSourceSet.add(currentSource);//add to int set NEED?
						}
						else{//otherwise create new lit object
							LitEvidence newLit = new LitEvidence();
							newLit.setPubMedID(refPlaceholder);
							literatureSet.add(newLit);//add to full set
							currentSession.save(newLit);
							
							//create source with newLit obkect
						    Source currentSource = createSource(currentSession, sourceSet, ttd, newLit);
						    currentSession.save(currentSource);
						    sourceSet.add(currentSource);//add to full set
						    interactionSourceSet.add(currentSource);//add to int set
						}
						
						
						//use Drug object drug, since match was found
						//use Target object target created above
						//null InteractionType **FOR NOW**
						//use interactionSourceSet, has placeholder Source/Lit **FOR NOW**
							//remember that this will check for current source set and add to it
						Interaction currentInteraction = createInteraction(currentSession, drug,target, null, interactionSet, interactionSourceSet);
						currentSession.save(currentInteraction);
						
						
					}
					//if no match found, keep going
					else{
						continue;
					}
				}
			}
			}
			
		}
		

//			//iterate through drug set
//			for (Drug drug: drugSet){
//				
//				//check for match to drug set
//				if (drug.nameIsEquivalent(drugName)){//then persist
//					//create target
//					Target target = createTarget(currentSession, targetSet, targetName, targetUniProt);
//					currentSession.save(target);
//					
//					//create source set (sources are saved to session within method)
//					Set<Source> interactionSourceSet = createSourceSet(currentSession, sourceSet, literatureSet, ttd, refBlock);
//					
//					//create interaction **CENTERPIECE OF DATA MODEL
//					Interaction currentInteraction = createInteraction(currentSession, drug, target, interactionType, interactionSet, interactionSourceSet);
//					currentSession.save(currentInteraction);
//
//				}//end drug match if statement
//				else{
//					continue;
//				}
//			}//end drug loop	
//		}//end file loop
		return currentSession;
	}//end method
	
	
	@Test
	public void testPersistBindingDB() throws IOException, ParseException{

		System.out.println("Starting test method: ");
		//Session currentSession = drugsPersist("drug_sets/testDrugSet.txt", 1, 0, "\t");
	    Session currentSession = beta_persistDrugSet("drug_sets/scrapedNCIDrugs_05.11.16.txt", 1, 0, "\t" );
		System.out.println("Drugs loaded to database.");
	
		System.out.println("Starting on persist BindingDB");
	    Session currentSessionBindingDB = persistBindingDB(currentSession);
	
	
	    //close session
	    currentSessionBindingDB.getTransaction().commit();
	    currentSessionBindingDB.close();
	    SessionFactory thisFactory = currentSessionBindingDB.getSessionFactory();
	    thisFactory.close();
	}

	/**
	 * Test method for stripping assay relation from BindingDB assay values.
	 */
	@Test
	public void testSeparateAssayRelation(){
		String assayValue= ">3000";
		String assayRelation;
		if (assayValue.startsWith(">") || assayValue.startsWith("<")){
			assayRelation=assayValue.substring(0, 1);
			assayValue= assayValue.substring(1, assayValue.length());
			System.out.println("Assay relation is " + assayRelation);
			System.out.println("Assay value is " + assayValue);
		}
		else{
			assayRelation="=";
			System.out.println("Assay relation is =.");
			System.out.println("Assay value is " + assayValue);
		}
	}
	/*
	 * Method just to check for imatinib/DDR
	 * REFERENCE :
	 */
	@Test
	public void testBindingDBCheckImatinib() throws IOException{
		//first get imatinib drug object
		DruggabilityHibernatePersist hb = new DruggabilityHibernatePersist();
		//make sure to use query file
		Session session = hb.openSession("resources/drugHibernateV2_query.cfg.xml");
		System.out.println("SessionFactory opened.");
		session.beginTransaction();
		
		PrintStream psEvidence = new PrintStream("BeatAML/BeatAMLDrugs_CancerTargetomeEvidence_062817_TESTIMATINIB.txt");
		psEvidence.println("BeatAMLDrug_ListedAs" + "\t" + "Drug" +"\t" + "Target_Name" + "\t" + "Target_Type"+ "\t"+ "Target_UniProt" + "\t" + "Target_Species" + "\t"+ "Database" + "\t"+ "Reference"+ "\t"+"Assay_Type"+"\t" + "Assay_Relation"+ "\t"+"Assay_Value" + "\t"+"EvidenceLevel_Assigned");

		PrintStream osReference = new PrintStream("BeatAML/BindingDB_CheckReference.txt");
		PrintStream osImatinib = new PrintStream("BeatAML/BindingDB_CheckImatinib.txt");
		


		//parser from project - JUST WANT imatinib
		DrugInteractionsParser parser = new DrugInteractionsParser();
		String imatinibName = "Imatinib mesylate";
		System.out.println(" Querying Cancer Targetome for:" + imatinibName );
		Drug imatinib = parser.queryDatabaseDrug3(session, imatinibName, psEvidence);
		Set<String> imatinibSynonyms = imatinib.getDrugSynonyms();
		System.out.println("Printing imatinib synonyms");
		for (String syn: imatinibSynonyms){
			System.out.println(syn);
		}
		
		System.out.println("Checking for imatinib/ddr1 problem ");
		
		FileUtility bindingDBFile = new FileUtility();
		bindingDBFile.setInput("resources/BindingDb/BindingDB_All_07.03.16.tsv");
		
		
		String line = null;
		int matchCounter=0;
		int lineCounter=0;
		String header = bindingDBFile.readLine();//headers to our check file
		osImatinib.println(header);
		osReference.println(header);
		while ((line = bindingDBFile.readLine()) != null){
			//System.out.println(line);
			String[] tokens = line.split("\t", -1);//-1 to preserve trailing empty strings
			
			//drug and target info
			String ligand= tokens[5]; //DRUG NAME
			//System.out.println("Ligand: " + ligand);
			String targetName = tokens[6];//TARGET NAME
			String targetOrg = tokens[7]; //TARGET ORG
			
			//assay info
			String ki = tokens[8]; //KI
			String ic50 = tokens[9]; //IC50
			String kd = tokens[10]; //KD
			String ec50 = tokens[11];//EC50
			String kON = tokens[12]; //KON
			String kOFF = tokens[13]; //KOFF
			
			String pubMedID = tokens[18];
			
			//START CHECK HERE FOR IMATINIB/DDR1
			//START WITH REFERENCE 18183025
			
			String[] ligands = ligand.split("::");
			for (String eachLigand: ligands){
				if (imatinib.nameIsEquivalent(eachLigand)){
					System.out.println("Imatinib entry found: ");
					System.out.println(line);
					osImatinib.println(line);
					break;//once found, break out of this for loop, only need once
				}
			}
			
			if (pubMedID.contains("18183025")){
				System.out.println("Reference found: ");
				System.out.println(line);
				osReference.println(line);//sent to output file
			}
			lineCounter++;
		}
		System.out.println("Total number lines: " + lineCounter);
	}
/**
 * Method persists BindingDB interactions and evidence to our database.
 * Update Wednesday, June 28, 2017
 * 	debugging statements added \\debugging indicated
 * @param currentSession
 * @return
 * @throws IOException
 */
	public Session persistBindingDB(Session currentSession) throws IOException{
		//imatinib file for debugging
		//PrintStream imatinibFile = new PrintStream("BeatAML/BindingDB_TESTING_ImatinibMatches_062917.txt");
		//file for flagged interactions after imatinib/ddr1 bug fix
		PrintStream ps = new PrintStream("results_070617/Targetome_FlaggedNewInteractions_070617.txt");
		ps.println("Drug" +"\t" + "Target_Name" + "\t" + "Target_Type"+ "\t"+ "Target_UniProt" + "\t" + "Target_Species" + "\t"+ "Database" + "\t"+ "Reference"+ "\t"+"Assay_Type"+"\t" + "Assay_Relation"+ "\t"+"Assay_Value" + "\t"+"EvidenceLevel_Assigned");
		Set<Interaction> flaggedInteractions = new HashSet<Interaction>();
		
		//query session for current sets in our database
		Set<Drug> drugSet = queryDrugSet(currentSession);
		Set<Target> targetSet = queryTargetSet(currentSession);
		Set<Interaction> interactionSet = queryInteractionSet(currentSession);
		Set<LitEvidence> literatureSet = queryLitEvidenceSet(currentSession);
		Set<ExpEvidence> experimentalSet = queryExpEvidenceSet(currentSession);
		Set<Source> sourceSet= querySourceSet(currentSession);
		Set<DatabaseRef> databaseSet = queryDatabaseSet(currentSession);
		
		//BindingDb_All_07.03.16.tsv
		FileUtility bindingDBFile = new FileUtility();
		bindingDBFile.setInput("resources/BindingDb/BindingDB_All_07.03.16.tsv");
		
		//Create bindingDb Database Ref
		DatabaseRef bindingDB = new DatabaseRef();
		bindingDB.setDatabaseName("BindingDB");
		bindingDB.setDownloadDate("07.03.2016");
		bindingDB.setDownloadURL("https://www.bindingdb.org/bind/chemsearch/marvin/SDFdownload.jsp?all_download=yes");
		bindingDB.setVersion("2016_m6");
		databaseSet.add(bindingDB);//prob not necessary
		currentSession.save(bindingDB);
		
		
		String line = null;
		int matchCounter=0;
		int lineCounter=0;
		System.out.println(bindingDBFile.readLine());//headers
		while ((line = bindingDBFile.readLine()) != null){
		//	System.out.println(line);
			String[] tokens = line.split("\t", -1);//-1 to preserve trailing empty strings
			
			//drug and target info
			String ligand= tokens[5]; //DRUG NAME
			String targetName = tokens[6];//TARGET NAME
			String targetOrg = tokens[7]; //TARGET ORG
//			if (!targetOrg.equals("Homo sapiens")){
//				continue;//skip over if not human target
//			}
			if (targetOrg.length()<1){//check if species is empty
				targetOrg=null;
			}
			
			
			
			//assay info
			String ki = tokens[8]; //KI
			String ic50 = tokens[9]; //IC50
			String kd = tokens[10]; //KD
			String ec50 = tokens[11];//EC50
			String kON = tokens[12]; //KON
			String kOFF = tokens[13]; //KOFF
			
			//get assay type and assay value
			String assayType = checkBindingDBAssayType(ki, ic50, kd, ec50, kON, kOFF);
			String assayValue = checkBindingDBAssayValue(ki, ic50, kd, ec50, kON, kOFF);
			
			//add assay relation, take of <, > from assayValue as necessary
			String assayRelation;
			if (assayValue==null || assayValue.equals("null") || assayValue.isEmpty()){
				assayRelation= null;
			}
			//		
			if (assayValue!=null && (assayValue.startsWith(">") || assayValue.startsWith("<"))){
				assayRelation=assayValue.substring(0, 1);
				assayValue= assayValue.substring(1, assayValue.length());
			}
			else{
				assayRelation="=";
			}
			
			//source info	
			String parentSource = tokens[16]; //PARENT SOURCE
			String DOI = tokens[17];
			String pubMedID = tokens[18];
			String aid= tokens[19];//pubchem AID if available
			
			//IF NO PUBMED ID
			//We can use parent source to create LIT object
			//**FIX THIS IN FUTURE UPDATES - PULL PUBCHEM INFO DIRECTLY?
			if (pubMedID.length()==0){
				//no lit ref, but could be pubchem
				if (parentSource.equals("PubChem")){
					pubMedID="NA_PubChem_" + aid;
				}
				else{
					pubMedID = "NA_BindingDB_" + parentSource;
				}
					//missingRefCounter++;
			}
			String[] pubMedIDs = pubMedID.split("\\|"); //PUBMED ID
				
			
			//use first entry/chain for our target uniprot
//			String uniProtName = tokens[39];
//			String uniProtEntry = tokens[40];
			String uniProtID;
//			if (tokens[41].length() != 0){//tokens[41] is the uniProtID = what we want
				uniProtID = tokens[41];
//			}
//			else{
//				continue;//just skip over now
//						 //can return for uniprot ID look up later
//				//uniProtID=null; //prev code was setting to null
//			}
			
			String targetType = assignTargetType(targetName, uniProtID);
			//skip if no target type identified for now
			if (targetType.equals("NA")){
					lineCounter++;
					continue;
			}
			
			//need to split ligand name to get all ligand synonyms
			String[] ligands = ligand.split("::");
			
			//find Drug object that matches bindingDB ligand
			//return Drug object that matches
			//if null, continue to next line (will go to next line in file)
			//WORKING
			Drug drugMatch = findDrugMatch(drugSet, ligands);
			
			if (drugMatch==null){
				lineCounter++;
				continue;
			}
			//added for imatinib debugging
//			if (drugMatch.nameIsEquivalent("Imatinib")){
//				imatinibFile.println("Imatinib entry found, print full line: ");
//				imatinibFile.println(line);
//				imatinibFile.println("Match found!: " + drugMatch.getDrugName() + "at line: " + lineCounter);
//				imatinibFile.println("Values found: " + "Ki = " + tokens[8]+ "IC50 = " + tokens[9]+ "KD = " + tokens[10]
//										+ "EC50 = " + tokens[11]
//												+ "KON = " + tokens[12]
//														+ "KOFF = " + tokens[13]);
//				
//			}
			
			
			//if match found, use as drug for persisting section
			System.out.println("Match found!: " + drugMatch.getDrugName() + "at line: " + lineCounter);
			
			//testing
			System.out.println("Values found: " + "Ki = " + tokens[8]
					+ "IC50 = " + tokens[9]
							+ "KD = " + tokens[10]
									+ "EC50 = " + tokens[11]
											+ "KON = " + tokens[12]
													+ "KOFF = " + tokens[13]);


			System.out.println("Checking Target: " + targetName);
			System.out.println("Checking Target Uniprot: " );
			if (uniProtID!=null){
				System.out.println("Uniprot: " + uniProtID);
			}
			else{
				System.out.println("No Uniprot for this target!!");
			}
			System.out.println("Assay Org/Species: " + targetOrg);
			System.out.println("Checking Assay Record: ");
			System.out.println("Assay Type: " + assayType);
			System.out.println("Assay Value: " + assayValue);
			System.out.println("Parent Source: " + parentSource );
			System.out.println("Checking pubmedIDs: " + pubMedIDs.length);
			System.out.println("PubMED ID: " + pubMedIDs[0]);
			System.out.println(line);

			//TARGET
			Target target = createTarget(currentSession, targetSet, targetName, uniProtID, targetType, targetOrg);
			currentSession.save(target);

			//EXP EVIDENCE
			//current session
			//database ref
			//assay type 
			//assay low = NULL
			//assay median = assay value here
			//assay high = NULL
			//assay relation = assay relation here * added 9/13/16
			//assay description = NULL
			//target organism
			//parent source = text field **FOR NOW**
			//pubmedIDs = string[], split on pipe, handled fine here
			//only 1 exp pubmedID per bindingDb entry
			ExpEvidence exp = createExpEvidence(currentSession, bindingDB, assayType, 
					"NA",  assayValue,
					"NA", assayRelation, "NA", "NA", parentSource,
					pubMedIDs, literatureSet, sourceSet, experimentalSet);

			currentSession.save(exp);

			//INTERACTION **CENTERPIECE OF DATA MODEL
			//interactionType = null for now
			Interaction currentInteraction = createInteractionWithExp(currentSession, drugMatch, target, null, interactionSet, exp, flaggedInteractions);
			currentSession.save(currentInteraction);
//			//debugging add to imatinib file
//			if (drugMatch.nameIsEquivalent("Imatinib")){
//				imatinibFile.println("Committed interaction information: ");
//				imatinibFile.println("Drug: " + currentInteraction.getIntDrug() + " Target :" +  currentInteraction.getIntTarget());
//				if (currentInteraction.getInteractionSourceSet()!=null){
//					imatinibFile.println("Sources: " + currentInteraction.getInteractionSourceSet().size());
//					for (Source source : currentInteraction.getInteractionSourceSet()){
//						imatinibFile.println("Source: " + source.getSourceDatabase() +" and " + source.getSourceLiterature().getPubMedID());
//					}
//				}
//				if (currentInteraction.getExpEvidenceSet() != null){
//					imatinibFile.println("Experimental Evidence Set Size: " + currentInteraction.getExpEvidenceSet().size());
//					for (ExpEvidence evidence: currentInteraction.getExpEvidenceSet()){
//						imatinibFile.println("Evidence value: " + evidence.getAssayRelation());
//						imatinibFile.println("Evidence value: " + evidence.getAssayValueMedian());
//						imatinibFile.println("Evidence value: " + evidence.getAssayType());
//						Set<Source> evidenceSources = evidence.getExpSourceSet();
//						for (Source evidenceSource: evidenceSources){
//							imatinibFile.println("Source: " + evidenceSource.getSourceDatabase() +" and " + evidenceSource.getSourceLiterature().getPubMedID());
//							
//						}
//					}
//				}
//				//imatinibFile.println();
//			}

//			matchCounter++;
//		}

			
			lineCounter++;
			
		}
		//can we print info on flagged interactions here
		for (Interaction flaggedInt: flaggedInteractions){
			//adding just FOR PRINTING FOR FLAGGED FILE
			Set<ExpEvidence> eachExpEvidence = flaggedInt.getExpEvidenceSet();
			//for each piece of experimental evidence
			for (ExpEvidence eachEvidence: eachExpEvidence){
				//make sure to go through source set **CHECK ME**
				Set<Source> sourcesToPrint = eachEvidence.getExpSourceSet();
				for (Source sourceToPrint: sourcesToPrint){
					if(ps!=null){
					ps.println(flaggedInt.getIntDrug().getDrugName() + "\t" + flaggedInt.getIntTarget().getTargetName() + "\t" + flaggedInt.getIntTarget().getTargetType() + "\t"+
							flaggedInt.getIntTarget().getUniprotID() + "\t" + flaggedInt.getIntTarget().getTargetSpecies() + "\t" + sourceToPrint.getSourceDatabase().getDatabaseName() + "\t" + sourceToPrint.getSourceLiterature().getPubMedID()+"\t"+ eachEvidence.getAssayType() + "\t" + eachEvidence.getAssayRelation() + "\t" + eachEvidence.getAssayValueMedian()  + "\t" + "III_Flagged");
					}
				}
			}
			
		}
		
		//System.out.println("Num matches found: " + matchCounter);
		return currentSession;	
				
	}
	private Drug findDrugMatch(Set<Drug> drugSet, String[] ligands) {
		
		for (String ligand: ligands){
			for (Drug drug: drugSet){
				if (drug.nameIsEquivalent(ligand)){
					return drug;
				}
			}
		}		
		return null;
	}

	/**
	 * Helper method to grab assay value from BindingDB
	 * @param ki
	 * @param ic50
	 * @param kd
	 * @param kON
	 * @param kOFF
	 * @return
	 */
	private String checkBindingDBAssayValue(String ki, String ic50, String kd, String ec50, String kON, String kOFF) {
		
		if (ki.length() != 0){
			return ki;
		}
		if (ic50.length() != 0){
			return ic50;
		}
		if (kd.length() != 0){
			return kd;
		}
		if (ec50.length() != 0){
			return ec50;
		}
		if (kON.length() != 0){
			return kON;
		}
		if (kOFF.length() != 0){
			return kOFF;
		}
		
			return null;
		
	}

	/**
	 * Helper method to grab assay type from BindingDB
	 * @param ki
	 * @param ic50
	 * @param kd
	 * @param kON
	 * @param kOFF
	 * @return
	 */
	private String checkBindingDBAssayType(String ki, String ic50, String kd, String ec50, String kON, String kOFF) {
		if (ki.length() != 0){
			return "Ki";
		}
		if (ic50.length() != 0){
			return "IC50";
		}
		if (kd.length() != 0){
			return "KD";
		}
		
		if (ec50.length() != 0){
			return "EC50";
		}
		
		if (kON.length() != 0){
			return "KON";
		}
		if (kOFF.length() != 0){
			return "KOFF";
		}
		
			return null;
	}

	/**
	 * Helper method to parse TTD file for persistTTD method.
	 * Returns map of (ttdID, ttdObject).
	 * @throws IOException
	 */
	
	public Map<String, TTDObject> parseTTD(String fileName) throws IOException{
		//open TTD file
		FileUtility ttdFile = new FileUtility();
		ttdFile.setInput(fileName);
		
		
		//parse TTD file 
		//skip headers = first 10 lines
		for (int headerCount=0; headerCount<=9;headerCount++){
			ttdFile.readLine();
		}
		
		String line = null;
		int lineCounter = 9;
		
		Map<String, TTDObject> mapToTTDObject= new HashMap<String, TTDObject>();
		
		//iterate through file, create one object for each TTDID
		//set will store only unique objects
		while ((line = ttdFile.readLine()) != null){
			System.out.println(line);
			String[] tokens = line.split("\t");
			if( tokens.length != 3){
				continue;
			}
			String id= tokens[0];
			String field = tokens[1];
			String value= null;
			
			if (tokens[2]!=null){
				value = tokens[2];
			}
			
			
			System.out.println("ID: " + id);
			
			//check if we have this id in map keyset
			if (mapToTTDObject.keySet().contains(id)){
				//then get value= ttd object, add to it
				TTDObject currentObject = mapToTTDObject.get(id);
				
				if (field.equals("Name")){
					System.out.println("NAME: " + value);
					currentObject.setTargetName(value);//set name, since first entry
				}
				
				if (field.equals("UniProt ID")){
					System.out.println("UNIPROT: " + value);
					currentObject.setTargetUniprot(value);//set uniprot
				}
				//check this?7/14/16
				mapToTTDObject.put(id, currentObject);
				
				//look for drugs
			    if (field.equals("Drug(s)")){
			    	System.out.println("Drug found!");
					if (currentObject.getTargetDrugs()!=null){
						Set<String> drugs = currentObject.getTargetDrugs();
						drugs.add(value);
						currentObject.setTargetDrugs(drugs);
					}
					else{
						Set<String> newDrugs = new HashSet<String>();
						newDrugs.add(value);
						currentObject.setTargetDrugs(newDrugs);
					}
				}

			}
			//if map does not contain id, create new object and move on
			else{
				TTDObject object = new TTDObject();
				object.setTargetID(id);
				
				if (field.equals("Name")){
					object.setTargetName(value);//set name, since first entry
				}
				
				if (field.equals("UniProt ID")){
					object.setTargetUniprot(value);//set uniprot
				}
				
				mapToTTDObject.put(id, object);
				System.out.println("Object added!");
			}
			
			lineCounter++;
			//break for testing
			//if (lineCounter==100){
			//	break;
			//}
		}//end while loop through file
			
		
	return mapToTTDObject;

	}
	
	/**
	 * @param currentSession
	 * @param literatureSet
	 * @param databaseRef
	 * @param refBlock //refs for our interaction
	 * @return
	 */
	private Set<Source> createSourceSet(Session currentSession, 
										Set<Source> fullSourceSet, 
										Set<LitEvidence> literatureSet, 
										DatabaseRef databaseRef, 
										String refBlock) {
		//create new source set
		Set<Source> interactionSourceSet = new HashSet<Source>();
		//for each ref, create new source object
		String[] refs = refBlock.split("\\|");
		for (String ref:refs){

			//check lit
			if (checkLiteratureSet(literatureSet, ref)){
				//then get and add
				LitEvidence lit = getLiteratureFromSet(literatureSet, ref);
				currentSession.save(lit);//prob not necc
				
				Source currentSource = createSource(currentSession, fullSourceSet, databaseRef, lit);
				currentSession.save(currentSource);
				fullSourceSet.add(currentSource);//add to full set
				interactionSourceSet.add(currentSource);//add to int set
			}
			else{//otherwise create new lit object
				LitEvidence newLit = new LitEvidence();
				newLit.setPubMedID(ref);
				literatureSet.add(newLit);//add to full set
				currentSession.save(newLit);
				
				//create source with newLit obkect
			    Source currentSource = createSource(currentSession, fullSourceSet, databaseRef, newLit);
			    currentSession.save(currentSource);
			    fullSourceSet.add(currentSource);//add to full set
			    interactionSourceSet.add(currentSource);//add to int set
			}
			
			
		}
		return interactionSourceSet;
	}

	/**
	 * @param currentSession
	 * @param fullSourceSet
	 * @param drugBank
	 * @param interactionSourceSet
	 * @param lit
	 */
	private Source createSource(Session currentSession, Set<Source> fullSourceSet, 
			                    DatabaseRef drugBank, LitEvidence lit) {
		//check full source set for presence of source object
		if (checkSourceSet(fullSourceSet, lit, drugBank)){//if present then get source
			Source existingSource = getSourceFromSet(fullSourceSet, lit, drugBank);
			
			return existingSource;
		}
		else{//otherwise create new source
			//create new source
			Source newSource = new Source();
			newSource.setSourceLiterature(lit);
			newSource.setSourceDatabase(drugBank);
			
			return newSource;
		}
	}
	
	
	public Map<Drug, String> beta_checkCoverageKinaseResource(Set<Drug> inputDrugSet) throws IOException{
		System.out.println("Reading in Sorger Kinase Database File");
		
		FileUtility fileUt = new FileUtility();
		fileUt.setInput("resources/beta_v2/sorger_targets.txt");
		Set<String> quickDrugSet = new HashSet<String>();
		
		fileUt.readLine();//skip headers
		String line = null;
		while ((line = fileUt.readLine()) != null){
			String[] tokens = line.split("\t");
			String ligand=tokens[1];

			System.out.println("Drug parsed: " + ligand.trim());
			quickDrugSet.add(ligand);
		}
		//now quick check over our drug set
		HashMap<Drug, String> drugToResourceCoverage = new HashMap<Drug, String>();
		for (Drug inputDrug: inputDrugSet){
			for (String ligand: quickDrugSet) {
			if (inputDrug.nameIsEquivalent(ligand.trim())) {
				drugToResourceCoverage.put(inputDrug, "KinaseResource_Yes");
			}
				
			}
		}

		return drugToResourceCoverage;
	
	}
	
	public Map<Drug, String> beta_checkCoverageTTD(Set<Drug> inputDrugSet) throws IOException{

		Set<String> quickDrugSet = new HashSet<String>();
		System.out.println("Reading in TTD File");

		//**Note, original parsing of TTD database was a little different
		//because it it set-up in a target centric manner
		//so first we parse the targets, and for coverage check we can just grab the drug sets off the target
		//Set TTD file
		String ttdFileName="resources/TTD/TTD_download_06.20.16.txt";//older version
		//we get more targets here

		//call parseTTD method here
		Map<String, TTDObject> setTTD = parseTTD(ttdFileName);

		//for each TTD entry
		for (String entry: setTTD.keySet()){
			//get TTD object	
			TTDObject object = setTTD.get(entry);

			//get drugs for the target			
			Set<String> entryDrugSet = object.getTargetDrugs();

			//iterate through drug set
			//make sure there are drugs listed for targets
			if (entryDrugSet!=null) {
				for (String entryDrug: entryDrugSet){
					quickDrugSet.add(entryDrug);//add each TTD drug to our quick set
				}
			}
		}

		System.out.println("Finished reading in the TTD drugs.");
		System.out.println("Now check TTD drugs against our set.");

		//loop to build map
		//now quick check over our drug set
		HashMap<Drug, String> drugToResourceCoverage = new HashMap<Drug, String>();

		for (Drug inputDrug: inputDrugSet){
			//note; for bindingDB this will be all the synonyms
			//this should still work, because if we hit a ligand that matches, we will store
			//03/19/21 should be good for just our quick coverage check
			for (String ligand: quickDrugSet) {
				if (inputDrug.nameIsEquivalent(ligand.trim())) {
					drugToResourceCoverage.put(inputDrug, "TTD_Yes");
				}

			}
		}
		System.out.println("Finished checking TTD drugs against our set.");

		return drugToResourceCoverage;

	}

	public Map<Drug, String> beta_checkCoverageBindingDB(Set<Drug> inputDrugSet) throws IOException{
		System.out.println("Reading in BindingDB Database File");

		FileUtility fileUt = new FileUtility();
		fileUt.setInput("resources/BindingDb/BindingDB_All_07.03.16.tsv");

		Set<String> quickDrugSet = new HashSet<String>();

		fileUt.readLine();//skip headers
		String line = null;
		
		int counter = 0;//add counter so we can parse BindingDB
		while ((line = fileUt.readLine()) != null){
			String[] tokens = line.split("\t", -1);//-1 to preserve trailing empty strings
			//drug 
			String ligand= tokens[5]; //DRUG NAME
			
			//System.out.println("BindingDB ligand: " + ligand);
			//need to split ligand name to get all ligand synonyms
			String[] ligands = ligand.split("::");
			for (String eachLigand: ligands) {
				//System.out.println("BindingDB eachligand: " + eachLigand);
				quickDrugSet.add(eachLigand);//add each individual BindingDB synonym to our drug set
			}

			//System.out.println("Drug parsed: " + ligand);
//			
//			if (counter==10){
//				break;
//			}
//			counter++;
		}
		System.out.println("Finished reading in the BindingDB drugs.");
		System.out.println("Now check BindingDB drugs against our set.");
		
		//now quick check over our drug set
		HashMap<Drug, String> drugToResourceCoverage = new HashMap<Drug, String>();
		
		for (Drug inputDrug: inputDrugSet){
			
			//note; for bindingDB this will be all the synonyms
			//this should still work, because if we hit a ligand that matches, we will store
			//03/19/21 should be good for just our quick coverage check
			for (String ligand: quickDrugSet) {
				if (inputDrug.nameIsEquivalent(ligand.trim())) {
					drugToResourceCoverage.put(inputDrug, "BindingDB_Yes");
				}

			}
		}

		return drugToResourceCoverage;
	}

	
	public Map<Drug, String> beta_checkCoverageDrugBank(Set<Drug> inputDrugSet) throws IOException{
		
		FileUtility fileUt = new FileUtility();
		fileUt.setInput("resources/beta_v2/drugbank_test_20210127.csv");
		Set<String> quickDrugSet = new HashSet<String>();
		
		String[] headers = fileUt.readLine().split(",");
		//int lineCounter = 2;
		String line = null;
		while ((line = fileUt.readLine()) != null){
			String[] tokens = line.split(",");

			String ligand=tokens[0];
			
			quickDrugSet.add(ligand);

		}
		//now quick check over our drug set
		
		//PrintStream ps = new PrintStream("results_beta_V2/RunningDrugDeck_V1_Add_BetaV2_IUPHAR.tsv");
		//ps.println("Drug" + "\t" + "IUPHAR");
		HashMap<Drug, String> drugToDrugBankCoverage = new HashMap<Drug, String>();
		for (Drug inputDrug: inputDrugSet){
			for (String drugbank_ligand: quickDrugSet) {
			if (inputDrug.nameIsEquivalent(drugbank_ligand)) {
				//ps.println(inputDrug.getDrugName() + "\t" + "Yes" );
				drugToDrugBankCoverage.put(inputDrug, "DrugBank_Yes");
			}
				
			}
		}
		
		//ps.close();

		return drugToDrugBankCoverage;
	
		
	} 

	/**
	 * TODO - clean up
	 * @param inputDrugSet
	 * @throws IOException 
	 */
	public Map<Drug, String> beta_checkCoverageIUPHAR(Set<Drug> inputDrugSet) throws IOException{


		//PARSE IUPHAR INTERACTIONS, PERSIST
		FileUtility fileUt = new FileUtility();
		//fix this - will need to use regex to ignore commas in data?**
		fileUt.setInput("resources/IUPHAR/IUPHAR_interactions_fixedAB_06.02.16.csv");
		//create DatabaseRef
		//				DatabaseRef iuphar = new DatabaseRef();
		//				iuphar.setDatabaseName("IUPHAR");
		//				iuphar.setDownloadDate("06.15.2016");
		//				iuphar.setDownloadURL("NA");
		//				iuphar.setVersion("NA");
		//				databaseSet.add(iuphar);//prob not necessary
		//				currentSession.save(iuphar);

		//parse file and create quick drug set
		//added 
		Set<String> quickDrugSet = new HashSet<String>();
		
		String[] headers = fileUt.readLine().split(",");
		int lineCounter = 2;
		String line = null;
		while ((line = fileUt.readLine()) != null){
			String[] tokens = line.split(",", -1);//need -1?

			//skip if lines != 34 ** fix this
			if (tokens.length != 34){
				System.out.println(lineCounter);
				continue;
			}
			//get iuphar info
			String targetGene=tokens[2];//target gene name
			String targetUniprot=tokens[3];//uniprot

			//					//skip any blank uniprots
			////					if (targetUniprot.length()==0){
			////						continue;
			////					}
			//					String targetType= assignTargetType(targetGene, targetUniprot);
			//					//skip if we can't identify target
			//					if (targetType.equals("NA")){
			//						continue;
			//					}
			//						
			//					String targetSpecies=tokens[9];//target species, all for now
			//					targetSpecies=getSpeciesName(targetSpecies);//convert to species name
			//																//if human
			//					
			String ligand=tokens[10];
			
			quickDrugSet.add(ligand);

		}
		//now quick check over our drug set
		
		//PrintStream ps = new PrintStream("results_beta_V2/RunningDrugDeck_V1_Add_BetaV2_IUPHAR.tsv");
		//ps.println("Drug" + "\t" + "IUPHAR");
		HashMap<Drug, String> drugToIUPHARCoverage = new HashMap<Drug, String>();
		for (Drug inputDrug: inputDrugSet){
			for (String IUPHAR_ligand: quickDrugSet) {
			if (inputDrug.nameIsEquivalent(IUPHAR_ligand)) {
				//ps.println(inputDrug.getDrugName() + "\t" + "Yes" );
				drugToIUPHARCoverage.put(inputDrug, "IUPHAR_Yes");
			}
				
			}
		}
		
		//ps.close();

		return drugToIUPHARCoverage;
	}
	
	/**
	 * Method parses IUPHAR interactions and evidence and persists to our database.
	 * Method returns currentSession. 
	 * @throws IOException 
	 */
	public Session persistIUPHAR(Session currentSession) throws IOException{
		//query database for current info
		Set<Drug> drugSet = queryDrugSet(currentSession);
		//no need to save these since these are saved throughout method
		//each time we make a change or update
		Set<Target> targetSet = queryTargetSet(currentSession);
		Set<Interaction> interactionSet = queryInteractionSet(currentSession);
		Set<LitEvidence> literatureSet = queryLitEvidenceSet(currentSession);
		Set<ExpEvidence> experimentalSet = queryExpEvidenceSet(currentSession);
		Set<Source> sourceSet= querySourceSet(currentSession);
		Set<DatabaseRef> databaseSet = queryDatabaseSet(currentSession);
		
	
		//PARSE IUPHAR INTERACTIONS, PERSIST
		FileUtility fileUt = new FileUtility();
		//fix this - will need to use regex to ignore commas in data?**
		fileUt.setInput("resources/IUPHAR/IUPHAR_interactions_fixedAB_06.02.16.csv");
		//create DatabaseRef
		DatabaseRef iuphar = new DatabaseRef();
		iuphar.setDatabaseName("IUPHAR");
		iuphar.setDownloadDate("06.15.2016");
		iuphar.setDownloadURL("NA");
		iuphar.setVersion("NA");
		databaseSet.add(iuphar);//prob not necessary
		currentSession.save(iuphar);
		
		//parse file 
		
		String[] headers = fileUt.readLine().split(",");
		int lineCounter = 2;
		String line = null;
		while ((line = fileUt.readLine()) != null){
			String[] tokens = line.split(",", -1);//need -1?
			
			//skip if lines != 34 ** fix this
			if (tokens.length != 34){
				System.out.println(lineCounter);
				continue;
			}
			//get iuphar info
			String targetGene=tokens[2];//target gene name
			String targetUniprot=tokens[3];//uniprot
			
			//skip any blank uniprots
//			if (targetUniprot.length()==0){
//				continue;
//			}
			String targetType= assignTargetType(targetGene, targetUniprot);
			//skip if we can't identify target
			if (targetType.equals("NA")){
				continue;
			}
				
			String targetSpecies=tokens[9];//target species, all for now
			targetSpecies=getSpeciesName(targetSpecies);//convert to species name
														//if human
			
			String ligand=tokens[10];//drug name
			String interactionType=tokens[16];//interaction type
			
			String assayType=tokens[25];//IC50, KD, KI, EC50
			//check to see if an original value is actualy recorded
			boolean noValueFound = false;
		    String assayValueLow= tokens[26];//low
		    String assayValueMedian= tokens[27];//med or single value
		    String assayValueHigh= tokens[28];//high
		    //check if assay values are all null, set flag if so
		    if (checkIUPHARValueNull(assayValueLow, assayValueMedian, assayValueHigh)){
		    	noValueFound=true;//use this flag later in creating interaction
		    }
		    
			String assayRelation=tokens[29];//>, <, or =
			
			String assayDescription=tokens[30];//additional metadata
			//if empty ref, we just mark as NA_IUPHAR for now
			String pubMedID = tokens[33];
			if (pubMedID.length()==0){
				pubMedID = "NA_IUPHAR";
			}
			//use pubMedID for ref block here
			//use string array for already split
			String[] pubMedIDs = pubMedID.split("\\|");//need to split
			
			
			//for each of our drugs
			for (Drug drug: drugSet){
				//if IUPHAR ligand matches drug
				if (drug.nameIsEquivalent(ligand)){
					System.out.println("MATCH found: " + ligand);
					
					
					//TARGET
					//method either pulls existing target or creates new target
					Target target = createTarget(currentSession, targetSet, targetGene, targetUniprot, targetType, targetSpecies);
					currentSession.save(target);
					
					//INTERACTION WITH NO EXPERIMENTAL EVIDENCE
					//added 9/13/16 because IUPHAR seems to have not retained some orig assay values
					//just create Interaction->Source, no ExpEvidence here
					if (noValueFound){//check flag for null assay values
						//create source set for this interaction
						Set<Source> currentInteractionSourceSet = createSourceSet(currentSession, sourceSet, literatureSet, iuphar, pubMedID);
						//creat interaction
						Interaction currentInteraction = createInteraction(currentSession, drug, target,interactionType, interactionSet, currentInteractionSourceSet);
						currentSession.save(currentInteraction);
					}
					//else make Interaction->ExpEvidence->Source (as previously)
					else{
					
					//INTERACTION WITH EXPERIMENTAL EVIDENCE
					//create Exp evidence from current IUPHAR entry
					//parent source = NA for now
					//target species added 7/20/16
					ExpEvidence exp = createExpEvidence(currentSession, iuphar, assayType, 
													    assayValueLow, assayValueMedian, assayValueHigh,
													    assayRelation, assayDescription, "NA", "NA",
													    pubMedIDs, literatureSet, sourceSet, experimentalSet);
											 //pass to create interaction method
					currentSession.save(exp);
					
					//take exp and pass to createInteraction()
					//within that method, checks existing interactionSet
					//if interaction already present, evidence gets added to the evidence set
					//otherwise, create new Interaction
					//save to session
					
					//create interaction **CENTERPIECE OF DATA MODEL
					Interaction currentInteraction = createInteractionWithExp(currentSession, drug, target,interactionType, interactionSet, exp, null);
					currentSession.save(currentInteraction);
					}
				}

		}
			lineCounter++;
		}
		System.out.println("Total num lines: " + lineCounter);
		return currentSession;
	}

	/**
	 * Method converts species name listed in IUPHAR to 
	 * standard species name.
	 * 
	 * @param targetSpecies
	 * @return
	 */
	private String getSpeciesName(String targetSpecies) {
		if (targetSpecies.equals("Human")){
			targetSpecies="Homo sapiens";
		}
		return targetSpecies;
		
	}

	/**
	 * Method checks three strings fields for IUPHAR value
	 * Returns false if there is a value present, returns true if all values 
	 * are null (or "-" which is used by IUPHAR)
	 * @param assayValueLow
	 * @param assayValueMedian
	 * @param assayValueHigh
	 * @return
	 */
	private boolean checkIUPHARValueNull(String assayValueLow, String assayValueMedian, String assayValueHigh) {
		// flags
		boolean noLow = false;
		boolean noMed = false;
		boolean noHigh = false;
		//check empty or null? or length 0
		if (assayValueLow.isEmpty() || assayValueLow.equals("-")){
			noLow= true;
		}
		if (assayValueMedian.isEmpty()|| assayValueMedian.equals("-")){
			noMed= true;
		}		
		if (assayValueHigh.isEmpty() || assayValueHigh.equals("-")){
			noHigh= true;
		}
		
		if (noLow && noMed && noHigh){
			return true;
		}
		
		
		//else return false
		return false;
	}
	/**
	 * Test method for checking IUPHAR values
	 * Working 9/13/16
	 */
	@Test
	public void testCheckIUPHARValueNull(){
		String low = null;
		String med = "-" ;
		String high = "89.3";//try with value/null here
		if (checkIUPHARValueNull(low, med, high)){
			System.out.println("All values are null.");
		}
		else{
			System.out.println("Assay value found.");
		}
	}

	private Target createTarget(Session currentSession, Set<Target> targetSet, String targetGene, String targetUniprot, String targetType, String targetSpecies) {
		
		//Target check
		boolean targetFound=false;
		for (Target target:targetSet){
			if (target.isEquivalentUniProt(targetUniprot, targetType, targetGene)){
				System.out.println("EXISTING TARGET found: " + target.getTargetName());
				targetFound=true;
				//update target synonyms and species if necess
				target.getTargetSynonyms().add(targetGene);
				//update species if we have more information
				if (targetSpecies!=null && target.getTargetSpecies()==null){//try this, just updating species record
					//if (target.getTargetSpecies().equalsIgnoreCase("null") || target.getTargetSpecies().equals("")|| target.getTargetSpecies()==null || target.getTargetSpecies().isEmpty() ){
						target.setTargetSpecies(targetSpecies);
					//}
				}
				//then use this target for interaction
				return target;
			}
			
		}
		System.out.println("NO EXISTING TARGET found");
		if (targetFound==false){
			//create new target
			Target newTarget = new Target();
			newTarget.setTargetName(targetGene);
			newTarget.setUniprotID(targetUniprot);
			newTarget.setTargetType(targetType);
			newTarget.setTargetSpecies(targetSpecies);
			Set<String> tarSynonyms= new HashSet<String>();
			tarSynonyms.add(targetGene);//add target name to synonym set
			newTarget.setTargetSynonyms(tarSynonyms);
			targetSet.add(newTarget);//update target set, need to update this
			return newTarget;
		}
		return null; //default?
	}

	/**
	 * Method creates ExpEvidence object with full set of Sources.
	 * @param currentSession
	 * @param databaseRef
	 * @param assayType
	 * @param assayValueLow
	 * @param assayValueMedian
	 * @param assayValueHigh
	 * @param assayRelation
	 * @param assayDescription
	 * @param assaySpecies
	 * @param parentSource
	 * @param pubMedIDs
	 * @param literatureSet
	 * @param sourceSet
	 * @param experimentalSet
	 * @return
	 */
	private ExpEvidence createExpEvidence(Session currentSession, DatabaseRef databaseRef, String assayType,
			String assayValueLow, String assayValueMedian, String assayValueHigh, String assayRelation,
			String assayDescription, String assaySpecies, String parentSource, String[] pubMedIDs,
			Set<LitEvidence> literatureSet, Set<Source> sourceSet, Set<ExpEvidence> experimentalSet) {

		//create expEvidence object
		ExpEvidence exp = new ExpEvidence();
		exp.setAssayType(assayType);
		exp.setAssayUnits("nM"); //set all iuphar to NM, set all bindingDB to nM
		exp.setAssayValueLow(assayValueLow);
		exp.setAssayValueMedian(assayValueMedian);
		exp.setAssayValueHigh(assayValueHigh);
		exp.setAssayRelation(assayRelation);
		exp.setAssayDescription(assayDescription);
		exp.setAssaySpecies(assaySpecies);
		exp.setParentSource(parentSource);
		
		//create Source for rel from exp->lit, database
		//create set for sources for this experimental evidence
		Set<Source> expEvSources = new HashSet<Source>();
		
		for (String pubMedID: pubMedIDs){//get each pubmed reg
			
			LitEvidence lit;
			if (checkLiteratureSet(literatureSet, pubMedID)){
				lit = getLiteratureFromSet(literatureSet, pubMedID);
				System.out.println("LIT OBJECT REUSED.");
				
			}
			else{
				lit = new LitEvidence();//new lit object
				lit.setPubMedID(pubMedID);
				currentSession.save(lit);
				literatureSet.add(lit);//necc?
				
			}
			//check for existing source
			Source source;
			if (checkSourceSet(sourceSet, lit, databaseRef)){
				//then use this one
				source = getSourceFromSet(sourceSet, lit, databaseRef);
				System.out.println("SOURCE OBJECT REUSED.");
			}
			else{//else create new
				source= new Source();
				source.setSourceLiterature(lit);
				source.setSourceDatabase(databaseRef);
				sourceSet.add(source);//necc?
			}
			//add to Sources, set source for experiment
			expEvSources.add(source);
			currentSession.save(source);
			exp.setExpSourceSet(expEvSources);//set 7/20
			
		}
		
		currentSession.save(exp);//save exp to session
		experimentalSet.add(exp);//remember to add to exp set
		return exp;
	}

	private ExpEvidence getExpEvidenceFromSet(String assayType, String assayValueMedian, String parentSource, Set<ExpEvidence> experimentalSet) {
		
		
		for (ExpEvidence eachExp: experimentalSet){
			if (eachExp.isEquivalent(assayType, assayValueMedian, parentSource)){
				return eachExp;
			}
		}

		return null;
	}

	private boolean checkSourceSet(Set<Source> sourceSet, LitEvidence lit, DatabaseRef iuphar) {
		
		for (Source source: sourceSet){
			DatabaseRef currentDatabaseRef = source.getSourceDatabase();
			String databaseName = currentDatabaseRef.getDatabaseName();
			LitEvidence currentLitEvidence = source.getSourceLiterature();
			String litPubMedID = currentLitEvidence.getPubMedID();
			
			//if lit 
			if (databaseName.equals(iuphar.getDatabaseName()) && litPubMedID.equals(lit.getPubMedID())){
				return true;
			}
		}
		return false;
	}

	private Source getSourceFromSet(Set<Source> sourceSet, LitEvidence lit, DatabaseRef iuphar) {
		
		for (Source source: sourceSet){
			DatabaseRef currentDatabaseRef = source.getSourceDatabase();
			String databaseName = currentDatabaseRef.getDatabaseName();
			LitEvidence currentLitEvidence = source.getSourceLiterature();
			String litPubMedID = currentLitEvidence.getPubMedID();
			
			//if lit 
			if (databaseName.equals(iuphar.getDatabaseName()) && litPubMedID.equals(lit.getPubMedID())){
				return source;
			}
		}
		return null;
	}
	
	private boolean checkLiteratureSet(Set<LitEvidence> literatureSet, String pubMedID) {
		
		for (LitEvidence lit: literatureSet){
			if (lit.getPubMedID().equals(pubMedID)){
				return true;
			}
		}
		return false;
	}
	
	private LitEvidence getLiteratureFromSet(Set<LitEvidence> literatureSet, String pubMedID) {
		
		for (LitEvidence lit: literatureSet){
			if (lit.getPubMedID().equals(pubMedID)){
				return lit;
			}
		}
		return null;
	}

	/**
	 * Method takes interaction set and checks whether an interaction is present
	 * for given drug and target. Drug checked by name/synonym, target checked by uniprot ID.
	 */
	public boolean checkInteractionSet(Set<Interaction> interactionSet, Drug drugToCheck, Target targetToCheck){
		
		if (interactionSet!= null){
		
		for (Interaction interaction: interactionSet){
			Drug intDrug = interaction.getIntDrug();
			Target intTarget = interaction.getIntTarget();
			
			//check for match
			if (intDrug.isEquivalent(drugToCheck)&& intTarget.isEquivalent(targetToCheck)){
				//then this is the interaction we want
				return true;
			}
			else{
				continue;
			}
		}
		}
		
		return false;
	}
	
	/**
	 * Method takes interaction set and retrieves matching Interaction based on
	 * a given drug and target. Drug checked by name/synonym, target checked by uniprot ID.
	 */
	public Interaction getInteractionFromSet(Set<Interaction> interactionSet, Drug drugToCheck, Target targetToCheck){
		
		for (Interaction interaction: interactionSet){
			Drug intDrug = interaction.getIntDrug();
			Target intTarget = interaction.getIntTarget();
			
			//check for match
			if (intDrug.isEquivalent(drugToCheck)&& intTarget.isEquivalent(targetToCheck)){
				//then this is the interaction we want
				return interaction;
			}
			else{
				continue;
			}
		}
		return null;//returns null if no interaction found**FIX THIS
		
	}
	
	/**
	 * Update method description here
	 * @param currentSession
	 * @param drug
	 * @param target
	 * @param targetSet
	 * @param interactionSet
	 * @param currentExpEvidence
	 * @return
	 */
	private Interaction createInteractionWithExp(Session currentSession, Drug drug, Target target, String interactionType, Set<Interaction> interactionSet, ExpEvidence currentExpEvidence, Set<Interaction> flaggedInteractions) {
		
		//check target here?
		
		//if interaction already exists, grab it
		if (checkInteractionSet(interactionSet, drug, target)){
			//our interaction
			Interaction interaction = getInteractionFromSet(interactionSet, drug, target);
			
			//update exp evidennce set for interaction
			Set<ExpEvidence> currentExpSet = interaction.getExpEvidenceSet();
			if (currentExpSet!=null){
				currentExpSet.add(currentExpEvidence);
				interaction.setExpEvidenceSet(currentExpSet);
			}
			//edits added Thurs 6/29/17
			else{//if there IS an interaction and no experimental evidence logged
				//we need to create new set and load into interaction

				Set<ExpEvidence> newExpSet = new HashSet<ExpEvidence>();
				newExpSet.add(currentExpEvidence);
				interaction.setExpEvidenceSet(newExpSet);
				
				//add flag so that we can print out this info**
				flaggedInteractions.add(interaction);			
			}
			
			//save interaction to session
			currentSession.save(interaction);
			return interaction;
		}
		//otherwise create interaction
		else{

			Interaction newInteraction = new Interaction();
			newInteraction.setIntDrug(drug);
			newInteraction.setIntTarget(target);//set target
			newInteraction.setInteractionType(interactionType);
			
			//create and set evidence
			Set<ExpEvidence> currentExpSet = new HashSet<ExpEvidence>();
			currentExpSet.add(currentExpEvidence);
			newInteraction.setExpEvidenceSet(currentExpSet);
			
			interactionSet.add(newInteraction);//remember to add to our running set
			return newInteraction;
		}
	}
		
/**
 * Method to create interaction with no Experimental Evidence	
 * @param currentSession
 * @param drug
 * @param target
 * @param interactionType
 * @param interactionSet //full set of interaction in the current session
 * @param interactionSourceSet //set of sources for interaction we are creating
 * @return
 */
	
private Interaction createInteraction(Session currentSession, Drug drug, Target target, String interactionType, Set<Interaction> interactionSet, Set<Source> interactionSourceSet) {
		
		//if interaction already exists, grab it
		if (checkInteractionSet(interactionSet, drug, target)){
			//our interaction
			Interaction interaction = getInteractionFromSet(interactionSet, drug, target);
			
			//check if interaction set has source set
			if (interaction.getInteractionSourceSet() != null && interaction.getInteractionSourceSet().size()>0){
				
				Set<Source> currentSourceSet = interaction.getInteractionSourceSet();
				//add interaction sources to current source set
				for (Source interactionSource: interactionSourceSet){
					currentSourceSet.add(interactionSource);
				}
				interaction.setInteractionSourceSet(currentSourceSet);
			}
			else{//otherwise create new intSourceSet
				Set<Source> newSourceSet = new HashSet<Source>();
				if ( interactionSourceSet != null){
					for (Source interactionSource: interactionSourceSet){
						newSourceSet.add(interactionSource);
					}
				}
				interaction.setInteractionSourceSet(newSourceSet);
			}
			
			
			//save interaction to session
			currentSession.save(interaction);
			return interaction;
		}
		//otherwise create new interaction
		else{Interaction newInteraction = new Interaction();
			newInteraction.setIntDrug(drug);//set drug
			newInteraction.setIntTarget(target);//set target
			newInteraction.setInteractionType(interactionType);//set interaction type
			newInteraction.setInteractionSourceSet(interactionSourceSet);
			
			interactionSet.add(newInteraction);//remember to add to our running set
			currentSession.save(newInteraction);
			return newInteraction;
		}
	}
	
	/**
	 * Method tests persistTTD(). Note that this is a stand-alone test method, 
	 * so it will only send ttdinteractions to our database.
	 * Tested, working Thurs 7/14/16.
	 * Need to address null targets proteins (no uniprot) and ambiguous targets.
	 * Dropping Nulls for now? **FIX THIS**
	 * @throws IOException 
	 * @throws ParseException 
	 */
	@Test
	public void testPersistTTD() throws IOException, ParseException{
		//Session currentSession = drugsPersist("drug_sets/testDrugSet.txt", 1, 0, "\t");
	    Session currentSession = beta_persistDrugSet("drug_sets/scrapedNCIDrugs_05.11.16.txt", 1, 0, "\t" );
		System.out.println("Drugs loaded to database.");
	
		System.out.println("Starting on persistTTD");
		//persistTTD works of TTD_download_06.20.16.txt
	    Session currentSessionTTD = persistTTD(currentSession);
	
	
	    //close session
	    currentSessionTTD.getTransaction().commit();
	    currentSessionTTD.close();
	    SessionFactory thisFactory = currentSessionTTD.getSessionFactory();
	    thisFactory.close();
	}


	/**
	 * Method tests persistDrugBank(). Note that this is a stand-alone test method, 
	 * so it will only send drugbank interactions to our database.
	 * 
	 * @throws IOException 
	 * @throws ParseException 
	 */
	@Test
	public void testPersistDrugBank() throws IOException, ParseException{
		//Session currentSession = drugsPersist("drug_sets/testDrugSet.txt", 1, 0, "\t");
	    Session currentSession = beta_persistDrugSet("drug_sets/scrapedNCIDrugs_05.11.16.txt", 1, 0, "\t" );
		System.out.println("Drugs loaded to database.");

	    //persist drugbank *FIX THIS - currently only is a check
	    //for parsing drugbank xml file
		System.out.println("Starting on persistDrugBank");
		//persistDrugBank works off of DrugBank_ParseInteractions_06.21.16.txt
	    Session currentSessionDrugBank = persistDrugBank(currentSession);

	    //added query here to get file for drug set
	    PrintStream ds = new PrintStream("resources/DruggabilityV2_AllDrugs_12.02.16.txt");
	    Set<Drug> allDrugs =  queryDrugSet(currentSessionDrugBank);
	   
	    for (Drug drug: allDrugs){
	    	ds.println(drug.getDrugName());
	    }
	    ds.close();
	    
	    //close session
	    currentSessionDrugBank.getTransaction().commit();
	    currentSessionDrugBank.close();
	    SessionFactory thisFactory = currentSessionDrugBank.getSessionFactory();
	    thisFactory.close();
	}
	
	/**
	 * Test method for IUPHARPersist(). 
	 * Correctly works on full NCI Drug list as of Wed, 6/15/16.
	 * Address IUPHAR fix-it list. **FIX IT
	 * @throws IOException
	 * @throws ParseException 
	 */
	@Test
	public void testPersistIUPHAR() throws IOException, ParseException{
		//Session currentSession = drugsPersist("drug_sets/testDrugSet.txt", 1, 0, "\t");
		Session currentSession = beta_persistDrugSet("drug_sets/scrapedNCIDrugs_05.11.16.txt", 1, 0, "\t" );
		
		//persist iuphar 
		Session currentSessionIUPHAR = persistIUPHAR(currentSession);

		
		//close session
		currentSessionIUPHAR.getTransaction().commit();
		currentSessionIUPHAR.close();
		SessionFactory thisFactory = currentSessionIUPHAR.getSessionFactory();
		thisFactory.close();
	}
	
	/**Quick check for interactions file for mockDruggability test
	 * @throws IOException 
	 * 
	 */
	@Test
	public void testCheckInteractionsFile() throws IOException{
		FileUtility fileUt = new FileUtility();
		fileUt.setInput("Druggability_Interactions_07.20.16_V4.txt");
		
		
		
		String[] headers = fileUt.readLine().split("\t");
		int lineCounter = 0;
		String line = null;
		Set<String> uniqueDrugSet = new HashSet<String>();
		while ((line = fileUt.readLine()) != null){
			String[] tokens = line.split("\t", -1);
			String drug= tokens[0];
			//count unique number of drugs
			uniqueDrugSet.add(drug);
			
		}
		System.out.println("Num unique drugs: " + uniqueDrugSet.size());
		
	}
	
	@Test
	public void testGetInteractionsEvidenceForPathwayFile() throws IOException{
		//interactions file
	    FileUtility fileUt = new FileUtility();
	    fileUt.setInput("Druggability_Interactions_07.20.16_V4.txt");
	    
	    FileUtility hits = new FileUtility();
	    //hits.setInput("results/NOTCHPathwaysWithHitEntities_07.29.16.txt");
	    hits.setInput("results/CellCyclePathwaysWithHitEntities_07.29.16.txt");
	    
	    //output file for all evidence
	    //PrintStream rs = new PrintStream("results/NOTCHEntitiesEvidence_07.29.16.txt");
	    PrintStream rs = new PrintStream("results/CellCycleEntitiesEvidence_07.29.16.txt");
	    rs.println("Drug" + "\t" + "Target_Name" + "\t" + "Target_UniProt" + "\t"+ "Database" + "\t"+ "Reference"+ "\t"+"Assay_Type"+ "\t"+"Assay_Value" + "\t"+"Level");		
	    
	    //notch or cell cycle pathways, has entities
	    //get set of these
	    String line;
	    Set<String> entitySet = new HashSet<String>();
	    hits.readLine();
	    while ((line = hits.readLine()) != null){
			String[] tokens = line.split("\t", -1);
			String pathwayId = tokens[0];
			String pathwayName=tokens[1];
			String numHits = tokens[2];
			String[] entities = tokens[3].split(";");
			
			//split entities
			for (String entity: entities){
				entitySet.add(entity);
			}
	    }
	    System.out.println("Num total entities: "+ entitySet.size());
//	    for (String entity: entitySet){
//	    	System.out.println(entity);
//	    }
	    
	    //now iterate through interactions file
	    //if we match on entity, then grab line and print out
	    String line2;
	    while ((line2 = fileUt.readLine()) != null){
			String[] tokens = line2.split("\t", -1);
			//just grab uniprot
			String uniprot = tokens[2];//3 is uniprot
			//System.out.println(uniprot);
			
			//check for match
			//if match, then print out
			for (String entity: entitySet){
				if (entity.equals(uniprot)){
					rs.println(line2);//print whole line?
				}
			}
			
			
			
	    }
	    
	    
	    
	}
	
	/**For a pathway, get all interactions targeting drugs in that 
	 * pathway.
	 * @throws IOException 
	 * 
	 */
	@Test
	public void testGetInteractionsForPathwayFile() throws IOException{
		
		//pathway results file
		FileUtility pathwayFile = new FileUtility();
		pathwayFile.setInput("results/Pathways_Levels123_07.27.16.csv");
		
		//output file
		//PrintStream rs = new PrintStream("results/CellCyclePathwaysWithHitEntities_07.29.16.txt");
		PrintStream rs = new PrintStream("results/NOTCHPathwaysWithHitEntities_07.31.16.txt");
		rs.println("PathwayID" + "\t" + "PathwayName" + "\t" + "NumberHitEntities" + "\t"+ "HitEntities");
		
		
//		
//		Signaling by NOTCH
//		Pre-NOTCH Expression and Processing
//			Pre-NOTCH Processing in the Endoplasmic Reticulum
//			Pre-NOTCH Processing in Golgi
//		Signaling by NOTCH2
//		Signaling by NOTCH3
//		Signaling by NOTCH4
		
		
		//specify pathway here
		//for that pathway, grab all hit uniprots
		//take those uniprots
		//grab all interactions (at all evidence levels)
		//and print out - each has own file?
		Set<String> pathNameSet = new HashSet<String>();
		//NOTCH SIGNALING HIT PATHWAYS
		pathNameSet.add("Signaling by NOTCH");
		pathNameSet.add("Pre-NOTCH Expression and Processing");
		pathNameSet.add("Pre-NOTCH Transcription and Translation"); //added 7/31
		pathNameSet.add("Signaling by NOTCH1");
		pathNameSet.add("Activated NOTCH1 Transmits Signal to the Nucleus");
		pathNameSet.add("NOTCH1 Intracellular Domain Regulates Transcription");
		
		//Cell cycle signaling hit pathways - Gs/1 branch
//		pathNameSet.add("G1/S DNA Damage Checkpoints");
//		pathNameSet.add("p53-Dependent G1/S DNA damage checkpoint");
//		pathNameSet.add("p53-Dependent G1 DNA Damage Response");
//		pathNameSet.add("Stabilization of p53");
//		pathNameSet.add("Autodegradation of the E3 ubiquitin ligase COP1");
//		
		
		//not hit: notch2, 3, 4 and one in pre-notch
		//CELULLAR - 
		
		String[] headers = pathwayFile.readLine().split(",");
		String line = null;
		while ((line = pathwayFile.readLine()) != null){
			String[] tokens = line.split(",", -1);
			String pathwayId = tokens[0];
			String pathwayName=tokens[1];
			pathwayName=pathwayName.substring(1,pathwayName.length()-1);
			System.out.println(pathwayName);
			//String[] hitEntities=tokens[2].split(";");
			String numHit=tokens[2];
			String hitEntities = tokens[12];
			hitEntities=hitEntities.substring(1,hitEntities.length()-1);
			
			//check for our pathways
			for (String pathName: pathNameSet){
				if (pathwayName.equals(pathName)){
					rs.println(pathwayId + "\t" + pathwayName + "\t"+ numHit+"\t"+ hitEntities);
				}
			}
			
		}
		System.out.println();
		
	}
	
	/**
	 * Test method for getFDAApprovalDate()
	 * Uses test cases Ceritnib (only 1 listing in Orange Book) 
	 * and imatinib (16 listings in Orange Book)
	 * both test cases working successful.
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test
	public void testGetFDAApprovalDate() throws IOException, ParseException{
		//test case = ceritinib, working 9/7/16
		Drug drug = new Drug();
		drug.setDrugName("Ceritinib");
		Set<String> synSet = new HashSet<String>();
		synSet.add("LDK378");
		drug.setDrugSynonyms(synSet);
		//test case = imatinib, working 9/7/16 re-do
//		Drug drug = new Drug();
//		drug.setDrugName("imatinib");
//		Set<String> synSet = new HashSet<String>();
//		synSet.add("Gleevec");
//		drug.setDrugSynonyms(synSet);
		
		//test using approval re-do, 9/7/16
		String fdaDate = getFDAApprovalDate(drug);
		
		System.out.println("Date found for drug: " + drug.getDrugName() + " , " + fdaDate);
		
	}
	/**
	 * Method takes drug and returns earliest FDA Approval Date found from 
	 * Orange Book Download file: products.txt.
	 * @param drug
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public String getAppFDAOrangeBook(Drug drug) throws IOException, ParseException{
		FileUtility fileUt = new FileUtility();
		fileUt.setInput("resources/DrugsAtFDA/EOBZIP_2016_07/products.txt");
		fileUt.readLine();//headers
		String line;
		//create set for all dates found
		Set<Date> dateSet = new HashSet<Date>();
		while ((line = fileUt.readLine()) != null){
			//System.out.println(line);
			String[] tokens = line.split("~");
			//here, need to determine what field to use, which approval date?
			String drugIng = tokens[0];
			String tradeName=tokens[2];
			String approvalDate = tokens[9];//do we want the earliest approval date?
			//since there are discon. formulations, 
			//etc
			//if before Jan 1, 1982, set to exact date:
			if (approvalDate.equals("Approved Prior to Jan 1, 1982")){
				approvalDate="January 1, 1982";
			}
			DateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
			Date appDate = dateFormat.parse(approvalDate);
			//check drug name/trade name, method nameIsEquivalent checks name & synonyms
			if (drug.nameIsEquivalent(drugIng) || drug.nameIsEquivalent(tradeName)){
				//add to date set
				dateSet.add(appDate);
			}
			else {
				continue;
			}

		}
		//get earliest approval date from set
		Date earliestDate = findEarliestDate(dateSet);
		//convert back to string
		Format formatter = new SimpleDateFormat("yyyy-MM-dd");
		String finalDate = formatter.format(earliestDate);
		//if matches today's date then set to null, no date was found
		if (finalDate.equals("2016-09-07")){
			finalDate=null;//set to null
		}
		return finalDate;
	}
	/**
	 * Method takes drug and returns earliest FDA Approval Date found from 
	 * Drugs@FDA Download files: Product.txt and RegAction.txt
	 * @param drug
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public String getFDAApprovalDate(Drug drug) throws IOException, ParseException{
		FileUtility fileUt = new FileUtility();
		fileUt.setInput("resources/DrugsAtFDA/drugsatfda/Product.txt");
		
		fileUt.readLine();//headers
		String line;
		//get all application numbers for this drug
		Set<String> applicationNos = new HashSet<String>();
		int skippedCounter=0;
		boolean matchFound=false;
		while ((line = fileUt.readLine()) != null){
			//System.out.println(line);
			String[] tokens = line.split("\t");
			if (tokens.length !=9){
				skippedCounter++;
				continue;
			}
			String applicationNo = tokens[0];
			String drugName = tokens[7];
			String drugIng = tokens[8];
			
			//check match to drug name/trade name
			if (drug.nameIsEquivalent(drugName)|| drug.nameIsEquivalent(drugIng)){
				applicationNos.add(applicationNo);
				matchFound=true;
			}
		}
		//if we found a drug match, look up the earliest N=approval date
		if (matchFound==true){
			//now look up earliest approval date for this set of applications numbers
			Date approvalDate = getFDARegActionDate(applicationNos);

			//convert back to string
			Format formatter = new SimpleDateFormat("yyyy-MM-dd");
			String finalDate = formatter.format(approvalDate);

			return finalDate;
		}
		else{
			return null;
		}
		
	}
	/**
	 * Method takes set of application numbers (for a drug look up) and looks up approval date in
	 * Drugs@FDA RegAction.txt file. 
	 * Returns earliest approval (N) status date. 
	 * @param applicationNo
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public Date getFDARegActionDate(Set<String> applicationNos) throws ParseException, IOException{
		FileUtility fileUt2 = new FileUtility();
		fileUt2.setInput("resources/DrugsAtFDA/drugsatfda/RegActionDate.txt");
		fileUt2.readLine();//headers
		Set<Date> dateSet = new HashSet<Date>();
		
		String line;
		while ((line = fileUt2.readLine()) != null){
			String[] tokens = line.split("\t");
			String applicationNo = tokens[0];
			String approvalDate = tokens[4];
			String action = tokens[5];
			//match on application no
			//look for action = N
			//grab date
			//if more than one action? flag*
			for (String eachAppNo: applicationNos){
				if (applicationNo.equals(eachAppNo)){
					//check for action= N = approved
					if (action.equals("N")){
						//convert string date to Date
						DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
						Date appDate = dateFormat.parse(approvalDate);
						dateSet.add(appDate);
					}
					
				}
			}
			
		}
		//no iterate through dateSet
		//find earliest date, get and return
		Date earliestDate = findEarliestDate(dateSet);//compare to current date?
		
		return earliestDate;
	}
	/**
	 * Method for finding earliest Date object in set of Dates.
	 * @param dateSet
	 * @return
	 * @throws ParseException
	 */
	private Date findEarliestDate(Set<Date> dateSet) throws ParseException {
		String todayDate = "Sep 7, 2016";//should this be auto-today's date?
		DateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
		Date today = dateFormat.parse(todayDate);
		Date earliestDate=today;
		for (Date eachDate: dateSet){
			if (eachDate.before(earliestDate)){
				earliestDate=eachDate;
			}
		}
		return earliestDate;
	}

	/**This method tests persisting all of our sources together to the Druggability Database:
	 * Drug set from NCI Scraper
	 * IUPHAR
	 * DrugBank
	 * TTD
	 * BindingDB
	 * This will eventually be the main method for creating our database.
	 *
	 * Updated July 6, 2017. Fixed set merge issue for interactions (flagged by imatinib/
	 * ddr1). Updated output in folder results_070617
	 * @throws IOException
	 * @throws ParseException 
	 * 
	 * Working here 03/07/21 on Beta V2
	 * -Test run for each of the sections
	 * -start with drug names and thesaurus
	 * -then run through what we have**
	 * -
	 */
	
	@Test
	public void testPersistAll() throws IOException, ParseException{
		//Session currentSession = drugsPersist("drug_sets/testDrugSet.txt", 1, 0, "\t");
		Session currentSession = beta_persistDrugSet("drug_sets/scrapedNCIDrugs_05.11.16.txt", 1, 0, "\t" );
		//retrieve pharm classes, added 10/11/16
		Session currentSessionWithClasses = persistDrugClasses(currentSession);
		
		//iuphar
		Session currentSessionIUPHAR = persistIUPHAR(currentSessionWithClasses);
		System.out.println("Done persisting IUPHAR.");
		//drugbank
		Session currentSessionDrugBank = persistDrugBank(currentSessionIUPHAR);
		System.out.println("Done persisting DrugBank.");
		//ttd
		Session currentSessionTTD = persistTTD(currentSessionDrugBank);
		System.out.println("Done persisting TTD.");
		//bindingDB
		Session currentSessionBindingDB = persistBindingDB(currentSessionTTD);
		System.out.println("Done persisting BindingDB.");
		
		//run check for uniprots and also assign the gene symbol as "target name"
		Session currentSessionTargetsChecked = persistTargetNames(currentSessionBindingDB);

		//OUTPUT INTERACTIONS HERE
		//Drug info file - for EDA
		Set<Drug> drugSet = queryDrugSet(currentSessionBindingDB);
		PrintStream ds = new PrintStream("results_070617/Targetome_DrugInformation_070617.txt");
		
		//PrintStream ds = new PrintStream("resultsV2/DruggabilityV2_Drugs_10.18.16.txt");
		ds.println("Drug" + "\t" +"Approval_Date"+"\t" + "ATC_ClassID" + "\t" + "ATC_ClassName" + "\t" + "ATC_ClassStatus" + "\t"+ "EPC_ClassID" + "\t" + "EPC_ClassName");

		
		//Druggability V2 Interactions - for EDA
		//PrintStream ps = new PrintStream("resultsV2/DruggabilityV2_10.18.16.txt");
		//Updated file - 07/06/17
		PrintStream ps = new PrintStream("results_070617/Targetome_FullEvidence_070617.txt");
		
		ps.println("Drug" +"\t" + "Target_Name" + "\t" + "Target_Type"+ "\t"+ "Target_UniProt" + "\t" + "Target_Species" + "\t"+ "Database" + "\t"+ "Reference"+ "\t"+"Assay_Type"+"\t" + "Assay_Relation"+ "\t"+"Assay_Value" + "\t"+"EvidenceLevel_Assigned");
		
		//for each drug
		for (Drug finalDrug: drugSet){
			String drugName = finalDrug.getDrugName();//get drug name
			//now query database for all drug info and print to file
			queryDatabaseDrug2(currentSessionBindingDB, drugName, ps);
			//also print drug file
			ds.println(finalDrug.getDrugName() + "\t" + finalDrug.getApprovalDate() + "\t" + finalDrug.getAtcClassID() + "\t" + finalDrug.getAtcClassName() + "\t" + finalDrug.getAtcClassStatus()+ "\t"+ finalDrug.getEpcClassID() + "\t" + finalDrug.getEpcClassName());
		}
		
		ps.close();
		ds.close();
		
		
		//close session, should always be LAST SESSION
		currentSessionTargetsChecked.getTransaction().commit();
		currentSessionTargetsChecked.close();
		SessionFactory thisFactory = currentSessionTargetsChecked.getSessionFactory();
		thisFactory.close();
	}
	/** This method checked all the targets of a current session and
	 * converts target.getName() field to official gene name according to uniprot
	 * and keeps listed name stored in synonyms. 
	 * To simplify target names in database and in figures/graphs.
	 * 
	 * @param currentSessionIUPHAR
	 * @return
	 * @throws IOException 
	 */
	public Session persistTargetNames(Session currentSession) throws IOException {
		PrintStream ps = new PrintStream("resources/UniProt/Uniprot_Targets_MappedToHuman_063017.txt");
		
		//load uniprot to gene map
		Map<String, String> uniProtToGeneMap = loadUniProtToGene();
		ps.println("Previous_TargetName" + "\t" + "UniprotID" + "\t" + "New_TargetName");
		
		//query our target set
		Set<Target> allTargets = queryTargetSet(currentSession);
		//now update our target names
		for (Target target: allTargets){
			//skip anything not human
//			if (target.getTargetSpecies()==null || !target.getTargetSpecies().equals("Homo sapiens")){
//				ps.println("No species information, checking uniprot map.");
//				continue;
//			}
			//move prev target name ->synonym list
			String targetName = target.getTargetName();
			Set<String> targetSyns = target.getTargetSynonyms();
			targetSyns.add(targetName);
			
			//now re-set the targetname field 
			String newTargetName = uniProtToGeneMap.get(target.getUniprotID());
			if (newTargetName!=null){//if we found mapping
				target.setTargetName(newTargetName);
				if (target.getTargetSpecies()==null || !target.getTargetSpecies().equals("Homo sapiens")  ){
					ps.println(targetName + "\t" + target.getUniprotID()+ "\t" + newTargetName);
				}
				target.setTargetSpecies("Homo sapiens");
			}
			else{
				continue;//cannot map to human uniprot, leave as it, will be excluded in R analysis
			}
			
			
			currentSession.save(target);//save
		}
		ps.close();
		return currentSession;
	}

	/**
	 * Method loads map of uniprot->gene name from uniprot ID mapping file
	 * downloaded from UniProt website 10/14/16
	 * @return
	 * @throws IOException
	 */
	private Map<String, String> loadUniProtToGene() throws IOException{
		Map<String, String> uniProtToGene = new HashMap<String, String>();
		
		
		
		FileUtility fileUt = new FileUtility();
		fileUt.setInput("resources/UniProt/HUMAN_9606_idmapping.dat");
		String line;
		System.out.println("Start loading uniprot map.");
		while ((line = fileUt.readLine()) != null){
			String[] tokens = line.split("\t");
			String uniprot = tokens[0];
			String idField = tokens[1];
			String value = tokens[2];
			//check for gene_name file, load to map
			if (idField.equals("Gene_Name")){
				uniProtToGene.put(uniprot, value);
			}
		}

		System.out.println("Finished loading uniprot map.");
		return uniProtToGene;
		
	}
	
	@Test
	public void testLoadUniProtToGene() throws IOException{
		Map<String, String> uniprotMap = loadUniProtToGene();
		String uniprot = "Q9UM73";
		String geneName = uniprotMap.get(uniprot);
		System.out.println("Checking uniprot: " + uniprot);
		System.out.println("Gene name: " + geneName);
	}
	
	

	/**
	 * Method retrieves drug classes from pre-loaded files. Drug classes
	 * retrieved from RxClass using RESTFUL API, see RxClassParser.
	 * @param currentSession
	 * @return
	 * @throws IOException 
	 */
	public Session persistDrugClasses(Session currentSession) throws IOException {
		//query drug set from session
		Set<Drug> drugSet = queryDrugSet(currentSession);
		//open drug class file
		FileUtility fileUt = new FileUtility();
		fileUt.setInput("resources/RxClass/DrugToClass_10.18.16.txt");
		String line = null;
		while ((line = fileUt.readLine()) != null){
			String[] tokens = line.split("\t");
			String drugName = tokens[0];
			String atcClassID = tokens[1];
			String atcClassName = tokens[2];
			String epcClassID = tokens[3];
			String epcClassName = tokens[4];
			//loop through drug set
			for (Drug drug: drugSet){
				//if match then set atc/epc fields for the drug
				if (drug.nameIsEquivalent(drugName)){
					drug.setAtcClassID(atcClassID);
					drug.setAtcClassName(atcClassName);
					drug.setEpcClassID(epcClassID);
					drug.setEpcClassName(epcClassName);
					//set atc class status
					if (atcClassID.equals("null")){
						drug=getAtcClassIDTemp(drug);//will set status to pending here
					}
					else{//if classID was found this pass->approved atc id status
						drug.setAtcClassStatus("Approved");
					}
					currentSession.save(drug);//don't forget to save
				}
			}
		}
	
		return currentSession;
	}

	private Drug getAtcClassIDTemp(Drug drug) throws IOException {
		//now check our manuall added file of ATC temp names
		//compiled from WHO ATC/DDD classification PDF
		FileUtility fileUt2 = new FileUtility();
		fileUt2.setInput("resources/RxClass/Drug_ATC_PendingApproval.txt");
		String line = null;
		while ((line = fileUt2.readLine()) != null){
			String[] tokens = line.split("\t");
			String drugName = tokens[0];
			String atcClassIDPending = tokens[1];
			String atcClassNamePending = tokens[2];
			//chck for match
				if (drug.nameIsEquivalent(drugName)){
					drug.setAtcClassID(atcClassIDPending);
					drug.setAtcClassName(atcClassNamePending);
					drug.setAtcClassStatus("Pending");
				}
			}
		
		fileUt2.close();
		return drug;
	}

	/**
	 * Method queries all interactions in our druggability database.
	 * Testing 9/26/16 to connect our database V2
	 * @throws FileNotFoundException 
	 */
	@Test
	public void testGetAllInteractions() throws FileNotFoundException{
//		//open new session here to our database
//		//added Mon 9/26/16
//		
//		//how do we get session factory?
//		Session mySession= sessionFactory.openSession();//what we need to call
		
//				
//				
//				
//		//can we use this to access our database?
//		//String configFileName = "resources/drugHibernateV2.cfg.xml";
//		//Session currentSession = DruggabilityHibernatePersist.DruggabilityPersist(configFileName);
//		Set<Drug> testDrugs = queryDrugSet(currentSession);
//		System.out.println("Testing accessing database");
//		for (Drug drug:testDrugs){
//			System.out.println(drug.getDrugName());
//			System.out.println(drug.getApprovalDate());
//		}
//		
//		//currentSession.beginTransaction();
//		
//		
//		
//		//save to resultsV2 folder
//		//PrintStream ps = new PrintStream("resultsV2/Druggability_IUPHAR_09.26.16.txt");
//		//ps.println("Drug" + "\t" + "Target_Name" + "\t" + "Target_UniProt" + "\t" + "Target_Species");
//		//need querying method here
//		
//		
//		//		Set<Drug> drugSet = queryDrugSet(currentSession);
////		//for each drug
////		for (Drug finalDrug: drugSet){
////			String drugName = finalDrug.getDrugName();//get drug name
////			queryDatabaseDrug(currentSession, drugName, ps);
////		}
//		
//		//ps.close();
//		currentSession.close();
//		
	}
//	
	/**
	 * Method queries database (takes current session) and gets all info on
	 * imatinib.
	 * @param currentSession
	 */

	public void queryDatabaseDrug(Session currentSession, String drugName, PrintStream rs){
		
		Set<Drug> drugSet = queryDrugSet(currentSession);
		//get database info
		Set<Target> targetSet = queryTargetSet(currentSession);
		Set<Interaction> interactionSet = queryInteractionSet(currentSession);
		Set<LitEvidence> literatureSet = queryLitEvidenceSet(currentSession);
		Set<ExpEvidence> experimentalSet = queryExpEvidenceSet(currentSession);
		Set<Source> sourceSet= querySourceSet(currentSession);
		Set<DatabaseRef> databaseSet = queryDatabaseSet(currentSession);
		
		System.out.println("Begin querying test.");
		for (Drug drug: drugSet){
			//print drug info
			if (drug.nameIsEquivalent(drugName)){
				System.out.println("Drug found as: " + drug.getDrugName());
//				System.out.println("Drug synonyms:");
//				for (String syn: drug.getDrugSynonyms()){
//					System.out.println(syn);
//				}
				
				//get all interactions involving drug
				for (Interaction interaction: interactionSet){
					if (interaction.getIntDrug().isEquivalent(drug)){
						System.out.println("Interaction: " + interaction.getIntDrug().getDrugName() + " and " + interaction.getIntTarget().getTargetName());
						
						//get sources and info
						if (interaction.getInteractionSourceSet()!=null){
							Set<Source> intSourceSet = interaction.getInteractionSourceSet();
							System.out.println("Num Interaction ONLY Evidence: " + intSourceSet.size());
							System.out.println("***INTERACTION ONLY EVIDENCE***");
							
							//we need to print out interaction for each source
							//
							for (Source source:intSourceSet){
								String litPubMed = source.getSourceLiterature().getPubMedID();
								String databaseRef = source.getSourceDatabase().getDatabaseName();
								System.out.println("PubMed ID: " + litPubMed);
								System.out.println("Database: " + databaseRef);
								
								String level;
								if (litPubMed.equals("null") || litPubMed==null || litPubMed.equals("NA_TTD") || litPubMed.equals("NA_BindingDB") || litPubMed.equals("NA_IUPHAR") || litPubMed.equals("NA_DrugBank")){
									level = "I";//LEVEL I
								}
								else{
									level = "II";//LEVEL II
								}
								//here we print to our file
								//drug/target/target uniprot/database/ref/assayTYPE/assayvalue/level
								//assaytype = NA
								//assayValue = NA
								rs.println(drug.getDrugName() + "\t"+ interaction.getIntTarget().getTargetName()+ "\t"+ interaction.getIntTarget().getUniprotID()+ "\t" + databaseRef + "\t" + litPubMed + "\t" + "NA" + "\t" + "NA"+ "\t" + level);
							}
						}
						//get exp sources and info
						if (interaction.getExpEvidenceSet()!= null){
							Set<ExpEvidence> intExpEvidenceSet= interaction.getExpEvidenceSet();
							System.out.println("Num Experiemental Evidence: " + intExpEvidenceSet.size());
							System.out.println("***EXPERIMENTAL EVIDENCE***");
							for (ExpEvidence exp: intExpEvidenceSet){
								String assayType = exp.getAssayType();
								String assayValue = exp.getAssayValueMedian();//just valueMed for now
								System.out.println("Assay Type: " + assayType);
								System.out.println("Val low: " +exp.getAssayValueLow());
								System.out.println("Val med: " +exp.getAssayValueMedian());
								System.out.println("Val high: " +exp.getAssayValueHigh());
								System.out.println("Units: " +exp.getAssayUnits());
								System.out.println("Species: " +exp.getAssaySpecies());
								//INCLUDE LOOP HERE FOR GOING THROUGH EXP SOURCE SET!
								System.out.println("Exp Source information: ");
								for (Source source:exp.getExpSourceSet()){
									String expLit = source.getSourceLiterature().getPubMedID();
									String expdatabase =  source.getSourceDatabase().getDatabaseName();
									System.out.println("Exp Source Lit: " + expLit);
									System.out.println("Exp Source Database: " + expdatabase);
								
									String expLevel;
									if ( expLit.equals("NA_BindingDB") || expLit.equals("NA_IUPHAR") ){
										expLevel = "I";//LEVEL I
									}
									else{//all else LEVEL III for now
										expLevel = "III";//LEVEL III
									}
									rs.println(drug.getDrugName() + "\t"+ interaction.getIntTarget().getTargetName()+ "\t"+ interaction.getIntTarget().getUniprotID()+ "\t" + expdatabase + "\t" + expLit + "\t" + assayType+ "\t" + assayValue+ "\t" + expLevel);
								}
								
							}
						}
						else{
							continue;
						}
					}
					
				}
				
				
			}
		}
		System.out.println("End querying test.");
			
			
	}
	
	/**
	 * Method queries database (takes current session) and gets all info on
	 * imatinib.
	 * For Druggability V2, working Mon 9/26/16
	 * @param currentSession
	 */
	public void queryDatabaseDrug2(Session currentSession, String drugName, PrintStream rs){
		//query database info
		Set<Drug> drugSet = queryDrugSet(currentSession);
		Set<Interaction> interactionSet = queryInteractionSet(currentSession);
		
		System.out.println("Begin querying test.");
		for (Drug drug: drugSet){
			//print drug info
			if (drug.nameIsEquivalent(drugName)){
				System.out.println("Drug found as: " + drug.getDrugName());

				//get all interactions involving drug
				for (Interaction interaction: interactionSet){
					if (interaction.getIntDrug().isEquivalent(drug)){
						System.out.println("Interaction: " + interaction.getIntDrug().getDrugName() + " and " + interaction.getIntTarget().getTargetName());
						
						//get sources and info
						if (interaction.getInteractionSourceSet()!=null){
							Set<Source> intSourceSet = interaction.getInteractionSourceSet();
							System.out.println("Num Interaction ONLY Evidence: " + intSourceSet.size());
							System.out.println("***INTERACTION ONLY EVIDENCE***");
							
							//we need to print out interaction for each source
							//
							for (Source source:intSourceSet){
								String litPubMed = source.getSourceLiterature().getPubMedID();
								String databaseRef = source.getSourceDatabase().getDatabaseName();
								System.out.println("PubMed ID: " + litPubMed);
								System.out.println("Database: " + databaseRef);
								
								String level;
								if (litPubMed.equals("null") || litPubMed==null || litPubMed.equals("NA_TTD") || litPubMed.equals("NA_BindingDB") || litPubMed.equals("NA_IUPHAR") || litPubMed.equals("NA_DrugBank")){
									level = "I";//LEVEL I
								}
								else{
									level = "II";//LEVEL II
								}
								//here we print to our file
								//drug/target/target uniprot/targetSpecies/database/ref/assayTYPE/assayvalue/level
								//assaytype = NA
								//assayRelation=NA
								//assayValue = NA
								rs.println(drug.getDrugName()+ "\t"+ interaction.getIntTarget().getTargetName()+ "\t"+ interaction.getIntTarget().getTargetType()+"\t"+ interaction.getIntTarget().getUniprotID()+ "\t" + interaction.getIntTarget().getTargetSpecies()+ "\t" + databaseRef + "\t" + litPubMed + "\t" + "NA" + "\t" + "NA"+ "\t" + "NA" +"\t" + level);
							}
						}
						//get exp sources and info
						if (interaction.getExpEvidenceSet()!= null){
							Set<ExpEvidence> intExpEvidenceSet= interaction.getExpEvidenceSet();
							System.out.println("Num Experiemental Evidence: " + intExpEvidenceSet.size());
							System.out.println("***EXPERIMENTAL EVIDENCE***");
							for (ExpEvidence exp: intExpEvidenceSet){
								String assayType = exp.getAssayType();
								String assayValue = exp.getAssayValueMedian();//just valueMed for now
								String assayRelation=exp.getAssayRelation();
								System.out.println("Assay Type: " + assayType);
								System.out.println("Val low: " +exp.getAssayValueLow());
								System.out.println("Val med: " +exp.getAssayValueMedian());
								System.out.println("Val high: " +exp.getAssayValueHigh());
								System.out.println("Units: " +exp.getAssayUnits());
								System.out.println("Species: " +exp.getAssaySpecies());
								//INCLUDE LOOP HERE FOR GOING THROUGH EXP SOURCE SET!
								System.out.println("Exp Source information: ");
								for (Source source:exp.getExpSourceSet()){
									String expLit = source.getSourceLiterature().getPubMedID();
									String expdatabase =  source.getSourceDatabase().getDatabaseName();
									System.out.println("Exp Source Lit: " + expLit);
									System.out.println("Exp Source Database: " + expdatabase);
								
									String expLevel;
									if ( expLit.equals("NA_BindingDB") || expLit.equals("NA_IUPHAR") ){
										expLevel = "I";//LEVEL I
									}
									else{//all else LEVEL III for now
										expLevel = "III";//LEVEL III
									}
									//added assay relation to be explicit since
									//we reformatted bindingDB and iuphar
									rs.println(drug.getDrugName() + "\t"+ interaction.getIntTarget().getTargetName()+ "\t"+ interaction.getIntTarget().getTargetType() + "\t"+interaction.getIntTarget().getUniprotID()+ "\t" + interaction.getIntTarget().getTargetSpecies() + "\t" + expdatabase + "\t" + expLit + "\t" + assayType+ "\t" + assayRelation + "\t"+ assayValue+ "\t" + expLevel);
								}
								
							}
						}
						else{
							continue;
						}
					}
					
				}
				
				
			}
		}
		System.out.println("End querying test.");
			
			
	}
	
	/**
	 * Method queries database (takes current session) and gets all info on
	 * single drug, appends to rs.
	 * for beatAML drug set, added 6/16/17
	 * -note file contains additional column - BeatAMLDrug_ListedAs based on listing on inhibtor panel
	 * @param currentSession
	 */
	public Drug queryDatabaseDrug3(Session currentSession, String drugName, PrintStream rs){
		boolean drugInCancerTargetome= false;
		
		//query database info
		Set<Drug> drugSet = queryDrugSet(currentSession);
		Set<Interaction> interactionSet = queryInteractionSet(currentSession);
		
//		System.out.println("Begin querying test.");
		for (Drug drug: drugSet){
			//print drug info
			if (drug.nameIsEquivalent(drugName)){
				drugInCancerTargetome=true;
//				System.out.println("Found in Cancer Targetome: " + drug.getDrugName());

				//get all interactions involving drug
				for (Interaction interaction: interactionSet){
					if (interaction.getIntDrug().isEquivalent(drug)){
	//					System.out.println("Interaction: " + interaction.getIntDrug().getDrugName() + " and " + interaction.getIntTarget().getTargetName());
						
						//get sources and info
						if (interaction.getInteractionSourceSet()!=null){
							Set<Source> intSourceSet = interaction.getInteractionSourceSet();
	//						System.out.println("Num Interaction ONLY Evidence: " + intSourceSet.size());
	//						System.out.println("***INTERACTION ONLY EVIDENCE***");
	//						
							//we need to print out interaction for each source
							//
							for (Source source:intSourceSet){
								String litPubMed = source.getSourceLiterature().getPubMedID();
								String databaseRef = source.getSourceDatabase().getDatabaseName();
	//							System.out.println("PubMed ID: " + litPubMed);
	//							System.out.println("Database: " + databaseRef);
								
								String level;
								if (litPubMed.equals("null") || litPubMed==null || litPubMed.equals("NA_TTD") || litPubMed.equals("NA_BindingDB") || litPubMed.equals("NA_IUPHAR") || litPubMed.equals("NA_DrugBank")){
									level = "I";//LEVEL I
								}
								else{
									level = "II";//LEVEL II
								}
								//here we print to our file
								//drug/target/target uniprot/targetSpecies/database/ref/assayTYPE/assayvalue/level
								//assaytype = NA
								//assayRelation=NA
								//assayValue = NA
								rs.println(drugName + "\t"+ drug.getDrugName()+ "\t"+ interaction.getIntTarget().getTargetName()+ "\t"+ interaction.getIntTarget().getTargetType()+"\t"+ interaction.getIntTarget().getUniprotID()+ "\t" + interaction.getIntTarget().getTargetSpecies()+ "\t" + databaseRef + "\t" + litPubMed + "\t" + "NA" + "\t" + "NA"+ "\t" + "NA" +"\t" + level);
							}
						}
						//get exp sources and info
						if (interaction.getExpEvidenceSet()!= null){
							Set<ExpEvidence> intExpEvidenceSet= interaction.getExpEvidenceSet();
	//						System.out.println("Num Experiemental Evidence: " + intExpEvidenceSet.size());
	//						System.out.println("***EXPERIMENTAL EVIDENCE***");
							for (ExpEvidence exp: intExpEvidenceSet){
								String assayType = exp.getAssayType();
								String assayValue = exp.getAssayValueMedian();//just valueMed for now
								String assayRelation=exp.getAssayRelation();
//								System.out.println("Assay Type: " + assayType);
//								System.out.println("Val low: " +exp.getAssayValueLow());
//								System.out.println("Val med: " +exp.getAssayValueMedian());
//								System.out.println("Val high: " +exp.getAssayValueHigh());
//								System.out.println("Units: " +exp.getAssayUnits());
//								System.out.println("Species: " +exp.getAssaySpecies());
//								//INCLUDE LOOP HERE FOR GOING THROUGH EXP SOURCE SET!
//								System.out.println("Exp Source information: ");
								for (Source source:exp.getExpSourceSet()){
									String expLit = source.getSourceLiterature().getPubMedID();
									String expdatabase =  source.getSourceDatabase().getDatabaseName();
//									System.out.println("Exp Source Lit: " + expLit);
//									System.out.println("Exp Source Database: " + expdatabase);
//								
									String expLevel;
									if ( expLit.equals("NA_BindingDB") || expLit.equals("NA_IUPHAR") ){
										expLevel = "I";//LEVEL I
									}
									else{//all else LEVEL III for now
										expLevel = "III";//LEVEL III
									}
									//added assay relation to be explicit since
									//we reformatted bindingDB and iuphar
									rs.println(drugName + "\t"+drug.getDrugName() + "\t"+ interaction.getIntTarget().getTargetName()+ "\t"+ interaction.getIntTarget().getTargetType() + "\t"+interaction.getIntTarget().getUniprotID()+ "\t" + interaction.getIntTarget().getTargetSpecies() + "\t" + expdatabase + "\t" + expLit + "\t" + assayType+ "\t" + assayRelation + "\t"+ assayValue+ "\t" + expLevel);
								}
								
							}
						}
						else{
							continue;
						}
					}
					
				}
				
			return drug;
			}
		}
//		System.out.println("End querying test.");
		return null;
			
			
	}
	
	
	/**
	 * This method is for parsing our output Interactions file:
	 * Druggability_Interactions_07.20.16_V4.txt
	 * Used for PSB Paper draft on 7/20/16
	 * @throws IOException
	 */
	@Test
	public void testParseInteractionFile() throws IOException{
		FileUtility fullData = new FileUtility();
		fullData.setInput("Druggability_Interactions_07.20.16_V4.txt");
		
		fullData.readLine();//headers
		String line;
		while ((line = fullData.readLine()) != null){
			//count number unique drugs we have data for
			
			//for each drug
			//get # unique targets
			//all levels
			//then skip I - just get II and III
			//then skip I and II - just get III
		}
		
		PrintStream rs = new PrintStream("DrugToNumTargets.txt");
		//print out drug to num targets for each level (1.2.3. = levels I, II, III)
		rs.println("Drug" + "\t" + "Targets_1.2.3" + "\t" + "Targets_2.3" + "\t"+ "Targets_3");
		
		
	}
	
	/**
	 * Test method for loading NCI drugs, synonyms, 
	 * Adding and reconciling BetaV2 drugs and synonyms
	 * And performing quick coverage check across our resources
	 * + updated DrugBank file, new Sorger database (Sophia parsed)
	 *
	 * @throws IOException
	 */
	@Test
	public void test_beta_LoadingDrugsAndCoverageCheck() throws IOException{
		//load drug set from file
		Set<String> testDrugs = loadDrugSetFromFile("resources/drug_sets/scrapedNCIDrugs_05.11.16.txt", 1, 0, "\t" );
		System.out.println("NCI Drug set loaded; number of drugs: " + testDrugs.size());
		
		//load NCI synonyms
		Map<String, Set<String>> drugSyns = loadNCISynonyms(testDrugs, "resources/NCIThesaurus_v04.25.16.txt");
		System.out.println("NCI thesaurus synonym deck loaded; map key set size: " + drugSyns.keySet().size());
		
//		//open hibernate session and transaction
//		String configFileName = "resources/drugHibernateMock.cfg.xml";
//		Session currentSession = DruggabilityHibernatePersist.openSession(configFileName);
//		currentSession.beginTransaction();
		
		Set<Drug> fullDrugSet = createDrugSetFromSynonymDeck(drugSyns);
		
		System.out.println("Total number of drugs: " + fullDrugSet.size()); //173 from our targetome V1 count
		
		//load sophia's drug-> synonym deck
		//then load into Drugs, then reconcile the two.
		Set<Drug> betaDrugSet= loadBetaDrugSets();
		System.out.println("Total number of drugs: " + betaDrugSet.size()); //430 in beta (includes dup formulations)
		
		//reconcile new set with old set
		//add method heree
		//OUTPUT ONE DRUG SET
		
		System.out.println("Reconciling V1 Drug and Beta Drug Sets");
		//initialize resolved set of drugs
		Set<Drug> reconciledSet = reconcileDrugSets(fullDrugSet, betaDrugSet);
		//final set number; note will include dup formulations for now
		System.out.println("Reconciled Drug Set " + reconciledSet.size()); 
		
		//remove vaccines
		Set<Drug> removeVaccinesFromDrugSet = removeVaccinesFromDrugSets(reconciledSet);
		System.out.println("Reconciled Drug Set - Remove Vaccines " + removeVaccinesFromDrugSet.size()); 
				
		//reconcile formulations*
		Set<Drug> reconcileFormulations = reconcileFormulationsFromDrugSets(removeVaccinesFromDrugSet);
		System.out.println("Reconciled Drug Set - Remove Formulations " + reconcileFormulations.size()); 
		
		
		//QUICK CHECKS COVERAGE
		//call the beta_checkCoverage methods for each database here
		//IUPHAR
		Map<Drug, String> drugToIUPHARCoverage = beta_checkCoverageIUPHAR(removeVaccinesFromDrugSet);
		
		//DrugBank
		Map<Drug, String> drugToDrugBankCoverage = beta_checkCoverageDrugBank(removeVaccinesFromDrugSet);
		
		//Sorger; new in Beta version
		Map<Drug, String> drugToKinaseResourceCoverage = beta_checkCoverageKinaseResource(removeVaccinesFromDrugSet);
		
		
		//BindingDB
		Map<Drug, String> drugToBindingDBCoverage = beta_checkCoverageBindingDB(removeVaccinesFromDrugSet);
				
		//TTD
		Map<Drug, String> drugToTTDCoverage = beta_checkCoverageTTD(removeVaccinesFromDrugSet);
		
		
		//output drugs and drug synonyms LONG format
		//start quick script R for coverage/ stats on drug/ synonym deck
		//for 03/08/21 meeting//output to file so we can keep track- done
		PrintStream ps = new PrintStream("results_beta_V2/RunningDrugDeck_V1_AddBetaV2_CheckDrugCoverage_RemoveVaccines_032121.tsv");
		ps.println("Drug" + "\t" +"IUPHAR" + "\t"+"DrugBank" + "\t"+"Sorger_KinaseResourcee" + "\t"+"BindingDB" + "\t"+"TTD" + "\t" + "Synonym_Deck_Size + \t" + "Synonyms ");
		for (Drug eachDrug: removeVaccinesFromDrugSet) {
			//System.out.println("Checking drug: " + drugName);
			ps.print(eachDrug.getDrugName() + "\t");

			//IUPHAR ANNOTATION
			String iuphar = "No";
			if(drugToIUPHARCoverage.get(eachDrug)!=null) {
				iuphar = drugToIUPHARCoverage.get(eachDrug);
			}
			ps.print(iuphar + "\t");

			//DRUGBANK ANNOTATION
			String drugbank = "No";
			if(drugToDrugBankCoverage.get(eachDrug)!=null) {
				drugbank = drugToDrugBankCoverage.get(eachDrug);
			}
			ps.print(drugbank + "\t");

			//SORGER KINASE RESOURCE ANNOTATION
			String kinaseResource = "No";
			if(drugToKinaseResourceCoverage.get(eachDrug)!=null) {
				kinaseResource = drugToKinaseResourceCoverage.get(eachDrug);
			}
			ps.print(kinaseResource + "\t");
			
			//BINDINGDB ANNOTATION
			String BindingDBResource = "No";
			if(drugToBindingDBCoverage.get(eachDrug)!=null) {
				BindingDBResource = drugToBindingDBCoverage.get(eachDrug);
			}
			ps.print(BindingDBResource + "\t");

			//TTD ANNOTATION
			String TTDResource = "No";
			if(drugToTTDCoverage.get(eachDrug)!=null) {
				TTDResource = drugToTTDCoverage.get(eachDrug);
			}
			ps.print(TTDResource + "\t");


			//SYNONYM
			if(eachDrug.getDrugSynonyms()!=null) {
				ps.print(eachDrug.getDrugSynonyms().size() + "\t");
				for (String synonym: eachDrug.getDrugSynonyms()) {
					//System.out.println("Synonym deck size: " + allSynonyms.size());
					ps.print(synonym + "|");

				}
			}
			ps.println();
		}
		ps.close();
		
//		currentSession.getTransaction().commit();
//		currentSession.close();
//		SessionFactory thisFactory = currentSession.getSessionFactory();
//		thisFactory.close();
		
	}
	/**
	 * Test method to load drug target interactions for our beta set
	 * 
	 * 03/08/21
	 * TODO: this will be updated to include persist() calls with hibernate
	 * currently it is just to get coverage estimates
	 */
	@Test
	public void test_Beta_LoadDrugTargeteInteractions() {
		
		//load from our drug sets
		Set<Drug> reconciledSet = new HashSet<Drug>(); //load this from previous
		
		
		
		//then spin through each database
		//IUPHAR - V1
		//then drugbank - new
		
		//iuphar
		Session currentSessionIUPHAR = persistIUPHAR(currentSessionWithClasses);
		System.out.println("Done persisting IUPHAR.");
		//drugbank
		Session currentSessionDrugBank = persistDrugBank(currentSessionIUPHAR);
		System.out.println("Done persisting DrugBank.");
	
	}

	/**Method to iterate through 2 Drug sets; and reconcile them;
	 * Adds any new drugs in 2nd set to the first set; updates synonym deck
	 * 
	 * Returns final resolved Drug Set
	 * @param fullDrugSet
	 * @param betaDrugSet
	 * @return
	 */
	private Set<Drug> reconcileDrugSets(Set<Drug> fullDrugSet, Set<Drug> betaDrugSet) {
		Set<Drug> reconciledSet = new HashSet<Drug>();
		//iterate through each set
		
		//FIRST PASS, ADD drugsV1 to reconciled set
		//AND PICK UP ANY NEW SYNONYMS FROM THE BETA SET

		System.out.println("FIRST PASS:");
		for(Drug drugV1: fullDrugSet) {
			System.out.println("DrugV1: " + drugV1.getDrugName());
			//first add to our reconciled set
			reconciledSet.add(drugV1);
			
			for (Drug drugBeta: betaDrugSet) {
				System.out.println("DrugBeta: " + drugBeta.getDrugName());
				
				//now compare - any additional drug name info in our beta set?
				if(drugBeta.isEquivalent(drugV1)) {//use our existing method to compare 2 Drug objects; nice
					System.out.println("Drug match in Beta Set found, grab synonyms");

					//then take drug name and synonyms from beta and add to drug V1 information
					//TODO: can make this less redundant
					if (drugV1.getDrugSynonyms() ==null) {
						Set<String> drugV1Synonyms = new HashSet<String>();
						drugV1Synonyms.add(drugBeta.getDrugName()); //first commit beta drug name
						for(String drugBetaSynonym:drugBeta.getDrugSynonyms() ) {
							drugV1Synonyms.add(drugBetaSynonym);//now commit each beta drug synonym as well
						}
					}
					else {
						Set<String> drugV1Synonyms =  drugV1.getDrugSynonyms();
						drugV1Synonyms.add(drugBeta.getDrugName()); //first commit beta drug name
						for(String drugBetaSynonym:drugBeta.getDrugSynonyms() ) {
							drugV1Synonyms.add(drugBetaSynonym);//now commit each beta drug synonym as well
						}
					}

				}
			}//end for drugBeta	
			System.out.println("Finished beta drug loop for drugV1: " + drugV1.getDrugName());
		}//end for drugV1
		//after this pass, now all of V1 have been added to reconciledSet plus they have extended synonym decks
		//from our beta set as appropriate
		
		//SECOND PASS, add beta drugs to reconciled set
		//this lets us go through and add (any beta drugs we don't have) into our reconciled set
		System.out.println("SECOND PASS");
		Set<Drug> flagBetaDrugsToAdd = new HashSet<Drug>();
		//boolean foundInReconciledSet = false;
		
		for (Drug drugBetaSecondPass: betaDrugSet) {
			boolean foundInReconciledSet = false;
			System.out.println("DrugBeta: " + drugBetaSecondPass.getDrugName());
			
		
			for(Drug drugInReconciledSet: reconciledSet) {
				System.out.println("DrugReconciled: " + drugInReconciledSet.getDrugName());
				if(drugBetaSecondPass.isEquivalent(drugInReconciledSet)) {
					System.out.println("beta drug found in reconciled set already; set FLAG to TRUE, break");
					foundInReconciledSet = true; //then PASS, we already have it * DO WE NEED SOME KIND OF INDICATOR HERE?
					//continue;
					//break; // end loop
				}
				//else, just go to nect drug to check 
			}//end loop reconciled set

			
			//if flag is STILL false; then add to reconciled set
			System.out.println("Assess flag statement: " + foundInReconciledSet);
			if(!foundInReconciledSet) {
				System.out.println("finished loop beta vs reconciled, not found: " + drugBetaSecondPass.getDrugName());
				
				flagBetaDrugsToAdd.add(drugBetaSecondPass);
				System.out.println("add to flagBetaDrugs " + drugBetaSecondPass.getDrugName());
			}
			//now that it's added, re-set the flag before we go to next beta drug
			foundInReconciledSet=true;
			
		}
		
		//now spin through the flagged beta drugs and add them to reconciled set at the end
		//this step is need so that we don't modify the reconciled set while iterating through it 
		//in our loop above!
		System.out.println("THIRD PASS:");
		for(Drug flaggedBetaDrug: flagBetaDrugsToAdd) {
			System.out.println("Add drug to final reconciled set: "+ flaggedBetaDrug.getDrugName());
			reconciledSet.add(flaggedBetaDrug);
			
		}

		return reconciledSet;
	}
	
	public Set<Drug> reconcileFormulationsFromDrugSets(Set<Drug> inputDrugSet){
		Set<Drug> cleanedDrugSet = new HashSet<Drug>();
		
		
		
		
		
		return cleanedDrugSet;
	}
	
	public Set<Drug> removeVaccinesFromDrugSets(Set<Drug> inputDrugSet) throws FileNotFoundException{
		
		Set<Drug> cleanedDrugSet = new HashSet<Drug>();
		
		//output file - removed drugs because vaccines
		PrintStream ps = new PrintStream("results_beta_V2/DrugSet_Excluded_Vaccines.tsv");
		ps.println("Drug" +" \t" + "Synonyms");
		
		for (Drug inputDrug: inputDrugSet) {
			String inputDrugName = inputDrug.getDrugName();
			if (inputDrugName.contains("Vaccine") | inputDrugName.contains("vaccine")) {
				//PRINT DRUG NAMe
				ps.print(inputDrugName + "\t");
				
				//PRINT SYNONYM
				if(inputDrug.getDrugSynonyms()!=null) {
					for (String synonym: inputDrug.getDrugSynonyms()) {
						//System.out.println("Synonym deck size: " + allSynonyms.size());
						ps.print(synonym + "|");
					}
				}
				ps.println();
			}
			//else add to cleaned Drug set
			else {
				cleanedDrugSet.add(inputDrug);
			}
					
		}
		
		ps.close();
		return cleanedDrugSet; 
	}
	
	/**
	 * Test method to make sure that if we have a synonym drug in beta test, it gets added correctly to the first test
	 * Check1  - run with just imatinib and gleevec are resolved to 1, synonyms are combined; correect
	 * Check2 - run with imatinib and alectinib in first set, gleevec in second; are other drugs kept correctly
	 *           in the sets?; yes, resolves imatinib/gleevec and keeps alectinib
	 * Check3 - run with imatinib/alectinib/pazopanib in first set, gleevec/alectinib2/crizotinib in second;
	 * 			good, reconciles to 4 drugs total (imatinib= gleevec, alectinib=alectinib2, paz, crizotinib)
	 * 03/07/21
	 */
	@Test
	public void testReconcileDrugSets() {
		Set<Drug> drugSetV1 = new HashSet<Drug>();
		Drug imatinib = new Drug();
		imatinib.setDrugName("imatinib");
		Set<String> imatinibSynonyms = new HashSet<String>();
		imatinibSynonyms.add("IMATINIB");
		imatinibSynonyms.add("gleevec");
		imatinib.setDrugSynonyms(imatinibSynonyms);
		drugSetV1.add(imatinib);
		
		Drug alectinib = new Drug();
		alectinib.setDrugName("Alectinib");
		Set<String> alectinibSynonyms = new HashSet<String>();
		alectinibSynonyms.add("Alectinib1");
		alectinibSynonyms.add("Alectinib2");
		alectinib.setDrugSynonyms(alectinibSynonyms);
		drugSetV1.add(alectinib);
		
		Drug pazopanib = new Drug();
		pazopanib.setDrugName("Pazopanib");
		Set<String> pazopanibSynonyms = new HashSet<String>();
		pazopanibSynonyms.add("Paz");
		pazopanib.setDrugSynonyms(pazopanibSynonyms);
		drugSetV1.add(pazopanib);
		
		Set<Drug> drugSetBeta = new HashSet<Drug>();
		Drug gleevec = new Drug();
		gleevec.setDrugName("gleevec");
		Set<String> gleevecSynonyms = new HashSet<String>();
		gleevecSynonyms.add("STI-571");
		gleevecSynonyms.add("STI571");
		gleevec.setDrugSynonyms(gleevecSynonyms);
		drugSetBeta.add(gleevec);
		
		Drug alectinib2 = new Drug();
		alectinib2.setDrugName("Alectinib2");
		Set<String> alectinib2Synonyms = new HashSet<String>();
		alectinib2Synonyms.add("ALK-22");
		alectinib2Synonyms.add("ALK2");
		alectinib2.setDrugSynonyms(alectinib2Synonyms);
		drugSetBeta.add(alectinib2);
		
		//problem child, additional drugs in beta set are not added at end?
		Drug crizotinib = new Drug();
		crizotinib.setDrugName("Crizotinib");
		Set<String> crizotinibSynonyms = new HashSet<String>();
		crizotinibSynonyms.add("ALK-1");
		crizotinibSynonyms.add("ALK1");
		crizotinib.setDrugSynonyms(crizotinibSynonyms);
		drugSetBeta.add(crizotinib);
		
		//now check what we have in reconciled set
		Set<Drug> reconciledSet = reconcileDrugSets(drugSetV1, drugSetBeta);
		System.out.println("Size of reconciled set: " + reconciledSet.size());
		for(Drug eachDrug: reconciledSet) {
			System.out.print("Drug: " + eachDrug.getDrugName() + "# Synonyms: "+  eachDrug.getDrugSynonyms().size());
			for (String eachSynonym: eachDrug.getDrugSynonyms()) {
				System.out.println("     " +  eachSynonym);
			}	
		}
		
	}
	
	/**
	 * Extracted method - take a deck of drugs -> synonym
	 * and create a Drug set
	 * 
	 * 03/07/21
	 * @param drugSynonymMap
	 * @return
	 */
	private Set<Drug> createDrugSetFromSynonymDeck(Map<String, Set<String>> drugSynonymMap) {
		//initialize running drug set
		Set<Drug> fullDrugSet = new HashSet<Drug>();
		
		//iterate through synonym map, get drugs
		int counter=0;
		
		//FOR EACH DRUG
		for (String drugName: drugSynonymMap.keySet()){
//			if (counter==2){
//				break;
//			}
			System.out.println("Create Drug object for  drug: " + drugName);
			
			//create drug //TODO add session persist here
			Drug currentDrug = new Drug();
			currentDrug.setDrugName(drugName);
			Set<String> currentDrugSyns = drugSynonymMap.get(drugName);
			if(currentDrugSyns!=null) {
				//System.out.println("Num synonyms: " + currentDrugSyns.size());
				currentDrug.setDrugSynonyms(currentDrugSyns);
				
//				for (String syn: currentDrugSyns){
//					System.out.println(syn);
//				}
			}
//			currentSession.save(currentDrug);
			fullDrugSet.add(currentDrug);//update our running set of drugs
			counter++;
		}
		return fullDrugSet;
	}
	
	/***
	 * Method to load BETA drug sets and synonyms into maps (drug name -> synonym deck) then into Drug objects
	 * Returns a Set of Drug objects
	 * Compiled by Sophia Jeng 2021
	 * 
	 * WORKING HERE
	 * TODO - combine these files into 1, and read in in 2 steps - thesaurus and manual
	 * @throws IOException 
	 */
	public Set<Drug> loadBetaDrugSets() throws IOException{
		//load drugs and synonyns for each file from sophia	
		//SMMART thesaurus
		Map<String, Set<String>> drugSynsSMMART = loadDrugsAndSynonymDeckFromFile("resources/beta_v2/nci_thesarus_mmtert_df.txt", true, 0, 1);
		System.out.println("SMMART synonym deck loaded; map key set size: " + drugSynsSMMART.keySet().size());
		
		//now build on existing deck; use overloaded method to pass an existing set AND file
		Map<String, Set<String>> drugSynsSMMART_2 = loadDrugsAndSynonymDeckFromFile(drugSynsSMMART, "resources/beta_v2/nci_thesarus_prime_act_df.txt", true, 0, 1);
		System.out.println("SMMART2 synonym deck added; map key set size: " + drugSynsSMMART_2.keySet().size());
				
		//build on deck, add hnscc
		Map<String, Set<String>> drugSynsAddHNSCC = loadDrugsAndSynonymDeckFromFile(drugSynsSMMART_2, "resources/beta_v2/nci_thesarus_hnscc_df.txt",true, 0, 1);
		System.out.println("HNSCC synonym deck added; map key set size: " + drugSynsAddHNSCC.keySet().size());
		
		//build on deck, add aml
		Map<String, Set<String>> drugSynsAddAML = loadDrugsAndSynonymDeckFromFile(drugSynsAddHNSCC, "resources/beta_v2/nci_thesarus_beataml_df.txt",true, 0, 1);
		System.out.println("HNSCC synonym deck added; map key set size: " + drugSynsAddAML.keySet().size());
		
		//now need to bring in the manual synonyms as well- manual smmart 1
		//NO HEADERS on these files
		Map<String, Set<String>> drugSynsManSMMART = loadDrugsAndSynonymDeckFromFile(drugSynsAddAML, "resources/beta_v2/mmtert_not_found_thesarus_manual.txt",false, 0, 1);
		System.out.println("SMMART manual deck 1 added; map key set size: " + drugSynsManSMMART.keySet().size());
		
		//now need to bring in the manual synonyms as well- manual smmart 1
		Map<String, Set<String>> drugSynsManSMMART2 = loadDrugsAndSynonymDeckFromFile(drugSynsManSMMART, "resources/beta_v2/prime_act_not_found_thesarus_manual.txt", false, 0, 1);
		System.out.println("SMMART manual deck 2 added; map key set size: " + drugSynsManSMMART2.keySet().size());
		
		//now need to bring in the manual synonyms as well- manual smmart 1
		Map<String, Set<String>> drugSynsManSMMART2_HNSCC = loadDrugsAndSynonymDeckFromFile(drugSynsManSMMART2, "resources/beta_v2/hnscc_not_found_not_nat_thesarus_manual.txt", false, 0, 1);
		System.out.println("HNSCC manual deck added; map key set size: " + drugSynsManSMMART2_HNSCC.keySet().size());
		
		//now need to bring in the manual synonyms as well- manual smmart 1
		Map<String, Set<String>> drugSyns_BetaV2 = loadDrugsAndSynonymDeckFromFile(drugSynsManSMMART2_HNSCC, "resources/beta_v2/aml_not_found_thesarus_manual.txt", false, 0, 1);
		System.out.println("AML manual deck added; map key set size: " + drugSyns_BetaV2.keySet().size());
		
		//now add code to load into a drug set
		//take our drug->synonym deck and load into a drug set
		Set<Drug> updatedDrugSet = createDrugSetFromSynonymDeck(drugSyns_BetaV2);
		System.out.println("Total number of drugs: " + updatedDrugSet.size()); //count: w/ some formulation duplicates
	
		return updatedDrugSet;
			
	}
	
	
	/***
	 * Test method for "loadBetaDrugSets" to load updated drug sets and synonyms into Drug objects
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testLoadBetaDrugSets() throws IOException{
		//load drugs and synonyns for each file from sophia	
		//SMMART thesaurus
		Map<String, Set<String>> drugSynsSMMART = loadDrugsAndSynonymDeckFromFile("resources/beta_v2/nci_thesarus_mmtert_df.txt", true, 0, 1);
		System.out.println("SMMART synonym deck loaded; map key set size: " + drugSynsSMMART.keySet().size());

		//now build on existing deck; use overloaded method to pass an existing set AND file
		Map<String, Set<String>> drugSynsSMMART_2 = loadDrugsAndSynonymDeckFromFile(drugSynsSMMART, "resources/beta_v2/nci_thesarus_prime_act_df.txt", true, 0, 1);
		System.out.println("SMMART2 synonym deck added; map key set size: " + drugSynsSMMART_2.keySet().size());

		//build on deck, add hnscc
		Map<String, Set<String>> drugSynsAddHNSCC = loadDrugsAndSynonymDeckFromFile(drugSynsSMMART_2, "resources/beta_v2/nci_thesarus_hnscc_df.txt",true, 0, 1);
		System.out.println("HNSCC synonym deck added; map key set size: " + drugSynsAddHNSCC.keySet().size());

		//build on deck, add aml
		Map<String, Set<String>> drugSynsAddAML = loadDrugsAndSynonymDeckFromFile(drugSynsAddHNSCC, "resources/beta_v2/nci_thesarus_beataml_df.txt",true, 0, 1);
		System.out.println("HNSCC synonym deck added; map key set size: " + drugSynsAddAML.keySet().size());

		//now need to bring in the manual synonyms as well- manual smmart 1
		//NO HEADERS on these files
		Map<String, Set<String>> drugSynsManSMMART = loadDrugsAndSynonymDeckFromFile(drugSynsAddAML, "resources/beta_v2/mmtert_not_found_thesarus_manual.txt",false, 0, 1);
		System.out.println("SMMART manual deck 1 added; map key set size: " + drugSynsManSMMART.keySet().size());

		//now need to bring in the manual synonyms as well- manual smmart 1
		Map<String, Set<String>> drugSynsManSMMART2 = loadDrugsAndSynonymDeckFromFile(drugSynsManSMMART, "resources/beta_v2/prime_act_not_found_thesarus_manual.txt", false, 0, 1);
		System.out.println("SMMART manual deck 2 added; map key set size: " + drugSynsManSMMART2.keySet().size());

		//now need to bring in the manual synonyms as well- manual smmart 1
		Map<String, Set<String>> drugSynsManSMMART2_HNSCC = loadDrugsAndSynonymDeckFromFile(drugSynsManSMMART2, "resources/beta_v2/hnscc_not_found_not_nat_thesarus_manual.txt", false, 0, 1);
		System.out.println("HNSCC manual deck added; map key set size: " + drugSynsManSMMART2_HNSCC.keySet().size());

		//now need to bring in the manual synonyms as well- manual smmart 1
		Map<String, Set<String>> drugSyns_BetaV2 = loadDrugsAndSynonymDeckFromFile(drugSynsManSMMART2_HNSCC, "resources/beta_v2/aml_not_found_thesarus_manual.txt", false, 0, 1);
		System.out.println("AML manual deck added; map key set size: " + drugSyns_BetaV2.keySet().size());


		//now add code to load into a drug set
		//take our drug->synonym deck and load into a drug set
		Set<Drug> updatedDrugSet = createDrugSetFromSynonymDeck(drugSyns_BetaV2);

		System.out.println("Total number of drugs: " + updatedDrugSet.size()); //count: w/ some formulation duplicates

		//output to file so we can keep track
		PrintStream ps = new PrintStream("results_beta_V2/RunningSynonymDeck_BetaV2.txt");
		ps.println("Drug" + "\t" + "Synonyms");
		for (String drugName: drugSyns_BetaV2.keySet()) {
			//System.out.println("Checking drug: " + drugName);
			ps.print(drugName + "\t");
			Set<String> allSynonyms = drugSyns_BetaV2.get(drugName);
			//System.out.println("Synonym deck size: " + allSynonyms.size());
			for (String synonym:allSynonyms) {
				ps.print(synonym + "|");
			}
			ps.println();
		}
		ps.close();

	}

	
	@Test
	public void testReadIUPHAR() throws IOException{
		
		FileUtility fileUt = new FileUtility();
		//prev file
		//fileUt.setInput("resources/IUPHAR/IUPHAR_interactions_fixedAB_06.02.16.csv");
		//file to fix
		fileUt.setInput("resources/IUPHAR_06.15.16/IUPHAR_interactions_06.15.16.csv");

		
		//parse file
		String[] headers = fileUt.readLine().split(",");//skip first line
		int lineCounter = 2;
		String line = null;
		char comma = ',';
		char quote = '"';
		
		
		
		boolean flag = false;
		while ((line = fileUt.readLine()) != null){
			//holder for parsed line line
			String parsedLine = "";//reset for new line
			
			char[] charArray = line.toCharArray();
			int lengthCharArray= charArray.length-1;//index for last char
			boolean startLine = true; //flag for first quote
			int firstIndex=0;
			int lastIndex=0;
			String commaBlock = "";
			
			if (lineCounter == 1286){
				System.out.println("Printing line: ");
				System.out.println(line);
				int arrayIndex = 0;
				
				//go through by each CHAR
				
				for (char each:charArray){
					System.out.println("Char: " + each + " ,"  + arrayIndex + " out of " + lengthCharArray);
					
					//make sure we are not at last char
					if (arrayIndex != lengthCharArray){
						//first quote
						if (each==quote && flag == false){
							System.out.println("first quote found");
							firstIndex = arrayIndex;
							flag = true; //start quote
							arrayIndex++;
							
							if (startLine !=true){
								//send commaBlock to parsed lne
								System.out.println("Parsed line prev cb: "+ parsedLine);
								//skip first comma in commaBlock, skip last comma
								System.out.println("Comma block prev: " +commaBlock);
								
								System.out.println("Comma block length: " + commaBlock.length());
								if (commaBlock.length()!=1){
									commaBlock = commaBlock.substring(1, commaBlock.length()-1);
									System.out.println("Comma block: " +commaBlock);
									//now split on comma
									String[] blocks = commaBlock.split(",");
									for (String block:blocks){
										System.out.println("Block: " + block);
										parsedLine = parsedLine + block+ "\t";
										
									}
									System.out.println("Parse line: " + parsedLine);
									commaBlock = "";//reset commaBlock
								
								}
								//if comma block length = 1
								else{
									parsedLine = parsedLine + "\t";//need ending tilde
									commaBlock = "";//reset commaBlock
								}
							}
							startLine = false;
						}
						//second quote
						else if (each==quote && flag == true){
							System.out.println("second quote found");
							lastIndex = arrayIndex;
							flag = false;//end quote
							
							//now substring line and send to array
							System.out.println("First quote index: " + firstIndex );
							System.out.println("Second quote index: " +  lastIndex);
							String inquote = line.substring(firstIndex + 1, lastIndex);
							System.out.println("Inquote: " + inquote);
							System.out.println("Parsed line prev: " + parsedLine);
							parsedLine = parsedLine + inquote + "\t";
							
							System.out.println("Parsed line: " + parsedLine);
							arrayIndex++;
						}
						//if outside quotes
						else if (each!=quote && flag == false){
							commaBlock = commaBlock + each;
							System.out.println("CommaBlock: " + commaBlock);
							arrayIndex++;
							
						}
						
						//if inside quotes, just increment forward
						else{
							//otherwise continue
							arrayIndex++;
							continue;
						}
					}
					
					else{
						//last character
						System.out.println("LAST CHARACTER");
					}
				}
					
				System.out.println("Parsed line: ");
				System.out.println(parsedLine);
				
				String[] parsedTokens = parsedLine.split("\t");
				System.out.println("Num parsed tokens: " + parsedTokens.length);
				System.out.println("Parsed tokens: ");
				for (String parsedToken: parsedTokens){
					System.out.println(parsedToken);
				}
				
				//fix first tab?
			}

			
			lineCounter++;
					
		}
	}
	
	private Drug createDrug(Session currentSession, Set<Drug> drugSet, String drugName) {
		//Drug check
		boolean drugFound=false;
		for (Drug drug:drugSet){
			if (drug.nameIsEquivalent(drugName)){
				System.out.println("Drug found: " + drug.getDrugName());
				drugFound=true;
				//then use this target for interaction
				return drug;
			}
		}
		if (drugFound==false){
			//create new target
			Drug newDrug = new Drug();
			newDrug.setDrugName(drugName);
			drugSet.add(newDrug);//update target set, need to update this
			return newDrug;
		}
		return null; //default
	}
}
