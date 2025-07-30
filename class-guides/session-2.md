# Summarization Project
In this session we'll create a Spring Boot 3.5 microservice that transforms any PDF into a concise summary using a local Large Language Model (LLM) served through Docker Model Runner.  


## Alfresco Document Transformation - Hands-On
In this hands-on demonstration we'll deploy a Java environment that will leverage Docker Model Runner producing a REST API endpoint that accepts a PDF file and returns a summary without the need to send data to the cloud.  
**The Architecture:**
| **Process**   | **Purpose**   | **Service**   |
| ---           | ---           | ---           |
| POM XML file  | Establishes the Java Environment  | [pom.xml](../summarization-service/pom.xml)  |
| PDF Summarization Controller   | Delegates to the PDFSummarizationService.java | [SummarizationController.java](../summarization-service/src/main/java/org/alfresco/ai/summarize/rest/SummarizationController.java)  |
| PDF Summarization Service   | Processes the Summarization | [PdfSummarizationService.java](../summarization-service/src/main/java/org/alfresco/ai/summarize/service/PdfSummarizationService.java)  |


### Set Up Your Local Dev Environment
CD into the PDF Summarization service directory:
1. Open a new terminal window (or tab) into the fllowing directory: ```/summarization-service```.  
2. Execute the following command to run the Java services:
```
java -jar target/springai-pdf-summary-0.8.0.jar
```
The java application should build and run in this Terminal tab.
3. In HTTPie, start a new tab and use the following specifications for a new HTTP request:
   - **Method:** ```POST```
   - **URL:** ```http://localhost:8080/api/summarize```
   - _Select the Body Tab_
   - _Configure the input to be a Multipart Form_
   - _Add a key pair value to the form titled ```file```.
   - _Use the file selector to navigate to the ```documents``` folder located in the root of the cloned repository and upload the file titled: ```veh_dmg_rep.pdf```_.
   - Press the green **SEND** button.
In the output window you should get a JSON response containing a text summarization of the document.
4. Select and copy **only the raw text** from the JSON response to your clipboard.
5. Use the following code block to paste in your copied text:
```
echo <input type="text" id="name" name="name"/> > ../summary-comparison-tool/examples/local.txt
```


### Connect & Send Queries to the Knowledge Enrichment APIs:

**Get a summarization of services from the _available_actions_ api:**
1. In HTTPie, start a new tab and use the following specifications for a new HTTP request:
   - **Method:** ```GET```
   - **URL:** ```http://localhost:8080/context/available_actions```
   - Press the green **SEND** button.
You should receive a JSON array reply of the available actions you may perform that should look like the following:
```
[
  "image-description",
  "image-metadata-generation",
  "text-metadata-generation",
  "text-classification",
  "text-summarization",
  "image-classification",
  "image-embeddings",
  "text-embeddings",
  "named-entity-recognition-image",
  "named-entity-recognition-text"
]
```

**Send a document for Summarization:**
2. In HTTPie, start a new tab and use the following specifications for a new HTTP request:
   - **Method:** ```POST```
   - **URL:** ```http://localhost:8080/context/upload```
   - _Select the Body Tab_
   - _Configure the input to be a Multipart Form_
   - _Add a key pair value to the form titled ```actions``` with the value of ```text-summarization```_
   - _Add a key pair value to the form titled ```file```.
   - _Use the file selector to navigate to the ```reports``` folder located in the root of the cloned repository and upload the file titled: ```veh_dmg_rep.pdf```_.
   - Press the green **SEND** button.
Once successfully executed, you'll get a JSON return of the "jobId" key pair value that looks like below. Keep this tab open, as you'll need to use that value in the next step.
```
{
  "jobId": "29cf3e0f-0145-4457-abc9-3e94fe83e886"
}
```

**Get a summarixation of the uploaded document.**
3. In HTTPie, start a new tab and use the following specifications for a new HTTP request:
   - **Method:** ```GET```
   - **URL:** ```http://localhost:8080/context/results/``` **Add the "jobId" value you received from the previous step to this url _(ex: http://localhost:8080/context/results/29cf3e0f-0145-4457-abc9-3e94fe83e886)_**
   - Press the green **SEND** button.
This request should return a JSON array containing a status. If the status is 'PROCESSING', then wait and submit the request again every 30 seconds until you get a status of 'SUCCESS' and a summarization of the document.
A completed request will look like this:
```
{
  "id": "29cf3e0f-0145-4457-abc9-3e94fe83e886",
  "timestamp": "2025-07-17T18:08:51.3824703+00:00",
  "results": [
    {
      "objectKey": "contents/d8cb6ba4-7bf7-48c6-8c19-3b50814359f4/35d3d004-6a52-4d3c-b287-e007cf9c5ffd/35d3d004-6a52-4d3c-b287-e007cf9c5ffd",
      "imageDescription": null,
      "imageMetadata": null,
      "textMetadata": null,
      "textSummary": {
        "isSuccess": true,
        "result": "The document provides details about a vehicle damage report for a claimant named John Doe. John's Toyota Prius was involved in a collision with a Land Rover driven by Sarah on June 17, 2025, during rainy weather conditions. The incident resulted in severe damage to the front and side of John's vehicle, including a bent hood, exposed engine, dangling bumper, crushed grill, and broken headlights. John's policy number is 16248FCS, and the policy description is Comprehensive. The document includes information about the claimant's license and the vehicle's make, model, year, and VIN. The summary captures all the essential details and key points from the document, including the claimant's information, the incident details, and the vehicle information, without adding any additional commentary or formatting.",
        "error": null
      },
      "textClassification": null,
      "imageClassification": null,
      "textEmbeddings": null,
      "imageEmbeddings": null,
      "generalProcessingErrors": null,
      "namedEntityText": null,
      "namedEntityImage": null
    }
  ],
  "status": "SUCCESS",
  "inProgress": false
}
```

**Upload a document for Data Curation:**
4. In HTTPie, start a new tab and use the following specifications for a new HTTP request:
   - **Method:** ```POST```
   - **URL:** ```http://localhost:8080/data-curation/upload```
   - _Select the Body Tab_
   - _Configure the input to be a Multipart Form_
   - _Add a key pair value to the form titled ```normalization``` with the value of ```true```_
   - _Add a key pair value to the form titled ```chunking``` with the value of ```true```_
   - _Add a key pair value to the form titled ```embedding``` with the value of ```true```_
   - _Add a key pair value to the form titled ```file```.
   - _Use the file selector to navigate to the ```reports``` folder located in the root of the cloned repository and upload the file titled: ```veh_dmg_rep.pdf```_.
   - Press the green **SEND** button.
This request will return a JSON of values that contain the "jobId", "status", and a "getURL". You'll need the "jobID" for the next step.

**Get the results from the Curation Upload & Embedding:**
5. In HTTPie, start a new tab and use the following specifications for a new HTTP request:
   - **Method:** ```GET```
   - **URL:** ```http://localhost:8080/data-curation/poll_results/``` **Add the "jobId" value you received from the previous step to this url _(ex: http://localhost:8080/data-curation/poll_results/29cf3e0f-0145-4457-abc9-3e94fe83e886)_**
   - Press the green **SEND** button.
This request will return a JSON response that will contain the curated Markdown, which will include text chunks and vector embeddings. Also notice that location arrays are provided, indicating where in the document the data chunk resides.




