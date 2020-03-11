package com.example.fn;

import java.io.PrintWriter;
import java.io.StringWriter;
//import com.cedarsoftware.util.io.JsonWriter;
import java.sql.*;
import java.util.concurrent.TimeUnit;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

public class GetDiscountPool {
    private long d1;
    private long d2;
    private PoolDataSource poolDataSource;
    private final String dbUser         = System.getenv().get("DB_USER");
    private final String dbPassword     = System.getenv().get("DB_PASSWORD");
    private final String dbUrl          = System.getenv().get("DB_URL") + System.getenv().get("DB_SERVICE_NAME") + "?TNS_ADMIN=/function/wallet";
    //private final String clientCredPath = System.getenv().get("CLIENT_CREDENTIALS");
    //private final String keyStorePasswd = System.getenv().get("KEYSTORE_PASSWORD");
    //private final String truStorePasswd = System.getenv().get("TRUSTSTORE_PASSWORD");

    public GetDiscountPool(){        
        System.err.println("Setting up pool data source");
        //*********** FOR TESTING ONLY *************************************
        //System.err.println("ENV::" + dbUser);
        //System.err.println("ENV::" + dbPassword);
        //System.err.println("ENV::" + dbUrl);
        poolDataSource = PoolDataSourceFactory.getPoolDataSource();
        try {
            poolDataSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
            poolDataSource.setURL(dbUrl);
            poolDataSource.setUser(dbUser);
            poolDataSource.setPassword(dbPassword);
            poolDataSource.setConnectionPoolName("UCP_POOL");
        }
        catch (SQLException e) {
            System.out.println("Pool data source error!");
            e.printStackTrace();
        }
        System.err.println("Pool data source setup...");
    }

    public static class Input {
        public String demozone;
        public String paymentMethod;
        public String pizzaPrice;

        public String toString() {
            StringBuilder stb = new StringBuilder("{");
            stb.append("'demozone':'").append(demozone).append("'");
            stb.append("'paymentMethod':'").append(paymentMethod).append("'");
            stb.append("'pizzaPrice':'").append(pizzaPrice).append("'");
            stb.append("}");
            return stb.toString();
        }
    }

    public String handleRequest(Input pizzaData) {
        String exitValues    = "SALIDA::";
        ResultSet resultSet  = null;
        Connection conn      = null;
        float discount       = 0;

        try {
            String paymentMethod = pizzaData.paymentMethod.toUpperCase();
            String demozone      = pizzaData.demozone.toUpperCase();
            String pizzaPrice    = pizzaData.pizzaPrice;

            d1 = System.currentTimeMillis();
            //cast string input into a float
            System.err.println("inside Discount Function gigis fn function!!! ");
            float totalPaidValue  = Float.parseFloat(pizzaPrice);

            System.setProperty("oracle.jdbc.fanEnabled", "false");

            conn = poolDataSource.getConnection();
            conn.setAutoCommit(false);

            try {                
                System.err.println("Connected to Oracle ATP DB Pool successfully");                
                //System.err.println("QUERY:: Driver getConnection");

                StringBuilder stb = new StringBuilder("SELECT NVL (");
                stb.append("(SELECT SUM(DISCOUNT) FROM CAMPAIGN WHERE ");
                stb.append("DEMOZONE LIKE ? ");
                stb.append("AND PAYMENTMETHOD LIKE ? ");
                stb.append("AND CURRENT_DATE BETWEEN DATE_BGN AND DATE_END+1 ");
                stb.append("AND MIN_AMOUNT <= ?)");
                stb.append(",0) as DISCOUNT FROM DUAL");
                PreparedStatement pstmt = conn.prepareStatement(stb.toString());

                pstmt.setString(1,demozone);
                pstmt.setString(2,paymentMethod);
                pstmt.setFloat(3,Float.parseFloat(pizzaPrice));
                
                /*********** FOR TESTING ONLY *************************************
                System.err.println("QUERY::    " + stb.toString());
                System.err.println("QUERY:: 1. " + demozone);
                System.err.println("QUERY:: 2. " + paymentMethod);
                System.err.println("QUERY:: 3. " + pizzaPrice);
                */
                
                System.err.println("[" + pizzaData.toString() + "] - Pizza Price before discount: " + totalPaidValue + "$");
                resultSet = pstmt.executeQuery();
                if (resultSet.next()){                                                
                    discount = Float.parseFloat(resultSet.getString("DISCOUNT"))/100;                        
                    if (discount > 0){
                        //apply calculation to float eg: discount = 10%
                        totalPaidValue -=  (totalPaidValue*discount);
                        System.err.println("[" + pizzaData.toString() + "] - discount: " + resultSet.getString("DISCOUNT") + "%");
                    }
                    else
                        System.err.println ("[" + pizzaData.toString() + "] - No Discount campaign for this payment! [0%]");
                }
                else {
                    System.err.println ("[" + pizzaData.toString() + "] - No Discount campaign for this payment!");
                }
                System.err.println("[" + pizzaData.toString() + "] - total Pizza Price after discount: " + totalPaidValue + "$");
                exitValues = Float.toString(totalPaidValue);                
            }     
            catch (Exception ex) {
                StringWriter errors = new StringWriter();
                ex.printStackTrace(new PrintWriter(errors));                 
                exitValues = pizzaData.toString() + " - Error: " + ex.toString() + "\n" + ex.getMessage() + errors.toString();                
            }  
            finally {
                conn.close();
            }
        }
        catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));                 
            exitValues = pizzaData.toString() + " - Error: " + ex.toString() + "\n" + ex.getMessage() + errors.toString();;
        }
        finally{
            d2 = System.currentTimeMillis();
            long diff = d2 - d1;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
            System.err.println("[" + pizzaData.toString() + "] - Function execution time: " + Long.toString(seconds));             
        }
        return exitValues;
    }
}