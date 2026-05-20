# ChessOL (Networked Java Chess)

A fully functional, multiplayer, graphical chess application built from scratch using Java Swing and Socket programming. ChessOL allows two players to connect over a network (LAN or WAN) and play a complete game of chess with real-time chat functionality.

## 🚀 Features

* **Complete Chess Logic Engine:** Hand-coded validation for all piece movements.
* **Advanced Rules Supported:** Fully supports Castling, En Passant, and Pawn Promotion.
* **Client/Server Architecture:** Host a game as a Server or connect via IP as a Client.
* **Automatic Board Flipping:** The board automatically flips so that the Client always views the board from Black's perspective and the Server from White's.
* **Integrated Chat:** Send instant messages to your opponent directly through the game interface.
* **Custom Graphics:** Features a beautifully custom-painted battle scene on the main menu and utilizes standard chess PNG sprites for gameplay.

## 📁 Project Structure

* `src/App.java` - Application entry point.
* `src/GUI.java` - Handles the Swing UI, canvas rendering, and Socket networking.
* `src/Game.java` - The core engine. Handles board state, move validation, and check/checkmate detection.
* `src/Piece.java` - Abstract base class representing a chess piece.
* `src/Pawn.java`, `King.java`, `Rook.java`, etc. - Individual piece movement logic.
* `ChessPieces/` - Directory containing the visual assets for the white and black pieces.

## 🛠️ Prerequisites

* Java Development Kit (JDK) 8 or higher.
* An IDE (Eclipse, VSCode, IntelliJ) or terminal capable of compiling Java source files.

## 🎮 How to Run

1. Clone this repository.
2. Ensure the `ChessPieces/` directory is located in the root of your working directory so the image loader can find the assets.
3. Compile and run the `GUI.java` file.
   ```bash
   javac -d bin src/*.java
   java -cp bin GUI
4. To Play:
   Player 1 (Host): Click Server. The game will begin waiting on port 8888.
   Player 2 (Join): Click Client. Enter the IP address of Player 1 (use 127.0.0.1 if testing on the same computer) and connect.

## ⌨️ Controls
Moving Pieces: Click a friendly piece to select it (it will highlight yellow). Click a valid destination square to move it.
Chatting: Type in the text field at the bottom of the window and press Enter to send a message to your opponent.
