Compass Triangulator finds where a lodestone compass points. It shows the compass direction to a tenth of a degree, takes readings from different places and crosses them into an estimated position with an error margin. The compass does not have to be held: readings can also be taken from a compass shown in a screen, such as a server's auction house.

**Features**

- **Compass data:** the game turns a lodestone compass's needle on the client, so the target is stored in the item. The tooltip and an on-screen panel show that target (`Lodestone: X, Y, Z`) and the exact direction and distance to it from where you stand. The needle you see moves in 11.25° steps; this direction is exact.
- **Triangulation:** each reading is a line from where you stand in the direction the compass points. The estimate is the point closest to all the lines (weighted least squares), with an error margin at about 95% confidence. This also finds the real place when a server sends a fake point in the right direction instead of the real target.
- **Readings from a screen:** hover a compass in a chest or an auction house and press the "Record a reading" key. Teleport somewhere far, open the screen again and record again. This also works in a server's confirmation window.
- **Readings by looking:** when the compass has no data (a server drawing the needle with its own resource pack), turn until the needle points straight ahead and record the direction you are looking in.
- **Waypoints:** with Xaero's Minimap installed, a key adds the compass target or the estimate as a waypoint, named from the listing: `<seller> Lodestone <price>`.
- Commands (`/compasstri`, `record`, `undo`, `clear`, `waypoint`, `item`) that only run on your computer, keys you can bind, a settings screen, Mod Menu support and 19 languages.

**On servers**

The mod only reads data the server already sends to your client, does the math on your computer and never sends anything. Some servers do not allow client mods that give an advantage, so check the rules of the server you play on before using it there.

**Requirements**

- Minecraft 26.1.x, 26.2 or 26.3, Fabric Loader 0.19 or newer, Java 25
- Fabric API
- Optional: Mod Menu, Xaero's Minimap (for waypoints)

Client-side only: the server needs nothing.
