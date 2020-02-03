package ua.ardas.esputnik.events.service;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.ardas.esputnik.dto.ParamWrapper;
import ua.ardas.esputnik.dto.Parameter;
import ua.ardas.esputnik.events.queue.EventRedisDto;

import java.util.ArrayList;
import java.util.List;

@Service
public class CampaignParamsService {
    private static final Gson GSON = new Gson();

    private static final String EVENT_KEY = "eventKey";
    private static final String EVENT_TYPE_KEY = "eventTypeKey";
    private static final String EVENT_PARAM_NAMES = "eventParamNames";
    private static final String EVENT_EMAIL = "eventEmail";
    private static final String RECIPIENT = "recipient";
    private static final String EMAIL_ADDRESS = "EmailAddress";

    @Value("${events.addEmailParam:false}")
    private boolean addEmailParam;


    public ParamWrapper transformParams(EventRedisDto event) {
        ParamWrapper params = new Gson().fromJson(event.getJson(), ParamWrapper.class);
        if (null == params.getParams()) {
            params.setParams(new ArrayList<>());
        }

        addParam(params, EVENT_TYPE_KEY, event.getEventTypeKey());
        addParam(params, EVENT_KEY, event.getKeyValue());
        addParam(params, EVENT_PARAM_NAMES, getParams(params));
        if (addEmailParam) {
            String emailParam = getEmailParam(params);
            addParam(params, EVENT_EMAIL, emailParam);
            addParam(params, RECIPIENT, emailParam);
        }

        return params;
    }

    private String getEmailParam(ParamWrapper params) {
        String res = StringUtils.EMPTY;
        for (Parameter p : params.getParams()) {
            if (EMAIL_ADDRESS.equals(p.getName())) {
                return p.getValue();
            }
        }
        return res;
    }

    private String getParams(ParamWrapper params) {
        List<String> res = new ArrayList<>();
        for (Parameter p : params.getParams()) {
            res.add(p.getName());
        }
        return GSON.toJson(res);
    }

    private void addParam(ParamWrapper params, String key, String value) {
        if (!paramAlreadyExist(params, key)) {
            params.getParams().add(new Parameter(key, value));
        }
    }

    private boolean paramAlreadyExist(ParamWrapper params, String name) {
        for (Parameter p : params.getParams()) {
            if (name.equals(p.getName())) {
                return true;
            }
        }
        return false;
    }

}
