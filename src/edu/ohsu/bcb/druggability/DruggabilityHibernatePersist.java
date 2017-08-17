package edu.ohsu.bcb.druggability;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

public class DruggabilityHibernatePersist {

	/**
	 * Method opens hibernate druggability session. Unit tests demonstrate with mockDruggability.
	 * 
	 * Method to store data in Druggability database. Needs refactoring so that we have a main
	 * method and supporting test methods.
	 * @throws IOException
	 */
	public static Session openSession(String configFileName){
		// hibernate set up
		DruggabilityHibernatePersist druggability = new DruggabilityHibernatePersist();
		File configFile = new File(configFileName);

		SessionFactory sessionFactory = new Configuration().configure(configFile).buildSessionFactory();
		Session session = sessionFactory.openSession();

		return session;
	}
	
	
	
	
//	/**
//	 * Unit test for druggabilityPersist Class. Mock data on test database. 
//	 */
//	@Test
//	public void testDruggabilityPersist(){
//		Session currentSession = DruggabilityPersist();
//		currentSession.beginTransaction();
//		
//		//mock drugs
//		Drug ceritinib = new Drug();
//		ceritinib.setDrugName("ceritinib");
//		TreeSet<String> cerSyn = new TreeSet<String>();
//		cerSyn.add("Ceritinib");
//		cerSyn.add("LDK378");
//		cerSyn.add("ldk378");
//		ceritinib.setDrugSynonyms(cerSyn);
//		currentSession.save(ceritinib);
//		
//		Drug imatinib = new Drug();
//		imatinib.setDrugName("imatinib");
//		TreeSet<String> imatSyn2 = new TreeSet<String>();
//		imatSyn2.add("gleevec");
//		imatSyn2.add("STI571");
//		imatinib.setDrugSynonyms(imatSyn2);
//		currentSession.save(imatinib);
//		
//		//mock target
//		Target abl = new Target();
//		abl.setTargetName("ABL");
//		abl.setUniprotID("UP54405");
//		TreeSet<String> ablSyns = new TreeSet<String>();
//		ablSyns.add("bcr/abl");
//		ablSyns.add("abl");
//		abl.setTargetSynonyms(ablSyns);
//		currentSession.save(abl);
//		Target alk = new Target();
//		alk.setTargetName("ALK");
//		alk.setUniprotID("UP54406");
//		TreeSet<String> alkSyns = new TreeSet<String>();
//		alkSyns.add("alk");
//		alkSyns.add("alk receptor");
//		alk.setTargetSynonyms(alkSyns);
//		currentSession.save(alk);
//		
//		//mock interactions
//		Interaction int1 = new Interaction();
//		int1.setIntDrug(imatinib);
//		int1.setIntTarget(abl);
//		currentSession.save(int1);
//		//test interaction2
//		Interaction int2 = new Interaction();
//		int2.setIntDrug(ceritinib);
//		int2.setIntTarget(alk);
//		currentSession.save(int2);
//		
//		
//		//mock source
//		Source drugbank = new Source();
//		drugbank.setDatabaseName("DrugBank");
//		drugbank.setDownloadDate("05.31.2016");
//		drugbank.setDownloadURL("drugbankURL");
//		drugbank.setVersion("4.0");
//		currentSession.save(drugbank);
//		
//		Source ttd = new Source();
//		ttd.setDatabaseName("TTD");
//		ttd.setDownloadDate("06.01.2016");
//		ttd.setDownloadURL("TTDURL");
//		ttd.setVersion("2.0");
//		currentSession.save(ttd);
//		
//		
//		//mock lit evidence
//		LitEvidence imatLit = new LitEvidence();
//		imatLit.setPubMedID("PUBMED001");
//		Set<Source> imatLitSources = new HashSet<Source>();
//		imatLitSources.add(drugbank);
//		imatLit.setCitedBySources(imatLitSources);
//		currentSession.save(imatLit);
//		
//		LitEvidence imatLit2 = new LitEvidence();
//		imatLit2.setPubMedID("PUBMED003");
//		Set<Source> imatLitSources2 = new HashSet<Source>();
//		imatLitSources2.add(drugbank);
//		imatLit2.setCitedBySources(imatLitSources2);
//		currentSession.save(imatLit2);
//		
//		LitEvidence cerLit = new LitEvidence();
//		cerLit.setPubMedID("PUBMED002");
//		Set<Source> cerLitSources = new HashSet<Source>();
//		cerLitSources.add(drugbank);
//		cerLitSources.add(ttd);
//		cerLit.setCitedBySources(cerLitSources);
//		currentSession.save(cerLit);
//		
//		//mock experimental assay evidence for imatinib
//		ExpEvidence imatExpEv = new ExpEvidence();
//		imatExpEv.setAssayValue(5.0);
//		imatExpEv.setAssayUnits("nM");
//		imatExpEv.setAssayType("Binding Assay");
//		imatExpEv.setAssaySpecies("Homo sapiens");
//		imatExpEv.setAssayCellLine("NA");
//		imatExpEv.setLitEvidence(imatLit);
//		currentSession.save(imatExpEv);
//		ExpEvidence imatExpEv2 = new ExpEvidence();
//		imatExpEv2.setAssayValue(7.0);
//		imatExpEv2.setAssayUnits("nM");
//		imatExpEv2.setAssayType("Binding Assay");
//		imatExpEv2.setAssaySpecies("NA");
//		imatExpEv2.setAssayCellLine("NA");
//		imatExpEv2.setLitEvidence(imatLit);
//		currentSession.save(imatExpEv2);
//		
//		
//		//add experimental evidence to imatinib interaction
//		Set<ExpEvidence> imatExpEvSet = new HashSet<ExpEvidence>();
//		imatExpEvSet.add(imatExpEv);
//		imatExpEvSet.add(imatExpEv2);
//		int1.setExpEvidenceSet(imatExpEvSet);
//		//add lit evidence (w/no exp) to imatinib interaction
//		Set<LitEvidence> imatLitOnlySet = new HashSet<LitEvidence>();
//		imatLitOnlySet.add(imatLit2);
//		int1.setLitEvidenceSet(imatLitOnlySet);
//		currentSession.save(int1);
//
//		currentSession.getTransaction().commit();
//		currentSession.close();
//		
//		SessionFactory sessionFactory = currentSession.getSessionFactory();
//		sessionFactory.close();
//		
//	}


}
