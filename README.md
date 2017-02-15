# The Cancer Targetome
This is an initial data release for the drug-target interactions of the cancer targetome. All drug-target interaction and bioactivity data aggregated from four publicly available resources: DrugBank, Therapeutic Targets Database, IUPHAR Guide to Pharmacology, and BindingDB. 

### Data Availability and Use
This work is open to public and academic research use. It is made available under the Creative Commons Attribution-Non Commercial Share Alike 4.0 International License. https://creativecommons.org/licenses/by-nc/4.0/legalcode

### Citations
The Cancer Targetome: FDA-Approved Antineoplastic Drugs and Targets from an Evidence Based Perspective. Blucher, A.S., Choonoo, G., Kulesz-Martin, M., Wu, G., McWeeney, S.K. (Manuscript in preparation)

### Accessing the Data
There are several options for accessing the data. Users interested in the data collection and aggregation should see the Java source code, while users looking for just the drug-target interaction data can find it in either as a mysql database dump or an abbreviated CSV file. 
#### Source Code in Java
The source code for collection and aggregation of the drug-target interaction and bioactivity data is in Java (DruggabilityV2). 
#### MySQL Database Dump
The full drug-target interaction database can be downloaded as a mysql database dump. The data schema can be found in. 
#### CSV Drug-Target Interactions File
This is an abbreviated file of drug-target interactions with assigned evidence levels. This is the file used for all analysis and figures generated in the manuscript above. It is accompanied by the R script CancerTargetome_AnalysisFull.R




Please contact Aurora Blucher (blucher@ohsu.edu) with suggestions and feedback.
