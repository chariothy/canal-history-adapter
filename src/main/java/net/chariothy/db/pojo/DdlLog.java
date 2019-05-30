package net.chariothy.db.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Henry Tian
 * @date 2019-05-20 11:10
 **/
@Data
@AllArgsConstructor
public class DdlLog {
    private String sql;
}
