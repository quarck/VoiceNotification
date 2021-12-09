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

import java.util.LinkedList

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class PackageSettings(context: Context) : SQLiteOpenHelper(context, PackageSettings.DATABASE_NAME, null, PackageSettings.DATABASE_VERSION)
{

	inner class Package
	{
		// Java boilerplate
		var packageName: String? = null

		// Java boilerplate
		var isHandlingThis: Boolean = false

		// Java boilerplate
		var remindIntervalSeconds: Int = 0

		constructor()
		{
		}

		constructor(_packageName: String, _handleThis: Boolean, _interval: Int) : super()
		{
			var _interval = _interval
			if (_interval == 0)
				_interval = DEFAULT_REMIND_INTERVAL
			packageName = _packageName
			isHandlingThis = _handleThis
			remindIntervalSeconds = _interval
		}

		override fun toString(): String
		{
			return "Package [package=$packageName, handle=$isHandlingThis, interval=$remindIntervalSeconds]"
		}
	}

	private val COLUMNS = arrayOf<String>(KEY_PACKAGE, KEY_HANDLE, KEY_INTERVAL)

	override fun onCreate(db: SQLiteDatabase)
	{
		var CREATE_PKG_TABLE = "CREATE TABLE $TABLE_NAME ( $KEY_PACKAGE TEXT PRIMARY KEY, $KEY_HANDLE INTEGER, $KEY_INTERVAL INTEGER )"

		Log.d(LOG_TAG, "Creating DB TABLE using query: " + CREATE_PKG_TABLE)

		db.execSQL(CREATE_PKG_TABLE)

		CREATE_PKG_TABLE = "CREATE TABLE $TABLE_DISABLED_NAME ( $KEY_PACKAGE TEXT PRIMARY KEY, $KEY_HANDLE INTEGER, $KEY_INTERVAL INTEGER )"

		Log.d(LOG_TAG, "Creating DB TABLE_DISABLED using query: " + CREATE_PKG_TABLE)

		db.execSQL(CREATE_PKG_TABLE)

		val CREATE_INDEX = "CREATE UNIQUE INDEX $INDEX_NAME ON $TABLE_NAME ($KEY_PACKAGE)"

		Log.d(LOG_TAG, "Creating DB INDEX using query: " + CREATE_INDEX)

		db.execSQL(CREATE_INDEX)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
	{
		Log.d(LOG_TAG, "DROPPING table and index")
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_DISABLED_NAME)
		db.execSQL("DROP INDEX IF EXISTS " + INDEX_NAME)
		this.onCreate(db)
	}

	fun addPackage(tableName: String, pkg: Package)
	{
		Log.d(LOG_TAG, "addPackage " + pkg.toString())

		val db = this.writableDatabase

		val values = ContentValues()
		values.put(KEY_PACKAGE, pkg.packageName)
		values.put(KEY_HANDLE, pkg.isHandlingThis)
		values.put(KEY_INTERVAL, pkg.remindIntervalSeconds)

		db.insert(tableName, // table
			null, // nullColumnHack
			values) // key/value -> keys = column names/ values = column
		// values

		db.close()
	}

	fun isPackageHandled(packageId: String): Boolean
	{
		val pkg = getPackage(TABLE_NAME, packageId)

		return pkg != null && pkg.isHandlingThis
	}

	fun getPackage(packageId: String): Package?
	{
		return getPackage(TABLE_NAME, packageId)
	}

	fun getPackage(tableName: String, packageId: String): Package?
	{
		val db = this.readableDatabase

		Log.d(LOG_TAG, "getPackage" + packageId)

		val cursor = db.query(tableName, // a. table
			COLUMNS, // b. column names
			" $KEY_PACKAGE = ?", // c. selections
			arrayOf<String>(packageId), // d. selections args
			null, // e. group by
			null, // f. h aving
			null, // g. order by
			null) // h. limit

		var pkg: Package? = null

		if (cursor != null && cursor.count >= 1)
		{
			cursor.moveToFirst()

			pkg = Package(cursor.getString(0), Integer.parseInt(cursor.getString(1)) != 0, Integer.parseInt(cursor.getString(2)))
		}

		cursor?.close()

		return pkg
	}

	val allPackages: List<Package>
		get()
		{
			val packages = LinkedList<Package>()

			val query = "SELECT  * FROM " + TABLE_NAME

			val db = this.writableDatabase
			val cursor = db.rawQuery(query, null)

			var pkg: Package? = null
			if (cursor.moveToFirst())
			{
				do
				{
					pkg = Package(cursor.getString(0), Integer.parseInt(cursor.getString(1)) != 0, Integer.parseInt(cursor.getString(2)))

					packages.add(pkg)
				} while (cursor.moveToNext())

				cursor.close()
			}

			return packages
		}

	val isEmpty: Boolean
		get()
		{
			var ret = true

			val query = "SELECT COUNT($KEY_PACKAGE) FROM $TABLE_NAME"

			val db = this.writableDatabase
			val cursor = db.rawQuery(query, null)

			if (cursor.moveToFirst())
			{
				val count = Integer.parseInt(cursor.getString(0))
				ret = (count == 0)

				cursor.close()
			}

			return ret
		}

	fun updatePackage(pkg: Package): Int
	{
		return updatePackage(TABLE_NAME, pkg)
	}

	fun updatePackage(tableName: String, pkg: Package): Int
	{
		val db = this.writableDatabase

		val values = ContentValues()
		// values.put(KEY_PACKAGE, pkg.getPackageName());
		values.put(KEY_HANDLE, pkg.isHandlingThis)
		values.put(KEY_INTERVAL, pkg.remindIntervalSeconds)

		val i = db.update(tableName, // table
			values, // column/value
			KEY_PACKAGE + " = ?", // selections
			arrayOf<String>(pkg.packageName!!)) // selection args

		db.close()

		return i
	}

	fun deletePackage(tableName: String, pkg: Package)
	{
		val db = this.writableDatabase

		db.delete(TABLE_NAME, // table name
			KEY_PACKAGE + " = ?", // selections
			arrayOf<String>(pkg.packageName!!)) // selections args

		db.close()

		Log.d(LOG_TAG, "deletePackage " + pkg.toString())
	}


	operator fun set(packageName: String, delay: Int, enabled: Boolean)
	{
		val pkg = getPackage(TABLE_NAME, packageName)

		if (pkg == null)
		{
			Log.d(LOG_TAG, "Added reminde for $packageName enabled: $enabled delay: $delay")
			addPackage(TABLE_NAME, Package(packageName, enabled, delay))
		}
		else
		{
			Log.d(LOG_TAG, "Updating reminder for $packageName enabled: $enabled delay: $delay")
			pkg.remindIntervalSeconds = delay
			pkg.isHandlingThis = enabled
			updatePackage(TABLE_NAME, pkg)
		}
	}

	fun getIsListed(packageName: String): Boolean
	{
		val pkg = getPackage(TABLE_NAME, packageName)
		return pkg != null
	}

	fun getIsHandled(packageName: String): Boolean
	{
		val pkg = getPackage(TABLE_NAME, packageName)
		return if (pkg != null) pkg.isHandlingThis else false
	}

	fun getInterval(packageName: String): Int
	{
		val pkg = getPackage(TABLE_NAME, packageName)
		return if (pkg != null) pkg.remindIntervalSeconds else DEFAULT_REMIND_INTERVAL
	}

	fun lookupEverywhereAndMoveOrInsertNew(packageName: String, _handleThis: Boolean, _interval: Int): Package
	{
		var pkg = getPackage(TABLE_NAME, packageName)

		if (pkg != null)
		{
			// nothing to do, it is already inside the main table
		}
		else
		{
			pkg = getPackage(TABLE_DISABLED_NAME, packageName)

			if (pkg != null)
			{
				deletePackage(TABLE_DISABLED_NAME, pkg) // delete it from the "disabled" table
			}
			else
			{
				pkg = Package(packageName, _handleThis, _interval)
			}

			addPackage(TABLE_NAME, pkg)
		}

		return pkg
	}

	fun hidePackage(pkg: Package)
	{
		deletePackage(TABLE_NAME, pkg)
		addPackage(TABLE_DISABLED_NAME, pkg)
	}

	companion object
	{
		private val LOG_TAG = "DB"

		val DEFAULT_REMIND_INTERVAL = 5 * 60

		private val DATABASE_VERSION = 5

		private val DATABASE_NAME = "Packages"

		private val TABLE_NAME = "packages"
		private val INDEX_NAME = "pkgidx"
		private val TABLE_DISABLED_NAME = "packages_disabled"

		// private static final String KEY_ID = "id";
		private val KEY_PACKAGE = "package"
		private val KEY_HANDLE = "handle"
		private val KEY_INTERVAL = "interval"
	}
}
