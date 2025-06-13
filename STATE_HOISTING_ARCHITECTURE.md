# State Hoisting Architecture - ForkSure App

## Overview

ForkSure now implements **proper state hoisting** following Jetpack Compose best practices. State hoisting moves state up to the lowest common ancestor of components that need to share it, making components stateless, reusable, and easier to test.

## State Hoisting Principles Applied

### 1. **Single Source of Truth**
- All UI state is centralized in dedicated state holder classes
- No duplicate state across components
- Clear ownership of state

### 2. **Unidirectional Data Flow**
- State flows down through parameters
- Events flow up through callbacks
- No direct state mutation in child components

### 3. **Stateless Components**
- Components receive state as parameters
- Components communicate through callbacks
- Components are pure functions of their inputs

## Architecture Components

### 1. State Holders

#### MainScreenState (`state/MainScreenState.kt`)
```kotlin
@Stable
class MainScreenState(
    initialPrompt: String = "",
    initialResult: String = "",
    initialSelectedImageIndex: Int = 0
) {
    // All UI state with private setters
    var prompt by mutableStateOf(initialPrompt) private set
    var result by mutableStateOf(initialResult) private set
    var selectedImageIndex by mutableIntStateOf(initialSelectedImageIndex) private set
    var capturedImage by mutableStateOf<Bitmap?>(null) private set
    var showReportDialog by mutableStateOf(false) private set
    
    // Derived state
    val isAnalyzeEnabled: Boolean
        get() = prompt.isNotEmpty() && (capturedImage != null || selectedImageIndex >= 0)
    
    // State update functions
    fun updatePrompt(newPrompt: String) { prompt = newPrompt }
    fun selectSampleImage(index: Int) { /* ... */ }
    // ... other update functions
}
```

**Key Features:**
- `@Stable` annotation for Compose optimization
- Private setters prevent external mutation
- Derived state computed from base state
- Clear update functions for state changes

#### ContentReportDialogState (`state/MainScreenState.kt`)
```kotlin
@Stable
class ContentReportDialogState(
    initialReason: ContentReportingHelper.ReportReason = ContentReportingHelper.ReportReason.INAPPROPRIATE
) {
    var selectedReason by mutableStateOf(initialReason) private set
    var additionalDetails by mutableStateOf("") private set
    
    fun updateSelectedReason(reason: ContentReportingHelper.ReportReason) { /* ... */ }
    fun updateAdditionalDetails(details: String) { /* ... */ }
    fun createReport(content: String): ContentReportingHelper.ContentReport { /* ... */ }
}
```

#### NavigationState (`state/NavigationState.kt`)
```kotlin
@Stable
class NavigationState(initialSelectedImageIndex: Int = 0) {
    private val _selectedImageState = mutableIntStateOf(initialSelectedImageIndex)
    val selectedImageState: MutableIntState = _selectedImageState
    
    var capturedImage by mutableStateOf<Bitmap?>(null) private set
    
    // Navigation-specific state management
    fun updateCapturedImage(bitmap: Bitmap?) { /* ... */ }
    fun selectCapturedImage() { /* ... */ }
}
```

### 2. Actions Interface

#### MainScreenActions (`state/MainScreenState.kt`)
```kotlin
interface MainScreenActions {
    fun onPromptChange(prompt: String)
    fun onSampleImageSelected(index: Int)
    fun onCapturedImageSelected()
    fun onCapturedImageUpdated(bitmap: Bitmap?)
    fun onAnalyzeClick()
    fun onNavigateToCamera()
    fun onShowReportDialog()
    fun onHideReportDialog()
    fun onReportSubmitted(report: ContentReportingHelper.ContentReport)
    fun onRetryAnalysis()
    fun onDismissError()
}
```

**Benefits:**
- Clear contract for all possible actions
- Type-safe action handling
- Easy to mock for testing
- Separates UI events from business logic

### 3. Stateless Components

#### Before State Hoisting
```kotlin
@Composable
fun ContentReportDialog(
    content: String,
    onDismiss: () -> Unit,
    onReportSubmitted: (ContentReportingHelper.ContentReport) -> Unit
) {
    // ❌ State managed inside component
    var selectedReason by remember { mutableStateOf(ContentReportingHelper.ReportReason.INAPPROPRIATE) }
    var additionalDetails by remember { mutableStateOf("") }
    
    // Component logic...
}
```

#### After State Hoisting
```kotlin
@Composable
fun StatelessContentReportDialog(
    content: String,
    selectedReason: ContentReportingHelper.ReportReason,
    additionalDetails: String,
    onReasonSelected: (ContentReportingHelper.ReportReason) -> Unit,
    onAdditionalDetailsChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onReportSubmitted: (ContentReportingHelper.ContentReport) -> Unit
) {
    // ✅ All state passed as parameters
    // ✅ All actions passed as callbacks
    // Component is now stateless and reusable
}
```

## State Flow Architecture

### Data Flow Diagram
```
┌─────────────────┐
│   Navigation    │
│     State       │ ← Navigation-level state
└─────────────────┘
         │
         ▼
┌─────────────────┐
│   MainScreen    │
│     State       │ ← Screen-level state
└─────────────────┘
         │
         ▼
┌─────────────────┐
│   Component     │
│   Parameters    │ ← Component-level props
└─────────────────┘
```

### Event Flow Diagram
```
┌─────────────────┐
│   User Action   │
└─────────────────┘
         │
         ▼
┌─────────────────┐
│   Component     │
│   Callback      │
└─────────────────┘
         │
         ▼
┌─────────────────┐
│   Actions       │
│  Interface      │
└─────────────────┘
         │
         ▼
┌─────────────────┐
│   State         │
│   Update        │
└─────────────────┘
```

## Implementation Examples

### 1. MainScreen State Hoisting

#### Before
```kotlin
@Composable
fun MainScreen(/* ... */) {
    // ❌ State scattered throughout component
    var prompt by rememberSaveable { mutableStateOf(placeholderPrompt) }
    var result by rememberSaveable { mutableStateOf(placeholderResult) }
    var showReportDialog by remember { mutableStateOf(false) }
    
    // ❌ Direct state mutations
    onSampleImageSelected = { index ->
        selectedImage.intValue = index
        onCapturedImageUpdated(null)
    }
}
```

#### After
```kotlin
@Composable
fun MainScreen(/* ... */) {
    // ✅ Centralized state management
    val mainScreenState = rememberMainScreenState(
        initialPrompt = placeholderPrompt,
        initialResult = placeholderResult,
        initialSelectedImageIndex = selectedImage.value
    )
    
    // ✅ Actions interface
    val actions = remember(mainScreenState, bakingViewModel) {
        DefaultMainScreenActions(
            state = mainScreenState,
            onNavigateToCamera = onNavigateToCamera,
            onAnalyze = { bitmap, prompt -> bakingViewModel.sendPrompt(bitmap, prompt, context) },
            // ... other actions
        )
    }
    
    // ✅ Stateless content
    MainScreenContent(
        state = mainScreenState,
        actions = actions,
        uiState = uiState,
        bakingViewModel = bakingViewModel
    )
}
```

### 2. Dialog State Hoisting

#### Before
```kotlin
@Composable
private fun MainResultsSection(/* ... */) {
    // ❌ Dialog state managed in results section
    var showReportDialog by remember { mutableStateOf(false) }
    
    if (showReportDialog) {
        ContentReportDialog(
            content = uiState.outputText,
            onDismiss = { showReportDialog = false },
            onReportSubmitted = { /* ... */ }
        )
    }
}
```

#### After
```kotlin
@Composable
private fun MainResultsSection(
    // ✅ Dialog state passed as parameters
    showReportDialog: Boolean,
    onShowReportDialog: () -> Unit,
    onHideReportDialog: () -> Unit,
    onReportSubmitted: (ContentReportingHelper.ContentReport) -> Unit,
    /* ... */
) {
    if (showReportDialog) {
        StatelessContentReportDialog(
            content = uiState.outputText,
            onDismiss = onHideReportDialog,
            onReportSubmitted = onReportSubmitted
        )
    }
}
```

## Benefits Achieved

### 1. **Improved Testability**
```kotlin
// ✅ Easy to test state logic
@Test
fun `updatePrompt should update prompt state`() {
    val state = MainScreenState()
    state.updatePrompt("new prompt")
    assertEquals("new prompt", state.prompt)
}

// ✅ Easy to test component behavior
@Test
fun `StatelessContentReportDialog calls onReasonSelected when reason changes`() {
    var selectedReason: ReportReason? = null
    
    composeTestRule.setContent {
        StatelessContentReportDialog(
            selectedReason = ReportReason.INAPPROPRIATE,
            onReasonSelected = { selectedReason = it },
            // ... other parameters
        )
    }
    
    // Test interaction
    composeTestRule.onNodeWithText("Offensive").performClick()
    assertEquals(ReportReason.OFFENSIVE, selectedReason)
}
```

### 2. **Better Reusability**
```kotlin
// ✅ Stateless components can be reused in different contexts
@Composable
fun PreviewContentReportDialog() {
    StatelessContentReportDialog(
        content = "Sample content",
        selectedReason = ReportReason.INAPPROPRIATE,
        additionalDetails = "",
        onReasonSelected = {},
        onAdditionalDetailsChanged = {},
        onDismiss = {},
        onReportSubmitted = {}
    )
}
```

### 3. **Clearer Data Flow**
- State changes are explicit and traceable
- No hidden state mutations
- Clear separation between UI and business logic

### 4. **Better Performance**
- `@Stable` annotations enable Compose optimizations
- Reduced unnecessary recompositions
- Efficient state updates

## State Persistence

### Automatic Persistence
```kotlin
// ✅ State automatically persisted across configuration changes
val mainScreenState = rememberMainScreenState(
    initialPrompt = placeholderPrompt,
    initialResult = placeholderResult
)
```

### Manual Persistence
```kotlin
// ✅ Custom persistence for complex state
@Composable
fun rememberMainScreenStateWithPersistence(): MainScreenState {
    return rememberSaveable(
        saver = MainScreenState.Saver
    ) {
        MainScreenState()
    }
}
```

## Migration Guide

### Step 1: Identify State
- Find all `remember { mutableStateOf() }` calls
- Identify shared state between components
- Map state dependencies

### Step 2: Create State Holders
- Create `@Stable` state holder classes
- Move state to state holders
- Add derived state properties
- Implement state update functions

### Step 3: Create Actions Interface
- Define all possible actions
- Create default implementation
- Replace direct state mutations with action calls

### Step 4: Make Components Stateless
- Pass state as parameters
- Pass actions as callbacks
- Remove internal state management

### Step 5: Update Tests
- Test state holders independently
- Test components with mock state
- Test action implementations

## Best Practices

### 1. **State Holder Design**
```kotlin
@Stable
class MyState {
    // ✅ Private setters
    var value by mutableStateOf("") private set
    
    // ✅ Derived state
    val isValid: Boolean get() = value.isNotEmpty()
    
    // ✅ Clear update functions
    fun updateValue(newValue: String) {
        value = newValue
    }
}
```

### 2. **Actions Interface**
```kotlin
// ✅ Use interface for flexibility
interface MyActions {
    fun onValueChange(value: String)
    fun onSubmit()
}

// ✅ Provide default implementation
class DefaultMyActions(
    private val state: MyState,
    private val onSubmit: (String) -> Unit
) : MyActions {
    override fun onValueChange(value: String) = state.updateValue(value)
    override fun onSubmit() = onSubmit(state.value)
}
```

### 3. **Component Design**
```kotlin
// ✅ Stateless component
@Composable
fun MyComponent(
    state: MyState,
    actions: MyActions
) {
    // Component implementation using state and actions
}

// ✅ Stateful wrapper (optional)
@Composable
fun MyComponentStateful() {
    val state = rememberMyState()
    val actions = remember { DefaultMyActions(state) { /* ... */ } }
    
    MyComponent(state = state, actions = actions)
}
```

## Conclusion

The state hoisting implementation in ForkSure provides:

- **Clear separation of concerns** between UI and state management
- **Improved testability** with isolated state logic
- **Better reusability** with stateless components
- **Enhanced performance** through Compose optimizations
- **Maintainable architecture** that scales with app complexity

This architecture follows Jetpack Compose best practices and provides a solid foundation for future development while maintaining all existing functionality and accessibility features. 