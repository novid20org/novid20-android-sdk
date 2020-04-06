/*
 *  Created by Christoph Kührer & Florian Knoll on 06.04.20 19:27
 *  Copyright (c) 2020 . All rights reserved.
 *  Last modified 06.04.20 18:35
 */

package org.novid20.sdk

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.location.DetectedActivity

private const val PREF_NAME = "novid20"
private const val PREF_UID = "uid"
private const val PREF_USERID = "userid"
private const val PREF_TOKEN = "token"
private const val PREF_AUTHTOKEN = "authToken"
private const val PREF_REGISTERED = "registered"
private const val PREF_ONBOARDED = "onboarded"
private const val PREF_INFECTED = "infected"
private const val PREF_INFECTED_CONFIRMED = "infected_confirm"
private const val PREF_ENABLED = "enabled"
private const val PREF_NOTIFICATIONS = "enabled"
private const val PREF_LAST_SUCCESSFUL_ANALYTICS_SYNC = "last_successful_analytics_sync"

private val TAG: String = Logger.makeLogTag("NovidConfig")

class NovidConfig(context: Context) {

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    init {
        sharedPref.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            val pref = sharedPreferences.all[key]
            Logger.debug(TAG, "Property changed '$key': $pref")
        }
    }

    internal lateinit var accessToken: String

    var notification: Boolean
        get() = sharedPref.getBoolean(PREF_NOTIFICATIONS, false)
        set(value) {
            sharedPref.edit().putBoolean(PREF_NOTIFICATIONS, value).apply()
        }

    var enabled: Boolean
        get() = sharedPref.getBoolean(PREF_ENABLED, false)
        set(value) {
            sharedPref.edit().putBoolean(PREF_ENABLED, value).apply()
        }

    var uid: String?
        get() = sharedPref.getString(PREF_UID, null)
        internal set(value) {
            sharedPref.edit().putString(PREF_UID, value).apply()
        }

    // Used for broadcasting
    var userId: String?
        get() = sharedPref.getString(PREF_USERID, null)
        internal set(value) {
            sharedPref.edit().putString(PREF_USERID, value).apply()
        }

    var deviceToken: String?
        get() = sharedPref.getString(PREF_TOKEN, null)
        internal set(value) {
            sharedPref.edit().putString(PREF_TOKEN, value).apply()
        }

    var authToken: String?
        get() = sharedPref.getString(PREF_AUTHTOKEN, null)
        set(value) {
            sharedPref.edit().putString(PREF_AUTHTOKEN, value).apply()
        }

    var registered: Boolean
        get() = sharedPref.getBoolean(PREF_REGISTERED, false)
        set(value) {
            sharedPref.edit().putBoolean(PREF_REGISTERED, value).apply()
        }

    var onboarded: Boolean
        get() = sharedPref.getBoolean(PREF_ONBOARDED, false)
        set(value) {
            sharedPref.edit().putBoolean(PREF_ONBOARDED, value).apply()
        }

    /**
     * This value is true when the user has submitted an infection report
     * This does not mean that the infection has already been confirmed
     * by the government. If the government has confirmed it, then
     * [infectionConfirmed] will be true.
     */
    var infectionReported: Boolean
        get() = sharedPref.getBoolean(PREF_INFECTED, false)
        set(value) {
            sharedPref.edit().putBoolean(PREF_INFECTED, value).apply()
        }

    var infectionConfirmed: Boolean
        get() = sharedPref.getBoolean(PREF_INFECTED_CONFIRMED, false)
        set(value) {
            sharedPref.edit().putBoolean(PREF_INFECTED_CONFIRMED, value).apply()
        }

    var lastSuccessfulSync: Long
        get() = sharedPref.getLong(PREF_LAST_SUCCESSFUL_ANALYTICS_SYNC, 0)
        set(value) {
            sharedPref.edit().putLong(PREF_LAST_SUCCESSFUL_ANALYTICS_SYNC, value).apply()
        }
}