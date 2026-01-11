# Codeowners Lens

> Simplify your pull request workflow with clear codeownership visibility!

A powerful IDE plugin/extension that integrates with your [GitHub CODEOWNERS file](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners) to provide valuable codeowner insights directly within your development environment.

## Available for Multiple IDEs

This project provides codeownership visibility for:

### ✅ IntelliJ IDEA (and JetBrains IDEs)
- Plugin available in JetBrains Marketplace
- Supports IntelliJ IDEA, PyCharm, WebStorm, and other JetBrains IDEs
- Source code in the root directory (Kotlin)

### ✅ Visual Studio Code
- Extension available in the `vscode-extension/` directory
- Full feature parity with IntelliJ version
- Source code written in TypeScript

---

## 🚀 Features

Both versions provide the same core functionality:

### 📍 Real-time Codeowner Awareness

- **Status Bar Integration**: View the current file's codeowner(s) conveniently displayed in the status bar
- **Instant Visibility**: Stay informed about who owns the code you're modifying
- **Quick Navigation**: Click on codeowner names to jump to the corresponding rule in the CODEOWNERS file

### 📋 Effortless Approval Assessment

- **Changelist Overview**: See a clear breakdown of codeowners for all modified files in your changelist/commit
- **Grouped by Owner**: Files organized by codeowner names help you identify who might need to approve your pull request
- **File Navigation**: Quickly jump to any changed file from the codeowners view

### 🎯 Smart Pattern Matching

- Full support for GitHub CODEOWNERS glob patterns
- Handles `*`, `**`, negation patterns (`!`), and path matching
- Last-match-wins semantics (GitHub compatible)
- Common prefix detection for cleaner owner display

### 🔄 Auto-Refresh

- Automatically reloads when CODEOWNERS file changes
- Real-time updates as you edit files
- No manual refresh needed

---

## 📦 Installation

### IntelliJ IDEA

1. **From JetBrains Marketplace:**
   - Open Settings → Plugins
   - Search for "Codeowners Lens"
   - Click Install

2. **Manual Installation:**
   - Download the plugin ZIP from releases
   - Open Settings → Plugins → ⚙️ → Install Plugin from Disk
   - Select the downloaded file

### VS Code

TBA

---

## 📖 Usage

### Setup Your Repository

Ensure your repository has a CODEOWNERS file in one of these locations:
- `.github/CODEOWNERS` (recommended)
- `CODEOWNERS` (repository root)
- `docs/CODEOWNERS`

**Example CODEOWNERS:**
```
# Default owners for everything
*       @org/team-leads

# Frontend code
/src/ui/**  @org/frontend-team @alice

# Backend code
/src/api/** @org/backend-team @bob

# Documentation
*.md    @org/docs-team
```

### Using the Plugin/Extension

#### Status Bar (Both IDEs)

The status bar shows the codeowner(s) for the currently active file:

- **Single Owner**: Displays owner name, click to navigate to rule
- **Multiple Owners**: Shows "Owner1 & Owner2" or "Owner1, Owner2 & X more"
- **Unknown Owner**: Displays "Unknown Codeowner" if no rule matches

#### Commit/Changelist Integration

**IntelliJ:**
- Click the codeowners icon in the commit window toolbar
- See all changed files grouped by owner
- Navigate to specific files

**VS Code:**
- Open Source Control panel (Ctrl/Cmd+Shift+G)
- Click the organization icon in SCM toolbar
- See changed files grouped by owner
- Navigate to specific files

---

## 🏗️ Project Structure

```
Codeowners Lens/
├── intellij/                     # IntelliJ plugin (Kotlin)
│   ├── src/                      # Plugin source code
│   ├── build.gradle.kts          # Build configuration
│   └── gradle/                   # Gradle wrapper
├── vscode/                       # VS Code extension (TypeScript)
│   ├── src/                      # Extension source code
│   ├── out/                      # Compiled JavaScript
│   ├── resources/                # Icons and assets
│   ├── package.json              # Extension manifest
│   ├── README.md                 # VS Code-specific docs
│   └── CHANGELOG.md              # VS Code changelog
├── docs/                         # Shared documentation
│   ├── ARCHITECTURE.md           # Technical architecture
│   └── CONTRIBUTING.md           # Contribution guidelines
└── README.md                     # This file
```

---

## 🛠️ Development

### IntelliJ Plugin

Built with:
- Kotlin
- Gradle
- IntelliJ Platform SDK
- Dagger 2 (dependency injection)

**Build:**
```bash
cd intellij
./gradlew build
```

**Run:**
```bash
cd intellij
./gradlew runIde
```

### VS Code Extension

Built with:
- TypeScript
- VS Code Extension API
- Node.js

**Build:**
```bash
cd vscode
npm install
npm run compile
```

**Run:**
- Open `vscode` folder in VS Code
- Press F5 to launch Extension Development Host

---

## 🤝 Benefits

- **Streamlined Collaboration**: Enhance communication by readily identifying relevant codeowners
- **Improved Efficiency**: Expedite your pull request process by knowing who to target for approvals
- **Reduced Friction**: Minimize confusion and delays with clear codeownership knowledge
- **Better Code Review**: Understand ownership context while reviewing code
- **Cross-IDE Support**: Use the same workflow regardless of your IDE choice

---

## 🐛 Troubleshooting

### Plugin/Extension not working

1. **Verify CODEOWNERS file exists** in a supported location
2. **Check file syntax** - ensure proper format (pattern + owners)
3. **Reload/Restart** your IDE
4. **Check IDE version** meets minimum requirements

### Status bar not showing

**IntelliJ:**
- Ensure the status bar widget is enabled in Settings

**VS Code:**
- Check you have a workspace folder open (not individual files)
- Look at Output panel (View → Output → "Codeowners Lens")

### Patterns not matching

- Test patterns at [GitHub CODEOWNERS docs](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners)
- Remember: last matching rule wins
- Check for common issues like missing `/` prefix

---

## ⭐ Support

If you find this plugin/extension helpful, please:
- Star the repository
- Share with your team
- Report issues on GitHub
- Contribute improvements

---

**Made with ❤️ for better code collaboration**
