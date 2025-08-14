# syntax=docker/dockerfile:1.7
FROM python:3.11-slim

RUN apt-get update && apt-get install -y --no-install-recommends git \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN useradd -m -u 10001 appuser

COPY requirements.txt ./

ARG TORCH_INDEX_URL=https://download.pytorch.org/whl/cpu

RUN pip install --upgrade pip \
  && if [ -n "$TORCH_INDEX_URL" ]; then \
       pip install --no-cache-dir torch --index-url "$TORCH_INDEX_URL"; \
     fi \
  && pip install --no-cache-dir -r requirements.txt

RUN git clone --depth=1 https://github.com/neulab/BARTScore.git /app/BARTScore
ENV PYTHONPATH=/app/BARTScore:${PYTHONPATH}

COPY . .

ENV HF_HOME=/home/appuser/.cache/huggingface
RUN mkdir -p "$HF_HOME" && chown -R appuser:appuser /home/appuser /app
USER appuser

ENTRYPOINT ["python", "summary_comparison.py"]
CMD ["--help"]
