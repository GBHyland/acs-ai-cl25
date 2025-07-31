# PDF Summarization Comparison Tool
In this sessioin you'll deploy a comprehensive evaluation tool for comparing the quality of two summaries against a source Markdown document using multiple AI-powered metrics.

### Features:
- **Multi-metric evaluation:** BARTScore, semantic similarity, coverage, conciseness, and factual consistency
- **Markdown support:** Direct processing of Markdown documents with proper text extraction
- **Flexible scoring:** Weighted combination of multiple evaluation criteria
- **GPU acceleration:** CUDA support for faster processing
- **Detailed analysis:** Optional breakdown of individual metrics


### Architecture
Note the architecture for this environment below.
**The Architecture:**
| **Process**   | **Purpose**   | **Service**   |
| ---           | ---           | ---           |
| Summary Comparison Python  | Processes the comparison between two docs against an original  | [summary_comparison.py](../summary-comparison-tool/summary_comparison.py)  |



### Deploy the Python Environment
In this hands-on session we'll deploy a local Python virtual environment that will leverage BARTScore, a generative text evaluator, that will compare two seperate generated summaries of an original document in order to guage accuracy.
1. Open a new terminal window (or tab) into the fllowing directory: ```/summary-comparison-tool```.  
2. Execute the following command to create the virtual environment:
```
python3 -m venv venv
source venv/bin/activate
```
3. Install Python dependencies:
```
pip install torch transformers sentence-transformers markdown beautifulsoup4 tqdm numpy
```
4. Clone the BARTScore dependency github:
```
git clone https://github.com/neulab/BARTScore.git
```
5. Execute the comparison between two previously generated markdown files compared to the original document:
```
python summary_comparison.py examples/john-doe-report.md "$(cat examples/keapi.txt)" "$(cat examples/local.txt)" --detailed
```
You should see a logged report providing a deatiled analysis along with an overall score (the lower the numbers the better the accuracy).


