# Contributing to Shaderlay

Thank you for your interest in contributing to Shaderlay! This document provides guidelines and information for contributors.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Contributing Guidelines](#contributing-guidelines)
- [Code Style](#code-style)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)
- [Issue Reporting](#issue-reporting)

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

## Getting Started

### Prerequisites

- **Android Studio Arctic Fox** (2020.3.1) or later
- **JDK 17** or later
- **Android SDK** with API level 26+
- **Android NDK** for native compilation
- **Git** for version control

### Development Setup

1. **Fork and clone the repository:**
   ```bash
   git clone https://github.com/mattakins/Shaderlay.git
   cd Shaderlay
   ```

2. **Open in Android Studio:**
   - Import the project
   - Sync Gradle files
   - Set up Android SDK if needed

3. **Build the project:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run tests:**
   ```bash
   ./gradlew test
   ```

## Contributing Guidelines

### Types of Contributions

We welcome the following types of contributions:

- **🐛 Bug fixes** - Fix existing issues
- **✨ New features** - Add new functionality
- **📖 Documentation** - Improve docs and comments
- **🎨 UI improvements** - Enhance user interface
- **⚡ Performance** - Optimize code and rendering
- **🧪 Tests** - Add or improve test coverage
- **🔧 Refactoring** - Improve code structure

### Before You Start

1. **Check existing issues** - Look for related work
2. **Create an issue** - Discuss major changes first
3. **Fork the repository** - Work on your own copy
4. **Create a branch** - Use descriptive branch names

### Branch Naming Convention

Use the following naming convention for branches:

- `feature/description` - New features
- `bugfix/description` - Bug fixes
- `docs/description` - Documentation updates
- `refactor/description` - Code refactoring
- `test/description` - Test additions

Examples:
```
feature/external-shader-loading
bugfix/overlay-transparency-issue
docs/update-installation-guide
refactor/shader-manager-cleanup
```

## Code Style

### Kotlin Style Guide

We follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html):

- **4 spaces** for indentation
- **camelCase** for function and property names
- **PascalCase** for class names
- **SCREAMING_SNAKE_CASE** for constants

### Android Specific Guidelines

- Use **Material Design 3** components
- Follow **Android Architecture Guidelines**
- Implement proper **lifecycle management**
- Use **ViewBinding** instead of findViewById
- Handle **configuration changes** properly

### Code Formatting

The project uses ktlint for Kotlin code formatting:

```bash
# Check formatting
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat
```

### Comments and Documentation

- **Public APIs** must have KDoc comments
- **Complex logic** should be commented
- **TODO comments** should include issue numbers
- **Shader code** should explain the effect

Example:
```kotlin
/**
 * Loads an external shader from the Android file system.
 *
 * @param uri The URI of the shader file (.slang or .slangp)
 * @return The shader name if loaded successfully, null otherwise
 */
fun loadExternalShader(uri: Uri): String? {
    // Implementation...
}
```

## Testing

### Unit Tests

Write unit tests for:
- **Business logic** in ShaderManager
- **Utility functions** and parsers
- **Data models** and caching

```kotlin
@Test
fun `loadExternalShader should return shader name on success`() {
    // Arrange
    val mockUri = mockk<Uri>()

    // Act
    val result = shaderManager.loadExternalShader(mockUri)

    // Assert
    assertThat(result).isNotNull()
}
```

### Integration Tests

Integration tests should cover:
- **OpenGL rendering** pipeline
- **File system** interactions
- **Permission handling**

### Manual Testing Checklist

Before submitting a PR, manually test:

- [ ] App launches successfully
- [ ] Overlay permission flow works
- [ ] Built-in shaders render correctly
- [ ] External shader loading works
- [ ] Settings persist between app restarts
- [ ] Performance is acceptable
- [ ] No memory leaks during normal usage

## Pull Request Process

### PR Requirements

1. **All tests pass** - CI checks must be green
2. **Code coverage** - Don't decrease overall coverage
3. **Documentation** - Update docs for user-facing changes
4. **Changelog** - Add entry to CHANGELOG.md
5. **Self-review** - Review your own code first

### PR Template

Use this template for your pull request:

```markdown
## Description
Brief description of changes made.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Refactoring

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Screenshots
Include screenshots for UI changes.

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests added/updated
- [ ] No new warnings introduced
```

### Review Process

1. **Automated checks** run first
2. **Maintainer review** for code quality
3. **Testing** on multiple devices
4. **Approval** and merge to main

## Issue Reporting

### Bug Reports

When reporting bugs, include:

- **Device information** (model, Android version)
- **App version** and build type
- **Steps to reproduce** the issue
- **Expected vs actual** behavior
- **Logs** and crash reports
- **Screenshots** if applicable

### Feature Requests

For new features, provide:

- **Use case** description
- **Proposed solution** or approach
- **Alternative solutions** considered
- **Impact assessment** (breaking changes?)

### Issue Labels

We use these labels to categorize issues:

- `bug` - Something isn't working
- `enhancement` - New feature request
- `documentation` - Documentation needs
- `good first issue` - Good for newcomers
- `help wanted` - Extra attention needed
- `priority:high` - Critical issues
- `priority:low` - Nice to have

## Development Tips

### Debugging Overlay Issues

1. **Use ADB logging:**
   ```bash
   adb logcat | grep Shaderlay
   ```

2. **Test on multiple devices** with different Android versions

3. **Monitor performance** with GPU profiling tools

### Shader Development

1. **Test shaders** in RetroArch first
2. **Validate syntax** with external tools
3. **Profile performance** on target devices
4. **Document effects** and parameters

### Building Releases

Release builds require signing configuration:

1. Create `keystore.properties` in project root
2. Add signing configuration (not committed)
3. Use release build type for testing

## Getting Help

- **GitHub Discussions** - Ask questions

## Recognition

Contributors are recognized in:
- **README.md** contributors section
- **Release notes** for significant contributions
- **Discord** contributor role

Thank you for contributing to Shaderlay! 🎉