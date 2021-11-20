package threads;

import client.User;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Queue;

public class ReadThread extends Thread {
    private DataInputStream reader;
    private User user;

//    public ReadThread(User user) throws IOException {
//        this.user = user;
//        reader = new DataInputStream(user.getSocket().getInputStream());
//    }
//
//    public void run() {
//        while (isAlive()) {
//            if (!user.getSocket().isClosed()){
//                try {
//                    String response = reader.readUTF();
//                    sendMessage(response);
//                } catch (IOException e) {
//                    System.out.println("Error while reading message: " + e.getMessage());
//                    e.printStackTrace();
//                    break;
//                }
//            } else {
//                closeReadThread();
//                break;
//            }
//        }
//    }

    private void sendMessage(String text) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        System.out.println(("\n<" + dateTimeFormatter.format(now) + ">" + text).trim());
    }

//    private void closeReadThread() {
//        try {
//            reader.close();
//            user.getSocket().close();
//        } catch (IOException e) {
//            System.out.println("Error closing ReadThread: " + e.getMessage());
//            e.printStackTrace();
//        }
//        interrupt();
//    }
}

