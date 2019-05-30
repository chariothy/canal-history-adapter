package net.chariothy.db.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Henry Tian
 */
@Data
@AllArgsConstructor
public class DmlLog {
    private String name;
    private String type;
    private String oldVal;
    private String newVal;

    public DmlLog() {
    }
}
