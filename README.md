# 💿 Dash's Custom Music Discs 🎵

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![NeoForge](https://img.shields.io/badge/Loader-NeoForge-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

**Ever felt like the vanilla music discs just don't hit the vibe of your base?** Stop settling for *Cat* or *Stal* and start spinning your own tracks. **Dash's Custom Music Discs** allows you to burn real-world `.mp3` files onto blank discs and play them in any standard Jukebox.

---

## ✨ Impressive Features

* 🎼 **Dynamic Audio Streaming**: Powered by an isolated **JLayer engine**, your music streams directly without bloat or massive resource packs.
* 📡 **Proximity-Based Sound**: Just like vanilla discs! The music gets quieter as you walk away and fades out completely at 64 blocks.
* 🎚️ **Vanilla-like Jukebox Logic**: Discs behave exactly as they should. Breaking the jukebox, ejecting the disc, or even a hopper pulling the record will instantly stop the audio. Jukeboxes playing the record also power adjacent blocks, emit particles, and make parrots dance.
* 🖥️ **Seamless GUI Burning**: No commands, no config digging. Just right-click with the **Disc Burner** to open the interface and burn your favorite tunes.
* 🤝 **Mod Compatibility**: Designed to work alongside other music mods without interfering with their keybinds or global playlists.
* 😋 **Immersing Music**: Multiple jukeboxes can be activated at the same time, and the nearest can be heard.  Moving between jukeboxes playing the same song **won't** interrupt the music. The music automatically loops and restarts, even if you rejoin the world.

---

## 🛠️ How to Use

### 1. Prepare your Track
Make sure you have a **.mp3** file ready on your computer. (Currently strictly supports `.mp3` for maximum stability).

### 2. The Burning Process
1.  Hold a **Blank Music Disc** in your **off-hand**.
2.  Right-click with the **Disc Burner** item in your main hand to open the **Upload GUI**.
3.  Select your file and hit **Burn**. 
4.  Once the "Burning Successful" message appears, your disc is ready!

### 3. Drop the Needle
Take your newly burned disc to any **Jukebox**. Right-click to insert and enjoy the high-fidelity sound.

---

## 📥 Installation

1.  Ensure you are running **NeoForge 1.21.1**.
2.  Drop the `.jar` into your `mods` folder.
3.  Make sure you have the **Background Music Mod** installed, this mod will automatically pause your background tracks when a Jukebox starts playing!

---

## 📂 Technical Details

* **Storage**: Burned music is stored server-side in `config/uploaded_music/`.
* **Networking**: Uses a custom chunked packet system to handle file uploads without crashing the server or timing out the player.
* **Sync**: Uses BlockState tracking to ensure audio is perfectly synced with the physical state of the Jukebox.

---

## 📜 Credits & Dependencies

* **Audio Engine**: Built using [JLayer](http://www.javazoom.net/javalayer/javalayer.html) for MP3 decoding.
* **Developer**: Created with ❤️ by **Dash**.

---

> _"Music is the shorthand of emotion." — Go build something epic with a soundtrack to match!_
