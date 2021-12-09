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
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.*
import org.jetbrains.annotations.NotNull

class SettingsActivity : Activity() {

	private val scope = MainScope()

	private inner class ApplicationPkgInfo(
		val pkgInfo: PackageSettings.Package,
		val name: String,
		var icon: Drawable? = null)
	{
	}

	private var handledApplications = ArrayList<ApplicationPkgInfo>()

	private lateinit var switchEnableSamsungTTS: SwitchMaterial
	private lateinit var switchEnableMediaStream: SwitchMaterial

	private lateinit var listHandledApplications: ListView
	private lateinit var textViewlonelyHere: TextView
	private lateinit var textViewListSmallPrint: TextView

	private lateinit var listAdapter: ListApplicationsAdapter

	private lateinit var pkgSettings: PackageSettings
	private lateinit var settings: PersistentState

	//private var listApplicationsLoader: LoadPackagesTask? = null

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		Log.d(TAG, "main activity created")

		Log.d(TAG, "onCreateView")

		settings = PersistentState(this)
		pkgSettings = PackageSettings(this)

		setContentView(R.layout.activity_settings)

		switchEnableSamsungTTS = findViewById(R.id.switchEnableSamsungTTS)
		switchEnableMediaStream = findViewById(R.id.switchUseMediaStream)

		switchEnableSamsungTTS.isChecked = settings.enableSamsungTTS
		switchEnableMediaStream.isChecked = settings.enableMediaStream

		switchEnableSamsungTTS.setOnClickListener {
			settings.enableSamsungTTS = switchEnableSamsungTTS.isChecked
		}
		switchEnableMediaStream.setOnClickListener {
			settings.enableMediaStream = switchEnableMediaStream.isChecked
		}

		findViewById<Button>(R.id.buttonSelectApps).setOnClickListener {
			val intent = Intent(this, EditApplicationsActivity::class.java)
			startActivity(intent)
		}

		findViewById<Button>(R.id.buttonSelectDevices).setOnClickListener {
			val intent = Intent(this, BluetoothDevicesActivity::class.java)
			startActivity(intent)
		}

		listHandledApplications = findViewById(R.id.listApplications) as ListView
		textViewlonelyHere = findViewById(R.id.textViewLonelyHere) as TextView
		textViewListSmallPrint = findViewById(R.id.textViewLblEnablePerAppSmallprint) as TextView

		listAdapter = ListApplicationsAdapter(this, ArrayList<ApplicationPkgInfo>())
		listHandledApplications.onItemClickListener =
			AdapterView.OnItemClickListener {
					parent, view, position, id ->
					(listHandledApplications.adapter as ListApplicationsAdapter).onItemClicked(position)
			}
		listHandledApplications.adapter = listAdapter
	}

	public override fun onPause() {
		scope.cancel()
		super.onPause()
	}

	private fun loadData(): ArrayList<ApplicationPkgInfo> {
		val pkgSettings = PackageSettings(this)
		val allPackages = pkgSettings.allPackages

		val packageManager = packageManager

		val applications = ArrayList<ApplicationPkgInfo>()

		for (pkg in allPackages) {
			try {
				val pmAppInfo = packageManager.getApplicationInfo(pkg.packageName ?: "", PackageManager.GET_META_DATA)
				val icon = pmAppInfo.loadIcon(packageManager)
				val name = packageManager.getApplicationLabel(pmAppInfo).toString()
				applications.add(ApplicationPkgInfo(pkg, name, icon))
			} catch (e: NameNotFoundException) {
				e.printStackTrace()
			}
		}

		applications.sortBy { it.name }

		return applications
	}

	public override fun onResume() {
		super.onResume()

		scope.launch(Dispatchers.IO) {

			val apps = loadData()

			withContext(Dispatchers.Main) {
				handledApplications = apps

				listAdapter.listApplications = apps

				if (apps.isEmpty()) {
					textViewlonelyHere.visibility = View.VISIBLE
					listHandledApplications.visibility = View.GONE
					textViewListSmallPrint.visibility = View.GONE
				} else {
					listHandledApplications.visibility = View.VISIBLE
					textViewlonelyHere.visibility = View.VISIBLE
					textViewlonelyHere.visibility = View.GONE

					listHandledApplications.setSelection(0)
				}

				listAdapter.notifyDataSetChanged()
			}
		}
	}

	private inner class ListApplicationsAdapter(
			private val context: Context,
			var listApplications: ArrayList<ApplicationPkgInfo>
		) : BaseAdapter()
	{

		fun onItemClicked(position: Int) {
			Log.d(TAG, "ListApplicationsAdapter::onItemClicked, pos=" + position)

			val appInfo = listApplications[position]

//			val alert = AlertDialog.Builder(this@SettingsActivity)
//
//			alert.setTitle("Remind interval")
//
//			val inflater = this@SettingsActivity.layoutInflater
//
//			val dialogView = inflater.inflate(R.layout.dlg_remind_interval, null)
//
//			alert.setView(dialogView)
//
//			val picker = dialogView.findViewById(R.id.numberPickerRemindInterval) as NumberPicker
//
//			picker.minValue = 1
//			picker.maxValue = 120
//			picker.value = appInfo.pkgInfo!!.remindIntervalSeconds / 60
//
//			alert.setPositiveButton(android.R.string.ok) {
//				x,y ->
//				val interval = picker.value
//
//				Log.d(TAG, "got val: " + interval)//value.toString());
//
//				try
//				{
//					appInfo.pkgInfo!!.remindIntervalSeconds = interval * 60
//					pkgSettings!!.updatePackage(appInfo.pkgInfo!!)
//
//					Log.d(TAG, "remind interval updated to " + interval + " for package " + appInfo.pkgInfo)
//				}
//				catch (ex: Exception)
//				{
//					ex.printStackTrace()
//				}
//
//				notifyDataSetChanged()
//			}
//
//			alert.setNegativeButton(android.R.string.cancel) { x,y ->  }
//
//			alert.show()
		}

		override fun getCount(): Int {
			return listApplications.size
		}

		override fun getItem(position: Int): Any {
			return listApplications[position]
		}

		override fun getItemId(position: Int): Long {
			return position.toLong()
		}

		override fun getViewTypeCount(): Int {
			return 2
		}

		override fun getItemViewType(position: Int): Int {
			return 0
		}

		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			var rowView: View? = convertView

			var viewHolder: ViewHolder? = if (rowView != null) rowView.tag as ViewHolder else null

			if (viewHolder == null)
			{
				val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

				rowView = inflater.inflate(R.layout.list_item, parent, false)

				viewHolder = ViewHolder()

				viewHolder.textViewRemindInterval = rowView!!.findViewById(R.id.textViewIntervalLabel) as TextView
				viewHolder.textViewAppName = rowView.findViewById(R.id.textViewAppName) as TextView
				viewHolder.imageViewAppIcon = rowView.findViewById(R.id.icon) as ImageView
				viewHolder.btnEnableForApp = rowView.findViewById(R.id.toggleButtonEnableForApp) as ToggleButton

				rowView.tag = viewHolder
			}

			val appInfo = listApplications[position] // this would not change as well - why lookup twice then?

			viewHolder.btnEnableForApp?.isChecked = appInfo.pkgInfo.isHandlingThis

//			val text = getString(R.string.every_nmin_fmt).format((appInfo.pkgInfo.remindIntervalSeconds / 60))
			viewHolder.textViewRemindInterval?.setText("_temp_")

			if (appInfo.name.isNotBlank())
				viewHolder.textViewAppName?.text = appInfo.name
			else
				viewHolder.textViewAppName?.text = appInfo.pkgInfo.packageName

			if (appInfo.icon != null)
				viewHolder.imageViewAppIcon!!.setImageDrawable(appInfo.icon)

			viewHolder.btnEnableForApp?.isEnabled = true
			viewHolder.textViewRemindInterval?.isEnabled = true
			viewHolder.imageViewAppIcon?.isEnabled = true
			viewHolder.textViewAppName?.isEnabled = true

			viewHolder.btnEnableForApp!!.setOnClickListener {
				btn ->
				Log.d(TAG, "saveSettingsOnClickListener.onClick()")

				appInfo.pkgInfo.isHandlingThis = (btn as ToggleButton).isChecked
				pkgSettings.updatePackage(appInfo.pkgInfo)
				notifyDataSetChanged()
			}

			return rowView!!
		}

		inner class ViewHolder
		{
			internal var btnEnableForApp: ToggleButton? = null
			internal var textViewRemindInterval: TextView? = null
			internal var textViewAppName: TextView? = null
			internal var imageViewAppIcon: ImageView? = null
		}
	}

	companion object {
		private val TAG = "MainActivity"
	}
}
