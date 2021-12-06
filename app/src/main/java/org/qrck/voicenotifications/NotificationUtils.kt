/*
 * Copyright (c) 2021, Sergey Parshin, quarck@gmail.com
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

val Notification.title: String
	get() {
		val extras = this.extras ?: return ""
		if (extras.get(Notification.EXTRA_TITLE) == null)
			return ""
		return extras.getCharSequence(Notification.EXTRA_TITLE).toString()
	}

val Notification.bigTitle: String
	get() {
		val extras = this.extras ?: return ""
		if (extras.get(Notification.EXTRA_TITLE_BIG) == null)
			return ""
		return (extras.getCharSequence(Notification.EXTRA_TITLE_BIG) as CharSequence).toString()
	}

val Notification.text: String
	get() {
		val extras = this.extras ?: return ""
		if (extras.get(Notification.EXTRA_TEXT) == null)
			return "";
		return extras.getCharSequence(Notification.EXTRA_TEXT).toString()
	}

val Notification.bigText: String
	get() {
		val extras = this.extras ?: return ""
		if (extras.get(Notification.EXTRA_TEXT_LINES) == null)
			return "";

		val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
		var text = ""
		if (lines != null) {
			for (line in lines) {
				text += line
				text += "\n"
			}
			text = text.trim();
		}
		return text
	}

fun Notification.getTitleAndText(): Pair<String, String>
{
	val extras = this.extras ?: return Pair("", "");

	var title = ""
	var text = ""

	val extraTitle = extras.get(Notification.EXTRA_TITLE)
	val extraTitleBig = extras.get(Notification.EXTRA_TITLE_BIG)
	val extraText = extras.get(Notification.EXTRA_TEXT)
	val extraTextLines = extras.get(Notification.EXTRA_TEXT_LINES)

	if (extraTitleBig != null) {
		val bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG) as CharSequence
		title = if (bigTitle.length < 40 || extraTitle == null)
			bigTitle.toString()
		else
			extras.getCharSequence(Notification.EXTRA_TITLE).toString()
	} else if (extraTitle != null) {
		title = extras.getCharSequence(Notification.EXTRA_TITLE).toString()
	}

	if (extraTextLines != null) {
		val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
		if (lines != null) {
			for (line in lines) {
				text += line
				text += "\n"
			}
			text = text.trim();
		} else if (extraText != null) {
			text = extras.getCharSequence(Notification.EXTRA_TEXT).toString()
		}
	} else if (extraText != null) {
		text = extras.getCharSequence(Notification.EXTRA_TEXT).toString()
	}

	return Pair(title, text)
}
