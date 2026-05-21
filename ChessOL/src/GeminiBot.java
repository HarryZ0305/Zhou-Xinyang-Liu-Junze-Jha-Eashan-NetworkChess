import java.net.*;
import java.net.URI;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class GeminiBot {

    private static final String API_KEY = loadApiKey();
    private static final String ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    private static String loadApiKey() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream("src/config.properties")) {
            props.load(in);
            return props.getProperty("gemini.api.key", "");
        } catch (Exception e) {
            System.out.println("config.properties not found, API key missing.");
            return "";
        }
    }

    // Returns [fromRow, fromCol, toRow, toCol] or null on failure.
    // Bot always plays as Black.
    public static int[] getMove(Piece[][] board) {
        String prompt = buildPrompt(board);
        String raw = callApi(prompt);
        if (raw == null) return null;
        return parseMove(raw);
    }

    private static String buildPrompt(Piece[][] board) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are playing chess as Black (lowercase pieces).\n");
        sb.append("Row 0 is Black's back rank, row 7 is White's back rank. Columns 0-7 (a-h).\n");
        sb.append("Board:\n");
        for (int r = 0; r < 8; r++) {
            sb.append(r).append(" ");
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p == null) {
                    sb.append(". ");
                } else {
                    String sym;
                    switch (p.getType()) {
                        case "Knight": sym = "N"; break;
                        case "Pawn":   sym = "P"; break;
                        case "Rook":   sym = "R"; break;
                        case "Bishop": sym = "B"; break;
                        case "Queen":  sym = "Q"; break;
                        default:       sym = "K"; break;
                    }
                    sb.append(p.isWhite ? sym : sym.toLowerCase()).append(" ");
                }
            }
            sb.append("\n");
        }
        sb.append("  0 1 2 3 4 5 6 7\n\n");
        sb.append("Choose a legal move for Black. ");
        sb.append("Reply with ONLY: fromRow,fromCol,toRow,toCol\n");
        sb.append("If it is a pawn promotion, append ,Q or ,R or ,B or ,N\n");
        sb.append("Example: 1,4,3,4");
        return sb.toString();
    }

    private static String callApi(String prompt) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(ENDPOINT).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);

            String escaped = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
            String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + escaped + "\"}]}]}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            return extractText(sb.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // Pull the first "text": "..." value out of the JSON response.
    private static String extractText(String json) {
        int idx = json.indexOf("\"text\":");
        if (idx < 0) return null;
        int start = json.indexOf("\"", idx + 7) + 1;
        int end   = json.indexOf("\"", start);
        if (start <= 0 || end <= start) return null;
        return json.substring(start, end)
                   .replace("\\n", "")
                   .trim();
    }

    // Parse "fromRow,fromCol,toRow,toCol[,PROMO]" into int[5]:
    //   [0-3] = row/col, [4] = promo char as int ('Q','R','B','N') or -1
    public static int[] parseMove(String text) {
        // Strip any non-digit/comma/letter noise from the model's reply
        text = text.replaceAll("[^0-9,QRBNqrbn]", "").trim();
        String[] parts = text.split(",");
        if (parts.length < 4) return null;
        try {
            int[] move = new int[5];
            move[0] = Integer.parseInt(parts[0]);
            move[1] = Integer.parseInt(parts[1]);
            move[2] = Integer.parseInt(parts[2]);
            move[3] = Integer.parseInt(parts[3]);
            move[4] = -1;
            if (parts.length >= 5 && !parts[4].isEmpty()) {
                move[4] = parts[4].toUpperCase().charAt(0);
            }
            // Basic sanity check
            for (int i = 0; i < 4; i++) {
                if (move[i] < 0 || move[i] > 7) return null;
            }
            return move;
        } catch (Exception e) {
            return null;
        }
    }
}
