public interface ChessNetworkListener {
    void onConnected();
    void onColorAssigned(boolean clientIsWhite);
    void onNameReceived(String name);
    void onChatReceived(String msg);
    void onMoveReceived(int fromRow, int fromCol, int toRow, int toCol, boolean isWhite, String pawnPromotion, boolean isCheck, int peerSig);
    void onResigned();
    void onDrawOffer();
    void onDrawAccept();
    void onDrawDecline();
    void onRematchRequest();
    void onRematchAccept();
    void onRematchDecline();
    void onDisconnected(String reason);
}
