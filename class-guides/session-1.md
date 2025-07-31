# Alfresco Document Transorm & Ingestion Service - Hands-on

## Run Ollama
1. Open a Terminal window and run the following commands:
Pull the llava service (run this command ** if you did not do this as a pre-requisite** and only run it once):
```
ollama pull llava
```
2. Run ollama once the service has completed downloading:
```
ollama serve        # or `ollama run llava` in another shell
```


## Knowledge Enrichment Presentation
Please regard the instructor for a quick presentation on Knowledge Enrichment that will provide baseline knowdge of Knowledge Enrichment, including the process and architecture of this session. 
- If you continue on your own, please do so at your own risk and wait for the instructor to catch up if you run into any issues.


## Alfresco Document Transformation - Hands-On
In this hands-on demonstration we'll employ Alfresco's Transform Engine to transform PDF docs into Markdown files to prepare for ingestion.


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
**Ensure Docker is running.**
Build the Docker File:
```
docker compose up --build -d
```
_The application can be tested at http://localhost:8090/_. <br>


**The Architecture:**
| **Process**   | **Purpose**   | **Service**   |
| ---           | ---           | ---           |
| Compose File  | Deploys Alfresco Service  | [Compose.yml](../transform-service/compose.yaml)  |
| Docker File   | Builds docker image | [Docker File](../transform-service/Dockerfile)  |
| Alfresco T-Engine | Transform PDF docs to Markdown  | [T-Engine (java)](../transform-service/src/main/java/org/alfresco/transform/MarkdownEngine.java) |
| Alfresco Markdown Transformer | Deploys the T-Engine | [Transformer (java)](../transform-service/src/main/java/org/alfresco/transform/transformer/MarkdownTransformer.java) |
<!-- | Docling Service | Document Parsing | [Docing Service](../transform-service/src/main/java/org/alfresco/transform/service/DoclingService.java) | -->

**Process the PDF to be transferred to Markdown**
Once the environment is running, process the sample PDF found within your directory:
```
curl -X POST \
-F "file=@../reports/john-doe-claim.pdf" \
"http://localhost:8090/transform?sourceMimetype=application/pdf&targetMimetype=text/markdown" \
-o ../outputs/john-doe-report.md
```
This will send the document to the transform service and output the results to a file titled _report.md_ inside of the _outputs_ folder.
Open the .pdf file and .md file to compare the information that was curated from the document.

Before moving on, copy the Markdown file from this directory and place it into a directory we'll use in a future lesson.
Execute the following command:
```
cp ../outputs/john-doe-report.md ../summary-comparison-tool/examples/john-doe-report.md
```

---

### Run and Utilize the RAG Service
This next step will deploy a a drop-in service that will ingest Markdown files, store chunks & captions in Elasticsearch vector search, and answers questions with retrieval-augmented generation (RAG) powered by local LLM(s).
This service utilizes [Docker Model Runner](https://docs.docker.com/ai/model-runner/) to provide a local embedding service.  <br>



### Build the RAG Service Environment 
1. Open a new Terminal tab in the following directory ```/alfresco-knowledge-enrichment```.
2. Run the Docker file:
```
docker compose up --build
```

**The Architecture:**
| **Process**   | **Purpose**   | **Service**   |
| ---           | ---           | ---           |
| Compose File  | Deploys Alfresco Service  | [Compose.yml](../alfresco-knowledge-enrichment/compose.yaml)  |
| Docker File   | Builds docker image | [Docker File](../alfresco-knowledge-enrichment/Dockerfile)  |
| RAG Ingestion Service | Stores chunks as vectors  | [RAG Ingestion Service (java)](../alfresco-knowledge-enrichment/src/main/java/org/alfresco/service/RagIngestService.java) |
| RAG Query Service | Returns chat query against stored data | [RAG Query Service (java)](../alfresco-knowledge-enrichment/src/main/java/org/alfresco/service/RagQueryService.java) |


**Document Ingestion:**
Ingest the Markdown file and send chat requests.
1. Ingest the Markdown file output from the previous Transform service using HTTPie (or other http application you're familiar with, i.e.: Postman, etc):
   - In HTTPie, start a new tab and use the following specifications for a new HTTP request:
   - **Method:** ```POST```
   - **URL:** ```http://localhost:8080/api/ingest```
   - _Select the Body Tab_
   - _Configure the input to be a Multipart Form_
   - _Add the following values to the form_
     - **uuid:** _enter any number, i.e.: ```123-456-789```_
     - **file:** _Use the file selector to open the Markdown file we created in earlier steps, which can be found in your local dev env at this path: ```outputs/report.md```._
   - Press the green **SEND** button.

Alternatively, you may ingest the Markdown file in terminal using the following command:
```
curl --request POST \
  --url http://localhost:8080/api/ingest \
  --form "file=@../outputs/report.md" \
  --form uuid=1010-10238-123
```
2. Set up a Chat http request:
   - In HTTPie, start a new tab and use the following specifications for a new HTTP request:
     - **Method:** ```POST```
      - **URL:** ```http://localhost:8080/api/chat```
      - _Select the Body Tab_
      - _Configure the input to be JSON Text_
      - _Add the following values to the form_
      - In the Body input, paste the following JSON, then press the green **SEND** button.
```
{"message":"How much damage is there to the vehicle?"}
```
Review the JSON resposne. You should get a JSON reply that includes a response to the question. The reply will also include a document object with an array of elements that are relative to the question asked. 


### Stop Your Enviroments
In each Terminal window/tab, stop the environments by pressing CTRL+C, or using the command below:
```
docker compose down
```


### Session 2: Hyland Knowledge Enrichment API Service
Navigate to [this guide](session-2.md) once this Session is completed.


