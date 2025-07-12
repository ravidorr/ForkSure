# Test Naming Conventions

## Overview
This document establishes consistent naming conventions for test files in the ForkSure project to ensure clarity, maintainability, and ease of understanding for all developers.

## Naming Patterns

### 1. Test Suites (`TestSuite`)
Use for comprehensive test collections that cover multiple related areas or coordinate multiple testing concerns.

**Format:** `[Purpose]TestSuite.kt`

**Examples:**
- `PerformanceTestSuite.kt` - Performance testing across multiple components
- `IntegrationTestSuite.kt` - Integration testing for multi-component workflows
- `PersistenceTestSuite.kt` - Data persistence and caching tests
- `NetworkApiTestSuite.kt` - Network and API integration tests
- `StressTestSuite.kt` - Stress and load testing

### 2. Validation Suites (`ValidationSuite`)
Use for meta-tests that validate the testing strategy itself or coordinate between test suites.

**Format:** `[Purpose]ValidationSuite.kt`

**Examples:**
- `TestStrategyValidationSuite.kt` - Validates testing strategy comprehensiveness
- `TestSuiteCoordinationValidationSuite.kt` - Validates coordination between test suites

### 3. Advanced Tests (`AdvancedTest`)
Use for comprehensive, complex testing of specific components or features.

**Format:** `[Component]AdvancedTest.kt`

**Examples:**
- `SecurityManagerAdvancedTest.kt` - Comprehensive security testing
- `BranchCoverageAdvancedTest.kt` - Advanced branch coverage testing
- `NavigationFlowAdvancedTest.kt` - Complex navigation scenario testing

### 4. Feature Tests (`Test`)
Use for focused testing of specific components, features, or classes.

**Format:** `[Component]Test.kt`

**Examples:**
- `MainActivityTest.kt` - MainActivity testing
- `SecurityManagerTest.kt` - Basic SecurityManager testing
- `CameraCaptureTest.kt` - Camera functionality testing
- `BakingViewModelTest.kt` - ViewModel testing

### 5. Specialized Tests (`[Purpose]Test`)
Use for specific testing concerns that don't fit standard component testing.

**Format:** `[Purpose]Test.kt`

**Examples:**
- `ConfigurationChangeTest.kt` - Configuration change testing
- `AccessibilityComplianceTest.kt` - Accessibility compliance testing
- `EnhancedErrorHandlerTest.kt` - Error handling testing

## Naming Rules

### DO:
- Use descriptive names that clearly indicate what is being tested
- Use consistent suffixes based on the test type
- Make names self-documenting
- Use PascalCase for class names
- Include the purpose/component being tested

### DON'T:
- Use timeline-based names (e.g., "Phase3", "Phase4")
- Use vague or generic names
- Mix naming patterns inconsistently
- Use abbreviations that aren't universally understood

## Examples of Good vs. Bad Names

### ✅ Good Names:
- `PerformanceTestSuite.kt` - Clear purpose (performance testing)
- `SecurityManagerAdvancedTest.kt` - Component and complexity level clear
- `AccessibilityComplianceTest.kt` - Specific testing focus clear
- `TestStrategyValidationSuite.kt` - Meta-testing purpose clear

### ❌ Bad Names:
- `Phase3TestSuite.kt` - Timeline-based, unclear purpose
- `Phase4TestSuite.kt` - Timeline-based, unclear purpose
- `TestSuite.kt` - Too generic
- `AdvancedTest.kt` - Unclear what's being tested

## File Organization

Tests should be organized by:
1. **Type** (suites, validation, advanced, feature)
2. **Component** (by the main class/feature being tested)
3. **Purpose** (performance, security, accessibility, etc.)

## Class and Method Naming

### Class Names:
- Match the file name exactly
- Use PascalCase
- Include descriptive suffixes

### Test Method Names:
- Use backticks for descriptive names
- Follow Given-When-Then structure in names when appropriate
- Be specific about what is being tested

**Examples:**
```kotlin
@Test
fun `should validate performance under high load conditions`()

@Test
fun `accessibility compliance should meet WCAG 2.1 AA standards`()

@Test
fun `navigation flow should handle back stack correctly`()
```

## Consistency Check

When adding new test files:
1. Identify the primary purpose of the test
2. Choose the appropriate suffix based on the rules above
3. Ensure the name is descriptive and self-documenting
4. Check existing files to maintain consistency
5. Update this document if new patterns emerge

## Migration Notes

Previously used phase-based naming has been migrated to purpose-based naming:
- `Phase3TestSuite.kt` → `TestStrategyValidationSuite.kt`
- `Phase4TestSuite.kt` → `TestSuiteCoordinationValidationSuite.kt`

These changes improve clarity and maintainability for future developers. 