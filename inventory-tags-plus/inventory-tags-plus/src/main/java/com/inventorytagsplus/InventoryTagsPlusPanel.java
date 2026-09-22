/*
 * Copyright (c) 2026, kekingbd
 * All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

@Slf4j
class InventoryTagsPlusPanel extends PluginPanel
{
	private final InventoryTagsPlusPlugin plugin;
	private final JLabel countLabel = new JLabel();
	private final JLabel statusLabel = new JLabel();

	InventoryTagsPlusPanel(InventoryTagsPlusPlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		final JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
		final JLabel title = new JLabel("Inventory Tags Plus");
		title.setFont(FontManager.getRunescapeBoldFont());
		header.add(title);
		countLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		header.add(countLabel);
		add(header, BorderLayout.NORTH);

		final JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
		buttons.add(button("Export to clipboard", this::exportToClipboard));
		buttons.add(button("Import from clipboard (merge)", () -> importFromClipboard(false)));
		buttons.add(button("Import from clipboard (replace)", () -> importFromClipboard(true)));
		buttons.add(button("Copy tags from core Inventory Tags", this::importFromCore));
		buttons.add(button("Clear all tags", this::clearAll));
		add(buttons, BorderLayout.CENTER);

		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		add(statusLabel, BorderLayout.SOUTH);

		refreshCount();
	}

	@Override
	public void onActivate()
	{
		refreshCount();
		setStatus(" ", false);
	}

	private JButton button(String text, Runnable action)
	{
		final JButton b = new JButton(text);
		b.setFocusPainted(false);
		b.addActionListener(e -> action.run());
		return b;
	}

	private void refreshCount()
	{
		final int count = plugin.getTagCount();
		countLabel.setText(count + (count == 1 ? " tagged item" : " tagged items"));
	}

	private void setStatus(String text, boolean error)
	{
		statusLabel.setForeground(error ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.PROGRESS_COMPLETE_COLOR);
		// html so long messages wrap inside the sidebar
		statusLabel.setText("<html>" + text + "</html>");
	}

	private void exportToClipboard()
	{
		plugin.exportJson(json ->
		{
			try
			{
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
				final int count = plugin.getTagCount();
				setStatus("Copied " + count + " tag" + (count == 1 ? "" : "s") + " to clipboard.", false);
			}
			catch (IllegalStateException e)
			{
				log.warn("Unable to write clipboard", e);
				setStatus("Clipboard unavailable, try again.", true);
			}
		});
	}

	private void importFromClipboard(boolean replace)
	{
		final String text;
		try
		{
			text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		}
		catch (Exception e)
		{
			setStatus("Clipboard does not contain text.", true);
			return;
		}

		if (replace && plugin.getTagCount() > 0 && !confirm(
			"Replace ALL of your current tags with the tags on your clipboard?\n"
				+ "Export first if you want a backup."))
		{
			return;
		}

		final InventoryTagsPlusPlugin.ImportResult result = plugin.importJson(text, replace);
		if (result.error != null)
		{
			setStatus(result.error + skippedSuffix(result.skipped), true);
		}
		else
		{
			setStatus("Imported " + result.imported + " tag" + (result.imported == 1 ? "" : "s")
				+ skippedSuffix(result.skipped), false);
		}
		refreshCount();
	}

	private void importFromCore()
	{
		if (!confirm("Copy all tags from the built-in Inventory Tags plugin?\n"
			+ "Tags on the same items will be overwritten."))
		{
			return;
		}
		final int n = plugin.importFromCorePlugin();
		setStatus(n == 0 ? "No core Inventory Tags found." : "Copied " + n + " tag" + (n == 1 ? "" : "s") + ".", n == 0);
		refreshCount();
	}

	private void clearAll()
	{
		if (plugin.getTagCount() == 0 || !confirm("Delete all of your tags? This cannot be undone."))
		{
			return;
		}
		plugin.clearAllTags();
		setStatus("All tags cleared.", false);
		refreshCount();
	}

	private static String skippedSuffix(int skipped)
	{
		return skipped > 0 ? " (" + skipped + " invalid skipped)" : "";
	}

	private boolean confirm(String message)
	{
		return JOptionPane.showConfirmDialog(this, message, "Inventory Tags Plus",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
	}
}
