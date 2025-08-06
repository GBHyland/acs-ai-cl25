# Hyland Knowledge Enrichment API Service


## Hyland Knowledge Enrichment Presentation
Please regard the instructor for a quick presentation that will provide essential knowledge of Hyland's SaaS Knowledge Enrichment API product. 
- If you continue on your own, please do so at your own risk and wait for the instructor to catch up if you run into any issues.


## Alfresco Document Transformation - Hands-On
In this hands-on session we'll deploy a Java environment that will leverage Hyland's Knowledge Enrichment APIs to ingest and curate our claims document.
**The Architecture:**
| **Process**   | **Purpose**   | **Service**   |
| ---           | ---           | ---           |
| POM XML file  | Establishes the Java Environment  | [pom.xml](../knowledge-enrichment-api/pom.xml)  |
| Application Yaml File   | Determine's application properties | [application file](../knowledge-enrichment-api/src/main/resources/application.yaml)  |
| Base API Java File   | Handles basic API requests | [Application.java](../knowledge-enrichment-api/src/main/java/org/alfresco/ke/contextenrichment/)  |



### Set Up Your Local Dev Environment
CD into the knowledge enrichment API directory:
1. Open a new terminal window (or tab) into the fllowing directory: ```/knowledge-enrichment-api```.  
2. Execute the following command to run the Java services:
```
./run.sh
```


### Connect & Send Queries to the Knowledge Enrichment APIs:

**Get a summarization of services from the _available_actions_ api:**
In HTTPie, start a new tab and use the following specifications for a new HTTP request:
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
In HTTPie, start a new tab and use the following specifications for a new HTTP request:
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
1. In HTTPie, start a new tab and use the following specifications for a new HTTP request:
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
2. Open a new Terminal window/tab at the root directory of the _(acs-ai-cl25)_.
3. Copy the following code block and paste into your terminal window **(DO NOT SUBMIT THE COMMAND YET)**:
```
echo "your copied text" > summary-comparison-tool/examples/keapi.txt
```
4. Go back into your HTTPie application and copy the text only to your clipboard from the JSON summary reply.  
5. Paste the copied text over the **"your copied text"** in the Terminal window, keeping the quotes. Run the command. 
   - This will create a text file of the summary into the **summary-comparison-tool** directory, which we'll use later.  


**Upload a document for Data Curation:**
In HTTPie, start a new tab and use the following specifications for a new HTTP request:
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
In HTTPie, start a new tab and use the following specifications for a new HTTP request:
   - **Method:** ```GET```
   - **URL:** ```http://localhost:8080/data-curation/poll_results/``` **Add the "jobId" value you received from the previous step to this url _(ex: http://localhost:8080/data-curation/poll_results/29cf3e0f-0145-4457-abc9-3e94fe83e886)_**
   - Press the green **SEND** button.
This request will return a JSON response that will contain the curated Markdown, which will include text chunks and vector embeddings. Also notice that location arrays are provided, indicating where in the document the data chunk resides.


**Stop the Environment and Close Tabs**
You may now use the CTRL+C command in the running environment for this session to stop the environment and close out of that Terminal window/tab.


### Next Steps: Summary Comparison Tool
Navigate to the [session-4 guide here](./session-4.md).

