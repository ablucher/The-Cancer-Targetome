package edu.ohsu.bcb.druggability.dataModel;

import java.util.Set;

import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

/**
 * @author blucher
 *
 */
@XmlRootElement
public class Target {
	@Override
	public String toString() {
		return "Target [targetName=" + targetName + "]";
	}

	
	private String targetName;
	private Integer targetID;
	private String targetType;//Protein, DNA, or RNA
	private String uniprotID;
	private Set<String> targetSynonyms;
	private String targetSpecies;
	
	public Target(){};
	
	//setters and getters
	public String getTargetName() {
		return targetName;
	}
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}
	public String getUniprotID() {
		return uniprotID;
	}
	public void setUniprotID(String uniprotID) {
		this.uniprotID = uniprotID;
	}
	public Set<String> getTargetSynonyms() {
		return targetSynonyms;
	}
	public void setTargetSynonyms(Set<String> targetSynonyms) {
		this.targetSynonyms = targetSynonyms;
	}

	@XmlID
	public String getId() {
	    return targetID + "";
	}
	
	public Integer getTargetID() {
		return targetID;
	}
	
	public void setTargetID(Integer targetID) {
		this.targetID = targetID;
	}
	
	/**
	 * Method checks whether two targets are equivalentby checking whether type and then
	 * UniProt ID are is the same, ignores case for uniprot.
	 * 
	 * @param otherTarget
	 * @return
	 */
	public boolean isEquivalent(Target otherTarget){
		//null check
		if (otherTarget.uniprotID !=null && this.uniprotID != null){
			//add check for target type
			if (otherTarget.getTargetType().equals(this.getTargetType())){
				
				//check target uniprotID
				if (otherTarget.uniprotID.equalsIgnoreCase(this.uniprotID)){
					return true;
				}
			}
			
		}
		//get rid of this else statement if STILL NOT WORKING**
		//if uniprot is false
		//quick check type and name, if equal then target is the same
		else if (otherTarget.getTargetType().equals(this.getTargetType())){
				//check target uniprot
				if (otherTarget.getTargetName().equals(this.getTargetName())){
					return true;
				}
		}
		return false;
		
	}
	
	/**
	 * Checks target for equivalency using uniprot and type
	 * @param uniprot
	 * @param type
	 * @return
	 */
	public boolean isEquivalentUniProt(String uniprot, String type, String name){
		
		//check target uniprotID
		if (uniprot != null && !uniprot.equals("null") && this.uniprotID!=null && !this.uniprotID.equals("null")){
			
			//check target type
			if (this.targetType.equals(type)){
				//check target uniprot
				if (this.uniprotID.equals(uniprot)){
					return true;
				}
				
			}
		}
		//get rid of this else statement if STILL NOT WORKING**
		//if uniprot is false
		//quick check type and name, if equal then target is the same
		else if (this.targetType.equals(type)){
				//check target uniprot
				if (this.targetName.equals(name)){
					return true;
				}
		}
		return false;
		
	}
	
	@Test
	public void testIsEquivalent(){
		Target abl = new Target();
		abl.setTargetName("ABL");
		abl.setTargetType("Protein");
		abl.setUniprotID("P00519");
		
		Target ablSyn= new Target();
		ablSyn.setTargetName("ABL1syn");
		ablSyn.setTargetType("DNA");//if protein, the same
									//if dna, not the same
		ablSyn.setUniprotID("P00519");//note same uniprot but different names
		
		Target alk = new Target();
		alk.setTargetName("ALK");
		alk.setUniprotID("Q9UM73");
		alk.setTargetType("Protein");
		
		//test on uniprot matching
		//first should be true
		//second should be false
		if (abl.isEquivalent(ablSyn)){
			System.out.println("These drugs are the same: "+  abl.getTargetName() + " AND " + ablSyn.getTargetName());
		}
		else{
			System.out.println("These drugs are NOT the same: "+  abl.getTargetName() + " AND " + ablSyn.getTargetName());
		}
		//test on synonym matching
		if (abl.isEquivalent(alk)){
			System.out.println("These drugs are the same: "+  abl.getTargetName() + " AND " + alk.getTargetName());
		}
		else{
			System.out.println("These drugs are NOT the same: "+  abl.getTargetName() + " AND " + alk.getTargetName());
			
		}
		
	}
	public String getTargetSpecies() {
		return targetSpecies;
	}

	public void setTargetSpecies(String targetSpecies) {
		this.targetSpecies = targetSpecies;
	}

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	
	
}
