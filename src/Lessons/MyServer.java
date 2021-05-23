package Lessons;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Непосредственно сервер
 */
public class MyServer {

    private List<ClientHandler> clients;
    private AuthService authService;

    public MyServer() {
        try (ServerSocket server = new ServerSocket(ChatConstants.PORT)) {
            authService = new BaseAuthService();
            authService.start();
            clients = new ArrayList<>();
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
       /* for (ClientHandler client : clients) {
            if (client.getName().equals(nick)) {
                return true;
            }
        }
        return false;*/
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClients();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClients();
    }

    /**
     * Отправляет сообщение всем пользователям
     *
     * @param message
     */
    public synchronized void broadcastMessage(String message) {
        clients.forEach(client -> client.sendMsg(message));

    }

    public synchronized void broadcastMessageToClients(String message, List<String> nicknames) {
        clients.stream()
                .filter(c -> nicknames.contains(c.getName()))
                .forEach(c -> c.sendMsg(message));

        /*for (ClientHandler client : clients) {
            if (!nicknames.contains(client.getName())) {
              continue;
            }
            client.sendMsg(message);
        }*/
    }

    public synchronized void broadcastClients() {
        String clientsMessage = ChatConstants.CLIENTS_LIST +
                " " +
                clients.stream()
                        .map(ClientHandler::getName)
                        .collect(Collectors.joining(" "));
        // /client nick1 nick2 nick3
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





}