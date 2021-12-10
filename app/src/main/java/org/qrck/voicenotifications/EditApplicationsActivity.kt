/*
 * Copyright (c) 2014, Sergey Parshin, quarck@gmail.com
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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.coroutines.*
import java.util.*

data class AppSelectionInfo(
	var loadComplete: Boolean = false,
	var name: String? = null,
	var packageName: String = "",
	var icon: Drawable? = null,
	var app: ApplicationInfo? = null
)

class ApplicationsData {

	var handledApps: ArrayList<AppSelectionInfo> = arrayListOf()
	var visibleApps: ArrayList<AppSelectionInfo> = arrayListOf()

	val all: ArrayList<ArrayList<AppSelectionInfo>> = ArrayList<ArrayList<AppSelectionInfo>>()

	init {
		all.add(handledApps)
		all.add(visibleApps)
	}

	var handled: ArrayList<AppSelectionInfo>
		get() = handledApps
		set(value){
			handledApps = value
			all[0] = handledApps
		}
	var visible: ArrayList<AppSelectionInfo>
		get() = visibleApps
		set(value){
			visibleApps = value
			all[1] = visibleApps
		}

	val flat: List<AppSelectionInfo>
		get() = all.flatten()
}


class ListApplicationsAdapter(
	val context: Context,
	val textViewResourceId: Int,
	var applications: ApplicationsData,
	var pkgSettings: PackageSettings
) : BaseAdapter()
{
	private var packageManager = context.getPackageManager()

	init {
		Log.d(TAG, "Adding list of applications:")
		for (asi in applications.flat) {
			Log.d(TAG, " ... " + asi.packageName)
		}
	}

	override fun getViewTypeCount(): Int {
		return 2
	}

	override fun getCount(): Int {
		var size = 0

		for (list in applications.all) {
			if (list.size > 0) {
				size += 1 + list.size
			}
		}
		return size
	}

	override fun getItemViewType(position: Int): Int {

		var pos = position

		for (list in applications.all) {
			if (list.size > 0) {
				if (pos == 0)
					return 1
				pos--

				if (pos < list.size)
					return 0

				pos -= list.size
			}
		}

		return 0
	}


	private var menuTitles: Array<String>? = null
		get() {
			synchronized (this) {
				if (field == null) {
					field = arrayOf<String>(
						"Handled Apps",
						"Other Apps"
					)
				}
			}

			return field
		}


	override fun getItem(position: Int): Any? {
		var pos = position
		val titles = menuTitles

		var titleIdx = 0
		for (list in applications.all) {
			if (list.size > 0) {
				if (pos == 0)
					return titles?.get(titleIdx)
				pos--

				if (pos < list.size)
					return list[pos]

				pos -= list.size
			}
			titleIdx++
		}

		return null
	}

	override fun getItemId(position: Int): Long {
		return position.toLong()
	}

	fun getItemView(position: Int, convertView: View?, parent: ViewGroup): View? {

		var rowView: View? = convertView

		if (rowView == null) {
			val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

			rowView = inflater.inflate(R.layout.edit_list_item, parent, false)

			val viewHolder = AppListViewHolder()

			viewHolder.btnShowHide = rowView?.findViewById(R.id.checkBoxShowApp) as CheckBox?
			viewHolder.textViewAppName = rowView.findViewById(R.id.textViewAppName) as TextView
			viewHolder.imageViewAppIcon = rowView.findViewById(R.id.editIcon) as ImageView

			rowView.tag = viewHolder
		}

		val viewHolder = rowView?.tag as AppListViewHolder?

		val appInfo = getItem(position) as AppSelectionInfo?

		appInfo?.let {
			if (!it.loadComplete) {
				synchronized (it) {
					if (!it.loadComplete) {
						it.icon = it.app?.loadIcon(packageManager)
						it.loadComplete = true
					}
				}
			}
		}


		viewHolder?.btnShowHide?.isChecked = pkgSettings.getIsListed(appInfo?.packageName ?: "") 

		if (appInfo?.name != null)
			viewHolder?.textViewAppName?.text = appInfo.name
		else
			viewHolder?.textViewAppName?.text = appInfo?.packageName

		if (appInfo?.icon != null) {
			try {
				viewHolder?.imageViewAppIcon?.setImageDrawable(appInfo.icon)
			} catch (ex: Exception) {
				ex.printStackTrace()
			}
		}

		viewHolder?.btnShowHide?.setOnClickListener() {
				btn ->

			Log.d(TAG, "saveSettingsOnClickListener.onClick()")

			if ((btn as CheckBox).isChecked) {
				pkgSettings.lookupEverywhereAndMoveOrInsertNew(appInfo?.packageName ?: "", true, false)
			} else {
				// must hide
				val pkg = pkgSettings.getPackage(appInfo?.packageName ?: "")
				pkg?.let{ pkgSettings.hidePackage(it) }
			}
		}

		return rowView
	}

	fun getTitleView(position: Int, convertView: View?, parent: ViewGroup): View? {
		var rowView: View? = convertView

		if (rowView == null) {
			val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
			rowView = inflater.inflate(R.layout.edit_list_item_title, parent, false)
		}

		val text = rowView?.findViewById(R.id.textViewGroupTitle) as TextView?

		val title = getItem(position) as String?
		text?.text = title

		return rowView
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
		if (getItemViewType(position) == 0) {
			return getItemView(position, convertView, parent)
		} else {
			return getTitleView(position, convertView, parent)
		}
	}

	companion object  {
		private val TAG = "ListApplicationsAdapter"
	}
}

class AppListViewHolder {
	internal var textViewAppName: TextView? = null
	internal var imageViewAppIcon: ImageView? = null

	internal var btnShowHide: CheckBox? = null
}

class EditApplicationsActivity : Activity() {

	private val scope = MainScope()

	private var applications = ApplicationsData()
	private lateinit var listApplications: ListView

	//private ArrayList<AppSelectionInfo> listApps = new ArrayList<AppSelectionInfo>();

	//private var menuTitles: Array<String>? = null

	private lateinit var pkgSettings: PackageSettings

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		Log.d(TAG, "onCreate")

		pkgSettings = PackageSettings(this)

		setContentView(R.layout.activity_edit_apps)

		listApplications = findViewById(R.id.listAddApplications)
		listApplications.emptyView = findViewById(R.id.progressBarLoading)

		listApplications.adapter = ListApplicationsAdapter(
			this@EditApplicationsActivity, R.layout.edit_list_item, applications, pkgSettings)
		listApplications.setSelection(0)

		actionBar?.let {
			it.setHomeButtonEnabled(true)
			it.title = "Show/Hide applications"
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			// app icon in action bar clicked; goto parent activity.
			this.finish()
			return true
		}

		return super.onOptionsItemSelected(item)
	}

	@SuppressLint("QueryPermissionsNeeded")
	private fun loadApplications() {

		val packageManager = packageManager

		val handledApps = ArrayList<AppSelectionInfo>()
		val visibleApps = ArrayList<AppSelectionInfo>()

		val alreadyLoadedAppsHash = HashMap<String, Int>()

		Log.d(TAG, "Loading applications")

		for (pkg in pkgSettings.allPackages) {

			val packageName = pkg.packageName
			if (packageName == null || alreadyLoadedAppsHash.containsKey(pkg.packageName)) {
				continue
			}

			try {
				val asi = AppSelectionInfo()

				asi.packageName = packageName
				asi.app = packageManager.getApplicationInfo(packageName, 0/*PackageManager.GET_META_DATA*/)
				val app = asi.app
				if (app != null)
					asi.name = packageManager.getApplicationLabel(app).toString()
				handledApps.add(asi)
				alreadyLoadedAppsHash[packageName] = 1

			} catch (e: NameNotFoundException) {
				e.printStackTrace()
			}

		}

		Log.d(TAG, "Loading all other applications")

		val installedApps = packageManager.getInstalledApplications(0)
		for (app in installedApps) {
			val asi = AppSelectionInfo()

			if (app.packageName == null)
				continue

			if (alreadyLoadedAppsHash.containsKey(app.packageName))
				continue // already loaded by somebody else

			asi.packageName = app.packageName

			alreadyLoadedAppsHash.put(asi.packageName, 1)

			asi.name = packageManager.getApplicationLabel(app).toString()
			asi.app = app

			visibleApps.add(asi)
		}

		handledApps.sortBy { it.name }
		visibleApps.sortBy { it.name }

		applications.handled = handledApps
		applications.visible = visibleApps
	}

	public override fun onPause() {
		scope.cancel()
		super.onPause()
	}

	public override fun onResume() {
		super.onResume()

		scope.launch(Dispatchers.IO) {
			loadApplications()

			withContext(Dispatchers.Main) {
				(listApplications.adapter as ListApplicationsAdapter).notifyDataSetChanged()
				listApplications.setSelection(0)
			}

			for (appInfo in applications.flat) {
				if (!appInfo.loadComplete) {
					//appInfo.name = packageManager.getApplicationLabel(appInfo.app).toString();
					appInfo.icon = appInfo.app?.loadIcon(packageManager)
					appInfo.loadComplete = true
				}
			}
		}
	}


	companion object  {
		private val TAG = "EditApplicationsActivity"
	}
}
