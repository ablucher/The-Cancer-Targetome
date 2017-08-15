package edu.ohsu.bcb.druggability.dataModel;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

import org.junit.Test;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpEvidence {
    
		private Integer expID;//unique ID here?
		
		private String assayType;
		private String assayValueLow;
		private String assayValueMedian;
		private String assayValueHigh;
		private String assayUnits;
		private String assayRelation;
		private String assayDescription;
		
		//needs work
		private String assaySpecies;
		private String parentSource; //**THIS NEEDS ADRESSING, for now just adding a column
		@XmlIDREF
		private Set<Source> expSourceSet;
		
		public ExpEvidence(){};
		
		
		//getters and setters
		public Integer getExpID() {
			return expID;
		}
		
		@XmlID
		public String getId() {
		    return expID + "";
		}
		
		public void setExpID(Integer expID) {
			this.expID = expID;
		}
		public String getAssayType() {
			return assayType;
		}
		public void setAssayType(String assayType) {
			this.assayType = assayType;
		}
		
		public String getAssaySpecies() {
			return assaySpecies;
		}
		public void setAssaySpecies(String assaySpecies) {
			this.assaySpecies = assaySpecies;
		}

		public String getAssayUnits() {
			return assayUnits;
		}
		public void setAssayUnits(String assayUnits) {
			this.assayUnits = assayUnits;
		}

	


		public String getAssayRelation() {
			return assayRelation;
		}

		public void setAssayRelation(String assayRelation) {
			this.assayRelation = assayRelation;
		}

		public String getAssayDescription() {
			return assayDescription;
		}

		public void setAssayDescription(String assayDescription) {
			this.assayDescription = assayDescription;
		}

		public String getAssayValueLow() {
			return assayValueLow;
		}

		public void setAssayValueLow(String assayValueLow) {
			this.assayValueLow = assayValueLow;
		}

		public String getAssayValueMedian() {
			return assayValueMedian;
		}

		public void setAssayValueMedian(String assayValueMedian) {
			this.assayValueMedian = assayValueMedian;
		}

		public String getAssayValueHigh() {
			return assayValueHigh;
		}

		public void setAssayValueHigh(String assayValueHigh) {
			this.assayValueHigh = assayValueHigh;
		}

		public String getParentSource() {
			return parentSource;
		}

		public void setParentSource(String parentSource) {
			this.parentSource = parentSource;
		}

		public boolean isEquivalent(String assayType, String assayValue, String parentSource){
			
			if (assayType!=null && assayValue!=null && parentSource !=null && this.assayValueMedian!=null){
				
				if (this.assayType.equals(assayType) && this.assayValueMedian.equals(assayValue)
						&& this.parentSource.equals(parentSource) ){
					return true;
				}
				return false;
			}
			else{
				return false;
			}
		}
		
		@Test
		public void testIsEquivalent(){
			
			ExpEvidence testExp = new ExpEvidence();
			testExp.setAssayType("IC50");
			testExp.setAssayValueMedian("5.00");
			testExp.setParentSource("CHEMBL");
			
			if (testExp.isEquivalent("IC50", "5.00", "CHEMBL")){
				System.out.println("Match found");
			}
			else{
				System.out.println("No match found");
			}
			
		}

		public Set<Source> getExpSourceSet() {
			return expSourceSet;
		}

		public void setExpSourceSet(Set<Source> expSourceSet) {
			this.expSourceSet = expSourceSet;
		}
	

}
