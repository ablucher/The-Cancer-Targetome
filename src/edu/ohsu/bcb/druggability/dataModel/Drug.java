package edu.ohsu.bcb.druggability.dataModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

/**
 * @author blucher
 *
 */
@XmlRootElement
public class Drug  {
	
	@Override
	public String toString() {
		return "Drug [drugName=" + drugName + "]";
	}

	
	private String drugName;
	private Integer drugID;//need unique ID for database tables
	private Set<String> drugSynonyms; //prev was tree set, change accompny methods
	private HashMap<String, Set<String>> drugFormulations;
	private String approvalDate;
	private String status;
	private String atcClassID;
	private String atcClassName;
	private String atcClassStatus;
	private String epcClassID;
	private String epcClassName;
	
	
	public Drug(){}//need this for hibernate?
	
	public Set<String> getDrugSynonyms() {
		return drugSynonyms;
	}
	public void setDrugSynonyms(Set<String> drugSynonyms) {
		this.drugSynonyms = drugSynonyms;
	}
	public String getDrugName() {
		return drugName;
	}
	public void setDrugName(String drugName) {
		this.drugName = drugName;
	}
	
	@XmlID
	public String getId() {
	    return drugID + "";
	}
	
	public Integer getDrugID() {
		return drugID;
	}
	public void setDrugID(Integer drugID) {
		this.drugID = drugID;
	}
	/**
	 * Method checks whether two drugs are equivalent. This first compares Drug objects by name, returns true if match found. 
	 * Then it calls method to check synonyms. If DrugA.drugName is in DrugB.drugSynonyms or
	 * vice versa, returns true. 
	 * @param otherDrug
	 * @return
	 */
	public boolean isEquivalent(Drug otherDrug){
		
		//check drug names
		if (otherDrug.drugName.equalsIgnoreCase(this.drugName)){
			return true;
		}
		
		
		else if (otherDrug.getDrugSynonyms()!=null && (synonymCheck(otherDrug.getDrugSynonyms(), this.getDrugName()))){
			return true;
		}
		else if (this.getDrugSynonyms()!=null && (synonymCheck(this.getDrugSynonyms(), otherDrug.getDrugName()))){
			return true;
		}
		return false;
		
	}
	/**
	 * This method checks whether a drug name is equivalent to a Drug object.
	 * Thus we can check whether a ligand name matches a Drug object based on name or synonyms.
	 * @param drugname
	 * @return
	 */
	public boolean nameIsEquivalent(String drugName){
		
		//check if string name matches drug name
		if (drugName.equalsIgnoreCase(this.getDrugName())){
			return true;
		}
		//check if string name in drug synonym set
		else if (this.getDrugSynonyms()!= null && synonymCheck(this.getDrugSynonyms(), drugName)){
			return true;
		}
		
		return false;
	}
	
	@Test
	public void testNameIsEquivalent(){
		//working here
		//test string against two drug objects and see
		//both in name or synonyms**
		
		Drug imatinib = new Drug();
		imatinib.setDrugName("Imatinib");
		Set<String> set1 = new HashSet<String>();
		set1.add("imatinib");
		set1.add("gleevec");
		set1.add("STI571");
		imatinib.setDrugSynonyms(set1);
		
		String testDrugName = "Imatinib";
		String testDrugName2 = "Ceritinib";
		String testDrugName3 = "Gleevec";
		
		if (imatinib.nameIsEquivalent(testDrugName)){
			System.out.println("Drug Imatinib equal to: " + testDrugName);
		}
		else{
			System.out.println("Drug Imatinib not equal to: " + testDrugName);
		}
		
		if (imatinib.nameIsEquivalent(testDrugName2)){
			System.out.println("Drug Imatinib equal to: " + testDrugName2);
		}
		else{
			System.out.println("Drug Imatinib not equal to: " + testDrugName2);
		}
			
		if (imatinib.nameIsEquivalent(testDrugName3)){
			System.out.println("Drug Imatinib equal to: " + testDrugName3);
		}
		else{
			System.out.println("Drug Imatinib not equal to: " + testDrugName3);
		}
			
		
				
	}
	
	/**
	 * Helper method for isEquivalent(). Checks string against string set of synonyms.
	 * @param drugSyns
	 * @param drugName
	 * @return
	 */
	
	public boolean synonymCheck(Set<String> drugSyns, String drugName){
		
		for (String syn: drugSyns){
			if (syn.equalsIgnoreCase(drugName)){
				return true;
			}
		}
		return false;
		
	}
	
	@Test
	public void testIsEquivalent(){
		Drug imatinib = new Drug();
		imatinib.setDrugName("imatinib");
		Drug imatinib2 = new Drug();
		imatinib2.setDrugName("imatinib2");
		Drug gleevec = new Drug();
		gleevec.setDrugName("gleevec");
		
		//syn set for testing
		//Drugs imatinib and gleevec identified as the same
		//if either is in the other's drug syn set
		Set<String> set1= new HashSet<String>();
		set1.add("Imatinib");
		set1.add("Gleevec");
		//imatinib.setDrugSynonyms(set1);
		gleevec.setDrugSynonyms(set1);
		
		System.out.println("Checking imatinib and imatinib2:");
		if (imatinib.isEquivalent(imatinib2)){
			System.out.println("These drugs are the same." );
		}
		else{
			System.out.println("These drugs are NOT the same." );
		}
		
		System.out.println("Checking imatinib and gleevec:");
		if (imatinib.isEquivalent(gleevec)){
			System.out.println("These drugs are the same." );
		}
		else{
			System.out.println("These drugs are NOT the same." );
		}
		
		
		
	}

	public String getApprovalDate() {
		return approvalDate;
	}

	public void setApprovalDate(String approvalDate) {
		this.approvalDate = approvalDate;
	}

	public String getAtcClassName() {
		return atcClassName;
	}

	public void setAtcClassName(String atcClassName) {
		this.atcClassName = atcClassName;
	}

	public String getAtcClassID() {
		return atcClassID;
	}

	public void setAtcClassID(String atcClassID) {
		this.atcClassID = atcClassID;
	}

	public String getEpcClassName() {
		return epcClassName;
	}

	public void setEpcClassName(String epcClassName) {
		this.epcClassName = epcClassName;
	}

	public String getEpcClassID() {
		return epcClassID;
	}

	public void setEpcClassID(String epcClassID) {
		this.epcClassID = epcClassID;
	}

	public String getAtcClassStatus() {
		return atcClassStatus;
	}

	public void setAtcClassStatus(String atcClassStatus) {
		this.atcClassStatus = atcClassStatus;
	}
	
	public boolean containedInSet(Set<Drug> setToCheck){
		for (Drug drugToCheck: setToCheck){
			if (drugToCheck.nameIsEquivalent(this.drugName)){
				return true;//if found, return true
			}
		}
		return false;//if not found, return false
	}

	public HashMap<String, Set<String>> getDrugFormulations() {
		return drugFormulations;
	}

	public void setDrugFormulations(HashMap<String, Set<String>> drugFormulations) {
		this.drugFormulations = drugFormulations;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
