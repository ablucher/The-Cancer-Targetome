package edu.ohsu.bcb.druggability;

import org.reactome.r3.util.FileUtility;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 * This class parses the target download from Therapeutic Target Database
 * and retrieves drug-target interaction information. 
 * @author blucher
 *
 */

public class TTDParser {

	/**Method parses original TTD file into tokens
	 * @throws IOException 
	 * 
	 */
	private void parseTTD(String ttdFile) throws IOException{
		//load ttd file
		//parse into tokens
		//for each  target
		//get drugs
		//get interaction type
		FileUtility fileUt = new FileUtility();
		fileUt.setInput(ttdFile);
		for (int i = 0; i <= 11; i++){//skip commenting
			System.out.println(fileUt.readLine());
		}
		//set for unique target ids
		Set<String> targetIDs = new HashSet<String>();
		//map for target info
		Map<String, ArrayList> fullTTDMap = new HashMap<String, ArrayList>();
		
		//iterate through lines
		int lineCounter = 12; //check line numbering**
		String line = null;
		while ((line = fileUt.readLine()) != null){
			//System.out.println("Testing:" + line);
			String[] tokens = line.split("\t");
			String targetID = tokens[0];
			targetIDs.add(targetID);//add to set of targets
			String category= tokens[1];
			String content = tokens[2];
			//combine category and content into list
			//want new list per row in file
			List<String> targetInfo = new ArrayList<String>();
			targetInfo.add(category);
			targetInfo.add(content);
			
			//add target information to map
			//if key in map, then add to list
			if (fullTTDMap.containsKey(targetID)){
				//then just add targetInfo to ongoing list
				ArrayList<List<String>> targetInfoAll = fullTTDMap.get(targetID);
				targetInfoAll.add(targetInfo);
			}
			else{
				ArrayList<List<String>> targetInfoAll = new ArrayList<List<String>>();
				targetInfoAll.add(targetInfo);
				fullTTDMap.put(targetID, targetInfoAll);
			}

			lineCounter++;
//			if (lineCounter >259){
//				break;//for testing, break at 1
//			}
//			
		}
		
		System.out.println("Number of lines: " + lineCounter);
		System.out.println("Number of unique targets: " + targetIDs.size());
		/////////////////
		//checking lists in array list in map:
		//testing 4/20/16
		System.out.println("Testing section: ");
		System.out.println("Map size: " + fullTTDMap.size());
		System.out.println("Map keys: " + fullTTDMap.keySet());
//		System.out.println("Checking Map Entries: ");
//		for (String key: fullTTDMap.keySet()){
//			System.out.println(key);
//			ArrayList<List<String>> value = fullTTDMap.get(key);
//			System.out.println("Number of entries in list:" + value.size());
//			for (List<String> v: value){
//				System.out.println("Inner List Row:" + v);
//			}
//		}
		///////////////
		
		fileUt.close();

	}
	@Test
	public void testParseTTD() throws IOException{
		String fileName = "resources/TTD_download.txt";
		parseTTD(fileName);
	}
	

}
