package com.sky.websocket;

import org.springframework.stereotype.Component;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket服務
 */
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    //存放會話對象
    private static Map<String, Session> sessionMap = new HashMap();

    /**
     * 連接建立成功調用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        System.out.println("客戶端：" + sid + "建立連接");
        sessionMap.put(sid, session);
    }

    /**
     * 收到客戶端消息後調用的方法
     *
     * @param message 客戶端發送過來的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        System.out.println("收到來自客戶端：" + sid + "的信息:" + message);
    }

    /**
     * 連接關閉調用方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        System.out.println("連接斷開:" + sid);
        sessionMap.remove(sid);
    }

    /**
     * 群發
     *
     * @param message
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                //服務氣象客戶端發送消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
