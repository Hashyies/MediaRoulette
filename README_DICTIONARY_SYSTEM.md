# Dictionary System Implementation

## Overview
A comprehensive dictionary system has been implemented for MediaRoulette that allows users to create, manage, and assign custom dictionaries to different content sources (Tenor, Reddit, Google, etc.).

## Features

### 📚 Dictionary Management
- **Create dictionaries**: Users can create custom word lists with names and descriptions
- **Edit dictionaries**: Add/remove words, change visibility settings
- **Share dictionaries**: Make dictionaries public for other users
- **Default dictionaries**: System-provided dictionaries for fallback

### ⚙️ Source Assignment
- **Independent assignment**: Each source (Tenor, Reddit, etc.) can have its own dictionary
- **User-specific**: Each user can have different dictionary assignments
- **Fallback system**: Uses default dictionaries when no custom assignment exists

### 🔧 Commands Added

#### `/dictionary` - Dictionary Management
- `/dictionary create <name> [description]` - Create a new dictionary
- `/dictionary list` - List your dictionaries
- `/dictionary view <id>` - View dictionary details
- `/dictionary edit <id>` - Edit dictionary (add/remove words, toggle public)
- `/dictionary delete <id>` - Delete a dictionary
- `/dictionary public` - Browse public dictionaries

#### `/settings` - Dictionary Assignment
- `/settings assign <source> <dictionary_id>` - Assign dictionary to source
- `/settings view` - View current assignments
- `/settings unassign <source>` - Remove assignment (use default)

### 🎯 Supported Sources
- `tenor` - Tenor GIFs
- `reddit` - Reddit posts
- `google` - Google Images
- `imgur` - Imgur
- `4chan` - 4Chan
- `picsum` - Picsum
- `rule34xxx` - Rule34
- `youtube` - YouTube
- `movies` - Movies
- `tvshow` - TV Shows

## Database Schema

### Collections
- `dictionary` - Stores dictionary data
- `dictionary_assignment` - Stores user-source-dictionary mappings

### Dictionary Document
```json
{
  "_id": "uuid",
  "name": "My Dictionary",
  "description": "Custom words for...",
  "createdBy": "user_id",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z",
  "words": ["word1", "word2", "word3"],
  "isPublic": false,
  "isDefault": false,
  "usageCount": 0
}
```

### Assignment Document
```json
{
  "userId": "user_id",
  "source": "tenor",
  "dictionaryId": "dictionary_uuid",
  "assignedAt": "2024-01-01T00:00:00Z",
  "lastUsed": "2024-01-01T00:00:00Z",
  "usageCount": 5
}
```

## Integration

### Provider Integration
The system integrates with existing providers through `DictionaryIntegration.java`:
- Providers can request words for specific users and sources
- Automatic fallback to existing `RandomDictionaryLineFetcher` system
- Maintains backward compatibility

### Example Usage in Provider
```java
// In TenorProvider.java
if (query == null || query.isEmpty()) {
    if (userId != null) {
        query = DictionaryIntegration.getRandomWordForSource(userId, "tenor");
    } else {
        query = DictionaryIntegration.getRandomWordForSource("tenor");
    }
}
```

## Files Added/Modified

### New Files
- `src/main/java/me/hash/mediaroulette/model/Dictionary.java`
- `src/main/java/me/hash/mediaroulette/model/DictionaryAssignment.java`
- `src/main/java/me/hash/mediaroulette/repository/DictionaryRepository.java`
- `src/main/java/me/hash/mediaroulette/repository/MongoDictionaryRepository.java`
- `src/main/java/me/hash/mediaroulette/service/DictionaryService.java`
- `src/main/java/me/hash/mediaroulette/bot/commands/dictionary/DictionaryCommand.java`
- `src/main/java/me/hash/mediaroulette/bot/commands/dictionary/SettingsCommand.java`
- `src/main/java/me/hash/mediaroulette/utils/DictionaryIntegration.java`

### Modified Files
- `src/main/java/me/hash/mediaroulette/Main.java` - Added dictionary service initialization
- `src/main/java/me/hash/mediaroulette/bot/Bot.java` - Registered new commands
- `src/main/java/me/hash/mediaroulette/content/provider/impl/gifs/TenorProvider.java` - Integrated dictionary system

## Usage Examples

1. **Create a dictionary**:
   ```
   /dictionary create name:"Anime Terms" description:"Words related to anime"
   ```

2. **Add words** (via edit button):
   ```
   anime, manga, kawaii, senpai, otaku
   ```

3. **Assign to Tenor**:
   ```
   /settings assign source:tenor dictionary:abc-123-def
   ```

4. **View assignments**:
   ```
   /settings view
   ```

Now when Tenor needs a random word for that user, it will use words from their assigned "Anime Terms" dictionary instead of the default basic dictionary.