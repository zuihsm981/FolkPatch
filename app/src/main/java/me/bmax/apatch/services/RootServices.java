package me.bmax.apatch.services;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.ipc.RootService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import me.bmax.apatch.IAPRootService;
import rikka.parcelablelist.ParcelableListSlice;

public class RootServices extends RootService {
    private static final String TAG = "RootServices";

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return new Stub();
    }

    int[] getUserIdsInternal() {
        List<Integer> result = new ArrayList<>();
        try {
            UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
            if (um != null) {
                // getUserProfiles() only returns the CURRENT user's profile group,
                // so it never lists other users (user 10/11/...). Use the hidden
                // UserManager.getUsers() to enumerate all users; RootService runs
                // with root so hidden API + MANAGE_USERS checks pass.
                try {
                    Method getUsers = um.getClass().getMethod("getUsers");
                    getUsers.setAccessible(true);
                    Object users = getUsers.invoke(um);
                    if (users instanceof List) {
                        for (Object u : (List<?>) users) {
                            try {
                                Method getIdentifier = u.getClass().getMethod("getIdentifier");
                                getIdentifier.setAccessible(true);
                                Object id = getIdentifier.invoke(u);
                                if (id instanceof Integer && !result.contains(id)) {
                                    result.add((Integer) id);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "UserManager.getUsers failed, fallback to profiles", e);
                }

                // Fallback: current user's profiles (only user 0 / work profile)
                if (result.isEmpty()) {
                    List<UserHandle> userProfiles = um.getUserProfiles();
                    if (userProfiles != null) {
                        for (UserHandle userProfile : userProfiles) {
                            if (!result.contains(userProfile.hashCode())) {
                                result.add(userProfile.hashCode());
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "getUserIds failed", e);
            // Fallback to current user if UserManager fails
            result.add(0);
        }
        if (result.isEmpty()) {
            result.add(0);
        }
        int[] out = new int[result.size()];
        for (int i = 0; i < result.size(); i++) {
            out[i] = result.get(i);
        }
        return out;
    }

    List<PackageInfo> getInstalledPackagesAsUser(int flags, int userId) {
        try {
            PackageManager pm = getPackageManager();
            Method getInstalledPackagesAsUser = pm.getClass().getDeclaredMethod("getInstalledPackagesAsUser", int.class, int.class);
            return (List<PackageInfo>) getInstalledPackagesAsUser.invoke(pm, flags, userId);
        } catch (Throwable e) {
            Log.e(TAG, "err", e);
        }

        return new ArrayList<>();
    }

    class Stub extends IAPRootService.Stub {
        @Override
        public ParcelableListSlice<PackageInfo> getPackagesForUser(int userId, int flags) {
            try {
                List<PackageInfo> list = getInstalledPackagesAsUser(flags, userId);
                Log.i(TAG, "getPackagesForUser: " + userId + " -> " + list.size());
                return new ParcelableListSlice<>(list);
            } catch (Throwable e) {
                Log.e(TAG, "getPackagesForUser failed", e);
                return new ParcelableListSlice<>(new ArrayList<>());
            }
        }

        @Override
        public int[] getUserIds() {
            return getUserIdsInternal();
        }

    }
}