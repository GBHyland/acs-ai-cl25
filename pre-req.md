# AI Ready Content in ACS - CommunityLive25

### Class Pre-requisits
1. Java 21 & Maven 3.9+ (for local builds)
   - Install Maven with Brew: [Brew Maven Install](https://formulae.brew.sh/formula/maven)
2. Docker (for running the service)
3. An Ollama daemon exposing llava LLM on ```http://localhost:11434```
   - In Terminal:
```
ollama pull llava
ollama serve
```
4. Docker:
   - Docker Desktop ≥ 4.24 (20 GiB RAM)
   - Docker Compose v2
   - Docker Model Runner:
     - Make sure that "Enable host-side TCP support" is enabled in Docker Desktop, and that the "port" is set to ```12434```
5. IDE: Laten build (Recommended IntelliJ IDEA)


 
