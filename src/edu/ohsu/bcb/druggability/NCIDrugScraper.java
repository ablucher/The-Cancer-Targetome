package edu.ohsu.bcb.druggability;



	
    import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;  
    import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.reactome.r3.util.FileUtility;  
    
    public class NCIDrugScraper{  
 
  
//        @Test
//        public void testGetNCIDrugs() throws FileNotFoundException{
//        	//get NCI Drug list ->print to file
//        	String outputFile = "scrapedNCIDrugs_05.11.16.txt";
//        	getNCIDrugSet(outputFile);
//        	cleanNCIDrugSet(outputFile, cleanedFile);
//        }
        
        /**
         * Method scrapes NCI Drugs from NCI website - A-Z Cancer Drugs.
         * This method is particular to this website, will not work for other web scraping jobs.
         * @throws FileNotFoundException
         */
        public void getNCIDrugSet(String outputFile) throws FileNotFoundException{
    		PrintStream ps = new PrintStream(outputFile);
    		ps.println("Drug" + "\t" + "Brand_Name" + "\t" + "FDA_Approval" + "\t" + "Date_Posted" + "\t" + "Date_Updated");
   			
    		//check try/catch statments with GW
	        try
	        {
	        	//connect to url
	            Document document = Jsoup.connect("http://www.cancer.gov/about-cancer/treatment/drugs").get();

	            
	            //need level <ul class="no-bullets"...>  
	            Elements drugs = document.select("ul.no-bullets > li");
	           
	            //**A-Z Drug Listing
	            int drugCounter = 0;
	            for (Element drug: drugs){
	            	//print text
	            	System.out.println("Drug: "+ drug.text());
	            	
	            	//for each drug, get link to drug info page
	            	Elements drugLinks = drug.select("a[href]");
	            	for (Element drugLink: drugLinks){
	            		String link = drugLink.attr("abs:href");
	            		System.out.println("Link: " + link);
	            		
	            		//**INDIVIDUAL DRUG INFO
	            		//now go to this link and get further information
	            		Document drugDoc = Jsoup.connect(link).get();
	            		
	            		String drugName = drugDoc.select("h1").get(0).text();
	            		System.out.println("Drug Name: " + drugName);
	            		
	            		//iterate to section with brand/fda info
	            		//get titles here
	            		Elements entries = drugDoc.select("div.two-columns.brand-fda").select("div.column1");
	            		System.out.println("num entries: "+ entries.size());
	            		

	            		String brandName = "";
	            		String fdaApproval = null;
	            		
	            		//if 2 entries, OK to get info
	            		if (entries.size() >=2){
	            			//get values for both entries
	            			Elements brandEntries = drugDoc.select("div.two-columns.brand-fda").select("div.column2");	            		
	            			//brand info
	            			Element brandNames = brandEntries.get(0);           		
		            		
		            		//we need to get the text nodes here
	            			int brandCounter = 0;
		            		List<TextNode> brands = brandNames.textNodes();
		            		for (TextNode brand: brands){
		            			//if last brand, don't include |
		            			if (brandCounter==(brands.size()-1)){
		            				brandName = brandName + brand.text();
		            			}
		            			//separate on pipe
		            			else{
		            				//need .text() here
			            			brandName = brandName + brand.text()  + "|" ;
			            			brandCounter++;
		            			}
		            			
		            			
		            		}
		            		
		            		//fda approval status
	            			fdaApproval = brandEntries.get(1).text();
	            			
	            		}
	            		//if only one, check what it is
	            		else if (entries.size()==1){
	            			//if fda approval, then brand name is null
	            			if (entries.text().equals("FDA Approved")){
	            				fdaApproval = entries.text();
	            				brandName = "NA";
	            			}
	            			//if brandname, then fda appoval is null
	            			else{
	            				brandName = entries.text();
	            				fdaApproval = "NA";
	            			}
	            			
	            		
	            		}
	            		//if no entries
	            		else{
	            			brandName = "NA";
	            			fdaApproval = "NA";
	            			System.out.println("No Brand Names found!");
	            		}
          		
	            		System.out.println("FDA App: " + fdaApproval);
	            		System.out.println("Brand(s):  "+ brandName);
          		
	            		
	            		//get posted and updated dates
	            		String postDate;
	            		String upDate;
	            		Elements dates = drugDoc.select("div.document-dates.horizontal").select("ul.clearfix > li");

	            		//check dates
	            		if (dates.size() == 2){
		            		//within <li>
		            		//first is posted date, get childNode 1 for unbolded text = date
		            		//second is updated date, get childNode 1 for unbolded text = date
		            		postDate = dates.get(0).childNode(1).toString();
		            		upDate = dates.get(1).childNode(1).toString();
		            		System.out.println("post date: " + postDate);
		            		System.out.println("up date: " + upDate);
	            		}
	            		else if (dates.size() ==1){
	            			postDate = dates.get(0).childNode(1).toString();
	            			System.out.println("post date: " + postDate);
	            			upDate = "NA";
	            		}
	            		else{
	            			postDate = "NA";
	            			upDate = "NA";
	            			
	            		}
	            		//print everything to file
	            		//all fields checked above
	            		ps.println(drugName+"\t"+brandName+"\t"+fdaApproval + "\t" + postDate + "\t" + upDate);
	            		
	            	}
	            	
	            	drugCounter++;
	            	//break for testing
//	            	if (drugCounter == 10){
//	            		break;
//	            	}
	            }
           
	        } 
	        catch (IOException e) //check where this should be
	        {
	            e.printStackTrace();
	        }  
        }
        
        /**
         * This method takes a file with scraped NCI drugs and cleans. 
         * Removes duplicates and creates an "aside" file with combo drugs.
         * IN PROGRESS
         * @param nciDrugFile
         */
        public void cleanNCIDrugSet(String nciDrugFile, String cleanedNciDrugFile, String comboDrugFile){
        	//not sure if this file set up is the best way to do it?
        	//work on this
        	//come back to this later
        	
        	
        	
        }
        
        

        
        
        
    }  
	

