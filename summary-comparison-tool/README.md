# Summary Comparison Tool

A comprehensive evaluation tool for comparing the quality of two summaries against a source Markdown document using multiple AI-powered metrics.

## Features

- **Multi-metric evaluation**: BARTScore, semantic similarity, coverage, conciseness, and factual consistency
- **Markdown support**: Direct processing of Markdown documents with proper text extraction
- **Flexible scoring**: Weighted combination of multiple evaluation criteria
- **GPU acceleration**: CUDA support for faster processing
- **Detailed analysis**: Optional breakdown of individual metrics

## Installation

### Prerequisites

- Python 3.7+
- PyTorch (CPU or GPU version)

### Setup

1. **Clone the repository**
```bash
git clone https://github.com/aborroy/summary-comparison-tool.git
cd summary-comparison-tool
```

2. **Create a virtual environment**
```bash
python3 -m venv venv
source venv/bin/activate
```

3. **Install Python dependencies**
```bash
pip install torch transformers sentence-transformers markdown beautifulsoup4 tqdm numpy
```

4. **Clone BARTScore dependency**
```bash
git clone https://github.com/neulab/BARTScore.git
```

### GPU Support (Optional)

For CUDA acceleration, install PyTorch with GPU support:
```bash
pip install torch --index-url https://download.pytorch.org/whl/cu118
```

## Usage

### Basic Comparison

```bash
python summary_comparison.py document.md "First summary text" "Second summary text"
```

### With GPU Acceleration

```bash
python summary_comparison.py document.md "First summary" "Second summary" --device cuda
```

### Detailed Analysis

```bash
python summary_comparison.py document.md "First summary" "Second summary" --detailed
```

## Example Output

### Standard Output
```
============================================================
SUMMARY EVALUATION RESULTS
============================================================
Summary 1 Overall Score: -0.167
Summary 2 Overall Score: -0.389

* Better Summary: Summary 1
  Margin: +0.222
```

### Detailed Output (with `--detailed` flag)
```
============================================================
SUMMARY EVALUATION RESULTS
============================================================

SUMMARY 1 BREAKDOWN:
  BARTScore:           -1.727
  Semantic Similarity: 0.527
  Coverage:            0.187
  Conciseness:         0.800
  Factual Consistency: 0.923
  * Overall Score:     -0.167

SUMMARY 2 BREAKDOWN:
  BARTScore:           -2.447
  Semantic Similarity: 0.476
  Coverage:            0.160
  Conciseness:         1.000
  Factual Consistency: 0.857
  * Overall Score:     -0.389

* Better Summary: Summary 1
  Margin: +0.222
```

## Evaluation Metrics

The tool evaluates summaries across five key dimensions:

### 1. BARTScore (Weight: 30%)
- Semantic similarity using BART model
- Measures how well the summary captures document meaning
- Higher scores indicate better semantic alignment

### 2. Semantic Similarity (Weight: 25%)
- Sentence embedding-based similarity
- Uses sentence-transformers for deep semantic understanding
- Fallback to word overlap if sentence-transformers unavailable

### 3. Coverage Score (Weight: 25%)
- Measures how well the summary covers key document content
- Based on important keyword overlap
- Filters out common stop words for better accuracy

### 4. Conciseness Score (Weight: 10%)
- Evaluates appropriate compression ratio
- Optimal range: 10-30% of original document length
- Penalizes both over-compression and verbosity

### 5. Factual Consistency (Weight: 10%)
- Checks if summary facts appear in source document
- Focuses on numbers and proper nouns
- Helps identify potential hallucinations

## Command Line Options

| Option | Description | Default |
|--------|-------------|---------|
| `md_file` | Path to Markdown source document | Required |
| `summary1` | First candidate summary text | Required |
| `summary2` | Second candidate summary text | Required |
| `--device` | Computation device (`cpu`, `cuda`, `cuda:0`) | `cpu` |
| `--detailed` | Show detailed metric breakdown | `false` |

## File Structure

```
summary-comparison-tool/
├── summary_comparison.py            # Main evaluation script
├── README.md                        # This file
├── requirements.txt                 # Python dependencies
├── examples/                        # Example documents and summaries
│   ├── sample_document.md
│   ├── good_summary.txt
│   └── best_summary.txt
└── BARTScore/                      # Git submodule (clone separately)
```

## Requirements

See `requirements.txt` for complete dependency list:

```txt
torch>=1.9.0
transformers>=4.20.0
sentence-transformers>=2.2.0
markdown>=3.4.0
beautifulsoup4>=4.11.0
tqdm>=4.64.0
numpy>=1.21.0
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [BARTScore](https://github.com/neulab/BARTScore) for semantic evaluation
- [Sentence Transformers](https://www.sbert.net/) for embedding-based similarity
- [Hugging Face Transformers](https://huggingface.co/transformers/) for model infrastructure

## Related Work

- **ROUGE**: Traditional n-gram based evaluation metrics
- **BERTScore**: BERT-based semantic similarity
- **BLEURT**: Learned evaluation metric for text generation
- **Factual Consistency**: Various approaches for hallucination detection