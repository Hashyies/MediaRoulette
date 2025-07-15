# Dictionary System - Reddit Support Added

## ✅ **Reddit Integration Complete!**

Reddit has been added as a supported source for custom dictionaries. Users can now create dictionaries with subreddit names and assign them to Reddit for more targeted content.

### 🎯 **How Reddit Dictionary Works:**

1. **Create Subreddit Dictionary:**
   ```
   /dictionary create name:"Anime Subreddits" description:"Subreddits for anime content"
   ```

2. **Add Subreddit Names:**
   ```
   /dictionary edit <id>
   ```
   Add subreddits like: `anime, manga, animemes, wholesomeanimemes, animegifs`

3. **Assign to Reddit:**
   ```
   /settings assign source:reddit dictionary:<id>
   ```

4. **Result:** When Reddit provider needs a subreddit, it will use your custom list instead of the default subreddit list!

### 🔧 **Technical Implementation:**

- **Smart Validation:** Dictionary subreddits are validated against the existing subreddit list
- **Fallback System:** If dictionary subreddit doesn't exist, falls back to random subreddit
- **User-Specific:** Each user can have their own subreddit preferences
- **Logging:** Proper logging shows when dictionary subreddits are used

### 📋 **Updated Supported Sources:**

1. **Tenor GIFs** (`tenor`) - Uses dictionary words for search queries
2. **Google Images** (`google`) - Uses dictionary words for search queries  
3. **Reddit** (`reddit`) - Uses dictionary words as subreddit names

### 🆘 **Support Command Added:**

New `/support` command provides:
- Link to support server: https://discord.gg/632JUPJKPB
- Quick help with bot features
- Community access for real-time support

### 🧪 **Example Reddit Dictionary Usage:**

```bash
# Create a gaming subreddits dictionary
/dictionary create name:"Gaming Subs" description:"Gaming related subreddits"

# Add gaming subreddits
/dictionary edit <id>
# Add: gaming, pcmasterrace, nintendo, playstation, xbox, retrogaming

# Assign to Reddit
/settings assign source:reddit dictionary:<id>

# Now when you get random Reddit content, it will come from gaming subreddits!
```

### 🎮 **Popular Dictionary Ideas:**

- **Anime/Manga:** anime, manga, animemes, wholesomeanimemes
- **Gaming:** gaming, pcmasterrace, nintendo, playstation, xbox
- **Art:** art, digitalart, drawing, painting, photography
- **Cute Animals:** aww, cats, dogs, rabbits, birbs
- **Memes:** memes, dankmemes, wholesomememes, prequelmemes
- **Science:** science, space, physics, chemistry, biology

The dictionary system now supports all major content sources that use search terms or categories! 🚀