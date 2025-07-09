# Alfresco Document Transorm & Ingestion Service - Hands-on

## Run Ollama
1. Open a Terminal window and run the following commands:
Pull the llava service (run this command only once):
```
ollama pull llava
```
Run ollama once the service has completed downloading:
```
ollama serve        # or `ollama run llava` in another shell
```


## Knowledge Enrichment Presentation
Please regard the instructor for a quick presentation on Knowledge Enrichment that will provide baseline knowdge of Knowledge Enrichment, including the process and architecture of this session. 
- If you continue on your own, please do so at your own risk and wait for the instructor to catch up if you run into any issues.


## Alfresco Document Transformation - Hands-On
In this hands-on demonstration we'll employ Alfresco's Transform Engine to transform PDF docs into Markdown files to prepare for ingestion.
**The Process Architecture:**
| **Process**   | **Service**   |
| ---           | ---           |
| Alfresco T-Engine | [T-Engine](transform-service/src/main/java/org/alfresco/transform/MarkdownEngine.java) |
| Alfresco Markdown Transformer | [Transformer](transform-service/src/main/java/org/alfresco/transform/transformer/MarkdownTransformer.java) |
| Docling | [Docing Service](transform-service/src/main/java/org/alfresco/transform/service/DoclingService.java) |


### Set Up Your Local Dev Environment
Create a local working direcroty and clone this reporitory.
1. Create a directory on your machine to use as a workspace for this lesson.  
2. Open a Terminal window to that directory location.
3. Enter the following Command to clone this repository:
```
git clone https://github.com/GBHyland/acs-ai-cl25.git
```
4. CD into the _transform-service_ directory:
```
cd acs-ai-cl25/transform-service/
```

### Run and Utilize the Transform Services
We'll run the docker file which will leverage Alfresco's Transform Service and Docker's Docling Service to curate and transform a PDF file into a Markdown file.
1. Run the Docker File:
```
docker compose up --build -d
```
_The application can be tested at http://localhost:8090/_
2. Once the environment is running, process the sample PDF found within your directory:
```
curl -X POST \
-F "file=/reports/veh-dmg-rep.pdf" \
"http://localhost:8090/transform?sourceMimetype=application/pdf&targetMimetype=text/markdown" \
-o ../outputs/report.md
```
This will send the document to the transform service and output the results to a file titled _report.md_ inside of the _outputs_ folder.
Open the .pdf file and .md file to compare the information that was curated from the document.


### Run the Utilize the RAG Service
This next step will deploy a a drop-in service that will ingest Markdown files, store chunks & captions in Elasticsearch vector search, and answers questions with retrieval-augmented generation (RAG) powered by local LLM(s).
This service utilizes [Docker Model Runner](https://docs.docker.com/ai/model-runner/) to provide a local embedding service.
1. Change local directories in Terminal to the Knledge Enrichment directory using the following commands:
```
cd ..
cd alfresco-knowledge-enrichment
```
2. Run the Docker file:
```
docker compose up --build
```
3. Ingest the Markdown file output from the previous Transform service using this command:
```
curl --request POST \
  --url http://localhost:8080/api/ingest \
  --form "file=@../outputs/report.md" \
  --form uuid=1010-10238-123
```




