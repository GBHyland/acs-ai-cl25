# Knowledge Enrichment API Gateway

> *The one‑stop Spring Boot service that lets you experiment locally with [Hyland Knowledge Enrichment](https://www.hyland.com/en/resources/articles/what-can-you-do-with-knowledge-enrichment) SaaS APIs*

> Upload any document, choose the actions you want (summarisation, entity extraction, OCR...), poll for the results, or feed a PDF into the curation pipeline and watch it get normalised, chunked and embedded.

## Why this exists

The Hyland Knowledge Enrichment currently exposes two public APIs:

| Remote API             | Purpose                                                                       | Typical output                                   |
| :--------------------- | :---------------------------------------------------------------------------- | :----------------------------------------------- |
| **Context Enrichment** | Run one‑off AI actions (summarise, translate, detect PII,...) on a single blob | JSON result per action                           |
| **Data Curation**      | Prepare large content for retrieval‑augmented generation pipelines            | Normalised / chunked JSON with vector embeddings |

Both services live behind OAuth2 and S3‑style presigned URLs. This gateway hides all of that complexity so you can:

* keep credentials out of your client code, set them once as env vars
* prototype locally, everything is proxied on port `8080`
* use familiar multipart uploads, instead of juggling presigned PUTs
* poll with a single call, no need to follow redirects or cache URLs

If you only need one command to test the platform, this is it.

## Quick start

```bash
# 1. Build
mvn clean package

# 2. Provide credentials
vi .env                                     # edit with your SaaS creds
export $(grep -v '^#' .env | xargs)         # linux/mac

# 3. Run
./run.sh                                    # starts on http://localhost:8080
```

## Configuration

| Variable                                                           | Description                                                     |
| :----------------------------------------------------------------- | :-------------------------------------------------------------- |
| `DATA_CURATION_CLIENT_ID` / `CONTEXT_ENRICHMENT_CLIENT_ID`         | OAuth2 client id                                                |
| `DATA_CURATION_CLIENT_SECRET` / `CONTEXT_ENRICHMENT_CLIENT_SECRET` | OAuth2 secret                                                   |
| `DATA_CURATION_API_URL` / `CONTEXT_ENRICHMENT_API_URL`             | Base REST URL                                                   |
| `DATA_CURATION_OAUTH_URL` / `CONTEXT_ENRICHMENT_OAUTH_URL`         | OAuth token endpoint                                            |

Copy `application.yaml` if you need to tweak ports or logging.

## HTTP API

### Context Enrichment

| Method | Path                         | Body / Query                                | Purpose                                   |
| :----- | :--------------------------- | :------------------------------------------ | :---------------------------------------- |
| `GET`  | `/context/available_actions` | –                                           | List actions supported by the remote SaaS |
| `POST` | `/context/upload`            | `multipart/form-data` ⇒ `file`, `actions[]` | Upload a blob and start a job             |
| `GET`  | `/context/results/{jobId}`   | –                                           | Fetch the final JSON result               |

### Data Curation

| Method | Path                                  | Body                                                             | Purpose                                      |
| :----- | :------------------------------------ | :--------------------------------------------------------------- | :------------------------------------------- |
| `POST` | `/data-curation/upload`               | `file`, `normalization`, `chunking`, `embedding`, `[jsonSchema]` | Kick off the pipeline                        |
| `GET`  | `/data-curation/status/{jobId}`       | –                                                                | Current job state                            |
| `GET`  | `/data-curation/poll_results/{jobId}` | –                                                                | Cached presigned‑URL read or bearer fallback |

## Smoke‑test recipes

Below are copy‑paste‑ready `curl` calls that hit every endpoint in order. Replace the filenames and action list to suit your use case.

```bash
# Discover what the SaaS can do
curl --silent --request GET \
  --url http://localhost:8080/context/available_actions
```

```bash
# Kick off summarisation on a Japanese PDF
curl --request POST \
  --url http://localhost:8080/context/upload \
  --header 'Content-Type: multipart/form-data' \
  --form actions=text-summarization \
  --form 'file=@日本.pdf'
# Response: {"jobId":"f221c1d0-b8a8-4dfd-97ba-cd174fdd9d75"}
```

```bash
# Wait for the job to finish, then fetch the result
curl --request GET \
  --url http://localhost:8080/context/results/f221c1d0-b8a8-4dfd-97ba-cd174fdd9d75
```

```bash
# Run the full curation pipeline
curl --request POST \
  --url http://localhost:8080/data-curation/upload \
  --header 'Content-Type: multipart/form-data' \
  --form file=@file.pdf \
  --form normalization=true \
  --form chunking=true \
  --form embedding=true
# Response: {{ "jobId": "API_134887c2-fab3-4d76-a7cf-9cb352b31afe", "getUrl": "..." }}
```

```bash
# Check status (optional – poll_results does this automatically)
curl --request GET \
  --url http://localhost:8080/data-curation/status/API_134887c2-fab3-4d76-a7cf-9cb352b31afe
```

```bash
# Poll until DONE and retrieve the final JSON
curl --request GET \
  --url http://localhost:8080/data-curation/poll_results/API_134887c2-fab3-4d76-a7cf-9cb352b31afe
```

## Internals worth knowing

* **OAuthTokenManager** caches access tokens for 50 minutes (`security.token-cache-duration`)
* **AbstractApiClient** adds automatic retry with exponential back‑off (3 tries by default)
* File MIME‑type detection is delegated to **Apache Tika** (handles edge‑cases such as `.md` or missing extensions)
* The curation controller persists presigned **GET** URLs in an in‑memory store to avoid an extra round‑trip on every poll

## Sequence diagrams

Below you’ll find end‑to‑end sequence diagrams that show every hop—from the first `curl` to the SaaS micro‑service and back

### Context Enrichment life‑cycle

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Gateway
    participant OAuth as OAuth2
    participant CEAPI as Context Enrichment API

    Client->>Gateway: POST /context/upload (file + actions)
    alt token expired
        Gateway->>OAuth: Get access token
        OAuth-->>Gateway: access_token
    end
    Gateway->>CEAPI: POST /jobs (payload)
    CEAPI-->>Gateway: { jobId }
    Gateway-->>Client: { jobId }

    loop until DONE
        Client->>Gateway: GET /context/results/{jobId}
        Gateway->>CEAPI: GET /jobs/{jobId}
        CEAPI-->>Gateway: status or final result
        Gateway-->>Client: status or final result
    end
```

### Data Curation life‑cycle

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Gateway
    participant OAuth as OAuth2
    participant DCAPI as Data Curation API
    participant S3 as S3 (presigned URL)

    Client->>Gateway: POST /data-curation/upload (file + flags)
    alt token expired
        Gateway->>OAuth: Get access token
        OAuth-->>Gateway: access_token
    end
    Gateway->>DCAPI: POST /pipelines (payload)
    DCAPI-->>Gateway: { jobId }
    Gateway-->>Client: { jobId }

    loop until DONE
        Client->>Gateway: GET /data-curation/poll_results/{jobId}
        Gateway->>DCAPI: GET /pipelines/{jobId}
        DCAPI-->>Gateway: status or presigned URL
        alt status == DONE
            Gateway->>S3: GET {url}
            S3-->>Gateway: curated JSON
        end
        Gateway-->>Client: status or curated JSON
    end
```
