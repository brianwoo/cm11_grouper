/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.providers.downloads;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.android.providers.downloads.OpenHelper;

import org.cyanogenmod.support.ui.LiveFolder;

import java.util.ArrayList;
import java.util.List;

public class LiveFolderReceiver extends BroadcastReceiver {

    private static int sNumLiveFolders = 0;

    private static Bitmap retrieveAndSetIcon(Context context, String mediaType) {
        if (mediaType == null) {
            return null;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromParts("file", "", null), mediaType);

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        if (list.isEmpty()) {
            return BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_download_misc_file_type);
        } else {
            Drawable d = list.get(0).activityInfo.loadIcon(pm);
            return ((BitmapDrawable) d).getBitmap();
        }
    }

    public static void updateFolders(Context context, long folderId) {
        if (sNumLiveFolders == 0) {
            return;
        }

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        dm.setAccessAllDownloads(true);

        DownloadManager.Query baseQuery =
                new DownloadManager.Query().setOnlyIncludeVisibleInDownloadsUi(true);
        Cursor cursor = dm.query(baseQuery);
        if (cursor == null) {
            return;
        }
        ArrayList<LiveFolder.Item> folderItems = new ArrayList<LiveFolder.Item>();

        while (cursor.moveToNext() && folderItems.size() < LiveFolder.Constants.MAX_ITEMS) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            String title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
            String mediaType =
                    cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));

            LiveFolder.Item folderItem = new LiveFolder.Item();
            folderItem.setLabel(title);
            folderItem.setIcon(retrieveAndSetIcon(context, mediaType));
            folderItem.setId((int) id);
            folderItems.add(folderItem);
        }

        if (folderItems.isEmpty()) {
            return;
        }

        if (folderId == 0) {
            LiveFolder.updateAllFolders(context, folderItems);
        } else {
            LiveFolder.updateSingleFolder(context, folderId, folderItems);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra(LiveFolder.Constants.FOLDER_UPDATE_TYPE_EXTRA);

        if (TextUtils.isEmpty(type)) {
            return;
        }

        long folderId = intent.getLongExtra(LiveFolder.Constants.FOLDER_ID_EXTRA, 0);
        if (folderId <= 0 && !type.equals(LiveFolder.Constants.EXISTING_FOLDERS_CREATED)) {
            return;
        }

        if (type.equals(LiveFolder.Constants.NEW_FOLDER_CREATED)) {
            sNumLiveFolders++;
            updateFolders(context, folderId);
        } else if (type.equals(LiveFolder.Constants.EXISTING_FOLDERS_CREATED)) {
            long[] existingFolders = intent.getLongArrayExtra(
                    LiveFolder.Constants.EXISTING_FOLDER_IDS_EXTRA);
            sNumLiveFolders = existingFolders.length;
            updateFolders(context, 0);
        } else if (type.equals(LiveFolder.Constants.FOLDER_ITEM_SELECTED)) {
            int itemId = intent.getIntExtra(LiveFolder.Constants.FOLDER_ITEM_ID_EXTRA, 0);
            OpenHelper.startViewIntent(context, itemId, Intent.FLAG_ACTIVITY_NEW_TASK);
        } else if (type.equals(LiveFolder.Constants.FOLDER_ITEM_REMOVED)) {
            // Get selected item id
            int itemId = intent.getIntExtra(LiveFolder.Constants.FOLDER_ITEM_ID_EXTRA, 0);
            DownloadManager dm =
                    (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

            dm.setAccessAllDownloads(true);
            dm.markRowDeleted(itemId);
        } else if (type.equals(LiveFolder.Constants.FOLDER_DELETED)) {
            sNumLiveFolders--;
        }
    }

}
