/*
 * Copyright (c) 2018 kulers
 * Copyright (c) 2022, Adam <Adam@sigterm.info>
 * Copyright (c) 2026, kekingbd
 * All rights reserved.
 *
 * Derived from the RuneLite Inventory Tags plugin.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.inventorytagsplus;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Shareable JSON format for tags.
 *
 * <pre>
 * {
 *   "version": 1,
 *   "tags": [
 *     { "id": 4151, "name": "Abyssal whip", "color": "#FFFF0000" }
 *   ]
 * }
 * </pre>
 *
 * "name" is informational only; import keys on "id". "color" accepts #RRGGBB or #AARRGGBB.
 * A bare array of tag objects is also accepted on import.
 */
final class TagTransfer
{
	static final int FORMAT_VERSION = 1;

	private TagTransfer()
	{
	}

	static final class ExportFile
	{
		int version = FORMAT_VERSION;
		List<ExportTag> tags = new ArrayList<>();
	}

	static final class ExportTag
	{
		Integer id;
		String name;
		String color;

		ExportTag()
		{
		}

		ExportTag(int id, String name, String color)
		{
			this.id = id;
			this.name = name;
			this.color = color;
		}
	}

	static String toHex(Color color)
	{
		return String.format("#%08X", color.getRGB());
	}

	/**
	 * @return the parsed color, or null if the string is not #RRGGBB / #AARRGGBB
	 */
	static Color fromHex(String hex)
	{
		if (hex == null)
		{
			return null;
		}

		String s = hex.trim();
		if (s.startsWith("#"))
		{
			s = s.substring(1);
		}
		else if (s.startsWith("0x") || s.startsWith("0X"))
		{
			s = s.substring(2);
		}

		if (s.length() != 6 && s.length() != 8)
		{
			return null;
		}

		try
		{
			long value = Long.parseLong(s, 16);
			if (s.length() == 6)
			{
				return new Color((int) value);
			}
			return new Color((int) value, true);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
