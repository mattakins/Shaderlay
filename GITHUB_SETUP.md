# GitHub Setup Guide for Shaderlay

This guide will help you set up the Shaderlay project on GitHub with all the necessary configurations for automated builds, releases, and code quality checks.

## 📋 Table of Contents

- [Repository Setup](#repository-setup)
- [GitHub Secrets Configuration](#github-secrets-configuration)
- [Branch Protection Rules](#branch-protection-rules)
- [Release Process](#release-process)
- [Troubleshooting](#troubleshooting)

## 🚀 Repository Setup

### 1. Create GitHub Repository

1. **Create new repository** on GitHub
2. **Repository name**: `shaderlay` (or your preferred name)
3. **Description**: "System-wide shader overlay app for Android with RetroArch slang support"
4. **Visibility**: Public (or Private if preferred)
5. **Initialize**: Don't initialize with README (we have one)

### 2. Push Local Code

```bash
# Initialize git (if not already done)
git init

# Add remote origin
git remote add origin https://github.com/yourusername/shaderlay.git

# Add all files
git add .

# Initial commit
git commit -m "Initial commit: Android shader overlay app

- System-wide overlay with TYPE_APPLICATION_OVERLAY
- RetroArch slang shader compatibility
- External shader loading from file system
- Hardware-accelerated OpenGL ES rendering
- Performance optimization with shader caching
- Complete CI/CD pipeline with GitHub Actions"

# Push to GitHub
git push -u origin main
```

## 🔐 GitHub Secrets Configuration

### Required Secrets for Release Builds

Navigate to **Settings → Secrets and variables → Actions** and add:

#### Signing Configuration
```
KEYSTORE_BASE64
├── Description: Base64 encoded Android keystore file
├── Value: [base64 encoded .jks/.keystore file]
└── Command to generate: base64 -i your-keystore.jks | tr -d '\n'

SIGNING_KEY_ALIAS
├── Description: Keystore key alias
└── Value: [your key alias]

SIGNING_KEY_PASSWORD
├── Description: Key password
└── Value: [your key password]

SIGNING_STORE_PASSWORD
├── Description: Keystore password
└── Value: [your keystore password]
```

#### Generating Android Keystore
```bash
# Generate new keystore for releases
keytool -genkey -v -keystore shaderlay-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias shaderlay-key

# Convert to base64 for GitHub secrets
base64 -i shaderlay-release.jks | tr -d '\n'
```

### Optional Secrets

```
CODECOV_TOKEN (if using code coverage)
SLACK_WEBHOOK (for notifications)
DISCORD_WEBHOOK (for notifications)
```

## 🛡️ Branch Protection Rules

### Main Branch Protection

Go to **Settings → Branches → Add rule** for `main`:

```yaml
Branch name pattern: main

Protection rules:
☑️ Require a pull request before merging
  ☑️ Require approvals: 1
  ☑️ Dismiss stale reviews when new commits are pushed
  ☑️ Require review from CODEOWNERS

☑️ Require status checks to pass before merging
  ☑️ Require branches to be up to date before merging
  Required status checks:
    - test
    - build
    - code-quality

☑️ Require conversation resolution before merging
☑️ Include administrators
```

### Develop Branch Protection (Optional)

For `develop` branch (if using git-flow):

```yaml
Branch name pattern: develop

Protection rules:
☑️ Require a pull request before merging
☑️ Require status checks to pass before merging
  Required status checks:
    - test
    - build
```

## 🚀 Release Process

### Automated Releases

The project uses **semantic versioning** with automated releases:

#### Creating a Release

1. **Create and push tag**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **GitHub Actions automatically**:
   - Builds release APK and AAB
   - Creates GitHub release
   - Uploads assets
   - Generates release notes

#### Version Numbering

Follow [Semantic Versioning](https://semver.org/):
- `v1.0.0` - Major release
- `v1.1.0` - Minor release (new features)
- `v1.0.1` - Patch release (bug fixes)

#### Pre-release Tags
- `v1.0.0-alpha.1` - Alpha releases
- `v1.0.0-beta.1` - Beta releases
- `v1.0.0-rc.1` - Release candidates

### Manual Release Process

If needed, create releases manually:

1. **Go to Releases** → **Create a new release**
2. **Tag version**: `v1.0.0`
3. **Release title**: `Shaderlay v1.0.0`
4. **Description**: Use the template from `release.yml`
5. **Upload assets**: APK, AAB, mapping files

## 🔧 Repository Settings

### General Settings

```yaml
Repository Settings:
  Default branch: main

  Features:
    ☑️ Issues
    ☑️ Projects
    ☑️ Wiki
    ☑️ Discussions
    ☑️ Sponsorships (optional)

  Pull Requests:
    ☑️ Allow merge commits
    ☑️ Allow squash merging
    ☑️ Allow rebase merging
    ☑️ Always suggest updating pull request branches
    ☑️ Automatically delete head branches
```

### Pages Setup (Optional)

For project documentation:

1. **Go to Settings → Pages**
2. **Source**: Deploy from a branch
3. **Branch**: `main` / `docs` folder
4. **Custom domain**: (optional)

### Security Settings

```yaml
Security & Analysis:
  Dependency graph: ☑️ Enabled
  Dependabot alerts: ☑️ Enabled
  Dependabot security updates: ☑️ Enabled
  Code scanning alerts: ☑️ Enabled
  Secret scanning alerts: ☑️ Enabled
```

## 📊 GitHub Actions Workflows

The repository includes several workflows:

### Main Workflows
- **`android.yml`** - Main CI/CD pipeline
- **`pr-validation.yml`** - Pull request validation
- **`release.yml`** - Automated releases

### Workflow Features
- ✅ **Automated testing** (unit tests, lint)
- ✅ **Multi-platform builds** (ARM64, x86)
- ✅ **Code quality checks** (Detekt, ktlint)
- ✅ **Security scanning** (Trivy)
- ✅ **Release automation** (APK, AAB)
- ✅ **Artifact uploads** (debug/release builds)

## 🔧 IDE Integration

### Android Studio

1. **Enable VCS integration**:
   - VCS → Enable Version Control Integration
   - Select Git

2. **Configure code style**:
   - Settings → Editor → Code Style → Kotlin
   - Import from `.editorconfig`

3. **Set up Detekt**:
   - Install Detekt plugin
   - Configure with `detekt.yml`

### VS Code (Optional)

Extensions for Kotlin development:
- Kotlin Language Support
- GitLens
- GitHub Pull Requests

## 🔍 Monitoring & Analytics

### Repository Insights

Monitor repository health:
- **Insights → Pulse** - Activity overview
- **Insights → Code frequency** - Commit activity
- **Insights → Contributors** - Contributor statistics
- **Insights → Traffic** - Clone and visitor stats

### Action Monitoring

Track CI/CD performance:
- **Actions tab** - Workflow runs
- **Insights → Actions** - Workflow usage
- Failed builds notifications

## 🐛 Troubleshooting

### Common Issues

#### Build Failures

**Issue**: NDK not found
```bash
Solution: Update Android Studio and install NDK
- Tools → SDK Manager → SDK Tools → NDK
```

**Issue**: Signing failures
```bash
Solution: Check GitHub secrets
- Verify KEYSTORE_BASE64 is correctly encoded
- Confirm all signing secrets are set
```

#### Permission Issues

**Issue**: Cannot write to repository
```bash
Solution: Check repository permissions
- Settings → Manage access
- Ensure proper write permissions
```

### Debug Commands

```bash
# Test local build
./gradlew assembleDebug

# Run tests locally
./gradlew test

# Check code quality
./gradlew detekt ktlintCheck

# Verify signing configuration
./gradlew signingReport
```

### Support Resources

- **GitHub Docs**: [GitHub Actions](https://docs.github.com/en/actions)
- **Android Docs**: [Build and Release](https://developer.android.com/studio/build)
- **Issues**: Use repository issue tracker
- **Discussions**: Use GitHub Discussions for questions

## ✅ Setup Checklist

- [ ] Repository created and code pushed
- [ ] GitHub secrets configured for signing
- [ ] Branch protection rules enabled
- [ ] First release tagged and deployed
- [ ] Documentation reviewed and updated
- [ ] Team members added with appropriate permissions
- [ ] Issue and PR templates tested
- [ ] Automated workflows verified
- [ ] Security settings enabled
- [ ] Repository settings configured

---

**🎉 Your Shaderlay repository is now ready for collaborative development!**