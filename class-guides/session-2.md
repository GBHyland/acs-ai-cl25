# Hyland Knowledge Enrichment Service


## Hyland Knowledge Enrichment Presentation
Please regard the instructor for a quick presentation that will provide essential knowledge of Hyland's SaaS Knowledge Enrichment API product. 
- If you continue on your own, please do so at your own risk and wait for the instructor to catch up if you run into any issues.


## Alfresco Document Transformation - Hands-On
In this hands-on demonstration we'll employ Java environment to that will leverage Hyland's Knowledge Enrichment APIs to ingest, curate, and X our claims document.
**The Architecture:**
| **Process**   | **Purpose**   | **Service**   |
| ---           | ---           | ---           |
| POPM XML file  | Establishes the Java Environment  | [pom.xml](../knowledge-enrichment-api/pom.xml)  |
| Application Yaml File   | Determine's application properties | [application file](../knowledge-enrichment-api/src/main/resources/application.yaml)  |
| Base API Java File   | Handles basic API requests | [Application.java](../knowledge-enrichment-api/src/main/java/org/alfresco/ke/contextenrichment/)  |
- add java files here

### Set Up Your Local Dev Environment
CD into the knowledge enrichment API directory:
1. Open a new terminal window (or tab) into the fllowing directory: ```/knowledge-enrichment-api```.  
2. Execute the following command to run the Java services:
```
./run.sh
```


### Connect & Send Queries to the Knowledge Enrichment APIs:

Get a summarization of services from the _available_actions_ api:
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

Send a document for Summarization:
2. In HTTPie, start a new tab and use the following specifications for a new HTTP request:
     - **Method:** ```POST```
      - **URL:** ```http://localhost:8080/context/upload```
      - _Select the Body Tab_
      - _Configure the input to be a Multipart Form_
      - _Add a key pair value to the form titled ```actions``` with the value of ```text-summarization```_
      - _Add a key pair value to the form titled ```file```.
      - _Use the file selector to navigate to the ```reports``` folder located in the root of the cloned repository and upload the file titled: ```veh_dmg_rep.pdf```_.
      - Press the green **SEND** button.





