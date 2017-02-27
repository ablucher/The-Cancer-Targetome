# The Cancer Targetome
A core tenet of precision oncology is the rational selection of pharmaceutical therapies 
to interact with patient-specific biological targets of interest, but it is currently 
difficult for researchers to obtain consistent and well-supported target information for 
pharmaceutical drugs. To address this gap we have aggregated drug-target interaction and 
bioactivity information for FDA-approved antineoplastic drugs across four publicly available 
resources to create the Cancer Targetome. Our work offers a novel contribution due to 
both the inclusion of putative target interactions encompassing multiple targets for each 
antineoplastic drug and the introduction of a framework for categorizing the supporting 
evidence behind each drug-target interaction.

This is an initial data release for the drug-target interactions of the Cancer Targetome. 
All drug-target interaction and bioactivity data has been aggregated from four 
publicly available resources: DrugBank, Therapeutic Targets Database, IUPHAR Guide to 
Pharmacology, and BindingDB. 

### Data Availability and Use
This work is open for public and academic research use. It is made available under the 
[Creative Commons Attribution-Non Commercial Share Alike 4.0 International License](https://creativecommons.org/licenses/by-nc/4.0/legalcode)

### Citations
The Cancer Targetome: A Critical Step Towards Evidence-Base Precision Oncology. 
Blucher, A.S., Choonoo, G., Kulesz-Martin, M., Wu, G., McWeeney, S.K. (Manuscript in preparation)

### Accessing the Data
There are several options for accessing the Cancer Targetome data. Users interested in the data collection and aggregation
should see the Java source code, while users interested in the final aggregated drug-target interactions can find access data either 
as a mysql database dump or an abbreviated CSV file. 
#### Source Code in Java
The source code for collection and aggregation of the drug-target interaction and bioactivity data is in Java (DruggabilityV2). 
#### MySQL Database Dump
The full drug-target interaction database can be downloaded as a mysql database dump. The database contains all drug-target interaction 
and bioactivity data with parent database, reference and experimental binding evidence lineage. 
#### CSV Drug-Target Interactions File
This is a file of drug-target interactions for 141 antineoplastic drugs. It also includes assigned evidence levels for each
piece of evidence supporting a drug-target interaction.  This file was used for all analysis 
and figures generated in the manuscript under preparation (indicated above). It is accompanied by the
R script CancerTargetome_Analysis.R

### Example Use Case: Imatinib
Imatinib 

Please contact Aurora Blucher (blucher@ohsu.edu) with questions and feedback. 
