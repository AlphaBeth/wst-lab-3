package ru.ifmo.wst.lab1.ws;

import lombok.extern.slf4j.Slf4j;
import ru.ifmo.wst.AuthChecker;
import ru.ifmo.wst.lab1.dao.ExterminatusDAO;
import ru.ifmo.wst.lab1.model.ExterminatusEntity;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@WebService
public class ExterminatusService {
    @Inject
    private ExterminatusDAO exterminatusDAO;
    private AuthChecker authChecker;
    @Resource
    private WebServiceContext webServiceContext;

    public ExterminatusService(ExterminatusDAO exterminatusDAO, AuthChecker authChecker) {
        this.authChecker = authChecker;
        this.exterminatusDAO = exterminatusDAO;
    }

    public ExterminatusService() {
    }

    @WebMethod
    public List<ExterminatusEntity> findAll() throws ExterminatusServiceException {
        return wrapException(() -> exterminatusDAO.findAll());
    }

    @WebMethod
    public List<ExterminatusEntity> filter(FilterParam filterParam)
            throws ExterminatusServiceException {
        return wrapException(() -> exterminatusDAO.filter(filterParam.getId(), filterParam.getInitiator(),
                filterParam.getReason(), filterParam.getMethod(), filterParam.getPlanet(), filterParam.getDate()));
    }

    @WebMethod
    public long create(@WebParam(name = "initiator") String initiator,
                       @WebParam(name = "reason") String reason, @WebParam(name = "method") String method,
                       @WebParam(name = "planet") String planet, @WebParam(name = "date") Date date)
            throws ExterminatusServiceException, UnuathorizedException, ForbiddenException {
        checkAuth();
        notNullArg("initiator", initiator);
        notNullArg("method", method);
        notNullArg("planet", planet);
        notNullArg("date", date);
        return wrapException(() -> exterminatusDAO.create(initiator, reason, method, planet, date));
    }

    private void checkAuth() throws ForbiddenException, UnuathorizedException {
        List<String> creds = decodeAuthHeader();
        String username = creds.get(0);
        String pass = creds.get(1);
        boolean check = authChecker.check(username, pass);
        if (!check) {
            String mes = "User not found";
            throw new ForbiddenException(mes, new ExterminatusServiceFault(mes));
        }
    }

    private List<String> decodeAuthHeader() throws UnuathorizedException, ForbiddenException {
        MessageContext mctx = webServiceContext.getMessageContext();
        Map headers = (Map) mctx.get(MessageContext.HTTP_REQUEST_HEADERS);
        List<String> authorization = (List<String>) headers.get("Authorization");
        if (authorization == null || authorization.isEmpty() || authorization.size() > 1) {
            throw new UnuathorizedException("No authorization header", new ExterminatusServiceFault("No authorization header"));
        }
        String header = authorization.get(0);
        String basicRegex = "^Basic\\s+";
        Pattern compile = Pattern.compile(basicRegex);
        Matcher matcher = compile.matcher(header);
        if (!matcher.find()) {
            String mes = "Not an basic auth";
            throw new ForbiddenException(mes, new ExterminatusServiceFault(mes));
        }
        String encodedCreds = matcher.replaceFirst("");
        String decodedCreds;
        try {
            decodedCreds = new String(Base64.getDecoder().decode(encodedCreds), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exc) {
            String mes = "Isn't base64 encoded";
            throw new ForbiddenException(mes, new ExterminatusServiceFault(mes));
        }
        String[] split = decodedCreds.split(":");
        if (split.length != 2) {
            String mes = "Wrong header format";
            throw new ForbiddenException(mes, new ExterminatusServiceFault(mes));
        }
        return Arrays.asList(split);
    }

    @WebMethod
    public int delete(@WebParam(name = "id") long id) throws ExterminatusServiceException, UnuathorizedException, ForbiddenException {
        checkAuth();
        return wrapException(() -> {
            int deletedCount = exterminatusDAO.delete(id);
            if (deletedCount <= 0) {
                String message = String.format("No records with id %d found to delete", id);
                throw new ExterminatusServiceException(message, new ExterminatusServiceFault(message));
            }
            return deletedCount;
        });
    }

    @WebMethod
    @WebResult(name = "updatedCount")
    public int update(@WebParam(name = "id") long id, @WebParam(name = "initiator") String initiator,
                      @WebParam(name = "reason") String reason, @WebParam(name = "method") String method,
                      @WebParam(name = "planet") String planet, @WebParam(name = "date") Date date) throws ExterminatusServiceException, UnuathorizedException, ForbiddenException {
        checkAuth();
        notNullArg("initiator", initiator);
        notNullArg("method", method);
        notNullArg("planet", planet);
        notNullArg("date", date);
        return wrapException(() -> {
            int updatedCount = exterminatusDAO.update(id, initiator, reason, method, planet, date);
            if (updatedCount <= 0) {
                String message = String.format("No records with id %d found to update", id);
                throw new ExterminatusServiceException(message, new ExterminatusServiceFault(message));
            }
            return updatedCount;
        });
    }

    private void notNullArg(String argName, Object argValue) throws ExterminatusServiceException {
        if (argValue == null) {
            String message = argName + " must be not null";
            throw new ExterminatusServiceException(message, new ExterminatusServiceFault(message));
        }
    }

    private <T> T wrapException(Supplier<T> supplier) throws ExterminatusServiceException {
        try {
            return supplier.produce();
        } catch (ExterminatusServiceException exc) {
            throw exc;
        } catch (SQLException exc) {
            String message = "Unexpected SQL exception with message " + exc.getMessage() + " and sql state " + exc.getSQLState();
            throw new ExterminatusServiceException(message, exc, new ExterminatusServiceFault(message));
        } catch (Exception exc) {
            String message = "Unexpected exception " + exc.getClass().getName() + " with message " + exc.getMessage();
            throw new ExterminatusServiceException(message, exc, new ExterminatusServiceFault(message));
        }
    }

    private interface Supplier<T> {
        T produce() throws Exception;
    }
}
