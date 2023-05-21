package tf.bug.chatchatfx.game;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import tf.bug.chatchatfx.model.GameStateModel;
import tf.bug.chatchatfx.model.PlayerModel;

public class GameWebsocket {

    private final GameStateModel gsm;
    private final HttpClient hc;
    private final Consumer<Throwable> onError;

    private CompletableFuture<WebSocket> cfw;

    public GameWebsocket(HttpClient hc, URI wsUri, GameStateModel gsm, Consumer<Throwable> onError) {
        this.hc = hc;
        this.gsm = gsm;
        this.onError = onError;

        this.cfw = this.hc.newWebSocketBuilder()
                .header("Origin", "https://chatchatgame.netlify.app")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
                .subprotocols("permessage-deflate", "client_max_window_bits")
                .buildAsync(wsUri, new Listener())
                .whenComplete((w, t) -> { if(t != null) this.onError.accept(t); });
    }

    public synchronized void sendBinary(ByteBuffer data, boolean last) {
        this.cfw = this.cfw.thenCompose(ws -> ws.sendBinary(data, last));
    }

    public synchronized void sendText(CharSequence data, boolean last) {
        this.cfw = this.cfw.thenCompose(ws -> ws.sendText(data, last));
    }

    public synchronized void close(int statusCode, String reason) {
        this.cfw = this.cfw.thenCompose(ws -> ws.sendClose(statusCode, reason));
    }

    private class Listener implements WebSocket.Listener {

        private ByteBuffer accumulatedByteBuffer;
        private final StringBuilder accumulatedString;

        private Listener() {
            this.accumulatedByteBuffer = ByteBuffer.allocate(1024);
            this.accumulatedString = new StringBuilder();
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            int position = this.accumulatedByteBuffer.position();
            try {
                this.accumulatedByteBuffer.put(data);
                if(last) {
                    ByteBuffer whole = this.accumulatedByteBuffer.duplicate();
                    whole.flip();
                    this.accumulatedByteBuffer.clear();
                    CompletableFuture.runAsync(() -> GameWebsocket.this.gsm.handleBytes(whole));
                }
                webSocket.request(1);
                return null;
            } catch (BufferOverflowException boe) {
                ByteBuffer newByteBuffer = ByteBuffer.allocate(this.accumulatedByteBuffer.capacity() * 2);
                newByteBuffer.put(this.accumulatedByteBuffer);
                newByteBuffer.position(position);
                this.accumulatedByteBuffer = newByteBuffer;
                return this.onBinary(webSocket, data, last);
            }
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            this.accumulatedString.append(data);
            if(last) {
                String whole = this.accumulatedString.toString();
                this.accumulatedString.setLength(0);
                CompletableFuture.runAsync(() -> {
                    int idxOfSpace = whole.indexOf(' ');
                    String tpe = whole.substring(0, idxOfSpace);
                    String payload = whole.substring(idxOfSpace + 1);
                    GameWebsocket.this.gsm.handleText(tpe, payload);
                });
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            GameWebsocket.this.onError.accept(error);
        }
    }

}
