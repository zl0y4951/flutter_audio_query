//Copyright (C) <2019>  <Marcos Antonio Boaventura Feitoza> <scavenger.gnu@gmail.com>
//
//        This program is free software: you can redistribute it and/or modify
//        it under the terms of the GNU General Public License as published by
//        the Free Software Foundation, either version 3 of the License, or
//        (at your option) any later version.
//
//        This program is distributed in the hope that it will be useful,
//        but WITHOUT ANY WARRANTY; without even the implied warranty of
//        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//        GNU General Public License for more details.
//
//        You should have received a copy of the GNU General Public License
//        along with this program.  If not, see <http://www.gnu.org/licenses/>.

package boaventura.com.devel.br.flutteraudioquery.loaders;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import boaventura.com.devel.br.flutteraudioquery.loaders.tasks.AbstractLoadTask;
import boaventura.com.devel.br.flutteraudioquery.sortingtypes.SongSortType;
import io.flutter.plugin.common.MethodChannel;

public class SongLoader extends AbstractLoader {

    private static final String TAG = "MDBG";
    //private static final String GENRE_NAME = "genre_name";

    private static final int QUERY_TYPE_GENRE_SONGS = 0x01;

    //private static final String MOST_PLAYED = "most_played"; //undocumented column
    //private static final String RECENTLY_PLAYED = "recently_played"; // undocumented column

    private static final String[] SONG_ALBUM_PROJECTION = {
            MediaStore.Audio.AlbumColumns.ALBUM,
            MediaStore.Audio.AlbumColumns.ALBUM_ART
    };

    static private final String[] SONG_PROJECTION = {
            MediaStore.Audio.Media._ID,// row id
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.IS_PODCAST,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_ALARM,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION, // duration of the audio file in ms
            MediaStore.Audio.Media.BOOKMARK, // position, in ms, where playback was at in last stopped
            MediaStore.Audio.Media.DATA, // file data path
            MediaStore.Audio.Media.SIZE, // string with file size in bytes
    };

    public SongLoader(final Context context){

        super(context);

        /*getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                new ContentObserver(null){
                    @Override
                    public boolean deliverSelfNotifications() {
                        return super.deliverSelfNotifications();
                    }

                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                        Log.i("MDBG", "onChange(self) in SongLoaderObserver");
                    }

                    @Override
                    public void onChange(boolean selfChange, Uri uri) {
                        super.onChange(selfChange, uri);
                        Log.i("MDBG", "onChange(self,uri) in SongLoaderObserver Uri: " + uri);
                    }
                }

                );*/
    }

    private String parseSortOrder(SongSortType sortType){
        String sortOrder;

        switch (sortType){

            case ALPHABETIC_COMPOSER:
                sortOrder = MediaStore.Audio.Media.COMPOSER+ " ASC";
                break;

            case GREATER_DURATION:
                sortOrder = MediaStore.Audio.Media.DURATION + " DESC";
                break;

            case SMALLER_DURATION:
                sortOrder = MediaStore.Audio.Media.DURATION + " ASC";
                break;

            case RECENT_YEAR:
                sortOrder = MediaStore.Audio.Media.YEAR + " DESC";
                break;

            case OLDEST_YEAR:
                sortOrder = MediaStore.Audio.Media.YEAR + " ASC";
                break;

            case ALPHABETIC_ARTIST:
                sortOrder = MediaStore.Audio.Media.ARTIST_KEY;
                break;

            case ALPHABETIC_ALBUM:
                sortOrder = MediaStore.Audio.Media.ALBUM_KEY;
                break;

            case SMALLER_TRACK_NUMBER:
                sortOrder = MediaStore.Audio.Media.TRACK + " ASC";
                break;

            case GREATER_TRACK_NUMBER:
                sortOrder = MediaStore.Audio.Media.TRACK + " DESC";
                break;

            case DISPLAY_NAME:
                sortOrder = MediaStore.Audio.Media.DISPLAY_NAME;
                break;
            case DEFAULT:
            default:
                sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
                break;
        }
        return sortOrder;
    }

    public void getSongs(final MethodChannel.Result result, final SongSortType sortType){

        createLoadTask( result,null,null,
                parseSortOrder(sortType), QUERY_TYPE_DEFAULT).execute();
    }

    public void getSongsFromPlaylist(MethodChannel.Result result, final List<String> songIds){
        String[] values;

        if ( (songIds != null) && (songIds.size() > 0) ){
             values = songIds.toArray(new String[songIds.size()] );
             this.
             createLoadTask(result, SONG_PROJECTION[0] + " =?", values, preparePlaylistSongsSortOrder(songIds), QUERY_TYPE_DEFAULT)
                     .execute();
        }
        else result.success( new ArrayList<Map<String,Object>>() );
    }

    /**
     * This method creates a SQL CASE WHEN THEN in order to get specific songs
     * from Media table where the query results is sorted matching [songIds] list values order.
     *
     * @param songIds Song ids list
     * @return Sql String case when then or null if songIds size is not greater then 1.
     */
    private String preparePlaylistSongsSortOrder(final List<String> songIds){
        if (songIds.size() == 1)
            return null;

        StringBuilder orderStr = new StringBuilder("CASE ")
                .append(MediaStore.MediaColumns._ID)
                .append(" WHEN '")
                .append(songIds.get(0))
                .append("'")
                .append(" THEN 0");

        for(int i = 1; i < songIds.size(); i++){
            orderStr.append(" WHEN '")
                    .append( songIds.get(i) )
                    .append("'")
                    .append(" THEN ")
                    .append(i);
        }

        orderStr.append(" END, ")
                .append(MediaStore.MediaColumns._ID)
                .append(" ASC");
        return orderStr.toString();
    }

    public void getSongsFromAlbum(final MethodChannel.Result result, final String albumId,
                                  final SongSortType sortType){

       createLoadTask( result, MediaStore.Audio.Media.ALBUM_ID + " =?",
                new String[] {albumId},
               parseSortOrder(sortType), QUERY_TYPE_DEFAULT ).execute();
    }

    public void getSongsFromArtist(final MethodChannel.Result result, final String artistName,
                                   final SongSortType sortType ){

        createLoadTask(result, MediaStore.Audio.Media.ARTIST + " =?",
                new String[] { artistName }, parseSortOrder(sortType), QUERY_TYPE_DEFAULT )
                .execute();
    }

    public void getSongsFromGenre(final MethodChannel.Result result, final String genre,
                                  final SongSortType sortType){

        createLoadTask(result, genre, null,
                parseSortOrder( sortType), QUERY_TYPE_GENRE_SONGS )
                .execute();
    }

    @Override
    protected SongTaskLoad createLoadTask(MethodChannel.Result result, final String selection, final String [] selectionArgs,
                                final String sortOrder, final int type){

        return new SongTaskLoad(result, getContentResolver(), selection, selectionArgs, sortOrder, type);

    }


    private static class SongTaskLoad extends AbstractLoadTask< List< Map<String,Object> > > {
        private MethodChannel.Result m_result;
        private ContentResolver m_resolver;
        private int m_queryType;

        /**
         *
         * @param result
         * @param m_resolver
         * @param selection
         * @param selectionArgs
         * @param sortOrder
         */
        SongTaskLoad(MethodChannel.Result result, ContentResolver m_resolver, String selection,
                     String[] selectionArgs, String sortOrder, int type){

            super(selection, selectionArgs, sortOrder);
            this.m_resolver = m_resolver;
            this.m_result =result;
            this.m_queryType = type;
        }

        @Override
        protected void onPostExecute(List<Map<String, Object>> map) {
            super.onPostExecute(map);
            m_result.success(map);
            this.m_resolver = null;
            this.m_result = null;
        }

        @Override
        protected List< Map<String,Object> > loadData(
                final String selection, final String [] selectionArgs,
                final String sortOrder ){

            switch (m_queryType){
                case QUERY_TYPE_DEFAULT:
                    if ( (selectionArgs!=null) && (selectionArgs.length > 1) ){
                        return basicLoad( createMultipleValueSelectionArgs(MediaStore.Audio.Media._ID,
                                selectionArgs), selectionArgs, sortOrder);

                    } else
                        return  basicLoad(selection, selectionArgs, sortOrder);


                case QUERY_TYPE_GENRE_SONGS:
                    List<String> songIds = getSongIdsFromGenre(selection);
                    int idCount = songIds.size();
                    if ( idCount > 0){

                        if (idCount > 1 ){
                            String[] args = songIds.toArray( new String[idCount] );
                            String createdSelection = createMultipleValueSelectionArgs(
                                    MediaStore.Audio.Media._ID, args);
                            return  basicLoad(
                                    createdSelection,
                                    args,sortOrder );
                        }

                        else {
                            return basicLoad(MediaStore.Audio.Media._ID + " =?",
                                    new String[]{ songIds.get(0)},
                                    sortOrder );
                        }
                    }
                    break;

                default:
                    break;
            }

            return new ArrayList<>();
        }

        private List<String> getSongIdsFromGenre(final String genre){
           Cursor songIdsCursor = m_resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[] {"Distinct " + MediaStore.Audio.Media._ID, "genre_name" },
                    "genre_name" + " =?",new String[] {genre},null);

           List<String> songIds = new ArrayList<>();

           if (songIdsCursor != null){

               while ( songIdsCursor.moveToNext() ){
                   try {
                       String id = songIdsCursor.getString(songIdsCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                       songIds.add(id);
                   }
                   catch (Exception ex){
                       Log.e(TAG_ERROR, "SongLoader::getSonIdsFromGenre method exception");
                       Log.e(TAG_ERROR, ex.getMessage() );
                   }
               }
               songIdsCursor.close();
           }

           return songIds;
        }

        private List<Map<String,Object>> basicLoad(final String selection, final String[] selectionArgs,
                                                   final String sortOrder){

            List< Map<String, Object> > dataList = new ArrayList<>();
            Cursor songsCursor = null;

            try{
                songsCursor = m_resolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        SongLoader.SONG_PROJECTION, selection, selectionArgs, sortOrder );

            }

            catch (RuntimeException ex){

                System.out.println("SongLoader::basicLoad " + ex);
                m_result.error("SONG_READ_ERROR", ex.getMessage() , null);
            }

            if (songsCursor != null){
                Map<String,String> albumArtMap = new HashMap<>();

                while( songsCursor.moveToNext() ){
                    try {

                        Map<String, Object> songData = new HashMap<>();
                        for (String column : songsCursor.getColumnNames())
                            songData.put(column, songsCursor.getString( songsCursor.getColumnIndex(column)) );

                        String albumKey = songsCursor.getString(
                                songsCursor.getColumnIndex(SONG_PROJECTION[4]));

                        String artPath;
                        if (!albumArtMap.containsKey(albumKey)) {

                            artPath = getAlbumArtPathForSong(albumKey);
                            albumArtMap.put(albumKey, artPath);

                            //Log.i("MDBG", "song for album  " + albumKey + "adding path: " + artPath);
                        }

                        artPath = albumArtMap.get(albumKey);
                        songData.put("album_artwork", artPath);
                        dataList.add(songData);
                    }

                    catch(Exception ex){
                        Log.e(TAG_ERROR, "SongLoader::basicLoad method exception");
                        Log.e(TAG_ERROR, ex.getMessage() );
                    }
                }

                songsCursor.close();
            }

            return dataList;
        }

        private String getAlbumArtPathForSong(String album){
            Cursor artCursor = m_resolver.query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    SONG_ALBUM_PROJECTION,
                    SONG_ALBUM_PROJECTION[0] +  " =?",
                    new String[] {album},
                    null);

            String artPath = null;

            if (artCursor !=null){
                while (artCursor.moveToNext()) {

                    try {
                        artPath = artCursor.getString(artCursor.getColumnIndex(SONG_ALBUM_PROJECTION[1]));

                    }

                    catch (Exception ex) {
                        Log.e(TAG_ERROR, "SongLoader::getAlbumArtPathForSong method exception");
                        Log.e(TAG_ERROR, ex.getMessage());
                    }
                }

                artCursor.close();
            }

            return artPath;
        }

    }
}


/*
 *  NON DOCUMENTED MEDIA COLUMNS
 *
 * album_artist
 * duration
 * genre_name
 * recently_played
 * most_played
 *
 */

/*
 *          ALL MEDIA COLUMNS
 *
 * I/MDBG    (15056): Column: _id
 * I/MDBG    (15056): Column: _data
 * I/MDBG    (15056): Column: _display_name
 * I/MDBG    (15056): Column: _size
 * I/MDBG    (15056): Column: mime_type
 * I/MDBG    (15056): Column: date_added
 * I/MDBG    (15056): Column: is_drm
 * I/MDBG    (15056): Column: date_modified
 * I/MDBG    (15056): Column: title
 * I/MDBG    (15056): Column: title_key
 * I/MDBG    (15056): Column: duration
 * I/MDBG    (15056): Column: artist_id
 * I/MDBG    (15056): Column: composer
 * I/MDBG    (15056): Column: album_id
 * I/MDBG    (15056): Column: track
 * I/MDBG    (15056): Column: year
 * I/MDBG    (15056): Column: is_ringtone
 * I/MDBG    (15056): Column: is_music
 * I/MDBG    (15056): Column: is_alarm
 * I/MDBG    (15056): Column: is_notification
 * I/MDBG    (15056): Column: is_podcast
 * I/MDBG    (15056): Column: bookmark
 * I/MDBG    (15056): Column: album_artist
 * I/MDBG    (15056): Column: is_sound
 * I/MDBG    (15056): Column: year_name
 * I/MDBG    (15056): Column: genre_name
 * I/MDBG    (15056): Column: recently_played
 * I/MDBG    (15056): Column: most_played
 * I/MDBG    (15056): Column: recently_added_remove_flag
 * I/MDBG    (15056): Column: is_favorite
 * I/MDBG    (15056): Column: bucket_id
 * I/MDBG    (15056): Column: bucket_display_name
 * I/MDBG    (15056): Column: recordingtype
 * I/MDBG    (15056): Column: latitude
 * I/MDBG    (15056): Column: longitude
 * I/MDBG    (15056): Column: addr
 * I/MDBG    (15056): Column: langagecode
 * I/MDBG    (15056): Column: is_secretbox
 * I/MDBG    (15056): Column: is_memo
 * I/MDBG    (15056): Column: label_id
 * I/MDBG    (15056): Column: weather_ID
 * I/MDBG    (15056): Column: sampling_rate
 * I/MDBG    (15056): Column: bit_depth
 * I/MDBG    (15056): Column: recorded_number
 * I/MDBG    (15056): Column: recording_mode
 * I/MDBG    (15056): Column: is_ringtone_theme
 * I/MDBG    (15056): Column: is_notification_theme
 * I/MDBG    (15056): Column: is_alarm_theme
 * I/MDBG    (15056): Column: datetaken
 * I/MDBG    (15056): Column: artist_id:1
 * I/MDBG    (15056): Column: artist_key
 * I/MDBG    (15056): Column: artist
 * I/MDBG    (15056): Column: album_id:1
 * I/MDBG    (15056): Column: album_key
 * I/MDBG    (15056): Column: album
 *
 *
 */
