package edu.ohsu.bcb.druggability.dataModel;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Interaction{

	@Override
	public String toString() {
		return "Interaction [intDrug=" + intDrug.getDrugName() + ", intTarget=" + intTarget.getTargetName() + "]";
	}

	private Integer interactionID; //unique ID for database tables
	//each interaction has 1 drug, 1 target
	@XmlElement(name="drug")
	@XmlIDREF
	private Drug intDrug;
	@XmlElement(name="target")
	@XmlIDREF
	private Target intTarget;
	//interaction type
	private String interactionType;
	//each interaction has set of Source
	@XmlElement(name="source")
	@XmlIDREF
	private Set<Source> interactionSourceSet;

	@XmlElement(name="expEvidence")
	@XmlIDREF
	private Set<ExpEvidence> expEvidenceSet;
	
	public Interaction(){}//default constructor
	
	//getters and setters
	public Drug getIntDrug() {
		return intDrug;
	}
	public void setIntDrug(Drug intDrug) {
		this.intDrug = intDrug;
	}
	public Target getIntTarget() {
		return intTarget;
	}
	public void setIntTarget(Target intTarget) {
		this.intTarget = intTarget;
	}
	public Integer getInteractionID() {
		return interactionID;
	}
	public void setInteractionID(Integer interactionID) {
		this.interactionID = interactionID;
	}

	public Set<ExpEvidence> getExpEvidenceSet() {
		return expEvidenceSet;
	}

	public void setExpEvidenceSet(Set<ExpEvidence> expEvidenceSet) {
		this.expEvidenceSet = expEvidenceSet;
	}

	@XmlID
	public String getId() {
	    return interactionID + "";
	}

	/**
	 * Method compares to Interaction objects. 
	 * Both the Drug object and Target object must be equal to return true.
	 * Relies on Drug and Target isEquivalent methods which check drug based on name/synonyms
	 * and check target based on uniprotID
	 * @param otherInt
	 * @return
	 */
	public boolean isEquivalent(Interaction otherInt){
		Drug otherDrug = otherInt.getIntDrug();
		Target otherTarget = otherInt.getIntTarget();
		//check if both drug and target are the same
		if (otherDrug.isEquivalent(this.getIntDrug()) && otherTarget.isEquivalent(this.getIntTarget())){
			return true;
		}
		
		return false;
		
	}
	
	@Test
	public void testIsEquivalent(){
		//Imatinib and Gleevec -> both go to ABL
		//should be identified as equivalent interactions
		Drug imatinib = new Drug();
		Set<String> set1 = new HashSet<String>();
		set1.add("gleevec");
		set1.add("STI571");
		imatinib.setDrugName("Imatinib");
		imatinib.setDrugSynonyms(set1);
		
		Drug gleevec = new Drug();
		gleevec.setDrugName("Gleevec");
		
		Target abl = new Target();
		abl.setTargetName("ABL");
		abl.setUniprotID("P00519");
		
		Target ablSyn = new Target();
		ablSyn.setTargetName("ABLSyn");
		ablSyn.setUniprotID("P00519");
				
		Interaction int1 = new Interaction();
		int1.setIntDrug(imatinib);
		int1.setIntTarget(ablSyn);
		
		Interaction int2 = new Interaction();
		int2.setIntDrug(gleevec);
		int2.setIntTarget(abl);
		
		if (int1.isEquivalent(int2)){
			System.out.println("These two Interactions are the same.");
		}
		else{
			System.out.println("These two Interactions are NOT the same.");
		}
		
		
	}

	public String getInteractionType() {
		return interactionType;
	}

	public void setInteractionType(String interactionType) {
		this.interactionType = interactionType;
	}

	public Set<Source> getInteractionSourceSet() {
		return interactionSourceSet;
	}

	public void setInteractionSourceSet(Set<Source> interactionSourceSet) {
		this.interactionSourceSet = interactionSourceSet;
	}

	


}
