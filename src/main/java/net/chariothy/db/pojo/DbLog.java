package net.chariothy.db.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.security.InvalidParameterException;
import java.util.List;

/**
 * @author Henry Tian
 */
@Data
@AllArgsConstructor
public class DbLog {
    protected String opType;
    protected String dbTable;
    protected Object json;
}
