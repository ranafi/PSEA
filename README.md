# PSEA
This repository contains the ongoing development version of Phase Set Enrichment Analysis (PSEA). 
The current (as of Dec 2015) executable version is PSEA1.1_VectorGraphics.jar

## Installation
Java version 1.5 or higher is required. 

## Instructions for using Phase Set Enrichment Analysis (PSEA)
#### STEP 1 Select file containing gene names and values. 

A tab-delimited text file which assigns all genes to their respective lab values (for example, phases). Each row contains a gene symbol followed by a numerical value. 

Example:

ARNTL	20

CLOCK	18

PER1	6

PER2	8

CRY1	9

CRY2	8

#### STEP 2 Select file containing gene sets (.gmt format):
A tab-delimited text file defining the gene sets to be considered. Each row contains a gene-set name, a gene-set description, and a list of all gene symbols comprising the gene set.

Example:
CANCER	some cancer genes 	TP53	KRAS	BRCA1	BRCA2	APC	ERBB2
CIRCA	some circadian genes 	ARNTL	CLOCK	PER1	CRY1
PYRUVATE MET	some genes involved in metabolism of pyruvate	ACAT1	ACYP1	ALDH2	LDHA PDHB

Well annotated gene sets can be download from the molecular signatures database
http://software.broadinstitute.org/gsea/msigdb/index.jsp

For convenience we are posting the most recent (as of April 2016) collection of curated gene sets compiled in the molecular signatures database. (containing KEGG, REACTOME, and BIOCARTA gene sets organized by gene symbol)
c2.all.v5.1.symbols.gmt
The gmt files obtained from the molecular signatures database contain only human genes.

#### STEP 3 Select parameters:

Min genes per set: Gene sets from STEP 2 which contain less than this number of cycling genes are ignored during analysis. Default is 10.

Max sims per test: Maximum number of random permutation tests PSEA can use to estimate Kuiper test significance. Higher value produces more precise p-values but will take longer to run. Default is 10000.

Domain min: Minimum value possible for the numerical values from STEP 1. For example, minimum circadian phase is 0. Default is 0.

Domain max: Maximum value possible for the numerical values from STEP 1. For example, maximum circadian phase is 24. Default is 24.

Save image if [p q] value < : Generate an image for gene sets having Kuiper test p-value (or q-value if selected) less than this value. Default is q-value < 0.05.

Max gene names to label: Images will not have individual gene symbols labelled when more than this many genes are contained in a gene set. Use this threshold to limit image size. Default is 20.

#### STEP 4 Select output folder:
A directory folder where output files and images will be saved.
Chose the graphics format for vector images. Default is SVG

#### STEP 5 Generate output:
Click the "START" button to begin analysis.

## Citation
Zhang R, Podtelezhnikov AA, Hogenesch JB, and Anafi RC. Discovering Biology in Periodic Data through Phase Set Enrichment Analysis (PSEA). J Biol Rhythms, 2016, pii: 0748730416631895.


