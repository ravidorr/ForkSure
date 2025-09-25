#!/bin/bash

# ForkSure - New Feature Script
# Usage: ./scripts/new-feature.sh feature-name

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_color() {
    printf "${2}${1}${NC}\n"
}

# Check if feature name is provided
if [ $# -eq 0 ]; then
    print_color "❌ Error: Please provide a feature name" $RED
    print_color "Usage: ./scripts/new-feature.sh feature-name" $YELLOW
    print_color "Examples:" $BLUE
    print_color "  ./scripts/new-feature.sh user-authentication" $BLUE
    print_color "  ./scripts/new-feature.sh recipe-sharing" $BLUE
    print_color "  ./scripts/new-feature.sh crash-reporting" $BLUE
    exit 1
fi

FEATURE_NAME=$1
BRANCH_NAME="feature/$FEATURE_NAME"

print_color "🚀 Starting new feature: $FEATURE_NAME" $BLUE

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    print_color "❌ Error: Not in a git repository" $RED
    exit 1
fi

# Check if branch already exists
if git show-ref --verify --quiet refs/heads/$BRANCH_NAME; then
    print_color "❌ Error: Branch $BRANCH_NAME already exists" $RED
    exit 1
fi

# Step 1: Ensure we're on main and up to date
print_color "📥 Switching to main branch..." $YELLOW
git checkout main

print_color "🔄 Pulling latest changes..." $YELLOW
git pull origin main

# Step 2: Create and switch to feature branch
print_color "🌿 Creating feature branch: $BRANCH_NAME" $YELLOW
git checkout -b $BRANCH_NAME

# Step 3: Success message
print_color "✅ Feature branch created successfully!" $GREEN
print_color "" $NC
print_color "📋 Next steps:" $BLUE
print_color "1. Make your code changes" $NC
print_color "2. Write tests for your feature" $NC
print_color "3. Update documentation if needed" $NC
print_color "4. Commit your changes:" $NC
print_color "   git add ." $YELLOW
print_color "   git commit -m \"feat: add $FEATURE_NAME\"" $YELLOW
print_color "5. Push your branch:" $NC
print_color "   git push origin $BRANCH_NAME" $YELLOW
print_color "6. Create a Pull Request on GitHub" $NC
print_color "" $NC
print_color "💡 Tips:" $BLUE
print_color "- Use conventional commit messages (feat:, fix:, docs:, etc.)" $NC
print_color "- Test both debug and release builds" $NC
print_color "- Run tests locally before pushing" $NC
print_color "" $NC
print_color "🛠️ Current branch: $BRANCH_NAME" $GREEN
print_color "Ready to code! 🎉" $GREEN