package ru.ifmo.wst.lab1;

import lombok.SneakyThrows;
import ru.ifmo.wst.lab1.client.ExterminatusServiceConsoleClient;
import ru.ifmo.wst.lab1.ws.client.ExterminatusService;
import ru.ifmo.wst.lab1.ws.client.ExterminatusServiceService;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConsoleClient {
    @SneakyThrows
    public static void main(String[] args) {
        ExterminatusServiceService exterminatusService = new ExterminatusServiceService();
        ExterminatusService service = exterminatusService.getExterminatusServicePort();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String endpointUrl;
        endpointUrl = "http://localhost:8080/EXTERMINATE";
        System.out.print("Enter endpoint url (or empty string for default " + endpointUrl + ")\n> ");
        String line = bufferedReader.readLine();
        if (line == null) {
            return;
        }
        ExterminatusServiceConsoleClient consoleClient = new ExterminatusServiceConsoleClient(service);
        if (!line.trim().isEmpty()) {
            endpointUrl = line.trim();
        }
        consoleClient.changeEndpointUrl(endpointUrl);

        consoleClient.info();
        consoleClient.start();

    }

}
