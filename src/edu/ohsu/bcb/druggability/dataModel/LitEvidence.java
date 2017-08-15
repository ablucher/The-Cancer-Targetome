package edu.ohsu.bcb.druggability.dataModel;

import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LitEvidence {
	public Integer literatureID;
	public String PubMedID;
	
	public LitEvidence(){};
	
	public Integer getLitID() {
		return literatureID;
	}
	
	@XmlID
	public String getId() {
	    return literatureID + "";
	}
	
	public void setLitID(Integer litID) {
		this.literatureID = litID;
	}

	//getters and setters
	public String getPubMedID() {
		return PubMedID;
	}
	public void setPubMedID(String pubMedID) {
		PubMedID = pubMedID;
	}


}
