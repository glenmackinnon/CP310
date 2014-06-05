 Readme.MD  -- homework-314-glenmack
 * 
  * -- Wanted to implement "pull to refresh" but didn't get through it - refresh icon is poor man's compromise, although
 *    it does push Delete off screen in many cases
 * -- interpreted "both C&F" from HRD as a toggle allowing switching between, not both sets of data onscreen at once
 * -- have three default locations, with Seattle being the first (to satisfy HRD) then Mercer Island (home) + Kona (ideal home)
 * -- WU supports Canadian postal codes, so regex on input allows ZIP or postal code
 * -- icons are from "meteocons" - http://www.alessioatzeni.com/meteocons - I converted them from SVG to PNG
 *    after issues with SVG rendering code :-)
 * -- FragmentStatePagerAdapter is interesting (allows for paging through) but makes managing the fragments kind of
 *    tricky - resorted to brute force recreation of fragments instead of saving/restoring state in a few cases, but at
 *    least these frags are pretty lightweight and the weather data itself isnt refetched
 * -- layouts are good but not perfect for all devices
 * -- drawing is done by Weather class into layout from WeatherFragment - longer term i may have refactored the drawing
 *    code back into the fragment and left Weather "data & fetch only"
 * -- JSON parsing code feels "hard coded" and frail - looked briefly at GSON but decided against it for simplicity
 * -- JSON "frail" - if elements missing, or JSON ever changes, it'll catch, and UI will show whatever fields it did
 *    manage to read, as well as marking the data as 'bad' so it will be refetched onthe next try instead of caching
 * -- minimal error handling or display around JSON reading... go "refresh" button!
 * -- no error handling if ZIP is mis-entered (only those passing regex are kept)
 * 
 * -- For weather feed, this uses WeatherUnderground "free" API tier - JSON based; Caveats:
 *      -- Max 10 calls per minute  (so I've added a "Throttle" class to make sure I dont go over 10 calls
 *         per minute - if it does, access key is disabled for the rest of the day (booo!)
 *      -- WU URL requires an account specific key right in the URL - the " " part
 *         For this exercise, I didnt attempt to hide it but in real world I imagine i'd be storing it in
 *         an Account and maybe even obscuring the web call itself
 *      -- Sometimes the "Celsius" version of the text forecast is blank in their data (not sure why)
 *      -- WU requires attribution logo - I put one  in the bottom right and linked it to their WunderMap radar map
 *      -- didnt use syncadapter for data - simple JSON fetch seemed quicker and easier for this simple case and small
 *         (16KB-ish) data stream
