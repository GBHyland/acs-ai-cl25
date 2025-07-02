# AI Ready Content in ACS - CommunityLive25

### Class Pre-requisits
1. Java 21 & Maven 3.9+ (for local builds)
   - Install Java with Brew:
   ```
    brew install java   #installs java
    java -version       #check java version (confirm install)
   ```
   - Install Maven with Brew: [Brew Maven Install](https://formulae.brew.sh/formula/maven)
2. Docker Desktop: [Docker Desktop](https://www.docker.com/get-started/)
   - Ensure you have the correct Docker Model Runner settings in Docker Desktop application; see below:
   - ![alt text](images/docker-desktop-settings.jpeg "Proper Docker Settings for Model Runner")
3. An Ollama daemon exposing llava LLM on ```http://localhost:11434```
   - In Terminal:
```
ollama pull llava   #install ollama
ollama serve        #run ollama
```
4. IDE: Latest build (Recommended IntelliJ IDEA)
   - [IntelliJ IDEA](https://www.jetbrains.com/help/idea/installation-guide.html#)


 
