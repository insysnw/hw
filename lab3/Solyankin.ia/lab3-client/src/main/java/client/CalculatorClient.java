package client;

import client.http.HttpHandler;
import client.resources.Parts;
import client.resources.Phrases;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalculatorClient {
    private String host;
    private BufferedReader reader;

    public CalculatorClient(String host) {
        this.host = host;
        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        try {
            HttpHandler handler = new HttpHandler(host);
            System.out.println(Phrases.USER_CONNECTED.getPhrase());
            while (true) {
                String line = reader.readLine().trim();

                switch (Phrases.fromString(line.trim())) {
                    case CLIENT_COMMAND_INFO:
                        System.out.println(Phrases.USER_INFO.getPhrase());
                        continue;
                    case CLIENT_COMMAND_QUIT:
                        System.out.println(Phrases.USER_DISCONNECTED.getPhrase());
                        System.exit(-1);
                }

                Parts parts = getParts(line);
                if (parts.getOperands().size() != 0) {
                    handler.httpPostRequest(parts);
                } else {
                    System.out.println(Phrases.CLIENT_INCORRECT_EXPRESSION_ERROR.getPhrase());
                }
            }
        } catch (IOException e) {
            System.out.println(Phrases.CLIENT_INPUT_ERROR.getPhrase());
        }
    }


    private Parts getParts(String line) {
        ArrayList<String> operands = new ArrayList<>();
        String operator = "";
        if (line.contains(Phrases.FACT_COMMAND.getPhrase())) {
            operator = Phrases.FACT_COMMAND.getPhrase();
            operands = parseLine(line, Phrases.FACT_COMMAND);
        } else if (line.contains(Phrases.SQRT_COMMAND.getPhrase())) {
            operator = Phrases.SQRT_COMMAND.getPhrase();
            operands = parseLine(line, Phrases.SQRT_COMMAND);
        } else {
            int count = 0;
            String regex = "[+\\-/*]";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                if (count++ == 0) {
                    operator = matcher.group();
                    try {
                        operands.add(BigDecimal.valueOf(Long.parseLong(line.substring(0, matcher.end() - 1))).toString());
                        operands.add(BigDecimal.valueOf(Long.parseLong(line.substring(matcher.end()))).toString());
                    } catch (NumberFormatException e) {
                        operands = new ArrayList<>();
                        return new Parts(operands, operator);
                    }
                } else {
                    operands = new ArrayList<>();
                    return new Parts(operands, operator);
                }
            }
        }
        return new Parts(operands, operator);
    }

    private ArrayList<String> parseLine(String line, Phrases phrase) {
        ArrayList<String> result = new ArrayList<>();
        String regexFact = phrase.getPhrase();
        Pattern patternFact = Pattern.compile(regexFact);
        Matcher matcherNumbers = patternFact.matcher(line);
        while (matcherNumbers.find()) {
            try {
                result.add(BigDecimal.valueOf(Long.parseLong(line.substring(matcherNumbers.end() + 1, line.length() - 1))).toString());
            } catch (NumberFormatException e) {
                break;
            }
        }
        return result;
    }
}
