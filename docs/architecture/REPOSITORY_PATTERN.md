# Repository Pattern Implementation

## Overview

This document outlines the comprehensive Repository pattern implementation in the ForkSure Android app. The Repository pattern provides a clean abstraction layer between the data sources and the business logic, enabling better testability, maintainability, and separation of concerns.

## Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   UI Layer      │    │  Business Logic │    │   Data Layer    │
│                 │    │                 │    │                 │
│ • Composables   │◄──►│ • ViewModels    │◄──►│ • Repositories  │
│ • Activities    │    │ • Use Cases     │    │ • Data Sources  │
│ • Fragments     │    │                 │    │ • APIs/DB       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Repository Structure

### 1. Data Models (`data/model/`)

#### Recipe.kt
```kotlin
@Immutable
data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val prepTime: String? = null,
    val cookTime: String? = null,
    val servings: String? = null,
    val difficulty: DifficultyLevel = DifficultyLevel.UNKNOWN,
    val tags: List<String> = emptyList(),
    val nutritionInfo: NutritionInfo? = null,
    val warnings: List<String> = emptyList(),
    val source: RecipeSource,
    val confidence: Float = 0.0f,
    val createdAt: Date = Date(),
    val imageHash: String? = null
)
```

#### UserPreferences.kt
```kotlin
@Immutable
data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "en",
    val enableHapticFeedback: Boolean = true,
    val enableSoundEffects: Boolean = true,
    val cacheRecipes: Boolean = true,
    val maxCacheSize: Int = 50,
    val autoDeleteOldRecipes: Boolean = true,
    val cacheRetentionDays: Int = 30,
    // ... more preferences
)
```

### 2. Data Sources (`data/source/`)

#### Local Data Sources
- **PreferencesDataSource**: Manages user preferences using SharedPreferences
- **RecipeCacheDataSource**: Handles recipe caching with LRU cache

#### Remote Data Sources
- **AIRepository**: Interfaces with Google's Generative AI
- **SecurityRepository**: Manages security validations

### 3. Repository Implementations (`repository/`)

#### RecipeRepository
```kotlin
interface RecipeRepository {
    suspend fun analyzeRecipe(bitmap: Bitmap, prompt: String): Flow<RecipeAnalysisResponse>
    suspend fun getCachedRecipes(): List<Recipe>
    suspend fun getRecentRecipes(limit: Int = 10): List<Recipe>
    suspend fun getFavoriteRecipes(favoriteIds: Set<String>): List<Recipe>
    suspend fun searchRecipes(query: String): List<Recipe>
    suspend fun clearCache()
    suspend fun getCacheStatistics(): RecipeCacheDataSource.CacheStatistics
}
```

## Key Features

### 1. Intelligent Caching System

The recipe repository implements a sophisticated caching mechanism:

```kotlin
// Check cache first
val cachedRecipe = cacheDataSource.getCachedRecipe(request)
if (cachedRecipe != null) {
    val response = RecipeAnalysisResponse(
        recipe = cachedRecipe.copy(source = RecipeSource.CACHED),
        rawResponse = "Cached result",
        processingTime = System.currentTimeMillis() - startTime,
        success = true
    )
    emit(response)
    return@flow
}
```

**Benefits:**
- Reduces API calls and improves performance
- Provides offline access to previously analyzed recipes
- Implements LRU eviction policy for memory management
- Tracks cache statistics and hit rates

### 2. AI Response Processing

The repository handles complex AI response parsing:

```kotlin
private fun parseAIResponse(response: String, imageHash: String): Recipe {
    val lines = response.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    
    var title = "Recipe Analysis"
    val ingredients = mutableListOf<String>()
    val instructions = mutableListOf<String>()
    
    // Intelligent parsing logic
    lines.forEach { line ->
        when {
            line.matches(Regex("^\\d+.*")) || 
            line.startsWith("•") || 
            line.contains("cup", ignoreCase = true) -> {
                ingredients.add(line)
            }
            line.contains("step", ignoreCase = true) ||
            line.contains("mix", ignoreCase = true) -> {
                instructions.add(line)
            }
        }
    }
    
    return Recipe(/* ... */)
}
```

### 3. User Preferences Management

Comprehensive user settings management:

```kotlin
interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    val usageStats: Flow<AppUsageStats>
    
    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateHapticFeedback(enabled: Boolean)
    suspend fun updateCacheSettings(enabled: Boolean, maxSize: Int, retentionDays: Int)
    suspend fun addFavoriteRecipe(recipeId: String)
    suspend fun incrementAnalysisCount()
    suspend fun exportSettings(): String
}
```

### 4. Security Integration

Security validation is seamlessly integrated:

```kotlin
// Security checks are handled at the repository level
when (val aiResult = aiRepository.generateContent(bitmap, prompt)) {
    is AIResponseProcessingResult.Success -> {
        // Process successful response
    }
    is AIResponseProcessingResult.Blocked -> {
        // Handle blocked content
    }
    is AIResponseProcessingResult.Error -> {
        // Handle errors
    }
}
```

## Data Flow

### Recipe Analysis Flow

1. **UI Layer**: User captures/selects image and enters prompt
2. **ViewModel**: Calls `recipeRepository.analyzeRecipe()`
3. **Repository**: 
   - Generates image hash for caching
   - Checks local cache first
   - If not cached, calls AI repository
   - Parses AI response into structured Recipe object
   - Caches result for future use
   - Returns Flow<RecipeAnalysisResponse>
4. **UI Layer**: Displays results to user

### Preferences Flow

1. **UI Layer**: User changes a setting
2. **ViewModel**: Calls appropriate repository method
3. **Repository**: Updates data source
4. **Data Source**: Persists to SharedPreferences
5. **Repository**: Emits updated preferences via Flow
6. **UI Layer**: Reacts to preference changes

## Dependency Injection

All repositories are provided through Hilt:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindRecipeRepository(
        recipeRepositoryImpl: RecipeRepositoryImpl
    ): RecipeRepository
    
    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository
}
```

## Testing Strategy

### Repository Testing

```kotlin
@Test
fun `analyzeRecipe should return cached result when available`() = runTest {
    // Given
    val request = RecipeAnalysisRequest(imageHash = "test", prompt = "test")
    val cachedRecipe = Recipe(/* test data */)
    coEvery { cacheDataSource.getCachedRecipe(request) } returns cachedRecipe
    
    // When
    val result = repository.analyzeRecipe(mockBitmap, "test").first()
    
    // Then
    assertThat(result.success).isTrue()
    assertThat(result.recipe?.source).isEqualTo(RecipeSource.CACHED)
}
```

### Fake Implementations

For testing, we provide fake implementations:

```kotlin
class FakeRecipeRepository : RecipeRepository {
    private val recipes = mutableListOf<Recipe>()
    
    override suspend fun analyzeRecipe(bitmap: Bitmap, prompt: String): Flow<RecipeAnalysisResponse> = flow {
        // Controllable test behavior
        emit(RecipeAnalysisResponse(/* test data */))
    }
}
```

## Performance Optimizations

### 1. Caching Strategy

- **LRU Cache**: Automatically evicts least recently used items
- **Size Management**: Configurable cache size based on user preferences
- **Time-based Expiration**: Automatic cleanup of old entries
- **Memory Efficiency**: Stores only essential data in cache

### 2. Coroutine Usage

- **Background Processing**: All repository operations use appropriate dispatchers
- **Flow-based**: Reactive data streams for real-time updates
- **Cancellation Support**: Proper coroutine cancellation handling

### 3. Image Processing

- **Hash Generation**: Efficient image hashing for cache keys
- **Compression**: Optimized image compression for storage
- **Memory Management**: Proper bitmap handling to prevent memory leaks

## Error Handling

### Repository-Level Error Handling

```kotlin
try {
    // Perform operation
    when (val aiResult = aiRepository.generateContent(bitmap, prompt)) {
        is AIResponseProcessingResult.Success -> {
            // Handle success
        }
        is AIResponseProcessingResult.Error -> {
            emit(RecipeAnalysisResponse(
                recipe = null,
                success = false,
                errorMessage = aiResult.message
            ))
        }
    }
} catch (e: Exception) {
    emit(RecipeAnalysisResponse(
        recipe = null,
        success = false,
        errorMessage = "Analysis failed: ${e.message}"
    ))
}
```

### Error Recovery

- **Graceful Degradation**: App continues to function even when some features fail
- **Retry Mechanisms**: Built-in retry logic for transient failures
- **Offline Support**: Cached data available when network is unavailable

## Future Enhancements

### 1. Database Integration

```kotlin
// Future: Room database integration
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    // ... other fields
)

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)
}
```

### 2. Remote Synchronization

```kotlin
// Future: Cloud sync capability
interface CloudSyncRepository {
    suspend fun syncRecipes(): Result<Unit>
    suspend fun uploadRecipe(recipe: Recipe): Result<String>
    suspend fun downloadRecipes(): Result<List<Recipe>>
}
```

### 3. Advanced Analytics

```kotlin
// Future: Enhanced analytics
interface AnalyticsRepository {
    suspend fun trackRecipeAnalysis(recipe: Recipe, processingTime: Long)
    suspend fun trackUserBehavior(event: AnalyticsEvent)
    suspend fun generateUsageReport(): UsageReport
}
```

## Best Practices Implemented

### 1. Single Source of Truth

Each repository serves as the single source of truth for its domain:
- Recipe data flows through RecipeRepository
- User preferences flow through UserPreferencesRepository
- Security operations flow through SecurityRepository

### 2. Separation of Concerns

- **Data Sources**: Handle raw data operations
- **Repositories**: Provide business logic and data transformation
- **ViewModels**: Consume repository data and manage UI state

### 3. Reactive Programming

- **Flow-based APIs**: All data streams use Kotlin Flow
- **Real-time Updates**: UI automatically updates when data changes
- **Backpressure Handling**: Proper flow collection and cancellation

### 4. Dependency Inversion

- **Interface-based Design**: All repositories implement interfaces
- **Dependency Injection**: Hilt provides implementations
- **Testability**: Easy to mock and test individual components

## Conclusion

The Repository pattern implementation in ForkSure provides:

1. **Clean Architecture**: Clear separation between data and business logic
2. **Testability**: Easy to unit test with fake implementations
3. **Maintainability**: Modular design supports future enhancements
4. **Performance**: Intelligent caching and optimization strategies
5. **Reliability**: Comprehensive error handling and recovery mechanisms

This implementation serves as a solid foundation for the app's data management needs while remaining flexible for future requirements and enhancements. 