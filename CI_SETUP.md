# CI/CD Setup Guide

This guide shows how to enforce build and test validation before allowing pushes to succeed.

## 🚀 GitHub Actions (Automated)

### What We've Set Up:

1. **`.github/workflows/ci.yml`** - Full CI/CD pipeline
2. **`.github/workflows/pre-commit.yml`** - Fast validation on every push

### Features:
- ✅ Kotlin compilation check
- ✅ Unit tests (`BakingViewModelTest`, etc.)
- ✅ Lint checks
- ✅ Build APKs (debug & release)
- ✅ Code quality analysis
- ✅ Messaging system specific tests
- ✅ Multilingual resource validation

---

## 🔒 GitHub Branch Protection Rules

### To enforce CI checks before merging:

1. **Go to GitHub Repository Settings**
   ```
   https://github.com/[username]/ForkSure/settings/branches
   ```

2. **Add Branch Protection Rule for `main`:**
   - Branch name pattern: `main`
   - ☑️ **Require status checks to pass before merging**
   - ☑️ **Require branches to be up to date before merging**
   - Select these status checks:
     - `build-and-test`
     - `quick-validation`
     - `messaging-system-tests` 
     - `code-quality`

3. **Additional Protection Options:**
   - ☑️ **Require pull request reviews before merging**
   - ☑️ **Dismiss stale PR approvals when new commits are pushed**
   - ☑️ **Require review from code owners**
   - ☑️ **Restrict pushes that create files larger than 100MB**

### Result:
- 🚫 **Direct pushes to `main` blocked** until CI passes
- ✅ **Pull requests required** with passing status checks
- 🔄 **Automatic validation** on every push

---

## 🏠 Local Git Hooks (Optional)

### Pre-commit Hook Setup:

1. **Create the hook:**
   ```bash
   # Create executable pre-commit hook
   cat > .git/hooks/pre-commit << 'EOF'
   #!/bin/bash
   
   echo "🔍 Running pre-commit validation..."
   
   # Check if gradlew exists
   if [ ! -f "./gradlew" ]; then
       echo "❌ gradlew not found"
       exit 1
   fi
   
   # Make gradlew executable
   chmod +x ./gradlew
   
   # Run compilation check
   echo "📝 Checking Kotlin compilation..."
   ./gradlew compileDebugKotlin compileReleaseKotlin
   if [ $? -ne 0 ]; then
       echo "❌ Compilation failed"
       exit 1
   fi
   
   # Run fast unit tests
   echo "🧪 Running unit tests..."
   ./gradlew testDebugUnitTest --parallel
   if [ $? -ne 0 ]; then
       echo "❌ Unit tests failed"
       exit 1
   fi
   
   # Run lint check
   echo "📋 Running lint check..."
   ./gradlew lintDebug
   if [ $? -ne 0 ]; then
       echo "⚠️ Lint issues found (allowing commit with warnings)"
   fi
   
   echo "✅ Pre-commit validation passed!"
   EOF
   
   chmod +x .git/hooks/pre-commit
   ```

2. **Test the hook:**
   ```bash
   # This will trigger the pre-commit hook
   git commit -m "test commit"
   ```

### Pre-push Hook (More Comprehensive):

```bash
cat > .git/hooks/pre-push << 'EOF'
#!/bin/bash

echo "🚀 Running pre-push validation..."

# Run full build
echo "🏗️ Building project..."
./gradlew build
if [ $? -ne 0 ]; then
    echo "❌ Build failed - push blocked"
    exit 1
fi

# Run messaging system tests specifically
echo "📱 Testing messaging system..."
./gradlew test --tests "*MessageDisplayHelper*" --tests "*ToastHelper*"
if [ $? -ne 0 ]; then
    echo "❌ Messaging system tests failed - push blocked"
    exit 1
fi

echo "✅ All validations passed - proceeding with push"
EOF

chmod +x .git/hooks/pre-push
```

---

## 🔧 Validation Levels

### **Level 1: Local Git Hooks**
- 🏠 **Runs locally** before commit/push
- ⚡ **Fast feedback** (30 seconds)
- 🔧 **Easy to bypass** (`git commit --no-verify`)

### **Level 2: GitHub Actions**
- ☁️ **Runs on GitHub servers** 
- 🔍 **Comprehensive testing** (5-10 minutes)
- 🚫 **Cannot be bypassed** when branch protection is enabled

### **Level 3: Branch Protection**
- 🔒 **Enforced by GitHub**
- 🚫 **Blocks merges** until CI passes
- 👥 **Team-wide enforcement**

---

## 📊 What Gets Tested

### **Build Validation:**
- ✅ Kotlin compilation (debug & release)
- ✅ Resource validation
- ✅ Dependency resolution
- ✅ APK generation

### **Test Validation:**
- ✅ Unit tests (`BakingViewModelTest`)
- ✅ Messaging system tests
- ✅ Accessibility tests
- ✅ Navigation tests

### **Code Quality:**
- ✅ Lint checks
- ✅ Static analysis
- ✅ Resource validation
- ✅ String translation coverage

### **Messaging System Specific:**
- ✅ `MessageDisplayHelper` tests
- ✅ `ToastHelper` functionality
- ✅ Multilingual string resources
- ✅ Snackbar theme validation

---

## 🎯 Getting Started

1. **Commit the workflow files:**
   ```bash
   git add .github/
   git commit -m "Add CI/CD workflows with build and test enforcement"
   git push origin main
   ```

2. **Set up branch protection** (see instructions above)

3. **Optional: Add local hooks** for faster feedback

4. **Test the system:**
   ```bash
   # Create a test branch
   git checkout -b test-ci
   
   # Make a small change
   echo "// Test change" >> app/src/main/java/com/ravidor/forksure/ToastHelper.kt
   
   # Commit and push
   git add .
   git commit -m "Test CI system"
   git push origin test-ci
   
   # Create PR and watch CI run!
   ```

---

## ✅ Success Criteria

After setup, your repository will:
- 🚫 **Block pushes** that don't compile
- 🚫 **Block merges** with failing tests  
- ✅ **Ensure code quality** through automated checks
- 📱 **Validate messaging system** functionality
- 🌐 **Check multilingual support**
- 📋 **Generate build artifacts** for releases

Your ForkSure app will be protected from broken builds! 🛡️ 