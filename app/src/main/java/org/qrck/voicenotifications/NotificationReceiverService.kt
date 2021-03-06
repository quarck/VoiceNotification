/*
 * Copyright (c) 2015, Sergey Parshin, s.parshin@outlook.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of developer (Sergey Parshin) nor the
 *       names of other project contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.qrck.voicenotifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.lang.Exception
import java.util.*

class NotificationReceiverService : NotificationListenerService()
{
	private var prevPkg: String = ""
	private var prevTitle: String = ""
	private var prevText: String = ""
	private var prevTime: Long = 0

	private lateinit var btManager: BTDeviceManager

	private lateinit var pkgSettings: PackageSettings

	private var blacklistedPackages = setOf(
		BuildConfig.APPLICATION_ID,
		"com.android.systemui"
	)

	override fun onCreate() {
		super.onCreate()
		btManager = BTDeviceManager(this)
		pkgSettings = PackageSettings(this)
	}

	override fun onDestroy() {
		super.onDestroy()
	}

	override fun onBind(intent: Intent): IBinder? {
		return super.onBind(intent)
	}

	override fun onNotificationPosted(notification: StatusBarNotification?, rankingMap: NotificationListenerService.RankingMap?) {

		if (notification == null)
			return

		if (rankingMap != null) {
			var ranking = NotificationListenerService.Ranking()
			if (rankingMap.getRanking(notification.key, ranking)) {
				if (ranking.importance <= NotificationManager.IMPORTANCE_LOW) {
					Log.d(LOG_TAG, "Skipping low importance notification for $packageName")
					return
				}
			}
		}

		val packageName = notification.packageName
		if (packageName in blacklistedPackages) {
			return
		}

		val pkg = pkgSettings.getPackage(packageName)
		if (pkg == null || !pkg.isHandlingThis) {
			return
		}

		if (!btManager.anyTriggerDevicesConnected) {
			Log.d(LOG_TAG, "No trigger devices connected, skipping notification from $packageName")
			return
		}

		var appName = ""
		try {
			val appInfo = packageManager.getPackageInfo(packageName, 0).applicationInfo
			if (appInfo != null)
				appName = packageManager.getApplicationLabel(appInfo) as String
		}
		catch (ex: Exception) {
			Log.d(LOG_TAG, ex.toString())
		}
		appName = if (appName.isNotEmpty()) appName else packageName

		var title = notification.notification.title
		if (title.isEmpty())
			title = notification.notification.bigTitle

		if (title.length > 50)
			title = title.substring(0, 47) + "..."

		var text = notification.notification.text
		if (text.isEmpty())
			text = notification.notification.bigText
		if (text.length > 120)
			text = text.substring(0, 117) + "..."

		val now = System.currentTimeMillis()
		if (packageName != prevPkg || prevTitle != title || prevText != text || (now - prevTime > 5000)) {
			prevPkg = packageName
			prevTitle = title
			prevText = text
			prevTime = now

			Log.d(LOG_TAG, "TTS-ingNotification from $packageName, $appName, key ${notification.key}, id ${notification.id}")

			val intent = Intent(this, PlayTTSService::class.java)
			intent.putExtra(PlayTTSService.INTENT_PKG, packageName)
			intent.putExtra(PlayTTSService.INTENT_KEY, notification.key)
			intent.putExtra(PlayTTSService.INTENT_ID, notification.id)

			intent.putExtra(PlayTTSService.INTENT_APP, appName)

			if (!pkg.onlyAnnounceAppName) {
				intent.putExtra(PlayTTSService.INTENT_TITLE, title)
				intent.putExtra(PlayTTSService.INTENT_TEXT, text)
			} else {
				intent.putExtra(PlayTTSService.INTENT_TITLE, "")
				intent.putExtra(PlayTTSService.INTENT_TEXT, "")
			}

			applicationContext.startForegroundService(intent)
		}
	}

	override fun onNotificationRemoved(
		notification: StatusBarNotification?,
		rankingMap: RankingMap?,
		reason: Int
	) {
		var interested = false

		when (reason) {
			NotificationListenerService.REASON_CLICK,
			NotificationListenerService.REASON_CANCEL,
			NotificationListenerService.REASON_CANCEL_ALL,
			NotificationListenerService.REASON_CHANNEL_BANNED,
			NotificationListenerService.REASON_SNOOZED, ->
				interested = true
		}

		if (!interested)
			return

		if (notification == null)
			return

		if (rankingMap != null) {
			var ranking = NotificationListenerService.Ranking()
			if (rankingMap.getRanking(notification.key, ranking)) {
				if (ranking.importance <= NotificationManager.IMPORTANCE_LOW) {
					Log.d(LOG_TAG, "Skipping low importance notification for $packageName")
					return
				}
			}
		}

		val packageName = notification.packageName
		if (packageName in blacklistedPackages) {
			return
		}

		val pkg = pkgSettings.getPackage(packageName)
		if (pkg == null || !pkg.isHandlingThis) {
			return
		}

		// user did something with the notifications and it is one of ours we are interested in -- stop TTS, user can read now
		PlayTTSService.stopActiveTTS(this)
	}

	companion object {
		val LOG_TAG = "NOTIFICATIONS"
	}
}
