package edu.ohsu.bcb.druggability.dataModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

/**
 * This helper/facilitator class holds the relationship between an Interaction and Evidence in the following manner:
 * Each Interaction has 1 Source = (LitRef, Database) tuple.
 * Each ExperimentalEvidence object has 1 Source = (LitRef, Database) tuple.
 * @author blucher
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Source {
	private Integer sourceID;
	@XmlIDREF
	private LitEvidence sourceLiterature;
	@XmlIDREF
	private DatabaseRef sourceDatabase;
	@XmlIDREF
	private DatabaseRef parentDatabase; //parent database for reference lineage
    //of databases
	
	
	public DatabaseRef getParentDatabase() {
		return parentDatabase;
	}


	public void setParentDatabase(DatabaseRef parentDatabase) {
		this.parentDatabase = parentDatabase;
	}


	public Source(){
		
	}

	@XmlID
	public String getId() {
	    return sourceID + "";
	}

	public Integer getSourceID() {
		return sourceID;
	}

	public void setSourceID(Integer sourceID) {
		this.sourceID = sourceID;
	}


	public LitEvidence getSourceLiterature() {
		return sourceLiterature;
	}


	public void setSourceLiterature(LitEvidence sourceLiterature) {
		this.sourceLiterature = sourceLiterature;
	}


	public DatabaseRef getSourceDatabase() {
		return sourceDatabase;
	}


	public void setSourceDatabase(DatabaseRef sourceDatabase) {
		this.sourceDatabase = sourceDatabase;
	}


	
}
