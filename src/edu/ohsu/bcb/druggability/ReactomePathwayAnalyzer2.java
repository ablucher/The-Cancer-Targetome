package edu.ohsu.bcb.druggability;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;//try with jdom2? mon 7/24/16
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
//import org.jdom.Document; //commented out for now
//import org.jdom.Element;
//import org.jdom.input.SAXBuilder;
import org.junit.Test;
import org.reactome.r3.util.FileUtility;


/*
 * Created on Jul 22, 2016
 *
 */


/**
 * @author gwu
 *
 */
public class ReactomePathwayAnalyzer2 {
    //private final String DIR = "resultsV2/"; //AB changed for druggability folder
    private final String DIR = "results_070617/";//updated 7/07/17
    private FileUtility fu = new FileUtility();
    
    /**
     * Default constructor.
     */
    public ReactomePathwayAnalyzer2() {
    }
    
    @Test
    public void removeDuplicates() throws IOException {
        String src = DIR + "Target_Uniprot_7.22.16.txt";
        String target = DIR + "Target_Uniprot_7.22.16_norm.txt";
        Set<String> proteins = fu.loadInteractions(src);
        System.out.println("Total genes: " + proteins.size());
        fu.saveCollection(proteins, target);
    }
    
    @Test
    public void testGetHitPathways() throws IOException {
        List<String> pathways = getHitPathways();
        System.out.println("Total pathways: " + pathways.size());
    }
    
    private List<String> getHitPathways() throws IOException {
    	//AB changed this to pathway results from all drugs
    	//10/20/16 using new pathway results (5 different level combos)
        //String file = DIR + "PathwayAnalysis/PathwayResults_Level3ExIC50_10_20_16.csv";
        //2/06/17 pathway table 2 for resource draft
        //String file = DIR + "PathwayAnalysis/PathwayResults_Level3_02_06_17.csv";
        String file = DIR + "PathwayAnalysis/PathwayResults_Levels3Exact100_070717.csv";
        //07/07/17 updated files
        
        fu.setInput(file);
        String line = fu.readLine();
        List<String> list = new ArrayList<String>();
        while ((line = fu.readLine()) != null) {
            // This regex is got from http://stackoverflow.com/questions/15738918/splitting-a-csv-file-with-quotes-as-text-delimiter-using-string-split
            String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            String pathway = tokens[1];
            if (!pathway.endsWith("\""))
                throw new IllegalStateException("Parsing error: " + line);
            list.add(pathway.substring(1, pathway.length() - 1)); // Remove quotation marks
        }
        fu.close();
        return list;
    }
    
    @Test
    public void generateDarkPathwayView() throws Exception {
        // Use set for quick performance
        Set<String> hitPathways = new HashSet<String>(getHitPathways());
        System.out.println("Total hit pathways: " + hitPathways.size());
        Set<String> totalPathways = getTotalPathways();
        System.out.println("Total pathways: " + totalPathways.size());
        Set<String> unhitPathways = new HashSet<String>(totalPathways);
        unhitPathways.removeAll(hitPathways);
        System.out.println("Unhit pathways: " + unhitPathways.size());
        
        Element treeRoot = loadPathwayTree();
        // We want to create a new tree by copying elements that should be displayed
        Element newTreeRoot = new Element(treeRoot.getName());
        generateNewElements(unhitPathways, treeRoot, newTreeRoot);
        
        //AB, put output file name here, not working
        //String output = DIR + "PathwayAnalysis/DarkPathways_Level3_02_06_17.txt";
        String output = DIR + "PathwayAnalysis/DarkPathways_Levels3Exact100_070717.txt";
        fu.setOutput(output);
        outputTree(newTreeRoot, "", fu);
        fu.close();
    }

    private void outputTree(Element treeRoot,
                            String delim,
                            FileUtility fu) throws IOException {
        // We don't want to show the tree root
        List<Element> children = treeRoot.getChildren();
        for (Element child : children) {
//            System.out.println(delim + child.getAttributeValue("displayName"));
            fu.printLine(delim + child.getAttributeValue("displayName"));
            outputTree(child, "\t" + delim, fu);
        }
    }

    private void generateNewElements(Set<String> unhitPathways, 
                                     Element pathwayElm, 
                                     Element newPathwayElm) {
        List<Element> topLevel = pathwayElm.getChildren();
        for (Element childPathwayElm : topLevel) {
            if (shouldBeKept(childPathwayElm, unhitPathways)) {
                Element newChildPathwayElm = new Element(childPathwayElm.getName());
                String childPathwayName = childPathwayElm.getAttributeValue("displayName");
                newChildPathwayElm.setAttribute("displayName", childPathwayName);
                newPathwayElm.addContent(newChildPathwayElm);
                if (unhitPathways.contains(childPathwayName))
                    continue; // There is no need to drill down
                generateNewElements(unhitPathways, 
                                    childPathwayElm, 
                                    newChildPathwayElm);
            }
        }
    }
    
    private boolean shouldBeKept(Element pathway,
                                 Set<String> keptNames) {
        String pathwayName = pathway.getAttributeValue("displayName");
        if (keptNames.contains(pathwayName))
            return true;
        List<Element> children = pathway.getChildren();
        for (Element child : children) {
            if (child.getName().equals("Pathway"))
                if(shouldBeKept(child, keptNames))
                    return true;
        }
        return false;
    }
    
    @Test
    public void calculateTotalPathways() throws Exception {
        Set<String> totalPathways = getTotalPathways();
        System.out.println("Total pathways: " + totalPathways.size());
    }
    
    private Set<String> getTotalPathways() throws Exception {
        Element tree = loadPathwayTree();
        Set<String> pathways = new HashSet<String>();
        parsePathways(tree, pathways);
        return pathways;
    }
    
    private void parsePathways(Element elemnt, Set<String> pathways) {
        List<Element> children = elemnt.getChildren();
        for (Element child : children) {
            String elementName = child.getName();
            if (elementName.equals("Pathway")) {
                String pathwayName = child.getAttributeValue("displayName");
                pathways.add(pathwayName);
                parsePathways(child, pathways);
            }
        }
    }
    
    private Element loadPathwayTree() throws Exception {
        String fileName = DIR + "homo_sapiens.xml";
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new File(fileName));
        return doc.getRootElement();
    }
    
}
