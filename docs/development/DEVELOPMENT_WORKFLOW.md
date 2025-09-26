# Development Workflow - ForkSure

This document outlines the recommended development workflow for contributing to ForkSure.

## 🔄 **Standard Feature Development Workflow**

### 1. **Start from Main**
```bash
# Ensure you're on main and up to date
git checkout main
git pull origin main
```

### 2. **Create Feature Branch**
```bash
# Create and switch to a feature branch
git checkout -b feature/your-feature-name

# Examples:
# git checkout -b feature/user-authentication
# git checkout -b feature/recipe-sharing
# git checkout -b bugfix/crash-on-startup
```

### 3. **Make Changes**
```bash
# Make your code changes
# Write tests
# Update documentation

# Commit frequently with meaningful messages,
git add .
git commit -m "feat: implement user authentication system"

# Or more specific commits
git add src/auth/
git commit -m "feat: add user login functionality"

git add tests/
git commit -m "test: add authentication tests"
```

### 4. **Push Feature Branch**
```bash
# Push feature branch to origin
git push origin feature/your-feature-name
```

### 5. **Create Pull Request**
1. Visit the URL provided by the git push output
2. Or go to GitHub and create a PR manually
3. Fill out PR template with:
   - **Description**: What does this change?
   - **Testing**: How was it tested?
   - **Screenshots**: If UI changes
   - **Breaking Changes**: Any breaking changes?

### 6. **PR Review Process**
1. **Automated Checks**: CI/CD runs tests
2. **Code Review**: Team reviews code
3. **Address Feedback**: Make requested changes
4. **Approval**: PR gets approved
5. **Merge**: PR is merged to main

### 7. **Clean Up**
```bash
# After PR is merged, clean up
git checkout main
git pull origin main
git branch -d feature/your-feature-name
git push origin --delete feature/your-feature-name
```

## 📝 **Commit Message Format**

Use conventional commits for a clear history:

```
type(scope): brief description

[optional body]

[optional footer]
```

### **Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### **Examples:**
```bash
git commit -m "feat: add Firebase Crashlytics integration"
git commit -m "fix: resolve Home button navigation issue"
git commit -m "docs: add development workflow guide"
git commit -m "test: add crash handler unit tests"
git commit -m "refactor: improve memory management system"
```

## 🛠️ **Branch Naming Conventions**

### **Feature Branches:**
- `feature/crash-prevention-system`
- `feature/user-authentication`
- `feature/recipe-export`

### **Bug Fix Branches:**
- `bugfix/home-button-not-working`
- `bugfix/memory-leak-in-camera`
- `bugfix/app-crash-on-startup`

### **Hotfix Branches:**
- `hotfix/critical-security-patch`
- `hotfix/production-crash-fix`

### **Documentation Branches:**
- `docs/api-documentation`
- `docs/user-guide-update`

## 🔍 **Pre-Commit Checklist**

Before creating a PR, ensure:

### **Code Quality:**
- [ ] Code follows project conventions
- [ ] No debugging code left in (console.log, print statements)
- [ ] Code is properly commented
- [ ] Variable names are descriptive

### **Testing:**
- [ ] New features have tests
- [ ] All tests pass locally
- [ ] Manual testing completed
- [ ] No regression in existing functionality

### **Documentation:**
- [ ] README updated if needed
- [ ] Code comments updated
- [ ] API documentation updated
- [ ] User-facing changes documented

### **Build & Deploy:**
- [ ] Project builds successfully
- [ ] No compilation warnings
- [ ] Debug and release builds work
- [ ] No sensitive data committed

## 🚨 **Emergency Hotfix Process**

For critical production issues:

### 1. **Create Hotfix Branch from Main**
```bash
git checkout main
git pull origin main
git checkout -b hotfix/critical-issue-description
```

### 2. **Make Minimal Fix**
```bash
# Make only the necessary changes
git add .
git commit -m "hotfix: resolve critical production issue"
git push origin hotfix/critical-issue-description
```

### 3. **Fast-Track PR**
- Create PR with "HOTFIX" label
- Request immediate review
- Merge as soon as approved
- Deploy immediately

### 4. **Follow-Up**
- Create a proper feature branch for a comprehensive fix
- Add tests for the issue
- Improve monitoring to prevent recurrence

## 📊 **PR Template**

When creating PRs, include:

```markdown
## Description
Brief description of what this PR does.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)  
- [ ] Breaking change (fix or feature that would cause existing functionality not to work as expected)
- [ ] Documentation update

## Testing
- [ ] Manual testing completed
- [ ] Unit tests added/updated
- [ ] All tests pass

## Screenshots (if applicable)
Add screenshots for UI changes.

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Code is properly commented
- [ ] Documentation updated
```

## 🎯 **Quick Commands Reference**

### **Start New Feature:**
```bash
git checkout main && git pull origin main && git checkout -b feature/my-feature
```

### **Update Feature Branch with Latest Main:**
```bash
git checkout main && git pull origin main && git checkout feature/my-feature && git rebase main
```

### **Clean Up After Merge:**
```bash
git checkout main && git pull origin main && git branch -d feature/my-feature
```

### **Check Status:**
```bash
git status && git log --oneline -5
```

This workflow ensures code quality, proper review, and maintainable history for the ForkSure project.