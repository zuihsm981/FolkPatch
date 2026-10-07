// IAPRootService.aidl
package me.bmax.apatch;

import android.content.pm.PackageInfo;
import rikka.parcelablelist.ParcelableListSlice;

interface IAPRootService {
    ParcelableListSlice<PackageInfo> getPackagesForUser(int userId, int flags);
    int[] getUserIds();
}