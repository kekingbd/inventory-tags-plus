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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Inventory Tags Plus",
	description = "Inventory Tags with outline opacity and JSON import/export for sharing tags",
	tags = {"highlight", "items", "overlay", "tagging", "inventory", "import", "export"}
)
@Slf4j
public class InventoryTagsPlusPlugin extends Plugin
{
	static final String MENU_OPTION = "Inventory tag+";

	private static final String TAG_KEY_PREFIX = "tag_";
	private static final String CORE_GROUP = "inventorytags";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private InventoryTagsPlusOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private Gson gson;

	@Inject
	private ColorPickerManager colorPickerManager;

	private NavigationButton navButton;

	@Provides
	InventoryTagsPlusConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InventoryTagsPlusConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);

		final InventoryTagsPlusPanel panel = new InventoryTagsPlusPanel(this);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Inventory Tags Plus")
			.icon(icon)
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		navButton = null;
	}

	// ---------------------------------------------------------------- tag storage

	Tag getTag(int itemId)
	{
		return readTag(InventoryTagsPlusConfig.GROUP, itemId);
	}

	void setTag(int itemId, Tag tag)
	{
		String json = gson.toJson(tag);
		configManager.setConfiguration(InventoryTagsPlusConfig.GROUP, TAG_KEY_PREFIX + itemId, json);
	}

	void unsetTag(int itemId)
	{
		configManager.unsetConfiguration(InventoryTagsPlusConfig.GROUP, TAG_KEY_PREFIX + itemId);
	}

	private Tag readTag(String group, int itemId)
	{
		String tag = configManager.getConfiguration(group, TAG_KEY_PREFIX + itemId);
		if (tag == null || tag.isEmpty())
		{
			return null;
		}

		try
		{
			return gson.fromJson(tag, Tag.class);
		}
		catch (JsonParseException e)
		{
			log.debug("Unable to parse tag for item {} in group {}", itemId, group, e);
			return null;
		}
	}

	/**
	 * @return item ids with a stored tag in the given group
	 */
	private List<Integer> getTaggedItemIds(String group)
	{
		final String prefix = group + "." + TAG_KEY_PREFIX;
		final List<Integer> ids = new ArrayList<>();
		for (String key : configManager.getConfigurationKeys(prefix))
		{
			try
			{
				ids.add(Integer.parseInt(key.substring(prefix.length())));
			}
			catch (NumberFormatException | IndexOutOfBoundsException e)
			{
				log.debug("Ignoring unexpected key {}", key);
			}
		}
		return ids;
	}

	private Map<Integer, Tag> getAllTags(String group)
	{
		final Map<Integer, Tag> tags = new TreeMap<>();
		for (int id : getTaggedItemIds(group))
		{
			Tag tag = readTag(group, id);
			if (tag != null && tag.color != null)
			{
				tags.put(id, tag);
			}
		}
		return tags;
	}

	int getTagCount()
	{
		return getTaggedItemIds(InventoryTagsPlusConfig.GROUP).size();
	}

	// ---------------------------------------------------------------- import / export

	/**
	 * Builds the export JSON on the client thread (item names need it), then hands
	 * the result to {@code callback} on the Swing thread.
	 */
	void exportJson(Consumer<String> callback)
	{
		clientThread.invokeLater(() ->
		{
			final TagTransfer.ExportFile file = new TagTransfer.ExportFile();
			for (Map.Entry<Integer, Tag> e : getAllTags(InventoryTagsPlusConfig.GROUP).entrySet())
			{
				final int id = e.getKey();
				String name = null;
				try
				{
					name = itemManager.getItemComposition(id).getName();
				}
				catch (Exception ex)
				{
					log.debug("Unable to look up name for item {}", id, ex);
				}
				file.tags.add(new TagTransfer.ExportTag(id, name, TagTransfer.toHex(e.getValue().color)));
			}

			final String json = gson.newBuilder().setPrettyPrinting().create().toJson(file);
			SwingUtilities.invokeLater(() -> callback.accept(json));
		});
	}

	static final class ImportResult
	{
		final int imported;
		final int skipped;
		final String error;

		private ImportResult(int imported, int skipped, String error)
		{
			this.imported = imported;
			this.skipped = skipped;
			this.error = error;
		}

		static ImportResult error(String message)
		{
			return new ImportResult(0, 0, message);
		}
	}

	/**
	 * @param replace if true, all existing tags are removed before importing (only
	 *                once the input has parsed and contains at least one valid tag)
	 */
	ImportResult importJson(String json, boolean replace)
	{
		if (json == null || json.trim().isEmpty())
		{
			return ImportResult.error("Clipboard is empty.");
		}

		final List<TagTransfer.ExportTag> entries;
		try
		{
			final JsonElement root = gson.fromJson(json, JsonElement.class);
			if (root == null || root.isJsonNull())
			{
				return ImportResult.error("No JSON found.");
			}
			if (root.isJsonArray())
			{
				entries = Arrays.asList(gson.fromJson(root, TagTransfer.ExportTag[].class));
			}
			else if (root.isJsonObject())
			{
				final TagTransfer.ExportFile file = gson.fromJson(root, TagTransfer.ExportFile.class);
				if (file.tags == null)
				{
					return ImportResult.error("JSON has no \"tags\" list.");
				}
				entries = file.tags;
			}
			else
			{
				return ImportResult.error("Unrecognised JSON.");
			}
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return ImportResult.error("Not valid tag JSON.");
		}

		// Validate everything first so a bad paste never wipes existing tags
		final Map<Integer, Color> valid = new LinkedHashMap<>();
		int skipped = 0;
		for (TagTransfer.ExportTag entry : entries)
		{
			final Color color = entry == null ? null : TagTransfer.fromHex(entry.color);
			if (entry == null || entry.id == null || entry.id < 0 || color == null)
			{
				skipped++;
				continue;
			}
			valid.put(entry.id, color);
		}

		if (valid.isEmpty())
		{
			return new ImportResult(0, skipped, "No valid tags found.");
		}

		if (replace)
		{
			clearAllTags();
		}

		for (Map.Entry<Integer, Color> e : valid.entrySet())
		{
			final Tag tag = new Tag();
			tag.color = e.getValue();
			setTag(e.getKey(), tag);
		}

		return new ImportResult(valid.size(), skipped, null);
	}

	/**
	 * Copies tags from the built-in RuneLite Inventory Tags plugin (read only).
	 * Existing tags for the same item are overwritten.
	 */
	int importFromCorePlugin()
	{
		final Map<Integer, Tag> core = getAllTags(CORE_GROUP);
		for (Map.Entry<Integer, Tag> e : core.entrySet())
		{
			setTag(e.getKey(), e.getValue());
		}
		return core.size();
	}

	void clearAllTags()
	{
		for (int id : getTaggedItemIds(InventoryTagsPlusConfig.GROUP))
		{
			unsetTag(id);
		}
	}

	// ---------------------------------------------------------------- events

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equals(InventoryTagsPlusConfig.GROUP))
		{
			overlay.invalidateCache();
		}
	}

	@Subscribe
	public void onMenuOpened(final MenuOpened event)
	{
		if (!client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		final MenuEntry[] entries = event.getMenuEntries();
		for (int idx = entries.length - 1; idx >= 0; --idx)
		{
			final MenuEntry entry = entries[idx];
			final Widget w = entry.getWidget();

			if (w != null && WidgetUtil.componentToInterface(w.getId()) == InterfaceID.INVENTORY
				&& "Examine".equals(entry.getOption()) && entry.getIdentifier() == 10)
			{
				final int itemId = w.getItemId();
				final Tag tag = getTag(itemId);

				final MenuEntry parent = client.createMenuEntry(idx)
					.setOption(MENU_OPTION)
					.setTarget(entry.getTarget())
					.setType(MenuAction.RUNELITE);
				final Menu submenu = parent.createSubMenu();

				Set<Color> invEquipmentColors = new HashSet<>();
				invEquipmentColors.addAll(getColorsFromItemContainer(InventoryID.INV));
				invEquipmentColors.addAll(getColorsFromItemContainer(InventoryID.WORN));
				for (Color color : invEquipmentColors)
				{
					if (tag == null || !color.equals(tag.color))
					{
						submenu.createMenuEntry(0)
							.setOption(ColorUtil.prependColorTag("Color", color))
							.setType(MenuAction.RUNELITE)
							.onClick(e ->
							{
								Tag t = new Tag();
								t.color = color;
								setTag(itemId, t);
							});
					}
				}

				submenu.createMenuEntry(0)
					.setOption("Pick")
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						Color color = tag == null || tag.color == null ? Color.WHITE : tag.color;
						SwingUtilities.invokeLater(() ->
						{
							RuneliteColorPicker colorPicker = colorPickerManager.create(client,
								color, "Inventory Tag", true);
							colorPicker.setOnClose(c ->
							{
								Tag t = new Tag();
								t.color = c;
								setTag(itemId, t);
							});
							colorPicker.setVisible(true);
						});
					});

				if (tag != null)
				{
					submenu.createMenuEntry(0)
						.setOption("Reset")
						.setType(MenuAction.RUNELITE)
						.onClick(e -> unsetTag(itemId));
				}
			}
		}
	}

	private List<Color> getColorsFromItemContainer(int inventoryID)
	{
		List<Color> colors = new ArrayList<>();
		ItemContainer container = client.getItemContainer(inventoryID);
		if (container != null)
		{
			for (Item item : container.getItems())
			{
				Tag tag = getTag(item.getId());
				if (tag != null && tag.color != null)
				{
					if (!colors.contains(tag.color))
					{
						colors.add(tag.color);
					}
				}
			}
		}
		return colors;
	}
}
