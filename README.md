# SolarSeasons

An Android app used to get UV information including the UV level, skin exposure durations, and solar times at the user's location from anywhere in the world. Includes a homescreen App Widget with regular background updates. Uses the [OpenUV](https://www.openuv.io/) API.

An API key is required due to limited free requests. Only a Google account is needed to get the key. Once obtained, add the key to the root [apiKeys.properties](./apiKeys.properties) file, assigned to the `OPENUV_API_KEY` property.

The app also uses the [WeatherAPI](https://www.weatherapi.com/) API to get cloud cover data. A new account is needed to obtain a key for this API. Once obtained, the key can also be placed in the [apiKeys.properties](./apiKeys.properties), assigned to the `WEATHER_API_KEY` property.

To avoid adding API keys in a commit, run the following command:
`git update-index --skip-worktree apiKeys.properties` after modifying the file.
