############################################################
#####Analysis for Cancer Targetome Publication
############################################################
#Assessment of drugs, targets, interactions, and evidence for 141 antineoplastic drugs
#queried from the Cancer Targetome database. 
#Generates figures for the TIPS paper, citation below. Figures labeled according to final ordering in accepted
#manuscript.
#Script authored by: Aurora S. Blucher
#Created: 11/22/16
#Updated: 07/06/17
#Script clean up and Cxommenting: 10/10/17 for TIPS paper publication 
#CITATION INFORMATION
#Blucher, A. S., Choonoo, G., Kulesz-Martin, M., Wu, G. & McWeeney, S. K. 
#Evidence-Based Precision Oncology with the Cancer Targetome. Trends Pharmacol. Sci. (2017).
#doi:10.1016/j.tips.2017.08.006

library(dplyr)
library(reshape2)
library(ggplot2)
library(scales) 
library(tidyr)

#set working dir to folder with full evidence file
setwd("~/git/Druggability/results_070617")

############################################################
#####Drug-Target Interaction Evidence From Cancer Targetome
############################################################
#full evidence file for drug-target interactions
allInteractionData<-read.csv(file="Targetome_FullEvidence_070617.txt",header = TRUE,sep = "\t") 
#View(allInteractionData)
#drug info, class name, etc.
alldrugData<-read.csv(file = "Targetome_DrugInformation_070617.txt", header= TRUE, sep="\t")
#View(alldrugData)

####FILTER TO ANTINEOPLASTIC DRUGS ONLY AND TIDYING UP EVIDENCE FILE
#Filter drug data to drug set = ATC Class L drugs only (141 total drugs)
cancerDrugs<-alldrugData%>% filter(grepl("^L", ATC_ClassID)) 
#send cancer drug list to file
#write.table(sort(cancerDrugs$Drug), "TargetomeDrugs_ATC_ClassL_070617.txt", sep="\t", row.names = FALSE, quote=FALSE, col.names = FALSE)
dim(cancerDrugs)#141 drugs
#excluded drugs to file
cancerDrugsExcluded<-alldrugData%>% filter(!grepl("^L", ATC_ClassID))
dim(cancerDrugsExcluded)#30 drugs excluded
#View(cancerDrugsExcluded)
#write.table(sort(cancerDrugsExcluded$Drug), "TargetomeDrugs_ExcludedFromAnalysis_070617.txt", sep="\t", row.names = FALSE, quote=FALSE, col.names = FALSE)

####ALL INTERACTION DATA
#Filter interaction data to target species = human
#Cancer Targetome data collection included check
#for any human uniprots that weren't identified by the resource 
allInteractionData_human<-allInteractionData %>% filter(Target_Species=="Homo sapiens")
#filter interaction data to just selected cancerdrugs from above
drugInteractions<-allInteractionData_human %>%filter(Drug %in% cancerDrugs$Drug)
#View(drugInteractions)  #15,226 entries

#Fix Kd, KD level, make sure to clean this up in next Cancer Targetome update
levels(drugInteractions$Assay_Type)<-list(KD=c("KD", "Kd"), Ki=c("Ki"), IC50=c("EC50"), EC50=c("EC50"), null=c("null"))
levels(drugInteractions$Assay_Type) #KD, Ki, IC50, EC50, or null

#add RNA/DNA to target name in file
drugInteractions_mut<-drugInteractions%>%mutate(Target_Name1=ifelse(Target_Type=="DNA", "DNA", as.character(Target_Name)) )
drugInteractions_mut<-drugInteractions_mut%>%mutate(Target_UniProt1=ifelse(Target_Type=="DNA", "NA_DNA", as.character(Target_UniProt)) )
drugInteractions_mut2<-drugInteractions_mut%>%mutate(Target_Name1=ifelse(Target_Type=="RNA", "RNA", as.character(Target_Name1)) )
drugInteractions_mut2<-drugInteractions_mut2%>%mutate(Target_UniProt1=ifelse(Target_Type=="RNA", "NA_RNA", as.character(Target_UniProt1)) )
drugInteractions_fixed<-drugInteractions_mut2 %>% select(Drug, Target_Name=Target_Name1, Target_Type, Target_UniProt=Target_UniProt1, Target_Species, Database, Reference, Assay_Type, Assay_Relation, Assay_Value, EvidenceLevel_Assigned)
drugInteractions<-drugInteractions_fixed#rename
#View(drugInteractions)#check, looks good

#write to file, cleaned up version for others to use
#write.table(drugInteractions, "Targetome_Evidence_TIPS_101017.txt", sep="\t", row.names = FALSE, quote=FALSE, col.names = TRUE)

####DRUG, TARGET, INTERACTION DATABASE COVERAGE, ALONE AND AGGREGATED
#Aggregated stats
numDrugsAll<-nlevels(factor(drugInteractions$Drug)) #137 drugs (141 total but didn't retrieve targets for all)
numDrugsAll
numTargetsAll<-nlevels(factor(drugInteractions$Target_UniProt)) #658 targets
numTargetsAll
nlevels(factor(drugInteractions$Target_Name))  #657
nlevels(factor(drugInteractions$Database)) #4
drugInteractionsGrouped<- drugInteractions %>% group_by(Drug, Target_UniProt)
interactionsCount<-summarize(drugInteractionsGrouped, count=n())#get unique interactions
numInteractionsAll<-nrow(interactionsCount) #6358 total interactions
#bind into table
databaseAll<-"All"
statsAll<-cbind(databaseAll, numDrugsAll, numTargetsAll, numInteractionsAll)
colnames(statsAll)<-c("Database", "Drugs", "Targets", "Interactions") #colnames
View(statsAll)

#Group by each database
#get drug, target counts for each database
drugInteractionsGroupedDatabase<- drugInteractions%>% 
                                  group_by(Database)%>% #group by database
                                  summarize(Drugs=n_distinct(Drug), #get number drugs
                                            Targets=n_distinct(Target_UniProt) #get num targets
                                  ) 

#View(drugInteractionsGroupedDatabase)
#drugbank
interactionsDrugBank<- drugInteractions %>% 
                       filter(Database=="DrugBank") %>%
                       group_by(Drug, Target_UniProt)
numInteractionsDrugBank<-nrow(summarize(interactionsDrugBank, count=n()) )
#ttd
interactionsTTD<- drugInteractions %>% 
                  filter(Database=="Therapeutic Target Database") %>%
                  group_by(Drug, Target_UniProt)
numInteractionsTTD<-nrow(summarize(interactionsTTD, count=n()) )
#iuphar
interactionsIUPHAR<- drugInteractions %>% 
                     filter(Database=="IUPHAR") %>%
                     group_by(Drug, Target_UniProt) 
numInteractionsIUPHAR<-nrow(summarize(interactionsIUPHAR, count=n()) )
#bindingdb
interactionsBindingDB<- drugInteractions %>% 
                        filter(Database=="BindingDB") %>%
                        group_by(Drug, Target_UniProt) 
numInteractionsBindingDB<-nrow(summarize(interactionsBindingDB, count=n()) )
#bind together, do alphabetical ordering for each database
statsInts<-rbind(numInteractionsBindingDB, numInteractionsDrugBank, numInteractionsIUPHAR, numInteractionsTTD)
colnames(statsInts)<-c("Interactions")
#View(statsInts)
#bind with stats from groupings by database
drugInteractionsDatabases<-cbind(drugInteractionsGroupedDatabase, statsInts)
rownames(drugInteractionsDatabases)<-NULL #get rid of carryover row names
#View(drugInteractionsDatabases) #looks correct
#bind with statsAll= from aggregated interactions
fullStats<-rbind(drugInteractionsDatabases, statsAll)
fullStats$Drugs<-as.numeric(as.character(fullStats$Drugs))
fullStats$Targets<-as.numeric(as.character(fullStats$Targets))
fullStats$Interactions<-as.numeric(as.character(fullStats$Interactions))
View(fullStats) 

#reshape data for plotting
#melt to long format
  #create item: Drug, Target
  #create value: Counts of above Items
fullStatsLong<-melt(fullStats, variable.name="Item", value.name="Count" )
#also create version with no interactions
#select just drug and target counts
fullStatsNoInt<-fullStats %>% dplyr::select(-Interactions) #call dplyr::select because of MASS
fullStatsNoIntLong<-melt(fullStatsNoInt, variable.name="Item", value.name="Count" )
#View(fullStatsNoIntLong)

#####PAPER FIGURE. SUPPLEMENTARY FIGURE 1.
#Plot drug, target counts for each database, and aggregated
#Titles and labels re-sized for PNG format
png("TIPS_Supp1.png", width = 5, height = 3.4, units = 'in', res = 300)
coverageByDatabase<-ggplot(data=fullStatsLong, #drug, target, interactions
                           aes(x=Item, y=Count, fill=Database)) + 
                           facet_wrap(~Item, scales="free")+ 
                           geom_bar(stat="identity", position="dodge", colour="black") +  
                           scale_fill_brewer(palette="YlGnBu") + 
                           ylab("Count") +
                           #ggtitle("Drug and Target Coverage, by Database and Aggregated") + 
                           #theme(plot.title = element_text(size=24, face="bold"))+
                           theme(plot.title = element_text(size=10, face="bold"))+
                           theme(axis.title.x=element_blank())+ 
                           theme(axis.text.x=element_blank())+ 
                           #theme(axis.text.x=element_text(size=14, face="bold"))+ use for drugs/targets
                           #theme(strip.text.x = element_text(size = 22, face="bold"))+ 
                           theme(strip.text.x = element_text(size = 8, face="bold"))+ 
                           theme(axis.text.y=element_text(size=8, face="bold"))+ #16
                           theme(axis.title.y=element_text(size=8, face="bold"))+#20
                           theme(legend.title=element_text(size=8, face="bold"))+#20
                           theme(legend.text=element_text(size=8)) + #16
                           theme(legend.position="bottom", legend.background=element_rect(colour = "black"))+
                           guides(fill=guide_legend(title="Resource",nrow=2))
coverageByDatabase
dev.off()

####DRUG, TARGET, INTERACTIONS BY EVIDENCE LEVEL
#levels I, II, III
drugInteractions_123Drugs<-nlevels(factor(drugInteractions$Drug)) #drugs
drugInteractions_123Targets<-nlevels(factor(drugInteractions$Target_UniProt)) #targets
drugInteractionsGrouped<- drugInteractions %>% 
                          group_by(Drug, Target_UniProt, Target_Name) %>%
                          summarize(count=n())
drugInteractions_123Ints<-nrow(drugInteractionsGrouped) #interactions
evidenceStats_123<-cbind("Levels I, II, III", drugInteractions_123Drugs, drugInteractions_123Targets, drugInteractions_123Ints)
colnames(evidenceStats_123)<-NULL

#levels II and III
drugInteractions_23<-drugInteractions%>% filter(EvidenceLevel_Assigned=="II" | EvidenceLevel_Assigned=="III")
drugInteractions_23Drugs<-nlevels(factor(drugInteractions_23$Drug)) #drugs
drugInteractions_23Targets<-nlevels(factor(drugInteractions_23$Target_UniProt)) #targets
drugInteractionsGrouped_23<- drugInteractions_23 %>% 
                             group_by(Drug, Target_UniProt) %>%
                             summarize(count=n())
drugInteractions_23Ints<-nrow(drugInteractionsGrouped_23) #interactions
evidenceStats_23<-cbind("Levels II, III", drugInteractions_23Drugs, drugInteractions_23Targets, drugInteractions_23Ints)
colnames(evidenceStats_23)<-NULL

#level III all
drugInteractions_3<-drugInteractions%>% filter(EvidenceLevel_Assigned=="III")
drugInteractions_3Drugs<-nlevels(factor(drugInteractions_3$Drug)) #drugs
drugInteractions_3Targets<-nlevels(factor(drugInteractions_3$Target_UniProt)) #targets
drugInteractionsGrouped_3<- drugInteractions_3 %>% 
                            group_by(Drug, Target_UniProt) %>%
                            summarize(count=n())
drugInteractions_3Ints<-nrow(drugInteractionsGrouped_3) #interactions
evidenceStats_3<-cbind("Level III", drugInteractions_3Drugs, drugInteractions_3Targets, drugInteractions_3Ints)
colnames(evidenceStats_3)<-NULL

#level 3 EXACT RELATION ONLY
drugInteractions_3exact<-drugInteractions_3%>% filter(Assay_Relation=="=")
drugInteractions_3exactDrugs<-nlevels(factor(drugInteractions_3exact$Drug)) #drugs
drugInteractions_3exactTargets<-nlevels(factor(drugInteractions_3exact$Target_UniProt)) #targets
drugInteractionsGrouped_3exact<- drugInteractions_3exact %>% 
                                 group_by(Drug, Target_UniProt) %>%
                                 summarize(count=n())
drugInteractions_3exactInts<-nrow(drugInteractionsGrouped_3exact) #interactions
evidenceStats_3exact<-cbind("Level III Exact", drugInteractions_3exactDrugs, drugInteractions_3exactTargets, drugInteractions_3exactInts)
colnames(evidenceStats_3exact)<-NULL

#level 3 thresholds, added Wed 11/16/16 note these thresholds are across all assay types, technically
                                        #assay types are not comparable like this
                                        #figure intended as overview/for coverage assessment only
drugInteractions_3exact$Assay_Value<-as.numeric(as.character(drugInteractions_3exact$Assay_Value))
#threshold to 10000 nM
drugInteractions_3exact_10000<-drugInteractions_3exact%>% filter(Assay_Value<10000) 
drugInteractions_3exact_10000Drugs<-nlevels(factor(drugInteractions_3exact_10000$Drug)) #drugs
drugInteractions_3exact_10000Targets<-nlevels(factor(drugInteractions_3exact_10000$Target_UniProt)) #targets
drugInteractions_3exact_10000Grouped<- drugInteractions_3exact_10000 %>% 
                                       group_by(Drug, Target_UniProt) %>%
                                       summarize(count=n())
drugInteractions_3exact_10000Ints<-nrow(drugInteractions_3exact_10000Grouped) #interactions
evidenceStats_3exact_10000<-cbind("Level III Exact<10,000nM", drugInteractions_3exact_10000Drugs, drugInteractions_3exact_10000Targets, drugInteractions_3exact_10000Ints)
#threshold to 1000nM
drugInteractions_3exact_1000<-drugInteractions_3exact%>% filter(Assay_Value<1000)  
drugInteractions_3exact_1000Drugs<-nlevels(factor(drugInteractions_3exact_1000$Drug)) #drugs
drugInteractions_3exact_1000Targets<-nlevels(factor(drugInteractions_3exact_1000$Target_UniProt)) #targets
drugInteractions_3exact_1000Grouped<- drugInteractions_3exact_1000 %>% 
                                      group_by(Drug, Target_UniProt) %>%
                                      summarize(count=n())
drugInteractions_3exact_1000Ints<-nrow(drugInteractions_3exact_1000Grouped) #interactions
evidenceStats_3exact_1000<-cbind("Level III Exact<1000nM", drugInteractions_3exact_1000Drugs, drugInteractions_3exact_1000Targets, drugInteractions_3exact_1000Ints)
#threshold to 100nM
drugInteractions_3exact_100<-drugInteractions_3exact%>% filter(Assay_Value<100)   
drugInteractions_3exact_100Drugs<-nlevels(factor(drugInteractions_3exact_100$Drug)) #drugs
drugInteractions_3exact_100Targets<-nlevels(factor(drugInteractions_3exact_100$Target_UniProt)) #targets
drugInteractions_3exact_100Grouped<- drugInteractions_3exact_100 %>% 
                                     group_by(Drug, Target_UniProt) %>%
                                     summarize(count=n())
drugInteractions_3exact_100Ints<-nrow(drugInteractions_3exact_100Grouped) #interactions
evidenceStats_3exact_100<-cbind("Level III Exact<100nM", drugInteractions_3exact_100Drugs, drugInteractions_3exact_100Targets, drugInteractions_3exact_100Ints)

#bind evidence stats from each level
evidenceStats_combined<-as.data.frame(rbind(evidenceStats_123, 
                                            evidenceStats_23, 
                                            evidenceStats_3, 
                                            evidenceStats_3exact, 
                                            evidenceStats_3exact_10000, 
                                            evidenceStats_3exact_1000, 
                                            evidenceStats_3exact_100))
colnames(evidenceStats_combined)<-c("AnalysisGroup", "Drugs", "Targets", "Interactions")
#View(evidenceStats_combined)
evidenceStats_combined$Drugs<-as.numeric(as.character(evidenceStats_combined$Drugs))
evidenceStats_combined$Targets<-as.numeric(as.character(evidenceStats_combined$Targets))
evidenceStats_combined$Interactions<-as.numeric(as.character(evidenceStats_combined$Interactions))
#re-order the factors for analysis groups, make it more clear for graphs
evidenceStats_combined$AnalysisGroup<-factor(evidenceStats_combined$AnalysisGroup, levels=c("Levels I, II, III", "Levels II, III", "Level III", "Level III Exact", "Level III Exact<10,000nM", "Level III Exact<1000nM", "Level III Exact<100nM"))
#RESHAPE to long format
  #id var = analysis group  
  #value var = count of each analysis group
evidenceStats_combinedLong<-melt(evidenceStats_combined, id.var="AnalysisGroup", variable.name="Item", value.name ="Count" )
#View(evidenceStats_combinedLong)


#####PAPER FIGURE. FIGURE 2.
#Drug, Target, Interactions by Evidence Level
png("TIPS_Fig2.png", width = 5, height = 3.4, units = 'in', res = 300)
coverageByEvidenceLevel<-ggplot(data=evidenceStats_combinedLong, 
                                aes(x=AnalysisGroup, y=Count, fill=AnalysisGroup)) + 
                                geom_bar(stat="identity", position="dodge", colour="black") + 
                                facet_wrap(~Item, scales="free") + #facet horizontally
                                scale_fill_brewer(palette="YlGnBu") + 
                                ylab("Count") +
                                #ggtitle("Number of Drugs, Targets, and Interactions by Supporting Evidence Level") + 
                                #ggtitle("(D)")+
                                theme(axis.title.x=element_blank(),
                                      axis.text.x=element_blank(),
                                      axis.ticks.x=element_blank()) + 
                                theme(strip.text.x = element_text(size = 8, face="bold"))+ 
                                #theme(plot.title = element_text(size=24, face="bold"))+
                                theme(axis.text.y=element_text(size=8, face="bold"))+
                                theme(axis.title.y=element_text(size=8, face="bold"))+
                                theme(legend.title=element_text(size=6, face="bold"))+
                                theme(legend.text=element_text(size=6)) + 
                                theme(legend.position="bottom", legend.background=element_rect(colour = "black"))+
                                guides(fill=guide_legend(title="Level of Evidence",nrow=3))
coverageByEvidenceLevel
dev.off()

####ANALYSIS BY APPROVAL DATE
#Examine each drug by number of targets, ordered by approval date
cancerDrugsWithDates<-cancerDrugs %>% 
                      filter(Approval_Date !="null") #exclude null approval dates
dim(cancerDrugsWithDates) #136 drugs --- 5 excluded
cancerDrugsWithDates<-cancerDrugsWithDates%>%dplyr::select(Drug, Approval_Date, ATC_ClassName)

#get unique interactions
  #currently subsetting to protein interactions * check this
#drugInteractionsProteins<-drugInteractions%>%
#                          filter(Target_Type=="Protein")
#drugInteractionsGrouped<- drugInteractionsProteins %>% group_by(Drug, Target_UniProt)
drugInteractionsGrouped<- drugInteractions%>% group_by(Drug, Target_UniProt) #updated Mon 12/12
interactionsCount<-summarize(drugInteractionsGrouped, count=n())
drugToTargetCounts<-summarize(interactionsCount, NumTargetsTested=n())
dim(drugToTargetCounts) #126 drugs
numInteractionsAll<-nrow(interactionsCount) #count number of interactions 

#join drugToTargetCounts and cancer drug dates
#use left join to preserve all drugs in drugToTargetCounts
joinedDrugToTargetCountsByDate<-left_join(drugToTargetCounts, cancerDrugsWithDates)
dim(joinedDrugToTargetCountsByDate)
joinedDrugToTargetCountsByDate$Approval_Date<-as.Date(as.character(joinedDrugToTargetCountsByDate$Approval_Date))
dim(joinedDrugToTargetCountsByDate) #137 by 4
#graph each drug: number of targets BY approval date, color the protein kinase inhibitors
byDate<-ggplot(joinedDrugToTargetCountsByDate, 
               aes(x=Approval_Date, y=NumTargetsTested, fill=ATC_ClassName=="Protein kinase inhibitors")) +
               geom_point(shape=21,size=4) + #fill needs shapes 21-25
               scale_x_date(labels = date_format("%Y"), breaks=date_breaks("5 years")) + #breaks = date_breaks("months")
               scale_color_brewer(palette="YlGnBu") +  #use b/c continuous fill
               theme(axis.text.x=element_text(angle=90, hjust=1))+ #turn horizontally
               xlab("FDA Approval Year") + ylab("NUmber Targets Tested") +
               ggtitle("Number of Targets Tested Per Drug, by Drug Approval Year, Across Levels I, II, & III") + 
               guides(fill=FALSE)

byDate  

#more accurate if we look only at level III evidence - this is what is actually experimentally tested
drugInteractions_level3<-drugInteractions%>% filter(EvidenceLevel_Assigned=="III")
drugInteractionsGrouped_level3<- drugInteractions_level3%>% 
                                 group_by(Drug, Target_UniProt)
interactionsCount_level3<-summarize(drugInteractionsGrouped_level3, 
                                    count=n(), 
                                    NumDistinctRef = n_distinct(Reference))
#View(interactionsCount_level3) #drug->target, #evidence, #distint references
drugToTargetCounts_level3<-summarize(interactionsCount_level3, 
                                     NumTargetsTested=n())
#join with cancer drug information
joinedDrugToTargetCountsByDate_level3<-left_join(drugToTargetCounts_level3, cancerDrugsWithDates)
joinedDrugToTargetCountsByDate_level3$Approval_Date<-as.Date(as.character(joinedDrugToTargetCountsByDate_level3$Approval_Date))
dim(joinedDrugToTargetCountsByDate_level3) #only 86 by 4?
#View(joinedDrugToTargetCountsByDate_level3)


#####PAPER FIGURE. SUPPLEMENTARY FIGURE 3.
png("TIPS_Supp2.png", width = 5, height = 3.4, units = 'in', res = 300)
byDatePlot<-ggplot(joinedDrugToTargetCountsByDate_level3, #use for paper graph - level 3 only
                   aes(x=Approval_Date, y=NumTargetsTested, fill=ATC_ClassName))+
                  geom_point(shape=21,size=5) + #fill needs shapes 21-25
                  scale_x_date(labels = date_format("%Y"), breaks=date_breaks("5 years")) + 
                  theme(axis.text.x=element_text(angle=90, hjust=1))+ #flip X axis labels horizontally
                  xlab("FDA Approval Year") + 
                  ylab("Number Targets Tested") +
                  scale_y_continuous(breaks=c(0,25, 50, 100,150,200,250,300,350,375))+
                  #ggtitle("Number of Targets Tested Per Drug by Drug Approval Year, \n Evidence Level III only")+
                  #ggtitle("(E)")+
                  #theme(plot.title = element_blank())+
                  #theme(plot.title=element_text(size=8, face="bold"))+
                  theme(axis.title.x=element_text(size=14, face="bold"))+
                  theme(axis.title.y=element_text(size=14, face="bold"))+
                  theme(axis.text.x=element_text(size=12, face="bold"))+
                  theme(axis.text.y=element_text(size=12, face="bold"))+
                  theme(legend.title=element_text(size=10, face="bold"))+
                  theme(legend.text=element_text(size=10, face="bold"))+
                  guides(fill=guide_legend(title="Anatomical Therapeutic Chemical Classification",ncol=2))+
                  theme(legend.justification = c(0, 1), legend.position = c(0, 1), legend.background = element_rect(colour = "black"))
byDatePlot
dev.off()

############################################################
#####Highly Tested Drug Subset
############################################################
#Here we examine those drugs with more than 300 targets tested by experimental binding assays
drugsHighlyTested<-drugToTargetCounts_level3%>%filter(NumTargetsTested>300)
#get highly tested drugs, 15 drugs total
drugInteractions_highlyTested<- drugInteractions%>% filter (Drug %in% drugsHighlyTested$Drug)
drugInteractions_highlyTested$Assay_Value<-as.numeric(as.character(drugInteractions_highlyTested$Assay_Value))
#subset to level III exact
highlyTestedExact<-drugInteractions_highlyTested%>%filter (EvidenceLevel_Assigned=="III" & Assay_Relation=="=")
highlyTestedExact$Assay_Value<-as.numeric(as.character(highlyTestedExact$Assay_Value))
highlyTestedSummarized<- drugInteractions_highlyTested %>%
                         group_by(Drug) %>%
                         summarize(NumTargets=n_distinct(Target_UniProt)) 
#exact values only
highlyTestedExactSummarized<- highlyTestedExact %>%
                              group_by(Drug) %>%
                              summarize(NumTargets=n_distinct(Target_UniProt)) 
#under 10,000nM
highlyTestedExactT1Summarized<- highlyTestedExact %>%
                                filter(Assay_Value<10000) %>%
                                group_by(Drug) %>%
                                summarize(NumTargets=n_distinct(Target_UniProt)) 
#under 1000nM
highlyTestedExactT2Summarized<- highlyTestedExact %>%
                                filter(Assay_Value<1000) %>%
                                group_by(Drug) %>%
                                summarize(NumTargets=n_distinct(Target_UniProt)) 
#under 100nM
highlyTestedExactT3Summarized<- highlyTestedExact %>%
                                filter(Assay_Value<100) %>%
                                group_by(Drug) %>%
                                summarize(NumTargets=n_distinct(Target_UniProt)) 
#bindtogether the target counts
highlyTestedTargetCounts<-cbind(highlyTestedSummarized, #all highly tested, level III
                                highlyTestedExactSummarized$NumTargets, #level III exact
                                highlyTestedExactT1Summarized$NumTargets,#level III exact <10,000nM
                                highlyTestedExactT2Summarized$NumTargets, #level III exact<1000nM
                                highlyTestedExactT3Summarized$NumTargets) #level III exact <100nM
colnames(highlyTestedTargetCounts)<-c("Drug", "Level III", "Level III Exact", "Level III Exact<10000nM", "Level III Exact<1000nM", "Level III Exact<100nM")
#melt to long format
  #variable name=threshold level
  #value = target counts
highlyTestedTargetCountsLong<-melt(highlyTestedTargetCounts, variable.name="Threshold_Level", value.name="Target_Count")

#####PAPER FIGURE. SUPPLEMENTARY FIGURE 4
barGraphHighlyTestedTargetCounts<-ggplot(data=highlyTestedTargetCountsLong, 
                                         aes(x=Threshold_Level, y=Target_Count, fill=Threshold_Level)) + 
                                         geom_bar(stat="identity", colour="black") + 
                                         facet_wrap(~Drug) + #facet by drug horizontally
                                         #guides(fill=FALSE)+
                                         scale_fill_brewer(palette="YlOrBr") + 
                                         ylab("Number of Targets") +
                                         theme(axis.title.x=element_blank(), #remove x-axis labels
                                         axis.text.x=element_blank(),
                                         axis.ticks.x=element_blank()) + 
                                         theme(strip.text.x = element_text(size = 16, face="bold"))+ 
                                         theme(axis.text.y=element_text(size=16, face="bold"))+
                                         theme(axis.title.y=element_text(size=20, face="bold"))+
                                         theme(plot.title=element_text(size=24, face="bold"))+ 
                                         theme(legend.title=element_text(size=14, face="bold"))+
                                         theme(legend.text=element_text(size=14))+
                                         #theme(legend.position="bottom") +
                                         guides(fill=guide_legend(title="Binding Affinity Treshold",ncol=1))
                                         #theme(legend.justification = c(1, 0), legend.position = c(1, 0),legend.background = element_rect(colour = "black"))
                                         #ggtitle("(F)")
                                         #ggtitle("Number of Targets for Highly Tested Subset of Drugs, \n Supporting Evidence Level III")

barGraphHighlyTestedTargetCounts

#Subset highlight tested to <1000
highlyTestedExact_1000<-highlyTestedExact%>% filter(Assay_Value<1000)
 
############################################################
#####IMATINIB AND VANDETANIB USE CASES
############################################################
#PAPER FIGURES 3A Imatinib and 3B Vandetanib. 
#For each drug, we examine all binding evidence under 100nM (and therefore potentially clinically relevant)
#Importantly, we facet by ASSAY TYPE, as each binding assay type can contribute different target information
#In both cases we see the contribution of assay type information and how different binding assay types
#may affect drug-target prioritization.

#####COLOR PALETTES
#Custom color palettes from: https://personal.sron.nl/~pault/colourschemes.pdf
#08/03/17 for paper revisions
#function to display the pallete colors
pal <- function(col, border = "light gray", ...){
  n <- length(col)
  plot(0, 0, type="n", xlim = c(0, 1), ylim = c(0, 1),
       axes = FALSE, xlab = "", ylab = "", ...)
  rect(0:(n-1)/n, 0, 1:n/n, 1, col = col, border = border)
}
#use for imatinib, which has 14 targets
tol14rainbow=c("#882E72", "#B178A6", "#D6C1DE", #ABL1, ABL2, CA1
               "#1965B0", "#5289C7", "#7BAFDE", #CA2, CA9, CSF1R
               "#4EB265", "#90C987", "#CAE0AB", #DDR1, DDR2, EGFR
               "#F7EE55", "#F6C141", "#F1932D", #KIT, LCK, PDGFRA
               "#DC050C", "#E8601C")            #PDGFRB, SLC47A1
pal(tol14rainbow)#show paletee in plot viewer
#use for vandetanib, which has 26 targets
tol21rainbow= c("#771155", "#AA4488", "#CC99BB", "#114477", "#4477AA", "#77AADD", "#117777", "#44AAAA", "#77CCCC", "#117744", "#44AA77", "#88CCAA", "#777711", "#AAAA44", "#DDDD77", "#774411", "#AA7744", "#DDAA77", "#771122", "#AA4455", "#DD7788")
tol27rainbow= c("#771155", "#AA4488", "#CC99BB", 
                "#114477", "#4477AA", "#77AADD","#8B8682", "#CDC5BF", "#EEE5DE",
                "#117777", "#44AAAA", "#77CCCC", "#8B3626", "#CD4F39","#FF6347", 
                "#777711", "#AAAA44", "#DDDD77", 
                "#774411", "#AA7744", "#DDAA77", 
                "#117744", "#44AA77", "#88CCAA", 
                "#771122", "#AA4455", "#DD7788")
pal(tol27rainbow)
tol27rainbow_alt=c("#882E72", "#B178A6", "#114477",  #ABL1, ABL2,BLK
                   "#4EB265",                        #DDR1,
                   "#CAE0AB",                        #EGFR
                   "#8B8682", "#CDC5BF", "#EEE5DE",  #EPHA6, EPHA8, EPHAB6
                   "#117777", "#44AAAA", "#77CCCC",  #FGFR2, FLT1, FLT3, 
                   "#8B3626", "#CD4F39","#FF6347",   #GAK, IRAK4, KDR
                   "#F7EE55", "#F6C141",             #KIT, LCK, 
                   "#777711",                        #MAP2K5,
                   "#DC050C",                        #PDGFRB, 
                   "#AAAA44", "#DDDD77",             #PTK6, RET
                   "#771122", "#AA4455", "#DD7788",  #RIPK2, SLK, SRC
                   "#117744", "#44AA77", "#88CCAA")  #STK10, STK35, TYRO3

pal(tol27rainbow_alt)#let's see if this looks okay with vandetanib

#####VANDETANIB
vandetanib_interactions<-drugInteractions%>% filter(Drug=="Vandetanib")
#View(vandetanib_interactions)
#now filter to assay relation exact, assay value under 1000 and 100
vandetanib_interactions$Assay_Value<-as.numeric(as.character(vandetanib_interactions$Assay_Value))
#vandetanib_interactions_1000<-vandetanib_interactions %>% filter(Assay_Relation=="=" & Assay_Value <=1000)
#View(vandetanib_interactions_1000)
vandetanib_interactions_100<-vandetanib_interactions %>% filter(Assay_Relation=="=" & Assay_Value <=100)
#View(vandetanib_interactions_100) 
vandetanib_interactions_100_dis<-vandetanib_interactions_100 %>% 
  distinct(Drug, Target_UniProt, Target_Name, Database, Assay_Type, Assay_Value, Assay_Relation, Reference)
#View(vandetanib_interactions_100_dis)

#####PAPER FIGURE. 3B Vandetanib.
png("TIPS_Box1FigureII_edits.png", width = 4.8, height = 3.75, units = 'in', res = 300)
vandetanib_under100_AllAssays<-ggplot(vandetanib_interactions_100_dis, 
                                      aes(x=Assay_Value, fill=Target_Name))+
                                      scale_fill_manual(values = tol27rainbow)+
                                      facet_grid(Assay_Type~.) + #facet by assay type 
                                      geom_histogram(binwidth = 1.0, colour="black") + 
                                      ylab("Count") +
                                      xlab("Binding Assay Value (nM), under 100nM")+
                                      scale_x_continuous(breaks=c(0,5, 10, 15, 20, 25, 50, 75, 100))+ 
                                      scale_y_continuous(breaks=c(0,1,2,3,4,5,6, 7, 8))+
                                      theme(axis.title.x=element_text(size=8, face="bold"))+
                                      theme(axis.text.x=element_text(size=8, face="bold"))+
                                      theme(axis.text.y=element_text(size=8, face="bold"))+ 
                                      theme(axis.title.y=element_text(size=8, face="bold"))+ 
                                      theme(strip.text.y = element_text(size = 8, face="bold"))+ 
                                      theme(plot.title=element_text(size=10, face="bold"))+ 
                                      #ggtitle("Vandetanib Assay Value by Assay Type, under 100nM \n Colored By Target Name, BinWidth=1nM")+
                                      ggtitle("B. Vandetanib")+
                                      #theme(legend.title=element_text(size=14, face="bold"))+
                                      #theme(legend.text=element_text(size=14))+
                                      theme(legend.position="bottom") +
                                      guides(fill=guide_legend(title="Target",nrow=3))+
                                      theme(legend.key.size = unit(0.4, "cm"), 
                                            legend.margin = unit(0, "cm"),
                                            legend.title = element_text(size = 4, face = "bold"),
                                            legend.text = element_text(size = 4))
                                    vandetanib_under100_AllAssays
dev.off()

####IMATINIB
imatinib_interactions<-drugInteractions%>% filter(Drug=="Imatinib Mesylate")
#View(imatinib_interactions)
imatinib_interactions$Assay_Value<-as.numeric(as.character(imatinib_interactions$Assay_Value))
#imatinib_interactions_1000<-imatinib_interactions %>% filter(Assay_Relation=="=" & Assay_Value <=1000)
#View(imatinib_interactions_1000) #98 interactions
imatinib_interactions_100<-imatinib_interactions %>% filter(Assay_Relation=="=" & Assay_Value <=100)
#View(imatinib_interactions_100) #86 pieces of evidence
imatinib_interactions_100_dis<-imatinib_interactions_100 %>% 
  distinct(Drug, Target_Name, Target_UniProt, Database, Assay_Type, Assay_Value, Assay_Relation, Reference)
#View(imatinib_interactions_100_dis)#86 total entries

#####PAPER FIGURE. 3A Imatinib.
png("TIPS_Box1FigureI_edits.png", width = 4.8, height = 3.50, units = 'in', res = 300)
imatinib_under100_AllAssays<-ggplot(imatinib_interactions_100_dis, #use for full assay plot
                                    aes(x=Assay_Value, fill=Target_Name))+
                                    scale_fill_manual(values = tol14rainbow)+
                                    facet_grid(Assay_Type~.) + #facet by assay type
                                    geom_histogram(binwidth = 1.0, colour="black") + 
                                    ylab("Count") +
                                    xlab("Binding Assay Value (nM), under 100nM")+
                                    scale_x_continuous(breaks=c(0,5, 10, 15, 20, 25, 50, 75, 100))+ 
                                    scale_y_continuous(breaks=c(0,1,2,3,4,5,6, 7, 8))+
                                    theme(axis.title.x=element_text(size=8, face="bold"))+
                                    theme(axis.text.x=element_text(size=8, face="bold"))+
                                    theme(axis.text.y=element_text(size=6, face="bold"))+ 
                                    theme(axis.title.y=element_text(size=8, face="bold"))+ 
                                    theme(strip.text.y = element_text(size = 8, face="bold"))+ 
                                    theme(plot.title=element_text(size=10, face="bold"))+ 
                                    ggtitle("A. Imatinib")+
                                    theme(legend.position="bottom") +
                                    guides(fill=guide_legend(title="Target",nrow=3))+
                                    theme(legend.key.size = unit(0.4, "cm"), 
                                          legend.margin = unit(0, "cm"),
                                          legend.title = element_text(size = 4, face = "bold"),
                                          legend.text = element_text(size = 4))
imatinib_under100_AllAssays
dev.off()

############################################################
#####GENERATE SUMMARY FILES
############################################################
#Summary files for different levels of analysis
#1 - Summarized at drug level
#2 - Summarized at drug-target interaction level
#3 - Sumarized at drug-target interaction level --- by each binding assay type

#Summary File 1A
drugInteractions_SummaryTable1A<-drugInteractions %>%  group_by(Drug) %>% 
                                                       summarize(
                                                                 Total_Databases=n_distinct(Database),
                                                                 Databases=paste(unique(Database), collapse = ", "),
                                                                 Total_Targets=n_distinct(Target_Name),
                                                                 #by each database - working
                                                                 DrugBank_Targets=length(unique(Target_Name[Database=="DrugBank"])),
                                                                 TTD_Targets= length(unique(Target_Name[Database=="Therapeutic Target Database"])),
                                                                 IUPHAR_Targets=length(unique(Target_Name[Database=="IUPHAR"])) ,
                                                                 BindingDB_Targets=length(unique(Target_Name[Database=="BindingDB"])),
                                                                 #by each evidence level -working
                                                                 L1_Targets=length(unique(Target_Name[EvidenceLevel_Assigned=="I"])), 
                                                                 L2_Targets=length(unique(Target_Name[EvidenceLevel_Assigned=="II"])), 
                                                                 L3_Targets=length(unique(Target_Name[EvidenceLevel_Assigned=="III"]))
                                                                 #L3Exact_Targest=length(unique(Target_Name[Assay_Relation=="="])) #this doesn't look corrext
                                                    )
#View(drugInteractions_SummaryTable1A)
#Summary File 1B
#calculate target numbers at evidence level III thresholds
drugInteractions_Level3<-drugInteractions %>% filter (EvidenceLevel_Assigned=="III" & Assay_Relation=="=") #LEVEL 3 AND EXACT
drugInteractions_Level3$Assay_Value<-as.numeric(as.character(drugInteractions_Level3$Assay_Value))
drugInteractions_SummaryTable1B<-drugInteractions_Level3 %>% group_by(Drug) %>%
                                                             summarize(
                                                                       #L3_Exact_Targets=length( unique(Target_Name[Assay_Relation=="="]) ),
                                                                       L3_Exact_Targets=length( unique(Target_Name[Assay_Value!="NA"]) ),
                                                                       L3_Exact_Under10000_Targets = length(unique(Target_Name[Assay_Value<10000])),
                                                                       L3_Exact_Under1000_Targets = length(unique(Target_Name[Assay_Value<1000])),
                                                                       L3_Exact_Under100_Targets = length(unique(Target_Name[Assay_Value<100])), 
                                                                       L3_Exact_Under10_Targets = length(unique(Target_Name[Assay_Value<10])) #not in paper, just to see
                                                             )
#View(drugInteractions_SummaryTable1B)
#Summary File 1 Full
drugInteractions_SummaryTable1<-full_join(drugInteractions_SummaryTable1A, drugInteractions_SummaryTable1B, "Drug")
#View(drugInteractions_SummaryTable1)


#SUMMARY FILE NUMBER 2 - drug->target interactions->num databases, list those databases
#                                                 ->num evidence levels, list evidence levels
#                                                 ->num assay types, list assay types
#                                                 ->best assay (i.e. min), value, relation, database, ref
drugInteractions$Assay_Value<-as.numeric(as.character(drugInteractions$Assay_Value))
drugInteractions_SummaryTable2_test<-drugInteractions %>% 
                                group_by(Drug, Target_Name, Target_UniProt) %>%
                                summarize(Num_Databases=n_distinct(Database),
                                          Databases = paste(unique(Database), collapse=", "),
                                          Num_EvidenceLevels=n_distinct(EvidenceLevel_Assigned),
                                          EvidenceLevels = paste(unique(EvidenceLevel_Assigned), collapse=", "),
                                          Num_AssayTypes=n_distinct(Assay_Type, na.rm=TRUE),
                                          AssayTypes = paste(unique(Assay_Type, na.rm=TRUE), collapse=", "),
                                          #added Wed 12/14, best assay type
                                          BestAssay=paste(unique(Assay_Value[which(Assay_Value == min(Assay_Value, na.rm=TRUE))]), collapse = ", "), 
                                          BestAssayRelation=paste(unique(Assay_Relation[which(Assay_Value == min(Assay_Value, na.rm=TRUE))]), collapse = ", "),
                                          BestAssayType=paste(unique(Assay_Type[which(Assay_Value == min(Assay_Value, na.rm=TRUE))]), collapse = ", "),
                                          BestAssayDatabase=paste(unique(Database[which(Assay_Value == min(Assay_Value, na.rm=TRUE))]), collapse = ", "),
                                          Reference_min=paste(unique(Reference[which(Assay_Value == min(Assay_Value, na.rm=TRUE))]), collapse = ", ")
                                          )
#View(drugInteractions_SummaryTable2)
#View(drugInteractions_SummaryTable2_test)

#mutate to clean up factor levels for supporting evidence level
  #I, II and II, I --> I, II
  #I, III and III, I -->I, III
drugInteractions_SummaryTable2mut<-drugInteractions_SummaryTable2_test %>%mutate(EvidenceLevelsClean=ifelse(EvidenceLevels=="II, I", "I, II", as.character(EvidenceLevels)) )
drugInteractions_SummaryTable2mut<-drugInteractions_SummaryTable2mut %>%mutate(EvidenceLevelsClean=ifelse(EvidenceLevels=="III, I", "I, III", as.character(EvidenceLevelsClean)) )
drugInteractions_SummaryTable2mut<-drugInteractions_SummaryTable2mut %>%mutate(EvidenceLevelsClean=ifelse(EvidenceLevels=="II, I, III", "I, II, III", as.character(EvidenceLevelsClean)) )
drugInteractions_SummaryTable2mut<-drugInteractions_SummaryTable2mut %>%mutate(EvidenceLevelsClean=ifelse(EvidenceLevels=="II, III, I", "I, II, III", as.character(EvidenceLevelsClean)) )
#View(drugInteractions_SummaryTable2mut) #checked, looks good
#rename and save
drugInteractions_SummaryTable2<-drugInteractions_SummaryTable2mut
#View(drugInteractions_SummaryTable2)

#SUMMARY FILE NUMBER 3 - drug->target interaction-->by assay type
#                                                 ->assay min, assay max
#                                                 ->relation of min, database of min, reference of min
#                                                 ->ISSUE* when we have ties, need to resolve
#on full set of drug target interactions
drugInteractions$Assay_Value<-as.numeric(as.character(drugInteractions$Assay_Value))
drugInteractions_SummaryTable3<-drugInteractions %>% 
                                group_by(Drug, Target_Name, Target_UniProt, EvidenceLevel_Assigned, Assay_Type) %>% 
                                summarize(
                                          Assay_min=min(Assay_Value, na.rm=TRUE), #only for level III
                                          Assay_max=max(Assay_Value, na.rm=TRUE), #only for level III
                                                                                  #can we also check for exact here??
                                          Assay_Relation_min=paste(unique(Assay_Relation[which(Assay_Value == min(Assay_Value, na.rm=TRUE))]), collapse = ", "), #need all listed
                                          Database_min=paste(unique(Database[which(Assay_Value == min(Assay_Value, na.rm=TRUE))]), collapse = ", "), #OK to use unique here
                                          Reference_min=paste(unique(Reference[which(Assay_Value == min(Assay_Value, na.rm=TRUE))]), collapse = ", ") ) #okay to use unique here
#View(drugInteractions_SummaryTable3)

#write all summary files to folder
#create README.text for these summary files
#commented out 12/23/16, re-run 7/07/17
#write.table(drugInteractions_SummaryTable1, "DrugInteractions_SummaryTable1_070717.txt", sep="\t", row.names = FALSE, quote=FALSE, col.names = TRUE)
#write.table(drugInteractions_SummaryTable2, "DrugInteractions_SummaryTable2_070717.txt", sep="\t", row.names = FALSE, quote=FALSE, col.names = TRUE)
#write.table(drugInteractions_SummaryTable3, "DrugInteractions_SummaryTable3_070717.txt", sep="\t", row.names = FALSE, quote=FALSE, col.names = TRUE)

############################################################
#####ADDITIONAL PLOTS HERE
############################################################
#Results here are cited in main text of paper (data not necessarily shown) or included in the Supplementary Material.
#
#####DRUG COVERAGE BY NUMBER OF DATABASES
DrugDatabaseCoverage<-ggplot(data=drugInteractions_SummaryTable1,
                             aes(x=Total_Databases, fill=factor(Total_Databases)))+ #use this for straight coverage
                             geom_bar(width=.75, colour="black", fill="dodgerblue4")+
                             #scale_fill_brewer(palette="YlGnBu") + 
                             xlab("Total Number of Databases With Drug")+
                             theme(axis.text.x=element_text(size=16, face="bold"))+
                             theme(axis.title.x=element_text(size=20, face="bold"))+
                             ylab("Drugs") +
                             ylim(0,50)+
                             theme(axis.text.y=element_text(size=16, face="bold"))+
                             theme(axis.title.y=element_text(size=20, face="bold"))+
                             #ggtitle("Drug Coverage by Number of Databases") + 
                             ggtitle("(B)") + 
                             theme(plot.title = element_text(size=22, face="bold"))+
                             guides(fill=FALSE)
DrugDatabaseCoverage

#####DRUGS IN ONE DATABASE ONLY
drugsInOnlyOneDatabase<-drugInteractions_SummaryTable1 %>% filter(Total_Databases==1)
#View(drugsInOnlyOneDatabase)
DrugDatabaseCoverage_1<-ggplot(data=drugsInOnlyOneDatabase,
                               aes(x=Databases, fill=factor(Databases)))+
                               geom_bar(width=.75, colour="black")+
                               scale_fill_brewer(palette="YlGnBu") + 
                               xlab("Database With Drug")+
                               theme(axis.text.x=element_text(size=14, face="bold"))+
                               theme(axis.title.x=element_text(size=16, face="bold"))+
                               ylab("Drugs") +
                               ylim(0,15)+
                               theme(axis.text.y=element_text(size=14, face="bold"))+
                               theme(axis.title.y=element_text(size=16, face="bold"))+
                               ggtitle("Database Coverage for Drugs Listed in Only One Databse") + 
                               theme(plot.title = element_text(size=16, face="bold"))+
                               guides(fill=FALSE)
DrugDatabaseCoverage_1

#####TARGET COVERAGE BY NUMBER OF DATABASES
#create target summary file here - for targets by database coverage
targets_SummaryTable1<-drugInteractions %>%  group_by(Target_Name) %>% 
                                             summarize(Total_Databases=n_distinct(Database),
                                                      Databases=paste(unique(Database), collapse = ", ")
                                                       )
#target database coverage
targetDatabaseCoverage<-ggplot(data=targets_SummaryTable1,
                               aes(x=Total_Databases, fill=factor(Total_Databases)))+
                               geom_bar(width=.75, colour="black", fill="dodgerblue4")+
                               #scale_fill_brewer(palette="YlGnBu") +
                               xlab("Total Number of Databases With Target")+
                               theme(axis.text.x=element_text(size=16, face="bold"))+
                               theme(axis.title.x=element_text(size=20, face="bold"))+
                               ylab("Targets") +
                               #ylim(0,50)+
                               theme(axis.text.y=element_text(size=17, face="bold"))+
                               theme(axis.title.y=element_text(size=20, face="bold"))+
                               #ggtitle("Target Coverage by Number of Databases") + 
                               ggtitle("(C)") + 
                               theme(plot.title = element_text(size=22, face="bold"))+
                               guides(fill=FALSE)
targetDatabaseCoverage
#####TARGETS IN ONE DATABASE ONLY
targetsInOnlyOneDatabase<-targets_SummaryTable1 %>% filter(Total_Databases==1)
#View(targetsInOnlyOneDatabase)
targetDatabaseCoverage_1<-ggplot(data=targetsInOnlyOneDatabase,
                                 aes(x=Databases, fill=factor(Databases)))+
                                  geom_bar(width=.75, colour="black")+
                                 scale_fill_brewer(palette="YlGnBu") +
                                  xlab("Database With Target")+
                                  theme(axis.text.x=element_text(size=14, face="bold"))+
                                  theme(axis.title.x=element_text(size=16, face="bold"))+
                                  ylab("Targets") +
                                  #ylim(0,15)+
                                  theme(axis.text.y=element_text(size=14, face="bold"))+
                                  theme(axis.title.y=element_text(size=16, face="bold"))+
                                  ggtitle("Database Coverage for Targets Listed in Only One Databse") + 
                                  theme(plot.title = element_text(size=16, face="bold"))+
                                  guides(fill=FALSE)
targetDatabaseCoverage_1

#####INTERACTIONS BY DATABASE
interactionDatabaseCoverage<-ggplot(data=drugInteractions_SummaryTable2,
                                    aes(x=Num_Databases, fill=factor(Num_Databases)))+
                                    geom_bar(width=.75, colour="black")+
                                    scale_fill_brewer(palette="YlGnBu") +
                                    xlab("Total Number of Databases With Interaction")+
                                    theme(axis.text.x=element_text(size=14, face="bold"))+
                                    theme(axis.title.x=element_text(size=16, face="bold"))+
                                    ylab("Interactions") +
                                    #ylim(0,50)+
                                    theme(axis.text.y=element_text(size=14, face="bold"))+
                                    theme(axis.title.y=element_text(size=16, face="bold"))+
                                    ggtitle("Interaction Coverage by Number of Databases") + 
                                    theme(plot.title = element_text(size=16, face="bold"))+
                                    guides(fill=FALSE)
interactionDatabaseCoverage
#####UBTERACTIONS IN ONE DATABASE ONLY
interactionsInOnlyOneDatabase<-drugInteractions_SummaryTable2 %>% filter(Num_Databases==1)
View(interactionsInOnlyOneDatabase)
interactionDatabaseCoverage_1<-ggplot(data=interactionsInOnlyOneDatabase,
                                      aes(x=Databases, fill=factor(Databases)))+
                                      geom_bar(width=.75)+
                                      xlab("Database With Interaction")+
                                      theme(axis.text.x=element_text(size=14, face="bold"))+
                                      theme(axis.title.x=element_text(size=16, face="bold"))+
                                      ylab("Interactions") +
                                      #ylim(0,15)+
                                      theme(axis.text.y=element_text(size=14, face="bold"))+
                                      theme(axis.title.y=element_text(size=16, face="bold"))+
                                      ggtitle("Database Coverage for Interactions Listed in Only One Databse") + 
                                      theme(plot.title = element_text(size=16, face="bold"))+
                                      guides(fill=FALSE)
interactionDatabaseCoverage_1

#####DRUGS ACCORDING TO NUMBER OF TARGETS, BY DATABASE
#drugbank, ttd, iuphar
drugToTargetDist_ByDatabase<-drugInteractions_SummaryTable1 %>% 
                             select(Drug, 
                                    DrugBank_Targets, 
                                    TTD_Targets, 
                                    IUPHAR_Targets)
#bindingDB
drugToTargetDist_BindingDB<-drugInteractions_SummaryTable1 %>%
                            select(Drug, BindingDB_Targets)
#melt to long format
drugToTargetDist_ByDatabaseLONG<-melt(drugToTargetDist_ByDatabase, 
                                      variable.name = "Database", 
                                      value.name = "Target_Count")
#filter our 0 counts for our plot, makes more sense
drugToTargetDist_ByDatabaseLONG_filtered<-drugToTargetDist_ByDatabaseLONG%>%filter(Target_Count>0)
#melt bindingDB to ong format
drugToTargetDist_BindingDBLONG<-melt(drugToTargetDist_BindingDB, 
                                      variable.name = "Database", 
                                      value.name = "Target_Count")
drugToTargetDist_BindingDBLONG_A<-drugToTargetDist_BindingDBLONG %>% filter(Target_Count>0 & Target_Count<30)
drugToTargetDist_BindingDBLONG_B<-drugToTargetDist_BindingDBLONG %>% filter(Target_Count>300)
#plot for drugbank, ttd, iuphar
drugToTargetDist_ByDatabase_Plot<-ggplot(#data=drugToTargetDist_ByDatabaseLONG,
                                  data=drugToTargetDist_ByDatabaseLONG_filtered, #filter out 0 counts
                                  aes(x=Target_Count, fill=Database)) + 
                                  geom_bar( colour="black") + 
                                  facet_wrap(~Database) + #keep scales the same for this comparison
                                  xlab("Target Count")+
                                  theme(axis.text.x=element_text(size=14, face="bold"))+
                                  theme(axis.title.x=element_text(size=16, face="bold"))+
                                  ylab("Drugs") +
                                  theme(axis.text.y=element_text(size=14, face="bold"))+
                                  theme(axis.title.y=element_text(size=16, face="bold"))+
                                  ggtitle("Target Distribution for Drugs in DrugBank, TTD, IUPHAR") + 
                                  theme(plot.title = element_text(size=16, face="bold"))+
                                  guides(fill=FALSE)
drugToTargetDist_ByDatabase_Plot
#bindingDB plot - plots A and B, use commented line
drugToTargetDist_BindingDB_Plot<-ggplot(#data=drugToTargetDist_BindingDBLONG_A,
                                        data=drugToTargetDist_BindingDBLONG_B, #when be makes sure to adjust limits
                                         aes(x=Target_Count)) + 
                                         geom_bar(fill="dodgerblue2", colour="black") + 
                                         xlab("Target Count")+
                                         theme(axis.text.x=element_text(size=14, face="bold"))+
                                         theme(axis.title.x=element_text(size=16, face="bold"))+
                                         ylab("Drugs") +
                                         ylim(0,80)+
                                         theme(axis.text.y=element_text(size=14, face="bold"))+
                                         theme(axis.title.y=element_text(size=16, face="bold"))+
                                         #ggtitle("Target Distribution for Drugs in BindingDB, <30 Targets") + 
                                         ggtitle("Target Distribution for Drugs in BindingDB, >300 Targets") + 
                                         theme(plot.title = element_text(size=16, face="bold"))+
                                        guides(fill=FALSE)
drugToTargetDist_BindingDB_Plot


#####SUBSETS OF INTEREST
#LEVEL III EVIDENCE
LevelIIIAnyFrom2<-drugInteractions_SummaryTable2 %>% filter (EvidenceLevelsClean=="I, III" | EvidenceLevelsClean=="II, III" | EvidenceLevelsClean=="I, II, III"| EvidenceLevelsClean=="III")
#View(LevelIIIAnyFrom2)
#now threshold to within nanomolar binding range (=<100, with =)
LevelIIIAnyFrom2$BestAssay<-as.numeric(as.character(LevelIIIAnyFrom2$BestAssay))
LevelIIIAnyFrom2_100<-LevelIIIAnyFrom2 %>% filter(BestAssay<=100 & BestAssayRelation=="=")
#View(LevelIIIAnyFrom2_100) #529 interactions (explicitly state in main text)

#####PAPER FIGURE. SUPPLEMENTARY FIGURE 2A.Interactions with strong experimental binding evidence, number of databases supported by.
#plot the number of supporting databases these came from (all evidence levels)
LevelIIIAnyFrom2_100Plot2<-ggplot(data=LevelIIIAnyFrom2_100,
                                 aes(x=Num_Databases, fill=factor(Num_Databases)))+
                                  geom_bar(width=.75, colour="black", fill="dodgerblue4")+
                                  xlab("Number Supporting Databases")+
                                  theme(axis.text.x=element_text(size=14, face="bold"))+
                                  theme(axis.title.x=element_text(size=16, face="bold"))+
                                  ylab("Interactions") +
                                  #ylim(0,15)+
                                  theme(axis.text.y=element_text(size=14, face="bold"))+
                                  theme(axis.title.y=element_text(size=16, face="bold"))+
                                  ggtitle("Number of Databases for Interactions \n with Experimental Binding Evidence (=<100nM)") + 
                                  theme(plot.title = element_text(size=16, face="bold"))+
                                  guides(fill=FALSE)
LevelIIIAnyFrom2_100Plot2

#clean up the bestdatabase category
LevelIIIAnyFrom2_100_mut<-LevelIIIAnyFrom2_100 %>% mutate(BestAssayDatabaseCleaned=ifelse(BestAssayDatabase=="IUPHAR, BindingDB", "BindingDB, IUPHAR", as.character(BestAssayDatabase)))
#View(LevelIIIAnyFrom2_100_mut)

#####PAPER FIGURE. SUPPLEMENTARY FIGURE 2B.Interactions with strong experimental binding evidence, 
#according to database contributing minimum assay value
LevelIIIAnyFrom2_100Plot3<-ggplot(data=LevelIIIAnyFrom2_100_mut,
                                  aes(x=BestAssayDatabaseCleaned, fill=factor(BestAssayDatabaseCleaned)))+
                                  geom_bar(width=.75, colour="black")+
                                  scale_fill_brewer(palette="YlGnBu") +
                                  xlab("Database Contributing Min Assay Value <100nM")+
                                  theme(axis.text.x=element_text(size=14, face="bold"))+
                                  theme(axis.title.x=element_text(size=16, face="bold"))+
                                  ylab("Interactions") +
                                  #ylim(0,15)+
                                  theme(axis.text.y=element_text(size=14, face="bold"))+
                                  theme(axis.title.y=element_text(size=16, face="bold"))+
                                  ggtitle("Database Contribution of Best Assay Value <=100nM") + 
                                  theme(plot.title = element_text(size=16, face="bold"))+
                                  guides(fill=FALSE)
LevelIIIAnyFrom2_100Plot3

############################################################
#####Generate Target Lists for Pathway Mapping
############################################################
#Get Target Lists by evidence type and binding value
#Run in Reactome Pathway analyzer and process in Java class for calculating light/dark apthways

#all evidence levels
Targets_AllLevels<-as.data.frame(unique(drugInteractions$Target_UniProt))
#View(Targets_AllLevels) 
#levels 2 and 3
Targets_Levels23<-drugInteractions %>% filter(EvidenceLevel_Assigned=="II" | EvidenceLevel_Assigned=="III")
Targets_Levels23<-as.data.frame(unique(Targets_Levels23$Target_UniProt))
#View(Targets_Levels23) 
#level 3 only
Targets_Levels3<-drugInteractions %>% filter(EvidenceLevel_Assigned=="III")
Targets_Levels3<-as.data.frame(unique(Targets_Levels3$Target_UniProt))
#View(Targets_Levels3) 
#level 3 exact
Targets_Levels3exact<-drugInteractions %>% filter(EvidenceLevel_Assigned=="III" & Assay_Relation=="=")
Targets_Levels3exact<-as.data.frame(unique(Targets_Levels3exact$Target_UniProt))
#View(Targets_Levels3exact) 
#level 3 <100nM, any assay type OK
Targets_Levels3exact100<-drugInteractions %>% filter(EvidenceLevel_Assigned=="III" & Assay_Relation=="=" & Assay_Value<100)
Targets_Levels3exact100<-as.data.frame(unique(Targets_Levels3exact100$Target_UniProt))
#View(Targets_Levels3exact100) 

#write to file
#write.table(Targets_AllLevels, "TargetList_LevelsAll_070717.txt", sep="\t", row.names = FALSE, quote=FALSE, col.names =FALSE)
#write.table(Targets_Levels23, "TargetList_Levels23_070717.txt", sep="\t", row.names = FALSE, quote=FALSE, col.names =FALSE)
#write.table(Targets_Levels3, "TargetList_Levels3_070717.txt", sep="\t", row.names = FALSE, quote=FALSE, col.names =FALSE)
#write.table(Targets_Levels3exact, "TargetList_Levels3exact_070717.txt", sep="\t", row.names = FALSE, quote=FALSE, col.names =FALSE)
#write.table(Targets_Levels3exact100, "TargetList_Levels3exact100_070717.txt", sep="\t", row.names = FALSE, quote=FALSE, col.names =FALSE)

#####SIGNALING BY NOTCH PATHWAY 
#Retrieve drug-target interaction information for NOTCH Signaling Pathway
#Used in Box 1 Figure I Figure.
#pathways
pathwayToTarget<-read.csv(file = "PathwayAnalysis/PathwayResults_LevelsAll_070717.csv", header= TRUE, sep=",", stringsAsFactors = FALSE)
#View(pathwayToTarget)
#select pathway for pre-notch expression and processing
#get targets
notchTargets<-pathwayToTarget %>% filter (Pathway.name=="Pre-NOTCH Transcription and Translation")
notchTargets<-notchTargets %>% select(Pathway.name, Submitted.entities.found)
#View(notchTargets)
pathwayToTarget_Table<-notchTargets %>% separate_rows(Submitted.entities.found) #separate 
notchEvidence<-drugInteractions %>% filter(Target_UniProt %in% pathwayToTarget_Table$Submitted.entities.found)
#View(notchEvidence) ####NOTCH EVIDENCE FOR BOX 1 FIGURE 1 NOTCH PATHWAY
