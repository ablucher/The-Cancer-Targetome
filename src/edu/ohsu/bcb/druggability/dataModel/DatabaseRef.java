package edu.ohsu.bcb.druggability.dataModel;

import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author blucher
 *
 */
@XmlRootElement
public class DatabaseRef {
	private Integer databaseID;
	private String databaseName;
	private String version;
	private String downloadURL;
	private String downloadDate;

	
	public DatabaseRef(){}


	public Integer getDatabaseID() {
		return databaseID;
	}
	
	@XmlID
	public String getId() {
	    return databaseID + "";
	}
	
	public void setDatabaseID(Integer databaseID) {
		this.databaseID = databaseID;
	}


	public String getDatabaseName() {
		return databaseName;
	}


	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}


	public String getDownloadURL() {
		return downloadURL;
	}


	public void setDownloadURL(String downloadURL) {
		this.downloadURL = downloadURL;
	}


	public String getDownloadDate() {
		return downloadDate;
	}


	public void setDownloadDate(String downloadDate) {
		this.downloadDate = downloadDate;
	}

	

	
	
	

}
