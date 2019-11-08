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

package example.geode.greenplum;


import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.Operation;
import org.apache.geode.cache.asyncqueue.AsyncEvent;
import org.apache.geode.cache.asyncqueue.AsyncEventListener;
import org.apache.geode.internal.logging.LogService;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import java.io.*;


import java.util.List;
import java.util.Properties;

public class GreenplumAsyncEventListener implements AsyncEventListener, Declarable {
    private static Logger logger = LogService.getLogger();
    private Connection connection = null;
    private String jdbcString = null;
    private String username = null;
    private String passwd = null;
    private String tablename = null;
    private String delim = null;
    //private static final String SQL_INSERT = "INSERT INTO TEST (ID, DATA) VALUES (?,?)";


    @Override
    public boolean processEvents(List<AsyncEvent> events) {

        String SQL_UPDATE = "UPDATE " + tablename + " SET DATA=? WHERE ID=?";
        String SQL_DELETE = "DELETE FROM " + tablename + " WHERE ID=?";

        String accum = "";

        try {

            // Connect to the db just the first time
            if (connection == null) {
                logger.info("database connection is null creating one...");
                this.connection = DriverManager.getConnection(jdbcString, username, passwd);
                this.connection.setAutoCommit(false);
            }

            // loop over the events arrived
            for (AsyncEvent asyncEvent : events) {

                String value = (String) asyncEvent.getDeserializedValue();
                String key = (String) asyncEvent.getKey();
                logger.info("value received: " + value);

                // Use copy, just accumulate the batch
                if (asyncEvent.getOperation().equals(Operation.CREATE)) {
                    accum += key + delim + value + "\n";

                } else if (asyncEvent.getOperation().equals(Operation.UPDATE)) {
                    PreparedStatement preparedStatement = this.connection.prepareStatement(SQL_UPDATE);
                    preparedStatement.setString(1, value);
                    preparedStatement.setString(2, key);
                    preparedStatement.executeUpdate();
                    connection.commit();
                }
                if (asyncEvent.getOperation().equals(Operation.DESTROY)) {
                    PreparedStatement preparedStatement = this.connection.prepareStatement(SQL_DELETE);
                    preparedStatement.setString(1, key);
                    preparedStatement.executeUpdate();
                    connection.commit();
                }

            }

            // Write INSERT part with copy
            if (!accum.equals(""))  {


                CopyManager cm = ((PGConnection) connection).getCopyAPI();
                logger.info("I'm copying");
                Reader inputString = new StringReader(accum);
                BufferedReader reader = new BufferedReader(inputString);
                cm.copyIn("copy " +  tablename + " from stdin with delimiter '" + delim + "';", reader);
                connection.commit();
                accum = "";


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
        username = props.getProperty("username");
        passwd =  props.getProperty("passwd");
        tablename = props.getProperty("tablename");
        delim = props.getProperty("delim");





    }
}
