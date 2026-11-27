![Burp Search Community](assets/banner.jpg)

# Burp Search Community Edition Extension

**Burp Search Community** is a Java extension built with the **Montoya API** that brings an advanced global search feature to **Burp Suite Community Edition**, similar to the search functionality available in Burp Suite Professional.

## Why This Project Exists
**Burp Suite Community Edition** does not include global search across intercepted traffic. During web security assessments, penetration testing, or bug bounty hunting, quickly locating a specific token, header, parameter, endpoint, or pattern across hundreds of Proxy or Site Map entries becomes slow and manual.

This extension solves that limitation by adding a dedicated **Burp Search** tab that asynchronously scans your Proxy history and Site Map, displaying matches in an interactive results table with an integrated request/response viewer and automatic match highlighting.

## Key Features
- **Search Scope:** Scans **Proxy History** and **Site Map** (HTTP requests and responses).
- **Matching Modes:** Plain-text or **Regular Expression (Regex)** search, with optional **Case-sensitive** matching.
- **Granular Filters:** Filter by URL, HTTP method, headers, and request/response body.
- **Asynchronous Execution:** Real-time progress bar and cancellation support for large histories.
- **Integrated Viewer with Highlighting:** Selecting a result displays the full request and response with all matches automatically highlighted in the editors.

## Requirements
- **Java 17** or higher.
- **Burp Suite Community Edition** (Montoya API compatible).

## Quick Installation
1. Download `burp-search-community.jar` from the **Releases** page.
2. Open Burp Suite.
3. Go to **Extensions** -> **Installed**.
4. Click **Add**.
5. Select **Extension Type: Java**.
6. Choose the downloaded `burp-search-community.jar` file.
7. Click **Next** and a new **Burp Search** tab will appear.

> **Note**: If you get a `NoSuchFileException` when loading the extension, try moving the `.jar` file to your home directory (e.g., `/home/rh4ps00dy/`) and loading it from there. This commonly happens when Burp is installed via Snap or Flatpak and lacks permission to access external drives or data partitions.

## Manual Build
If you want to modify the source code and rebuild the extension, run the following commands from your terminal:

### 1. Compile classes
```bash
mkdir -p build/classes
javac -d build/classes -cp "libs/montoya-api.jar" src/main/java/com/rh4ps00dy/burpsearch/BurpSearchExtension.java src/main/java/com/rh4ps00dy/burpsearch/ui/SearchPanel.java
```

### 2. Package JAR
```bash
jar cvf burp-search-community.jar -C build/classes .
```

## Project Structure
- `src/`: Java source code (`BurpSearchExtension.java` and `ui/SearchPanel.java`).
- `libs/`: Required dependencies (`montoya-api.jar` and `flatlaf.jar`).
- `assets/`: Repository visual assets.
- `burp-search-community.jar`: Pre-built JAR ready to load into Burp Suite.
- `build.gradle`: Gradle build configuration (optional).

## Disclaimer
This tool is developed strictly for **educational purposes and authorized security testing**. The author is not responsible for any misuse of this software or any damages resulting from its use on systems without prior explicit authorization. Use it only on environments you own or within an explicitly authorized scope.
