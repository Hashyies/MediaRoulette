# 🎲 New Chances Configuration System

## Overview

The new `/chances` command completely replaces the old config system with a modern, user-friendly interface for managing image source chances and preferences. This system uses Discord's latest UI components including modals, select menus, and interactive buttons with pagination.

## Features

### 🎨 Modern UI Components
- **Interactive Embeds**: Rich, colorful embeds with real-time statistics
- **Pagination**: Navigate through sources with Previous/Next buttons
- **Filtering**: Filter sources by status, chance levels, and more
- **Modals**: Clean popup forms for editing chance values
- **Select Menus**: Dropdown filters for easy navigation

### 🔧 Functionality
- **Toggle Sources**: Enable/disable individual image sources
- **Edit Chances**: Set custom chance percentages (0-100%)
- **Bulk Operations**: Enable/disable all sources at once
- **Reset to Defaults**: Restore original settings
- **Real-time Preview**: See changes before saving
- **Unsaved Changes Tracking**: Clear indication of pending changes

### 📊 Statistics & Information
- **Live Statistics**: Total sources, enabled/disabled counts, total chance percentage
- **Source Information**: Each source shows status, chance, and emoji indicators
- **Filter Results**: See how many sources match current filter
- **Status Indicators**: Visual feedback for enabled/disabled sources

## Usage

### Basic Commands
```
/chances
```
Opens the main chances configuration interface.

### Interface Components

#### 🔍 Filter Dropdown
- **All Sources**: Show all available image sources
- **Enabled Only**: Show only currently enabled sources  
- **Disabled Only**: Show only disabled sources
- **High Chance**: Sources with chance > 10%
- **Low Chance**: Sources with chance ≤ 10%

#### 🎛️ Navigation Buttons
- **◀ Previous**: Go to previous page
- **Next ▶**: Go to next page
- **💾 Save Changes**: Apply all pending changes

#### ⚡ Quick Actions
- **🟢 Enable All**: Enable all image sources
- **🔴 Disable All**: Disable all image sources
- **🔄 Reset to Default**: Restore original settings

#### 🎯 Source Controls
- **🟢/🔴 Enable/Disable**: Toggle individual sources
- **✏️ Edit**: Open modal to edit chance percentage

### Editing Chances

1. Click the **✏️ Edit [Source]** button for any source
2. A modal popup will appear with the current chance value
3. Enter a new percentage (0-100)
4. Click Submit to apply the change
5. Remember to click **💾 Save Changes** to persist your settings

## Image Sources

The system supports the following image sources:

| Source | Emoji | Description |
|--------|-------|-------------|
| Reddit | 🔴 | Reddit image posts |
| Imgur | 🟢 | Imgur image hosting |
| 4Chan | 🍀 | 4Chan image boards |
| Picsum | 🖼️ | Lorem Picsum placeholder images |
| Rule34 | 🔞 | Rule34 content (NSFW) |
| Tenor | 🎭 | Tenor GIF service |
| Google | 🔍 | Google image search |
| Movies | 🎬 | Movie posters and stills |
| TV Shows | 📺 | TV show images |
| YouTube | 📹 | YouTube video thumbnails |
| YouTube Shorts | ⏱️ | YouTube Shorts content |
| Urban Dictionary | 📚 | Urban Dictionary definitions |

## Technical Details

### Session Management
- Each user gets a temporary session for managing changes
- Sessions store working copies of settings until saved
- Unsaved changes are clearly indicated
- Sessions automatically refresh if expired

### Data Validation
- Chance values must be between 0-100
- Invalid inputs show clear error messages
- Changes are validated before saving
- Database updates only occur on explicit save

### Performance Features
- Pagination prevents UI overload
- Filtering reduces cognitive load
- Async operations prevent blocking
- Efficient session cleanup

## Migration from Old Config

The new system automatically:
- Imports existing user settings
- Initializes default values for new users
- Maintains backward compatibility
- Preserves all user preferences

Users can continue using the old `/config` command, but the new `/chances` command provides a much better experience.

## Benefits Over Old System

### ❌ Old Config Problems
- Complex text-based configuration
- Hard to understand chance format
- No visual feedback
- Difficult to navigate
- Error-prone manual entry
- No preview of changes

### ✅ New Chances Solutions
- Visual, interactive interface
- Clear percentage-based chances
- Real-time statistics and feedback
- Easy pagination and filtering
- Guided input with validation
- Preview changes before saving

## Future Enhancements

Planned improvements include:
- **Preset Configurations**: Save and load chance presets
- **Advanced Filtering**: More filter options and sorting
- **Chance Templates**: Quick-apply common configurations
- **Usage Analytics**: See which sources are actually used
- **Bulk Edit**: Edit multiple sources simultaneously
- **Import/Export**: Share configurations between users

## Support

If you encounter any issues with the new chances system:
1. Try using `/chances` again to refresh your session
2. Check that your input values are valid (0-100 for chances)
3. Make sure to save your changes before closing
4. Report any bugs to the development team

The new system is designed to be intuitive and user-friendly. Enjoy the improved experience! 🎉