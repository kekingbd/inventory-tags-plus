package com.inventorytagsplus;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class InventoryTagsPlusPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(InventoryTagsPlusPlugin.class);
		RuneLite.main(args);
	}
}
