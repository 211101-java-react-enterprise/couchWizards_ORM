package com.revature.cardorm.ORM;

import com.revature.cardorm.annotations.Column;
import com.revature.cardorm.annotations.Id;
import com.revature.cardorm.annotations.Table;
import com.revature.cardorm.util.ConnectionFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.util.UUID;
import java.util.*;
import java.util.stream.Collectors;

public class ORM {

    // Able to take any object that's prooperyl annotated (Tables + Columns + Id) and returns if it succeeded.
    public boolean genSave (Object target) throws Exception {
        try (Connection conn = ConnectionFactory.getInstance().getConnection()){
            // Check that we have something to work with first.
            if (target == null) throw new NullPointerException("Given object is null!");

            // Create a generic class that we can reference to for reflection.
            Class<?> targetClass = target.getClass();
            // Then check that it's got a table name
            if(!targetClass.isAnnotationPresent(Table.class)){
                throw new RuntimeException("The class " + targetClass.getSimpleName() + " is not annotated with a table!");
            }
            // Boolean flag to check that we have an id
            boolean doesIdExist = false;
            // See if the existant primary id is the default value of '' or if it has a value. If it is '' then fill it.
            Map<String, String> objMap = new HashMap<>();
            // The for loop reflects the targetClass's fields we referenced and makes them accessible.
            for (Field field: targetClass.getDeclaredFields()){
                field.setAccessible(true); // Accessible moment

                // Checks that we have a valid UUID (Which will have exactly 36 characters)
                if (field.isAnnotationPresent(Id.class)){
                    doesIdExist = true; // Update our flag
                    // We don't want to call tostring on a null value
                    if (field.get(target) != null)
                    {   // UUID's always have 36 characters
                        if (field.get(target).toString().length() != 36 ) objMap.put(field.getAnnotation(Id.class).columnName(), UUID.randomUUID().toString());
                        else objMap.put(field.getAnnotation(Id.class).columnName(), "'" + field.get(target).toString() + "'");
                    } // .getAnnotation(AnnoName.class) references the annotation which we can then pull .columnName() from.
                    else objMap.put(field.getAnnotation(Id.class).columnName(), "'" + UUID.randomUUID().toString() + "'");
                } // If it's a normal column, adds that as well
                else if (field.isAnnotationPresent(Column.class)){
                    if (field.get(target) == null) objMap.put(field.getAnnotation(Column.class).columnName(), null);
                    else objMap.put(field.getAnnotation(Column.class).columnName(), "'" + field.get(target).toString() + "'");
                }
            }
            if (!doesIdExist) throw new RuntimeException("The class " + targetClass.getSimpleName() + " does not have a primary key!");

            // Build our string out.
            String sql = "insert into ";
            sql = sql + target.getClass().getAnnotation(Table.class).tableName() + " (";
            // Obligatory stream
            String sqlColumn = objMap.entrySet().stream().map(entry -> entry.getKey()).collect(Collectors.joining(", "));
            sql = sql + sqlColumn + ") values ( ";
            sqlColumn = objMap.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.joining(", "));
            sql = sql + sqlColumn + ")";
            PreparedStatement genSQL = conn.prepareStatement(sql);

            int rowsInserted = genSQL.executeUpdate();

            if (rowsInserted != 0) {
                return true;
            }

        }catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public LinkedList<Object> genRead(Object target) throws Exception{
        try (Connection conn = ConnectionFactory.getInstance().getConnection()){
            // Check that we have something to work with first.
            if (target == null) throw new NullPointerException("Given object is null!");

            // Create a generic class that we can reference to for reflection.
            Class<?> targetClass = target.getClass();
            // Then check that it's got a table name
            if(!targetClass.isAnnotationPresent(Table.class)){
                throw new RuntimeException("The class " + targetClass.getSimpleName() + " is not annotated with a table!");
            }
            // See if the existant primary id is the default value of '' or if it has a value. If it is '' then fill it.
            Map<String, String> objMap = new HashMap<>();
            // The for loop reflects the targetClass's fields we referenced and makes them accessible.
            for (Field field: targetClass.getDeclaredFields()){
                field.setAccessible(true); // Accessible moment

                // Checks that we have a valid UUID (Which will have exactly 36 characters)
                if (field.isAnnotationPresent(Id.class)){
                    // We don't want to call tostring on a null value
                    if (field.get(target) != null)
                    {   // Saves the id to the map if it's given.
                        objMap.put(field.getAnnotation(Id.class).columnName(), field.get(target).toString());
                    } // .getAnnotation(AnnoName.class) references the annotation which we can then pull .columnName() from.

                }
                else if (field.isAnnotationPresent(Column.class)){ // Saves any fields given to query to the map
                    if (field.get(target) != null) objMap.put(field.getAnnotation(Column.class).columnName(), field.get(target).toString());
                }
            }

            // Build our string out.
            String sql = "select * from ";
            sql = sql + target.getClass().getAnnotation(Table.class).tableName() + " where ";
            // Obligatory stream
            String sqlColumn = objMap.entrySet().stream().map(entry -> entry.getKey() + " = '" + entry.getValue() + "'").collect(Collectors.joining(" and "));
            sql = sql + sqlColumn;

            // Turns our string into a statement
            PreparedStatement genSQL = conn.prepareStatement(sql);
            // Gets a result set
            ResultSet queryResult = genSQL.executeQuery();
            // Generic list to hold our data
            LinkedList<Object> queryList = new LinkedList<>();
            while (queryResult.next()) { // Continues until there is no result (in which case queryResult.next() returns false)
                // Dummy object to store into
                Object tempObj = targetClass.newInstance();
                for (Field field: targetClass.getDeclaredFields()){ // Checks for every field in our dummy instance of the object.
                    field.setAccessible(true); // Accessible moment

                    // Checks if it's the primary key
                    if (field.isAnnotationPresent(Id.class)){
                        field.set(tempObj, queryResult.getObject(field.getAnnotation(Id.class).columnName()));
                    }
                    // If not, will check for if it's a column in the table
                    else if (field.isAnnotationPresent(Column.class)){ // Saves any fields given to query to the map
                        // If the value isn't null
                        if (queryResult.getObject(field.getAnnotation(Column.class).columnName()) != null)
                        {
                            // If it's a big decimal, we need to convert to float.
                            if (queryResult.getObject(field.getAnnotation(Column.class).columnName()).getClass() == BigDecimal.class){
                                // Since you can't directly convert BigDecimal to doubles generically using cast, this workaround exists.
                                BigDecimal tempDeci = queryResult.getBigDecimal(field.getAnnotation(Column.class).columnName());
                                field.set(tempObj, tempDeci.doubleValue());
                            }
                            else { // Save non big decimal values 1:1 since other types play nice.
                                field.set(tempObj,queryResult.getObject(field.getAnnotation(Column.class).columnName()));
                            }
                        }
                        else { // Save null values
                            field.set(tempObj,queryResult.getObject(field.getAnnotation(Column.class).columnName()));
                        }

                    }
                }
                // Add the object to the result list
                queryList.add(tempObj);
            }
            // Check if the list is empty and yeet an exception, return if not empty.
            if (!queryList.isEmpty()) {
                return queryList;
            }

        }catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LinkedList<Object> genDelete(Object target) throws Exception{
        try (Connection conn = ConnectionFactory.getInstance().getConnection()){
            // Check that we have something to work with first.
            if (target == null) throw new NullPointerException("Given object is null!");

            LinkedList<Object> delTargets = genRead(target);
            int i = 0;
            while(i < delTargets.size())
            {
                // Get the target object to be deleted
                target = delTargets.get(i);

                // Build our string out.
                String sql = "delete from ";
                sql = sql + target.getClass().getAnnotation(Table.class).tableName() + " where ";

                // Grab the ID from a reflection
                for (Field field: target.getClass().getDeclaredFields()) { // Checks for every field in our dummy instance of the object.
                    field.setAccessible(true); // Accessible moment

                    // If it's the ID, we save it
                    if (field.isAnnotationPresent(Id.class)) sql = sql + field.getAnnotation(Id.class).columnName() + "= '" + field.get(target).toString() + "'";

                }
                PreparedStatement prepDel = conn.prepareStatement(sql);
                int rowsDeleted = prepDel.executeUpdate();
                i ++;
            }


            // Check if the list is empty and yeet an exception, return if not empty.
            if (!delTargets.isEmpty()) {
                return delTargets;
            }

        }catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean genUp (Object target) throws Exception{
        try (Connection conn = ConnectionFactory.getInstance().getConnection()){
            // Check that we have something to work with first.
            if (target == null) throw new NullPointerException("Given object is null!");

            // Reflect the ID into a new object we can send to select our targets.
            // Grab from a reflection
            Object tempObj = target.getClass().newInstance();
            for (Field field: target.getClass().getDeclaredFields()) { // Checks for every field in our dummy instance of the object.
                field.setAccessible(true); // Accessible moment

                // Parse our target and save any valid statics so we can find the object in a search.
                // If it's the ID, we save it
                if (field.isAnnotationPresent(Id.class) && field.getAnnotation(Id.class)!=null) {
                    field.set(tempObj, field.get(target));
                }
                else if (field.isAnnotationPresent(Column.class)) {
                    if (field.getAnnotation(Column.class).isStatic()){
                        field.set(tempObj, field.get(target));
                    } else field.set(tempObj, null);
                }

            }

            // Put our targets into a list
            List<Object> upTargets = genRead(tempObj);
            // Iterator + integer to store our rows inserted to see if the update happened.
            int i = 0;
            int rowsInserted = 0;
            // While loop that sends the updates
            while (i < upTargets.size()){
                String idHolder = new String();
                tempObj = upTargets.get(i);
                // See if the existant primary id is the default value of '' or if it has a value. If it is '' then fill it.
                Map<String, Object> objMap = new HashMap<>();
                // The for loop reflects the targetClass's fields we referenced and makes them accessible.
                for (Field field: target.getClass().getDeclaredFields()){
                    field.setAccessible(true); // Accessible moment

                    // Checks that we have a valid UUID (Which will have exactly 36 characters)
                    if (field.isAnnotationPresent(Id.class)){
                        // We don't want to call tostring on a null value
                        if (field.get(tempObj) != null)
                        {
                            idHolder = field.getAnnotation(Id.class).columnName() +  " = '" + field.get(tempObj).toString() + "'";
                        } // .getAnnotation(AnnoName.class) references the annotation which we can then pull .columnName() from.
                        else throw new NullPointerException("ERROR: somehow we got this far with a null Primary Key");
                    } // If it's a normal column, adds that as well
                    else if (field.isAnnotationPresent(Column.class)){
                        // If the field is null in the update, then we just take the value we found with the select.
                        if (field.get(target) == null) {
                            // If the field in the tempObj retrieved from select is null, record without ' so we get literal nulls.
                            if (field.get(tempObj) == null) objMap.put(field.getAnnotation(Column.class).columnName(),field.get(tempObj));
                            else objMap.put(field.getAnnotation(Column.class).columnName(), "'" + field.get(tempObj) + "'");
                        }
                        else objMap.put(field.getAnnotation(Column.class).columnName(), "'" + field.get(target).toString() + "'");
                    }
                }

                // Build our string out.
                String sql = "update ";
                sql = sql + target.getClass().getAnnotation(Table.class).tableName() + " set ";
                // Obligatory stream that creates our new values
                String sqlColumn = objMap.entrySet().stream().map(entry -> entry.getKey() + " = " + entry.getValue() + "").collect(Collectors.joining(" , "));
                // Smash together the update, set, and where portions of the update.
                sql = sql + sqlColumn + " where " + idHolder;
                // Perform the query
                PreparedStatement prepUp = conn.prepareStatement(sql);
                rowsInserted = rowsInserted + prepUp.executeUpdate();

                i++;
            }


            // If we did any updates, this will be true
            if (rowsInserted != 0) {
                return true;
            }

        }catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public List<Object> genFindAll(Object target) throws Exception {
        //Has an argument

        Class<?> targetClass = target.getClass();

        //Reflect the table annotiation
        String table_Name = target.getClass().getAnnotation(Table.class).tableName();

        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
            // Create the query
            String sql = "select * from " + table_Name;
            Statement pstmt = conn.createStatement();

            // Gets a result set
            ResultSet queryResult = pstmt.executeQuery(sql);


            // Generic list to hold our data
            List<Object> queryList = new LinkedList<>();

            while (queryResult.next()) { // Continues until there is no result (in which case queryResult.next() returns false)
                // Dummy object to store into
                Object tempObj = targetClass.newInstance();
                for (Field field: targetClass.getDeclaredFields()){ // Checks for every field in our dummy instance of the object.
                    field.setAccessible(true); // Accessible moment

                    // Checks if it's the primary key
                    if (field.isAnnotationPresent(Id.class)){
                        field.set(tempObj, queryResult.getObject(field.getAnnotation(Id.class).columnName()));
                    }
                    // If not, will check for if it's a column in the table
                    else if (field.isAnnotationPresent(Column.class)){ // Saves any fields given to query to the map
                        // If the value isn't null
                        if (queryResult.getObject(field.getAnnotation(Column.class).columnName()) != null)
                        {
                            // If it's a big decimal, we need to convert to float.
                            if (queryResult.getObject(field.getAnnotation(Column.class).columnName()).getClass() == BigDecimal.class){
                                // Since you can't directly convert BigDecimal to doubles generically using cast, this workaround exists.
                                BigDecimal tempDeci = queryResult.getBigDecimal(field.getAnnotation(Column.class).columnName());
                                field.set(tempObj, tempDeci.doubleValue());
                            }
                            else { // Save non big decimal values 1:1 since other types play nice.
                                field.set(tempObj,queryResult.getObject(field.getAnnotation(Column.class).columnName()));
                            }
                        }
                        else { // Save null values
                            field.set(tempObj,queryResult.getObject(field.getAnnotation(Column.class).columnName()));
                        }

                    }
                }
                // Add the object to the result list
                queryList.add(tempObj);
            }
            // Check if the list is empty and yeet an exception, return if not empty.
            if (!queryList.isEmpty()) {
                return queryList;
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

}
