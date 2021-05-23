package Lessons;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class MyServer {

    private List<ClientHandler> clients;
    private AuthService authService;


    public MyServer() {

        try (ServerSocket server = new ServerSocket(ChatConstants.PORT)) {
            authService = new BaseAuthService();
            authService.start();
            clients = new ArrayList<>();
            connectionTimeout();

            while (true) {
                System.out.println("Сервер ожидает подключения");
                Socket socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (authService != null) {
                authService.stop();
            }
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized boolean isNickBusy(String nick) {
        return clients.stream().anyMatch(client -> client.getName().equals(nick));

    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClients();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClients();
    }


    public synchronized void broadcastMessage(String message) {
        clients.forEach(client -> client.sendMsg(message));

    }

    public synchronized void broadcastClients() {
        String clientsMessage = ChatConstants.CLIENTS_LIST +
                " " +
                clients.stream()
                        .map(ClientHandler::getName)
                        .collect(Collectors.joining(" "));
        clients.forEach(c-> c.sendMsg(clientsMessage));
    }


    public synchronized void privateMessage(String message, List<String> targeted ) {
        for (ClientHandler client : clients) {
            if (!targeted.contains(client.getName())) {
                continue;
            }
            String name = targeted.get(1);
            client.sendMsg("Лично от " + "[" + name + "]:" + " " + message);
        }
    }
    private void connectionTimeout() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    LocalDateTime now = LocalDateTime.now();

                    Iterator<ClientHandler> i = clients.iterator();
                    while (i.hasNext()) {
                        ClientHandler client = i.next();
                        if (!client.isActive()
                                && Duration.between(client.getConnectTime(), now).getSeconds() > ChatConstants.MAX_DELAY_TIME) {
                            System.out.println("Тайм-аут ожидания авторизации. Соединение закрыто");
                            client.closeConnection();
                            i.remove();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }





}