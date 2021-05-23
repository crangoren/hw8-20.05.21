package Lessons;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;



public class ClientHandler {

    private MyServer server;
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private LocalDateTime connectTime = LocalDateTime.now();
    private String name;
    private boolean isAuth = false;

    public String getName() {
        return name;
    }

    public ClientHandler(MyServer server, Socket socket) {

        try {
            this.server = server;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            this.name = "";
            new Thread(() -> {
                try {
                    authentification();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }

            }).start();
        } catch (IOException ex) {
            System.out.println("Проблема при создании клиента");
        }
    }



    private void readMessages() throws IOException {
        while (true) {
            String messageFromClient = inputStream.readUTF();
            System.out.println("от " + name + ": " + messageFromClient);
            if (messageFromClient.equals(ChatConstants.STOP_WORD)) {
                return;
            } else if (messageFromClient.startsWith(ChatConstants.SEND_TO_LIST)) {
                String[] splittedStr = messageFromClient.split("\\s+");
                List<String> nicknames = new ArrayList<>();
                for (int i = 1; i < splittedStr.length - 1; i++) {
                    nicknames.add(splittedStr[i]);
                }
            } else if (messageFromClient.startsWith(ChatConstants.CLIENTS_LIST)) {
                server.broadcastClients();
            } else if (messageFromClient.startsWith(ChatConstants.PRIVATE_MESSAGE)){
                String[] targetMessage = messageFromClient.split("\\s+");
                for (int i = 0; i < targetMessage.length; i++) {
                    targetMessage[i] = targetMessage[i].replaceAll("[^\\w]", "");
                }
                List<String> target = Collections.singletonList(targetMessage[1]);
                target.add(name);

                String[] messageArr = messageFromClient.split("\\s+");
                for (int i = 2; i < messageArr.length; i++) {
                    messageArr[i] = messageArr[i].replaceAll("[^\\w]", "");
                }
                String message = messageArr.toString();

                server.privateMessage(message, target);

            } else {

                server.broadcastMessage("[" + name + "]: " + messageFromClient);
            }

        }
    }

    private void authentification() throws IOException {


        while (true) {
            String message = inputStream.readUTF();
            if (message.startsWith(ChatConstants.AUTH_COMMAND)) {
                String[] parts = message.split("\\s+");
                Optional<String> nick = server.getAuthService().getNickByLoginAndPass(parts[1], parts[2]);
                if (nick.isPresent()) {

                    if (!server.isNickBusy(nick.get())) {
                        sendMsg(ChatConstants.AUTH_OK + " " + nick);
                        isAuth = true;
                        name = nick.get();
                        server.subscribe(this);
                        server.broadcastMessage(name + " вошел в чат");

                        return;
                    } else {
                        sendMsg("Ник уже используется");
                    }
                } else {
                    sendMsg("Неверные логин/пароль");
                }
            }
        }
    }
    boolean isActive() {
        return isAuth;
    }

    public void sendMsg(String message) {
        try {
            outputStream.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        server.unsubscribe(this);
        server.broadcastMessage(name + " вышел из чата");
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    LocalDateTime getConnectTime() {
        return connectTime;
    }




}
