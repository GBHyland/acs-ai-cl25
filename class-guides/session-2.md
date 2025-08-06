# Summarization Project
In this session we'll create a Spring Boot 3.5 microservice that transforms any PDF into a concise summary using a local Large Language Model (LLM) served through Docker Model Runner.  


## Docker Model Runner Summarizatioin Service
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


### Send a PDF to the Summarization Service
1. In HTTPie, start a new tab and use the following specifications for a new HTTP request:
   - **Method:** ```POST```
   - **URL:** ```http://localhost:8080/api/summarize```
   - _Select the Body Tab_
   - _Configure the input to be a Multipart Form_
   - _Add a key pair value to the form titled ```file```.
   - _Use the file selector to navigate to the following file (from the root dir): ```reports/veh_dmg_rep.pdf```.
   - Press the green **SEND** button.
In the output window you should get a JSON response containing a text summarization of the document.
2. Open a new terminal window (or tab) into the root directory: ```/acs-ai-cl25```.  
3. Copy the following code block and paste into your terminal window **(DO NOT SUBMIT THE COMMAND YET)**:
```
echo "your copied text" > summary-comparison-tool/examples/local.txt
```
4. Go back into HTTPie, select and copy **only the raw text** from the JSON response to your clipboard.  
5. Paste the copied text over the **"your copied text"** in the Terminal window, keeping the quotes. Run the command. 
   - This will create a text file of the summary into the **summary-comparison-tool** directory, which we'll use later. 
   - For now, review the summary of the document for accuracy.  
6. In the original Terminal window you opened and executed this environment, press CTRL+C to stop the running environment. You may close any Terminal tabs/windows you opened for this session.


### Next Steps: Hyland Knowledge Enrichment API
Navigate to the [session-3 guide here](./session-3.md).

