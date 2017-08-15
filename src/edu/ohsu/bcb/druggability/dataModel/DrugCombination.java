
package edu.ohsu.bcb.druggability.dataModel;

/**
 * DrugCombination class for combination of 2 Drug objects.
 * @author blucher
 *
 */

public class DrugCombination {
	private Drug drug1;
	private Drug drug2;
	
	public Drug getDrug1() {
		return drug1;
	}
	public void setDrug1(Drug drug1) {
		this.drug1 = drug1;
	}
	public Drug getDrug2() {
		return drug2;
	}
	public void setDrug2(Drug drug2) {
		this.drug2 = drug2;
	}
	
}
