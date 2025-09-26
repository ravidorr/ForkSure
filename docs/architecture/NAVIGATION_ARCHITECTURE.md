# Navigation Architecture - ForkSure App

## Overview

ForkSure now uses **Navigation Compose** for modern, type-safe navigation between screens. This replaces the previous boolean-based navigation system (`showCamera`) with a proper navigation architecture that follows Android best practices.

## Architecture Components

### 1. Navigation Graph (`ForkSureNavigation.kt`)
- **Location**: `app/src/main/java/com/ravidor/forksure/navigation/ForkSureNavigation.kt`
- **Purpose**: Defines the navigation graph and manages navigation between screens
- **Key Features**:
  - Centralized navigation logic
  - Accessibility announcements for screen transitions
  - Shared state management between screens
  - Proper back stack handling

### 2. Screen Components

#### MainScreen (`screens/MainScreen.kt`)
- **Purpose**: Main app screen with image selection, prompt input, and results
- **Features**:
  - Image selection (captured or sample images)
  - AI prompt input and analysis
  - Results display with error handling
  - Content reporting functionality

#### CameraCapture (existing)
- **Purpose**: Camera screen for capturing photos
- **Integration**: Now properly integrated with Navigation Compose
- **Navigation**: Automatically returns to main screen after capture

### 3. Navigation Constants
- **Location**: `Constants.kt` → `NavigationConstants` object
- **Routes**:
  - `ROUTE_MAIN = "main"` - Main screen route
  - `ROUTE_CAMERA = "camera"` - Camera screen route
  - `ROUTE_RESULTS = "results"` - Results screen route (future use)

## Navigation Flow

```
MainActivity
    ↓
ForkSureNavigation (NavHost)
    ├── MainScreen (startDestination)
    └── CameraCapture
```

### Navigation Actions

1. **Take Photo**: `MainScreen` → `CameraCapture`
   ```kotlin
   onNavigateToCamera = {
       navController.navigate(ROUTE_CAMERA)
   }
   ```

2. **Photo Captured**: `CameraCapture` → `MainScreen`
   ```kotlin
   onImageCaptured = { bitmap ->
       capturedImage = bitmap
       navController.popBackStack()
   }
   ```

3. **Error Handling**: `CameraCapture` → `MainScreen`
   ```kotlin
   onError = { error ->
       navController.popBackStack()
   }
   ```

## State Management

### Shared State
- **Captured Image**: `Bitmap?` - Shared between screens
- **Selected Image Index**: `MutableIntState` - Tracks sample image selection
- **Navigation State**: Managed by NavController

### State Persistence
- **Prompt Text**: Saved using `rememberSaveable`
- **Results Text**: Saved using `rememberSaveable`
- **Image Selection**: Maintained in navigation state

## Accessibility Features

### Navigation Announcements
- Screen reader announcements for navigation transitions
- Haptic feedback for navigation actions
- Proper content descriptions for navigation elements

### Implementation
```kotlin
LaunchedEffect(Unit) {
    if (AccessibilityHelper.isScreenReaderEnabled(context)) {
        AccessibilityHelper.announceForAccessibility(
            context, 
            ACCESSIBILITY_NAVIGATION_TO_MAIN
        )
    }
}
```

## Migration from Old System

### Before (Boolean-based Navigation)
```kotlin
var showCamera by remember { mutableStateOf(false) }

if (showCamera) {
    CameraCapture() // CameraCapture(...)
} else {
    BakingMainContent() // BakingMainContent(...)
}
```

### After (Navigation Compose)
```kotlin
NavHost(navController, startDestination = ROUTE_MAIN) {
    composable(ROUTE_MAIN) { MainScreen() } // MainScreen(...)
    composable(ROUTE_CAMERA) { CameraCapture() } // CameraCapture(...)
}
```

## Benefits of New Architecture

### 1. **Type Safety**
- Compile-time route validation
- No more boolean state management
- Clear navigation contracts

### 2. **Better UX**
- Proper back stack handling
- System back button support
- Predictable navigation behavior

### 3. **Scalability**
- Easy to add new screens
- Centralized navigation logic
- Support for deep linking (future)

### 4. **Accessibility**
- Automatic navigation announcements
- Better screen reader support
- Consistent haptic feedback

### 5. **Testing**
- Easier to test navigation flows
- Mockable navigation components
- Clear separation of concerns

## Future Enhancements

### 1. **Deep Linking**
```kotlin
// Future implementation
composable(
    route = "camera",
    deepLinks = listOf(navDeepLink { uriPattern = "forksure://camera" })
) {
    CameraCapture() // CameraCapture(...)
}
```

### 2. **Navigation Arguments**
```kotlin
// Future implementation for passing data
composable(
    route = "results/{imageId}",
    arguments = listOf(navArgument("imageId") { type = NavType.StringType })
) { backStackEntry ->
    val imageId = backStackEntry.arguments?.getString("imageId")
    ResultsScreen(imageId = imageId)
}
```

### 3. **Nested Navigation**
```kotlin
// Future implementation for complex flows
navigation(startDestination = "camera_setup", route = "camera_flow") {
    composable("camera_setup") { CameraSetupScreen() }
    composable("camera_capture") { CameraCapture() }
    composable("camera_preview") { CameraPreviewScreen() }
}
```

## Best Practices

### 1. **Navigation Actions**
- Always use navigation callbacks, not direct NavController access
- Provide haptic feedback for navigation actions
- Handle navigation errors gracefully

### 2. **State Management**
- Keep navigation state in the navigation graph
- Use `rememberSaveable` for persistent state
- Share state through navigation parameters when needed

### 3. **Accessibility**
- Always announce navigation changes
- Provide clear content descriptions
- Test with TalkBack enabled

### 4. **Error Handling**
- Handle navigation failures gracefully
- Provide fallback navigation paths
- Log navigation events for debugging

## Testing Navigation

### Unit Tests
```kotlin
@Test
fun `navigation to camera screen works correctly`() {
    // Test navigation logic
}
```

### UI Tests
```kotlin
@Test
fun `camera navigation maintains state correctly`() {
    // Test full navigation flow
}
```

### Accessibility Tests
```kotlin
@Test
fun `navigation announcements work with TalkBack`() {
    // Test accessibility features
}
```

## Dependencies

### Required Dependencies
```kotlin
// Navigation Compose
implementation("androidx.navigation:navigation-compose:2.7.6")

// Lifecycle Compose (for state management)
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
```

### Version Compatibility
- **Navigation Compose**: 2.7.6
- **Compose BOM**: Latest stable
- **Kotlin**: 1.9.0+

## Troubleshooting

### Common Issues

1. **State Loss on Navigation**
   - Use `rememberSaveable` for important state
   - Consider using ViewModel for complex state

2. **Back Button Behavior**
   - Ensure proper back stack configuration
   - Handle system back button in navigation

3. **Accessibility Issues**
   - Test with TalkBack enabled
   - Verify navigation announcements work

### Debug Navigation
```kotlin
// Add logging to track navigation
navController.addOnDestinationChangedListener { _, destination, _ ->
    Log.d("Navigation", "Navigated to: ${destination.route}")
}
```

## Conclusion

The new Navigation Compose architecture provides a robust, scalable, and accessible navigation system for ForkSure. It replaces the previous boolean-based system with modern Android navigation patterns while maintaining all existing functionality and improving the user experience. 