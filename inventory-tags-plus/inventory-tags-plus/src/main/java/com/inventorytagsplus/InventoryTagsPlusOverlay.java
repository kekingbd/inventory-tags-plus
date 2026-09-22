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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

class InventoryTagsPlusOverlay extends WidgetItemOverlay
{
	private static final Tag NONE = new Tag();

	private final ItemManager itemManager;
	private final InventoryTagsPlusPlugin plugin;
	private final InventoryTagsPlusConfig config;
	private final Cache<Long, Image> fillCache;
	private final Cache<Integer, Tag> tagCache;

	@Inject
	private InventoryTagsPlusOverlay(ItemManager itemManager, InventoryTagsPlusPlugin plugin, InventoryTagsPlusConfig config)
	{
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.config = config;
		showOnEquipment();
		showOnInventory();
		showOnInterfaces(
			InterfaceID.RAIDS_STORAGE_SIDE,
			InterfaceID.RAIDS_STORAGE_PRIVATE,
			InterfaceID.RAIDS_STORAGE_SHARED,
			InterfaceID.GRAVESTONE_GENERIC
		);
		fillCache = CacheBuilder.newBuilder()
			.concurrencyLevel(1)
			.maximumSize(32)
			.build();
		tagCache = CacheBuilder.newBuilder()
			.concurrencyLevel(1)
			.maximumSize(39)
			.build();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		final Tag tag = getTag(itemId);
		if (tag == null || tag.color == null)
		{
			return;
		}

		final Color color = tag.color;
		final Rectangle bounds = widgetItem.getCanvasBounds();

		if (config.showTagOutline())
		{
			final int opacity = config.outlineOpacity();
			if (opacity > 0)
			{
				final BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), color);
				final Composite original = graphics.getComposite();
				if (opacity < 255)
				{
					graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity / 255f));
				}
				graphics.drawImage(outline, (int) bounds.getX(), (int) bounds.getY(), null);
				graphics.setComposite(original);
			}
		}

		if (config.showTagFill())
		{
			final Image image = getFillImage(color, widgetItem.getId(), widgetItem.getQuantity());
			graphics.drawImage(image, (int) bounds.getX(), (int) bounds.getY(), null);
		}

		if (config.showTagUnderline())
		{
			int heightOffSet = (int) bounds.getY() + (int) bounds.getHeight() + 2;
			graphics.setColor(color);
			graphics.drawLine((int) bounds.getX(), heightOffSet, (int) bounds.getX() + (int) bounds.getWidth(), heightOffSet);
		}
	}

	private Tag getTag(int itemId)
	{
		Tag tag = tagCache.getIfPresent(itemId);
		if (tag == null)
		{
			tag = plugin.getTag(itemId);
			tagCache.put(itemId, tag == null ? NONE : tag);
		}
		return tag == NONE ? null : tag;
	}

	private Image getFillImage(Color color, int itemId, int qty)
	{
		long key = (((long) itemId) << 32) | qty;
		Image image = fillCache.getIfPresent(key);
		if (image == null)
		{
			final Color fillColor = ColorUtil.colorWithAlpha(color, config.fillOpacity());
			image = ImageUtil.fillImage(itemManager.getImage(itemId, qty, false), fillColor);
			fillCache.put(key, image);
		}
		return image;
	}

	void invalidateCache()
	{
		fillCache.invalidateAll();
		tagCache.invalidateAll();
	}
}
