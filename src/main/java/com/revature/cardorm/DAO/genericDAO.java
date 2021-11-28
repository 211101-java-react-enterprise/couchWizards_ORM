package com.revature.cardorm.DAO;

// When given a class with tables and column annotations inside, performs various functions

import com.revature.cardorm.annotations.Table;
import com.revature.cardorm.annotations.Column;
import com.revature.cardorm.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;


public class genericDAO {


    // Saves a new object into the database
    public boolean save(Class givenClass) {
        try (Connection conn = ConnectionFactory.getInstance().getConnection()){
            // TODO: Figure out how to set default values for non given values like c_id

            // TODO: create annotation based logic that can build an sql query string
            // Would use a simple loop with a reflected annotation, pulling the values from it and performing the appropriate
            // inserts.
            // Possible hangups
            //      - Changing to a different insert based on the value type
            //      - Figuring out the data type from the annotations + how the annotations are actually grouped?
            String sql = "";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            // Insert the rows and record any changes to make sure they happen
            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted != 0) {
                return true;
            }

        }catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
