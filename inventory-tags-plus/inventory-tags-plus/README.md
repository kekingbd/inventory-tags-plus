# Inventory Tags Plus

Everything the built-in **Inventory Tags** plugin does, plus:

- **Outline opacity** – a slider (0–255) for the outline, alongside the existing fill opacity.
- **Import / export** – share tags as JSON through the clipboard from the sidebar panel.
- **Copy from core** – one click to pull in the tags you already made with the built-in plugin.

## Tagging items

Shift + right-click an item in your inventory → **Inventory tag+** → pick a color, reuse a
color already on your inventory/equipment, or **Reset**. Tags render in the inventory,
equipment, Chambers of Xeric storage and gravestone interfaces.

## Sharing tags

Open the **Inventory Tags Plus** sidebar panel:

| Button | Effect |
| --- | --- |
| Export to clipboard | Copies all tags as JSON |
| Import (merge) | Adds tags from the clipboard, overwriting the same items |
| Import (replace) | Removes all tags, then imports (asks first) |
| Copy tags from core Inventory Tags | Reads the built-in plugin's tags into this one |
| Clear all tags | Removes every tag (asks first) |

### Format

```json
{
  "version": 1,
  "tags": [
    { "id": 4151, "name": "Abyssal whip", "color": "#FFFF0000" }
  ]
}
```

`id` is the item id. `name` is only for readability and is ignored on import. `color`
accepts `#RRGGBB` or `#AARRGGBB`. A bare array of tag objects is also accepted. Invalid
entries are skipped and a bad paste never clears your existing tags.

## Notes

If you run this alongside the built-in Inventory Tags plugin, both will draw — disable one.
