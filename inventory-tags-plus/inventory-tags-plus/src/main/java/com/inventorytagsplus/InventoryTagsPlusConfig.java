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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(InventoryTagsPlusConfig.GROUP)
public interface InventoryTagsPlusConfig extends Config
{
	String GROUP = "inventorytagsplus";

	@ConfigSection(
		name = "Tag display mode",
		description = "How tags are displayed in the inventory.",
		position = 0
	)
	String tagStyleSection = "tagStyleSection";

	@ConfigItem(
		position = 0,
		keyName = "showTagOutline",
		name = "Outline",
		description = "Configures whether or not item tags should be outlined.",
		section = tagStyleSection
	)
	default boolean showTagOutline()
	{
		return true;
	}

	@Range(
		max = 255
	)
	@ConfigItem(
		position = 1,
		keyName = "outlineOpacity",
		name = "Outline opacity",
		description = "Configures the opacity of the tag outline (0 = invisible, 255 = solid)",
		section = tagStyleSection
	)
	default int outlineOpacity()
	{
		return 255;
	}

	@ConfigItem(
		position = 2,
		keyName = "tagUnderline",
		name = "Underline",
		description = "Configures whether or not item tags should be underlined.",
		section = tagStyleSection
	)
	default boolean showTagUnderline()
	{
		return false;
	}

	@ConfigItem(
		position = 3,
		keyName = "tagFill",
		name = "Fill",
		description = "Configures whether or not item tags should be filled.",
		section = tagStyleSection
	)
	default boolean showTagFill()
	{
		return false;
	}

	@Range(
		max = 255
	)
	@ConfigItem(
		position = 4,
		keyName = "fillOpacity",
		name = "Fill opacity",
		description = "Configures the opacity of the tag fill",
		section = tagStyleSection
	)
	default int fillOpacity()
	{
		return 50;
	}
}
