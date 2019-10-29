/*
 * Copyright  2018 Charlie Black
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package example.geode.kafka;


import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.Operation;
import org.apache.geode.cache.asyncqueue.AsyncEvent;
import org.apache.geode.cache.asyncqueue.AsyncEventListener;
import org.apache.geode.internal.logging.LogService;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.postgresql.core.BaseConnection;
import org.postgresql.copy.CopyManager;
import java.io.*;

import java.util.List;
import java.util.Properties;

public class KafkaAsyncEventListener implements AsyncEventListener, Declarable {
    private static Logger logger = LogService.getLogger();
    private Connection connection = null;
   // private static final String jdbcString = "jdbc:postgresql://172.16.125.152:5432/example";
    private String jdbcString = null;
    private static final String username = "gpadmin";
    private static final String passwd = "";
    private static final String SQL_INSERT = "INSERT INTO TEST (ID, DATA) VALUES (?,?)";
    private static final String SQL_UPDATE = "UPDATE TEST SET DATA=? WHERE ID=?";
    private static final String SQL_DELETE = "DELETE FROM TEST WHERE ID=?";

    @Override
    public boolean processEvents(List<AsyncEvent> events) {

        String buffer = "";

        try {

            // Connect to the db just the first time
            if (connection == null) {
                logger.info("database connection is null creating one...");
                this.connection = DriverManager.getConnection(jdbcString, username, passwd);
            }

            // loop over the events arrived
            for (AsyncEvent asyncEvent : events) {


                String value = (String) asyncEvent.getDeserializedValue();
                String key = (String) asyncEvent.getKey();
                logger.info("value received: " + value);

                if(asyncEvent.getOperation().equals(Operation.CREATE)) {
                    PreparedStatement preparedStatement = this.connection.prepareStatement(SQL_INSERT);
                    preparedStatement.setString(1, key);
                    preparedStatement.setString(2, value);
                    preparedStatement.executeUpdate();
                }
                else if(asyncEvent.getOperation().equals(Operation.UPDATE)) {
                    PreparedStatement preparedStatement = this.connection.prepareStatement(SQL_UPDATE);
                    preparedStatement.setString(1, value);
                    preparedStatement.setString(2, key);
                    preparedStatement.executeUpdate();
                }
                if(asyncEvent.getOperation().equals(Operation.DESTROY)) {
                    PreparedStatement preparedStatement = this.connection.prepareStatement(SQL_DELETE);
                    preparedStatement.setString(1, key);
                    //preparedStatement.setString(2, value);
                    preparedStatement.executeUpdate();
                }

            }


        } catch (Exception e) {
            logger.error("Could not insert data to Postgresql/Greenplum.", e);
            return false;
        }

        return true;
    }


    @Override
    public void close() {
        try {
            connection.close();
        }
        catch (Exception e)    {
            logger.error("Impossible to close the connection to the Postgresql/Greenplum instance", e);

        }
    }

    @Override
    public void init(Properties props) {

        jdbcString = props.getProperty("jdbcString");



    }
}
