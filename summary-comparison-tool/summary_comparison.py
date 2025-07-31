#!/usr/bin/env python3
"""
compare_md_summaries.py
Evaluate two summaries against a Markdown document using BARTScore.

USAGE
=====
python compare_md_summaries.py document.md "first summary text" "second summary text" \
       [--device cuda] [--detailed]

Dependencies
============
git clone https://github.com/neulab/BARTScore.git
pip install sentence-transformers torch transformers markdown beautifulsoup4 tqdm numpy
"""

import os
os.environ["TOKENIZERS_PARALLELISM"] = "false"

import argparse
import sys
from pathlib import Path
from typing import List, Dict, Tuple
from dataclasses import dataclass
import re
from markdown import markdown
from bs4 import BeautifulSoup
import numpy as np

# Ensure BARTScore is available
sys.path.append("BARTScore")
from bart_score import BARTScorer
from transformers import AutoTokenizer, AutoModel
import torch
import torch.nn.functional as F

MODEL = "facebook/bart-large-cnn"
SENTENCE_MODEL = "sentence-transformers/all-MiniLM-L6-v2"
MAX_TOKENS = 1024

@dataclass
class EvaluationResult:
    bart_score: float
    semantic_similarity: float
    coverage_score: float
    conciseness_score: float
    factual_consistency: float
    overall_score: float

class EnhancedSummaryEvaluator:
    def __init__(self, device="cpu"):
        self.device = device
        self.bart_scorer = BARTScorer(device=device, checkpoint=MODEL)
        self.bart_tokenizer = AutoTokenizer.from_pretrained(MODEL)
        
        # For semantic similarity
        try:
            from sentence_transformers import SentenceTransformer
            self.sentence_model = SentenceTransformer(SENTENCE_MODEL, device=device)
        except ImportError:
            print("Warning: sentence-transformers not available. Using fallback similarity.")
            self.sentence_model = None
    
    def md_to_text(self, md: str) -> str:
        """Convert Markdown to plain text."""
        html = markdown(md, output_format="html")
        soup = BeautifulSoup(html, "html.parser")
        return soup.get_text(separator=" ", strip=True)
    
    def chunk_text(self, text: str, max_len: int = MAX_TOKENS) -> List[str]:
        """Split text into manageable chunks."""
        tokens = self.bart_tokenizer.encode(text, add_special_tokens=False)
        return [
            self.bart_tokenizer.decode(tokens[i:i + max_len])
            for i in range(0, len(tokens), max_len)
        ] or [""]
    
    def get_sentences(self, text: str) -> List[str]:
        """Simple sentence splitting."""
        sentences = re.split(r'[.!?]+', text)
        return [s.strip() for s in sentences if s.strip()]
    
    def bart_score_evaluation(self, document: str, summary: str) -> float:
        """Original BARTScore evaluation with improved aggregation."""
        chunks = self.chunk_text(document)
        scores = self.bart_scorer.score(chunks, [summary] * len(chunks))
        
        # Weighted average based on chunk length
        chunk_lengths = [len(chunk.split()) for chunk in chunks]
        total_length = sum(chunk_lengths)
        
        if total_length == 0:
            return 0.0
        
        weighted_score = sum(score * length for score, length in zip(scores, chunk_lengths))
        return weighted_score / total_length
    
    def semantic_similarity_score(self, document: str, summary: str) -> float:
        """Compute semantic similarity using sentence embeddings."""
        if self.sentence_model is None:
            # Fallback: simple word overlap
            doc_words = set(document.lower().split())
            sum_words = set(summary.lower().split())
            if len(sum_words) == 0:
                return 0.0
            return len(doc_words & sum_words) / len(sum_words)
        
        # Use sentence transformer
        embeddings = self.sentence_model.encode([document, summary])
        similarity = F.cosine_similarity(
            torch.tensor(embeddings[0]).unsqueeze(0),
            torch.tensor(embeddings[1]).unsqueeze(0)
        ).item()
        return max(0.0, similarity)  # Ensure non-negative
    
    def coverage_score(self, document: str, summary: str) -> float:
        """Evaluate how well the summary covers key content."""
        doc_sentences = self.get_sentences(document)
        sum_sentences = self.get_sentences(summary)
        
        if not doc_sentences or not sum_sentences:
            return 0.0
        
        # Simple keyword-based coverage
        doc_words = set()
        for sent in doc_sentences:
            doc_words.update(sent.lower().split())
        
        sum_words = set()
        for sent in sum_sentences:
            sum_words.update(sent.lower().split())
        
        # Remove common stop words for better coverage estimation
        stop_words = {'the', 'a', 'an', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for', 'of', 'with', 'by', 'is', 'are', 'was', 'were', 'be', 'been', 'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would', 'could', 'should'}
        
        important_doc_words = doc_words - stop_words
        important_sum_words = sum_words - stop_words
        
        if len(important_doc_words) == 0:
            return 1.0
        
        coverage = len(important_sum_words & important_doc_words) / len(important_doc_words)
        return min(1.0, coverage)
    
    def conciseness_score(self, document: str, summary: str) -> float:
        """Evaluate summary conciseness (shorter is generally better)."""
        doc_length = len(document.split())
        sum_length = len(summary.split())
        
        if doc_length == 0:
            return 1.0 if sum_length == 0 else 0.0
        
        compression_ratio = sum_length / doc_length
        
        # Ideal compression ratio is between 0.1 and 0.3
        if compression_ratio <= 0.1:
            return 0.5  # Too aggressive
        elif compression_ratio <= 0.3:
            return 1.0  # Good compression
        elif compression_ratio <= 0.5:
            return 0.8  # Acceptable
        else:
            return max(0.1, 1.0 - compression_ratio)  # Too verbose
    
    def factual_consistency_score(self, document: str, summary: str) -> float:
        """Simple heuristic for factual consistency."""
        # Extract potential facts (numbers, proper nouns, etc.)
        import re
        
        # Extract numbers and capitalized words as potential facts
        doc_facts = set(re.findall(r'\b(?:\d+(?:\.\d+)?|\b[A-Z][a-zA-Z]*)\b', document))
        sum_facts = set(re.findall(r'\b(?:\d+(?:\.\d+)?|\b[A-Z][a-zA-Z]*)\b', summary))
        
        # All facts in summary should be in document
        if len(sum_facts) == 0:
            return 1.0
        
        consistent_facts = sum_facts & doc_facts
        consistency = len(consistent_facts) / len(sum_facts)
        
        return consistency
    
    def evaluate_summary(self, document: str, summary: str) -> EvaluationResult:
        """Comprehensive summary evaluation."""
        bart_score = self.bart_score_evaluation(document, summary)
        semantic_sim = self.semantic_similarity_score(document, summary)
        coverage = self.coverage_score(document, summary)
        conciseness = self.conciseness_score(document, summary)
        factual_consistency = self.factual_consistency_score(document, summary)
        
        # Weighted combination of scores
        weights = {
            'bart': 0.3,
            'semantic': 0.25,
            'coverage': 0.25,
            'conciseness': 0.1,
            'factual': 0.1
        }
        
        overall = (
            weights['bart'] * bart_score +
            weights['semantic'] * semantic_sim +
            weights['coverage'] * coverage +
            weights['conciseness'] * conciseness +
            weights['factual'] * factual_consistency
        )
        
        return EvaluationResult(
            bart_score=bart_score,
            semantic_similarity=semantic_sim,
            coverage_score=coverage,
            conciseness_score=conciseness,
            factual_consistency=factual_consistency,
            overall_score=overall
        )

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("md_file", type=Path, help="Markdown file")
    parser.add_argument("summary1", help="First summary")
    parser.add_argument("summary2", help="Second summary")
    parser.add_argument("--device", default="cpu", help="Device for computation")
    parser.add_argument("--detailed", action="store_true", help="Show detailed breakdown")
    
    args = parser.parse_args()
    
    # Load document
    md_text = args.md_file.read_text(encoding="utf-8")
    evaluator = EnhancedSummaryEvaluator(device=args.device)
    document = evaluator.md_to_text(md_text)
    
    # Evaluate both summaries
    result1 = evaluator.evaluate_summary(document, args.summary1)
    result2 = evaluator.evaluate_summary(document, args.summary2)
    
    print("=" * 60)
    print("SUMMARY EVALUATION RESULTS")
    print("=" * 60)
    
    if args.detailed:
        print(f"\nSUMMARY 1 BREAKDOWN:")
        print(f"  BARTScore:           {result1.bart_score:.3f}")
        print(f"  Semantic Similarity: {result1.semantic_similarity:.3f}")
        print(f"  Coverage:            {result1.coverage_score:.3f}")
        print(f"  Conciseness:         {result1.conciseness_score:.3f}")
        print(f"  Factual Consistency: {result1.factual_consistency:.3f}")
        print(f"  * Overall Score:     {result1.overall_score:.3f}")
        
        print(f"\nSUMMARY 2 BREAKDOWN:")
        print(f"  BARTScore:           {result2.bart_score:.3f}")
        print(f"  Semantic Similarity: {result2.semantic_similarity:.3f}")
        print(f"  Coverage:            {result2.coverage_score:.3f}")
        print(f"  Conciseness:         {result2.conciseness_score:.3f}")
        print(f"  Factual Consistency: {result2.factual_consistency:.3f}")
        print(f"  * Overall Score:     {result2.overall_score:.3f}")
    else:
        print(f"Summary 1 Overall Score: {result1.overall_score:.3f}")
        print(f"Summary 2 Overall Score: {result2.overall_score:.3f}")
    
    # Determine winner
    if result1.overall_score > result2.overall_score:
        winner = "Summary 1"
        margin = result1.overall_score - result2.overall_score
    elif result2.overall_score > result1.overall_score:
        winner = "Summary 2"
        margin = result2.overall_score - result1.overall_score
    else:
        winner = "Tie"
        margin = 0.0
    
    print(f"\n* Better Summary: {winner}")
    if margin > 0:
        print(f"  Margin: +{margin:.3f}")

if __name__ == "__main__":
    main()