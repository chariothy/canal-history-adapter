基于Canal的适配器，可以将所监测的数据库的改动历史记录下来，以备以后查询。

在resources目录下可以放置application-local.properties，用于覆盖默认配置。

目前理论上历史记录可以写入大部分RDB数据库，但只测试了写入mysql.

要求：
* jdk >= 1.8
* mysql >= 5.7
* [canal](Chttps://github.com/alibaba/canal) >= 1.13

准备

* 先在数据库中运行create_db.sql中的内容。

运行

* 开始：bin/startup.sh

* 停止：bin/stop.sh



TODO: 增加更多的写入目标，如csv、nosql等等。