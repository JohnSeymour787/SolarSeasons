# SolarSeasons

An Android app used to get UV information including the UV level, skin exposure durations, and solar times at the user's location from anywhere in the world. Includes a homescreen App Widget with regular background updates. Uses the [OpenUV](https://www.openuv.io/) API.

An API key is required due to limited free requests. Only a Google account is needed to get the key. Once obtained, add the key to the [APIKeys.kt](./app/src/main/java/com/johnseymour/solarseasons/api/APIKeys.kt) file.
To avoid adding the API key in a commit, run the following command `git update-index --skip-worktree app/src/main/java/com/johnseymour/solarseasons/api/APIKeys.kt` after modifying the file.
