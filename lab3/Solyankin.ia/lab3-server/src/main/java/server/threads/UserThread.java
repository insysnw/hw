package server.threads;

import protocol.interaction.DataReader;
import protocol.interaction.DataWriter;
import protocol.messages.MessageFromClient;
import protocol.messages.MessageFromServer;
import protocol.messages.ResultWithErrCode;
import server.calculator.Calculator;
import server.resources.Phrases;

import java.io.IOException;
import java.net.Socket;

public class UserThread extends Thread {
    private Socket socket;
    private ServerThread server;
    private DataReader dataReader;
    private DataWriter dataWriter;
    private Calculator calculator;

    UserThread(Socket socket, ServerThread server) {
        try {
            this.server = server;
            this.socket = socket;
            dataReader = new DataReader(socket.getInputStream());
            dataWriter = new DataWriter(socket.getOutputStream());
            calculator = new Calculator();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println(String.format("<'%s', %s> %s", socket.getInetAddress(), socket.getPort(), Phrases.USER_CONNECTED.getPhrase()));
        server.addUserThreads(this);

        while (true) {
            if (dataReader.hasMessage()) {
                MessageFromClient message = dataReader.read();
                ResultWithErrCode resultWithErrCode = new ResultWithErrCode();
                switch (message.getOpcode()) {
                    case Calculator.SUM:
                        resultWithErrCode = calculator.sum(message.getArg1(), message.getArg2());
                        break;
                    case Calculator.SUB:
                        resultWithErrCode = calculator.sub(message.getArg1(), message.getArg2());
                        break;
                    case Calculator.MUL:
                        resultWithErrCode = calculator.mul(message.getArg1(), message.getArg2());
                        break;
                    case Calculator.DIV:
                        resultWithErrCode = calculator.div(message.getArg1(), message.getArg2());
                        break;
                    // Async
                    case Calculator.FACT:
                        resultWithErrCode = calculator.frac(message.getArg1());
                        break;
                    // Async
                    case Calculator.SQRT:
                        resultWithErrCode = calculator.sqrt(message.getArg1());
                        break;
                    case "6":
                        System.out.println(String.format("<'%s', %s> %s", socket.getInetAddress(), socket.getPort(), Phrases.USER_DISCONNECTED.getPhrase()));
                        closeUserThread();
                        break;
                }
                MessageFromServer messageFromServer = new MessageFromServer(message.getId(), resultWithErrCode);
                dataWriter.write(messageFromServer);
            }
        }
    }

    private void closeUserThread() {
        dataReader.close();
        dataWriter.close();
        server.removeUser(this);
        interrupt();
        System.exit(-1);
    }
}
