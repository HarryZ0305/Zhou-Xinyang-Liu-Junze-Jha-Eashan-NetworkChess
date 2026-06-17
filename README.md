# ChessOL - Modern Web Multiplayer Chess

A premium, responsive, multiplayer chess web application built from scratch in HTML5, CSS3, and ES6. ChessOL features a custom chess engine, a client-side minimax bot with alpha-beta pruning, and serverless peer-to-peer online multiplayer powered by WebRTC.

---

## ✨ Features

* **Complete Chess Engine (`chess.js`):** Hand-coded validation for all movement rules, castling, en passant, pawn promotions, and draw logic (50-move clock, threefold repetition, and insufficient material).
* **Minimax AI Bot (`bot.js`):** Play offline against an intelligent bot utilizing a 3-ply minimax depth search, alpha-beta pruning, and Piece-Square Table (PST) positional evaluation.
* **Serverless WebRTC Multiplayer (`network.js`):** Host or join online multiplayer matches over the internet using PeerJS—no central servers or database required. Connections are established directly between players using a simple opponent code.
* **Pass & Play (Local):** Play head-to-head local matches with a friend on the same screen.
* **Interactive UI & Visual Polish:**
  - Premium dark theme featuring smooth CSS glassmorphic panels and responsive grids.
  - Automatic board flipping based on your assigned color (White on bottom for host, Black on bottom for client).
  - Selected, last-move, and king-check highlighting.
  - Movable piece mechanics with drag-and-drop or two-click actions.
* **Procedural Sound Synthesis:** Dynamic click and alert audio synthesized directly in the browser via the native **Web Audio API** (no bulky audio assets required).
* **SAN Log & PGN Export:** Records game history in Standard Algebraic Notation (SAN) in real-time, allowing you to download standard PGN logs at game end.

---

## 📁 Project Structure

* `index.html` - Application entry point containing structural screens, game board container, and modals.
* `style.css` - Custom styling library defining dark mode variables, glassmorphism, responsive chess grids, and micro-animations.
* `chess.js` - The core chess logic model, move validator, and algebraic generator.
* `bot.js` - Standard minimax bot engine and PST tables.
* `network.js` - WebRTC PeerJS networking layer and communication handlers.
* `ui.js` - Controller orchestrating gameplay actions, timers, procedural sound events, and view states.
* `ChessPieces/` - Sprites for White and Black pieces.

---

## 🎮 How to Play

### 1. Locally (Double-click)
Simply open the [index.html](index.html) file in any modern web browser. 

### 2. On a Local Server
To run a local server:
```bash
# Using Python
python -m http.server 8000

# Using Node.js (npx)
npx serve
```
Then navigate to `http://localhost:8000` or `http://localhost:3000`.

### 3. Deploying to GitHub Pages
Since this is a fully static application, you can publish it directly to **GitHub Pages** by committing it to a repository and enabling Pages in the repository settings.

---

## ⌨️ Controls & Game Options

* **Moving Pieces:** Click a piece to select it (or drag it). Valid destinations and captures will show visual dots/rings. Click the destination to complete the move, or drag and drop. Click the selected piece again to deselect.
* **Draw Offer & Resign:** Buttons in the bottom-right control panel let you offer draws or resign.
* **PGN Export:** Click "Export PGN" on the Game Over screen to download a standard PGN file of your match.
