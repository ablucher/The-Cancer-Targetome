package edu.ohsu.bcb.druggability;

import java.util.Set;

public class TTDObject {
	private String targetID;
	private String targetName;
	private String targetType;
	private String targetUniprot;
	private Set<String> targetSynonyms;
	private Set<String> targetDrugs;
	
	/**
	 * Checks if targetID and TTDObject match.
	 * @param targetID
	 * @return
	 */
	public boolean isEquivalentObject(String targetID){
		if (this.targetID.equals(targetID)){
			return true;
		}
		return false;
	}
	
	public String getTargetName() {
		return targetName;
	}
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}
	public String getTargetType() {
		return targetType;
	}
	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}
	public String getTargetUniprot() {
		return targetUniprot;
	}
	public void setTargetUniprot(String targetUniprot) {
		this.targetUniprot = targetUniprot;
	}
	public Set<String> getTargetSynonyms() {
		return targetSynonyms;
	}
	public void setTargetSynonyms(Set<String> targetSynonyms) {
		this.targetSynonyms = targetSynonyms;
	}
	public Set<String> getTargetDrugs() {
		return targetDrugs;
	}
	public void setTargetDrugs(Set<String> targetDrugs) {
		this.targetDrugs = targetDrugs;
	}

	public String getTargetID() {
		return targetID;
	}

	public void setTargetID(String targetID) {
		this.targetID = targetID;
	}

}
