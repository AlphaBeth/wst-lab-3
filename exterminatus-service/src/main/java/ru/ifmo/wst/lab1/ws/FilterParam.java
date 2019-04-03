package ru.ifmo.wst.lab1.ws;

import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@Data
@XmlRootElement
public class FilterParam {
    private Long id;
    private String initiator;
    private String reason;
    private String method;
    private String planet;
    private Date date;
}
