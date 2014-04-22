/**
 * Notes : Homework 311 - Glen MacKinnon (glenmack)
 * 
 *  You will need to import appcompat_v7 as well to compile this project (it's in the Git!)
 *
 * -- implemented both phone view and tablet (two column, list+details) using fragments
 * -- implemented a cursorLoader for the listview (in MainListFragment) in case there are a lot of entries,
 *      but just used the content provider (in UI thread) to get detail view/content *and* for loading the XML
 *      file from resource.   Will definitely rewrite that to load content asynchronously for HW312, figured I
 *      should leave something to do in that exercise :-)
 * 
 * -- used sensor shake code provided by example mentioned in class (http://www.survivingwithandroid.com/2014/04/android-shake-to-refresh-tutorial.html)
 * -- add, display and store date field that's mostly unused for now (HW312 sorts on date)
 * -- XML parsing code (using XPath) assumes well-formed, item/content pairs for this exercise
 * -- "visualization" of refresh is a progress dialog - minimally useful given how fast loading 5 xml entries is
 *    (had to add delay logic to show it for at least 2.5s - again, expect it'll be more flushed out in HW312)
 * -- I added a menu item to "Shake" for easier testing in emulator
 * -- use pref key to track whether or not we've loaded the data in the DB at all (do it automatically when first run)
 */