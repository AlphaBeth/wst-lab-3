package ru.ifmo.wst.lab1.client;

import lombok.Getter;
import lombok.SneakyThrows;
import ru.ifmo.wst.lab1.Box;
import ru.ifmo.wst.lab1.Pair;
import ru.ifmo.wst.lab1.command.Command;
import ru.ifmo.wst.lab1.command.CommandArg;
import ru.ifmo.wst.lab1.command.CommandArgDescription;
import ru.ifmo.wst.lab1.command.CommandInterpreter;
import ru.ifmo.wst.lab1.command.NoLineFoundException;
import ru.ifmo.wst.lab1.command.args.DateArg;
import ru.ifmo.wst.lab1.command.args.EmptyStringToNull;
import ru.ifmo.wst.lab1.command.args.LongArg;
import ru.ifmo.wst.lab1.command.args.StringArg;
import ru.ifmo.wst.lab1.ws.client.ForbiddenException;
import ru.ifmo.wst.lab1.ws.client.UnuathorizedException;
import ru.ifmo.wst.lab1.ws.client.Create;
import ru.ifmo.wst.lab1.ws.client.ExterminatusEntity;
import ru.ifmo.wst.lab1.ws.client.ExterminatusService;
import ru.ifmo.wst.lab1.ws.client.ExterminatusServiceException;
import ru.ifmo.wst.lab1.ws.client.FilterParam;
import ru.ifmo.wst.lab1.ws.client.Update;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class ExterminatusServiceConsoleClient {
    private static final String ID_ARG_NAME = "id";
    private static final String ID_ARG_DESCR = "Exterminatus id";
    private static final String INITIATOR_ARG_NAME = "initiator";
    private static final String INITIATOR_ARG_DESCR = "Initiator name";
    private static final String REASON_ARG_NAME = "reason";
    private static final String REASON_ARG_DESCR = "Reason of exterminatus";
    private static final String METHOD_ARG_NAME = "method";
    private static final String METHOD_ARG_DESCR = "Method of exterminatus";
    private static final String PLANET_ARG_NAME = "planet";
    private static final String PLANET_ARG_DESCR = "Exterminated planet";
    private static final String DATE_ARG_NAME = "date";
    private static final String DATE_ARG_DESCR = "Date of exterminatus";


    private static final StringArg INITATOR_COMMAND_ARG = new StringArg(INITIATOR_ARG_NAME, INITIATOR_ARG_DESCR);
    private static final StringArg REASON_COMMAND_ARG = new StringArg(REASON_ARG_NAME, REASON_ARG_DESCR);
    private static final StringArg METHOD_COMMAND_ARG = new StringArg(METHOD_ARG_NAME, METHOD_ARG_DESCR);
    private static final StringArg PLANET_COMMAND_ARG = new StringArg(PLANET_ARG_NAME, PLANET_ARG_DESCR);
    private static final DateArg DATE_COMMAND_ARG = new DateArg(DATE_ARG_NAME, DATE_ARG_DESCR);
    private static final LongArg ID_COMMAND_ARG = new LongArg(ID_ARG_NAME, ID_ARG_DESCR);

    private final CommandInterpreter commandInterpreter;
    @Getter
    private boolean exit = false;
    private ExterminatusService service;
    private String username = "";
    private String password = "";

    public ExterminatusServiceConsoleClient(ExterminatusService service) {
        this.service = service;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        Command<Void> infoCommand = new Command<>("info", "Print help for commands", (arg) -> this.info());
        Command<Box<String>> changeEndpointAddressCommand = new Command<>("endpoint", "Changes endpoint address",
                Arrays.asList(
                        new CommandArg<>(new StringArg("url", "New exterminatus endpoint url"), Box::setValue)
                ), Box::new, this::changeEndpointUrl
        );
        Command<Void> findAllCommand = new Command<>("findAll", "Return list of all exterminatus entities", (arg) -> this.findAll());
        Command<FilterParam> filterCommand = new Command<>("filter",
                "Filter exterminatus entities by column values (ignore case contains for strings), empty values are ignored",
                asList(
                        new CommandArg<>(toNull(ID_COMMAND_ARG), FilterParam::setId),
                        new CommandArg<>(toNull(INITATOR_COMMAND_ARG), FilterParam::setInitiator),
                        new CommandArg<>(toNull(REASON_COMMAND_ARG), FilterParam::setReason),
                        new CommandArg<>(toNull(METHOD_COMMAND_ARG), FilterParam::setMethod),
                        new CommandArg<>(toNull(PLANET_COMMAND_ARG), FilterParam::setPlanet),
                        new CommandArg<>(toNull(DATE_COMMAND_ARG), (filter, date) -> filter.setDate(fromDate(date)))
                ),
                FilterParam::new, this::filter);
        Command<Create> createCommand = new Command<>("create",
                "Create new exterminatus entity",
                asList(
                        new CommandArg<>(toNull(INITATOR_COMMAND_ARG), Create::setInitiator),
                        new CommandArg<>(toNull(REASON_COMMAND_ARG), Create::setReason),
                        new CommandArg<>(toNull(METHOD_COMMAND_ARG), Create::setMethod),
                        new CommandArg<>(toNull(PLANET_COMMAND_ARG), Create::setPlanet),
                        new CommandArg<>(toNull(DATE_COMMAND_ARG), (filter, date) -> filter.setDate(fromDate(date)))
                ), Create::new, this::create);
        Command<Update> updateCommand = new Command<>("update",
                "Update exterminatus by id",
                asList(
                        new CommandArg<>(ID_COMMAND_ARG, Update::setId),
                        new CommandArg<>(toNull(INITATOR_COMMAND_ARG), Update::setInitiator),
                        new CommandArg<>(toNull(REASON_COMMAND_ARG), Update::setReason),
                        new CommandArg<>(toNull(METHOD_COMMAND_ARG), Update::setMethod),
                        new CommandArg<>(toNull(PLANET_COMMAND_ARG), Update::setPlanet),
                        new CommandArg<>(toNull(DATE_COMMAND_ARG), (filter, date) -> filter.setDate(fromDate(date)))
                ), Update::new, this::update
        );
        Command<Box<Long>> deleteCommand = new Command<>("delete", "Delete exterminatus by id",
                asList(
                        new CommandArg<>(ID_COMMAND_ARG, Box::setValue)
                ), Box::new, this::delete);
        Command<Pair<String, String>> authCommand = new Command<>("auth", "Change auth info",
                asList(
                        new CommandArg<>(new StringArg("username", "Username"), Pair::setLeft),
                        new CommandArg<>(new StringArg("password", "Password"), Pair::setRight)
                ), Pair::new, this::updateAuth);
        Command<Void> exitCommand = new Command<>("exit", "Exit application", (arg) -> this.exit = true);
        this.commandInterpreter = new CommandInterpreter(() -> readLine(bufferedReader),
                System.out::print, asList(infoCommand, changeEndpointAddressCommand, authCommand, findAllCommand, filterCommand,
                createCommand, updateCommand, deleteCommand, exitCommand),
                "No command found",
                "Enter command", "> ");

    }

    private void updateAuth(Pair<String, String> authInfo) {
        this.username = authInfo.getLeft();
        this.password = authInfo.getRight();
    }

    @SneakyThrows
    public void create(Create createArg) {
        preAuth();
        long createdId = service.create(createArg.getInitiator(), createArg.getReason(), createArg.getMethod(), createArg.getPlanet(),
                createArg.getDate());
        System.out.printf("Entity with id %d was created\n", createdId);
    }

    private void preAuth() {
        BindingProvider bp = (BindingProvider) service;
        Map<String, Object> rc = bp.getRequestContext();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Authorization", singletonList("Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8))));
        rc.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
    }

    @SneakyThrows
    public void update(Update updateArg) {
        preAuth();
        int updateCount = service.update(updateArg.getId(), updateArg.getInitiator(), updateArg.getReason(), updateArg.getMethod(),
                updateArg.getPlanet(), updateArg.getDate());
        System.out.printf("%d rows were updated by id %d\n", updateCount, updateArg.getId());
    }

    @SneakyThrows
    public void delete(long id) {
        preAuth();
        int deleteCount = service.delete(id);
        System.out.printf("%d rows were deleted by id %d\n", deleteCount, id);
    }

    @SneakyThrows
    private void delete(Box<Long> id) {
        delete(id.getValue());
    }


    public void start() {
        while (!exit) {
            try {
                commandInterpreter.readCommand();
            } catch (NoLineFoundException exc) {
                exit = true;
            } catch (ExterminatusServiceException exc) {
                System.out.println("Error in service:");
                System.out.println(exc.getFaultInfo().getMessage());
            } catch (ForbiddenException exc) {
                System.out.println("Forbidden");
                System.out.println(exc.getFaultInfo().getMessage());
            } catch (UnuathorizedException exc) {
                System.out.println("Authorization required");
                System.out.println(exc.getFaultInfo().getMessage());
            } catch (Exception exc) {
                System.out.println("Unknown error");
                exc.printStackTrace();
            }

        }
    }

    public void changeEndpointUrl(Box<String> box) {
        changeEndpointUrl(box.getValue());
    }

    public void changeEndpointUrl(String endpointUrl) {
        ((BindingProvider) service).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
    }

    @SneakyThrows
    public void findAll() {
        List<ExterminatusEntity> all = service.findAll();
        System.out.println("Result of operation:");
        all.forEach(ee -> System.out.println(exterminatusToString(ee)));
    }

    @SneakyThrows
    public void filter(FilterParam filterArg) {
        List<ExterminatusEntity> filterRes = service.filter(filterArg);
        System.out.println("Result of operation:");
        filterRes.forEach(ee -> System.out.println(exterminatusToString(ee)));
    }

    public void info() {
        commandInterpreter.info();
    }


    @SneakyThrows
    private static String readLine(BufferedReader reader) {
        return reader.readLine();
    }

    @SneakyThrows
    private static XMLGregorianCalendar fromDate(Date date) {
        if (date == null) {
            return null;
        }
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(
                gregorianCalendar);
    }

    private static <T> CommandArgDescription<T> toNull(CommandArgDescription<T> commandArg) {
        return new EmptyStringToNull<>(commandArg);
    }

    private static String exterminatusToString(ru.ifmo.wst.lab1.ws.client.ExterminatusEntity ee) {
        return "ExterminatusEntity{" +
                "date=" + ee.getDate() +
                ", id=" + ee.getId() +
                ", initiator='" + ee.getInitiator() + '\'' +
                ", method='" + ee.getMethod() + '\'' +
                ", planet='" + ee.getPlanet() + '\'' +
                ", reason='" + ee.getReason() + '\'' +
                '}';

    }
}
