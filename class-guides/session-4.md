# PDF Summarization Comparison Tool
In this sessioin you'll deploy a comprehensive evaluation tool for comparing the quality of two summaries against a source Markdown document using multiple AI-powered metrics.

### Features:
- **Multi-metric evaluation:** BARTScore, semantic similarity, coverage, conciseness, and factual consistency
- **Markdown support:** Direct processing of Markdown documents with proper text extraction
- **Flexible scoring:** Weighted combination of multiple evaluation criteria
- **Detailed analysis:** Detailed breakdown of individual metrics, including similarities, factual consistency, and document coverage.
<!--- **GPU acceleration:** CUDA support for faster processing-->


### Architecture
Note the architecture for this environment below.
**The Architecture:**
| **Process**   | **Purpose**   | **Service**   |
| ---           | ---           | ---           |
| Docker File  | Installs Python dependencies & builds a virtual environment  | [Dockerfile](../summary-comparison-tool/Dockerfile)  |
| Summary Comparison Python  | Processes the comparison between two docs against an original  | [summary_comparison.py](../summary-comparison-tool/summary_comparison.py)  |



### Deploy the Python Environment & Run Comparison Script
In this hands-on session we'll run a Docker script that will deploy a local Python virtual environment which leverages BARTScore, a generative text evaluator, that will compare two seperate generated summaries of an original document in order to guage accuracy.
1. Open a new terminal window (or tab) into the fllowing directory: ```/summary-comparison-tool```.  
2. Execute the following command to create the virtual environment:
```
docker build -t summary-compare .
docker run --rm -v "$PWD":/work -w /work summary-compare \
  examples/veh-dmg-rep.md “./examples/keapi.txt” “./examples/local.txt“ --detailed
```
You should see a logged report providing a deatiled analysis along with an overall score (the lower the numbers the better the accuracy).



